package com.example.slsHrms

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.slsHrms.adapter.AttendanceUpdateAdapter
import com.example.slsHrms.api.AttendanceRecord
import com.example.slsHrms.api.AttendanceReportResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.api.Shift
import com.example.slsHrms.api.ShiftResponse
import com.example.slsHrms.databinding.ActivityAttendanceUpdateBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AttendanceUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceUpdateBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private var shifts = mutableListOf<Shift>()
    private var selectedBranchId: Int = 0
    private var selectedCompanyId: Int = 0
    
    private lateinit var adapter: AttendanceUpdateAdapter
    private var attendanceRecords = mutableListOf<AttendanceRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttendanceUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedCompanyId = intent.getIntExtra("CO_ID", 0)
        selectedBranchId = intent.getIntExtra("BRANCH_ID", 0)

        setupToolbar()
        setupDefaultDates()
        setupShiftSpinner()
        setupRecyclerView()
        setupSearchFilters()
        setupButtons()
        
        loadShifts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDefaultDates() {
        // Set default date to today
        val today = Calendar.getInstance()
        binding.etFromDate.setText(dateFormat.format(today.time))
    }

    private fun setupShiftSpinner() {
        val shiftAdapter = ArrayAdapter<String>(
            this,
            R.layout.spinner_item_black,
            mutableListOf("All Shifts")
        )
        shiftAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        binding.spinnerSearchShift.adapter = shiftAdapter
    }

    private fun setupRecyclerView() {
        adapter = AttendanceUpdateAdapter(attendanceRecords) { record ->
            // Navigate to edit screen
            openEditScreen(record)
        }
        binding.recyclerViewAttendance.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAttendance.adapter = adapter
    }

    private fun setupSearchFilters() {
        // Date picker
        binding.etFromDate.setOnClickListener {
            showDatePicker { date ->
                binding.etFromDate.setText(date)
            }
        }
    }

    private fun setupButtons() {
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        binding.btnClear.setOnClickListener {
            clearFilters()
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                onDateSelected(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun loadShifts() {
        RetrofitClient.getApiService(this).getShifts(selectedBranchId).enqueue(
            object : Callback<ShiftResponse> {
                override fun onResponse(call: Call<ShiftResponse>, response: Response<ShiftResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val data = response.body()?.shifts ?: emptyList()
                        shifts.clear()
                        shifts.addAll(data)
                        updateShiftSpinner()
                    }
                }

                override fun onFailure(call: Call<ShiftResponse>, t: Throwable) {
                    // Silently fail - shift filter is optional
                }
            }
        )
    }

    private fun updateShiftSpinner() {
        val shiftNames = mutableListOf("All Shifts")
        shiftNames.addAll(shifts.map { it.name })
        
        val shiftAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item_black,
            shiftNames
        )
        shiftAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        binding.spinnerSearchShift.adapter = shiftAdapter
    }

    private fun performSearch() {
        val empCode = binding.etSearchEmpCode.text.toString().trim()
        val empName = binding.etSearchName.text.toString().trim()
        val selectedDate = binding.etFromDate.text.toString().trim()

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Get selected spell id (if any)
        val selectedShiftPos = binding.spinnerSearchShift.selectedItemPosition
        val selectedSpellId = if (selectedShiftPos > 0) {
            shifts[selectedShiftPos - 1].id
        } else {
            null
        }

        // Send all parameters to backend
        RetrofitClient.getApiService(this).getAttendanceReport(
            date = selectedDate,
            empCode = empCode.ifEmpty { null },
            empName = empName.ifEmpty { null },
            spellId = selectedSpellId,
            branchId = selectedBranchId
        ).enqueue(object : Callback<AttendanceReportResponse> {
            override fun onResponse(
                call: Call<AttendanceReportResponse>,
                response: Response<AttendanceReportResponse>
            ) {
                showLoading(false)
                
                if (response.isSuccessful && response.body()?.status == "success") {
                    val records = response.body()?.data ?: emptyList()
                    updateRecordsList(records)
                } else {
                    Toast.makeText(
                        this@AttendanceUpdateActivity,
                        "Failed to load attendance records",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateRecordsList(emptyList())
                }
            }

            override fun onFailure(call: Call<AttendanceReportResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(
                    this@AttendanceUpdateActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                updateRecordsList(emptyList())
            }
        })
    }

    private fun updateRecordsList(records: List<AttendanceRecord>) {
        attendanceRecords.clear()
        attendanceRecords.addAll(records)
        adapter.updateData(attendanceRecords)
        
        // Update results count
        binding.tvResultsCount.text = "${records.size} record(s) found"
        
        // Show/hide no data view
        if (records.isEmpty()) {
            binding.recyclerViewAttendance.visibility = View.GONE
            binding.layoutNoData.visibility = View.VISIBLE
        } else {
            binding.recyclerViewAttendance.visibility = View.VISIBLE
            binding.layoutNoData.visibility = View.GONE
        }
    }

    private fun clearFilters() {
        binding.etSearchEmpCode.text?.clear()
        binding.etSearchName.text?.clear()
        setupDefaultDates()
        binding.spinnerSearchShift.setSelection(0)
        attendanceRecords.clear()
        adapter.updateData(attendanceRecords)
        binding.tvResultsCount.text = "0 record(s) found"
        binding.recyclerViewAttendance.visibility = View.GONE
        binding.layoutNoData.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSearch.isEnabled = !show
    }

    private fun openEditScreen(record: AttendanceRecord) {
        // TODO: Implement edit functionality
        Toast.makeText(
            this,
            "Edit feature - Coming soon\nRecord ID: ${record.id}",
            Toast.LENGTH_SHORT
        ).show()
        
        /* 
        val intent = Intent(this, AttendanceEditActivity::class.java)
        intent.putExtra("ATTENDANCE_ID", record.id)
        intent.putExtra("CO_ID", selectedCompanyId)
        intent.putExtra("BRANCH_ID", selectedBranchId)
        startActivity(intent)
        */
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when returning from edit screen
        if (attendanceRecords.isNotEmpty()) {
            performSearch()
        }
    }
}

