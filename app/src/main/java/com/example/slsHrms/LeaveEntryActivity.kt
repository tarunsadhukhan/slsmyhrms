package com.example.slsHrms

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.LeaveTransactionAdapter
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LeaveEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId = 0
    private var userId   = 0          // logged-in user_id

    private var filterFromDate = ""
    private var filterToDate   = ""
    private var filterEbId     = 0

    private lateinit var adapter: LeaveTransactionAdapter
    private lateinit var rvLeave: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvFromDate: TextView
    private lateinit var tvToDate: TextView
    private lateinit var etFilterEmpCode: EditText
    private lateinit var tvFilterEmpName: TextView

    // Masters loaded from API
    private val leaveTypeList = mutableListOf<LeaveType>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leave_v2)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        rvLeave         = findViewById(R.id.rvLeave)
        progressBar     = findViewById(R.id.progressBar)
        tvEmpty         = findViewById(R.id.tvEmpty)
        tvFromDate      = findViewById(R.id.tvFromDate)
        tvToDate        = findViewById(R.id.tvToDate)
        etFilterEmpCode = findViewById(R.id.etEmpCode)
        tvFilterEmpName = findViewById(R.id.tvFilterEmpName)

        // Default date range: this month
        val cal = Calendar.getInstance()
        filterToDate = apiDate.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        filterFromDate = apiDate.format(cal.time)
        tvFromDate.text = dispDate.format(apiDate.parse(filterFromDate)!!)
        tvToDate.text   = dispDate.format(apiDate.parse(filterToDate)!!)

        // Date pickers
        findViewById<View>(R.id.btnFromDate).setOnClickListener { pickDate(true) }
        findViewById<View>(R.id.btnToDate).setOnClickListener   { pickDate(false) }

        // Employee search wiring
        val btnSearchEmpFilter = findViewById<android.widget.ImageView>(R.id.btnSearchEmp)
        btnSearchEmpFilter.setOnClickListener { lookupFilterEmployee() }
        etFilterEmpCode.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) lookupFilterEmployee() }
        etFilterEmpCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                lookupFilterEmployee(); true
            } else false
        }

        // RecyclerView
        adapter = LeaveTransactionAdapter(emptyList(),
            onEdit   = { showLeaveDialog(it) },
            onDelete = { confirmDelete(it) })
        rvLeave.layoutManager = LinearLayoutManager(this)
        rvLeave.adapter = adapter

        // Search
        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val empCode = etFilterEmpCode.text.toString().trim().takeIf { it.isNotEmpty() }
            loadLeaves(empCode)
        }

        // Add New
        findViewById<Button>(R.id.btnAddNew).setOnClickListener { showLeaveDialog(null) }

        // Load masters then initial list
        loadLeaveTypes()
        loadLeaves(null)
    }

    // ── Employee lookup for filter ──────────────────────────────────────────────
    private fun lookupFilterEmployee() {
        val code = etFilterEmpCode.text.toString().trim()
        if (code.isEmpty()) { tvFilterEmpName.text = ""; filterEbId = 0; return }
        tvFilterEmpName.text = "Searching…"
        RetrofitClient.getApiService(this).searchEmployees(code, if (branchId > 0) branchId else null)
            .enqueue(object : Callback<EmployeeResponse> {
                override fun onResponse(call: Call<EmployeeResponse>, response: Response<EmployeeResponse>) {
                    val emp = response.body()?.employees?.firstOrNull {
                        it.empCode.equals(code, ignoreCase = true)
                    } ?: response.body()?.employees?.firstOrNull()
                    if (emp != null) {
                        filterEbId = emp.id
                        tvFilterEmpName.text = emp.name
                    } else {
                        filterEbId = 0
                        tvFilterEmpName.text = "Not found"
                    }
                }
                override fun onFailure(call: Call<EmployeeResponse>, t: Throwable) {
                    filterEbId = 0; tvFilterEmpName.text = "Lookup failed"
                }
            })
    }

    // ── Date Pickers ───────────────────────────────────────────────────────────

    private fun pickDate(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        val src = if (isFrom) filterFromDate else filterToDate
        try { apiDate.parse(src)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            val api  = apiDate.format(cal.time)
            val disp = dispDate.format(cal.time)
            if (isFrom) { filterFromDate = api; tvFromDate.text = disp }
            else        { filterToDate   = api; tvToDate.text   = disp }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── Load Leave Transactions ────────────────────────────────────────────────

    private fun loadLeaves(empCode: String?) {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility     = View.GONE
        rvLeave.visibility     = View.GONE

        RetrofitClient.getApiService(this).getLeaveTransactions(
            branchId = if (branchId > 0) branchId else null,
            fromDate = filterFromDate,
            toDate   = filterToDate,
            empCode  = empCode,
            statusId = null
        ).enqueue(object : Callback<LeaveListResponse> {
            override fun onResponse(call: Call<LeaveListResponse>, response: Response<LeaveListResponse>) {
                progressBar.visibility = View.GONE
                val list = response.body()?.transactions.orEmpty()
                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rvLeave.visibility = View.VISIBLE
                    adapter.update(list)
                }
            }
            override fun onFailure(call: Call<LeaveListResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@LeaveEntryActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Load Leave Types ───────────────────────────────────────────────────────

    private fun loadLeaveTypes() {
        RetrofitClient.getApiService(this).getLeaveTypes()
            .enqueue(object : Callback<LeaveTypeResponse> {
                override fun onResponse(call: Call<LeaveTypeResponse>, response: Response<LeaveTypeResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.leaveTypes?.let {
                            leaveTypeList.clear()
                            leaveTypeList.addAll(it)
                        }
                    }
                }
                override fun onFailure(call: Call<LeaveTypeResponse>, t: Throwable) {
                    Toast.makeText(this@LeaveEntryActivity, "Error loading leave types", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Add / Edit Dialog ──────────────────────────────────────────────────────

    private fun showLeaveDialog(existing: LeaveTransaction?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_leave_entry, null)

        val etEmpCode    = view.findViewById<EditText>(R.id.etDialogEmpCode)
        val tvEmpName    = view.findViewById<TextView>(R.id.tvDialogEmpName)
        val btnSearchEmp = view.findViewById<android.widget.ImageView>(R.id.btnSearchEmp)
        val spLeaveType  = view.findViewById<Spinner>(R.id.spDialogLeaveType)
        val tvFrom       = view.findViewById<TextView>(R.id.tvDialogFromDate)
        val tvTo         = view.findViewById<TextView>(R.id.tvDialogToDate)
        val btnFrom      = view.findViewById<View>(R.id.btnDialogFromDate)
        val btnTo        = view.findViewById<View>(R.id.btnDialogToDate)
        val etDays       = view.findViewById<EditText>(R.id.etDialogDays)
        val etReason     = view.findViewById<EditText>(R.id.etDialogReason)

        // Leave type spinner
        val leaveTypeNames = leaveTypeList.map { it.name }
        spLeaveType.adapter = ArrayAdapter(this, R.layout.spinner_item_black, leaveTypeNames)
            .also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }


        var dialogEbId = existing?.ebId ?: 0
        var dialogFrom = existing?.fromDate ?: ""
        var dialogTo   = existing?.toDate   ?: ""

        // Helper: recalculate no_of_days whenever from/to changes
        fun calcDays() {
            if (dialogFrom.isNotEmpty() && dialogTo.isNotEmpty()) {
                try {
                    val d1 = apiDate.parse(dialogFrom)!!
                    val d2 = apiDate.parse(dialogTo)!!
                    val days = (d2.time - d1.time) / 86400000L + 1
                    etDays.setText(if (days > 0) days.toString() else "1")
                } catch (_: Exception) { etDays.setText("") }
            } else {
                etDays.setText("")
            }
        }

        // Pre-fill if editing
        existing?.let { t ->
            etEmpCode.setText(t.empCode ?: "")
            tvEmpName.text = t.empName ?: ""
            val ltIdx = leaveTypeList.indexOfFirst { it.id == t.leaveTypeId }
            if (ltIdx >= 0) spLeaveType.setSelection(ltIdx)
            tvFrom.text = t.fromDate?.let { safeReformat(it) } ?: ""
            tvTo.text   = t.toDate?.let   { safeReformat(it) } ?: ""
            etDays.setText(t.noOfDays?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: "")
            etReason.setText(t.reason ?: "")
        }

        // ── Employee lookup ────────────────────────────────────────────────────
        fun lookupEmployee() {
            val code = etEmpCode.text.toString().trim()
            if (code.isEmpty()) { tvEmpName.text = ""; dialogEbId = 0; return }
            tvEmpName.text = "Searching…"
            RetrofitClient.getApiService(this).searchEmployees(code, if (branchId > 0) branchId else null)
                .enqueue(object : Callback<EmployeeResponse> {
                    override fun onResponse(call: Call<EmployeeResponse>, response: Response<EmployeeResponse>) {
                        val emp = response.body()?.employees?.firstOrNull {
                            it.empCode.equals(code, ignoreCase = true)
                        } ?: response.body()?.employees?.firstOrNull()
                        if (emp != null) {
                            dialogEbId = emp.id
                            tvEmpName.text = emp.name
                        } else {
                            dialogEbId = 0
                            tvEmpName.text = "Not found"
                            Toast.makeText(this@LeaveEntryActivity, "Employee not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<EmployeeResponse>, t: Throwable) {
                        dialogEbId = 0; tvEmpName.text = "Lookup failed"
                    }
                })
        }

        btnSearchEmp.setOnClickListener { lookupEmployee() }
        etEmpCode.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) lookupEmployee() }
        etEmpCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) { lookupEmployee(); true } else false
        }

        // ── Date pickers ───────────────────────────────────────────────────────
        fun pickDialogDate(isFrom: Boolean) {
            val cal = Calendar.getInstance()
            val src = if (isFrom) dialogFrom else dialogTo
            try { apiDate.parse(src)?.let { cal.time = it } } catch (_: Exception) {}
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                val api  = apiDate.format(cal.time)
                val disp = dispDate.format(cal.time)
                if (isFrom) { dialogFrom = api; tvFrom.text = disp }
                else        { dialogTo   = api; tvTo.text   = disp }
                calcDays()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnFrom.setOnClickListener { pickDialogDate(true) }
        btnTo.setOnClickListener   { pickDialogDate(false) }

        val title = if (existing == null) "Add Leave Entry" else "Edit Leave Entry"
        AlertDialog.Builder(this, R.style.DarkDialogTheme)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                if (dialogEbId <= 0) {
                    showAlert("Missing Employee", "Please lookup a valid employee", AlertType.WARNING)
                    return@setPositiveButton
                }
                if (dialogFrom.isEmpty() || dialogTo.isEmpty()) {
                    showAlert("Missing Dates", "Please select dates", AlertType.WARNING)
                    return@setPositiveButton
                }
                val selectedLeaveType = leaveTypeList.getOrNull(spLeaveType.selectedItemPosition)

                if (selectedLeaveType == null || selectedLeaveType.id == null) {
                    showAlert("Missing Leave Type", "Please select a leave type", AlertType.WARNING)
                    return@setPositiveButton
                }

                val noOfDays = etDays.text.toString().trim().toDoubleOrNull() ?: 0.0

                val req = LeaveSaveRequest(
                    ebId         = dialogEbId,
                    userId       = userId,
                    leaveTypeId  = selectedLeaveType.id,
                    fromDate     = dialogFrom,
                    toDate       = dialogTo,
                    noOfDays     = noOfDays,
                    reason       = etReason.text.toString().trim(),
                    remarks      = "",
                    statusId     = 3,
                    branchId     = if (branchId > 0) branchId else null,
                    details      = buildDetails(dialogFrom, dialogTo)
                )
                saveLeave(req)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun safeReformat(apiDateStr: String): String =
        try { dispDate.format(apiDate.parse(apiDateStr)!!) } catch (_: Exception) { apiDateStr }

    private fun buildDetails(from: String, to: String): List<LeaveTranDetailRequest> {
        val result = mutableListOf<LeaveTranDetailRequest>()
        try {
            val cal = Calendar.getInstance()
            cal.time = apiDate.parse(from)!!
            val end  = apiDate.parse(to)!!
            while (!cal.time.after(end)) {
                result.add(LeaveTranDetailRequest(leaveDate = apiDate.format(cal.time)))
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
        } catch (_: Exception) {}
        return result
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    private fun saveLeave(req: LeaveSaveRequest) {
        RetrofitClient.getApiService(this).saveLeaveTransaction(req)
            .enqueue(object : Callback<LeaveSaveResponse> {
                override fun onResponse(call: Call<LeaveSaveResponse>, response: Response<LeaveSaveResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.status == "success") {
                        showAlert("Success", body.message ?: "Leave saved", AlertType.SUCCESS)
                        loadLeaves(null)
                    } else {
                        // On an HTTP error the message is in errorBody, not body.
                        val errMsg = body?.message
                            ?: response.errorBody()?.string()?.let { extractErrorMessage(it) }
                            ?: "Save failed (${response.code()})"
                        showAlert("Leave Not Saved", errMsg)
                    }
                }
                override fun onFailure(call: Call<LeaveSaveResponse>, t: Throwable) {
                    showAlert("Network Error", t.localizedMessage ?: "Request failed")
                }
            })
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    private fun confirmDelete(item: LeaveTransaction) {
        AlertDialog.Builder(this, R.style.DarkDialogTheme)
            .setTitle("Delete Leave")
            .setMessage("Delete leave entry for ${item.empName ?: item.empCode}?")
            .setPositiveButton("Delete") { _, _ ->
                val id = item.id ?: return@setPositiveButton
                RetrofitClient.getApiService(this).deleteLeaveTransaction(id)
                    .enqueue(object : Callback<LeaveSaveResponse> {
                        override fun onResponse(call: Call<LeaveSaveResponse>, response: Response<LeaveSaveResponse>) {
                            val body = response.body()
                            if (response.isSuccessful && body?.status == "success") {
                                showAlert("Deleted", body.message ?: "Leave entry deleted",
                                    AlertType.SUCCESS)
                            } else {
                                val errMsg = body?.message
                                    ?: response.errorBody()?.string()?.let { extractErrorMessage(it) }
                                    ?: "Delete failed (${response.code()})"
                                showAlert("Delete Failed", errMsg)
                            }
                            loadLeaves(null)
                        }
                        override fun onFailure(call: Call<LeaveSaveResponse>, t: Throwable) {
                            showAlert("Network Error", t.localizedMessage ?: "Request failed")
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
