package com.example.slsHrms

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.DoffTransactionAdapter
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SpinningDoffActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId = 0
    private var userId   = 0
    private var filterDate = ""
    private var entryDate  = ""
    private var editingId: Int? = null

    // Entry form views
    private lateinit var tvEntryTitle: TextView
    private lateinit var tvEntryDate:  TextView
    private lateinit var spEntrySpell: Spinner
    private lateinit var spEntryMachine: Spinner
    private lateinit var spEntryQuality: Spinner
    private lateinit var spEntryTrolly:  Spinner
    private lateinit var etGross:      EditText
    private lateinit var etTare:       EditText
    private lateinit var etNet:        EditText
    private lateinit var btnSave:      Button
    private lateinit var btnReset:     Button

    // List views
    private lateinit var adapter: DoffTransactionAdapter
    private lateinit var rvDoff: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvFilterDate: TextView
    private lateinit var spFilterSpell: Spinner

    private val spellList    = mutableListOf<Spell>()
    private val filterSpells = mutableListOf<Spell>()
    private val machineList  = mutableListOf<DoffMachine>()
    private val qualityList  = mutableListOf<SpinningQuality>()
    private val trollyList   = mutableListOf<Trolly>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spinning_doff_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        // Bind views
        tvEntryTitle    = findViewById(R.id.tvEntryTitle)
        tvEntryDate     = findViewById(R.id.tvEntryDate)
        spEntrySpell    = findViewById(R.id.spEntrySpell)
        spEntryMachine  = findViewById(R.id.spEntryMachine)
        spEntryQuality  = findViewById(R.id.spEntryQuality)
        spEntryTrolly   = findViewById(R.id.spEntryTrolly)
        etGross         = findViewById(R.id.etEntryGross)
        etTare          = findViewById(R.id.etEntryTare)
        etNet           = findViewById(R.id.etEntryNet)
        btnSave         = findViewById(R.id.btnEntrySave)
        btnReset        = findViewById(R.id.btnEntryReset)

        rvDoff          = findViewById(R.id.rvDoff)
        progressBar     = findViewById(R.id.progressBar)
        tvEmpty         = findViewById(R.id.tvEmpty)
        tvFilterDate    = findViewById(R.id.tvFilterDate)
        spFilterSpell   = findViewById(R.id.spFilterSpell)

        val cal = Calendar.getInstance()
        filterDate        = apiDate.format(cal.time)
        entryDate         = filterDate
        tvFilterDate.text = dispDate.format(cal.time)
        tvEntryDate.text  = dispDate.format(cal.time)

        findViewById<View>(R.id.btnFilterDate).setOnClickListener { pickFilterDate() }
        findViewById<View>(R.id.btnEntryDate).setOnClickListener  { pickEntryDate() }

        adapter = DoffTransactionAdapter(emptyList(),
            onEdit   = { loadIntoForm(it) },
            onDelete = { confirmDelete(it) })
        rvDoff.layoutManager = LinearLayoutManager(this)
        rvDoff.adapter = adapter
        rvDoff.isNestedScrollingEnabled = false

        findViewById<Button>(R.id.btnSearch).setOnClickListener { reloadList() }
        btnSave.setOnClickListener { saveEntry() }
        btnReset.setOnClickListener { clearForm() }

        // Auto-compute net weight
        val weightWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcNet() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        }
        etGross.addTextChangedListener(weightWatcher)
        etTare.addTextChangedListener(weightWatcher)

        // Trolly change → auto-fill tare
        spEntryTrolly.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val t = trollyList.getOrNull(pos - 1) ?: return
                val tare = (t.trollyWeight ?: 0.0) + (t.bucketWeight ?: 0.0)
                if (tare > 0) etTare.setText(String.format(Locale.getDefault(), "%.3f", tare))
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Machine change → load defaults (only when adding new)
        spEntryMachine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (editingId == null) loadDefaultsForMachine()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        loadSpells { loadMasters { reloadList() } }
    }

    // ── Date pickers ───────────────────────────────────────────────
    private fun pickFilterDate() {
        val cal = Calendar.getInstance()
        try { apiDate.parse(filterDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            filterDate = apiDate.format(cal.time)
            tvFilterDate.text = dispDate.format(cal.time)
            reloadList()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickEntryDate() {
        val cal = Calendar.getInstance()
        try { apiDate.parse(entryDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            entryDate = apiDate.format(cal.time)
            tvEntryDate.text = dispDate.format(cal.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── List load ──────────────────────────────────────────────────
    private fun reloadList() {
        // Filter spinner contains real spells only; default selection is the
        // first spell (position 0).
        val pos = spFilterSpell.selectedItemPosition
        val spellId = if (pos < 0) null else filterSpells.getOrNull(pos)?.spellId
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility     = View.GONE
        rvDoff.visibility      = View.GONE
        RetrofitClient.getApiService(this).getDoffTransactions(
            date     = filterDate,
            spellId  = spellId,
            branchId = if (branchId > 0) branchId else null
        ).enqueue(object : Callback<DoffListResponse> {
            override fun onResponse(call: Call<DoffListResponse>, response: Response<DoffListResponse>) {
                progressBar.visibility = View.GONE
                val list = response.body()?.transactions.orEmpty()
                if (list.isEmpty()) tvEmpty.visibility = View.VISIBLE
                else { rvDoff.visibility = View.VISIBLE; adapter.update(list) }
            }
            override fun onFailure(call: Call<DoffListResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            }
        })
    }

    // ── Spell loading ──────────────────────────────────────────────
    private fun loadSpells(onDone: () -> Unit) {
        RetrofitClient.getApiService(this).getSpells(branchId.takeIf { it > 0 })
            .enqueue(object : Callback<SpellResponse> {
                override fun onResponse(call: Call<SpellResponse>, response: Response<SpellResponse>) {
                    response.body()?.spells?.let {
                        spellList.clear()
                        // Drop any stray "All Spells" row that may exist in spell_mst
                        spellList.addAll(it.filter { sp ->
                            !sp.spellName.equals("All Spells", ignoreCase = true)
                        })
                    }
                    filterSpells.clear()
                    filterSpells.addAll(spellList)
                    // Filter spinner shows real spells only; first spell is
                    // selected by default.
                    spFilterSpell.adapter = ArrayAdapter(
                        this@SpinningDoffActivity,
                        R.layout.spinner_item_black,
                        filterSpells.map { it.spellName ?: "" }
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (filterSpells.isNotEmpty()) spFilterSpell.setSelection(0)

                    val entryNames = mutableListOf("-- Select Spell --")
                    entryNames.addAll(spellList.map { it.spellName ?: "" })
                    spEntrySpell.adapter = ArrayAdapter(
                        this@SpinningDoffActivity,
                        R.layout.spinner_item_black, entryNames
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    // Default entry spell = first fetched spell (position 1,
                    // because position 0 is the "-- Select Spell --" placeholder).
                    if (spellList.isNotEmpty()) spEntrySpell.setSelection(1)
                    onDone()
                }
                override fun onFailure(call: Call<SpellResponse>, t: Throwable) { onDone() }
            })
    }

    // ── Master loading ─────────────────────────────────────────────
    private fun loadMasters(onAllDone: () -> Unit) {
        var done = 0
        fun finish() { done++; if (done == 3) {
            spEntryMachine.adapter = ArrayAdapter(
                this, R.layout.spinner_item_black,
                listOf("-- Machine --") + machineList.map { it.toString() }
            ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }

            // Make machine spinner searchable: intercept its touch and show
            // a custom dialog with a search field + filtered list.
            attachSearchableMachinePicker()

            spEntryQuality.adapter = ArrayAdapter(
                this, R.layout.spinner_item_black,
                listOf("-- Quality --") + qualityList.map { it.qualityName ?: "" }
            ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }

            spEntryTrolly.adapter = ArrayAdapter(
                this, R.layout.spinner_item_black,
                listOf("-- Trolly --") + trollyList.map { it.toString() }
            ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }

            onAllDone()
        } }

        RetrofitClient.getApiService(this).getDoffMachines(
            branchId = if (branchId > 0) branchId else null
        ).enqueue(object : Callback<DoffMachineResponse> {
            override fun onResponse(call: Call<DoffMachineResponse>, response: Response<DoffMachineResponse>) {
                response.body()?.machines?.let { machineList.clear(); machineList.addAll(it) }
                finish()
            }
            override fun onFailure(call: Call<DoffMachineResponse>, t: Throwable) { finish() }
        })
        RetrofitClient.getApiService(this).getDoffQualities(
            branchId = if (branchId > 0) branchId else null
        ).enqueue(object : Callback<SpinningQualityResponse> {
            override fun onResponse(call: Call<SpinningQualityResponse>, response: Response<SpinningQualityResponse>) {
                response.body()?.qualities?.let { qualityList.clear(); qualityList.addAll(it) }
                finish()
            }
            override fun onFailure(call: Call<SpinningQualityResponse>, t: Throwable) { finish() }
        })
        RetrofitClient.getApiService(this).getDoffTrollies(
            branchId = if (branchId > 0) branchId else null
        ).enqueue(object : Callback<TrollyResponse> {
            override fun onResponse(call: Call<TrollyResponse>, response: Response<TrollyResponse>) {
                response.body()?.trollies?.let { trollyList.clear(); trollyList.addAll(it) }
                finish()
            }
            override fun onFailure(call: Call<TrollyResponse>, t: Throwable) { finish() }
        })
    }

    // ── Form helpers ───────────────────────────────────────────────
    private fun recalcNet() {
        val gross = etGross.text.toString().toDoubleOrNull() ?: 0.0
        val tare  = etTare.text.toString().toDoubleOrNull()  ?: 0.0
        etNet.setText(String.format(Locale.getDefault(), "%.3f", gross - tare))
    }

    private fun loadDefaultsForMachine() {
        val pos = spEntryMachine.selectedItemPosition - 1
        val mc = machineList.getOrNull(pos) ?: return

        // Always reset quality/trolly/tare before loading; if no last data
        // is found, these stay empty.
        spEntryQuality.setSelection(0)
        spEntryTrolly.setSelection(0)
        etTare.setText("")

        RetrofitClient.getApiService(this).getDoffLastByMachine(mc.mcId)
            .enqueue(object : Callback<DoffDefaultsResponse> {
                override fun onResponse(call: Call<DoffDefaultsResponse>, response: Response<DoffDefaultsResponse>) {
                    val body = response.body() ?: return
                    body.qualityId?.let { id ->
                        val idx = qualityList.indexOfFirst { it.qualityId == id }
                        if (idx >= 0) spEntryQuality.setSelection(idx + 1)
                    }
                    body.trollyId?.let { id ->
                        val idx = trollyList.indexOfFirst { it.trollyId == id }
                        if (idx >= 0) spEntryTrolly.setSelection(idx + 1)
                    }
                }
                override fun onFailure(call: Call<DoffDefaultsResponse>, t: Throwable) {}
            })
    }

    private fun loadIntoForm(item: DoffTransaction) {
        editingId  = item.id
        tvEntryTitle.text = "Edit Doff Entry"
        item.doffDate?.let {
            entryDate = it
            tvEntryDate.text = safeReformat(it)
        }
        item.spellId?.let { id ->
            val idx = spellList.indexOfFirst { it.spellId == id }
            if (idx >= 0) spEntrySpell.setSelection(idx + 1)
        }
        item.mcId?.let { id ->
            val idx = machineList.indexOfFirst { it.mcId == id }
            if (idx >= 0) spEntryMachine.setSelection(idx + 1)
        }
        item.qualityId?.let { id ->
            val idx = qualityList.indexOfFirst { it.qualityId == id }
            if (idx >= 0) spEntryQuality.setSelection(idx + 1)
        }
        item.trollyId?.let { id ->
            val idx = trollyList.indexOfFirst { it.trollyId == id }
            if (idx >= 0) spEntryTrolly.setSelection(idx + 1)
        }
        etGross.setText(item.grossWeight?.let { String.format(Locale.getDefault(), "%.3f", it) } ?: "")
        etTare.setText(item.tareWeight?.let   { String.format(Locale.getDefault(), "%.3f", it) } ?: "")
        etNet.setText(item.netWeight?.let     { String.format(Locale.getDefault(), "%.3f", it) } ?: "")
    }

    private fun clearForm(keepSpell: Boolean = false) {
        editingId = null
        tvEntryTitle.text = "New Doff Entry"
        // Date and Spell are intentionally NOT reset
        spEntryMachine.setSelection(0)
        spEntryQuality.setSelection(0)
        spEntryTrolly.setSelection(0)
        etGross.setText("")
        etTare.setText("")
        etNet.setText("")
    }

    // ── Save entry ─────────────────────────────────────────────────
    private fun saveEntry() {
        val spellPos   = spEntrySpell.selectedItemPosition - 1
        val machinePos = spEntryMachine.selectedItemPosition - 1
        val qualityPos = spEntryQuality.selectedItemPosition - 1
        val trollyPos  = spEntryTrolly.selectedItemPosition - 1

        val spellSel   = spellList.getOrNull(spellPos)
        val mcSel      = machineList.getOrNull(machinePos)
        val qSel       = qualityList.getOrNull(qualityPos)
        val tSel       = trollyList.getOrNull(trollyPos)

        if (spellSel == null || mcSel == null) {
            Toast.makeText(this, "Please select spell and machine", Toast.LENGTH_SHORT).show()
            return
        }
        val gross = etGross.text.toString().toDoubleOrNull()
        if (gross == null || gross <= 0.0) {
            Toast.makeText(this, "Please enter gross weight", Toast.LENGTH_SHORT).show()
            return
        }
        val tare = etTare.text.toString().toDoubleOrNull() ?: 0.0
        val net  = etNet.text.toString().toDoubleOrNull() ?: (gross - tare)

        val req = DoffSaveRequest(
            id           = editingId,
            doffDate     = entryDate,
            spellId      = spellSel.spellId,
            mcId         = mcSel.mcId,
            qualityId    = qSel?.qualityId,
            trollyId     = tSel?.trollyId,
            ebId         = 0,
            grossWeight  = gross,
            tareWeight   = tare,
            netWeight    = net,
            weightType   = null,
            branchId     = if (branchId > 0) branchId else null,
            userId       = userId
        )

        btnSave.isEnabled = false
        RetrofitClient.getApiService(this).saveDoffTransaction(req)
            .enqueue(object : Callback<DoffSaveResponse> {
                override fun onResponse(call: Call<DoffSaveResponse>, response: Response<DoffSaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Failed"
                    Toast.makeText(this@SpinningDoffActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm(keepSpell = true)
                        reloadList()
                    }
                }
                override fun onFailure(call: Call<DoffSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@SpinningDoffActivity,
                        "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Searchable machine picker ──────────────────────────────────
    @Suppress("ClickableViewAccessibility")
    private fun attachSearchableMachinePicker() {
        spEntryMachine.setOnTouchListener { v, ev ->
            if (ev.action == MotionEvent.ACTION_UP) {
                v.performClick()
                showMachineSearchDialog()
            }
            true // consume so the native dropdown never opens
        }
    }

    private fun showMachineSearchDialog() {
        if (machineList.isEmpty()) {
            Toast.makeText(this, "No machines available", Toast.LENGTH_SHORT).show()
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }
        val etSearch = EditText(this).apply {
            hint = "Search machine"
            isSingleLine = true
        }
        val listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(etSearch)
        container.addView(listView)

        // Keep a parallel list of original indices so we can map back to
        // machineList after filtering.
        var filtered: List<Pair<Int, DoffMachine>> =
            machineList.mapIndexed { i, m -> i to m }

        val listAdapter = ArrayAdapter(
            this,
            R.layout.spinner_dropdown_item_black,
            filtered.map { it.second.toString() }.toMutableList()
        )
        listView.adapter = listAdapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Machine")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .create()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim().orEmpty()
                filtered = if (q.isEmpty()) machineList.mapIndexed { i, m -> i to m }
                else machineList.mapIndexedNotNull { i, m ->
                    if (m.toString().contains(q, ignoreCase = true)) i to m else null
                }
                listAdapter.clear()
                listAdapter.addAll(filtered.map { it.second.toString() })
                listAdapter.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        listView.setOnItemClickListener { _, _, pos, _ ->
            val origIndex = filtered.getOrNull(pos)?.first ?: return@setOnItemClickListener
            // +1 because spinner has the "-- Machine --" placeholder at 0
            spEntryMachine.setSelection(origIndex + 1)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun safeReformat(apiDateStr: String): String =
        try { dispDate.format(apiDate.parse(apiDateStr)!!) } catch (_: Exception) { apiDateStr }

    private fun confirmDelete(item: DoffTransaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Doff Entry")
            .setMessage("Delete this doff entry?")
            .setPositiveButton("Delete") { _, _ ->
                val id = item.id ?: return@setPositiveButton
                RetrofitClient.getApiService(this).deleteDoffTransaction(id)
                    .enqueue(object : Callback<DoffSaveResponse> {
                        override fun onResponse(call: Call<DoffSaveResponse>, response: Response<DoffSaveResponse>) {
                            Toast.makeText(this@SpinningDoffActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            reloadList()
                        }
                        override fun onFailure(call: Call<DoffSaveResponse>, t: Throwable) {
                            Toast.makeText(this@SpinningDoffActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
