package com.example.slsHrms

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.slsHrms.adapter.DepartmentAdapter
import com.example.slsHrms.api.AddDepartmentRequest
import com.example.slsHrms.api.AddDepartmentResponse
import com.example.slsHrms.api.Department
import com.example.slsHrms.api.DepartmentResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.databinding.ActivityDepartmentMasterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DepartmentMasterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDepartmentMasterBinding
    private lateinit var adapter: DepartmentAdapter
    private var allDepartments = mutableListOf<Department>()
    private var selectedCompanyId: Int = 0
    private var selectedBranchId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepartmentMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedCompanyId = intent.getIntExtra("CO_ID", 0)
        selectedBranchId  = intent.getIntExtra("BRANCH_ID", 0)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFab()
        loadDepartments()
    }

    // ── Toolbar ──────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    // ── RecyclerView ─────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = DepartmentAdapter(
            departments = emptyList(),
            onEdit = { dept -> showEditDialog(dept) },
            onDelete = { dept -> showDeleteConfirmation(dept) }
        )
        binding.rvDepartments.layoutManager = LinearLayoutManager(this)
        binding.rvDepartments.adapter = adapter
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
            allDepartments
        } else {
            allDepartments.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        adapter.updateList(filtered)
        updateUI(filtered)
    }

    // ── FAB ──────────────────────────────────────────────────────

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    // ── Load Departments from API ────────────────────────────────

    private fun loadDepartments() {
        showLoading(true)

        RetrofitClient.getApiService(this).getDepartments(
            coId = if (selectedCompanyId > 0) selectedCompanyId else null,
            branchId = if (selectedBranchId > 0) selectedBranchId else null
        )
            .enqueue(object : Callback<DepartmentResponse> {
                override fun onResponse(
                    call: Call<DepartmentResponse>,
                    response: Response<DepartmentResponse>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val data = response.body()?.departments ?: emptyList()
                        allDepartments.clear()
                        allDepartments.addAll(data)
                        adapter.updateList(allDepartments)
                        updateUI(allDepartments)
                    } else {
                        Toast.makeText(
                            this@DepartmentMasterActivity,
                            "Failed to load departments",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(emptyList())
                    }
                }

                override fun onFailure(call: Call<DepartmentResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(
                        this@DepartmentMasterActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(emptyList())
                }
            })
    }

    // ── Add Dialog ───────────────────────────────────────────────

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "Enter department name"
            setPadding(60, 40, 60, 40)
            setTextColor(android.graphics.Color.BLACK)
        }

        AlertDialog.Builder(this)
            .setTitle("Add Department")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    addDepartmentToServer(name)
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addDepartmentToServer(name: String) {
        showLoading(true)

        val request = AddDepartmentRequest(name)
        RetrofitClient.getApiService(this).addDepartment(request)
            .enqueue(object : Callback<AddDepartmentResponse> {
                override fun onResponse(
                    call: Call<AddDepartmentResponse>,
                    response: Response<AddDepartmentResponse>
                ) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(
                            this@DepartmentMasterActivity,
                            response.body()?.message ?: "Department added!",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Reload list from server
                        loadDepartments()
                    } else {
                        Toast.makeText(
                            this@DepartmentMasterActivity,
                            "Failed to add department",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<AddDepartmentResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(
                        this@DepartmentMasterActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ── Edit Dialog ──────────────────────────────────────────────

    private fun showEditDialog(dept: Department) {
        val input = EditText(this).apply {
            setText(dept.name)
            hint = "Department name"
            setPadding(60, 40, 60, 40)
            setTextColor(android.graphics.Color.BLACK)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Department")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val index = allDepartments.indexOfFirst { it.id == dept.id }
                    if (index >= 0) {
                        allDepartments[index] = Department(dept.id, name)
                        allDepartments.sortBy { it.name }
                        adapter.updateList(allDepartments.toList())
                        Toast.makeText(this, "Department updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Delete Confirmation ──────────────────────────────────────

    private fun showDeleteConfirmation(dept: Department) {
        AlertDialog.Builder(this)
            .setTitle("Delete Department")
            .setMessage("Are you sure you want to delete '${dept.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                allDepartments.removeAll { it.id == dept.id }
                adapter.updateList(allDepartments.toList())
                updateUI(allDepartments)
                Toast.makeText(this, "'${dept.name}' deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── UI Helpers ───────────────────────────────────────────────

    private fun updateUI(list: List<Department>) {
        binding.tvCount.text = "Total: ${list.size}"
        if (list.isEmpty()) {
            binding.rvDepartments.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvDepartments.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}

