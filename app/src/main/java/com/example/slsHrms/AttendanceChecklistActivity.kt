package com.example.slsHrms

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
        adapter = AttendanceChecklistAdapter(emptyList())
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
        binding.progressBar.visibility   = View.VISIBLE
        binding.hsvTable.visibility      = View.GONE
        binding.layoutEmpty.visibility   = View.GONE
        binding.layoutSummary.visibility = View.GONE

        val spellSel  = spells.getOrNull(binding.spSpell.selectedItemPosition)
        val spellName = if ((spellSel?.id ?: 0) > 0) spellSel?.name else null

        val deptSel = departments.getOrNull(binding.spDepartment.selectedItemPosition)
        selectedDeptId = if ((deptSel?.id ?: 0) > 0) deptSel?.id else null

        val desigSel = designations.getOrNull(binding.spDesignation.selectedItemPosition)
        val desigId  = if ((desigSel?.id ?: 0) > 0) desigSel?.id else null

        RetrofitClient.getApiService(this).getAttendanceReportRange(
            fromDate      = fromDateApi,
            toDate        = toDateApi,
            departmentId  = selectedDeptId,
            branchId      = if (branchId > 0) branchId else null,
            shiftName     = spellName,
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
            }

            override fun onFailure(call: Call<AttendanceReportResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this@AttendanceChecklistActivity,
                    "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
