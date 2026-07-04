package com.example.slsHrms

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class JuteReceivedActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate      : TextView
    private lateinit var llQualities      : LinearLayout
    private lateinit var tvQualitiesStatus: TextView
    private lateinit var etNoOfBales      : EditText
    private lateinit var etWeight         : EditText
    private lateinit var btnSave          : Button
    private lateinit var rvEntries        : RecyclerView
    private lateinit var tvEntriesEmpty   : TextView
    private lateinit var pbEntries        : ProgressBar

    private val qualitiesList = mutableListOf<SpreaderQuality>()
    private var selectedQualityId: Int? = null
    private var editingEntryId  : Int? = null

    private val adapter = EntriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jute_received)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate       = findViewById(R.id.tvEntryDate)
        llQualities       = findViewById(R.id.llQualities)
        tvQualitiesStatus = findViewById(R.id.tvQualitiesStatus)
        etNoOfBales       = findViewById(R.id.etNoOfBales)
        etWeight          = findViewById(R.id.etWeight)
        btnSave           = findViewById(R.id.btnSave)
        rvEntries         = findViewById(R.id.rvEntries)
        tvEntriesEmpty    = findViewById(R.id.tvEntriesEmpty)
        pbEntries         = findViewById(R.id.pbEntries)

        rvEntries.layoutManager = LinearLayoutManager(this)
        rvEntries.adapter = adapter
        rvEntries.isNestedScrollingEnabled = false

        val cal = Calendar.getInstance()
        entryDate        = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)

        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickDate() }
        btnSave.setOnClickListener { saveEntry() }

        loadQualities()
        loadEntries()
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        try { apiDate.parse(entryDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            entryDate        = apiDate.format(cal.time)
            tvEntryDate.text = dispDate.format(cal.time)
            loadEntries()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── Qualities ────────────────────────────────────────────────────────────

    private fun loadQualities() {
        if (branchId <= 0) {
            tvQualitiesStatus.text = "Branch not set"
            tvQualitiesStatus.visibility = View.VISIBLE
            return
        }
        llQualities.removeAllViews()
        selectedQualityId = null
        tvQualitiesStatus.visibility = View.VISIBLE
        tvQualitiesStatus.text = "Loading…"

        RetrofitClient.getApiService(this).getJuteReceivedQualities(branchId)
            .enqueue(object : Callback<SpreaderQualityResponse> {
                override fun onResponse(call: Call<SpreaderQualityResponse>, response: Response<SpreaderQualityResponse>) {
                    qualitiesList.clear()
                    qualitiesList.addAll(response.body()?.qualities.orEmpty())
                    if (qualitiesList.isEmpty()) {
                        tvQualitiesStatus.text = "No qualities found"
                        tvQualitiesStatus.visibility = View.VISIBLE
                    } else {
                        tvQualitiesStatus.visibility = View.GONE
                        renderQualityButtons()
                    }
                }
                override fun onFailure(call: Call<SpreaderQualityResponse>, t: Throwable) {
                    tvQualitiesStatus.text = "Failed to load qualities"
                    tvQualitiesStatus.visibility = View.VISIBLE
                }
            })
    }

    private fun renderQualityButtons() {
        llQualities.removeAllViews()
        val dp = resources.displayMetrics.density
        for (q in qualitiesList) {
            val btn = Button(this)
            btn.text = q.label().take(10)
            btn.textSize = 11f
            btn.setTypeface(null, Typeface.BOLD)
            btn.setTextColor(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#6A1B9A"))
            val lp = LinearLayout.LayoutParams((68 * dp).toInt(), (36 * dp).toInt())
            lp.setMargins(0, 0, (6 * dp).toInt(), 0)
            btn.layoutParams = lp
            btn.setPadding(0, 0, 0, 0)
            btn.tag = q
            btn.setOnClickListener { onQualitySelected(btn, q) }
            llQualities.addView(btn)
        }
    }

    private fun onQualitySelected(btn: Button, q: SpreaderQuality) {
        selectedQualityId = q.qualityId
        for (i in 0 until llQualities.childCount) {
            val child = llQualities.getChildAt(i) as? Button ?: continue
            child.setBackgroundColor(
                if (child == btn) Color.parseColor("#2E7D32")
                else              Color.parseColor("#6A1B9A")
            )
        }
    }

    // ── Save / Update ────────────────────────────────────────────────────────

    private fun saveEntry() {
        if (branchId <= 0) {
            Toast.makeText(this, "Branch not set", Toast.LENGTH_SHORT).show(); return
        }
        val qualityId = selectedQualityId
        if (qualityId == null) {
            Toast.makeText(this, "Please select a Quality", Toast.LENGTH_SHORT).show(); return
        }
        val bales  = etNoOfBales.text.toString().toIntOrNull()
        val weight = etWeight.text.toString().toIntOrNull()
        if ((bales == null || bales <= 0) && (weight == null || weight <= 0)) {
            Toast.makeText(this, "Enter No of Bales and/or Net Weight",
                Toast.LENGTH_SHORT).show(); return
        }

        btnSave.isEnabled = false
        val req = JuteReceivedSaveRequest(
            date      = entryDate,
            qualityId = qualityId,
            noOfBales = bales,
            weight    = weight,
            branchId  = branchId,
            userId    = userId
        )

        val editId = editingEntryId
        val callback = object : Callback<JuteReceivedSaveResponse> {
            override fun onResponse(call: Call<JuteReceivedSaveResponse>, response: Response<JuteReceivedSaveResponse>) {
                btnSave.isEnabled = true
                val msg = response.body()?.message
                    ?: if (response.isSuccessful) (if (editId != null) "Updated" else "Saved")
                       else (if (editId != null) "Update failed" else "Save failed")
                Toast.makeText(this@JuteReceivedActivity, msg, Toast.LENGTH_SHORT).show()
                if (response.isSuccessful && response.body()?.status == "success") {
                    clearForm()
                    loadEntries()
                }
            }
            override fun onFailure(call: Call<JuteReceivedSaveResponse>, t: Throwable) {
                btnSave.isEnabled = true
                Toast.makeText(this@JuteReceivedActivity,
                    "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
        if (editId != null) {
            RetrofitClient.getApiService(this).updateJuteReceivedEntry(editId, req).enqueue(callback)
        } else {
            RetrofitClient.getApiService(this).saveJuteReceivedEntry(req).enqueue(callback)
        }
    }

    // ── Edit ─────────────────────────────────────────────────────────────────

    private fun beginEdit(e: JuteReceivedEntry) {
        val id = e.id ?: return
        editingEntryId = id
        btnSave.text = "Update"

        e.qualityId?.let { qid ->
            selectedQualityId = qid
            for (i in 0 until llQualities.childCount) {
                val child = llQualities.getChildAt(i) as? Button ?: continue
                val tag = child.tag as? SpreaderQuality
                child.setBackgroundColor(
                    if (tag?.qualityId == qid) Color.parseColor("#2E7D32")
                    else                       Color.parseColor("#6A1B9A")
                )
            }
        }
        etNoOfBales.setText(e.noOfBales?.toString() ?: "")
        etWeight.setText(e.weight?.toString() ?: "")
        Toast.makeText(this, "Editing entry #$id", Toast.LENGTH_SHORT).show()
    }

    private fun clearForm() {
        etNoOfBales.setText("")
        etWeight.setText("")
        selectedQualityId = null
        editingEntryId = null
        btnSave.text = "Save"
        for (i in 0 until llQualities.childCount) {
            (llQualities.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#6A1B9A"))
        }
    }

    // ── Entries list ─────────────────────────────────────────────────────────

    private fun loadEntries() {
        if (branchId <= 0) return
        pbEntries.visibility      = View.VISIBLE
        tvEntriesEmpty.visibility = View.GONE
        rvEntries.visibility      = View.GONE

        RetrofitClient.getApiService(this).getJuteReceivedEntries(
            date     = entryDate,
            branchId = branchId
        ).enqueue(object : Callback<JuteReceivedEntryListResponse> {
            override fun onResponse(call: Call<JuteReceivedEntryListResponse>, response: Response<JuteReceivedEntryListResponse>) {
                pbEntries.visibility = View.GONE
                val list = response.body()?.entries.orEmpty()
                if (list.isEmpty()) {
                    tvEntriesEmpty.visibility = View.VISIBLE
                } else {
                    rvEntries.visibility = View.VISIBLE
                    adapter.update(list)
                }
            }
            override fun onFailure(call: Call<JuteReceivedEntryListResponse>, t: Throwable) {
                pbEntries.visibility      = View.GONE
                tvEntriesEmpty.visibility = View.VISIBLE
                tvEntriesEmpty.text       = "Failed to load entries"
            }
        })
    }

    private fun confirmDelete(e: JuteReceivedEntry) {
        val id = e.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("${e.quality ?: "-"} | Bales ${e.noOfBales ?: 0} | Wt ${e.weight ?: 0}")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteJuteReceivedEntry(id)
                    .enqueue(object : Callback<JuteReceivedSaveResponse> {
                        override fun onResponse(call: Call<JuteReceivedSaveResponse>, response: Response<JuteReceivedSaveResponse>) {
                            Toast.makeText(this@JuteReceivedActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            if (editingEntryId == id) clearForm()
                            loadEntries()
                        }
                        override fun onFailure(call: Call<JuteReceivedSaveResponse>, t: Throwable) {
                            Toast.makeText(this@JuteReceivedActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.VH>() {
        private val items = mutableListOf<JuteReceivedEntry>()
        fun update(list: List<JuteReceivedEntry>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_jute_received_entry, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvQuality = v.findViewById<TextView>(R.id.tvQuality)
            private val tvBales   = v.findViewById<TextView>(R.id.tvBales)
            private val tvWeight  = v.findViewById<TextView>(R.id.tvWeight)
            private val ivEdit    = v.findViewById<ImageView>(R.id.ivEdit)
            private val ivDelete  = v.findViewById<ImageView>(R.id.ivDelete)
            fun bind(e: JuteReceivedEntry) {
                tvQuality.text = e.quality ?: "-"
                tvBales.text   = e.noOfBales?.toString() ?: "-"
                tvWeight.text  = e.weight?.toString() ?: "-"
                ivEdit.setOnClickListener   { beginEdit(e) }
                ivDelete.setOnClickListener { confirmDelete(e) }
            }
        }
    }
}
