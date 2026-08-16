package com.example.slsHrms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.slsHrms.api.FaceRecognitionRequest
import com.example.slsHrms.api.FaceRecognitionResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.databinding.ActivityFaceValidateBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.util.Base64 as AndroidBase64

/**
 * Live, hands-free face validation.
 *
 * Opens a CameraX preview, runs on-device ML Kit face detection on each frame,
 * and the moment a face appears it captures one frame, sends it to the existing
 * `/check-face` endpoint (matched against employee_face_mst on the configured
 * backend), and — on a match — returns the employee code + name to the caller
 * and closes itself. No shutter button required.
 */
class FaceValidateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BRANCH_ID = "BRANCH_ID"
        const val EXTRA_CO_ID = "CO_ID"
        const val EXTRA_EMP_CODE = "EMP_CODE"
        const val EXTRA_EMP_NAME = "EMP_NAME"
        private const val TAG = "FACE_VALIDATE"

        // Blink state machine: eyes open → closed → open again.
        private const val STAGE_NEED_OPEN = 0
        private const val STAGE_NEED_CLOSED = 1
        private const val STAGE_NEED_REOPEN = 2

        // Both eyes must be above this to count as "open", below the lower one
        // to count as "closed". The gap avoids flicker around the threshold.
        private const val EYE_OPEN_THRESHOLD = 0.6f
        private const val EYE_CLOSED_THRESHOLD = 0.3f
    }

    private lateinit var binding: ActivityFaceValidateBinding
    private var branchId: Int = 0

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    // Bound camera provider + the lens we are currently showing, so the
    // "Flip Camera" button can rebind between the front and back lenses.
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    // True from the instant a blink is confirmed until validation finishes,
    // so we only fire one capture/network round-trip at a time.
    private val isProcessing = AtomicBoolean(false)

    // ── Liveness (anti-spoof) blink gate ─────────────────────────
    // Require a real blink (eyes open → closed → open) before we ever capture &
    // validate, so a still photo — which can't blink — is rejected. Partial
    // progress survives dropped frames: the camera briefly loses the face mid-
    // blink and we must NOT throw the progress away when that happens. Touched
    // from the analyzer thread and reset from the UI thread, hence the atomics.
    private val blinkStage = java.util.concurrent.atomic.AtomicInteger(STAGE_NEED_OPEN)
    @Volatile private var lastPrompt = ""

    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            // Classification gives us per-eye open probabilities for blink detection.
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.2f)
            .build()
        FaceDetection.getClient(options)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceValidateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        branchId = intent.getIntExtra(EXTRA_BRANCH_ID, 0)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.btnFlipCamera.setOnClickListener { flipCamera() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            bindCamera()
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * (Re)bind the preview, capture and analysis use cases to the lens given by
     * [lensFacing]. Called on first start and again whenever the user flips.
     * If the requested lens isn't available we fall back to the other one.
     */
    private fun bindCamera() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, FaceAnalyzer()) }

        val selector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, imageCapture, analysis)
        } catch (e: Exception) {
            // The requested lens may not exist (e.g. a tablet with no front
            // camera) — fall back to the opposite lens.
            Log.w(TAG, "Lens $lensFacing bind failed, trying the other lens", e)
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            val fallback = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, fallback, preview, imageCapture, analysis)
            } catch (e2: Exception) {
                Log.e(TAG, "Camera bind failed", e2)
                Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }

    /** Toggle between the front and back lens, then rebind. */
    private fun flipCamera() {
        if (cameraProvider == null) return
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        // Re-arm detection so a capture in flight doesn't carry over to the new lens.
        isProcessing.set(false)
        resetLiveness()
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.text = "Position your face and blink"
        bindCamera()
    }

    private inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        @ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage == null || isProcessing.get()) {
                imageProxy.close()
                return
            }
            val input = InputImage.fromMediaImage(
                mediaImage, imageProxy.imageInfo.rotationDegrees
            )
            faceDetector.process(input)
                .addOnSuccessListener { faces ->
                    if (!isProcessing.get()) trackLiveness(faces.firstOrNull())
                }
                .addOnFailureListener { e -> Log.e(TAG, "Face detect failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        }
    }

    /**
     * Advance the blink state machine for the most prominent [face]:
     * eyes open → closed → open. Partial progress is kept across frames where
     * the face momentarily drops out (which happens naturally mid-blink). Once a
     * full blink is seen we capture once. A still photo can't blink, so it never
     * gets here.
     */
    private fun trackLiveness(face: com.google.mlkit.vision.face.Face?) {
        // Dropped frame — keep whatever progress we have, just wait for the next.
        if (face == null) return

        val left = face.leftEyeOpenProbability
        val right = face.rightEyeOpenProbability
        // Probabilities are null when the model couldn't classify this frame; skip it.
        if (left == null || right == null) return

        val eyesOpen = left > EYE_OPEN_THRESHOLD && right > EYE_OPEN_THRESHOLD
        val eyesClosed = left < EYE_CLOSED_THRESHOLD && right < EYE_CLOSED_THRESHOLD

        when (blinkStage.get()) {
            STAGE_NEED_OPEN -> if (eyesOpen) {
                blinkStage.set(STAGE_NEED_CLOSED)
                updatePrompt("Blink to confirm you're live")
            }
            STAGE_NEED_CLOSED -> if (eyesClosed) blinkStage.set(STAGE_NEED_REOPEN)
            STAGE_NEED_REOPEN -> if (eyesOpen) {
                // Full blink seen — confirm liveness and capture exactly once.
                if (isProcessing.compareAndSet(false, true)) {
                    runOnUiThread { onLivenessConfirmed() }
                }
            }
        }
    }

    /** Update the status banner only when the text actually changes. */
    private fun updatePrompt(text: String) {
        if (text != lastPrompt) {
            lastPrompt = text
            runOnUiThread { binding.tvStatus.text = text }
        }
    }

    private fun onLivenessConfirmed() {
        binding.tvStatus.text = "Liveness confirmed — validating…"
        binding.progressBar.visibility = View.VISIBLE
        captureAndValidate()
    }

    /** Clear blink progress so the challenge starts fresh. */
    private fun resetLiveness() {
        blinkStage.set(STAGE_NEED_OPEN)
        lastPrompt = ""
    }

    private fun captureAndValidate() {
        val capture = imageCapture ?: run {
            resetForRetry("Camera not ready")
            return
        }
        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val b64 = try {
                        imageProxyToResizedBase64(image)
                    } catch (e: Exception) {
                        Log.e(TAG, "encode failed", e)
                        null
                    } finally {
                        image.close()
                    }
                    runOnUiThread {
                        if (b64 == null) resetForRetry("Could not read frame")
                        else sendToBackend(b64)
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "capture error", exc)
                    runOnUiThread { resetForRetry("Capture failed") }
                }
            }
        )
    }

    private fun sendToBackend(base64Image: String) {
        val request = FaceRecognitionRequest(
            image = base64Image,
            branchId = if (branchId > 0) branchId else null
        )
        RetrofitClient.getApiService(this).checkFace(request)
            .enqueue(object : Callback<FaceRecognitionResponse> {
                override fun onResponse(
                    call: Call<FaceRecognitionResponse>,
                    response: Response<FaceRecognitionResponse>
                ) {
                    binding.progressBar.visibility = View.GONE
                    val result = response.body()
                    if (response.isSuccessful && result != null && result.status == "success") {
                        returnResult(result.empCode, result.empName)
                    } else {
                        resetForRetry(result?.message ?: "Face not recognized")
                    }
                }

                override fun onFailure(call: Call<FaceRecognitionResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    resetForRetry("Network error: ${t.localizedMessage}")
                }
            })
    }

    private fun returnResult(empCode: String?, empName: String?) {
        val data = Intent().apply {
            putExtra(EXTRA_EMP_CODE, empCode ?: "")
            putExtra(EXTRA_EMP_NAME, empName ?: "")
        }
        setResult(RESULT_OK, data)
        Toast.makeText(this, "Matched: ${empName ?: empCode}", Toast.LENGTH_SHORT).show()
        finish()
    }

    /** Show a message, then re-arm detection after a brief pause to avoid spamming. */
    private fun resetForRetry(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.text = "$message — try again"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        binding.previewView.postDelayed({
            if (!isFinishing) {
                isProcessing.set(false)
                // Require a fresh blink before the next capture.
                resetLiveness()
                binding.tvStatus.text = "Position your face and blink"
            }
        }, 1500)
    }

    /**
     * Convert a captured JPEG [ImageProxy] to a rotation-corrected, downscaled
     * (max 720 px) base64 JPEG — same wire format the rest of the app uses, so
     * the round-trip and server-side encoding stay fast.
     */
    private fun imageProxyToResizedBase64(
        image: ImageProxy, maxDim: Int = 720, quality: Int = 80
    ): String {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Bitmap decode failed")

        val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (scale < 1f) {
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            if (scaled != bitmap) bitmap.recycle()
            bitmap = scaled
        }

        val rotationDeg = image.imageInfo.rotationDegrees
        if (rotationDeg != 0) {
            val matrix = Matrix().apply { postRotate(rotationDeg.toFloat()) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated != bitmap) bitmap.recycle()
            bitmap = rotated
        }

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        bitmap.recycle()
        return AndroidBase64.encodeToString(baos.toByteArray(), AndroidBase64.NO_WRAP)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
        faceDetector.close()
    }
}
