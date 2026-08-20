package com.example.slsHrms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.slsHrms.databinding.ActivityFaceValidateBinding
import com.example.slsHrms.face.FaceCamera
import com.example.slsHrms.face.FacePrefs
import com.example.slsHrms.face.FaceEngine
import com.example.slsHrms.face.FaceFrameAnalyzer
import com.example.slsHrms.face.FaceGallery
import com.example.slsHrms.face.FaceOverlayView
import com.example.slsHrms.sync.OfflinePhotoStore
import com.google.mlkit.vision.face.Face
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Live, hands-free face recognition for attendance.
 *
 * Every camera frame: detect -> align -> MobileFaceNet embed -> cosine match
 * against the branch's enrolled faces (FaceGallery, on this device). Recognised
 * faces get a green bracket with the name and score, unknown ones an amber one.
 * The moment the person in front is a confident match, the employee code + name
 * are returned to the caller and this screen closes. No shutter, no server
 * round-trip; the capture still travels with the record so the server can
 * re-verify it with dlib.
 */
class FaceValidateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BRANCH_ID = "BRANCH_ID"
        const val EXTRA_CO_ID = "CO_ID"
        const val EXTRA_EMP_CODE = "EMP_CODE"
        const val EXTRA_EMP_NAME = "EMP_NAME"

        // Match results — the caller queues these with the attendance so the
        // server can re-verify with dlib once the record is uploaded.
        const val EXTRA_MATCHED_OFFLINE = "MATCHED_OFFLINE"
        const val EXTRA_PHOTO_PATH = "PHOTO_PATH"
        const val EXTRA_CONFIDENCE = "CONFIDENCE"

        // ponytail: two consecutive ACCEPT frames on the same person before we
        // return — a single frame can flicker onto a look-alike.
        private const val CONFIRM_FRAMES = 2
        // Borderline (REVIEW) on the same person for this long -> ask the
        // operator instead of scanning forever. ~1.5 s at analysis frame rate.
        private const val REVIEW_FRAMES = 12
    }

    private lateinit var binding: ActivityFaceValidateBinding
    private lateinit var executor: ExecutorService
    private var analyzer: FaceFrameAnalyzer? = null
    private var branchId = 0

    // Set the instant we decide on a result, so later frames are ignored.
    private val done = AtomicBoolean(false)
    private var lastEbId = -1
    private var acceptStreak = 0
    private var reviewStreak = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceValidateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Shop-floor use: the screen must not dim/lock mid-scan.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        branchId = intent.getIntExtra(EXTRA_BRANCH_ID, 0)
        executor = Executors.newSingleThreadExecutor()

        binding.btnClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        binding.btnFlipCamera.setOnClickListener {
            FacePrefs.setUseFront(this, !FacePrefs.useFront(this))
            startCamera()
        }
        binding.btnSettings.setOnClickListener { showSettings() }

        if (!FaceEngine.isAvailable(this)) {
            Toast.makeText(this, "Face model missing on this build — use manual entry", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Room count: off the UI thread. An empty gallery is not fatal — the
        // operator can still see the camera, but nothing will ever match.
        executor.execute {
            val enrolled = FaceGallery.countFor(this, branchId)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                binding.tvStatus.text = if (enrolled == 0) {
                    "No employee faces downloaded for this branch — open Sync Center, or enter the code manually"
                } else {
                    "Scanning…  ($enrolled enrolled faces)"
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val a = FaceFrameAnalyzer(::handleFrame).also { analyzer = it }
        FaceCamera.start(
            this, binding.previewView, executor, a, FacePrefs.useFront(this),
            onBound = { front -> binding.overlay.mirrored = front },
            onError = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                setResult(RESULT_CANCELED)
                finish()
            }
        )
    }

    /** Analysis thread, every frame. */
    private fun handleFrame(frame: Bitmap, faces: List<Face>) {
        if (done.get()) return
        if (faces.isEmpty()) {
            binding.overlay.clear()
            lastEbId = -1; acceptStreak = 0; reviewStreak = 0
            return
        }

        // Every face gets a box; only the person in front (biggest face) can decide.
        val largest = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
        var decision: FaceGallery.Match? = null
        val boxes = ArrayList<FaceOverlayView.Box>(faces.size)

        for (face in faces) {
            val box = face.boundingBox
            if (!FaceEngine.isUsable(face, frame.width)) {
                boxes.add(FaceOverlayView.Box(box, "Come closer", false))
                continue
            }
            val aligned = FaceEngine.align(frame, face)
            val embedding = aligned?.let { FaceEngine.embed(this, it) }
            if (embedding == null) {
                boxes.add(FaceOverlayView.Box(box, "", false))
                continue
            }
            val match = FaceGallery.match(this, branchId, embedding)
            val score = match?.let { String.format("%.2f", it.similarity) }
            boxes.add(
                when (match?.verdict) {
                    FaceGallery.Verdict.ACCEPT -> FaceOverlayView.Box(box, "${match.empName}  $score", true)
                    FaceGallery.Verdict.REVIEW -> FaceOverlayView.Box(box, "${match.empName}?  $score", false)
                    else -> FaceOverlayView.Box(box, "UNKNOWN", false)
                }
            )
            if (face === largest) decision = match
        }
        binding.overlay.setResults(frame.width, frame.height, boxes)
        decide(decision, frame)
    }

    /** Streak logic for the face in front; fires the result exactly once. */
    private fun decide(match: FaceGallery.Match?, frame: Bitmap) {
        if (match != null) android.util.Log.d(
            "FACE_MATCH", "%s %s sim=%.3f runnerUp=%.3f".format(match.verdict, match.empCode, match.similarity, match.runnerUp)
        )
        if (match == null || match.verdict == FaceGallery.Verdict.REJECT) {
            lastEbId = -1; acceptStreak = 0; reviewStreak = 0
            return
        }
        if (match.ebId != lastEbId) {
            lastEbId = match.ebId; acceptStreak = 0; reviewStreak = 0
        }
        if (match.verdict == FaceGallery.Verdict.ACCEPT) {
            // Cooldown: this employee was punched from this device a moment ago.
            val cooldown = FacePrefs.cooldownSec(this)
            val since = FacePrefs.secondsSincePunch(match.empCode)
            if (cooldown > 0 && since != null && since < cooldown) {
                acceptStreak = 0
                runOnUiThread {
                    binding.tvStatus.text = "${match.empName} already marked ${since}s ago — wait ${cooldown - since}s"
                }
                return
            }
            acceptStreak++; reviewStreak = 0
            if (acceptStreak >= CONFIRM_FRAMES && done.compareAndSet(false, true)) {
                analyzer?.paused = true
                val photo = OfflinePhotoStore.write(this, toJpegBytes(frame))
                runOnUiThread { returnResult(match, photo) }
            }
        } else {
            reviewStreak++
            if (reviewStreak >= REVIEW_FRAMES && done.compareAndSet(false, true)) {
                analyzer?.paused = true
                val photo = OfflinePhotoStore.write(this, toJpegBytes(frame))
                runOnUiThread { confirmLowConfidence(match, photo) }
            }
        }
    }

    /** Borderline score: one tap from the operator instead of a silent guess. */
    private fun confirmLowConfidence(match: FaceGallery.Match, photoPath: String?) {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Confirm employee")
            .setMessage(
                "Matched with low confidence.\n\n" +
                    "${match.empName} (${match.empCode})\n" +
                    "Confidence: ${"%.1f".format(match.confidencePct)}%\n\n" +
                    "Is this the right person?"
            )
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ -> returnResult(match, photoPath) }
            .setNegativeButton("No") { _, _ ->
                OfflinePhotoStore.delete(photoPath)
                lastEbId = -1; acceptStreak = 0; reviewStreak = 0
                done.set(false)
                analyzer?.paused = false
            }
            .show()
    }

    /** ⚙ — match threshold (0.40..1.00, overrides the server value) and punch cooldown. */
    private fun showSettings() {
        val view = layoutInflater.inflate(R.layout.dialog_face_settings, null)
        val txtThreshold = view.findViewById<TextView>(R.id.txtThreshold)
        val seekThreshold = view.findViewById<SeekBar>(R.id.seekThreshold)
        val txtCooldown = view.findViewById<TextView>(R.id.txtCooldown)
        val seekCooldown = view.findViewById<SeekBar>(R.id.seekCooldown)

        val serverThreshold = FaceGallery.serverAcceptThreshold(this)
        val current = FacePrefs.threshold(this) ?: serverThreshold
        seekThreshold.max = 60                                   // 0.40 .. 1.00 in 0.01 steps
        seekThreshold.progress = ((current - 0.40f) * 100).toInt().coerceIn(0, 60)
        fun thresholdOf(p: Int) = 0.40f + p / 100f
        fun showThreshold(v: Float) {
            txtThreshold.text = "Match threshold: %.2f%s".format(v, if (v == serverThreshold) "  (server default)" else "")
        }
        showThreshold(thresholdOf(seekThreshold.progress))
        seekThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, p: Int, fromUser: Boolean) = showThreshold(thresholdOf(p))
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        seekCooldown.max = 300
        seekCooldown.progress = FacePrefs.cooldownSec(this)
        fun showCooldown(sec: Int) { txtCooldown.text = "Cooldown: ${if (sec == 0) "off" else "$sec s"}" }
        showCooldown(seekCooldown.progress)
        seekCooldown.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, p: Int, fromUser: Boolean) = showCooldown(p)
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        AlertDialog.Builder(this)
            .setTitle("Face settings")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val t = thresholdOf(seekThreshold.progress)
                FacePrefs.setThreshold(this, if (t == serverThreshold) null else t)
                FacePrefs.setCooldownSec(this, seekCooldown.progress)
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Use server default") { _, _ ->
                FacePrefs.setThreshold(this, null)
                FacePrefs.setCooldownSec(this, FacePrefs.DEFAULT_COOLDOWN_SEC)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun returnResult(match: FaceGallery.Match, photoPath: String?) {
        val data = Intent().apply {
            putExtra(EXTRA_EMP_CODE, match.empCode)
            putExtra(EXTRA_EMP_NAME, match.empName)
            // Matched by the on-device engine: the server re-runs dlib on the
            // capture when the record arrives, so the match is auditable.
            putExtra(EXTRA_MATCHED_OFFLINE, true)
            putExtra(EXTRA_PHOTO_PATH, photoPath)
            putExtra(EXTRA_CONFIDENCE, match.confidencePct)
        }
        setResult(RESULT_OK, data)
        Toast.makeText(this, "Matched: ${match.empName}", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun toJpegBytes(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return baos.toByteArray()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::executor.isInitialized) executor.shutdown()
    }
}
