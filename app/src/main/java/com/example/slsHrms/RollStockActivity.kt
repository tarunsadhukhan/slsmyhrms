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

class RollStockActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate      : TextView
    private lateinit var spEntrySpell     : Spinner
    private lateinit var spMachine        : Spinner
    private lateinit var tvMachinesStatus : TextView
    private lateinit var llQualities      : LinearLayout
    private lateinit var tvQualitiesStatus: TextView
    private lateinit var etNoOfRolls      : EditText
    private lateinit var btnSave          : Button
    private lateinit var rvEntries        : RecyclerView
    private lateinit var tvEntriesEmpty   : TextView
    private lateinit var pbEntries        : ProgressBar

    private val spellList     = mutableListOf<Spell>()
    private val machinesList  = mutableListOf<McCode>()
    private val qualitiesList = mutableListOf<SpreaderQuality>()

    private var selectedQualityId   : Int? = null
    private var editingEntryId      : Int? = null

    private val adapter = EntriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_roll_stock)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate       = findViewById(R.id.tvEntryDate)
        spEntrySpell      = findViewById(R.id.spEntrySpell)
        spMachine         = findViewById(R.id.spMachine)
        tvMachinesStatus  = findViewById(R.id.tvMachinesStatus)
        llQualities       = findViewById(R.id.llQualities)
        tvQualitiesStatus = findViewById(R.id.tvQualitiesStatus)
        etNoOfRolls       = findViewById(R.id.etNoOfRolls)
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

        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadEntries()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveEntry() }

        loadSpells {
            loadMachines()
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

    // ── Spells ───────────────────────────────────────────────────────────────

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
                        this@RollStockActivity,
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

    // ── Machines ─────────────────────────────────────────────────────────────

    private fun loadMachines() {
        if (branchId <= 0) {
            tvMachinesStatus.text = "Branch not set"
            tvMachinesStatus.visibility = View.VISIBLE
            return
        }
        tvMachinesStatus.visibility = View.VISIBLE
        tvMachinesStatus.text = "Loading…"

        RetrofitClient.getApiService(this).getRollStockMcCodes(branchId)
            .enqueue(object : Callback<McCodeResponse> {
                override fun onResponse(call: Call<McCodeResponse>, response: Response<McCodeResponse>) {
                    machinesList.clear()
                    machinesList.addAll(response.body()?.machines.orEmpty())
                    if (machinesList.isEmpty()) {
                        tvMachinesStatus.text = "No machines found"
                        tvMachinesStatus.visibility = View.VISIBLE
                        spMachine.adapter = null
                    } else {
                        tvMachinesStatus.visibility = View.GONE
                        spMachine.adapter = ArrayAdapter(
                            this@RollStockActivity,
                            R.layout.spinner_item_black,
                            machinesList.map { it.label() }
                        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                        if (machinesList.isNotEmpty()) spMachine.setSelection(0)
                    }
                }
                override fun onFailure(call: Call<McCodeResponse>, t: Throwable) {
                    tvMachinesStatus.text = "Failed to load machines"
                    tvMachinesStatus.visibility = View.VISIBLE
                }
            })
    }

    private fun selectedMcCodeId(): Int? =
        machinesList.getOrNull(spMachine.selectedItemPosition)?.mcCodeId

    // ── Qualities (sprd) ─────────────────────────────────────────────────────

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
            btn.setBackgroundColor(Color.parseColor("#00838F"))
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
                else              Color.parseColor("#00838F")
            )
        }
    }

    // ── Save / Update ────────────────────────────────────────────────────────

    private fun saveEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) {
            Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show(); return
        }
        val mcCodeId = selectedMcCodeId()
        if (mcCodeId == null) {
            Toast.makeText(this, "Please select a Machine", Toast.LENGTH_SHORT).show(); return
        }
        val qualityId = selectedQualityId
        if (qualityId == null) {
            Toast.makeText(this, "Please select a Quality", Toast.LENGTH_SHORT).show(); return
        }
        val rolls = etNoOfRolls.text.toString().toIntOrNull()
        if (rolls == null || rolls <= 0) {
            Toast.makeText(this, "Please enter No of Rolls", Toast.LENGTH_SHORT).show(); return
        }

        btnSave.isEnabled = false
        val editId = editingEntryId
        if (editId != null) {
            val updReq = RollStockUpdateRequest(
                mcCodeId = mcCodeId, sprdQualityId = qualityId,
                noOfRolls = rolls, userId = userId
            )
            RetrofitClient.getApiService(this).updateRollStock(editId, updReq)
                .enqueue(object : Callback<RollStockSaveResponse> {
                    override fun onResponse(call: Call<RollStockSaveResponse>, response: Response<RollStockSaveResponse>) {
                        btnSave.isEnabled = true
                        val msg = response.body()?.message ?: if (response.isSuccessful) "Updated" else "Update failed"
                        Toast.makeText(this@RollStockActivity, msg, Toast.LENGTH_SHORT).show()
                        if (response.isSuccessful && response.body()?.status == "success") {
                            clearForm()
                            loadEntries()
                        }
                    }
                    override fun onFailure(call: Call<RollStockSaveResponse>, t: Throwable) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@RollStockActivity,
                            "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
            return
        }

        val req = RollStockSaveRequest(
            date          = entryDate,
            spellId       = spellId,
            mcCodeId      = mcCodeId,
            sprdQualityId = qualityId,
            noOfRolls     = rolls,
            userId        = userId
        )
        RetrofitClient.getApiService(this).saveRollStock(req)
            .enqueue(object : Callback<RollStockSaveResponse> {
                override fun onResponse(call: Call<RollStockSaveResponse>, response: Response<RollStockSaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed"
                    Toast.makeText(this@RollStockActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm()
                        loadEntries()
                    }
                }
                override fun onFailure(call: Call<RollStockSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@RollStockActivity,
                        "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun beginEdit(e: RollStockEntry) {
        editingEntryId = e.id
        btnSave.text = "Update"
        e.mcCodeId?.let { id ->
            val pos = machinesList.indexOfFirst { it.mcCodeId == id }
            if (pos >= 0) spMachine.setSelection(pos)
        }
        e.sprdQualityId?.let { id ->
            selectedQualityId = id
            for (i in 0 until llQualities.childCount) {
                val child = llQualities.getChildAt(i) as? Button ?: continue
                val tag = child.tag as? SpreaderQuality
                child.setBackgroundColor(
                    if (tag?.qualityId == id) Color.parseColor("#2E7D32")
                    else                      Color.parseColor("#00838F")
                )
            }
        }
        etNoOfRolls.setText(e.noOfRolls?.toString() ?: "")
        Toast.makeText(this, "Editing entry #${e.id}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(e: RollStockEntry) {
        val id = e.id ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("${e.mcName ?: e.mcCode ?: "-"} | ${e.quality ?: "-"} | ${e.noOfRolls ?: 0}")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteRollStock(id)
                    .enqueue(object : Callback<RollStockSaveResponse> {
                        override fun onResponse(call: Call<RollStockSaveResponse>, response: Response<RollStockSaveResponse>) {
                            Toast.makeText(this@RollStockActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            if (editingEntryId == id) clearForm()
                            loadEntries()
                        }
                        override fun onFailure(call: Call<RollStockSaveResponse>, t: Throwable) {
                            Toast.makeText(this@RollStockActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearForm() {
        etNoOfRolls.setText("")
        selectedQualityId = null
        editingEntryId    = null
        btnSave.text = "Save"
        for (i in 0 until llQualities.childCount) {
            (llQualities.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#00838F"))
        }
    }

    // ── Entries list ─────────────────────────────────────────────────────────

    private fun loadEntries() {
        val spellId = selectedSpellId() ?: return

        pbEntries.visibility      = View.VISIBLE
        tvEntriesEmpty.visibility = View.GONE
        rvEntries.visibility      = View.GONE

        RetrofitClient.getApiService(this).getRollStockEntries(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId.takeIf { it > 0 }
        ).enqueue(object : Callback<RollStockListResponse> {
            override fun onResponse(call: Call<RollStockListResponse>, response: Response<RollStockListResponse>) {
                pbEntries.visibility = View.GONE
                val list = response.body()?.entries.orEmpty()
                if (list.isEmpty()) {
                    tvEntriesEmpty.visibility = View.VISIBLE
                } else {
                    rvEntries.visibility = View.VISIBLE
                    adapter.update(list)
                }
            }
            override fun onFailure(call: Call<RollStockListResponse>, t: Throwable) {
                pbEntries.visibility      = View.GONE
                tvEntriesEmpty.visibility = View.VISIBLE
                tvEntriesEmpty.text       = "Failed to load entries"
            }
        })
    }

    inner class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.VH>() {
        private val items = mutableListOf<RollStockEntry>()
        fun update(list: List<RollStockEntry>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_roll_stock_entry, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvMachine = v.findViewById<TextView>(R.id.tvMachine)
            private val tvQuality = v.findViewById<TextView>(R.id.tvQuality)
            private val tvRolls   = v.findViewById<TextView>(R.id.tvRolls)
            private val ivEdit    = v.findViewById<ImageView>(R.id.ivEdit)
            private val ivDelete  = v.findViewById<ImageView>(R.id.ivDelete)
            fun bind(e: RollStockEntry) {
                tvMachine.text = e.mcName ?: e.mcCode ?: "-"
                tvQuality.text = e.quality ?: "-"
                tvRolls.text   = (e.noOfRolls ?: 0).toString()
                ivEdit.setOnClickListener   { beginEdit(e) }
                ivDelete.setOnClickListener { confirmDelete(e) }
            }
        }
    }
}