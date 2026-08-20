package com.example.slsHrms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.Intent
import android.graphics.BitmapFactory
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
    private var capturedBase64: String? = null
    // MobileFaceNet template averaged over the live samples FaceEnrollActivity
    // took — sent as embedding_mobile so the worker is matchable offline at once.
    private var capturedTemplate: FloatArray? = null

    companion object {
        /** Must match MOBILE_MODEL_VER in src/sync/routes.py (v2 = landmark crop). */
        const val MOBILE_MODEL_VER = "mobilefacenet-v2"
    }

    // ── Live enrolment launcher ──────────────────────────────────
    // FaceEnrollActivity takes 5 live samples, averages them into one template
    // and hands back that template plus the best frame as a JPEG.
    private val enrollLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val path = data?.getStringExtra(FaceEnrollActivity.EXTRA_PHOTO_PATH)
        val bmp = path?.let { BitmapFactory.decodeFile(it) }
        if (result.resultCode != RESULT_OK || path == null || bmp == null) {
            Toast.makeText(this, "Face capture cancelled", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        capturedBase64 = android.util.Base64.encodeToString(File(path).readBytes(), android.util.Base64.NO_WRAP)
        capturedTemplate = data.getFloatArrayExtra(FaceEnrollActivity.EXTRA_TEMPLATE)
        binding.ivFacePreview.setImageBitmap(bmp)
        binding.ivFacePreview.visibility = View.VISIBLE
        binding.btnRegisterFace.isEnabled = true
        data.getStringExtra(FaceEnrollActivity.EXTRA_DUPLICATE_OF)?.let { other ->
            Toast.makeText(this, "Note: this face already matches $other", Toast.LENGTH_LONG).show()
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
        capturedTemplate = null
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
                    } else if (response.code() == 504) {
                        // OkHttp's "unsatisfiable cache request" — we are offline
                        // with nothing cached for this code, not "no such employee".
                        allowOfflineEnrolment(empCode)
                    } else {
                        // 4xx bodies live in errorBody() — e.g. the server's
                        // "… is in HR status OPEN — must be JOINED before face enrolment".
                        val msg = response.body()?.message
                            ?: response.errorBody()?.string()?.let { extractErrorMessage(it) }
                            ?: "Employee not found"
                        Toast.makeText(this@OnBoardingActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<OnBoardingEmployeeResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    allowOfflineEnrolment(empCode)
                }
            })
    }

    /**
     * Enrol with no server. The employee's existence and the 3-face limit can
     * only be checked by the backend, so both are enforced when the queued
     * record uploads: an invalid enrolment comes back as a CONFLICT row in the
     * Sync Center rather than being silently accepted or silently lost.
     */
    private fun allowOfflineEnrolment(empCode: String) {
        binding.progressBar.visibility = View.GONE
        currentEmpCode = empCode
        currentEbId = 0

        binding.tvEmpName.text = "(offline — looking up…)"
        binding.tvEmpCode.text = "Code: $empCode"

        // Both lookups hit disk (Room, then the cached /employees response), so
        // neither can run on the UI thread — the previous version called the
        // gallery here and swallowed the resulting exception, which is why this
        // always read "name not available".
        kotlin.concurrent.thread {
            val known = kotlin.runCatching {
                com.example.slsHrms.face.FaceGallery.lookupByCode(this, branchId, empCode)
            }.getOrNull()
            val name = known?.second
                ?: com.example.slsHrms.sync.OfflineEmployees.nameFor(this, empCode)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                binding.tvEmpName.text = name ?: "(offline — name not available)"
            }
        }
        binding.tvDeptDesig.text = "Offline — will be verified on upload"
        binding.cardEmployee.visibility = View.VISIBLE
        // Face count is unknown offline; allow the capture and let the server
        // reject a 4th face at upload time.
        updateFaceCount(0, true)
        binding.tvFaceCount.text = "Faces Registered: unknown (offline)"
        Toast.makeText(
            this,
            "Offline — the enrolment is saved on this device and checked by the " +
                "server when it uploads.",
            Toast.LENGTH_LONG
        ).show()
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
        enrollLauncher.launch(Intent(this, FaceEnrollActivity::class.java).apply {
            putExtra(FaceEnrollActivity.EXTRA_BRANCH_ID, branchId)
            putExtra(FaceEnrollActivity.EXTRA_EMP_CODE, currentEmpCode)
            putExtra(FaceEnrollActivity.EXTRA_EMP_NAME, binding.tvEmpName.text.toString())
        })
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
            registerFace(currentEmpCode, base64, capturedTemplate?.toList())
        }
    }

    private fun registerFace(empCode: String, base64: String, embeddingMobile: List<Float>?) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegisterFace.isEnabled = false

        val request = OnBoardingRegisterRequest(
            empCode = empCode,
            faceImage = base64,
            branchId = if (branchId > 0) branchId else null,
            embeddingMobile = embeddingMobile,
            mobileModelVer = if (embeddingMobile != null) MOBILE_MODEL_VER else null
        )
        RetrofitClient.getApiService(this).registerOnBoardingFace(request)
            .enqueue(object : Callback<OnBoardingRegisterResponse> {
                override fun onResponse(call: Call<OnBoardingRegisterResponse>, response: Response<OnBoardingRegisterResponse>) {
                    binding.progressBar.visibility = View.GONE
                    val body = response.body()
                    if (response.isSuccessful && body?.status == "success") {
                        val queued = body.queued == true
                        Toast.makeText(
                            this@OnBoardingActivity,
                            when {
                                queued -> "Saved offline — the enrolment uploads and is " +
                                    "verified when the network returns."
                                else -> body.message ?: "Face registered!"
                            },
                            Toast.LENGTH_LONG
                        ).show()
                        capturedBase64 = null
                        capturedTemplate = null
                        binding.ivFacePreview.setImageResource(R.drawable.ic_person)
                        if (queued) {
                            // No server to re-read the face count from; leave the
                            // card as-is so the operator can enrol the next person.
                            binding.btnRegisterFace.isEnabled = false
                        } else {
                            // Reload employee to refresh count
                            loadEmployee(empCode)
                        }
                    } else {
                        // 4xx bodies are in errorBody(), not body() — e.g. the
                        // server's "No face detected in the image".
                        val msg = body?.message
                            ?: response.errorBody()?.string()?.let { extractErrorMessage(it) }
                            ?: "Failed to register face (HTTP ${response.code()})"
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

