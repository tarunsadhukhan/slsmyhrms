package com.example.slsHrms

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.slsHrms.adapter.EmployeeAdapter
import com.example.slsHrms.api.*
import com.example.slsHrms.databinding.ActivityEmployeeMasterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File

class EmployeeMasterActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EmployeeMaster"
    }

    private lateinit var binding: ActivityEmployeeMasterBinding
    private lateinit var adapter: EmployeeAdapter
    private var allEmployees = mutableListOf<Employee>()

    // Dropdown data
    private var departments = mutableListOf<Department>()
    private var occupations = mutableListOf<Occupation>()
    private var shifts = mutableListOf<Shift>()
    private var selectedCompanyId: Int = 0
    private var selectedBranchId: Int = 0

    // Camera handling
    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private var capturedBase64: String? = null
    private var dialogFaceImageView: ImageView? = null
    private var dialogPhotoStatus: TextView? = null
    private var dialogEmpCode: EditText? = null
    private var dialogEmpName: EditText? = null

    // ── Camera launcher ──────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
            if (bitmap != null) {
                // Resize to save bandwidth
                val scaled = scaleBitmap(bitmap, 480)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                capturedBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

                // Show preview in dialog
                dialogFaceImageView?.setImageBitmap(scaled)
                dialogFaceImageView?.setPadding(0, 0, 0, 0)
                dialogFaceImageView?.imageTintList = null
                dialogPhotoStatus?.text = "✅ Face captured"
                dialogPhotoStatus?.setTextColor(
                    ContextCompat.getColor(this, R.color.login_btn_green)
                )

                // Show API data in msg box before calling
                val baseUrl = ApiConfig.getBaseUrl(this)
                val apiUrl = "${baseUrl}check-face"
                val imageLen = capturedBase64!!.length

                val preview = """
                    |API: POST $apiUrl
                    |
                    |Request Body:
                    |{
                    |  "image": "<base64 $imageLen chars>"
                    |}
                    |
                    |This will check if the face exists
                    |in the employee database.
                    |If matched, emp_code will auto-fill.
                """.trimMargin()

                AlertDialog.Builder(this)
                    .setTitle("📸 Check Face in Database?")
                    .setMessage(preview)
                    .setPositiveButton("Check Now") { _, _ ->
                        dialogPhotoStatus?.text = "🔍 Checking face..."
                        dialogPhotoStatus?.setTextColor(
                            ContextCompat.getColor(this, R.color.hint_text)
                        )
                        checkFaceInDatabase(capturedBase64!!)
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        dialogPhotoStatus?.text = "✅ Face captured (Not checked)"
                        dialogPhotoStatus?.setTextColor(
                            ContextCompat.getColor(this, R.color.login_btn_green)
                        )
                    }
                    .show()
            }
        } else {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Permission launcher ──────────────────────────────────────
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmployeeMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedCompanyId = intent.getIntExtra("CO_ID", 0)
        selectedBranchId  = intent.getIntExtra("BRANCH_ID", 0)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFab()
        loadDropdownData()
        loadEmployees()
    }

    // ── Toolbar ──────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ── RecyclerView ─────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = EmployeeAdapter(
            employees = emptyList(),
            onEdit = { emp -> showAddEditDialog(emp) },
            onDelete = { emp -> showDeleteConfirmation(emp) }
        )
        binding.rvEmployees.layoutManager = LinearLayoutManager(this)
        binding.rvEmployees.adapter = adapter
    }

    // ── Search ───────────────────────────────────────────────────

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String) {
        val filtered = if (query.isEmpty()) {
            allEmployees
        } else {
            allEmployees.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.empCode.contains(query, ignoreCase = true)
            }
        }
        adapter.updateList(filtered)
        updateUI(filtered)
    }

    // ── FAB ──────────────────────────────────────────────────────

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    // ── Load Dropdown Data ───────────────────────────────────────

    private fun loadDropdownData() {
        // Load departments by branch
        RetrofitClient.getApiService(this).getDepartments(
            coId = if (selectedCompanyId > 0) selectedCompanyId else null,
            branchId = if (selectedBranchId > 0) selectedBranchId else null
        ).enqueue(object : Callback<DepartmentResponse> {
                override fun onResponse(call: Call<DepartmentResponse>, response: Response<DepartmentResponse>) {
                    if (response.isSuccessful) {
                        departments.clear()
                        departments.addAll(response.body()?.departments ?: emptyList())
                    }
                }
                override fun onFailure(call: Call<DepartmentResponse>, t: Throwable) {}
            })

        // Load designations by branch (all, dept-specific will be loaded on dept selection)
        if (selectedBranchId > 0) {
            loadDesignationsByBranch(selectedBranchId, null)
        }

        // Load shifts
        RetrofitClient.getApiService(this).getShifts()
            .enqueue(object : Callback<ShiftResponse> {
                override fun onResponse(call: Call<ShiftResponse>, response: Response<ShiftResponse>) {
                    if (response.isSuccessful) {
                        shifts.clear()
                        shifts.addAll(response.body()?.shifts ?: emptyList())
                    }
                }
                override fun onFailure(call: Call<ShiftResponse>, t: Throwable) {}
            })
    }

    private fun loadDesignationsByBranch(branchId: Int, subDeptId: Int?, onDone: (() -> Unit)? = null) {
        RetrofitClient.getApiService(this).getDesignations(
            branchId = branchId,
            subDeptId = subDeptId
        ).enqueue(object : Callback<DesignationResponse> {
            override fun onResponse(call: Call<DesignationResponse>, response: Response<DesignationResponse>) {
                if (response.isSuccessful) {
                    occupations.clear()
                    occupations.addAll(
                        (response.body()?.designations ?: emptyList()).map { Occupation(it.id, it.name) }
                    )
                    onDone?.invoke()
                }
            }
            override fun onFailure(call: Call<DesignationResponse>, t: Throwable) {}
        })
    }

    // ── Load Employees from API ──────────────────────────────────

    private fun loadEmployees() {
        showLoading(true)

        RetrofitClient.getApiService(this).getEmployees()
            .enqueue(object : Callback<EmployeeResponse> {
                override fun onResponse(call: Call<EmployeeResponse>, response: Response<EmployeeResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val data = response.body()?.employees ?: emptyList()
                        allEmployees.clear()
                        allEmployees.addAll(data)
                        adapter.updateList(allEmployees)
                        updateUI(allEmployees)
                    } else {
                        Toast.makeText(this@EmployeeMasterActivity, "Failed to load employees", Toast.LENGTH_SHORT).show()
                        updateUI(emptyList())
                    }
                }

                override fun onFailure(call: Call<EmployeeResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@EmployeeMasterActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    updateUI(emptyList())
                }
            })
    }

    // ── Add / Edit Dialog ────────────────────────────────────────

    private fun showAddEditDialog(employee: Employee?) {
        val isEdit = employee != null
        capturedBase64 = null

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_employee, null)

        val etEmpCode = dialogView.findViewById<EditText>(R.id.etEmpCode)
        val etEmpName = dialogView.findViewById<EditText>(R.id.etEmpName)
        val spinnerDept = dialogView.findViewById<Spinner>(R.id.spinnerDepartment)
        val spinnerDesig = dialogView.findViewById<Spinner>(R.id.spinnerDesignation)
        val spinnerShift = dialogView.findViewById<Spinner>(R.id.spinnerShift)
        val ivFacePhoto = dialogView.findViewById<ImageView>(R.id.ivFacePhoto)
        val btnCapture = dialogView.findViewById<ImageView>(R.id.btnCapturePhoto)
        val tvPhotoStatus = dialogView.findViewById<TextView>(R.id.tvPhotoStatus)

        // Keep references for camera callback
        dialogFaceImageView = ivFacePhoto
        dialogPhotoStatus = tvPhotoStatus
        dialogEmpCode = etEmpCode
        dialogEmpName = etEmpName

        // Setup department spinner
        val deptNames = mutableListOf("Select Department")
        deptNames.addAll(departments.map { it.name })
        spinnerDept.adapter = ArrayAdapter(this, R.layout.spinner_item_black, deptNames).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        }

        // When dept changes, reload designations for that dept+branch
        spinnerDept.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val subDeptId = if (position > 0) departments[position - 1].id else null
                if (selectedBranchId > 0) {
                    loadDesignationsByBranch(selectedBranchId, subDeptId) {
                        // Refresh designation spinner after reload
                        val desigNames = mutableListOf("Select Designation")
                        desigNames.addAll(occupations.map { it.name })
                        spinnerDesig.adapter = ArrayAdapter(this@EmployeeMasterActivity, R.layout.spinner_item_black, desigNames).apply {
                            setDropDownViewResource(R.layout.spinner_dropdown_item_black)
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Setup designation spinner
        val desigNames = mutableListOf("Select Designation")
        desigNames.addAll(occupations.map { it.name })
        spinnerDesig.adapter = ArrayAdapter(this, R.layout.spinner_item_black, desigNames).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        }

        // Setup shift spinner
        val shiftNames = mutableListOf("Select Shift")
        shiftNames.addAll(shifts.map { it.name })
        spinnerShift.adapter = ArrayAdapter(this, R.layout.spinner_item_black, shiftNames).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        }

        // Pre-fill for edit
        if (isEdit) {
            etEmpCode.setText(employee!!.empCode)
            etEmpName.setText(employee.name)

            // Select correct department
            val deptIdx = departments.indexOfFirst { it.id == employee.departmentId }
            if (deptIdx >= 0) spinnerDept.setSelection(deptIdx + 1)

            // Select correct designation
            val desigIdx = occupations.indexOfFirst { it.id == employee.designationId }
            if (desigIdx >= 0) spinnerDesig.setSelection(desigIdx + 1)

            // Select correct shift
            val shiftIdx = shifts.indexOfFirst { it.id == employee.shiftId }
            if (shiftIdx >= 0) spinnerShift.setSelection(shiftIdx + 1)

            // Show existing photo from photo_base64 (server extracts from photo_html)
            if (!employee.photoBase64.isNullOrBlank()) {
                try {
                    val bytes = Base64.decode(employee.photoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ivFacePhoto.setImageBitmap(bitmap)
                    ivFacePhoto.setPadding(0, 0, 0, 0)
                    ivFacePhoto.imageTintList = null
                    tvPhotoStatus.text = "✅ Face registered"
                    tvPhotoStatus.setTextColor(ContextCompat.getColor(this, R.color.login_btn_green))
                    capturedBase64 = employee.photoBase64
                } catch (e: Exception) { /* ignore */ }
            }
        }

        // Camera button
        btnCapture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Also allow clicking on the photo itself
        ivFacePhoto.setOnClickListener {
            btnCapture.performClick()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Employee" else "Add Employee")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Update" else "Save", null)  // set null to handle manually
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val empCode = etEmpCode.text.toString().trim()
                val empName = etEmpName.text.toString().trim()
                val deptPos = spinnerDept.selectedItemPosition
                val desigPos = spinnerDesig.selectedItemPosition
                val shiftPos = spinnerShift.selectedItemPosition

                // Validation
                if (empCode.isEmpty()) {
                    etEmpCode.error = "Enter employee code"
                    etEmpCode.requestFocus()
                    return@setOnClickListener
                }
                if (empName.isEmpty()) {
                    etEmpName.error = "Enter employee name"
                    etEmpName.requestFocus()
                    return@setOnClickListener
                }
                if (capturedBase64 == null) {
                    Toast.makeText(this@EmployeeMasterActivity, "Please capture face photo first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val deptId = if (deptPos > 0) departments[deptPos - 1].id else null
                val desigId = if (desigPos > 0) occupations[desigPos - 1].id else null
                val shiftId = if (shiftPos > 0) shifts[shiftPos - 1].id else null

                val request = AddEmployeeRequest(
                    empCode = empCode,
                    name = empName,
                    departmentId = deptId,
                    designationId = desigId,
                    shiftId = shiftId,
                    faceImage = capturedBase64
                )

                // ── Show data in a confirmation dialog before calling API ──
                val deptName = if (deptPos > 0) spinnerDept.selectedItem.toString() else "None"
                val desigName = if (desigPos > 0) spinnerDesig.selectedItem.toString() else "None"
                val shiftName = if (shiftPos > 0) spinnerShift.selectedItem.toString() else "None"
                val hasFace = capturedBase64 != null

                val baseUrl = com.example.slsHrms.api.ApiConfig.getBaseUrl(this@EmployeeMasterActivity)
                val apiUrl = if (isEdit) "${baseUrl}employees/${employee!!.id}" else "${baseUrl}register"
                val method = if (isEdit) "PUT" else "POST"

                val jsonPreview = """
                    |API: $method $apiUrl
                    |
                    |{
                    |  "emp_code": "$empCode",
                    |  "name": "$empName",
                    |  "department_id": $deptId,
                    |  "designation_id": $desigId,
                    |  "shift_id": $shiftId,
                    |  "image": ${if (hasFace) "\"<base64 ${capturedBase64!!.length} chars>\"" else "null"}
                    |}
                    |
                    |Department : $deptName
                    |Designation: $desigName
                    |Shift      : $shiftName
                    |Face Photo : ${if (hasFace) "✅ Yes" else "❌ No"}
                """.trimMargin()

                AlertDialog.Builder(this@EmployeeMasterActivity)
                    .setTitle("📤 Data to Send")
                    .setMessage(jsonPreview)
                    .setPositiveButton("Send") { _, _ ->
                        if (isEdit) {
                            updateEmployee(employee!!.id, request, dialog)
                        } else {
                            addEmployee(request, dialog)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        dialog.show()
    }

    // ── Add Employee API ─────────────────────────────────────────

    private fun addEmployee(request: AddEmployeeRequest, dialog: AlertDialog) {
        showLoading(true)

        val call = RetrofitClient.getApiService(this).addEmployee(request)

        Log.d(TAG, "📤 Calling API: ${call.request().method} ${call.request().url}")

        call.enqueue(object : Callback<AddEmployeeResponse> {
                override fun onResponse(call: Call<AddEmployeeResponse>, response: Response<AddEmployeeResponse>) {
                    showLoading(false)

                    val url = call.request().url.toString()
                    val httpCode = response.code()

                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@EmployeeMasterActivity,
                            response.body()?.message ?: "Employee added!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadEmployees()
                    } else {
                        // Read the full error body
                        val errorBody = try {
                            response.errorBody()?.string() ?: response.body()?.message ?: "Unknown error"
                        } catch (e: Exception) {
                            "Could not read error body"
                        }

                        val errorDetail = """
                            |❌ API Error
                            |
                            |URL: $url
                            |HTTP Status: $httpCode
                            |Method: ${call.request().method}
                            |
                            |Response Body:
                            |$errorBody
                        """.trimMargin()

                        Log.e(TAG, errorDetail)

                        // Show full error in a message box
                        AlertDialog.Builder(this@EmployeeMasterActivity)
                            .setTitle("❌ Error $httpCode")
                            .setMessage(errorDetail)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }

                override fun onFailure(call: Call<AddEmployeeResponse>, t: Throwable) {
                    showLoading(false)

                    val url = call.request().url.toString()

                    val errorDetail = """
                        |❌ Network Error
                        |
                        |URL: $url
                        |Method: ${call.request().method}
                        |
                        |Error: ${t.javaClass.simpleName}
                        |Message: ${t.localizedMessage}
                    """.trimMargin()

                    Log.e(TAG, errorDetail, t)

                    // Show full error in a message box
                    AlertDialog.Builder(this@EmployeeMasterActivity)
                        .setTitle("❌ Network Error")
                        .setMessage(errorDetail)
                        .setPositiveButton("OK", null)
                        .show()
                }
            })
    }

    // ── Update Employee API ──────────────────────────────────────

    private fun updateEmployee(id: Int, request: AddEmployeeRequest, dialog: AlertDialog) {
        showLoading(true)

        RetrofitClient.getApiService(this).updateEmployee(id, request)
            .enqueue(object : Callback<AddEmployeeResponse> {
                override fun onResponse(call: Call<AddEmployeeResponse>, response: Response<AddEmployeeResponse>) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@EmployeeMasterActivity,
                            response.body()?.message ?: "Employee updated!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadEmployees()
                    } else {
                        val errorMsg = response.body()?.message ?: "Failed to update employee"
                        Toast.makeText(this@EmployeeMasterActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AddEmployeeResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@EmployeeMasterActivity,
                        "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Delete Confirmation ──────────────────────────────────────

    private fun showDeleteConfirmation(emp: Employee) {
        AlertDialog.Builder(this)
            .setTitle("Delete Employee")
            .setMessage("Are you sure you want to delete '${emp.name}' (${emp.empCode})?")
            .setPositiveButton("Delete") { _, _ ->
                deleteEmployee(emp)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEmployee(emp: Employee) {
        showLoading(true)

        RetrofitClient.getApiService(this).deleteEmployee(emp.id)
            .enqueue(object : Callback<AddEmployeeResponse> {
                override fun onResponse(call: Call<AddEmployeeResponse>, response: Response<AddEmployeeResponse>) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@EmployeeMasterActivity,
                            "'${emp.name}' deleted", Toast.LENGTH_SHORT).show()
                        loadEmployees()
                    } else {
                        Toast.makeText(this@EmployeeMasterActivity,
                            "Failed to delete employee", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<AddEmployeeResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@EmployeeMasterActivity,
                        "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Camera ───────────────────────────────────────────────────

    private fun openCamera() {
        val photosDir = File(cacheDir, "photos")
        photosDir.mkdirs()
        photoFile = File(photosDir, "emp_face_${System.currentTimeMillis()}.jpg")

        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile!!
        )

        cameraLauncher.launch(photoUri)
    }

    // ── Check Face in Database ─────────────────────────────────

    private fun checkFaceInDatabase(base64Image: String) {
        val request = FaceRecognitionRequest(image = base64Image)
        val call = RetrofitClient.getApiService(this).checkFace(request)

        Log.d(TAG, "📤 Checking face: POST ${call.request().url}")

        call.enqueue(object : Callback<FaceRecognitionResponse> {
            override fun onResponse(
                call: Call<FaceRecognitionResponse>,
                response: Response<FaceRecognitionResponse>
            ) {
                val httpCode = response.code()
                val url = call.request().url.toString()

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null && result.status == "success") {
                        // Face MATCHED — auto-fill emp_code and name
                        dialogEmpCode?.setText(result.empCode ?: "")
                        dialogPhotoStatus?.text = "✅ Face matched: ${result.empName} (${result.empCode})"
                        dialogPhotoStatus?.setTextColor(
                            ContextCompat.getColor(this@EmployeeMasterActivity, R.color.login_btn_green)
                        )

                        AlertDialog.Builder(this@EmployeeMasterActivity)
                            .setTitle("✅ Face Found in Database")
                            .setMessage(
                                "Employee: ${result.empName}\n" +
                                "Emp Code: ${result.empCode}\n\n" +
                                "Emp code has been auto-filled."
                            )
                            .setPositiveButton("OK", null)
                            .show()

                    } else {
                        // Face not matched — new employee
                        dialogPhotoStatus?.text = "✅ Face captured (New employee)"
                        dialogPhotoStatus?.setTextColor(
                            ContextCompat.getColor(this@EmployeeMasterActivity, R.color.login_btn_green)
                        )
                    }
                } else {
                    // HTTP error — read error body
                    val errorBody = try {
                        response.errorBody()?.string() ?: "Unknown error"
                    } catch (e: Exception) { "Could not read error" }

                    when (httpCode) {
                        400 -> {
                            // No face detected in image
                            dialogPhotoStatus?.text = "⚠️ No face detected! Retake photo"
                            dialogPhotoStatus?.setTextColor(
                                ContextCompat.getColor(this@EmployeeMasterActivity, android.R.color.holo_red_dark)
                            )
                            capturedBase64 = null  // Clear so user must retake

                            AlertDialog.Builder(this@EmployeeMasterActivity)
                                .setTitle("⚠️ No Face Detected")
                                .setMessage("API: $url\nHTTP: $httpCode\n\n$errorBody\n\nPlease retake the photo with face clearly visible.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        401 -> {
                            // Face not recognized — new employee, that's OK
                            dialogPhotoStatus?.text = "✅ Face captured (New employee)"
                            dialogPhotoStatus?.setTextColor(
                                ContextCompat.getColor(this@EmployeeMasterActivity, R.color.login_btn_green)
                            )
                        }
                        404 -> {
                            // No employees registered
                            dialogPhotoStatus?.text = "✅ Face captured (No employees yet)"
                            dialogPhotoStatus?.setTextColor(
                                ContextCompat.getColor(this@EmployeeMasterActivity, R.color.login_btn_green)
                            )
                        }
                        else -> {
                            dialogPhotoStatus?.text = "⚠️ API Error ($httpCode)"
                            dialogPhotoStatus?.setTextColor(
                                ContextCompat.getColor(this@EmployeeMasterActivity, android.R.color.holo_red_dark)
                            )

                            AlertDialog.Builder(this@EmployeeMasterActivity)
                                .setTitle("❌ Error $httpCode")
                                .setMessage("API: $url\nHTTP: $httpCode\n\n$errorBody")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<FaceRecognitionResponse>, t: Throwable) {
                val url = call.request().url.toString()
                dialogPhotoStatus?.text = "⚠️ Network error"
                dialogPhotoStatus?.setTextColor(
                    ContextCompat.getColor(this@EmployeeMasterActivity, android.R.color.holo_red_dark)
                )

                AlertDialog.Builder(this@EmployeeMasterActivity)
                    .setTitle("❌ Network Error")
                    .setMessage(
                        "URL: $url\n\n" +
                        "Error: ${t.javaClass.simpleName}\n" +
                        "Message: ${t.localizedMessage}\n\n" +
                        "Check if API server is running and reachable."
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        })
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    private fun updateUI(list: List<Employee>) {
        binding.tvCount.text = "Total: ${list.size}"
        if (list.isEmpty()) {
            binding.rvEmployees.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvEmployees.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Extract base64 image data from an HTML img tag like:
     * <img src="data:image/jpeg;base64,/9j/4AA..." />
     */
    private fun extractBase64FromHtml(html: String): String? {
        val regex = Regex("""base64,([^"<\s]+)""")
        val match = regex.find(html)
        return match?.groupValues?.get(1)?.trim()
    }
}

