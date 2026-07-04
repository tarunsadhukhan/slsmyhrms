package com.example.slsHrms

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.ContWindingEntryAdapter
import com.example.slsHrms.api.ContWindingEntriesResponse
import com.example.slsHrms.api.ContWindingEntry
import com.example.slsHrms.api.ContWindingEntrySaveRequest
import com.example.slsHrms.api.ContWindingEntrySaveResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.api.SpinningQuality
import com.example.slsHrms.api.WindingQualityResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ContWindingEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate   : TextView
    private lateinit var spQuality     : Spinner
    private lateinit var etProdKgs     : EditText
    private lateinit var btnSave       : Button
    private lateinit var rvSummary     : RecyclerView
    private lateinit var tvSummaryEmpty: TextView
    private lateinit var pbSummary     : ProgressBar

    private val qualityList = mutableListOf<SpinningQuality>()
    private lateinit var summaryAdapter: ContWindingEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cont_winding_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate    = findViewById(R.id.tvEntryDate)
        spQuality      = findViewById(R.id.spQuality)
        etProdKgs      = findViewById(R.id.etProdKgs)
        btnSave        = findViewById(R.id.btnSave)
        rvSummary      = findViewById(R.id.rvSummary)
        tvSummaryEmpty = findViewById(R.id.tvSummaryEmpty)
        pbSummary      = findViewById(R.id.pbSummary)

        summaryAdapter = ContWindingEntryAdapter { row -> confirmDelete(row) }
        rvSummary.layoutManager = LinearLayoutManager(this)
        rvSummary.adapter = summaryAdapter
        rvSummary.isNestedScrollingEnabled = false

        val cal = Calendar.getInstance()
        entryDate        = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)

        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickDate() }
        btnSave.setOnClickListener { saveEntry() }

        loadQualities()
        loadSummary()
    }

    // ── Date picker ──────────────────────────────────────────────────────────
    private fun pickDate() {
        val cal = Calendar.getInstance()
        try { apiDate.parse(entryDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            entryDate        = apiDate.format(cal.time)
            tvEntryDate.text = dispDate.format(cal.time)
            loadSummary()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── Quality dropdown ─────────────────────────────────────────────────────
    private fun loadQualities() {
        RetrofitClient.getApiService(this).getWindingQualities(branchId.takeIf { it > 0 })
            .enqueue(object : Callback<WindingQualityResponse> {
                override fun onResponse(call: Call<WindingQualityResponse>, response: Response<WindingQualityResponse>) {
                    qualityList.clear()
                    response.body()?.qualities?.let { qualityList.addAll(it) }
                    spQuality.adapter = ArrayAdapter(
                        this@ContWindingEntryActivity,
                        R.layout.spinner_item_black,
                        qualityList
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                }
                override fun onFailure(call: Call<WindingQualityResponse>, t: Throwable) {
                    Toast.makeText(this@ContWindingEntryActivity,
                        "Failed to load qualities", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun selectedQuality(): SpinningQuality? =
        qualityList.getOrNull(spQuality.selectedItemPosition)

    // ── Save ─────────────────────────────────────────────────────────────────
    private fun saveEntry() {
        val quality = selectedQuality()
        if (quality == null) {
            Toast.makeText(this, "Please select a Quality", Toast.LENGTH_SHORT).show(); return
        }
        val prodKgs = etProdKgs.text.toString().trim().toIntOrNull()
        if (prodKgs == null || prodKgs <= 0) {
            Toast.makeText(this, "Please enter Prod Kgs", Toast.LENGTH_SHORT).show(); return
        }

        val req = ContWindingEntrySaveRequest(
            date      = entryDate,
            qualityId = quality.qualityId,
            prodKgs   = prodKgs,
            userId    = userId
        )
        btnSave.isEnabled = false
        RetrofitClient.getApiService(this).saveContWindingEntry(req)
            .enqueue(object : Callback<ContWindingEntrySaveResponse> {
                override fun onResponse(call: Call<ContWindingEntrySaveResponse>, response: Response<ContWindingEntrySaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed"
                    Toast.makeText(this@ContWindingEntryActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        etProdKgs.setText("")
                        loadSummary()
                    }
                }
                override fun onFailure(call: Call<ContWindingEntrySaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@ContWindingEntryActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Summary list ─────────────────────────────────────────────────────────
    private fun loadSummary() {
        pbSummary.visibility      = View.VISIBLE
        tvSummaryEmpty.visibility = View.GONE
        rvSummary.visibility      = View.GONE

        RetrofitClient.getApiService(this).getContWindingEntries(entryDate)
            .enqueue(object : Callback<ContWindingEntriesResponse> {
                override fun onResponse(call: Call<ContWindingEntriesResponse>, response: Response<ContWindingEntriesResponse>) {
                    pbSummary.visibility = View.GONE
                    val list = response.body()?.entries.orEmpty()
                    if (list.isEmpty()) {
                        tvSummaryEmpty.visibility = View.VISIBLE
                    } else {
                        rvSummary.visibility = View.VISIBLE
                        summaryAdapter.update(list)
                    }
                }
                override fun onFailure(call: Call<ContWindingEntriesResponse>, t: Throwable) {
                    pbSummary.visibility      = View.GONE
                    tvSummaryEmpty.visibility = View.VISIBLE
                    tvSummaryEmpty.text       = "Failed to load entries"
                }
            })
    }

    // ── Delete ───────────────────────────────────────────────────────────────
    private fun confirmDelete(row: ContWindingEntry) {
        val id = row.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("Remove ${row.qualityName ?: ""} (${row.prodKgs ?: 0} kg)?")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteContWindingEntry(id)
                    .enqueue(object : Callback<ContWindingEntrySaveResponse> {
                        override fun onResponse(call: Call<ContWindingEntrySaveResponse>, response: Response<ContWindingEntrySaveResponse>) {
                            Toast.makeText(this@ContWindingEntryActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            loadSummary()
                        }
                        override fun onFailure(call: Call<ContWindingEntrySaveResponse>, t: Throwable) {
                            Toast.makeText(this@ContWindingEntryActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
