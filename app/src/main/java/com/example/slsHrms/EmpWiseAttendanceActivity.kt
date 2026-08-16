package com.example.slsHrms

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.EmpAttFixedAdapter
import com.example.slsHrms.adapter.EmpAttScrollableAdapter
import com.example.slsHrms.api.*
import com.example.slsHrms.databinding.ActivityEmpWiseAttendanceBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EmpWiseAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmpWiseAttendanceBinding
    private lateinit var fixedAdapter: EmpAttFixedAdapter
    private lateinit var scrollableAdapter: EmpAttScrollableAdapter

    private val apiDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var fromDateApi = ""
    private var toDateApi = ""
    private var branchId: Int = 0
    private var coId: Int = 0

    private var selectedSpellId: Int? = null
    private var selectedDeptId: Int? = null
    private var selectedDesigId: Int? = null

    private val spells = mutableListOf<Shift>()
    private val departments = mutableListOf<Department>()
    private val designations = mutableListOf<Designation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmpWiseAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        coId     = intent.getIntExtra("CO_ID", 0)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Default dates: 1st of current month to today
        val cal = Calendar.getInstance()
        toDateApi   = apiDate.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        fromDateApi = apiDate.format(cal.time)

        binding.tvFromDate.text = dispDate.format(apiDate.parse(fromDateApi)!!)
        binding.tvToDate.text   = dispDate.format(apiDate.parse(toDateApi)!!)

        setupDatePickers()
        setupReportTypeSpinner()
        setupAttTypeSpinner()
        setupRecyclerViews()
        loadDropdowns()
        syncScroll()

        binding.btnSearch.setOnClickListener { loadReport() }
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

    // ── Report Type Spinner ───────────────────────────────────────

    private fun setupReportTypeSpinner() {
        val types = listOf("Date Wise", "FN Wise", "Monthly")
        binding.spReportType.adapter = ArrayAdapter(this,
            R.layout.spinner_item_black, types).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
    }

    // ── Att Type Spinner ─────────────────────────────────────────

    private fun setupAttTypeSpinner() {
        val types = listOf("All", "Regular (R)", "OT (O)", "Cash (C)")
        binding.spAttType.adapter = ArrayAdapter(this,
            R.layout.spinner_item_black, types).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
    }

    // ── RecyclerViews ─────────────────────────────────────────────

    private fun setupRecyclerViews() {
        fixedAdapter     = EmpAttFixedAdapter(emptyList())
        scrollableAdapter = EmpAttScrollableAdapter(emptyList(), emptyList())

        binding.rvFixed.layoutManager = LinearLayoutManager(this)
        binding.rvFixed.adapter       = fixedAdapter
        binding.rvFixed.itemAnimator  = null

        binding.rvScrollable.layoutManager = LinearLayoutManager(this)
        binding.rvScrollable.adapter       = scrollableAdapter
        binding.rvScrollable.itemAnimator  = null
    }

    // ── Sync vertical scroll between two RecyclerViews ───────────

    private fun syncScroll() {
        binding.rvFixed.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                binding.rvScrollable.scrollBy(0, dy)
            }
        })
        binding.rvScrollable.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                binding.rvFixed.scrollBy(0, dy)
            }
        })
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
                    binding.spSpell.adapter = ArrayAdapter(this@EmpWiseAttendanceActivity,
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
                    binding.spDepartment.adapter = ArrayAdapter(this@EmpWiseAttendanceActivity,
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
                    binding.spDesignation.adapter = ArrayAdapter(this@EmpWiseAttendanceActivity,
                        R.layout.spinner_item_black, designations).also {
                        it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                }
                override fun onFailure(call: Call<DesignationResponse>, t: Throwable) {}
            })
    }

    // ── Load Report ───────────────────────────────────────────────

    private fun loadReport() {
        binding.progressBar.visibility  = View.VISIBLE
        binding.layoutTable.visibility  = View.GONE
        binding.layoutEmpty.visibility  = View.GONE
        binding.layoutSummary.visibility = View.GONE

        val spellSel = spells.getOrNull(binding.spSpell.selectedItemPosition)
        selectedSpellId = if ((spellSel?.id ?: 0) > 0) spellSel?.id else null

        val desigSel = designations.getOrNull(binding.spDesignation.selectedItemPosition)
        selectedDesigId = if ((desigSel?.id ?: 0) > 0) desigSel?.id else null

        val attTypeRaw = when (binding.spAttType.selectedItemPosition) {
            1 -> "R"; 2 -> "O"; 3 -> "C"; else -> null
        }

        val reportType = when (binding.spReportType.selectedItemPosition) {
            1 -> "fn_wise"; 2 -> "monthly"; else -> "date_wise"
        }

        RetrofitClient.getApiService(this).getEmpWiseAttendance(
            fromDate      = fromDateApi,
            toDate        = toDateApi,
            reportType    = reportType,
            branchId      = if (branchId > 0) branchId else null,
            spellId       = selectedSpellId,
            deptId        = selectedDeptId,
            designationId = selectedDesigId,
            attType       = attTypeRaw
        ).enqueue(object : Callback<EmpWiseAttendanceResponse> {
            override fun onResponse(call: Call<EmpWiseAttendanceResponse>,
                                    response: Response<EmpWiseAttendanceResponse>) {
                binding.progressBar.visibility = View.GONE
                val data = response.body()
                if (!response.isSuccessful || data == null || data.status != "success") {
                    Toast.makeText(this@EmpWiseAttendanceActivity,
                        "Failed: ${data?.status ?: response.message()}", Toast.LENGTH_SHORT).show()
                    binding.layoutEmpty.visibility = View.VISIBLE
                    return
                }
                val employees = data.employees.orEmpty()
                val columns   = data.columns.orEmpty()

                if (employees.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    return
                }

                // Build scrollable header
                buildScrollableHeader(columns)

                fixedAdapter.update(employees)
                scrollableAdapter.update(employees, columns)

                // employees.size is now the ROW count (a person can span several
                // dept/shift/designation/type lines, plus a subtotal). The real
                // headcount comes from the response.
                binding.tvTotalEmp.text  = "Employees: ${data.totalEmployees} · Rows: ${employees.size}"
                binding.tvDateRange.text = "${data.fromDate} to ${data.toDate}"
                binding.layoutSummary.visibility = View.VISIBLE
                binding.layoutTable.visibility   = View.VISIBLE
            }

            override fun onFailure(call: Call<EmpWiseAttendanceResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this@EmpWiseAttendanceActivity,
                    "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ── Build scrollable header ───────────────────────────────────

    private fun buildScrollableHeader(columns: List<String>) {
        binding.headerScrollable.removeAllViews()
        val colWidthPx = dpToPx(EmpAttScrollableAdapter.COL_WIDTH_DP)
        val totWidthPx = dpToPx(EmpAttScrollableAdapter.TOT_WIDTH_DP)

        for (col in columns) {
            binding.headerScrollable.addView(makeHeaderCell(col, colWidthPx))
        }
        // Per-type tallies then the grand total. Order and width come from the
        // adapter so header and cells cannot drift apart.
        val typeWidthPx = dpToPx(EmpAttScrollableAdapter.TYPE_WIDTH_DP)
        for (t in EmpAttScrollableAdapter.TYPE_COLS) {
            binding.headerScrollable.addView(makeHeaderCell(t, typeWidthPx))
        }
        binding.headerScrollable.addView(makeHeaderCell("Total", totWidthPx, Color.parseColor("#FFF176")))
    }

    private fun makeHeaderCell(text: String, widthPx: Int, textColor: Int = Color.WHITE): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, LinearLayout.LayoutParams.MATCH_PARENT)
            this.text       = text
            gravity         = android.view.Gravity.CENTER
            textSize        = 11f
            setTextColor(textColor)
            setBackgroundResource(R.drawable.table_header_cell)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
