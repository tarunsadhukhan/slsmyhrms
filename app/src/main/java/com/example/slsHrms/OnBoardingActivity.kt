package com.example.slsHrms

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import com.example.slsHrms.api.OnBoardingRegisterRequest
import com.example.slsHrms.api.OnBoardingRegisterResponse
import com.example.slsHrms.api.OnBoardingEmployeeResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.databinding.ActivityOnBoardingBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnBoardingBinding
    private var currentEmpCode: String = ""
    private var currentEbId: Int = 0
    private var branchId: Int = 0
    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private var capturedBase64: String? = null

    // ── Camera launcher ──────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            // Some phones (e.g. Samsung Galaxy M05/M01) save the JPEG in the sensor's
            // native landscape orientation and record the rotation only in the EXIF
            // Orientation tag. BitmapFactory ignores that tag, so the captured portrait
            // photo would display sideways. Apply the EXIF rotation up front so both the
            // preview and the image sent to the backend are upright.
            val bmp = decodeOrientedBitmap(photoFile!!)
            // Re-encode the corrected bitmap so the backend receives the upright image too.
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            capturedBase64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
            // Show preview
            binding.ivFacePreview.setImageBitmap(bmp)
            binding.ivFacePreview.visibility = View.VISIBLE
            binding.btnRegisterFace.isEnabled = true
        } else {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera() else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        branchId = intent.getIntExtra("BRANCH_ID", 0)

        setupToolbar()
        setupSearchButton()
        setupCaptureButton()
        setupRegisterButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ── Search employee by eb_id ─────────────────────────────────

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener {
            val empCode = binding.etEbId.text.toString().trim()
            if (empCode.isEmpty()) {
                binding.etEbId.error = "Enter a valid Employee Code"
                return@setOnClickListener
            }
            loadEmployee(empCode)
        }
    }

    private fun loadEmployee(empCode: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.cardEmployee.visibility = View.GONE
        binding.cardCamera.visibility = View.GONE
        capturedBase64 = null
        binding.btnRegisterFace.isEnabled = false

        RetrofitClient.getApiService(this).getOnBoardingEmployee(empCode, if (branchId > 0) branchId else null)
            .enqueue(object : Callback<OnBoardingEmployeeResponse> {
                override fun onResponse(call: Call<OnBoardingEmployeeResponse>, response: Response<OnBoardingEmployeeResponse>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val emp = response.body()!!
                        currentEmpCode = emp.empCode ?: ""
                        currentEbId = emp.ebId ?: 0
                        binding.tvEmpName.text = emp.name ?: "-"
                        binding.tvEmpCode.text = "Code: ${emp.empCode ?: "-"}"
                        binding.tvDeptDesig.text = "${emp.departmentName ?: "-"} | ${emp.designationName ?: "-"}"
                        updateFaceCount(emp.faceCount ?: 0, emp.canRegister ?: false)
                        binding.cardEmployee.visibility = View.VISIBLE
                    } else {
                        val msg = response.body()?.message ?: "Employee not found"
                        Toast.makeText(this@OnBoardingActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<OnBoardingEmployeeResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@OnBoardingActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateFaceCount(count: Int, canRegister: Boolean) {
        binding.tvFaceCount.text = "Faces Registered: $count / 3"
        // Colour indicators
        val dot1 = binding.dotFace1
        val dot2 = binding.dotFace2
        val dot3 = binding.dotFace3
        val activeColor = getColor(R.color.toolbar_dark_blue)
        val inactiveColor = getColor(R.color.input_border)
        dot1.setColorFilter(if (count >= 1) activeColor else inactiveColor)
        dot2.setColorFilter(if (count >= 2) activeColor else inactiveColor)
        dot3.setColorFilter(if (count >= 3) activeColor else inactiveColor)

        binding.cardCamera.visibility = if (canRegister) View.VISIBLE else View.GONE
        if (!canRegister) {
            Toast.makeText(this, "Maximum 3 faces already registered for this employee.", Toast.LENGTH_LONG).show()
        }
    }

    // ── Camera ───────────────────────────────────────────────────

    private fun setupCaptureButton() {
        binding.btnCapture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val dir = File(filesDir, "face_photos").also { it.mkdirs() }
        photoFile = File(dir, "face_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile!!)
        cameraLauncher.launch(photoUri)
    }

    /**
     * Decodes [file] into a Bitmap and rotates/flips it to match the EXIF Orientation tag,
     * so images captured on phones that only set the EXIF tag (e.g. Samsung Galaxy M05/M01)
     * are upright instead of sideways.
     */
    private fun decodeOrientedBitmap(file: File): Bitmap {
        val bmp = BitmapFactory.decodeFile(file.absolutePath)
        val orientation = try {
            ExifInterface(file.absolutePath)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }

    // ── Register face ────────────────────────────────────────────

    private fun setupRegisterButton() {
        binding.btnRegisterFace.setOnClickListener {
            val base64 = capturedBase64
            if (base64 == null) {
                Toast.makeText(this, "Please capture a face photo first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentEmpCode.isEmpty()) {
                Toast.makeText(this, "No employee selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registerFace(currentEmpCode, base64)
        }
    }

    private fun registerFace(empCode: String, base64: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegisterFace.isEnabled = false

        val request = OnBoardingRegisterRequest(
            empCode = empCode,
            faceImage = base64,
            branchId = if (branchId > 0) branchId else null
        )
        RetrofitClient.getApiService(this).registerOnBoardingFace(request)
            .enqueue(object : Callback<OnBoardingRegisterResponse> {
                override fun onResponse(call: Call<OnBoardingRegisterResponse>, response: Response<OnBoardingRegisterResponse>) {
                    binding.progressBar.visibility = View.GONE
                    val body = response.body()
                    if (response.isSuccessful && body?.status == "success") {
                        Toast.makeText(this@OnBoardingActivity, body.message ?: "Face registered!", Toast.LENGTH_LONG).show()
                        capturedBase64 = null
                        binding.ivFacePreview.setImageResource(R.drawable.ic_person)
                        // Reload employee to refresh count
                        loadEmployee(empCode)
                    } else {
                        val msg = body?.message ?: "Failed to register face"
                        Toast.makeText(this@OnBoardingActivity, msg, Toast.LENGTH_LONG).show()
                        binding.btnRegisterFace.isEnabled = true
                    }
                }
                override fun onFailure(call: Call<OnBoardingRegisterResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegisterFace.isEnabled = true
                    Toast.makeText(this@OnBoardingActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

