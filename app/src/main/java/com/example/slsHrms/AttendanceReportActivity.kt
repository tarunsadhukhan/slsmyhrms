package com.example.slsHrms

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.slsHrms.adapter.AttendanceReportAdapter
import com.example.slsHrms.api.AttendanceReportResponse
import com.example.slsHrms.api.Department
import com.example.slsHrms.api.DepartmentResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.api.Shift
import com.example.slsHrms.api.ShiftResponse
import com.example.slsHrms.databinding.ActivityAttendanceReportBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceReportBinding
    private lateinit var adapter: AttendanceReportAdapter
    private var departments = mutableListOf<Department>()
    private var shifts = mutableListOf<Shift>()
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private var selectedCompanyId: Int = 0
    private var selectedBranchId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupDatePickers()
        setupDefaultDates()
        setupSearchButton()

        // Read extras FIRST so CO_ID/BRANCH_ID are available for loadDepartments
        handleIntentExtras()
        loadDepartments()
        loadShifts()
    }

    // ── Handle Intent Extras from Dashboard ─────────────────────

    private var pendingDepartmentId: Int = -1
    private var autoSearchOnLoad = false

    private fun handleIntentExtras() {
        val fromDate = intent.getStringExtra("FROM_DATE")
        pendingDepartmentId = intent.getIntExtra("DEPARTMENT_ID", -1)
        autoSearchOnLoad = intent.getBooleanExtra("AUTO_SEARCH", false)
        selectedCompanyId = intent.getIntExtra("CO_ID", 0)
        selectedBranchId  = intent.getIntExtra("BRANCH_ID", 0)

        if (!fromDate.isNullOrBlank()) {
            binding.etFromDate.setText(fromDate)
        }
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
        adapter = AttendanceReportAdapter(
            records = emptyList(),
            departments = emptyList(),
            shifts = emptyList(),
            branchId = selectedBranchId,
            showMachineSelector = { desigId, selectedIds, machinesCache, onClosed ->
                showMachineSelectorDialog(desigId, machinesCache, selectedIds, onClosed)
            },
            onSaved = { performSearch() }
        )
        binding.rvAttendance.layoutManager = LinearLayoutManager(this)
        binding.rvAttendance.adapter = adapter
    }

    private fun rebuildAdapter() {
        // departments includes a virtual "All Departments" entry at index 0; strip it.
        val realDepts = departments.filter { it.id > 0 }
        adapter = AttendanceReportAdapter(
            records = emptyList(),
            departments = realDepts,
            shifts = shifts,
            branchId = selectedBranchId,
            showMachineSelector = { desigId, selectedIds, machinesCache, onClosed ->
                showMachineSelectorDialog(desigId, machinesCache, selectedIds, onClosed)
            },
            onSaved = { performSearch() }
        )
        binding.rvAttendance.adapter = adapter
    }

    // ── Default dates (current month) ────────────────────────────

    private fun setupDefaultDates() {
        val cal = Calendar.getInstance()
        val today = displayDateFormat.format(cal.time)
        binding.etFromDate.setText(today)
    }

    // ── Date Pickers ─────────────────────────────────────────────

    private fun setupDatePickers() {
        binding.etFromDate.setOnClickListener { showDatePicker(true) }
    }

    private fun showDatePicker(isFromDate: Boolean) {
        val cal = Calendar.getInstance()

        // Try to parse existing value (dd-MM-yyyy)
        try {
            val existing = binding.etFromDate.text.toString()
            if (existing.isNotBlank()) {
                val parsed = displayDateFormat.parse(existing)
                if (parsed != null) cal.time = parsed
            }
        } catch (_: Exception) { }

        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day)
            val selected = displayDateFormat.format(cal.time)
            binding.etFromDate.setText(selected)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            .show()
    }

    // ── Load Departments for Spinner ─────────────────────────────

    private fun loadDepartments() {
        RetrofitClient.getApiService(this).getDepartments(
            coId = if (selectedCompanyId > 0) selectedCompanyId else null,
            branchId = if (selectedBranchId > 0) selectedBranchId else null
        )
            .enqueue(object : Callback<DepartmentResponse> {
                override fun onResponse(
                    call: Call<DepartmentResponse>,
                    response: Response<DepartmentResponse>
                ) {
                    if (response.isSuccessful) {
                        val deptList = response.body()?.departments ?: emptyList()
                        departments.clear()
                        // Add "All Departments" as first option
                        departments.add(Department(id = -1, name = "All Departments"))
                        departments.addAll(deptList)

                        val deptNames = departments.map { it.name }
                        val spinnerAdapter = ArrayAdapter(
                            this@AttendanceReportActivity,
                            R.layout.spinner_item_black,
                            deptNames
                        )
                        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
                        binding.spinnerDepartment.adapter = spinnerAdapter

                        // Rebuild row adapter so cards have department list available
                        rebuildAdapter()

                        // Auto-select department if opened from dashboard
                        if (pendingDepartmentId > 0) {
                            val idx = departments.indexOfFirst { it.id == pendingDepartmentId }
                            if (idx >= 0) {
                                binding.spinnerDepartment.setSelection(idx)
                            }
                        }

                        // Auto-search if opened from dashboard
                        if (autoSearchOnLoad) {
                            binding.spinnerDepartment.post { performSearch() }
                        }
                    }
                }

                override fun onFailure(call: Call<DepartmentResponse>, t: Throwable) {
                    Toast.makeText(
                        this@AttendanceReportActivity,
                        "Failed to load departments",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ── Search Button ────────────────────────────────────────────

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener { performSearch() }
    }

    // ── Load Shifts (Spell) for Spinner ─────────────────────────

    private fun loadShifts() {
        // Initialize with default option in case API fails
        val defaultAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item_black,
            listOf("All Spells")
        )
        defaultAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        binding.spinnerSpell.adapter = defaultAdapter

        RetrofitClient.getApiService(this).getShifts(
            if (selectedBranchId > 0) selectedBranchId else null
        ).enqueue(object : Callback<ShiftResponse> {
            override fun onResponse(call: Call<ShiftResponse>, response: Response<ShiftResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()?.shifts ?: emptyList()
                    shifts.clear()
                    shifts.addAll(data)

                    val names = mutableListOf("All Spells")
                    names.addAll(shifts.map { it.name })
                    val adapter = ArrayAdapter(
                        this@AttendanceReportActivity,
                        R.layout.spinner_item_black,
                        names
                    )
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
                    binding.spinnerSpell.adapter = adapter

                    // Rebuild row adapter so cards have shifts list available
                    rebuildAdapter()
                }
            }

            override fun onFailure(call: Call<ShiftResponse>, t: Throwable) {
                // optional filter, silently ignore
            }
        })
        // Also rebuild row adapter so cards have shifts list available (will fire when shifts arrive)
        // We listen via post-success above (rebuildAdapter is called there too).
    }

    private fun performSearch() {
        val fromDateDisplay = binding.etFromDate.text.toString().trim()

        if (fromDateDisplay.isBlank()) {
            Toast.makeText(this, "Please select Date", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert dd-MM-yyyy → yyyy-MM-dd for API
        val fromDate: String
        val toDate: String
        try {
            val fromParsed = displayDateFormat.parse(fromDateDisplay)
            fromDate = apiDateFormat.format(fromParsed!!)
            toDate = fromDate
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected department
        val selectedPos = binding.spinnerDepartment.selectedItemPosition
        val departmentId = if (selectedPos > 0 && departments.size > selectedPos) {
            departments[selectedPos].id
        } else null

        // Get selected spell
        val spellPos = binding.spinnerSpell.selectedItemPosition
        val shiftName = if (spellPos > 0 && shifts.size >= spellPos) {
            shifts[spellPos - 1].name
        } else null

        // Get emp code filter
        val empCode = binding.etEmpCode.text.toString().trim().ifBlank { null }

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.rvAttendance.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.btnSearch.isEnabled = false

        RetrofitClient.getApiService(this).getAttendanceReportRange(
            fromDate = fromDate,
            toDate = toDate,
            departmentId = departmentId,
            empCode = empCode,
            branchId = if (selectedBranchId > 0) selectedBranchId else null,
            shiftName = shiftName
        ).enqueue(object : Callback<AttendanceReportResponse> {
            override fun onResponse(
                call: Call<AttendanceReportResponse>,
                response: Response<AttendanceReportResponse>
            ) {
                binding.progressBar.visibility = View.GONE
                binding.btnSearch.isEnabled = true

                if (response.isSuccessful) {
                    val result = response.body()
                    val records = result?.data ?: emptyList()

                    binding.tvCount.text = "Total: ${records.size}"

                    if (records.isEmpty()) {
                        binding.rvAttendance.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = "No attendance records found"
                    } else {
                        binding.rvAttendance.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        adapter.updateList(records)
                    }
                } else {
                    binding.rvAttendance.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "Error loading data"
                    Toast.makeText(
                        this@AttendanceReportActivity,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<AttendanceReportResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.btnSearch.isEnabled = true
                binding.rvAttendance.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.tvEmptyMessage.text = "Network error"
                Toast.makeText(
                    this@AttendanceReportActivity,
                    "Network error: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // ── Edit Attendance Dialog ───────────────────────────────────

    private fun showEditDialog(record: com.example.slsHrms.api.AttendanceRecord) {
        val dialog = android.app.AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_attendance, null)
        dialog.setView(dialogView)

        val alertDialog = dialog.create()
        alertDialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        // Find views
        val tvEmpCode = dialogView.findViewById<android.widget.TextView>(R.id.tvEmpCode)
        val tvEmpName = dialogView.findViewById<android.widget.TextView>(R.id.tvEmpName)
        val tvDate = dialogView.findViewById<android.widget.TextView>(R.id.tvDate)
        val tvSpell = dialogView.findViewById<android.widget.TextView>(R.id.tvSpell)
        val spinnerDepartment = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerDepartment)
        val spinnerDesignation = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerDesignation)
        val spinnerAttType = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerAttType)
        val etWorkingHours = dialogView.findViewById<android.widget.EditText>(R.id.etWorkingHours)
        val etIdleHours = dialogView.findViewById<android.widget.EditText>(R.id.etIdleHours)
        val layoutMachineNumbers = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutMachineNumbers)
        val tvSelectedMachines = dialogView.findViewById<android.widget.TextView>(R.id.tvSelectedMachines)
        val tvMachineCount = dialogView.findViewById<android.widget.TextView>(R.id.tvMachineCount)
        val btnSave = dialogView.findViewById<android.widget.Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)

        // Set non-editable fields
        tvEmpCode.text = record.empCode
        tvEmpName.text = record.empName

        // Convert date to dd-MM-yyyy format for display
        val displayDate = try {
            val parsed = apiDateFormat.parse(record.attendanceDate)
            displayDateFormat.format(parsed!!)
        } catch (_: Exception) {
            record.attendanceDate
        }
        tvDate.text = displayDate
        tvSpell.text = record.shiftName ?: "-"

        // Set editable fields
        etWorkingHours.setText(String.format("%.1f", record.workingHours ?: 0.0))
        etIdleHours.setText(String.format("%.1f", record.idleHours ?: 0.0))

        // ── Local state for this dialog ──
        val dialogDesignations = mutableListOf<com.example.slsHrms.api.Designation>()
        val dialogMachines = mutableListOf<com.example.slsHrms.api.Machine>()
        val selectedMachineIds = mutableSetOf<Int>()
        // Initial machine codes from record (comma separated mech_codes)
        val initialMachineCodes = record.machineNos
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toMutableSet() ?: mutableSetOf()

        // Setup Department Spinner (skip "All Departments" virtual entry at index 0)
        val realDepartments = departments.filter { it.id > 0 }
        val deptNames = mutableListOf("Select Department")
        deptNames.addAll(realDepartments.map { it.name })
        val deptAdapter = ArrayAdapter(this, R.layout.spinner_item_black, deptNames)
        deptAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        spinnerDepartment.adapter = deptAdapter

        val currentDeptIndex = realDepartments.indexOfFirst { it.name == record.departmentName }

        // Helper to refresh designation spinner UI from dialogDesignations
        fun refreshDesignationSpinner(preselectName: String? = null) {
            val items = mutableListOf("Select Designation")
            items.addAll(dialogDesignations.map { it.name })
            val adp = ArrayAdapter(this, R.layout.spinner_item_black, items)
            adp.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
            spinnerDesignation.adapter = adp
            if (preselectName != null) {
                val idx = dialogDesignations.indexOfFirst { it.name == preselectName }
                if (idx >= 0) spinnerDesignation.setSelection(idx + 1)
            }
        }

        // Helper to update machine display text
        fun refreshMachineDisplay() {
            if (selectedMachineIds.isEmpty()) {
                tvSelectedMachines.text = "Tap to select machines"
                tvSelectedMachines.setTextColor(getColor(R.color.hint_text))
                tvMachineCount.visibility = View.GONE
            } else {
                val sel = dialogMachines.filter { (it.id ?: 0) > 0 && selectedMachineIds.contains(it.id) }
                val display = if (sel.isNotEmpty())
                    sel.joinToString(", ") { it.getDisplayName() }
                else
                    initialMachineCodes.joinToString(", ")
                tvSelectedMachines.text = display
                tvSelectedMachines.setTextColor(getColor(R.color.label_text))
                tvMachineCount.text = "${selectedMachineIds.size} machine(s) selected"
                tvMachineCount.visibility = View.VISIBLE
            }
        }

        // Pre-fill display with initial codes (until machines are loaded for resolution)
        if (initialMachineCodes.isNotEmpty()) {
            tvSelectedMachines.text = initialMachineCodes.joinToString(", ")
            tvSelectedMachines.setTextColor(getColor(R.color.label_text))
            tvMachineCount.text = "${initialMachineCodes.size} machine(s) selected"
            tvMachineCount.visibility = View.VISIBLE
        }

        // Load machines for a designation, then preselect by mech_code matching
        fun loadMachinesForDesignation(designationId: Int) {
            RetrofitClient.getApiService(this).getMachines(designationId).enqueue(
                object : Callback<com.example.slsHrms.api.MachineResponse> {
                    override fun onResponse(
                        call: Call<com.example.slsHrms.api.MachineResponse>,
                        response: Response<com.example.slsHrms.api.MachineResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            val data = response.body()?.data ?: emptyList()
                            val valid = data.filter { (it.id ?: 0) > 0 }
                            dialogMachines.clear()
                            dialogMachines.addAll(valid)

                            // Preselect from initialMachineCodes by mech_code match
                            if (initialMachineCodes.isNotEmpty() && selectedMachineIds.isEmpty()) {
                                valid.forEach { m ->
                                    val code = m.mechCode?.trim() ?: ""
                                    if (code.isNotEmpty() && initialMachineCodes.contains(code)) {
                                        m.id?.let { selectedMachineIds.add(it) }
                                    }
                                }
                                refreshMachineDisplay()
                            }
                        }
                    }
                    override fun onFailure(call: Call<com.example.slsHrms.api.MachineResponse>, t: Throwable) {}
                }
            )
        }

        // Load designations for a department; on completion refresh spinner & machines
        fun loadDesignationsForDept(subDeptId: Int?, preselectDesigName: String?) {
            if (selectedBranchId <= 0) {
                dialogDesignations.clear()
                refreshDesignationSpinner(preselectDesigName)
                return
            }
            RetrofitClient.getApiService(this).getDesignations(
                branchId = selectedBranchId,
                subDeptId = subDeptId
            ).enqueue(object : Callback<com.example.slsHrms.api.DesignationResponse> {
                override fun onResponse(
                    call: Call<com.example.slsHrms.api.DesignationResponse>,
                    response: Response<com.example.slsHrms.api.DesignationResponse>
                ) {
                    if (response.isSuccessful) {
                        dialogDesignations.clear()
                        dialogDesignations.addAll(response.body()?.designations ?: emptyList())
                        refreshDesignationSpinner(preselectDesigName)

                        // After designation list loaded, if a designation is selected, load its machines
                        val selPos = spinnerDesignation.selectedItemPosition
                        if (selPos > 0 && dialogDesignations.size >= selPos) {
                            val desigId = dialogDesignations[selPos - 1].id
                            loadMachinesForDesignation(desigId)
                        }
                    } else {
                        Toast.makeText(this@AttendanceReportActivity,
                            "Failed to load designations", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<com.example.slsHrms.api.DesignationResponse>, t: Throwable) {}
            })
        }

        // Department change listener → reload designations
        spinnerDepartment.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            private var first = true
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val subDeptId = if (position > 0) realDepartments[position - 1].id else null
                // Preserve current designation only on the very first auto-trigger (initial load)
                val preselect = if (first) record.designationName else null
                first = false
                // Clear previously selected machines when department changes (after first)
                if (preselect == null) {
                    selectedMachineIds.clear()
                    dialogMachines.clear()
                    refreshMachineDisplay()
                }
                loadDesignationsForDept(subDeptId, preselect)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Designation change listener → reload machines
        spinnerDesignation.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            private var first = true
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position <= 0 || dialogDesignations.size < position) return
                val desigId = dialogDesignations[position - 1].id
                if (!first) {
                    // User changed designation manually → clear machine selection
                    selectedMachineIds.clear()
                    dialogMachines.clear()
                    refreshMachineDisplay()
                }
                first = false
                loadMachinesForDesignation(desigId)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Trigger initial load by selecting current department
        spinnerDepartment.setSelection(if (currentDeptIndex >= 0) currentDeptIndex + 1 else 0)

        // Setup Attendance Type Spinner
        val attTypes = listOf("Regular", "OT", "Cash")
        val attTypeAdapter = ArrayAdapter(this, R.layout.spinner_item_black, attTypes)
        attTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        spinnerAttType.adapter = attTypeAdapter

        val attTypeIndex = when (record.attType) {
            "R" -> 0
            "O" -> 1
            "C" -> 2
            else -> 0
        }
        spinnerAttType.setSelection(attTypeIndex)

        // ── Mc Nos selector ──
        layoutMachineNumbers.setOnClickListener {
            val pos = spinnerDesignation.selectedItemPosition
            if (pos <= 0 || dialogDesignations.size < pos) {
                Toast.makeText(this, "Please select designation first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val desigId = dialogDesignations[pos - 1].id
            showMachineSelectorDialog(desigId, dialogMachines, selectedMachineIds) {
                refreshMachineDisplay()
            }
        }

        // Cancel button
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        // Save button
        btnSave.setOnClickListener {
            val workingHours = etWorkingHours.text.toString().toDoubleOrNull() ?: 0.0
            val idleHours = etIdleHours.text.toString().toDoubleOrNull() ?: 0.0
            val selectedAttType = when (spinnerAttType.selectedItemPosition) {
                0 -> "R"
                1 -> "O"
                2 -> "C"
                else -> "R"
            }
            // Selected designation id
            val selDesigPos = spinnerDesignation.selectedItemPosition
            val designationId = if (selDesigPos > 0 && dialogDesignations.size >= selDesigPos)
                dialogDesignations[selDesigPos - 1].id else null
            // Selected department id
            val selDeptPos = spinnerDepartment.selectedItemPosition
            val departmentId = if (selDeptPos > 0 && realDepartments.size >= selDeptPos)
                realDepartments[selDeptPos - 1].id else null
            val machineIds = selectedMachineIds.toList()

            val request = com.example.slsHrms.api.UpdateAttendanceRequest(
                empCode        = record.empCode,
                attendanceDate = record.attendanceDate,
                attType        = selectedAttType,
                departmentId   = departmentId,
                shiftId        = null,
                designationId  = designationId,
                shiftHours     = record.shiftHours,
                workingHours   = workingHours,
                idleHours      = idleHours,
                machineIds     = machineIds
            )

            btnSave.isEnabled = false
            RetrofitClient.getApiService(this).updateAttendance(record.id, request)
                .enqueue(object : Callback<com.example.slsHrms.api.UpdateAttendanceResponse> {
                    override fun onResponse(
                        call: Call<com.example.slsHrms.api.UpdateAttendanceResponse>,
                        response: Response<com.example.slsHrms.api.UpdateAttendanceResponse>
                    ) {
                        btnSave.isEnabled = true
                        val body = response.body()
                        if (response.isSuccessful && body?.status == "success") {
                            showAlert("Success", body.message ?: "Attendance updated",
                                AlertType.SUCCESS)
                            alertDialog.dismiss()
                            // Refresh the list to show updated data
                            performSearch()
                        } else {
                            // On a 400 (leave / spell-hours conflict) the message
                            // is in errorBody, not body.
                            val errMsg = body?.message
                                ?: response.errorBody()?.string()
                                    ?.let { extractErrorMessage(it) }
                                ?: "Update failed (${response.code()})"
                            showAlert("Update Failed", errMsg)
                        }
                    }
                    override fun onFailure(
                        call: Call<com.example.slsHrms.api.UpdateAttendanceResponse>,
                        t: Throwable
                    ) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@AttendanceReportActivity,
                            "Network error: ${t.localizedMessage}",
                            Toast.LENGTH_SHORT).show()
                    }
                })
        }

        alertDialog.show()
    }

    // ── Machine Selector Dialog (shared with AttendanceActivity layout) ──
    private fun showMachineSelectorDialog(
        designationId: Int,
        machinesList: MutableList<com.example.slsHrms.api.Machine>,
        selectedMachineIds: MutableSet<Int>,
        onClosed: () -> Unit
    ) {
        val builder = android.app.AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_machine_selector, null)
        builder.setView(view)
        val dlg = builder.create()

        val etSearch = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchMachine)
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewMachines)
        val tvSelectedCount = view.findViewById<android.widget.TextView>(R.id.tvSelectedCount)
        val btnOk = view.findViewById<android.widget.Button>(R.id.btnOk)
        val btnCancel = view.findViewById<android.widget.Button>(R.id.btnCancel)

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val machineAdapter = com.example.slsHrms.adapter.MachineSelectionAdapter(
            machinesList,
            selectedMachineIds
        ) { count -> tvSelectedCount.text = "$count machine(s) selected" }
        recyclerView.adapter = machineAdapter

        // If list is empty, fetch from backend
        if (machinesList.isEmpty()) {
            RetrofitClient.getApiService(this).getMachines(designationId).enqueue(
                object : Callback<com.example.slsHrms.api.MachineResponse> {
                    override fun onResponse(
                        call: Call<com.example.slsHrms.api.MachineResponse>,
                        response: Response<com.example.slsHrms.api.MachineResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            val valid = (response.body()?.data ?: emptyList())
                                .filter { (it.id ?: 0) > 0 }
                            machinesList.clear()
                            machinesList.addAll(valid)
                            machineAdapter.updateMachines(machinesList)
                        }
                    }
                    override fun onFailure(call: Call<com.example.slsHrms.api.MachineResponse>, t: Throwable) {}
                }
            )
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                machineAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        tvSelectedCount.text = "${selectedMachineIds.size} machine(s) selected"

        btnOk.setOnClickListener {
            onClosed()
            dlg.dismiss()
        }
        btnCancel.setOnClickListener { dlg.dismiss() }
        dlg.show()
    }
}

