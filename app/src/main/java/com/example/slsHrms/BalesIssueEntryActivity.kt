package com.example.slsHrms

import android.app.DatePickerDialog
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

class BalesIssueEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var entryDate = ""

    private lateinit var tvEntryDate    : TextView
    private lateinit var spEntrySpell   : Spinner
    private lateinit var spQuality      : Spinner
    private lateinit var spCustomer     : Spinner
    private lateinit var tvStdWeight    : TextView
    private lateinit var etNoOfBales    : EditText
    private lateinit var etActWeight    : EditText
    private lateinit var btnSave        : Button
    private lateinit var rvEntries      : RecyclerView
    private lateinit var tvEntriesEmpty : TextView
    private lateinit var pbEntries      : ProgressBar

    private val spellList    = mutableListOf<Spell>()
    private val qualityList  = mutableListOf<FinishingQuality>()
    private val customerList = mutableListOf<Customer>()

    private var editingEntryId: Int? = null

    private val adapter = EntriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bales_issue_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate    = findViewById(R.id.tvEntryDate)
        spEntrySpell   = findViewById(R.id.spEntrySpell)
        spQuality      = findViewById(R.id.spQuality)
        spCustomer     = findViewById(R.id.spCustomer)
        tvStdWeight    = findViewById(R.id.tvStdWeight)
        etNoOfBales    = findViewById(R.id.etNoOfBales)
        etActWeight    = findViewById(R.id.etActWeight)
        btnSave        = findViewById(R.id.btnSave)
        rvEntries      = findViewById(R.id.rvEntries)
        tvEntriesEmpty = findViewById(R.id.tvEntriesEmpty)
        pbEntries      = findViewById(R.id.pbEntries)

        rvEntries.layoutManager = LinearLayoutManager(this)
        rvEntries.adapter = adapter
        rvEntries.isNestedScrollingEnabled = false

        val cal = Calendar.getInstance()
        entryDate        = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)

        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickDate() }

        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = loadEntries()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = updateStdWeightInfo()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        spCustomer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = updateStdWeightInfo()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveEntry() }
        findViewById<ImageView>(R.id.btnShowStock).setOnClickListener { showBalesStockDialog() }

        loadSpells {
            loadEntries()
        }
        loadQualities()
        loadCustomers()
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
                        this@BalesIssueEntryActivity,
                        R.layout.spinner_item_black,
                        spellList.map { it.spellName ?: "" }
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (spellList.isNotEmpty()) spEntrySpell.setSelection(0)
                    onDone()
                }
                override fun onFailure(call: Call<SpellResponse>, t: Throwable) { onDone() }
            })
    }

    private fun loadQualities() {
        RetrofitClient.getApiService(this).getFinishingQualities()
            .enqueue(object : Callback<FinishingQualityResponse> {
                override fun onResponse(call: Call<FinishingQualityResponse>, response: Response<FinishingQualityResponse>) {
                    qualityList.clear()
                    qualityList.addAll(response.body()?.qualities.orEmpty())
                    spQuality.adapter = ArrayAdapter(
                        this@BalesIssueEntryActivity,
                        R.layout.spinner_item_black,
                        qualityList.map { it.label() }
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (qualityList.isNotEmpty()) spQuality.setSelection(0)
                }
                override fun onFailure(call: Call<FinishingQualityResponse>, t: Throwable) {
                    Toast.makeText(this@BalesIssueEntryActivity,
                        "Failed to load qualities", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadCustomers() {
        RetrofitClient.getApiService(this).getCustomers()
            .enqueue(object : Callback<CustomerResponse> {
                override fun onResponse(call: Call<CustomerResponse>, response: Response<CustomerResponse>) {
                    customerList.clear()
                    customerList.addAll(response.body()?.customers.orEmpty())
                    spCustomer.adapter = ArrayAdapter(
                        this@BalesIssueEntryActivity,
                        R.layout.spinner_item_black,
                        customerList.map { it.label() }
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (customerList.isNotEmpty()) spCustomer.setSelection(0)
                }
                override fun onFailure(call: Call<CustomerResponse>, t: Throwable) {
                    Toast.makeText(this@BalesIssueEntryActivity,
                        "Failed to load customers", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun selectedSpellId(): Int? =
        spellList.getOrNull(spEntrySpell.selectedItemPosition)?.spellId

    private fun selectedQuality(): FinishingQuality? =
        qualityList.getOrNull(spQuality.selectedItemPosition)

    private fun selectedCustomer(): Customer? =
        customerList.getOrNull(spCustomer.selectedItemPosition)

    private fun updateStdWeightInfo() {
        val q = selectedQuality()
        val c = selectedCustomer()
        val std = q?.stdWeightBale

        if (q == null) {
            tvStdWeight.visibility = View.GONE
            tvStdWeight.text = ""
            return
        }

        val base = "Std Weight / Bale — ${q.label()}: ${std?.let { fmt(it) } ?: "-"}"
        tvStdWeight.visibility = View.VISIBLE
        tvStdWeight.text = base

        if (c == null) return

        RetrofitClient.getApiService(this).getBalesStockPair(q.qualityId, c.customerId)
            .enqueue(object : Callback<BalesStockPairResponse> {
                override fun onResponse(call: Call<BalesStockPairResponse>, response: Response<BalesStockPairResponse>) {
                    if (selectedQuality()?.qualityId != q.qualityId ||
                        selectedCustomer()?.customerId != c.customerId) return  // user moved on
                    val stock = response.body()?.stock ?: 0
                    tvStdWeight.text =
                        "$base   |   Stock — ${q.label()} / ${c.label()}: $stock bales"
                }
                override fun onFailure(call: Call<BalesStockPairResponse>, t: Throwable) { /* keep base */ }
            })
    }

    private fun saveEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) {
            Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show(); return
        }
        val quality = selectedQuality()
        if (quality == null) {
            Toast.makeText(this, "Please select a Quality", Toast.LENGTH_SHORT).show(); return
        }
        val customer = selectedCustomer()
        if (customer == null) {
            Toast.makeText(this, "Please select a Customer", Toast.LENGTH_SHORT).show(); return
        }
        val bales = etNoOfBales.text.toString().toIntOrNull()
        if (bales == null || bales <= 0) {
            Toast.makeText(this, "Please enter No of Bales", Toast.LENGTH_SHORT).show(); return
        }
        val weight = etActWeight.text.toString().toDoubleOrNull()

        btnSave.isEnabled = false
        val editId = editingEntryId
        if (editId != null) {
            val updReq = BalesIssueUpdateRequest(
                fngQualityId  = quality.qualityId,
                customerId    = customer.customerId,
                issueBales    = bales,
                actBaleWeight = weight
            )
            RetrofitClient.getApiService(this).updateBalesIssueEntry(editId, updReq)
                .enqueue(object : Callback<BalesProdSaveResponse> {
                    override fun onResponse(call: Call<BalesProdSaveResponse>, response: Response<BalesProdSaveResponse>) {
                        btnSave.isEnabled = true
                        val msg = response.body()?.message ?: if (response.isSuccessful) "Updated" else "Update failed"
                        Toast.makeText(this@BalesIssueEntryActivity, msg, Toast.LENGTH_SHORT).show()
                        if (response.isSuccessful && response.body()?.status == "success") {
                            clearForm()
                            loadEntries()
                        }
                    }
                    override fun onFailure(call: Call<BalesProdSaveResponse>, t: Throwable) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@BalesIssueEntryActivity,
                            "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
            return
        }

        val req = BalesIssueSaveRequest(
            date          = entryDate,
            spellId       = spellId,
            fngQualityId  = quality.qualityId,
            customerId    = customer.customerId,
            issueBales    = bales,
            actBaleWeight = weight
        )
        RetrofitClient.getApiService(this).saveBalesIssueEntry(req)
            .enqueue(object : Callback<BalesProdSaveResponse> {
                override fun onResponse(call: Call<BalesProdSaveResponse>, response: Response<BalesProdSaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed"
                    Toast.makeText(this@BalesIssueEntryActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm()
                        loadEntries()
                    }
                }
                override fun onFailure(call: Call<BalesProdSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@BalesIssueEntryActivity,
                        "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun beginEdit(e: BalesProdEntry) {
        editingEntryId = e.id
        btnSave.text = "Update"
        e.fngQualityId?.let { id ->
            val pos = qualityList.indexOfFirst { it.qualityId == id }
            if (pos >= 0) spQuality.setSelection(pos)
        }
        e.customerId?.let { id ->
            val pos = customerList.indexOfFirst { it.customerId == id }
            if (pos >= 0) spCustomer.setSelection(pos)
        }
        etNoOfBales.setText(e.issueBales?.toString() ?: "")
        etActWeight.setText(e.actBaleWeight?.let { fmt(it) } ?: "")
        Toast.makeText(this, "Editing entry #${e.id}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(e: BalesProdEntry) {
        val id = e.id ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("${e.quality ?: "-"} | ${e.customer ?: "-"} | ${e.issueBales ?: 0} bales")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteBalesIssueEntry(id)
                    .enqueue(object : Callback<BalesProdSaveResponse> {
                        override fun onResponse(call: Call<BalesProdSaveResponse>, response: Response<BalesProdSaveResponse>) {
                            Toast.makeText(this@BalesIssueEntryActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            if (editingEntryId == id) clearForm()
                            loadEntries()
                        }
                        override fun onFailure(call: Call<BalesProdSaveResponse>, t: Throwable) {
                            Toast.makeText(this@BalesIssueEntryActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearForm() {
        etNoOfBales.setText("")
        etActWeight.setText("")
        editingEntryId = null
        btnSave.text = "Save"
    }

    private fun loadEntries() {
        val spellId = selectedSpellId() ?: return

        pbEntries.visibility      = View.VISIBLE
        tvEntriesEmpty.visibility = View.GONE
        rvEntries.visibility      = View.GONE

        RetrofitClient.getApiService(this).getBalesIssueEntries(
            date    = entryDate,
            spellId = spellId
        ).enqueue(object : Callback<BalesProdEntryListResponse> {
            override fun onResponse(call: Call<BalesProdEntryListResponse>, response: Response<BalesProdEntryListResponse>) {
                pbEntries.visibility = View.GONE
                val list = response.body()?.entries.orEmpty()
                if (list.isEmpty()) {
                    tvEntriesEmpty.visibility = View.VISIBLE
                } else {
                    rvEntries.visibility = View.VISIBLE
                    adapter.update(list)
                }
            }
            override fun onFailure(call: Call<BalesProdEntryListResponse>, t: Throwable) {
                pbEntries.visibility      = View.GONE
                tvEntriesEmpty.visibility = View.VISIBLE
                tvEntriesEmpty.text       = "Failed to load entries"
            }
        })
    }

    private fun fmt(v: Double?) = String.format(Locale.getDefault(), "%.3f", v ?: 0.0)

    private fun showBalesStockDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_bales_stock_list, null)
        val rv       = view.findViewById<RecyclerView>(R.id.rvStock)
        val tvEmpty  = view.findViewById<TextView>(R.id.tvStockEmpty)
        val pb       = view.findViewById<ProgressBar>(R.id.pbStock)
        val btnClose = view.findViewById<ImageView>(R.id.btnDialogClose)

        rv.layoutManager = LinearLayoutManager(this)
        val stockAdapter = BalesStockAdapter()
        rv.adapter = stockAdapter

        val dlg = android.app.AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()
        btnClose.setOnClickListener { dlg.dismiss() }

        pb.visibility      = View.VISIBLE
        rv.visibility      = View.GONE
        tvEmpty.visibility = View.GONE

        RetrofitClient.getApiService(this).getBalesStock()
            .enqueue(object : Callback<BalesStockResponse> {
                override fun onResponse(call: Call<BalesStockResponse>, response: Response<BalesStockResponse>) {
                    pb.visibility = View.GONE
                    val rows = response.body()?.rows.orEmpty()
                    if (rows.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rv.visibility = View.VISIBLE
                        stockAdapter.update(rows)
                    }
                }
                override fun onFailure(call: Call<BalesStockResponse>, t: Throwable) {
                    pb.visibility      = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text       = "Failed to load stock"
                }
            })
        dlg.show()
    }

    inner class BalesStockAdapter : RecyclerView.Adapter<BalesStockAdapter.VH>() {
        private val items = mutableListOf<BalesStockItem>()
        fun update(list: List<BalesStockItem>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bales_stock, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvQuality   = v.findViewById<TextView>(R.id.tvQuality)
            private val tvCustomer  = v.findViewById<TextView>(R.id.tvCustomer)
            private val tvBales     = v.findViewById<TextView>(R.id.tvBales)
            private val tvStdWeight = v.findViewById<TextView>(R.id.tvStdWeight)
            fun bind(s: BalesStockItem) {
                tvQuality.text   = s.quality ?: "-"
                tvCustomer.text  = s.customer ?: "-"
                tvBales.text     = (s.stock ?: 0).toString()
                tvStdWeight.text = fmt(s.stdWeight)
            }
        }
    }

    inner class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.VH>() {
        private val items = mutableListOf<BalesProdEntry>()
        fun update(list: List<BalesProdEntry>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bales_prod_entry, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvQuality  = v.findViewById<TextView>(R.id.tvQuality)
            private val tvCustomer = v.findViewById<TextView>(R.id.tvCustomer)
            private val tvBales    = v.findViewById<TextView>(R.id.tvBales)
            private val tvWeight   = v.findViewById<TextView>(R.id.tvWeight)
            private val ivEdit     = v.findViewById<ImageView>(R.id.ivEdit)
            private val ivDelete   = v.findViewById<ImageView>(R.id.ivDelete)
            fun bind(e: BalesProdEntry) {
                tvQuality.text  = e.quality ?: "-"
                tvCustomer.text = e.customer ?: "-"
                tvBales.text    = (e.issueBales ?: 0).toString()
                tvWeight.text   = fmt(e.actBaleWeight)
                ivEdit.setOnClickListener   { beginEdit(e) }
                ivDelete.setOnClickListener { confirmDelete(e) }
            }
        }
    }
}
