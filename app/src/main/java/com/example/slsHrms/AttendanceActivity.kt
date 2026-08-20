package com.example.slsHrms

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.slsHrms.api.Department
import com.example.slsHrms.api.DepartmentResponse
import com.example.slsHrms.api.DesignationResponse
import com.example.slsHrms.api.Employee
import com.example.slsHrms.api.EmployeeResponse
import com.example.slsHrms.api.FaceRecognitionRequest
import com.example.slsHrms.api.FaceRecognitionResponse
import com.example.slsHrms.api.Occupation
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.api.Shift
import com.example.slsHrms.api.ShiftResponse
import com.example.slsHrms.databinding.ActivityAttendanceBinding
import com.example.slsHrms.face.FaceGallery
import com.example.slsHrms.sync.Connectivity
import com.example.slsHrms.sync.OfflineEmployees
import com.example.slsHrms.sync.OfflinePhotoStore
import com.example.slsHrms.sync.SyncEngine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.util.Base64 as AndroidBase64
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceBinding
    private val calendar = Calendar.getInstance()

    private var departments = mutableListOf<Department>()
    private var shifts = mutableListOf<Shift>()
    private var occupations = mutableListOf<Occupation>()
    private var selectedTab = "Regular"
    private var isEmployeeVerified = false
    // True when the current employee identity was established via the live
    // face-validate button (4th button). Such attendance is saved as "Face".
    // Reset whenever the employee code changes by any other means.
    private var faceValidated = false
    // Set when the identity came from the ON-DEVICE matcher (no network). The
    // capture and its score travel with the record so the server can re-run
    // dlib on it; the file is deleted as soon as the record uploads.
    private var matchedOffline = false
    private var offlinePhotoPath: String? = null
    private var offlineConfidence: Double = 0.0
    // Leave state (vw_leave_dates) for the verified employee on the selected
    // date. leavePayable: null = not on leave, "Y" = paid leave (only Cash/OT
    // allowed), anything else = unpaid leave (Submit disabled).
    private var verifiedEbId: Int? = null
    private var leavePayable: String? = null
    private var selectedCompanyId: Int = 0
    private var selectedBranchId: Int = 0

    // Device geo-location ("latitude,longitude") kept fresh while this screen is
    // open and attached to the attendance record when Submit is pressed. Empty
    // string until the first GPS fix arrives; attendance is never blocked on it.
    private var capturedGeoLocation: String = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // Camera photo file
    private var photoFile: File? = null
    private var photoUri: Uri? = null
    private var capturedBase64: String? = null  // held in memory until Submit

    // Machine selection
    private var machines = mutableListOf<com.example.slsHrms.api.Machine>()
    private var selectedMachineIds = mutableSetOf<Int>()
    // Designation the current selectedMachineIds belong to. Machines are
    // designation-specific (mech_occu_link), so when the designation changes
    // we drop the previously-picked machines.
    private var machinesForDesignationId = 0

    // Pending defaults from face match (auto-fill after spinners load)
    private var pendingDesignationId: Int? = null
    private var pendingMachineIds: List<Int>? = null

    // Last-worked defaults from the most recent employee-code lookup. Applied to
    // the designation + machine fields whenever the selected department matches
    // lastWorkedDeptId — regardless of whether the department is chosen before or
    // after the employee code is validated. Cleared when the code is edited.
    private var lastWorkedDeptId: Int? = null
    private var lastWorkedDesigId: Int? = null
    private var lastWorkedMachineIds: List<Int>? = null

    // ── Camera launcher ──────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            // Show progress immediately — decoding happens off the UI thread
            // and the network call follows.
            binding.progressBar.visibility = View.VISIBLE
            binding.btnCamera.isEnabled = false

            Thread {
                val b64 = try {
                    loadResizedJpegBase64(photoFile!!)
                } catch (e: Exception) {
                    android.util.Log.e("PHOTO_DEBUG", "resize failed, falling back to raw", e)
                    AndroidBase64.encodeToString(photoFile!!.readBytes(), AndroidBase64.NO_WRAP)
                }
                runOnUiThread {
                    capturedBase64 = b64
                    identifyFace(capturedBase64!!)
                }
            }.start()
        } else {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Decode the camera JPEG into a bitmap no larger than [maxDim] on its
     * longest side, fix orientation from EXIF, re-encode as JPEG quality
     * [quality], and return the base64 string. A typical 4032×3024 / 4-MB
     * camera frame becomes ~80-120 KB after this — round-trip drops from
     * ~15 s to ~1 s on a slow LAN, and `face_encodings` on the server
     * finishes in ~0.3 s instead of ~3 s.
     */
    private fun loadResizedJpegBase64(file: File, maxDim: Int = 720, quality: Int = 80): String {
        val path = file.absolutePath

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight)

        var sample = 1
        while (longest / (sample * 2) >= maxDim) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = BitmapFactory.decodeFile(path, opts)
            ?: throw IllegalStateException("Bitmap decode failed for $path")

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

        val orientation = ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotationDeg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotationDeg != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDeg) }
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

    // ── Live face-validate launcher ──────────────────────────────
    // Opens FaceValidateActivity (live camera + auto detection). On a match it
    // returns the employee code + name; we fill the code field and run the
    // normal lookup so the photo/info card populates exactly like a manual ✓.
    private val faceValidateLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val empCode = result.data?.getStringExtra(FaceValidateActivity.EXTRA_EMP_CODE).orEmpty()
            val empName = result.data?.getStringExtra(FaceValidateActivity.EXTRA_EMP_NAME).orEmpty()
            if (empCode.isNotEmpty()) {
                binding.etEmployeeCode.setText(empCode)
                if (empName.isNotEmpty()) {
                    binding.tvEmployeeName.text = "Employee: $empName"
                    binding.cardEmployeeInfo.visibility = View.VISIBLE
                }
                // Enrich with photo + defaults and confirm verification.
                lookupEmployeeByCode(empCode)
                // Mark this as a live face-validated identity (4th button) AFTER
                // setText above, whose TextWatcher would otherwise clear the flag.
                // Attendance submitted now is recorded with source "Face".
                faceValidated = true

                // An offline match is provisional: carry the capture and the
                // score through to submit so the server can re-verify with dlib.
                OfflinePhotoStore.delete(offlinePhotoPath)
                matchedOffline = result.data?.getBooleanExtra(
                    FaceValidateActivity.EXTRA_MATCHED_OFFLINE, false
                ) == true
                offlinePhotoPath =
                    result.data?.getStringExtra(FaceValidateActivity.EXTRA_PHOTO_PATH)
                offlineConfidence =
                    result.data?.getDoubleExtra(FaceValidateActivity.EXTRA_CONFIDENCE, 0.0) ?: 0.0
                if (matchedOffline) {
                    binding.tvEmployeeName.text =
                        "Employee: $empName  ·  face matched on device, server re-verifies"
                }
            }
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

    // ── Location permission launcher ─────────────────────────────
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedCompanyId = intent.getIntExtra("CO_ID", 0)
        selectedBranchId  = intent.getIntExtra("BRANCH_ID", 0)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        ensureLocationPermission()

        setupToolbar()
        setupDate()
        setupTabs()
        setupOccupationSpinner()
        setupMachineSelector()
        loadDepartments()
        loadShifts()
        setupCameraButton()
        setupCheckButton()
        setupSearchButton()
        setupFaceValidateButton()
        setupSubmitButton()
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    // ── Geo-location ─────────────────────────────────────────────
    // Keeps capturedGeoLocation updated with the latest "lat,long" fix so it can
    // be attached to the attendance record on Submit. Never blocks attendance.

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureLocationPermission() {
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        // Seed immediately with the last known fix so we have something on a
        // quick Submit, then keep refreshing.
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) capturedGeoLocation = formatLocation(loc)
            }
        } catch (e: SecurityException) {
            android.util.Log.w("GEO_DEBUG", "lastLocation denied", e)
        }

        if (locationCallback != null) return  // already running

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { capturedGeoLocation = formatLocation(it) }
            }
        }
        locationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            android.util.Log.w("GEO_DEBUG", "requestLocationUpdates denied", e)
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun formatLocation(loc: Location): String =
        String.format(Locale.US, "%.6f,%.6f", loc.latitude, loc.longitude)

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    // ── Date Picker ──────────────────────────────────────────────

    private fun setupDate() {
        updateDateDisplay()

        binding.tvDate.setOnClickListener {
            showDatePicker()
        }
        // Also allow click on the parent FrameLayout
        (binding.tvDate.parent as? View)?.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                updateDateDisplay()
                // Leave is per-date — re-check for the newly picked date.
                checkLeaveStatus()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        // Disable all future dates
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun updateDateDisplay() {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val suffix = getDaySuffix(day)
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yy", Locale.getDefault())
        val month = monthFormat.format(calendar.time)
        val year = yearFormat.format(calendar.time)
        binding.tvDate.text = "${day}${suffix} ${month}' ${year}"
    }

    private fun getDaySuffix(day: Int): String {
        return when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
    }

    // ── Tabs (Regular / OT / Cash) ───────────────────────────────

    private fun setupTabs() {
        val tabs = listOf(binding.tabRegular, binding.tabOT, binding.tabCash)
        val tabNames = listOf("Regular", "OT", "Cash")

        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                selectedTab = tabNames[index]
                tabs.forEach { t ->
                    t.setBackgroundResource(R.drawable.bg_tab_unselected)
                    t.setTextColor(resources.getColor(R.color.hint_text, theme))
                    t.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
                tab.setTextColor(resources.getColor(R.color.label_text, theme))
                tab.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }

    // ── Departments from API ─────────────────────────────────────

    private fun loadDepartments() {
        RetrofitClient.getApiService(this).getDepartments(
            coId = if (selectedCompanyId > 0) selectedCompanyId else null,
            branchId = if (selectedBranchId > 0) selectedBranchId else null
        ).enqueue(object : Callback<DepartmentResponse> {
            override fun onResponse(call: Call<DepartmentResponse>, response: Response<DepartmentResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.departments ?: emptyList()
                    departments.clear()
                    departments.addAll(data)

                    val items = mutableListOf("Select Department")
                    items.addAll(data.map { it.name })

                    val adapter = ArrayAdapter(
                        this@AttendanceActivity,
                        R.layout.spinner_item_black,
                        items
                    )
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
                    binding.spinnerDepartment.adapter = adapter

                    // When department changes, reload designations for that dept+branch
                    binding.spinnerDepartment.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                            val subDeptId = if (position > 0) departments[position - 1].id else null
                            if (selectedBranchId > 0) {
                                // If this department is where the employee last worked, carry
                                // the last-worked designation + machines into the reload via
                                // the pending path so the designation defaults correctly.
                                if (subDeptId != null && lastWorkedDeptId != null && subDeptId == lastWorkedDeptId) {
                                    pendingDesignationId = lastWorkedDesigId
                                    pendingMachineIds    = lastWorkedMachineIds
                                }
                                loadDesignations(selectedBranchId, subDeptId)
                            }
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }

                    // Load all designations for branch initially
                    if (selectedBranchId > 0) loadDesignations(selectedBranchId, null)

                } else {
                    Toast.makeText(this@AttendanceActivity, "Failed to load departments", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DepartmentResponse>, t: Throwable) {
                Toast.makeText(this@AttendanceActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Shifts from API ──────────────────────────────────────────

    private fun loadShifts() {
        RetrofitClient.getApiService(this).getShifts(
            branchId = if (selectedBranchId > 0) selectedBranchId else null
        ).enqueue(object : Callback<ShiftResponse> {
            override fun onResponse(call: Call<ShiftResponse>, response: Response<ShiftResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.shifts ?: emptyList()
                    shifts.clear()
                    shifts.addAll(data)

                    val items = data.map { it.name }.toMutableList()
                    if (items.isEmpty()) items.add("No shifts")

                    val adapter = ArrayAdapter(
                        this@AttendanceActivity,
                        R.layout.spinner_item_black,
                        items
                    )
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
                    binding.spinnerShift.adapter = adapter
                    
                    // Set up listener to update shift hours and working hours when shift is selected
                    binding.spinnerShift.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                            if (position >= 0 && position < shifts.size) {
                                val selectedShift = shifts[position]
                                val shiftHours = selectedShift.shiftHours ?: 0.0
                                binding.etShiftHours.setText(String.format(Locale.getDefault(), "%.0f", shiftHours))
                                // Auto-fill working hours to match shift hours
                                binding.etWorkingHours.setText(String.format(Locale.getDefault(), "%.0f", shiftHours))
                            }
                        }
                        
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                            // Do nothing
                        }
                    }
                    
                    // Automatically populate shift hours and working hours for the first shift (default selection)
                    if (shifts.isNotEmpty()) {
                        val firstShift = shifts[0]
                        val shiftHours = firstShift.shiftHours ?: 0.0
                        binding.etShiftHours.setText(String.format(Locale.getDefault(), "%.0f", shiftHours))
                        // Auto-fill working hours to match shift hours
                        binding.etWorkingHours.setText(String.format(Locale.getDefault(), "%.0f", shiftHours))
                    }
                } else {
                    Toast.makeText(this@AttendanceActivity, "Failed to load shifts", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ShiftResponse>, t: Throwable) {
                Toast.makeText(this@AttendanceActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Occupation (default placeholder) ─────────────────────────

    private fun setupOccupationSpinner() {
        val items = listOf("Select Designation")
        val adapter = ArrayAdapter(this, R.layout.spinner_item_black, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        binding.spinnerOccupation.adapter = adapter

        // Changing the designation drops any machines picked for the previous
        // one — they're linked to that designation and won't be valid here.
        binding.spinnerOccupation.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val desigId = if (position > 0 && occupations.size >= position) occupations[position - 1].id else 0
                    if (desigId != machinesForDesignationId) {
                        if (selectedMachineIds.isNotEmpty()) {
                            selectedMachineIds.clear()
                            machines.clear()
                            updateMachineDisplay()
                        }
                        machinesForDesignationId = desigId
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
    }

    // ── Designations from designation_mst ───────────────────────

    private fun loadDesignations(branchId: Int, subDeptId: Int?) {
        RetrofitClient.getApiService(this).getDesignations(
            branchId = branchId,
            subDeptId = subDeptId
        ).enqueue(object : Callback<DesignationResponse> {
            override fun onResponse(call: Call<DesignationResponse>, response: Response<DesignationResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.designations ?: emptyList()
                    occupations.clear()
                    occupations.addAll(data.map { Occupation(it.id, it.name) })

                    val items = mutableListOf("Select Designation")
                    items.addAll(data.map { it.name })

                    val adapter = ArrayAdapter(
                        this@AttendanceActivity,
                        R.layout.spinner_item_black,
                        items
                    )
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
                    binding.spinnerOccupation.adapter = adapter

                    // ── Apply pending designation default from face-match ──
                    pendingDesignationId?.let { desigId ->
                        val idx = occupations.indexOfFirst { it.id == desigId }
                        if (idx >= 0) {
                            // Select + load machines (posted so it survives the adapter swap).
                            selectDesignation(idx + 1, desigId)
                        }
                        pendingDesignationId = null
                    }
                } else {
                    Toast.makeText(this@AttendanceActivity, "Failed to load designations", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DesignationResponse>, t: Throwable) {
                Toast.makeText(this@AttendanceActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @Suppress("unused")
    private fun loadOccupations() { /* replaced by loadDesignations */ }

    // ── Check Button (tick) — lookup employee by code ──────────

    private fun setupCheckButton() {
        // Reset verification when employee code is manually changed
        binding.etEmployeeCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isEmployeeVerified = false
                // Editing the code by hand drops any prior live face-validation.
                faceValidated = false
                // ...and any leave restriction for the previous employee.
                verifiedEbId = null
                leavePayable = null
                binding.btnSubmit.isEnabled = true
                // ...and any last-worked defaults from the previous employee.
                lastWorkedDeptId = null
                lastWorkedDesigId = null
                lastWorkedMachineIds = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Auto-lookup when employee code field loses focus
        binding.etEmployeeCode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val empCode = binding.etEmployeeCode.text.toString().trim()
                if (empCode.isNotEmpty() && !isEmployeeVerified) {
                    lookupEmployeeByCode(empCode)
                }
            }
        }

        // Manual lookup via tick button
        binding.btnCheck.setOnClickListener {
            val empCode = binding.etEmployeeCode.text.toString().trim()
            if (empCode.isEmpty()) {
                binding.etEmployeeCode.error = "Enter employee code first"
                binding.etEmployeeCode.requestFocus()
                return@setOnClickListener
            }
            lookupEmployeeByCode(empCode)
        }
    }

    private fun lookupEmployeeByCode(empCode: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCheck.isEnabled = false
        // Reset leave state — a failed lookup must not keep the previous
        // employee's leave restriction (or lack of it).
        verifiedEbId = null
        leavePayable = null
        binding.btnSubmit.isEnabled = true

        // No network: the lookup endpoint cannot answer and blocking Submit here
        // would stop the shop floor. Accept the code provisionally — the server
        // rejects an unknown employee at sync time and the row lands in the Sync
        // Center as a CONFLICT, which is visible instead of lost.
        if (!Connectivity.isOnline(this)) {
            verifyOffline(empCode)
            return
        }

        RetrofitClient.getApiService(this).getEmployeeByCode(
            empCode = empCode,
            branchId = if (selectedBranchId > 0) selectedBranchId else null
        )
            .enqueue(object : Callback<FaceRecognitionResponse> {
                override fun onResponse(
                    call: Call<FaceRecognitionResponse>,
                    response: Response<FaceRecognitionResponse>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCheck.isEnabled = true

                    if (response.isSuccessful) {
                        val result = response.body()
                        android.util.Log.d("PHOTO_DEBUG", "employee lookup response: status=${result?.status}, empName=${result?.empName}, photoHtml=${if (result?.photoHtml.isNullOrEmpty()) "NULL/EMPTY" else "${result?.photoHtml?.length} chars"}")

                        if (result != null && result.status == "success") {
                            isEmployeeVerified = true
                            verifiedEbId = result.ebId
                            checkLeaveStatus()
                            val name = result.empName ?: ""
                            if (name.isNotEmpty()) {
                                binding.tvEmployeeName.text = "Employee: $name"
                                binding.cardEmployeeInfo.visibility = View.VISIBLE
                            }
                            // Show photo from photo_html
                            showEmployeePhoto(result.photoHtml)

                            // Pre-fill last-worked designation + machines when the
                            // selected department matches where the employee last worked.
                            applyLastWorkedDefaults(result)

                            Toast.makeText(
                                this@AttendanceActivity,
                                "Employee found: $name",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            isEmployeeVerified = false
                            binding.tvEmployeeName.text = "⚠ Employee not found"
                            binding.cardEmployeeInfo.visibility = View.VISIBLE
                            binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
                            Toast.makeText(
                                this@AttendanceActivity,
                                result?.message ?: "Employee not found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (response.code() == 504) {
                        // OkHttp's synthetic "unsatisfiable cache request": we are
                        // effectively offline with nothing cached for this code.
                        // Not the same as "no such employee".
                        verifyOffline(empCode)
                    } else {
                        // 4xx bodies live in errorBody(): a 403 here carries the
                        // reason ("… is in HR status OPEN — not eligible"), which
                        // matters when the face was just recognised on the device.
                        val reason = response.errorBody()?.string()?.let { extractErrorMessage(it) }
                            ?.takeIf { it.isNotBlank() && !it.trimStart().startsWith("<") }
                        isEmployeeVerified = false
                        binding.tvEmployeeName.text = when {
                            reason != null -> "⚠ $reason"
                            faceValidated -> "⚠ $empCode recognised by face but not accepted by the server for attendance"
                            else -> "⚠ Employee not found"
                        }
                        binding.cardEmployeeInfo.visibility = View.VISIBLE
                        binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
                        Toast.makeText(
                            this@AttendanceActivity,
                            reason ?: "Employee code not found",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FaceRecognitionResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCheck.isEnabled = true
                    // The server went away between the check and now — same
                    // reasoning as the offline branch above.
                    verifyOffline(empCode)
                }
            })
    }

    /**
     * Provisional verification with no server. The cached face gallery doubles as
     * an offline employee directory for everyone with an enrolled face; anyone
     * else is accepted on the operator's word and validated on upload.
     */
    private fun verifyOffline(empCode: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnCheck.isEnabled = true
        isEmployeeVerified = true
        // Leave status needs the server; offline we cannot know, so no
        // restriction is applied and the server re-checks on save.
        leavePayable = null

        binding.tvEmployeeName.text = "Employee: $empCode  ·  offline"
        binding.cardEmployeeInfo.visibility = View.VISIBLE
        binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
        Toast.makeText(this, "Offline — will be verified when the network returns",
            Toast.LENGTH_SHORT).show()

        // The gallery lookup touches the local DB, so it cannot run here.
        kotlin.concurrent.thread {
            val known = FaceGallery.lookupByCode(this, selectedBranchId, empCode)
            // The face gallery only holds people whose mobile embedding has been
            // backfilled server-side, so it is empty until that job runs. The
            // downloaded employee list is the directory that is actually there,
            // and it is what turns this screen from "accept anything" into a
            // real offline check.
            val directory = if (known == null) OfflineEmployees.directory(this) else null
            val name = known?.second ?: directory?.get(empCode.trim().uppercase())
            // Enrolment-photo thumbnail from the gallery — the offline stand-in
            // for the photo_html the server shows online.
            val photo = if (known != null) FaceGallery.thumbFor(this, selectedBranchId, empCode)
                ?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) } else null

            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                photo?.let { binding.ivEmployeePhoto.setImageBitmap(it) }
                when {
                    name != null -> {
                        known?.let { verifiedEbId = it.first }
                        binding.tvEmployeeName.text = "Employee: $name  ·  offline"
                    }
                    // We hold the downloaded list and this code is not in it:
                    // that is a typo, not a sync gap. Say so and block Submit,
                    // rather than queueing a punch the server will only reject
                    // hours later when nobody is standing at the gate.
                    directory != null -> {
                        isEmployeeVerified = false
                        binding.tvEmployeeName.text =
                            "⚠ $empCode is not in the downloaded employee list"
                        Toast.makeText(
                            this@AttendanceActivity,
                            "No employee with code $empCode — check the code, or " +
                                "connect once to refresh the downloaded list.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    // Nothing downloaded to check against. Stay provisional:
                    // a device that has never synced must not stop the gate.
                    else -> Unit
                }
            }
        }
    }

    // ── Leave check (vw_leave_dates) ─────────────────────────────
    // After the employee is verified (and again whenever the date changes),
    // ask the backend whether the employee is on leave on the selected date.
    // Unpaid leave → Submit disabled. Paid leave → only Cash/OT allowed
    // (enforced in the Submit validation). The server re-checks on save.

    private fun checkLeaveStatus() {
        val ebId = verifiedEbId ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        RetrofitClient.getApiService(this).getAttendanceLeaveStatus(ebId, date)
            .enqueue(object : Callback<com.example.slsHrms.api.LeaveStatusResponse> {
                override fun onResponse(
                    call: Call<com.example.slsHrms.api.LeaveStatusResponse>,
                    response: Response<com.example.slsHrms.api.LeaveStatusResponse>
                ) {
                    // Ignore a late response for a different employee.
                    if (ebId != verifiedEbId) return
                    val data = response.body()?.data
                    applyLeaveStatus(data?.onLeave == true, data?.payable)
                }

                override fun onFailure(call: Call<com.example.slsHrms.api.LeaveStatusResponse>, t: Throwable) {
                    // Don't block on a network hiccup — the server enforces
                    // the same rule again on save.
                    if (ebId == verifiedEbId) applyLeaveStatus(false, null)
                }
            })
    }

    private fun applyLeaveStatus(onLeave: Boolean, payable: String?) {
        leavePayable = if (onLeave) (payable?.trim()?.uppercase() ?: "N") else null
        when {
            !onLeave -> binding.btnSubmit.isEnabled = true
            leavePayable == "Y" -> {
                binding.btnSubmit.isEnabled = true
                showAlert(
                    "On Paid Leave",
                    "Employee is on paid leave on this date — only Cash or Over Time attendance is allowed",
                    AlertType.WARNING
                )
            }
            else -> {
                binding.btnSubmit.isEnabled = false
                showAlert(
                    "On Leave",
                    "Employee is on leave on this date — attendance cannot be saved"
                )
            }
        }
    }

    // showAlert / AlertType / extractErrorMessage live in SweetAlert.kt
    // (shared with the attendance report/update screens).

    // ── Search Button — search employee by name ──────────────

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener {
            showEmployeeSearchDialog()
        }
    }

    private fun showEmployeeSearchDialog() {
        val editText = android.widget.EditText(this).apply {
            hint = "Type employee name..."
            setPadding(48, 32, 48, 32)
            setTextColor(android.graphics.Color.BLACK)
        }

        val listView = android.widget.ListView(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(editText)
            addView(listView)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Search Employee")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .create()

        var allEmployees = listOf<Employee>()
        val displayList = mutableListOf<String>()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        listView.adapter = adapter

        // Only JOINED employees: anyone else is refused by /employee/<code> anyway.
        RetrofitClient.getApiService(this).getEmployees(joined = 1)
            .enqueue(object : Callback<EmployeeResponse> {
                override fun onResponse(call: Call<EmployeeResponse>, response: Response<EmployeeResponse>) {
                    if (response.isSuccessful) {
                        allEmployees = response.body()?.employees ?: emptyList()
                        displayList.clear()
                        displayList.addAll(allEmployees.map { "${it.empCode} - ${it.name}" })
                        adapter.notifyDataSetChanged()
                    }
                }
                override fun onFailure(call: Call<EmployeeResponse>, t: Throwable) {
                    Toast.makeText(this@AttendanceActivity, "Failed to load employees", Toast.LENGTH_SHORT).show()
                }
            })

        // Filter as user types
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                displayList.clear()
                displayList.addAll(
                    allEmployees.filter {
                        it.name.lowercase().contains(query) || it.empCode.lowercase().contains(query)
                    }.map { "${it.empCode} - ${it.name}" }
                )
                adapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // When user taps an employee
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedText = displayList[position]
            val empCode = selectedText.split(" - ").firstOrNull()?.trim() ?: ""
            binding.etEmployeeCode.setText(empCode)
            dialog.dismiss()
            // Auto-verify the selected employee
            lookupEmployeeByCode(empCode)
        }

        dialog.show()
    }

    // ── Live Face-Validate Button (auto camera) ──────────────────

    private fun setupFaceValidateButton() {
        binding.btnFaceValidate.setOnClickListener {
            // Face matching is scoped to the branch selected on the dashboard.
            if (selectedBranchId <= 0) {
                Toast.makeText(this, "Please select a branch first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, FaceValidateActivity::class.java).apply {
                putExtra(FaceValidateActivity.EXTRA_BRANCH_ID, selectedBranchId)
                putExtra(FaceValidateActivity.EXTRA_CO_ID, selectedCompanyId)
            }
            faceValidateLauncher.launch(intent)
        }
    }

    // ── Camera Button ────────────────────────────────────────────

    private fun setupCameraButton() {
        binding.btnCamera.setOnClickListener {
            // Face matching is scoped to the branch selected on the dashboard.
            // Without a branch we can't validate the face, so block the capture.
            if (selectedBranchId <= 0) {
                Toast.makeText(this, "Please select a branch first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photosDir = File(cacheDir, "photos")
        photosDir.mkdirs()
        photoFile = File(photosDir, "face_${System.currentTimeMillis()}.jpg")

        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile!!
        )

        cameraLauncher.launch(photoUri)
    }

    // ── Identify Face via /check-face (no attendance saved) ─────

    private fun identifyFace(base64Image: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCamera.isEnabled = false

        val request = FaceRecognitionRequest(
            image = base64Image,
            branchId = if (selectedBranchId > 0) selectedBranchId else null
        )

        RetrofitClient.getApiService(this).checkFace(request)
            .enqueue(object : Callback<FaceRecognitionResponse> {
                override fun onResponse(
                    call: Call<FaceRecognitionResponse>,
                    response: Response<FaceRecognitionResponse>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCamera.isEnabled = true

                    if (response.isSuccessful) {
                        val result = response.body()
                        android.util.Log.d("PHOTO_DEBUG", "check-face response: status=${result?.status}, empName=${result?.empName}, photoHtml=${if (result?.photoHtml.isNullOrEmpty()) "NULL/EMPTY" else "${result?.photoHtml?.length} chars"}")

                        if (result != null && result.status == "success") {
                            // setText fires the TextWatcher, which resets the
                            // leave/verified state — set these AFTER it.
                            binding.etEmployeeCode.setText(result.empCode ?: "")
                            isEmployeeVerified = true
                            verifiedEbId = result.ebId
                            checkLeaveStatus()

                            val name = result.empName ?: ""
                            if (name.isNotEmpty()) {
                                binding.tvEmployeeName.text = "Employee: $name"
                                binding.cardEmployeeInfo.visibility = View.VISIBLE
                            }

                            // Decode photo from photo_html and show in ImageView
                            showEmployeePhoto(result.photoHtml)

                            // Auto-fill department / designation / machines from last attendance
                            applyFaceMatchDefaults(result)

                            Toast.makeText(
                                this@AttendanceActivity,
                                "Face matched: ${result.empName}. Press Submit to mark attendance.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            isEmployeeVerified = false
                            binding.tvEmployeeName.text = "⚠ Face not recognized"
                            binding.cardEmployeeInfo.visibility = View.VISIBLE
                            binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
                            Toast.makeText(
                                this@AttendanceActivity,
                                result?.message ?: "Face not recognized",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        isEmployeeVerified = false
                        binding.tvEmployeeName.text = "⚠ Recognition failed"
                        binding.cardEmployeeInfo.visibility = View.VISIBLE
                        binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
                        Toast.makeText(
                            this@AttendanceActivity,
                            "Recognition failed: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<FaceRecognitionResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCamera.isEnabled = true
                    binding.tvEmployeeName.text = "⚠ Network error"
                    binding.cardEmployeeInfo.visibility = View.VISIBLE
                    binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // ── Decode photo from HTML and show in ImageView ─────────────

    private fun showEmployeePhoto(photoHtml: String?) {
        android.util.Log.d("PHOTO_DEBUG", "showEmployeePhoto called, photoHtml is ${if (photoHtml.isNullOrEmpty()) "NULL/EMPTY" else "${photoHtml.length} chars"}")

        if (photoHtml.isNullOrEmpty()) {
            binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
            return
        }

        try {
            // photo_html may be either:
            //   1) HTML wrapper: <img src="data:image/jpeg;base64,XXXX" />
            //   2) Raw base64 string: /9j/4AAQSk...
            val trimmed = photoHtml.trim()
            val base64Data: String? = when {
                trimmed.contains("base64,") -> {
                    val regex = Regex("""base64,([A-Za-z0-9+/=]+)""")
                    regex.find(trimmed)?.groupValues?.get(1)?.trim()
                }
                // looks like a raw base64 (no HTML tags)
                !trimmed.contains('<') -> trimmed.replace("\\s".toRegex(), "")
                else -> null
            }

            android.util.Log.d("PHOTO_DEBUG", "base64Data length: ${base64Data?.length ?: 0}")

            if (!base64Data.isNullOrEmpty()) {
                val imageBytes = AndroidBase64.decode(base64Data, AndroidBase64.DEFAULT)
                android.util.Log.d("PHOTO_DEBUG", "Decoded bytes: ${imageBytes.size}")
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                android.util.Log.d("PHOTO_DEBUG", "Bitmap: ${bitmap != null}, size: ${bitmap?.width}x${bitmap?.height}")
                if (bitmap != null) {
                    binding.ivEmployeePhoto.setImageBitmap(bitmap)
                } else {
                    binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
                }
            } else {
                android.util.Log.d("PHOTO_DEBUG", "No base64 data extracted. First 200 chars: ${photoHtml.take(200)}")
                binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
            }
        } catch (e: Exception) {
            android.util.Log.e("PHOTO_DEBUG", "Error decoding photo", e)
            binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
        }
    }

    // ── Mark Attendance via /attendance (called on Submit) ───────

    private fun markAttendance(
        base64Image: String, attType: String,
        deptId: Int?, shiftId: Int?, desigId: Int?,
        attendanceDate: String, shiftHours: Double, workingHours: Double, idleHours: Double,
        getLocation: String
    ) {
        // /attendance identifies the person server-side with dlib, so it cannot
        // be queued — there is nobody to match against offline. Point the
        // operator at the two paths that do work without a network.
        if (!Connectivity.isOnline(this)) {
            showAlert(
                "No network",
                "Photo attendance needs a connection because the server does the " +
                    "matching.\n\nUse the face-validate button (matches on this " +
                    "device) or enter the employee code manually — both save offline.",
                AlertType.WARNING
            )
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        val request = FaceRecognitionRequest(
            image = base64Image,
            attType = attType,
            departmentId = deptId,
            shiftId = shiftId,
            designationId = desigId,
            attendanceDate = attendanceDate,
            shiftHours = shiftHours,
            workingHours = workingHours,
            idleHours = idleHours,
            machineIds = if (selectedMachineIds.isNotEmpty()) selectedMachineIds.toList() else null,
            branchId = if (selectedBranchId > 0) selectedBranchId else null,
            getLocation = getLocation
        )

        RetrofitClient.getApiService(this).recognizeFace(request)
            .enqueue(object : Callback<FaceRecognitionResponse> {
                override fun onResponse(
                    call: Call<FaceRecognitionResponse>,
                    response: Response<FaceRecognitionResponse>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true

                    android.util.Log.d("ATTENDANCE_DEBUG", "<<< Response code=${response.code()}, success=${response.isSuccessful}")

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null && result.status == "success") {
                            showAlert(
                                "Success",
                                "Attendance marked for ${result.empName}!",
                                AlertType.SUCCESS
                            )
                            // Clear form for next entry
                            capturedBase64 = null
                            isEmployeeVerified = false
                            binding.etEmployeeCode.setText("")
                            binding.cardEmployeeInfo.visibility = View.GONE
                            binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
                            binding.tvEmployeeName.text = ""
                        } else {
                            Toast.makeText(
                                this@AttendanceActivity,
                                result?.message ?: "Failed to mark attendance",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "no body"
                        android.util.Log.e("ATTENDANCE_DEBUG", "<<< Error body: $errorBody")
                        showAlert("Attendance Not Saved", extractErrorMessage(errorBody))
                    }
                }

                override fun onFailure(call: Call<FaceRecognitionResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    android.util.Log.e("ATTENDANCE_DEBUG", "<<< Network failure", t)
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // ── Submit ───────────────────────────────────────────────────

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            // A branch (selected on the dashboard) is required — attendance and
            // face matching are both scoped to it.
            if (selectedBranchId <= 0) {
                Toast.makeText(this, "Please select a branch first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val empCode = binding.etEmployeeCode.text.toString().trim()
            val deptPos = binding.spinnerDepartment.selectedItemPosition
            val shiftPos = binding.spinnerShift.selectedItemPosition
            val occPos = binding.spinnerOccupation.selectedItemPosition

            val shiftHoursStr = binding.etShiftHours.text.toString().trim()
            val workingHoursStr = binding.etWorkingHours.text.toString().trim()
            val idleHoursStr = binding.etIdleHours.text.toString().trim()

            // 1. Valid employee code (not needed for face mode)
            if (capturedBase64 == null) {
                // Manual mode — employee code required and must be verified
                if (empCode.isEmpty()) {
                    binding.etEmployeeCode.error = "Please enter employee code"
                    binding.etEmployeeCode.requestFocus()
                    return@setOnClickListener
                }
                if (!isEmployeeVerified) {
                    Toast.makeText(this, "Please verify employee code first (use ✓)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 1b. Paid leave (vw_leave_dates payable='Y') — only Cash / OT
            // attendance is allowed; a Regular mark would double-pay the day.
            // (Unpaid leave never reaches here — Submit is disabled for it.)
            if (leavePayable == "Y" && selectedTab == "Regular") {
                showAlert(
                    "On Paid Leave",
                    "Employee is on paid leave on this date — only Cash or Over Time attendance is allowed",
                    AlertType.WARNING
                )
                return@setOnClickListener
            }

            // 2. Department must be selected
            if (deptPos == 0) {
                Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Shift must be selected
            if (shifts.isEmpty() || shiftPos < 0) {
                Toast.makeText(this, "Please select a shift", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 4. Occupation must be selected (position 0 is "Select Occupation")
            if (occPos == 0) {
                Toast.makeText(this, "Please select an occupation", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 5. Shift hours > 0
            val shiftHours = shiftHoursStr.toDoubleOrNull() ?: 0.0
            if (shiftHours <= 0) {
                binding.etShiftHours.error = "Shift hours must be greater than 0"
                binding.etShiftHours.requestFocus()
                return@setOnClickListener
            }

            // 6. Working hours > 0
            val workingHours = workingHoursStr.toDoubleOrNull() ?: 0.0
            if (workingHours <= 0) {
                binding.etWorkingHours.error = "Working hours must be greater than 0"
                binding.etWorkingHours.requestFocus()
                return@setOnClickListener
            }

            // 7. working_hours - idle_hours > 0
            val idleHours = idleHoursStr.toDoubleOrNull() ?: 0.0
            if ((workingHours - idleHours) <= 0) {
                Toast.makeText(this, "Working hours minus idle hours must be greater than 0", Toast.LENGTH_SHORT).show()
                binding.etIdleHours.requestFocus()
                return@setOnClickListener
            }

            // Determine att_type from selected tab
            val attType = when (selectedTab) {
                "OT" -> "O"
                "Cash" -> "C"
                else -> "R"  // Regular
            }

            // Extract actual IDs from spinners
            val deptId = if (deptPos > 0 && departments.size >= deptPos) departments[deptPos - 1].id else null
            val shiftId = if (shifts.isNotEmpty() && shiftPos < shifts.size) shifts[shiftPos].id else null
            val occId = if (occPos > 0 && occupations.size >= occPos) occupations[occPos - 1].id else null

            // Gather all form data for debug display.
            // A live face-validated identity (4th button) counts as Face too,
            // even though it posts to /mark-attendance without an image.
            val mode = if (capturedBase64 != null || faceValidated) "Face" else "Manual"
            val deptName = if (deptPos > 0 && departments.size >= deptPos) departments[deptPos - 1].name else "N/A"
            val shiftName = if (shifts.isNotEmpty() && shiftPos < shifts.size) shifts[shiftPos].name else "N/A"
            val occName = if (occPos > 0 && occupations.size >= occPos) occupations[occPos - 1].name else "N/A"
            val dateStr = binding.tvDate.text.toString()
            val baseUrl = com.example.slsHrms.api.ApiConfig.getBaseUrl(this)
            // Endpoint depends on whether a photo was captured (camera button),
            // NOT on `mode` — face-validated entries post to /mark-attendance.
            val apiUrl = if (capturedBase64 != null) "${baseUrl}attendance" else "${baseUrl}mark-attendance"

            val debugMsg = """
                |API: POST $apiUrl
                |Mode: $mode | Tab: $selectedTab | att_type: $attType
                |EmpCode: $empCode | Date: $dateStr
                |Dept: $deptName (id=$deptId) | Shift: $shiftName (id=$shiftId) | Occ: $occName (id=$occId)
                |ShiftHrs: $shiftHoursStr | WorkHrs: $workingHoursStr | IdleHrs: $idleHoursStr
                |Image: ${if (capturedBase64 != null) "${capturedBase64!!.length} chars" else "none"}
            """.trimMargin()

            android.util.Log.d("ATTENDANCE_DEBUG", debugMsg)
            Toast.makeText(this, debugMsg, Toast.LENGTH_LONG).show()

            // Format attendance date as yyyy-MM-dd for the API
            val attendanceDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // All validations passed — determine Face vs Manual
            if (capturedBase64 != null) {
                // Face attendance — send image to /attendance
                markAttendance(capturedBase64!!, attType, deptId, shiftId, occId,
                    attendanceDate, shiftHours, workingHours, idleHours, capturedGeoLocation)
            } else {
                // Manual attendance — send emp_code to /mark-attendance.
                // Source is "Face" when the identity came from live face-validation
                // (4th button), otherwise "Manual".
                val manualStatus = if (faceValidated) "Face" else "Manual"
                markAttendanceManual(empCode, attType, deptId, shiftId, occId,
                    attendanceDate, shiftHours, workingHours, idleHours, capturedGeoLocation,
                    manualStatus)
            }
        }
    }

    // ── Manual Attendance via /mark-attendance ───────────────────

    private fun markAttendanceManual(
        empCode: String, attType: String,
        deptId: Int?, shiftId: Int?, desigId: Int?,
        attendanceDate: String, shiftHours: Double, workingHours: Double, idleHours: Double,
        getLocation: String, status: String = "Manual"
    ) {

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        // An offline match travels with its capture so the server can re-verify.
        // Online, the JPEG is inlined; offline, only the path goes on the header
        // and OfflineInterceptor keeps the image out of the queued payload.
        val online = Connectivity.isOnline(this)
        val inlinePhoto = if (matchedOffline && online) {
            offlinePhotoPath
                ?.let { OfflinePhotoStore.read(this, it) }
                ?.let { AndroidBase64.encodeToString(it, AndroidBase64.NO_WRAP) }
        } else null

        val request = com.example.slsHrms.api.MarkAttendanceRequest(
            empCode = empCode,
            status = status,
            attType = attType,
            departmentId = deptId,
            shiftId = shiftId,
            designationId = desigId,
            attendanceDate = attendanceDate,
            shiftHours = shiftHours,
            workingHours = workingHours,
            idleHours = idleHours,
            machineIds = if (selectedMachineIds.isNotEmpty()) selectedMachineIds.toList() else null,
            branchId = if (selectedBranchId > 0) selectedBranchId else null,
            getLocation = getLocation,
            matchedOffline = if (matchedOffline) true else null,
            matchConfidence = if (matchedOffline) offlineConfidence else null,
            faceImageB64 = inlinePhoto
        )

        RetrofitClient.getApiService(this)
            .markAttendanceManual(request, if (matchedOffline) offlinePhotoPath else null)
            .enqueue(object : Callback<FaceRecognitionResponse> {
                override fun onResponse(
                    call: Call<FaceRecognitionResponse>,
                    response: Response<FaceRecognitionResponse>
                ) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true

                    android.util.Log.d("ATTENDANCE_DEBUG", "<<< Response code=${response.code()}, success=${response.isSuccessful}")

                    if (response.isSuccessful) {
                        val result = response.body()
                        if (result != null && result.status == "success") {
                            val who = result.empName?.takeIf { it.isNotBlank() } ?: empCode
                            if (result.queued == true) {
                                showAlert(
                                    "Saved offline",
                                    "Attendance for $who is saved on this device and will " +
                                        "upload automatically when the network returns.",
                                    AlertType.SUCCESS
                                )
                            } else {
                                showAlert("Success", "Attendance marked for $who!", AlertType.SUCCESS)
                            }
                            // Start the face cooldown clock for this employee.
                            if (faceValidated) com.example.slsHrms.face.FacePrefs.notePunch(empCode)
                            // Clear form for next entry
                            capturedBase64 = null
                            isEmployeeVerified = false
                            // The outbox owns the capture from here; a live send
                            // already uploaded it, so drop our copy either way.
                            if (result.queued != true) OfflinePhotoStore.delete(offlinePhotoPath)
                            matchedOffline = false
                            offlinePhotoPath = null
                            offlineConfidence = 0.0
                            binding.etEmployeeCode.setText("")
                            binding.cardEmployeeInfo.visibility = View.GONE
                            binding.ivEmployeePhoto.setImageResource(R.drawable.ic_person)
                            binding.tvEmployeeName.text = ""
                        } else {
                            Toast.makeText(
                                this@AttendanceActivity,
                                result?.message ?: "Failed to mark attendance",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "no body"
                        android.util.Log.e("ATTENDANCE_DEBUG", "<<< Error body: $errorBody")
                        showAlert("Attendance Not Saved", extractErrorMessage(errorBody))
                    }
                }

                override fun onFailure(call: Call<FaceRecognitionResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    android.util.Log.e("ATTENDANCE_DEBUG", "<<< Network failure", t)
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // ── Machine Selector ─────────────────────────────────────────
    private fun setupMachineSelector() {
        binding.layoutMachineNumbers.setOnClickListener {
            val occupationPos = binding.spinnerOccupation.selectedItemPosition
            if (occupationPos == 0) {
                Toast.makeText(this, "Please select occupation first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val designationId = occupations[occupationPos - 1].id
            showMachineSelectorDialog(designationId)
        }
    }

    private fun showMachineSelectorDialog(designationId: Int) {
        val dialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_machine_selector, null)
        dialog.setView(dialogView)
        
        val alertDialog = dialog.create()
        
        val etSearch = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchMachine)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMachines)
        val tvSelectedCount = dialogView.findViewById<TextView>(R.id.tvSelectedCount)
        val btnOk = dialogView.findViewById<android.widget.Button>(R.id.btnOk)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)
        
        // Setup RecyclerView
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter = com.example.slsHrms.adapter.MachineSelectionAdapter(
            machines,
            selectedMachineIds
        ) { count ->
            tvSelectedCount.text = "$count machine(s) selected"
        }
        recyclerView.adapter = adapter
        
        // Load machines
        loadMachines(designationId, adapter)
        
        // Search functionality
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Update initial count
        tvSelectedCount.text = "${selectedMachineIds.size} machine(s) selected"
        
        btnOk.setOnClickListener {
            updateMachineDisplay()
            alertDialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }

    private fun loadMachines(designationId: Int, adapter: com.example.slsHrms.adapter.MachineSelectionAdapter? = null) {
        // Remember which designations this gate actually uses, so the Download
        // button warms their machine lists and not 186 irrelevant ones.
        SyncEngine.rememberDesignation(this, designationId)
        RetrofitClient.getApiService(this).getMachines(designationId).enqueue(
            object : Callback<com.example.slsHrms.api.MachineResponse> {
                override fun onResponse(
                    call: Call<com.example.slsHrms.api.MachineResponse>,
                    response: Response<com.example.slsHrms.api.MachineResponse>
                ) {
                    android.util.Log.d("MACHINE_DEBUG", "Response code: ${response.code()}")
                    android.util.Log.d("MACHINE_DEBUG", "Response successful: ${response.isSuccessful}")
                    android.util.Log.d("MACHINE_DEBUG", "Response body: ${response.body()}")
                    
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val data = response.body()?.data ?: emptyList()
                        
                        // Filter valid machines (id must be non-null and > 0)
                        val validMachines = data.filter { it.id != null && it.id > 0 }
                        
                        // Log for debugging
                        android.util.Log.d("MACHINE_DEBUG", "Total machines: ${data.size}, Valid: ${validMachines.size}")
                        
                        // Load valid machines
                        machines.clear()
                        machines.addAll(validMachines)
                        
                        // Update adapter if provided
                        adapter?.updateMachines(machines)
                            
                    } else {
                        android.util.Log.e("MACHINE_DEBUG", "API call failed: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<com.example.slsHrms.api.MachineResponse>, t: Throwable) {
                    android.util.Log.e("MACHINE_DEBUG", "API call failed", t)
                }
            }
        )
    }

    private fun updateMachineDisplay() {
        if (selectedMachineIds.isEmpty()) {
            binding.tvSelectedMachines.text = "Tap to select machines"
            binding.tvSelectedMachines.setTextColor(getColor(R.color.hint_text))
            binding.tvMachineCount.visibility = View.GONE
        } else {
            val selectedMachines = machines.filter { 
                val machineId = it.id ?: 0
                machineId > 0 && selectedMachineIds.contains(machineId) 
            }
            val machineDisplayNames = selectedMachines.map { it.getDisplayName() }.joinToString(", ")
            binding.tvSelectedMachines.text = machineDisplayNames
            binding.tvSelectedMachines.setTextColor(getColor(R.color.label_text))
            binding.tvMachineCount.text = "${selectedMachineIds.size} machine(s) selected"
            binding.tvMachineCount.visibility = View.VISIBLE
        }
    }

    // ── Auto-fill last-worked defaults after eb_no validation ────
    // After a successful employee-code lookup, carry over the employee's
    // last-worked designation and machine numbers — but ONLY when the
    // department the user has already selected matches the last-worked
    // department. Otherwise the designation/machines belong to a different
    // department and aren't valid here, so we clear them.
    private fun applyLastWorkedDefaults(result: FaceRecognitionResponse) {
        val deptPos = binding.spinnerDepartment.selectedItemPosition
        val selectedDeptId =
            if (deptPos > 0 && departments.size >= deptPos) departments[deptPos - 1].id else null
        val lastDeptId  = result.defaultDepartmentId
        val lastDesigId = result.defaultDesignationId

        // Remember them so picking the matching department later also fills in.
        lastWorkedDeptId     = lastDeptId
        lastWorkedDesigId    = lastDesigId
        lastWorkedMachineIds = result.defaultMachineIds

        // Always start from a clean machine state each lookup.
        selectedMachineIds.clear()
        machines.clear()
        updateMachineDisplay()

        val deptMatches = selectedBranchId > 0 && selectedDeptId != null &&
            lastDeptId != null && selectedDeptId == lastDeptId

        if (!deptMatches || lastDesigId == null) {
            // No department selected, mismatch, or no last designation — clear it.
            binding.spinnerOccupation.setSelection(0)
            return
        }

        // Departments match — default the designation spinner to the last-worked
        // designation. Designations for this dept were already loaded when the
        // department was selected, so select directly; only if they aren't loaded
        // yet do we (re)fetch and apply via the pending path.
        val idx = occupations.indexOfFirst { it.id == lastDesigId }
        if (idx >= 0) {
            pendingMachineIds = result.defaultMachineIds
            selectDesignation(idx + 1, lastDesigId)
        } else {
            pendingDesignationId = lastDesigId
            pendingMachineIds    = result.defaultMachineIds
            loadDesignations(selectedBranchId, selectedDeptId)
        }
    }

    // Move the designation spinner to [position] reliably. A setSelection issued
    // in the same frame as an adapter swap is often swallowed by the layout pass,
    // so we also post it. Machines for the designation are applied afterwards.
    private fun selectDesignation(position: Int, designationId: Int) {
        binding.spinnerOccupation.setSelection(position)
        binding.spinnerOccupation.post {
            binding.spinnerOccupation.setSelection(position)
            applyPendingMachines(designationId)
        }
    }

    // ── Auto-fill defaults after face match ──────────────────────
    // Backend's /check-face response now carries default_department_id,
    // default_designation_id and default_machine_ids derived from the
    // employee's last daily_attendance + daily_ebmc_attendance rows.
    private fun applyFaceMatchDefaults(result: FaceRecognitionResponse) {
        // Reset any previous machine selection
        selectedMachineIds.clear()
        machines.clear()
        updateMachineDisplay()

        pendingDesignationId = result.defaultDesignationId
        pendingMachineIds    = result.defaultMachineIds

        val defaultDeptId = result.defaultDepartmentId
        if (defaultDeptId != null && defaultDeptId > 0 && departments.isNotEmpty()) {
            val deptIdx = departments.indexOfFirst { it.id == defaultDeptId }
            if (deptIdx >= 0) {
                // Triggers onItemSelected -> loadDesignations(...) -> applies pendingDesignationId
                binding.spinnerDepartment.setSelection(deptIdx + 1)
            } else if (selectedBranchId > 0) {
                // Department not in current list -> still try to load designations directly
                loadDesignations(selectedBranchId, defaultDeptId)
            }
        } else if (pendingDesignationId != null && selectedBranchId > 0) {
            // No dept, but we have a designation default -> reload all designations
            loadDesignations(selectedBranchId, null)
        }
    }

    // Called from loadDesignations() once the designations list is ready
    // and we have a pending designationId to preselect.
    private fun applyPendingMachines(designationId: Int) {
        val pending = pendingMachineIds ?: emptyList()
        pendingMachineIds = null
        // Claim this designation now (synchronously) so the spinner's change
        // listener doesn't wipe the machines we're about to auto-select for it.
        machinesForDesignationId = designationId
        SyncEngine.rememberDesignation(this, designationId)
        RetrofitClient.getApiService(this).getMachines(designationId).enqueue(
            object : Callback<com.example.slsHrms.api.MachineResponse> {
                override fun onResponse(
                    call: Call<com.example.slsHrms.api.MachineResponse>,
                    response: Response<com.example.slsHrms.api.MachineResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val data = response.body()?.data ?: emptyList()
                        val valid = data.filter { (it.id ?: 0) > 0 }
                        machines.clear()
                        machines.addAll(valid)

                        if (pending.isNotEmpty()) {
                            selectedMachineIds.clear()
                            valid.forEach { m ->
                                val mid = m.id ?: 0
                                if (mid > 0 && pending.contains(mid)) {
                                    selectedMachineIds.add(mid)
                                }
                            }
                        }
                        updateMachineDisplay()
                    }
                }
                override fun onFailure(
                    call: Call<com.example.slsHrms.api.MachineResponse>,
                    t: Throwable
                ) {
                    android.util.Log.e("MACHINE_DEBUG", "applyPendingMachines failed", t)
                }
            }
        )
    }
}
