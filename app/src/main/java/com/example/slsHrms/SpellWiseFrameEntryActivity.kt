package com.example.slsHrms
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.FrameEntryAdapter
import com.example.slsHrms.adapter.WindingEntryAdapter
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
class SpellWiseFrameEntryActivity : AppCompatActivity() {
    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""
    private var entryType = "S"
    // --- Spinning views ---
    private lateinit var rowSelectAll      : LinearLayout
    private lateinit var cbSelectAll       : CheckBox
    private lateinit var tvSelectedCount   : TextView
    private lateinit var layoutSpinning    : View
    private lateinit var rvFrames          : RecyclerView
    private lateinit var progressBar       : ProgressBar
    private lateinit var tvEmpty           : TextView
    private lateinit var tvSpinningFallback: TextView
    private lateinit var btnSave           : Button
    private lateinit var btnReload         : Button
    // --- Common filter views ---
    private lateinit var tvEntryDate     : TextView
    private lateinit var spEntrySpell    : Spinner
    private lateinit var btnTypeSpinning : Button
    private lateinit var btnTypeWinding  : Button
    // --- Winding form views ---
    private lateinit var cardWindingForm : View
    private lateinit var etEbNo          : EditText
    private lateinit var tvEbName        : TextView
    private lateinit var spWindingQuality: Spinner
    private lateinit var btnWindingSave  : Button
    private lateinit var btnWindingClear : Button
    // --- Winding list views ---
    private lateinit var layoutWindingList : View
    private lateinit var tvWindingFallback : TextView
    private lateinit var rvWindingEntries  : RecyclerView
    // --- Adapters ---
    private lateinit var spinningAdapter: FrameEntryAdapter
    private lateinit var windingAdapter : WindingEntryAdapter
    // --- State ---
    private val spellList      = mutableListOf<Spell>()
    private val machineList    = mutableListOf<DoffMachine>()
    private val windingQualityList = mutableListOf<SpinningQuality>()
    /** eb_id resolved from lookup, 0 means none */
    private var resolvedEbId   = 0L
    /** When editing an existing record, its id; else null */
    private var editingEntryId : Int? = null

