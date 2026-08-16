package com.example.slsHrms

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.slsHrms.adapter.AttendanceChecklistAdapter
import com.example.slsHrms.api.*
import com.example.slsHrms.databinding.ActivityAttendanceChecklistBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceChecklistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceChecklistBinding
    private lateinit var adapter: AttendanceChecklistAdapter

    private val apiDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var fromDateApi = ""
    private var toDateApi = ""
    private var branchId: Int = 0
    private var coId: Int = 0

    private var selectedDeptId: Int? = null

    private val spells = mutableListOf<Shift>()
    private val departments = mutableListOf<Department>()
    private val designations = mutableListOf<Designation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceChecklistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        coId     = intent.getIntExtra("CO_ID", 0)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Default dates: today
        val cal = Calendar.getInstance()
        fromDateApi = apiDate.format(cal.time)
        toDateApi   = apiDate.format(cal.time)
        binding.tvFromDate.text = dispDate.format(cal.time)
        binding.tvToDate.text   = dispDate.format(cal.time)

        setupDatePickers()
        setupRecyclerView()
        loadDropdowns()

        binding.btnSearch.setOnClickListener { loadReport() }
        binding.etEbNo.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { loadReport(); true } else false
        }
    }

    // ── Date Pickers ─────────────────────────────────────────────

    private fun setupDatePickers() {
        binding.btnFromDate.setOnClickListener { pickDate(true) }
        binding.btnToDate.setOnClickListener   { pickDate(false) }
    }

    private fun pickDate(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        val d   = if (isFrom) fromDateApi else toDateApi
        try { val parsed = apiDate.parse(d); if (parsed != null) cal.time = parsed } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, day ->
            cal.set(y, m, day)
            val api  = apiDate.format(cal.time)
            val disp = dispDate.format(cal.time)
            if (isFrom) { fromDateApi = api; binding.tvFromDate.text = disp }
            else        { toDateApi   = api; binding.tvToDate.text   = disp }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── RecyclerView ─────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = AttendanceChecklistAdapter(emptyList()) {
            AttendanceViewActivity.start(this, it, branchId)
        }
        binding.rvChecklist.layoutManager = LinearLayoutManager(this)
        binding.rvChecklist.adapter       = adapter
        binding.rvChecklist.itemAnimator  = null
    }

    // ── Load dropdowns ────────────────────────────────────────────

    private fun loadDropdowns() {
        // Spells
        RetrofitClient.getApiService(this).getShifts(if (branchId > 0) branchId else null)
            .enqueue(object : Callback<ShiftResponse> {
                override fun onResponse(call: Call<ShiftResponse>, response: Response<ShiftResponse>) {
                    spells.clear()
                    spells.add(Shift(0, name = "All Spells"))
                    spells.addAll(response.body()?.shifts.orEmpty())
                    binding.spSpell.adapter = ArrayAdapter(this@AttendanceChecklistActivity,
                        R.layout.spinner_item_black, spells).also {
                        it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                }
                override fun onFailure(call: Call<ShiftResponse>, t: Throwable) {}
            })

        // Departments
        RetrofitClient.getApiService(this).getDepartments(
            coId = if (coId > 0) coId else null,
            branchId = if (branchId > 0) branchId else null)
            .enqueue(object : Callback<DepartmentResponse> {
                override fun onResponse(call: Call<DepartmentResponse>, response: Response<DepartmentResponse>) {
                    departments.clear()
                    departments.add(Department(0, "All Departments"))
                    departments.addAll(response.body()?.departments.orEmpty())
                    binding.spDepartment.adapter = ArrayAdapter(this@AttendanceChecklistActivity,
                        R.layout.spinner_item_black, departments).also {
                        it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }

                    binding.spDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                            val dept = departments.getOrNull(pos)
                            selectedDeptId = if ((dept?.id ?: 0) > 0) dept?.id else null
                            loadDesignations()
                        }
                        override fun onNothingSelected(p: AdapterView<*>?) {}
                    }
                }
                override fun onFailure(call: Call<DepartmentResponse>, t: Throwable) {}
            })
    }

    private fun loadDesignations() {
        if (branchId <= 0) return
        RetrofitClient.getApiService(this).getDesignations(branchId, selectedDeptId)
            .enqueue(object : Callback<DesignationResponse> {
                override fun onResponse(call: Call<DesignationResponse>, response: Response<DesignationResponse>) {
                    designations.clear()
                    designations.add(Designation(0, "All Designations"))
                    designations.addAll(response.body()?.designations.orEmpty())
                    binding.spDesignation.adapter = ArrayAdapter(this@AttendanceChecklistActivity,
                        R.layout.spinner_item_black, designations).also {
                        it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                }
                override fun onFailure(call: Call<DesignationResponse>, t: Throwable) {}
            })
    }

    // ── Load Report ───────────────────────────────────────────────

    private fun loadReport() {
        // Covers both entry points — the keyboard would otherwise sit over the results.
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.root.windowToken, 0)

        binding.progressBar.visibility        = View.VISIBLE
        binding.hsvTable.visibility           = View.GONE
        binding.layoutEmpty.visibility        = View.GONE
        binding.layoutSummary.visibility      = View.GONE
        binding.layoutDesigSummary.visibility = View.GONE

        val spellSel = spells.getOrNull(binding.spSpell.selectedItemPosition)
        val spellId  = if ((spellSel?.id ?: 0) > 0) spellSel?.id else null

        val deptSel = departments.getOrNull(binding.spDepartment.selectedItemPosition)
        selectedDeptId = if ((deptSel?.id ?: 0) > 0) deptSel?.id else null

        val desigSel = designations.getOrNull(binding.spDesignation.selectedItemPosition)
        val desigId  = if ((desigSel?.id ?: 0) > 0) desigSel?.id else null

        // Blank = no filter. The backend matches emp_code with LIKE %..%, so a
        // partial EB No is a valid search.
        val ebNo = binding.etEbNo.text.toString().trim().ifBlank { null }

        RetrofitClient.getApiService(this).getAttendanceReportRange(
            fromDate      = fromDateApi,
            toDate        = toDateApi,
            departmentId  = selectedDeptId,
            empCode       = ebNo,
            branchId      = if (branchId > 0) branchId else null,
            spellId       = spellId,
            designationId = desigId
        ).enqueue(object : Callback<AttendanceReportResponse> {
            override fun onResponse(call: Call<AttendanceReportResponse>,
                                    response: Response<AttendanceReportResponse>) {
                binding.progressBar.visibility = View.GONE
                val data = response.body()
                if (!response.isSuccessful || data == null || data.status != "success") {
                    Toast.makeText(this@AttendanceChecklistActivity,
                        "Failed: ${data?.message ?: response.message()}", Toast.LENGTH_SHORT).show()
                    binding.layoutEmpty.visibility = View.VISIBLE
                    return
                }
                val records = data.data.orEmpty()
                if (records.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    return
                }

                adapter.update(records)
                binding.tvTotalRecords.text = "Records: ${records.size}"
                binding.tvDateRange.text = "${binding.tvFromDate.text} to ${binding.tvToDate.text}"
                binding.layoutSummary.visibility = View.VISIBLE
                binding.hsvTable.visibility      = View.VISIBLE
                showDesignationSummary(records)
            }

            override fun onFailure(call: Call<AttendanceReportResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this@AttendanceChecklistActivity,
                    "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Designation-wise summary (No of Hands) ────────────────────
    // hands = (working_hours - idle_hours) / spell_hours, bucketed R | O | C

    private fun showDesignationSummary(records: List<AttendanceRecord>) {
        val rowsBox = binding.llDesigRows
        rowsBox.removeAllViews()

        // Leave is not attendance: it contributes no hands, and the person must
        // not show up in the head count either. Filtering here rather than in
        // the loop keeps them out of the (n) counts and the TOTAL alike.
        val worked = records.filter { (it.attType ?: "R").trim().uppercase() != "L" }

        fun hands(r: AttendanceRecord): Double {
            val sh = r.shiftHours ?: 0.0
            if (sh <= 0.0) return 0.0
            return ((r.workingHours ?: 0.0) - (r.idleHours ?: 0.0)) / sh
        }

        val types = listOf("R", "O", "C")
        val grand = DoubleArray(3)
        worked.groupBy { it.designationName?.trim().takeUnless { n -> n.isNullOrEmpty() } ?: "—" }
            .toSortedMap()
            .forEach { (desig, recs) ->
                val v = DoubleArray(3)
                recs.forEach { r ->
                    val i = types.indexOf((r.attType ?: "R").trim().uppercase())
                    if (i >= 0) v[i] += hands(r)
                }
                for (i in 0..2) grand[i] += v[i]
                val empCount = recs.distinctBy { it.ebId ?: it.empCode }.size
                rowsBox.addView(summaryRow("$desig ($empCount)", v, bold = false))
            }
        val totalEmp = worked.distinctBy { it.ebId ?: it.empCode }.size
        rowsBox.addView(summaryRow("TOTAL ($totalEmp)", grand, bold = true))
        binding.layoutDesigSummary.visibility = View.VISIBLE
    }

    private fun summaryRow(label: String, v: DoubleArray, bold: Boolean): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(dp(6), dp(5), dp(6), dp(5))
            if (bold) setBackgroundColor(0xFFE3F2FD.toInt())
        }
        fun cell(text: String, weight: Float, alignEnd: Boolean) = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            this.text = text
            textSize = 11f
            setTextColor(0xFF212121.toInt())
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = if (alignEnd) Gravity.END else Gravity.START
        }
        fun fmt(d: Double) = if (d == 0.0) "-" else String.format(Locale.US, "%.2f", d)
        row.addView(cell(label, 2f, false))
        v.forEach { row.addView(cell(fmt(it), 1f, true)) }
        row.addView(cell(fmt(v.sum()), 1.2f, true))
        return row
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
