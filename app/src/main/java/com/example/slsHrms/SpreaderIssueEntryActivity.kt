package com.example.slsHrms

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

class SpreaderIssueEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate     : TextView
    private lateinit var spEntrySpell    : Spinner
    private lateinit var llQualities     : LinearLayout
    private lateinit var tvQualitiesStatus: TextView
    private lateinit var tvStockInfo     : TextView
    private lateinit var etIssue         : EditText
    private lateinit var etBinNo         : EditText
    private lateinit var btnSave         : Button
    private lateinit var btnShowStock    : ImageView
    private lateinit var rvEntries       : RecyclerView
    private lateinit var tvEntriesEmpty  : TextView
    private lateinit var pbEntries       : ProgressBar

    private val spellList     = mutableListOf<Spell>()
    private val qualitiesList = mutableListOf<SpreaderQuality>()

    private var selectedQualityId: Int? = null
    private var editingEntryId   : Int? = null

    private val adapter = EntriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spreader_issue_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate       = findViewById(R.id.tvEntryDate)
        spEntrySpell      = findViewById(R.id.spEntrySpell)
        llQualities       = findViewById(R.id.llQualities)
        tvQualitiesStatus = findViewById(R.id.tvQualitiesStatus)
        tvStockInfo       = findViewById(R.id.tvStockInfo)
        etIssue           = findViewById(R.id.etIssue)
        etBinNo           = findViewById(R.id.etBinNo)
        btnSave           = findViewById(R.id.btnSave)
        btnShowStock      = findViewById(R.id.btnShowStock)
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

        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadEntries()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveEntry() }
        btnShowStock.setOnClickListener { showQualityStockDialog() }

        loadSpells {
            loadQualities()
            loadEntries()
        }
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

    private fun loadSpells(onDone: () -> Unit) {
        RetrofitClient.getApiService(this).getSpells(branchId.takeIf { it > 0 })
            .enqueue(object : Callback<SpellResponse> {
                override fun onResponse(call: Call<SpellResponse>, response: Response<SpellResponse>) {
                    spellList.clear()
                    response.body()?.spells?.let {
                        spellList.addAll(it.filter { sp ->
                            !sp.spellName.equals("All Spells", ignoreCase = true)
                        })
                    }
                    spEntrySpell.adapter = ArrayAdapter(
                        this@SpreaderIssueEntryActivity,
                        R.layout.spinner_item_black,
                        spellList.map { it.spellName ?: "" }
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (spellList.isNotEmpty()) spEntrySpell.setSelection(0)
                    onDone()
                }
                override fun onFailure(call: Call<SpellResponse>, t: Throwable) { onDone() }
            })
    }

    private fun selectedSpellId(): Int? =
        spellList.getOrNull(spEntrySpell.selectedItemPosition)?.spellId

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

        RetrofitClient.getApiService(this).getSpreaderSprdQualities(branchId)
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
        loadStock(q)
    }

    private fun loadStock(q: SpreaderQuality) {
        tvStockInfo.visibility = View.VISIBLE
        tvStockInfo.text = "Loading stock for ${q.label()}…"
        RetrofitClient.getApiService(this).getSpreaderQualityStock(q.qualityId)
            .enqueue(object : Callback<SpreaderQualityStockResponse> {
                override fun onResponse(call: Call<SpreaderQualityStockResponse>, response: Response<SpreaderQualityStockResponse>) {
                    if (selectedQualityId != q.qualityId) return  // user moved on
                    val body = response.body()
                    val stock = body?.stock ?: 0.0
                    val name  = body?.shrName ?: q.label()
                    tvStockInfo.text = "No of Roll in Stock — $name: ${fmtInt(stock)}"
                }
                override fun onFailure(call: Call<SpreaderQualityStockResponse>, t: Throwable) {
                    if (selectedQualityId != q.qualityId) return
                    tvStockInfo.text = "Stock lookup failed"
                }
            })
    }

    private fun fmtInt(v: Double) = String.format(Locale.getDefault(), "%.0f", v)

    private fun saveEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) {
            Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show(); return
        }
        if (branchId <= 0) {
            Toast.makeText(this, "Branch not set", Toast.LENGTH_SHORT).show(); return
        }
        val qualityId = selectedQualityId
        if (qualityId == null) {
            Toast.makeText(this, "Please select a Quality", Toast.LENGTH_SHORT).show(); return
        }
        val issue = etIssue.text.toString().toDoubleOrNull()
        if (issue == null || issue <= 0.0) {
            Toast.makeText(this, "Please enter Issue", Toast.LENGTH_SHORT).show(); return
        }
        val binNo = etBinNo.text.toString().trim().ifBlank { null }

        btnSave.isEnabled = false
        val editId = editingEntryId
        if (editId != null) {
            val updReq = SpreaderIssueUpdateRequest(
                qualityId = qualityId, binNo = binNo, issue = issue, userId = userId
            )
            RetrofitClient.getApiService(this).updateSpreaderIssueEntry(editId, updReq)
                .enqueue(object : Callback<SpreaderIssueSaveResponse> {
                    override fun onResponse(call: Call<SpreaderIssueSaveResponse>, response: Response<SpreaderIssueSaveResponse>) {
                        btnSave.isEnabled = true
                        val msg = response.body()?.message ?: if (response.isSuccessful) "Updated" else "Update failed"
                        Toast.makeText(this@SpreaderIssueEntryActivity, msg, Toast.LENGTH_SHORT).show()
                        if (response.isSuccessful && response.body()?.status == "success") {
                            clearForm()
                            loadEntries()
                        }
                    }
                    override fun onFailure(call: Call<SpreaderIssueSaveResponse>, t: Throwable) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@SpreaderIssueEntryActivity,
                            "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
            return
        }

        val req = SpreaderIssueSaveRequest(
            date      = entryDate,
            spellId   = spellId,
            branchId  = branchId,
            qualityId = qualityId,
            binNo     = binNo,
            issue     = issue,
            userId    = userId
        )
        RetrofitClient.getApiService(this).saveSpreaderIssueEntry(req)
            .enqueue(object : Callback<SpreaderIssueSaveResponse> {
                override fun onResponse(call: Call<SpreaderIssueSaveResponse>, response: Response<SpreaderIssueSaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed"
                    Toast.makeText(this@SpreaderIssueEntryActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm()
                        loadEntries()
                    }
                }
                override fun onFailure(call: Call<SpreaderIssueSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@SpreaderIssueEntryActivity,
                        "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun beginEdit(e: SpreaderIssueEntry) {
        editingEntryId = e.id
        btnSave.text = "Update"
        e.qualityId?.let { id ->
            selectedQualityId = id
            for (i in 0 until llQualities.childCount) {
                val child = llQualities.getChildAt(i) as? Button ?: continue
                val tag = child.tag as? SpreaderQuality
                child.setBackgroundColor(
                    if (tag?.qualityId == id) Color.parseColor("#2E7D32")
                    else                      Color.parseColor("#6A1B9A")
                )
            }
            qualitiesList.firstOrNull { it.qualityId == id }?.let { loadStock(it) }
        }
        etIssue.setText(e.issue?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: "")
        etBinNo.setText(e.binNo ?: "")
        Toast.makeText(this, "Editing entry #${e.id}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(e: SpreaderIssueEntry) {
        val id = e.id ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("${e.quality ?: "-"} | Bin ${e.binNo ?: "-"} | ${fmt(e.issue)}")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteSpreaderIssueEntry(id)
                    .enqueue(object : Callback<SpreaderIssueSaveResponse> {
                        override fun onResponse(call: Call<SpreaderIssueSaveResponse>, response: Response<SpreaderIssueSaveResponse>) {
                            Toast.makeText(this@SpreaderIssueEntryActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            if (editingEntryId == id) clearForm()
                            loadEntries()
                        }
                        override fun onFailure(call: Call<SpreaderIssueSaveResponse>, t: Throwable) {
                            Toast.makeText(this@SpreaderIssueEntryActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearForm() {
        etIssue.setText("")
        etBinNo.setText("")
        selectedQualityId = null
        editingEntryId = null
        btnSave.text = "Save"
        for (i in 0 until llQualities.childCount) {
            (llQualities.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#6A1B9A"))
        }
        tvStockInfo.visibility = View.GONE
        tvStockInfo.text = ""
    }

    private fun loadEntries() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) return

        pbEntries.visibility      = View.VISIBLE
        tvEntriesEmpty.visibility = View.GONE
        rvEntries.visibility      = View.GONE

        RetrofitClient.getApiService(this).getSpreaderIssueEntries(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId
        ).enqueue(object : Callback<SpreaderIssueEntryListResponse> {
            override fun onResponse(call: Call<SpreaderIssueEntryListResponse>, response: Response<SpreaderIssueEntryListResponse>) {
                pbEntries.visibility = View.GONE
                val list = response.body()?.entries.orEmpty()
                if (list.isEmpty()) {
                    tvEntriesEmpty.visibility = View.VISIBLE
                } else {
                    rvEntries.visibility = View.VISIBLE
                    adapter.update(list)
                }
            }
            override fun onFailure(call: Call<SpreaderIssueEntryListResponse>, t: Throwable) {
                pbEntries.visibility      = View.GONE
                tvEntriesEmpty.visibility = View.VISIBLE
                tvEntriesEmpty.text       = "Failed to load entries"
            }
        })
    }

    private fun fmt(v: Double?) = String.format(Locale.getDefault(), "%.3f", v ?: 0.0)

    private fun showQualityStockDialog() {
        if (branchId <= 0) {
            Toast.makeText(this, "Branch not set", Toast.LENGTH_SHORT).show(); return
        }
        val view = layoutInflater.inflate(R.layout.dialog_quality_stock_list, null)
        val rv          = view.findViewById<RecyclerView>(R.id.rvStock)
        val tvEmpty     = view.findViewById<TextView>(R.id.tvStockEmpty)
        val pb          = view.findViewById<ProgressBar>(R.id.pbStock)
        val btnClose    = view.findViewById<ImageView>(R.id.btnDialogClose)

        rv.layoutManager = LinearLayoutManager(this)
        val stockAdapter = StockAdapter()
        rv.adapter = stockAdapter

        val dlg = android.app.AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()
        btnClose.setOnClickListener { dlg.dismiss() }

        pb.visibility      = View.VISIBLE
        rv.visibility      = View.GONE
        tvEmpty.visibility = View.GONE

        RetrofitClient.getApiService(this).getSpreaderQualityStockList(branchId)
            .enqueue(object : Callback<SpreaderQualityStockListResponse> {
                override fun onResponse(call: Call<SpreaderQualityStockListResponse>, response: Response<SpreaderQualityStockListResponse>) {
                    pb.visibility = View.GONE
                    val rows = response.body()?.rows.orEmpty()
                    if (rows.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rv.visibility = View.VISIBLE
                        stockAdapter.update(rows)
                    }
                }
                override fun onFailure(call: Call<SpreaderQualityStockListResponse>, t: Throwable) {
                    pb.visibility      = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text       = "Failed to load stock"
                }
            })
        dlg.show()
    }

    inner class StockAdapter : RecyclerView.Adapter<StockAdapter.VH>() {
        private val items = mutableListOf<SpreaderQualityStockItem>()
        fun update(list: List<SpreaderQualityStockItem>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quality_stock, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvQuality = v.findViewById<TextView>(R.id.tvQuality)
            private val tvBinNo   = v.findViewById<TextView>(R.id.tvBinNo)
            private val tvStock   = v.findViewById<TextView>(R.id.tvStock)
            fun bind(s: SpreaderQualityStockItem) {
                tvQuality.text = s.quality ?: "-"
                tvBinNo.text   = s.binNo ?: "-"
                tvStock.text   = fmtInt(s.stock ?: 0.0)
            }
        }
    }

    inner class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.VH>() {
        private val items = mutableListOf<SpreaderIssueEntry>()
        fun update(list: List<SpreaderIssueEntry>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_spreader_issue_entry, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvQuality = v.findViewById<TextView>(R.id.tvQuality)
            private val tvBinNo   = v.findViewById<TextView>(R.id.tvBinNo)
            private val tvIssue   = v.findViewById<TextView>(R.id.tvIssue)
            private val ivEdit    = v.findViewById<ImageView>(R.id.ivEdit)
            private val ivDelete  = v.findViewById<ImageView>(R.id.ivDelete)
            fun bind(e: SpreaderIssueEntry) {
                tvQuality.text = e.quality ?: "-"
                tvBinNo.text   = e.binNo ?: "-"
                tvIssue.text   = fmt(e.issue)
                ivEdit.setOnClickListener   { beginEdit(e) }
                ivDelete.setOnClickListener { confirmDelete(e) }
            }
        }
    }
}
