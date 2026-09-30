package com.example.slsHrms

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Leave entry: a form (EB No/Name, Leave Type, From/To, Non Working Days with
 * the computed Leave Days, Remarks, Save) over a read-only table of the
 * branch's leaves in the From/To filter, newest first. Saved as status 3.
 */
class LeaveEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId = 0
    private var userId   = 0
    private var leaveTypes = listOf<LeaveType>()

    // Form state
    private var ebId = 0
    private var from = ""
    private var to   = ""
    // Records filter
    private var filterFrom = ""
    private var filterTo   = ""

    private lateinit var etEbNo: EditText
    private lateinit var tvName: TextView
    private lateinit var spType: Spinner
    private lateinit var tvFrom: TextView
    private lateinit var tvTo: TextView
    private lateinit var etNonWorking: EditText
    private lateinit var tvLeaveDays: TextView
    private lateinit var etRemarks: EditText
    private lateinit var btnSave: Button
    private lateinit var tvFilterFrom: TextView
    private lateinit var tvFilterTo: TextView
    private lateinit var rows: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leave_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        etEbNo       = findViewById(R.id.etEbNo)
        tvName       = findViewById(R.id.tvName)
        spType       = findViewById(R.id.spLeaveType)
        tvFrom       = findViewById(R.id.tvFrom)
        tvTo         = findViewById(R.id.tvTo)
        etNonWorking = findViewById(R.id.etNonWorking)
        tvLeaveDays  = findViewById(R.id.tvLeaveDays)
        etRemarks    = findViewById(R.id.etRemarks)
        btnSave      = findViewById(R.id.btnSave)
        tvFilterFrom = findViewById(R.id.tvFilterFrom)
        tvFilterTo   = findViewById(R.id.tvFilterTo)
        rows         = findViewById(R.id.rowsContainer)
        progressBar  = findViewById(R.id.progressBar)
        tvEmpty      = findViewById(R.id.tvEmpty)
        tvTotal      = findViewById(R.id.tvTotal)

        // ── Form ──
        // Look the name up ~0.6 s after typing stops: tapping the Leave Type
        // spinner never takes focus from this box, so focus-loss alone missed it.
        val lookupSoon = Runnable { lookup() }
        etEbNo.doAfterTextChanged {
            ebId = 0; tvName.text = ""
            etEbNo.removeCallbacks(lookupSoon)
            if (!it.isNullOrBlank()) etEbNo.postDelayed(lookupSoon, 600)
        }
        etEbNo.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) lookup() }
        etEbNo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) lookup()
            false
        }
        tvFrom.setOnClickListener {
            pickDate(from) { from = it; if (to < it) to = it; showFormDates() }
        }
        tvTo.setOnClickListener {
            pickDate(to) {
                if (it < from) showAlert("To Date", "To Date cannot be before From Date", AlertType.WARNING)
                else { to = it; showFormDates() }
            }
        }
        etNonWorking.doAfterTextChanged { showLeaveDays() }
        btnSave.setOnClickListener { save() }
        resetForm()

        // ── Records filter: this month ──
        val cal = Calendar.getInstance()
        filterTo = apiDate.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        filterFrom = apiDate.format(cal.time)
        tvFilterFrom.text = disp(filterFrom)
        tvFilterTo.text   = disp(filterTo)
        tvFilterFrom.setOnClickListener { pickDate(filterFrom) { filterFrom = it; tvFilterFrom.text = disp(it) } }
        tvFilterTo.setOnClickListener   { pickDate(filterTo)   { filterTo = it;   tvFilterTo.text = disp(it) } }
        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            if (filterTo < filterFrom) showAlert("Filter", "To Date cannot be before From Date", AlertType.WARNING)
            else loadRecords()
        }

        if (branchId <= 0) showAlert("Missing Branch", "Select a branch on the dashboard first", AlertType.WARNING)
        loadLeaveTypes()
        loadRecords()
    }

    private fun disp(api: String?) =
        if (api.isNullOrEmpty()) "" else try { dispDate.format(apiDate.parse(api)!!) } catch (_: Exception) { api }

    private fun pickDate(current: String, onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        try { apiDate.parse(current)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            onPicked(apiDate.format(cal.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── Form helpers ───────────────────────────────────────────────────────────

    private fun resetForm() {
        etEbNo.setText("")                      // its watcher clears ebId + name
        from = apiDate.format(Calendar.getInstance().time)
        to = from
        showFormDates()
        etNonWorking.setText("0")
        etRemarks.setText("")
        if (spType.adapter != null) spType.setSelection(0)
        etEbNo.requestFocus()
    }

    private fun showFormDates() {
        tvFrom.text = disp(from)
        tvTo.text   = disp(to)
        showLeaveDays()
    }

    private fun totalDays() =
        ((apiDate.parse(to)!!.time - apiDate.parse(from)!!.time) / 86_400_000L + 1).toInt()

    private fun nonWorking() = etNonWorking.text.toString().trim().ifEmpty { "0" }.toIntOrNull() ?: 0

    /** Leave Days = To − From + 1 − Non Working Days. */
    private fun leaveDays() = totalDays() - nonWorking()

    private fun showLeaveDays() {
        val days = leaveDays()
        tvLeaveDays.text = days.toString()
        tvLeaveDays.setTextColor(if (days < 1) Color.parseColor("#C62828") else Color.parseColor("#1B5E20"))
    }

    // ── Masters ────────────────────────────────────────────────────────────────

    private fun loadLeaveTypes() {
        RetrofitClient.getApiService(this).getLeaveTypes(branchId.takeIf { it > 0 })
            .enqueue(object : Callback<LeaveTypeResponse> {
                override fun onResponse(call: Call<LeaveTypeResponse>, response: Response<LeaveTypeResponse>) {
                    leaveTypes = response.body()?.leaveTypes.orEmpty()
                    spType.adapter = ArrayAdapter(this@LeaveEntryActivity, R.layout.spinner_item_black,
                        listOf("Select Leave Type") + leaveTypes.map { it.name })
                        .also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (leaveTypes.isEmpty()) {
                        Toast.makeText(this@LeaveEntryActivity, "No leave types found", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<LeaveTypeResponse>, t: Throwable) {
                    Toast.makeText(this@LeaveEntryActivity, "Error loading leave types", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── EB No lookup ───────────────────────────────────────────────────────────

    private fun lookup(then: (() -> Unit)? = null) {
        val code = etEbNo.text.toString().trim()
        if (code.isEmpty()) return
        if (ebId > 0) { then?.invoke(); return }
        tvName.text = "Searching…"
        RetrofitClient.getApiService(this).searchEmployees(code, branchId.takeIf { it > 0 })
            .enqueue(object : Callback<EmployeeResponse> {
                override fun onResponse(call: Call<EmployeeResponse>, response: Response<EmployeeResponse>) {
                    // Exact EB No only — a partial match would book leave to the wrong person.
                    val emp = response.body()?.employees?.firstOrNull { it.empCode.equals(code, ignoreCase = true) }
                    if (etEbNo.text.toString().trim() != code) return   // typed on since
                    ebId = emp?.id ?: 0
                    tvName.text = emp?.name ?: "Not found in this branch"
                    if (emp != null) then?.invoke()
                }
                override fun onFailure(call: Call<EmployeeResponse>, t: Throwable) {
                    tvName.text = "Lookup failed"
                }
            })
    }

    // ── Save (status 3 on the server) ──────────────────────────────────────────

    private fun save() {
        if (branchId <= 0) {
            showAlert("Missing Branch", "Select a branch on the dashboard first", AlertType.WARNING); return
        }
        if (etEbNo.text.isNullOrBlank()) {
            showAlert("Missing EB No", "Enter the EB No", AlertType.WARNING); return
        }
        if (ebId <= 0) {                    // lookup not finished yet: finish it, then save
            lookup { save() }; return
        }
        val type = leaveTypes.getOrNull(spType.selectedItemPosition - 1)   // 0 = "Select Leave Type"
        if (type?.id == null) {
            showAlert("Missing Leave Type", "Select a leave type", AlertType.WARNING); return
        }
        if (leaveDays() < 1) {
            showAlert("Leave Days", "Leave days must be at least 1 — reduce Non Working Days", AlertType.WARNING); return
        }

        btnSave.isEnabled = false
        RetrofitClient.getApiService(this).saveLeaveTransaction(
            LeaveSaveRequest(ebId, userId, type.id, from, to, nonWorking(),
                etRemarks.text.toString().trim(), branchId)
        ).enqueue(object : Callback<LeaveSaveResponse> {
            override fun onResponse(call: Call<LeaveSaveResponse>, response: Response<LeaveSaveResponse>) {
                btnSave.isEnabled = true
                val body = response.body()
                if (response.isSuccessful && body?.status == "success") {
                    showAlert("Saved", "Leave saved for ${tvName.text} (${leaveDays()} day(s))", AlertType.SUCCESS)
                    resetForm()
                    loadRecords()
                } else {
                    // On an HTTP error the message is in errorBody, not body.
                    val msg = body?.message
                        ?: response.errorBody()?.string()?.let { extractErrorMessage(it) }
                        ?: "Save failed (${response.code()})"
                    showAlert("Leave Not Saved", msg)
                }
            }
            override fun onFailure(call: Call<LeaveSaveResponse>, t: Throwable) {
                btnSave.isEnabled = true
                showAlert("Network Error", t.localizedMessage ?: "Request failed")
            }
        })
    }

    // ── Records table (newest first, from the server) ──────────────────────────

    private fun loadRecords() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        RetrofitClient.getApiService(this).getLeaveTransactions(
            branchId = branchId.takeIf { it > 0 }, fromDate = filterFrom, toDate = filterTo
        ).enqueue(object : Callback<LeaveListResponse> {
            override fun onResponse(call: Call<LeaveListResponse>, response: Response<LeaveListResponse>) {
                progressBar.visibility = View.GONE
                if (!response.isSuccessful) {
                    Toast.makeText(this@LeaveEntryActivity, "Could not load records (${response.code()})", Toast.LENGTH_SHORT).show()
                }
                showRecords(response.body()?.transactions.orEmpty())
            }
            override fun onFailure(call: Call<LeaveListResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@LeaveEntryActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                showRecords(emptyList())
            }
        })
    }

    private fun showRecords(list: List<LeaveTransaction>) {
        rows.removeAllViews()
        val inflater = LayoutInflater.from(this)
        var totalDays = 0
        list.forEachIndexed { i, t ->
            val v = inflater.inflate(R.layout.item_leave_record, rows, false)
            val days = try {
                ((apiDate.parse(t.toDate!!)!!.time - apiDate.parse(t.fromDate!!)!!.time) / 86_400_000L + 1).toInt() -
                    (t.nonWorkingDays ?: 0)
            } catch (_: Exception) { 0 }
            if (t.statusId == 3) totalDays += days
            v.findViewById<TextView>(R.id.tvEbNo).text    = t.empCode ?: ""
            v.findViewById<TextView>(R.id.tvName).text    = t.empName ?: ""
            v.findViewById<TextView>(R.id.tvType).text    = t.leaveType ?: "-"
            v.findViewById<TextView>(R.id.tvFrom).text    = disp(t.fromDate)
            v.findViewById<TextView>(R.id.tvTo).text      = disp(t.toDate)
            v.findViewById<TextView>(R.id.tvNonWkg).text  = (t.nonWorkingDays ?: 0).toString()
            v.findViewById<TextView>(R.id.tvDays).text    = days.toString()
            v.findViewById<TextView>(R.id.tvRemarks).text = t.remarks ?: ""
            v.findViewById<TextView>(R.id.tvStatus).apply {
                text = t.status ?: t.statusId?.toString() ?: ""
                setTextColor(Color.parseColor(when (t.statusId) {
                    3 -> "#2E7D32"; 4, 6 -> "#C62828"; else -> "#EF6C00"
                }))
            }
            if (i % 2 == 1) v.setBackgroundColor(0xFFF7F9FC.toInt())   // zebra rows
            rows.addView(v)
        }
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        tvTotal.text = "Records: ${list.size}   Approved leave days: $totalDays   (${disp(filterFrom)} to ${disp(filterTo)})"
    }
}