    private val ebLookupHandler  = Handler(Looper.getMainLooper())
    private var ebLookupRunnable : Runnable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spell_wise_frame_entry)
        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }
        tvEntryDate      = findViewById(R.id.tvEntryDate)
        spEntrySpell     = findViewById(R.id.spEntrySpell)
        btnTypeSpinning  = findViewById(R.id.btnTypeSpinning)
        btnTypeWinding   = findViewById(R.id.btnTypeWinding)
        rowSelectAll     = findViewById(R.id.rowSelectAll)
        cbSelectAll      = findViewById(R.id.cbSelectAll)
        tvSelectedCount  = findViewById(R.id.tvSelectedCount)
        layoutSpinning   = findViewById(R.id.layoutSpinningList)
        rvFrames         = findViewById(R.id.rvFrames)
        progressBar      = findViewById(R.id.progressBar)
        tvEmpty          = findViewById(R.id.tvEmpty)
        tvSpinningFallback = findViewById(R.id.tvSpinningFallback)
        btnSave          = findViewById(R.id.btnSave)
        btnReload        = findViewById(R.id.btnReload)
        cardWindingForm  = findViewById(R.id.cardWindingForm)
        etEbNo           = findViewById(R.id.etEbNo)
        tvEbName         = findViewById(R.id.tvEbName)
        spWindingQuality = findViewById(R.id.spWindingQuality)
        btnWindingSave   = findViewById(R.id.btnWindingSave)
        btnWindingClear  = findViewById(R.id.btnWindingClear)
        layoutWindingList = findViewById(R.id.layoutWindingList)
        tvWindingFallback = findViewById(R.id.tvWindingFallback)
        rvWindingEntries  = findViewById(R.id.rvWindingEntries)
        val cal = Calendar.getInstance()
        entryDate = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)
        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickEntryDate() }
        spinningAdapter = FrameEntryAdapter(onSelectionChanged = { updateSelectedLabel(it) })
        rvFrames.layoutManager = LinearLayoutManager(this)
        rvFrames.adapter = spinningAdapter
        windingAdapter = WindingEntryAdapter(
            onEdit   = { entry -> startEditWinding(entry) },
            onDelete = { entry -> confirmDeleteWinding(entry) }
        )
        rvWindingEntries.layoutManager = LinearLayoutManager(this)
        rvWindingEntries.adapter = windingAdapter
        cbSelectAll.setOnClickListener {
            if (cbSelectAll.isChecked) spinningAdapter.selectAll() else spinningAdapter.clearAll()
        }
        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (entryType == "W") loadWindingEntries() else reloadSpinningEntries()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        btnTypeSpinning.setOnClickListener { switchType("S") }
        btnTypeWinding.setOnClickListener  { switchType("W") }
        updateTypeButtons()
        btnSave.setOnClickListener   { saveSpinningEntries() }
        btnReload.setOnClickListener { if (entryType == "W") loadWindingEntries() else reloadSpinningEntries() }
        // EB No → validate on every change with 500 ms debounce
        etEbNo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                resolvedEbId = 0L
                tvEbName.text = ""
                ebLookupRunnable?.let { ebLookupHandler.removeCallbacks(it) }
                val text = s?.toString()?.trim() ?: ""
                if (text.isEmpty()) return
                val runnable = Runnable { lookupEb() }
                ebLookupRunnable = runnable
                ebLookupHandler.postDelayed(runnable, 500)
            }
        })
        btnWindingSave.setOnClickListener  { saveWindingEntry() }
        btnWindingClear.setOnClickListener { clearWindingForm() }
        if (branchId <= 0) Toast.makeText(this, "Branch not selected", Toast.LENGTH_SHORT).show()
        loadMachines { loadQualities { loadFrameDefaults { loadSpells { reloadSpinningEntries() } } } }
    }
    // ── Type toggle ────────────────────────────────���─────────────
    private fun switchType(type: String) {
        if (entryType == type) return
        entryType = type
        updateTypeButtons()
        applyTypeVisibility()
        if (type == "W") {
            loadWindingQualities { loadWindingEntries() }
        } else {
            loadMachines { loadQualities { loadFrameDefaults { reloadSpinningEntries() } } }
        }
    }
    private fun applyTypeVisibility() {
        val isW = entryType == "W"
        rowSelectAll.visibility     = if (isW) View.GONE  else View.VISIBLE
        layoutSpinning.visibility   = if (isW) View.GONE  else View.VISIBLE
        btnSave.visibility          = if (isW) View.GONE  else View.VISIBLE
        cardWindingForm.visibility  = if (isW) View.VISIBLE else View.GONE
        layoutWindingList.visibility = if (isW) View.VISIBLE else View.GONE
    }
    private fun updateTypeButtons() {
        val active   = ColorStateList.valueOf(0xFF1565C0.toInt())
        val inactive = ColorStateList.valueOf(0xFF757575.toInt())
        btnTypeSpinning.backgroundTintList = if (entryType == "S") active else inactive
        btnTypeWinding.backgroundTintList  = if (entryType == "W") active else inactive
    }
    // ── Date picker ───────────────────────────────────────────────
    private fun pickEntryDate() {
        val cal = Calendar.getInstance()
        try { apiDate.parse(entryDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            entryDate = apiDate.format(cal.time)
            tvEntryDate.text = dispDate.format(cal.time)
            if (entryType == "W") loadWindingEntries() else reloadSpinningEntries()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
    // ── Spinning loaders ──────────────────────────────────────────
    private fun loadMachines(onDone: () -> Unit) {
        if (branchId <= 0) { onDone(); return }
        progressBar.visibility = View.VISIBLE; tvEmpty.visibility = View.GONE
        RetrofitClient.getApiService(this).getFrameMachines(branchId)
            .enqueue(object : Callback<DoffMachineResponse> {
                override fun onResponse(c: Call<DoffMachineResponse>, r: Response<DoffMachineResponse>) {
                    progressBar.visibility = View.GONE
                    machineList.clear(); r.body()?.machines?.let { machineList.addAll(it) }
                    if (machineList.isEmpty()) tvEmpty.visibility = View.VISIBLE
                    spinningAdapter.update(machineList); onDone()
                }
                override fun onFailure(c: Call<DoffMachineResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE; tvEmpty.visibility = View.VISIBLE; onDone()
                }
            })
    }
    private fun loadQualities(onDone: () -> Unit) {
        if (branchId <= 0) { onDone(); return }
        RetrofitClient.getApiService(this).getDoffQualities(branchId)
            .enqueue(object : Callback<SpinningQualityResponse> {
                override fun onResponse(c: Call<SpinningQualityResponse>, r: Response<SpinningQualityResponse>) {
                    spinningAdapter.setQualities(r.body()?.qualities.orEmpty()); onDone()
                }
                override fun onFailure(c: Call<SpinningQualityResponse>, t: Throwable) { onDone() }
            })
    }
    private fun loadFrameDefaults(onDone: () -> Unit) {
        if (branchId <= 0) { onDone(); return }
        RetrofitClient.getApiService(this).getLastQualityByMc(branchId, "S")
            .enqueue(object : Callback<LastQualityByMcResponse> {
                override fun onResponse(c: Call<LastQualityByMcResponse>, r: Response<LastQualityByMcResponse>) {
                    val map = r.body()?.data.orEmpty()
                        .mapKeys { it.key.toIntOrNull() ?: -1 }.filterKeys { it >= 0 }
                    if (map.isNotEmpty()) spinningAdapter.setQualityDefaults(map, replaceAll = true)
                    onDone()
                }
                override fun onFailure(c: Call<LastQualityByMcResponse>, t: Throwable) { onDone() }
            })
    }
    private fun loadSpells(onDone: () -> Unit) {
        RetrofitClient.getApiService(this).getSpells(branchId.takeIf { it > 0 })
            .enqueue(object : Callback<SpellResponse> {
                override fun onResponse(c: Call<SpellResponse>, r: Response<SpellResponse>) {
                    spellList.clear()
                    r.body()?.spells?.let { spellList.addAll(it.filter { s -> !s.spellName.equals("All Spells", ignoreCase = true) }) }
                    spEntrySpell.adapter = ArrayAdapter(this@SpellWiseFrameEntryActivity,
                        R.layout.spinner_item_black, spellList.map { it.spellName ?: "" })
                        .also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (spellList.isNotEmpty()) spEntrySpell.setSelection(0)
                    onDone()
                }
                override fun onFailure(c: Call<SpellResponse>, t: Throwable) { onDone() }
            })
    }
    private fun reloadSpinningEntries() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) return
        tvSpinningFallback.visibility = View.GONE
        RetrofitClient.getApiService(this).getFrameEntries(entryDate, spellId, branchId)
            .enqueue(object : Callback<FrameEntriesResponse> {
                override fun onResponse(c: Call<FrameEntriesResponse>, r: Response<FrameEntriesResponse>) {
                    val body    = r.body()
                    val entries = body?.entries.orEmpty()
                    val ids     = if (entries.isNotEmpty()) entries.map { it.mcId }
                                  else body?.mcIds.orEmpty()
                    val qmap    = entries.mapNotNull { e -> e.qualityId?.let { e.mcId to it } }.toMap()
                    if (qmap.isNotEmpty()) spinningAdapter.setQualityDefaults(qmap, replaceAll = false)
                    spinningAdapter.setSelected(ids)
                    cbSelectAll.isChecked = ids.size == machineList.size && machineList.isNotEmpty()
                    // Auto-insert from fallback date when no records found
                    if (body?.isFallback == true && !body.fallbackDate.isNullOrEmpty()) {
                        tvSpinningFallback.text = "No records for selected date. Loaded from: ${body.fallbackDate}"
                        tvSpinningFallback.visibility = View.VISIBLE
                        AlertDialog.Builder(this@SpellWiseFrameEntryActivity)
                            .setTitle("Auto Copy")
                            .setMessage("No records found for selected date/spell.\nCopy data from ${body.fallbackDate}?")
                            .setPositiveButton("Yes") { _, _ -> autoSaveSpinningEntries(spellId) }
                            .setNegativeButton("No", null)
                            .show()
                    } else {
                        tvSpinningFallback.visibility = View.GONE
                    }
                }
                override fun onFailure(c: Call<FrameEntriesResponse>, t: Throwable) {
                    spinningAdapter.setSelected(emptyList())
                    tvSpinningFallback.visibility = View.GONE
                }
            })
    }

    private fun autoSaveSpinningEntries(spellId: Int) {
        val entries = spinningAdapter.selectedEntries()
        if (entries.isEmpty()) { toast("No frame entries to copy"); return }
        if (entries.any { it.qualityId == null }) { toast("Quality missing in fallback data, please set manually"); return }
        val req = FrameEntriesSaveRequest(date = entryDate, spellId = spellId, branchId = branchId, userId = userId, entries = entries)
        RetrofitClient.getApiService(this).saveFrameEntries(req)
            .enqueue(object : Callback<FrameEntriesSaveResponse> {
                override fun onResponse(c: Call<FrameEntriesSaveResponse>, r: Response<FrameEntriesSaveResponse>) {
                    toast("Auto-copied: ${r.body()?.message ?: "Done"}")
                    tvSpinningFallback.visibility = View.GONE
                    reloadSpinningEntries()
                }
                override fun onFailure(c: Call<FrameEntriesSaveResponse>, t: Throwable) {
                    toast("Auto-copy failed: ${t.localizedMessage}")
                }
            })
    }
    // ── Winding loaders ───────────────────────────────────────────
    private fun loadWindingQualities(onDone: () -> Unit = {}) {
        RetrofitClient.getApiService(this).getWindingQualities(branchId.takeIf { it > 0 })
            .enqueue(object : Callback<WindingQualityResponse> {
                override fun onResponse(c: Call<WindingQualityResponse>, r: Response<WindingQualityResponse>) {
                    windingQualityList.clear()
                    r.body()?.qualities?.let { windingQualityList.addAll(it) }
                    spWindingQuality.adapter = ArrayAdapter(this@SpellWiseFrameEntryActivity,
                        R.layout.spinner_item_black, windingQualityList.map { it.qualityName ?: "" })
                        .also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    onDone()
                }
                override fun onFailure(c: Call<WindingQualityResponse>, t: Throwable) { onDone() }
            })
    }
    private fun loadWindingEntries() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) return
        RetrofitClient.getApiService(this).getWindingEntries(entryDate, spellId, branchId)
            .enqueue(object : Callback<WindingEntriesResponse> {
                override fun onResponse(c: Call<WindingEntriesResponse>, r: Response<WindingEntriesResponse>) {
                    val body = r.body()
                    if (body?.isFallback == true && !body.fallbackDate.isNullOrEmpty()) {
                        tvWindingFallback.text = "No records for selected date. Loaded from: ${body.fallbackDate}"
                        tvWindingFallback.visibility = View.VISIBLE
                        windingAdapter.update(body.entries.orEmpty())
                        AlertDialog.Builder(this@SpellWiseFrameEntryActivity)
                            .setTitle("Auto Copy")
                            .setMessage("No records found for selected date/spell.\nCopy winding data from ${body.fallbackDate}?")
                            .setPositiveButton("Yes") { _, _ -> autoSaveWindingEntries(spellId, body.entries.orEmpty()) }
                            .setNegativeButton("No", null)
                            .show()
                    } else {
                        tvWindingFallback.visibility = View.GONE
                        windingAdapter.update(body?.entries.orEmpty())
                    }
                }
                override fun onFailure(c: Call<WindingEntriesResponse>, t: Throwable) {
                    Toast.makeText(this@SpellWiseFrameEntryActivity, "Failed to load records", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun autoSaveWindingEntries(spellId: Int, entries: List<WindingEntry>) {
        if (entries.isEmpty()) { toast("No winding entries to copy"); return }
        var saved = 0; var failed = 0
        val total = entries.size
        for (entry in entries) {
            val ebId = entry.ebId ?: continue
            RetrofitClient.getApiService(this).saveWindingEntry(
                WindingEntrySaveRequest(
                    date = entryDate, spellId = spellId, branchId = branchId,
                    ebId = ebId, qualityId = entry.qualityId, userId = userId
                )
            ).enqueue(object : Callback<WindingEntrySaveResponse> {
                override fun onResponse(c: Call<WindingEntrySaveResponse>, r: Response<WindingEntrySaveResponse>) {
                    if (r.isSuccessful && r.body()?.status == "success") saved++ else failed++
                    if (saved + failed == total) {
                        toast("Auto-copied: $saved saved, $failed failed")
                        tvWindingFallback.visibility = View.GONE
                        loadWindingEntries()
                    }
                }
                override fun onFailure(c: Call<WindingEntrySaveResponse>, t: Throwable) {
                    failed++
                    if (saved + failed == total) {
                        toast("Auto-copy done: $saved saved, $failed failed")
                        loadWindingEntries()
                    }
                }
            })
        }
    }
    private fun lookupEb() {
        val ebText = etEbNo.text?.toString()?.trim() ?: return
        val ebNo = ebText.toLongOrNull() ?: return
        if (ebNo <= 0) return
        RetrofitClient.getApiService(this).windingEbLookup(ebNo, branchId)
            .enqueue(object : Callback<WindingEbLookupResponse> {
                override fun onResponse(c: Call<WindingEbLookupResponse>, r: Response<WindingEbLookupResponse>) {
                    val body = r.body()
                    if (body?.status == "success" && body.ebId != null) {
                        resolvedEbId = body.ebId
                        tvEbName.text = body.empName ?: ""
                    } else {
                        resolvedEbId = 0L
                        tvEbName.text = ""
                        Toast.makeText(this@SpellWiseFrameEntryActivity, "Employee not found", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(c: Call<WindingEbLookupResponse>, t: Throwable) {
                    resolvedEbId = 0L; tvEbName.text = ""
                }
            })
    }
    private fun saveWindingEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) { toast("Please select spell"); return }
        if (branchId <= 0)   { toast("Branch not selected"); return }
        // If editing, allow saving without re-resolving EB if already set
        if (resolvedEbId <= 0L) {
            val ebText = etEbNo.text?.toString()?.trim() ?: ""
            if (ebText.isEmpty()) { toast("Enter EB No"); return }
            toast("Validating EB No..."); lookupEb(); return
        }
        val qIdx = spWindingQuality.selectedItemPosition
        val qualityId = windingQualityList.getOrNull(qIdx)?.qualityId
        val editId = editingEntryId
        if (editId != null) {
            // Update existing
            btnWindingSave.isEnabled = false
            RetrofitClient.getApiService(this).updateWindingEntry(
                editId,
                WindingEntryUpdateRequest(ebId = resolvedEbId, qualityId = qualityId)
            ).enqueue(object : Callback<WindingEntrySaveResponse> {
                override fun onResponse(c: Call<WindingEntrySaveResponse>, r: Response<WindingEntrySaveResponse>) {
                    btnWindingSave.isEnabled = true
                    toast(r.body()?.message ?: "Updated")
                    clearWindingForm(); loadWindingEntries()
                }
                override fun onFailure(c: Call<WindingEntrySaveResponse>, t: Throwable) {
                    btnWindingSave.isEnabled = true; toast("Error: ${t.localizedMessage}")
                }
            })
        } else {
            // New
            btnWindingSave.isEnabled = false
            RetrofitClient.getApiService(this).saveWindingEntry(
                WindingEntrySaveRequest(
                    date = entryDate, spellId = spellId, branchId = branchId,
                    ebId = resolvedEbId, qualityId = qualityId, userId = userId
                )
            ).enqueue(object : Callback<WindingEntrySaveResponse> {
                override fun onResponse(c: Call<WindingEntrySaveResponse>, r: Response<WindingEntrySaveResponse>) {
                    btnWindingSave.isEnabled = true
                    toast(r.body()?.message ?: "Saved")
                    clearWindingForm(); loadWindingEntries()
                }
                override fun onFailure(c: Call<WindingEntrySaveResponse>, t: Throwable) {
                    btnWindingSave.isEnabled = true; toast("Error: ${t.localizedMessage}")
                }
            })
        }
    }
    private fun startEditWinding(entry: WindingEntry) {
        editingEntryId  = entry.id
        resolvedEbId    = entry.ebId ?: 0L
        etEbNo.setText(entry.ebId?.toString() ?: "")
        tvEbName.text   = entry.empName ?: ""
        // select quality
        val qIdx = windingQualityList.indexOfFirst { it.qualityId == entry.qualityId }
        if (qIdx >= 0) spWindingQuality.setSelection(qIdx)
        btnWindingSave.text = "UPDATE"
        etEbNo.requestFocus()
    }
    private fun confirmDeleteWinding(entry: WindingEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete record for EB ${entry.ebId} (${entry.empName})?")
            .setPositiveButton("Delete") { _, _ ->
                val id = entry.id ?: return@setPositiveButton
                RetrofitClient.getApiService(this).deleteWindingEntry(id)
                    .enqueue(object : Callback<WindingEntrySaveResponse> {
                        override fun onResponse(c: Call<WindingEntrySaveResponse>, r: Response<WindingEntrySaveResponse>) {
                            toast(r.body()?.message ?: "Deleted"); loadWindingEntries()
                        }
                        override fun onFailure(c: Call<WindingEntrySaveResponse>, t: Throwable) { toast("Error") }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun clearWindingForm() {
        editingEntryId = null; resolvedEbId = 0L
        etEbNo.text?.clear(); tvEbName.text = ""
        if (windingQualityList.isNotEmpty()) spWindingQuality.setSelection(0)
        btnWindingSave.text = "SAVE"
    }
    // ── Spinning save ──────────────────────────────────────────────
    private fun saveSpinningEntries() {
        val spellId = selectedSpellId()
        if (spellId == null) { toast("Please select spell"); return }
        if (branchId <= 0)   { toast("Branch not selected"); return }
        val entries = spinningAdapter.selectedEntries()
        if (entries.any { it.qualityId == null }) { toast("Please select quality for all selected frames"); return }
        val req = FrameEntriesSaveRequest(date = entryDate, spellId = spellId, branchId = branchId, userId = userId, entries = entries)
        btnSave.isEnabled = false
        RetrofitClient.getApiService(this).saveFrameEntries(req)
            .enqueue(object : Callback<FrameEntriesSaveResponse> {
                override fun onResponse(c: Call<FrameEntriesSaveResponse>, r: Response<FrameEntriesSaveResponse>) {
                    btnSave.isEnabled = true
                    toast(r.body()?.message ?: if (r.isSuccessful) "Saved" else "Failed")
                }
                override fun onFailure(c: Call<FrameEntriesSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true; toast("Error: ${t.localizedMessage}")
                }
            })
    }
    // ── Helpers ───────────────────────────────────────────────────
    private fun selectedSpellId(): Int? = spellList.getOrNull(spEntrySpell.selectedItemPosition)?.spellId
    private fun updateSelectedLabel(count: Int) {
        val total = spinningAdapter.totalCount()
        tvSelectedCount.text = String.format(Locale.getDefault(), "Selected: %d / %d", count, total)
        if (total > 0 && count == total) { if (!cbSelectAll.isChecked) cbSelectAll.isChecked = true }
        else if (count == 0) { if (cbSelectAll.isChecked) cbSelectAll.isChecked = false }
    }
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
