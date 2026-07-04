package com.example.slsHrms

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.slsHrms.adapter.OccupationAdapter
import com.example.slsHrms.api.AddOccupationRequest
import com.example.slsHrms.api.AddOccupationResponse
import com.example.slsHrms.api.Department
import com.example.slsHrms.api.DepartmentResponse
import com.example.slsHrms.api.DesignationResponse
import com.example.slsHrms.api.Occupation
import com.example.slsHrms.api.OccupationResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.databinding.ActivityOccupationMasterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OccupationMasterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOccupationMasterBinding
    private lateinit var adapter: OccupationAdapter
    private var allOccupations = mutableListOf<Occupation>()

    private var selectedBranchId: Int = 0
    private var departments = mutableListOf<Department>()
    private var selectedSubDeptId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOccupationMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedBranchId = intent.getIntExtra("BRANCH_ID", 0)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFab()

        if (selectedBranchId > 0) {
            loadDepartments()
        } else {
            loadOccupations()
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
        adapter = OccupationAdapter(
            occupations = emptyList(),
            onEdit = { occ -> showEditDialog(occ) },
            onDelete = { occ -> showDeleteConfirmation(occ) }
        )
        binding.rvOccupations.layoutManager = LinearLayoutManager(this)
        binding.rvOccupations.adapter = adapter
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
        val filtered = if (query.isEmpty()) allOccupations
        else allOccupations.filter { it.name.contains(query, ignoreCase = true) }
        adapter.updateList(filtered)
        updateUI(filtered)
    }

    // ── FAB ──────────────────────────────────────────────────────

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { showAddDialog() }
    }

    // ── Load Departments for Spinner ─────────────────────────────

    private fun loadDepartments() {
        RetrofitClient.getApiService(this)
            .getDepartments(branchId = selectedBranchId)
            .enqueue(object : Callback<DepartmentResponse> {
                override fun onResponse(call: Call<DepartmentResponse>, response: Response<DepartmentResponse>) {
                    departments.clear()
                    departments.addAll(response.body()?.departments ?: emptyList())
                    setupDepartmentSpinner()
                    // Load all designations for branch initially
                    loadDesignationsByBranch(null)
                }
                override fun onFailure(call: Call<DepartmentResponse>, t: Throwable) {
                    setupDepartmentSpinner()
                    loadDesignationsByBranch(null)
                }
            })
    }

    private fun setupDepartmentSpinner() {
        val names = mutableListOf("All Departments")
        names.addAll(departments.map { it.name })
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDepartment.adapter = spinnerAdapter

        binding.spinnerDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSubDeptId = if (position == 0) null else departments[position - 1].id
                loadDesignationsByBranch(selectedSubDeptId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ── Load Designations from API ───────────────────────────────

    private fun loadDesignationsByBranch(subDeptId: Int?) {
        if (selectedBranchId <= 0) { loadOccupations(); return }
        showLoading(true)
        RetrofitClient.getApiService(this)
            .getDesignations(branchId = selectedBranchId, subDeptId = subDeptId)
            .enqueue(object : Callback<DesignationResponse> {
                override fun onResponse(call: Call<DesignationResponse>, response: Response<DesignationResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val data = (response.body()?.designations ?: emptyList())
                            .map { Occupation(it.id, it.name) }
                        allOccupations.clear()
                        allOccupations.addAll(data)
                        adapter.updateList(allOccupations)
                        updateUI(allOccupations)
                        binding.etSearch.setText("")
                    } else {
                        Toast.makeText(this@OccupationMasterActivity, "Failed to load designations", Toast.LENGTH_SHORT).show()
                        updateUI(emptyList())
                    }
                }
                override fun onFailure(call: Call<DesignationResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@OccupationMasterActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    updateUI(emptyList())
                }
            })
    }

    // ── Load Occupations from API (fallback when no branch) ──────

    private fun loadOccupations() {
        showLoading(true)
        RetrofitClient.getApiService(this).getOccupations()
            .enqueue(object : Callback<OccupationResponse> {
                override fun onResponse(call: Call<OccupationResponse>, response: Response<OccupationResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val data = response.body()?.occupations ?: emptyList()
                        allOccupations.clear()
                        allOccupations.addAll(data)
                        adapter.updateList(allOccupations)
                        updateUI(allOccupations)
                    } else {
                        Toast.makeText(this@OccupationMasterActivity, "Failed to load occupations", Toast.LENGTH_SHORT).show()
                        updateUI(emptyList())
                    }
                }
                override fun onFailure(call: Call<OccupationResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@OccupationMasterActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    updateUI(emptyList())
                }
            })
    }

    // ── Add Dialog ───────────────────────────────────────────────

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "Enter occupation name"
            setPadding(60, 40, 60, 40)
            setTextColor(android.graphics.Color.BLACK)
        }
        AlertDialog.Builder(this)
            .setTitle("Add Occupation")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) addOccupationToServer(name)
                else Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addOccupationToServer(name: String) {
        showLoading(true)
        val request = AddOccupationRequest(name)
        RetrofitClient.getApiService(this).addOccupation(request)
            .enqueue(object : Callback<AddOccupationResponse> {
                override fun onResponse(call: Call<AddOccupationResponse>, response: Response<AddOccupationResponse>) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@OccupationMasterActivity, response.body()?.message ?: "Occupation added!", Toast.LENGTH_SHORT).show()
                        if (selectedBranchId > 0) loadDesignationsByBranch(selectedSubDeptId) else loadOccupations()
                    } else {
                        Toast.makeText(this@OccupationMasterActivity,
                            if (response.code() == 409) "Occupation already exists!" else "Failed to add occupation",
                            Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<AddOccupationResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@OccupationMasterActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Edit Dialog ──────────────────────────────────────────────

    private fun showEditDialog(occ: Occupation) {
        val input = EditText(this).apply {
            setText(occ.name)
            hint = "Occupation name"
            setPadding(60, 40, 60, 40)
            setTextColor(android.graphics.Color.BLACK)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Edit Occupation")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) updateOccupationOnServer(occ.id, name)
                else Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateOccupationOnServer(id: Int, name: String) {
        showLoading(true)
        val request = AddOccupationRequest(name)
        RetrofitClient.getApiService(this).updateOccupation(id, request)
            .enqueue(object : Callback<AddOccupationResponse> {
                override fun onResponse(call: Call<AddOccupationResponse>, response: Response<AddOccupationResponse>) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@OccupationMasterActivity, response.body()?.message ?: "Occupation updated!", Toast.LENGTH_SHORT).show()
                        if (selectedBranchId > 0) loadDesignationsByBranch(selectedSubDeptId) else loadOccupations()
                    } else {
                        Toast.makeText(this@OccupationMasterActivity,
                            if (response.code() == 409) "Occupation name already exists!" else "Failed to update occupation",
                            Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<AddOccupationResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@OccupationMasterActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Delete Confirmation ──────────────────────────────────────

    private fun showDeleteConfirmation(occ: Occupation) {
        AlertDialog.Builder(this)
            .setTitle("Delete Occupation")
            .setMessage("Are you sure you want to delete '${occ.name}'?")
            .setPositiveButton("Delete") { _, _ -> deleteOccupationOnServer(occ.id, occ.name) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteOccupationOnServer(id: Int, name: String) {
        showLoading(true)
        RetrofitClient.getApiService(this).deleteOccupation(id)
            .enqueue(object : Callback<AddOccupationResponse> {
                override fun onResponse(call: Call<AddOccupationResponse>, response: Response<AddOccupationResponse>) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@OccupationMasterActivity, "'$name' deleted", Toast.LENGTH_SHORT).show()
                        if (selectedBranchId > 0) loadDesignationsByBranch(selectedSubDeptId) else loadOccupations()
                    } else {
                        Toast.makeText(this@OccupationMasterActivity, "Failed to delete occupation", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<AddOccupationResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@OccupationMasterActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── UI Helpers ───────────────────────────────────────────────

    private fun updateUI(list: List<Occupation>) {
        binding.tvCount.text = "Total: ${list.size}"
        if (list.isEmpty()) {
            binding.rvOccupations.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvOccupations.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}

