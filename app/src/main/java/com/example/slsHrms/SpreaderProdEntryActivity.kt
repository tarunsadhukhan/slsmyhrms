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

class SpreaderProdEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate     : TextView
    private lateinit var spEntrySpell    : Spinner
    private lateinit var llMachines      : LinearLayout
    private lateinit var tvMachinesStatus: TextView
    private lateinit var llQualities     : LinearLayout
    private lateinit var tvQualitiesStatus: TextView
    private lateinit var llSprdQualities      : LinearLayout
    private lateinit var tvSprdQualitiesStatus: TextView
    private lateinit var etProduction    : EditText
    private lateinit var etBinNo         : EditText
    private lateinit var btnSave         : Button
    private lateinit var btnShowStock    : ImageView
    private lateinit var rvEntries       : RecyclerView
    private lateinit var tvEntriesEmpty  : TextView
    private lateinit var pbEntries       : ProgressBar

    private val spellList         = mutableListOf<Spell>()
    private val machinesList      = mutableListOf<SpreaderMachine>()
    private val qualitiesList     = mutableListOf<SpreaderQuality>()
    private val sprdQualitiesList = mutableListOf<SpreaderQuality>()

    private var selectedMcId         : Int? = null
    private var selectedQualityId    : Int? = null
    private var selectedSprdQualityId: Int? = null
    private var editingEntryId       : Int? = null

    private val adapter = EntriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spreader_prod_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate       = findViewById(R.id.tvEntryDate)
        spEntrySpell      = findViewById(R.id.spEntrySpell)
        llMachines        = findViewById(R.id.llMachines)
        tvMachinesStatus  = findViewById(R.id.tvMachinesStatus)
        llQualities       = findViewById(R.id.llQualities)
        tvQualitiesStatus = findViewById(R.id.tvQualitiesStatus)
        llSprdQualities       = findViewById(R.id.llSprdQualities)
        tvSprdQualitiesStatus = findViewById(R.id.tvSprdQualitiesStatus)
        etProduction      = findViewById(R.id.etProduction)
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
            loadMachines()
            loadQualities()
            loadSprdQualities()
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
                        this@SpreaderProdEntryActivity,
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

    private fun loadMachines() {
        if (branchId <= 0) {
            tvMachinesStatus.text = "Branch not set"
            tvMachinesStatus.visibility = View.VISIBLE
            return
        }
        llMachines.removeAllViews()
        selectedMcId = null
        tvMachinesStatus.visibility = View.VISIBLE
        tvMachinesStatus.text = "Loading…"

        RetrofitClient.getApiService(this).getSpreaderMachines(branchId)
            .enqueue(object : Callback<SpreaderMachineResponse> {
                override fun onResponse(call: Call<SpreaderMachineResponse>, response: Response<SpreaderMachineResponse>) {
                    machinesList.clear()
                    machinesList.addAll(response.body()?.machines.orEmpty())
                    if (machinesList.isEmpty()) {
                        tvMachinesStatus.text = "No machines found"
                        tvMachinesStatus.visibility = View.VISIBLE
                    } else {
                        tvMachinesStatus.visibility = View.GONE
                        renderMachineButtons()
                    }
                }
                override fun onFailure(call: Call<SpreaderMachineResponse>, t: Throwable) {
                    tvMachinesStatus.text = "Failed to load machines"
                    tvMachinesStatus.visibility = View.VISIBLE
                }
            })
    }

    private fun renderMachineButtons() {
        llMachines.removeAllViews()
        val dp = resources.displayMetrics.density
        for (m in machinesList) {
            val btn = Button(this)
            btn.text = m.label().take(14)
            btn.textSize = 11f
            btn.setTypeface(null, Typeface.BOLD)
            btn.setTextColor(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#1565C0"))
            val lp = LinearLayout.LayoutParams((92 * dp).toInt(), (36 * dp).toInt())
            lp.setMargins(0, 0, (6 * dp).toInt(), 0)
            btn.layoutParams = lp
            btn.setPadding(0, 0, 0, 0)
            btn.tag = m
            btn.setOnClickListener { onMachineSelected(btn, m) }
            llMachines.addView(btn)
        }
    }

    private fun onMachineSelected(btn: Button, m: SpreaderMachine) {
        selectedMcId = m.mcId
        for (i in 0 until llMachines.childCount) {
            val child = llMachines.getChildAt(i) as? Button ?: continue
            child.setBackgroundColor(
                if (child == btn) Color.parseColor("#2E7D32")
                else              Color.parseColor("#1565C0")
            )
        }
    }

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

        RetrofitClient.getApiService(this).getSpreaderQualities(branchId)
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

    private fun loadSprdQualities() {
        if (branchId <= 0) {
            tvSprdQualitiesStatus.text = "Branch not set"
            tvSprdQualitiesStatus.visibility = View.VISIBLE
            return
        }
        llSprdQualities.removeAllViews()
        selectedSprdQualityId = null
        tvSprdQualitiesStatus.visibility = View.VISIBLE
        tvSprdQualitiesStatus.text = "Loading…"

        RetrofitClient.getApiService(this).getSpreaderSprdQualities(branchId)
            .enqueue(object : Callback<SpreaderQualityResponse> {
                override fun onResponse(call: Call<SpreaderQualityResponse>, response: Response<SpreaderQualityResponse>) {
                    sprdQualitiesList.clear()
                    sprdQualitiesList.addAll(response.body()?.qualities.orEmpty())
                    if (sprdQualitiesList.isEmpty()) {
                        tvSprdQualitiesStatus.text = "No spreader qualities found"
                        tvSprdQualitiesStatus.visibility = View.VISIBLE
                    } else {
                        tvSprdQualitiesStatus.visibility = View.GONE
                        renderSprdQualityButtons()
                    }
                }
                override fun onFailure(call: Call<SpreaderQualityResponse>, t: Throwable) {
                    tvSprdQualitiesStatus.text = "Failed to load spreader qualities"
                    tvSprdQualitiesStatus.visibility = View.VISIBLE
                }
            })
    }

    private fun renderSprdQualityButtons() {
        llSprdQualities.removeAllViews()
        val dp = resources.displayMetrics.density
        for (q in sprdQualitiesList) {
            val btn = Button(this)
            btn.text = q.label().take(10)
            btn.textSize = 11f
            btn.setTypeface(null, Typeface.BOLD)
            btn.setTextColor(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#00838F"))
            val lp = LinearLayout.LayoutParams((68 * dp).toInt(), (36 * dp).toInt())
            lp.setMargins(0, 0, (6 * dp).toInt(), 0)
            btn.layoutParams = lp
            btn.setPadding(0, 0, 0, 0)
            btn.tag = q
            btn.setOnClickListener { onSprdQualitySelected(btn, q) }
            llSprdQualities.addView(btn)
        }
    }

    private fun onSprdQualitySelected(btn: Button, q: SpreaderQuality) {
        selectedSprdQualityId = q.qualityId
        for (i in 0 until llSprdQualities.childCount) {
            val child = llSprdQualities.getChildAt(i) as? Button ?: continue
            child.setBackgroundColor(
                if (child == btn) Color.parseColor("#2E7D32")
                else              Color.parseColor("#00838F")
            )
        }
    }

    private fun saveEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) {
            Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show(); return
        }
        if (branchId <= 0) {
            Toast.makeText(this, "Branch not set", Toast.LENGTH_SHORT).show(); return
        }
        val mcId = selectedMcId
        if (mcId == null) {
            Toast.makeText(this, "Please select a Machine", Toast.LENGTH_SHORT).show(); return
        }
        val qualityId = selectedQualityId
        if (qualityId == null) {
            Toast.makeText(this, "Please select a Quality", Toast.LENGTH_SHORT).show(); return
        }
        val sprdQualityId = selectedSprdQualityId
        if (sprdQualityId == null) {
            Toast.makeText(this, "Please select a Spreader Jute Quality", Toast.LENGTH_SHORT).show(); return
        }
        val production = etProduction.text.toString().toDoubleOrNull()
        if (production == null || production <= 0.0) {
            Toast.makeText(this, "Please enter Production", Toast.LENGTH_SHORT).show(); return
        }
        val binNo = etBinNo.text.toString().trim().ifBlank { null }

        btnSave.isEnabled = false
        val editId = editingEntryId
        if (editId != null) {
            val updReq = SpreaderProdUpdateRequest(
                mcId = mcId, qualityId = qualityId,
                sprdQualityId = sprdQualityId,
                binNo = binNo, production = production, userId = userId
            )
            RetrofitClient.getApiService(this).updateSpreaderProdEntry(editId, updReq)
                .enqueue(object : Callback<SpreaderProdSaveResponse> {
                    override fun onResponse(call: Call<SpreaderProdSaveResponse>, response: Response<SpreaderProdSaveResponse>) {
                        btnSave.isEnabled = true
                        val msg = response.body()?.message ?: if (response.isSuccessful) "Updated" else "Update failed"
                        Toast.makeText(this@SpreaderProdEntryActivity, msg, Toast.LENGTH_SHORT).show()
                        if (response.isSuccessful && response.body()?.status == "success") {
                            clearForm()
                            loadEntries()
                        }
                    }
                    override fun onFailure(call: Call<SpreaderProdSaveResponse>, t: Throwable) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@SpreaderProdEntryActivity,
                            "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
            return
        }

        val req = SpreaderProdSaveRequest(
            date          = entryDate,
            spellId       = spellId,
            branchId      = branchId,
            mcId          = mcId,
            qualityId     = qualityId,
            sprdQualityId = sprdQualityId,
            binNo         = binNo,
            production    = production,
            userId        = userId
        )
        RetrofitClient.getApiService(this).saveSpreaderProdEntry(req)
            .enqueue(object : Callback<SpreaderProdSaveResponse> {
                override fun onResponse(call: Call<SpreaderProdSaveResponse>, response: Response<SpreaderProdSaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed"
                    Toast.makeText(this@SpreaderProdEntryActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm()
                        loadEntries()
                    }
                }
                override fun onFailure(call: Call<SpreaderProdSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@SpreaderProdEntryActivity,
                        "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun beginEdit(e: SpreaderProdEntry) {
        editingEntryId = e.id
        btnSave.text = "Update"
        e.mcId?.let { id ->
            selectedMcId = id
            for (i in 0 until llMachines.childCount) {
                val child = llMachines.getChildAt(i) as? Button ?: continue
                val tag = child.tag as? SpreaderMachine
                child.setBackgroundColor(
                    if (tag?.mcId == id) Color.parseColor("#2E7D32")
                    else                 Color.parseColor("#1565C0")
                )
            }
        }
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
        }
        selectedSprdQualityId = e.sprdQualityId
        for (i in 0 until llSprdQualities.childCount) {
            val child = llSprdQualities.getChildAt(i) as? Button ?: continue
            val tag = child.tag as? SpreaderQuality
            child.setBackgroundColor(
                if (e.sprdQualityId != null && tag?.qualityId == e.sprdQualityId)
                    Color.parseColor("#2E7D32")
                else
                    Color.parseColor("#00838F")
            )
        }
        etProduction.setText(e.production?.let { fmt(it) } ?: "")
        etBinNo.setText(e.binNo ?: "")
        Toast.makeText(this, "Editing entry #${e.id}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(e: SpreaderProdEntry) {
        val id = e.id ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("Mc ${e.mcNo ?: "-"} | ${e.quality ?: "-"} | ${fmt(e.production)}")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteSpreaderProdEntry(id)
                    .enqueue(object : Callback<SpreaderProdSaveResponse> {
                        override fun onResponse(call: Call<SpreaderProdSaveResponse>, response: Response<SpreaderProdSaveResponse>) {
                            Toast.makeText(this@SpreaderProdEntryActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            if (editingEntryId == id) clearForm()
                            loadEntries()
                        }
                        override fun onFailure(call: Call<SpreaderProdSaveResponse>, t: Throwable) {
                            Toast.makeText(this@SpreaderProdEntryActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearForm() {
        etProduction.setText("")
        etBinNo.setText("")
        selectedMcId = null
        selectedQualityId = null
        selectedSprdQualityId = null
        editingEntryId = null
        btnSave.text = "Save"
        for (i in 0 until llMachines.childCount) {
            (llMachines.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#1565C0"))
        }
        for (i in 0 until llQualities.childCount) {
            (llQualities.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#6A1B9A"))
        }
        for (i in 0 until llSprdQualities.childCount) {
            (llSprdQualities.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#00838F"))
        }
    }

    private fun fmt(v: Double?) = String.format(Locale.getDefault(), "%.0f", v ?: 0.0)

    private fun loadEntries() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) return

        pbEntries.visibility      = View.VISIBLE
        tvEntriesEmpty.visibility = View.GONE
        rvEntries.visibility      = View.GONE

        RetrofitClient.getApiService(this).getSpreaderProdEntries(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId
        ).enqueue(object : Callback<SpreaderProdEntryListResponse> {
            override fun onResponse(call: Call<SpreaderProdEntryListResponse>, response: Response<SpreaderProdEntryListResponse>) {
                pbEntries.visibility = View.GONE
                val list = response.body()?.entries.orEmpty()
                if (list.isEmpty()) {
                    tvEntriesEmpty.visibility = View.VISIBLE
                } else {
                    rvEntries.visibility = View.VISIBLE
                    adapter.update(list)
                }
            }
            override fun onFailure(call: Call<SpreaderProdEntryListResponse>, t: Throwable) {
                pbEntries.visibility      = View.GONE
                tvEntriesEmpty.visibility = View.VISIBLE
                tvEntriesEmpty.text       = "Failed to load entries"
            }
        })
    }

    private fun showQualityStockDialog() {
        if (branchId <= 0) {
            Toast.makeText(this, "Branch not set", Toast.LENGTH_SHORT).show(); return
        }
        val view = layoutInflater.inflate(R.layout.dialog_quality_stock_list, null)
        val rv       = view.findViewById<RecyclerView>(R.id.rvStock)
        val tvEmpty  = view.findViewById<TextView>(R.id.tvStockEmpty)
        val pb       = view.findViewById<ProgressBar>(R.id.pbStock)
        val btnClose = view.findViewById<ImageView>(R.id.btnDialogClose)

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
                tvStock.text   = String.format(Locale.getDefault(), "%.0f", s.stock ?: 0.0)
            }
        }
    }

    inner class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.VH>() {
        private val items = mutableListOf<SpreaderProdEntry>()
        fun update(list: List<SpreaderProdEntry>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_spreader_prod_entry, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvMcNo        = v.findViewById<TextView>(R.id.tvMcNo)
            private val tvQuality     = v.findViewById<TextView>(R.id.tvQuality)
            private val tvSprdQuality = v.findViewById<TextView>(R.id.tvSprdQuality)
            private val tvProduction  = v.findViewById<TextView>(R.id.tvProduction)
            private val ivEdit        = v.findViewById<ImageView>(R.id.ivEdit)
            private val ivDelete      = v.findViewById<ImageView>(R.id.ivDelete)
            fun bind(e: SpreaderProdEntry) {
                tvMcNo.text        = e.mcNo ?: "-"
                tvQuality.text     = e.quality ?: "-"
                tvSprdQuality.text = e.sprdQuality ?: "-"
                tvProduction.text  = fmt(e.production)
                ivEdit.setOnClickListener   { beginEdit(e) }
                ivDelete.setOnClickListener { confirmDelete(e) }
            }
        }
    }
}