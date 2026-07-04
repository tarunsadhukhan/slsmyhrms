package com.example.slsHrms

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class AssortingEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate      : TextView
    private lateinit var spShedType       : Spinner
    private lateinit var llMachines       : LinearLayout
    private lateinit var tvMachinesStatus : TextView
    private lateinit var llSelectors      : LinearLayout
    private lateinit var tvSelectorsStatus: TextView
    private lateinit var llQualities      : LinearLayout
    private lateinit var tvQualitiesStatus: TextView
    private lateinit var etTrollyNo       : EditText
    private lateinit var etGrossWt        : EditText
    private lateinit var etTareWt         : EditText
    private lateinit var etNetWt          : EditText
    private lateinit var btnSave          : Button
    private lateinit var rvEntries        : RecyclerView
    private lateinit var tvEntriesEmpty   : TextView
    private lateinit var pbEntries        : ProgressBar

    private val shedList      = mutableListOf<String>()
    private val machinesList  = mutableListOf<SpreaderMachine>()
    private val selectorsList = mutableListOf<Selector>()
    private val qualitiesList = mutableListOf<SpreaderQuality>()

    private var selectedShedType  : String? = null
    private var selectedMcId      : Int? = null
    private var selectedSelectorId: Int? = null
    private var selectedQualityId : Int? = null
    private var validatedTrollyId : Int? = null
    private var editingEntryId    : Int? = null

    private val adapter = EntriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assorting_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate       = findViewById(R.id.tvEntryDate)
        spShedType        = findViewById(R.id.spShedType)
        llMachines        = findViewById(R.id.llMachines)
        tvMachinesStatus  = findViewById(R.id.tvMachinesStatus)
        llSelectors       = findViewById(R.id.llSelectors)
        tvSelectorsStatus = findViewById(R.id.tvSelectorsStatus)
        llQualities       = findViewById(R.id.llQualities)
        tvQualitiesStatus = findViewById(R.id.tvQualitiesStatus)
        etTrollyNo        = findViewById(R.id.etTrollyNo)
        etGrossWt         = findViewById(R.id.etGrossWt)
        etTareWt          = findViewById(R.id.etTareWt)
        etNetWt           = findViewById(R.id.etNetWt)
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

        spShedType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedShedType = shedList.getOrNull(pos)
                loadMachines()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        etTrollyNo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString()?.trim().orEmpty()
                if (raw.isEmpty()) {
                    validatedTrollyId = null
                    etTareWt.setText("")
                    recalcNet()
                    etTrollyNo.setBackgroundResource(R.drawable.bg_input_rounded_light)
                } else {
                    validateTrolly(raw)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcNet() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etGrossWt.addTextChangedListener(watcher)
        etTareWt.addTextChangedListener(watcher)

        btnSave.setOnClickListener { saveEntry() }

        loadSheds()
        loadSelectors()
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

    // ── Sheds ────────────────────────────────────────────────────────────────

    private fun loadSheds() {
        if (branchId <= 0) {
            Toast.makeText(this, "Branch not set", Toast.LENGTH_SHORT).show()
            return
        }
        RetrofitClient.getApiService(this).getAssortingSheds(branchId)
            .enqueue(object : Callback<AssortingShedsResponse> {
                override fun onResponse(call: Call<AssortingShedsResponse>, response: Response<AssortingShedsResponse>) {
                    shedList.clear()
                    shedList.addAll(response.body()?.sheds.orEmpty())
                    spShedType.adapter = ArrayAdapter(
                        this@AssortingEntryActivity,
                        R.layout.spinner_item_black,
                        shedList
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (shedList.isEmpty()) {
                        selectedShedType = null
                        machinesList.clear()
                        llMachines.removeAllViews()
                        tvMachinesStatus.text = "No shed types found"
                        tvMachinesStatus.visibility = View.VISIBLE
                    }
                    // Selection of position 0 fires onItemSelected, which loads machines
                }
                override fun onFailure(call: Call<AssortingShedsResponse>, t: Throwable) {
                    Toast.makeText(this@AssortingEntryActivity,
                        "Failed to load shed types", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Machines ─────────────────────────────────────────────────────────────

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

        RetrofitClient.getApiService(this).getAssortingMachines(branchId, selectedShedType)
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
        loadEntries()
    }

    // ── Selectors ────────────────────────────────────────────────────────────

    private fun loadSelectors() {
        if (branchId <= 0) {
            tvSelectorsStatus.text = "Branch not set"
            tvSelectorsStatus.visibility = View.VISIBLE
            return
        }
        llSelectors.removeAllViews()
        selectedSelectorId = null
        tvSelectorsStatus.visibility = View.VISIBLE
        tvSelectorsStatus.text = "Loading…"

        RetrofitClient.getApiService(this).getAssortingSelectors(branchId)
            .enqueue(object : Callback<SelectorResponse> {
                override fun onResponse(call: Call<SelectorResponse>, response: Response<SelectorResponse>) {
                    selectorsList.clear()
                    selectorsList.addAll(response.body()?.selectors.orEmpty())
                    if (selectorsList.isEmpty()) {
                        tvSelectorsStatus.text = "No selectors found"
                        tvSelectorsStatus.visibility = View.VISIBLE
                    } else {
                        tvSelectorsStatus.visibility = View.GONE
                        renderSelectorButtons()
                    }
                }
                override fun onFailure(call: Call<SelectorResponse>, t: Throwable) {
                    tvSelectorsStatus.text = "Failed to load selectors"
                    tvSelectorsStatus.visibility = View.VISIBLE
                }
            })
    }

    private fun renderSelectorButtons() {
        llSelectors.removeAllViews()
        val dp = resources.displayMetrics.density
        for (s in selectorsList) {
            val btn = Button(this)
            btn.text = s.label().take(12)
            btn.textSize = 11f
            btn.setTypeface(null, Typeface.BOLD)
            btn.setTextColor(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#00838F"))
            val lp = LinearLayout.LayoutParams((84 * dp).toInt(), (36 * dp).toInt())
            lp.setMargins(0, 0, (6 * dp).toInt(), 0)
            btn.layoutParams = lp
            btn.setPadding(0, 0, 0, 0)
            btn.tag = s
            btn.setOnClickListener { onSelectorSelected(btn, s) }
            llSelectors.addView(btn)
        }
    }

    private fun onSelectorSelected(btn: Button, s: Selector) {
        selectedSelectorId = s.selectorId
        for (i in 0 until llSelectors.childCount) {
            val child = llSelectors.getChildAt(i) as? Button ?: continue
            child.setBackgroundColor(
                if (child == btn) Color.parseColor("#2E7D32")
                else              Color.parseColor("#00838F")
            )
        }
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

        RetrofitClient.getApiService(this).getAssortingQualities(branchId)
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

    // ── Trolly ───────────────────────────────────────────────────────────────

    private fun validateTrolly(trollyNo: String) {
        RetrofitClient.getApiService(this).validateAssortingTrolly(trollyNo, branchId.takeIf { it > 0 })
            .enqueue(object : Callback<AssortingTrollyValidateResponse> {
                override fun onResponse(call: Call<AssortingTrollyValidateResponse>, response: Response<AssortingTrollyValidateResponse>) {
                    val body = response.body()
                    if (response.isSuccessful && body?.status == "success" && body.trollyId != null) {
                        validatedTrollyId = body.trollyId
                        etTrollyNo.setBackgroundResource(R.drawable.bg_input_valid)
                        body.trollyWeight?.let {
                            etTareWt.setText(fmt(it))
                        } ?: etTareWt.setText("")
                        recalcNet()
                    } else {
                        validatedTrollyId = null
                        etTrollyNo.setBackgroundResource(R.drawable.bg_input_invalid)
                        etTareWt.setText("")
                        recalcNet()
                    }
                }
                override fun onFailure(call: Call<AssortingTrollyValidateResponse>, t: Throwable) {
                    validatedTrollyId = null
                    etTrollyNo.setBackgroundResource(R.drawable.bg_input_invalid)
                    etTareWt.setText("")
                    recalcNet()
                }
            })
    }

    // ── Net Wt computation ───────────────────────────────────────────────────

    private fun recalcNet() {
        val gross = etGrossWt.text.toString().toDoubleOrNull() ?: 0.0
        val tare  = etTareWt.text.toString().toDoubleOrNull()  ?: 0.0
        val net   = gross - tare
        etNetWt.setText(fmt(net))
        if (net < 0) {
            etNetWt.setBackgroundResource(R.drawable.bg_input_invalid)
        } else {
            etNetWt.setBackgroundResource(R.drawable.bg_input_readonly)
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private fun saveEntry() {
        if (branchId <= 0) {
            Toast.makeText(this, "Branch not set", Toast.LENGTH_SHORT).show(); return
        }
        val mcId = selectedMcId
        if (mcId == null) {
            Toast.makeText(this, "Please select a Machine", Toast.LENGTH_SHORT).show(); return
        }
        val selectorId = selectedSelectorId
        if (selectorId == null) {
            Toast.makeText(this, "Please select a Selector", Toast.LENGTH_SHORT).show(); return
        }
        val qualityId = selectedQualityId
        if (qualityId == null) {
            Toast.makeText(this, "Please select a Quality", Toast.LENGTH_SHORT).show(); return
        }
        val trollyNoTxt = etTrollyNo.text.toString().trim()
        if (trollyNoTxt.isNotEmpty() && validatedTrollyId == null) {
            Toast.makeText(this, "Trolly No is invalid", Toast.LENGTH_SHORT).show(); return
        }
        val gross = etGrossWt.text.toString().toDoubleOrNull() ?: 0.0
        val tare  = etTareWt.text.toString().toDoubleOrNull()  ?: 0.0
        val net   = gross - tare
        if (net < 0) {
            Toast.makeText(this, "Net Wt cannot be negative", Toast.LENGTH_SHORT).show(); return
        }
        val shedType = spShedType.selectedItem?.toString()

        btnSave.isEnabled = false
        val req = AssortingSaveRequest(
            date       = entryDate,
            shedType   = shedType,
            branchId   = branchId,
            mcId       = mcId,
            selectorId = selectorId,
            qualityId  = qualityId,
            trollyId   = validatedTrollyId,
            trollyNo   = trollyNoTxt.ifBlank { null },
            grossWt    = gross,
            tareWt     = tare,
            netWt      = net,
            userId     = userId
        )

        val editId = editingEntryId
        val callback = object : Callback<AssortingSaveResponse> {
            override fun onResponse(call: Call<AssortingSaveResponse>, response: Response<AssortingSaveResponse>) {
                btnSave.isEnabled = true
                val msg = response.body()?.message
                    ?: if (response.isSuccessful) (if (editId != null) "Updated" else "Saved")
                       else (if (editId != null) "Update failed" else "Save failed")
                Toast.makeText(this@AssortingEntryActivity, msg, Toast.LENGTH_SHORT).show()
                if (response.isSuccessful && response.body()?.status == "success") {
                    clearForm()
                    loadEntries()
                }
            }
            override fun onFailure(call: Call<AssortingSaveResponse>, t: Throwable) {
                btnSave.isEnabled = true
                Toast.makeText(this@AssortingEntryActivity,
                    "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
        if (editId != null) {
            RetrofitClient.getApiService(this).updateAssortingEntry(editId, req).enqueue(callback)
        } else {
            RetrofitClient.getApiService(this).saveAssortingEntry(req).enqueue(callback)
        }
    }

    // ── Begin Edit ───────────────────────────────────────────────────────────

    private fun beginEdit(e: AssortingEntry) {
        val id = e.id ?: return
        editingEntryId = id
        btnSave.text = "Update"

        // Shed type
        e.shedType?.let { shed ->
            val pos = shedList.indexOf(shed)
            if (pos >= 0) spShedType.setSelection(pos)
        }

        // Machine
        e.mcId?.let { mid ->
            selectedMcId = mid
            for (i in 0 until llMachines.childCount) {
                val child = llMachines.getChildAt(i) as? Button ?: continue
                val tag = child.tag as? SpreaderMachine
                child.setBackgroundColor(
                    if (tag?.mcId == mid) Color.parseColor("#2E7D32")
                    else                  Color.parseColor("#1565C0")
                )
            }
        }

        // Selector
        e.selectorId?.let { sid ->
            selectedSelectorId = sid
            for (i in 0 until llSelectors.childCount) {
                val child = llSelectors.getChildAt(i) as? Button ?: continue
                val tag = child.tag as? Selector
                child.setBackgroundColor(
                    if (tag?.selectorId == sid) Color.parseColor("#2E7D32")
                    else                        Color.parseColor("#00838F")
                )
            }
        }

        // Quality
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

        // Trolly + weights
        etTrollyNo.setText(e.trollyNo ?: "")
        validatedTrollyId = e.trollyId
        if (e.trollyId != null) {
            etTrollyNo.setBackgroundResource(R.drawable.bg_input_valid)
        } else {
            etTrollyNo.setBackgroundResource(R.drawable.bg_input_rounded_light)
        }
        etGrossWt.setText(e.grossWt?.let { fmt(it) } ?: "")
        etTareWt.setText(e.tareWt?.let { fmt(it) } ?: "")
        recalcNet()

        Toast.makeText(this, "Editing entry #$id", Toast.LENGTH_SHORT).show()
    }

    private fun clearForm() {
        etTrollyNo.setText("")
        etTrollyNo.setBackgroundResource(R.drawable.bg_input_rounded_light)
        etGrossWt.setText("")
        etTareWt.setText("")
        etNetWt.setText("")
        etNetWt.setBackgroundResource(R.drawable.bg_input_readonly)
        validatedTrollyId = null
        selectedSelectorId = null
        selectedQualityId = null
        editingEntryId = null
        btnSave.text = "Save"
        for (i in 0 until llSelectors.childCount) {
            (llSelectors.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#00838F"))
        }
        for (i in 0 until llQualities.childCount) {
            (llQualities.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#6A1B9A"))
        }
    }

    private fun fmt(v: Double?) = String.format(Locale.getDefault(), "%.3f", v ?: 0.0)

    // ── Entries list ─────────────────────────────────────────────────────────

    private fun loadEntries() {
        if (branchId <= 0) return
        pbEntries.visibility      = View.VISIBLE
        tvEntriesEmpty.visibility = View.GONE
        rvEntries.visibility      = View.GONE

        RetrofitClient.getApiService(this).getAssortingEntries(
            date     = entryDate,
            branchId = branchId,
            mcId     = selectedMcId
        ).enqueue(object : Callback<AssortingEntryListResponse> {
            override fun onResponse(call: Call<AssortingEntryListResponse>, response: Response<AssortingEntryListResponse>) {
                pbEntries.visibility = View.GONE
                val list = response.body()?.entries.orEmpty()
                if (list.isEmpty()) {
                    tvEntriesEmpty.visibility = View.VISIBLE
                } else {
                    rvEntries.visibility = View.VISIBLE
                    adapter.update(list)
                }
            }
            override fun onFailure(call: Call<AssortingEntryListResponse>, t: Throwable) {
                pbEntries.visibility      = View.GONE
                tvEntriesEmpty.visibility = View.VISIBLE
                tvEntriesEmpty.text       = "Failed to load entries"
            }
        })
    }

    private fun confirmDelete(e: AssortingEntry) {
        val id = e.id ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("Mc ${e.mcNo ?: "-"} | Trolly ${e.trollyNo ?: "-"} | Net ${fmt(e.netWt)}")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteAssortingEntry(id)
                    .enqueue(object : Callback<AssortingSaveResponse> {
                        override fun onResponse(call: Call<AssortingSaveResponse>, response: Response<AssortingSaveResponse>) {
                            Toast.makeText(this@AssortingEntryActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            loadEntries()
                        }
                        override fun onFailure(call: Call<AssortingSaveResponse>, t: Throwable) {
                            Toast.makeText(this@AssortingEntryActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.VH>() {
        private val items = mutableListOf<AssortingEntry>()
        fun update(list: List<AssortingEntry>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_assorting_entry, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvMcNo     = v.findViewById<TextView>(R.id.tvMcNo)
            private val tvSelector = v.findViewById<TextView>(R.id.tvSelector)
            private val tvQuality  = v.findViewById<TextView>(R.id.tvQuality)
            private val tvTrollyNo = v.findViewById<TextView>(R.id.tvTrollyNo)
            private val tvNetWt    = v.findViewById<TextView>(R.id.tvNetWt)
            private val ivEdit     = v.findViewById<ImageView>(R.id.ivEdit)
            private val ivDelete   = v.findViewById<ImageView>(R.id.ivDelete)
            fun bind(e: AssortingEntry) {
                tvMcNo.text     = e.mcNo ?: "-"
                tvSelector.text = e.selectorName ?: "-"
                tvQuality.text  = e.quality ?: "-"
                tvTrollyNo.text = e.trollyNo ?: "-"
                tvNetWt.text    = fmt(e.netWt)
                ivEdit.setOnClickListener   { beginEdit(e) }
                ivDelete.setOnClickListener { confirmDelete(e) }
            }
        }
    }
}
