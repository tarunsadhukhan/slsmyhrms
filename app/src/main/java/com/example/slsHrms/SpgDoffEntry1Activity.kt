package com.example.slsHrms

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.Spg1SummaryAdapter
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SpgDoffEntry1Activity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId = 0
    private var userId   = 0
    private var entryDate = ""

    private lateinit var tvEntryDate: TextView
    private lateinit var spEntrySpell: Spinner
    private lateinit var llMechCodes: LinearLayout
    private lateinit var tvMechCodesStatus: TextView
    private lateinit var etTrollyNo: EditText
    private lateinit var tvTrollyInfo: TextView
    private lateinit var etGrossWt: EditText
    private lateinit var etTareWt: EditText
    private lateinit var etNetWt: EditText
    private lateinit var btnSave: Button
    private lateinit var rvSummary: RecyclerView
    private lateinit var tvSummaryEmpty: TextView
    private lateinit var pbSummary: ProgressBar
    private lateinit var ivQualityShiftReport: ImageView

    private val spellList = mutableListOf<Spell>()
    private lateinit var summaryAdapter: Spg1SummaryAdapter

    // Selected machine from mech-code buttons
    private var selectedMcId: Int? = null
    private var selectedMechCode: Int? = null

    // Validated trolly
    private var validatedTrollyId: Int? = null
    private var validatedTareWt: Double = 0.0

    private val mainHandler = Handler(Looper.getMainLooper())
    private var trollyDebounce: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spg_doff_entry1)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate       = findViewById(R.id.tvEntryDate)
        spEntrySpell      = findViewById(R.id.spEntrySpell)
        llMechCodes       = findViewById(R.id.llMechCodes)
        tvMechCodesStatus = findViewById(R.id.tvMechCodesStatus)
        etTrollyNo        = findViewById(R.id.etTrollyNo)
        tvTrollyInfo      = findViewById(R.id.tvTrollyInfo)
        etGrossWt         = findViewById(R.id.etGrossWt)
        etTareWt          = findViewById(R.id.etTareWt)
        etNetWt           = findViewById(R.id.etNetWt)
        btnSave           = findViewById(R.id.btnSave)
        rvSummary         = findViewById(R.id.rvSummary)
        tvSummaryEmpty    = findViewById(R.id.tvSummaryEmpty)
        pbSummary         = findViewById(R.id.pbSummary)
        ivQualityShiftReport = findViewById(R.id.ivQualityShiftReport)

        summaryAdapter = Spg1SummaryAdapter { row -> showDetailDialog(row) }
        rvSummary.layoutManager = LinearLayoutManager(this)
        rvSummary.adapter = summaryAdapter
        rvSummary.isNestedScrollingEnabled = false

        // Report icon click listener
        ivQualityShiftReport.setOnClickListener { showQualityShiftReport() }

        // Init date
        val cal = Calendar.getInstance()
        entryDate = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)

        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickDate() }

        // Gross weight watcher → recalculate net
        etGrossWt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcNet() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        // Trolly No: debounced lookup
        etTrollyNo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                resetTrolly()
                trollyDebounce?.let { mainHandler.removeCallbacks(it) }
                val v = s?.toString()?.trim().orEmpty()
                if (v.isEmpty()) return
                val r = Runnable { lookupTrolly(v) }
                trollyDebounce = r
                mainHandler.postDelayed(r, 500)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        etTrollyNo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = etTrollyNo.text.toString().trim()
                if (v.isNotEmpty() && validatedTrollyId == null) lookupTrolly(v)
            }
        }

        // Spell change → reload mech codes
        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadMechCodes()
                loadSummary()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveEntry() }

        loadSpells {
            loadMechCodes()
            loadSummary()
        }
    }

    // ── Date picker ─────────────────────────────────────────────────────────
    private fun pickDate() {
        val cal = Calendar.getInstance()
        try { apiDate.parse(entryDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            entryDate = apiDate.format(cal.time)
            tvEntryDate.text = dispDate.format(cal.time)
            loadMechCodes()
            loadSummary()
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
                        this@SpgDoffEntry1Activity,
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

    // ── Mech Codes ───────────────────────────────────────────────────────────
    private fun loadMechCodes() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) return

        llMechCodes.removeAllViews()
        selectedMcId     = null
        selectedMechCode = null
        tvMechCodesStatus.visibility = View.VISIBLE
        tvMechCodesStatus.text = "Loading…"

        RetrofitClient.getApiService(this).getSpg1MechCodes(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId
        ).enqueue(object : Callback<Spg1MechCodesResponse> {
            override fun onResponse(call: Call<Spg1MechCodesResponse>, response: Response<Spg1MechCodesResponse>) {
                val codes = response.body()?.codes.orEmpty()
                if (codes.isEmpty()) {
                    tvMechCodesStatus.text = "No machines found for selected date/spell"
                    tvMechCodesStatus.visibility = View.VISIBLE
                } else {
                    tvMechCodesStatus.visibility = View.GONE
                    llMechCodes.removeAllViews()
                    for (code in codes) {
                        val rawLabel = code.mechPostingCode?.toString()
                            ?: code.mcCode?.takeIf { it.isNotBlank() }
                            ?: "MC${code.mcId}"
                        val label = rawLabel.take(3)   // max 3 chars
                        val btn = Button(this@SpgDoffEntry1Activity)
                        btn.text = label
                        btn.textSize = 11f
                        btn.setTypeface(null, Typeface.BOLD)
                        btn.setTextColor(Color.WHITE)
                        btn.setBackgroundColor(Color.parseColor("#1565C0"))  // blue default
                        val dp = resources.displayMetrics.density
                        val lp = LinearLayout.LayoutParams(
                            (44 * dp).toInt(),
                            (34 * dp).toInt()
                        )
                        lp.setMargins(0, 0, (6 * dp).toInt(), 0)
                        btn.layoutParams = lp
                        btn.setPadding(0, 0, 0, 0)
                        btn.tag = code
                        btn.setOnClickListener { onMechCodeSelected(btn, code) }
                        llMechCodes.addView(btn)
                    }
                }
            }
            override fun onFailure(call: Call<Spg1MechCodesResponse>, t: Throwable) {
                tvMechCodesStatus.text = "Failed to load mech codes"
                tvMechCodesStatus.visibility = View.VISIBLE
            }
        })
    }

    private fun onMechCodeSelected(btn: Button, code: Spg1MechCode) {
        selectedMcId     = code.mcId
        selectedMechCode = code.mechPostingCode

        // Highlight selected=green, reset others=blue
        for (i in 0 until llMechCodes.childCount) {
            val child = llMechCodes.getChildAt(i) as? Button ?: continue
            if (child == btn) {
                child.setTextColor(Color.WHITE)
                child.setBackgroundColor(Color.parseColor("#2E7D32"))  // green selected
            } else {
                child.setTextColor(Color.WHITE)
                child.setBackgroundColor(Color.parseColor("#1565C0"))  // blue default
            }
        }
    }

    // ── Trolly lookup ─────────────────────────────────────────────────────────
    private fun resetTrolly() {
        validatedTrollyId = null
        validatedTareWt   = 0.0
        etTareWt.setText("")
        etNetWt.setText("")
        tvTrollyInfo.text = ""
        etTrollyNo.setBackgroundResource(R.drawable.bg_input_rounded_light)
    }

    private fun lookupTrolly(trollyNo: String) {
        RetrofitClient.getApiService(this).validateDoffTrolly(
            trollyNo = trollyNo,
            branchId = branchId.takeIf { it > 0 }
        ).enqueue(object : Callback<DoffTrollyValidateResponse> {
            override fun onResponse(call: Call<DoffTrollyValidateResponse>, response: Response<DoffTrollyValidateResponse>) {
                val body = response.body()
                if (response.isSuccessful && body?.status == "success" && body.trollyId != null) {
                    validatedTrollyId = body.trollyId
                    val tare = (body.trollyWeight ?: 0.0) + (body.bucketWeight ?: 0.0)
                    validatedTareWt = tare
                    etTrollyNo.setBackgroundResource(R.drawable.bg_input_valid)
                    tvTrollyInfo.text = (body.trollyName ?: "Valid") +
                        if (tare > 0) "  (Tare: ${fmt(tare)})" else ""
                    tvTrollyInfo.setTextColor(Color.parseColor("#2E7D32"))
                    etTareWt.setText(fmt(tare))
                    recalcNet()
                } else {
                    etTrollyNo.setBackgroundResource(R.drawable.bg_input_invalid)
                    tvTrollyInfo.text = body?.message ?: "Trolly not found"
                    tvTrollyInfo.setTextColor(Color.parseColor("#C62828"))
                }
            }
            override fun onFailure(call: Call<DoffTrollyValidateResponse>, t: Throwable) {
                etTrollyNo.setBackgroundResource(R.drawable.bg_input_invalid)
                tvTrollyInfo.text = "Lookup failed"
                tvTrollyInfo.setTextColor(Color.parseColor("#C62828"))
            }
        })
    }

    // ── Net weight calculation ────────────────────────────────────────────────
    private fun recalcNet() {
        val gross = etGrossWt.text.toString().toDoubleOrNull() ?: 0.0
        val tare  = etTareWt.text.toString().toDoubleOrNull()  ?: 0.0
        val net   = gross - tare
        etNetWt.setText(fmt(net))
        etNetWt.setTextColor(if (net < 0.0) Color.parseColor("#C62828") else Color.parseColor("#1E1E1E"))

        // Gross wt background feedback
        when {
            etGrossWt.text.toString().isBlank() ->
                etGrossWt.setBackgroundResource(R.drawable.bg_input_rounded_light)
            gross > 0.0 ->
                etGrossWt.setBackgroundResource(R.drawable.bg_input_valid)
            else ->
                etGrossWt.setBackgroundResource(R.drawable.bg_input_invalid)
        }
    }

    private fun fmt(v: Double) = String.format(Locale.getDefault(), "%.3f", v)

    // ── Save ─────────────────────────────────────────────────────────────────
    private fun saveEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) { Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show(); return }
        val mcId = selectedMcId
        if (mcId == null) { Toast.makeText(this, "Please select a Mech Posting Code", Toast.LENGTH_SHORT).show(); return }
        val trollyId = validatedTrollyId
        if (trollyId == null) { Toast.makeText(this, "Please enter a valid Trolly No", Toast.LENGTH_SHORT).show(); return }
        val gross = etGrossWt.text.toString().toDoubleOrNull()
        if (gross == null || gross <= 0.0) { Toast.makeText(this, "Please enter Gross Weight", Toast.LENGTH_SHORT).show(); return }
        val tare = etTareWt.text.toString().toDoubleOrNull() ?: 0.0
        val net  = gross - tare
        if (net < 0.0) { Toast.makeText(this, "Net Weight is negative. Cannot save.", Toast.LENGTH_LONG).show(); return }

        val req = DoffSaveRequest(
            id = null, doffDate = entryDate, spellId = spellId, mcId = mcId,
            qualityId = null, trollyId = trollyId, ebId = 0,
            grossWeight = gross, tareWeight = tare, netWeight = net,
            weightType = "SPG1", branchId = branchId.takeIf { it > 0 }, userId = userId
        )
        btnSave.isEnabled = false
        RetrofitClient.getApiService(this).saveDoffTransaction(req)
            .enqueue(object : Callback<DoffSaveResponse> {
                override fun onResponse(call: Call<DoffSaveResponse>, response: Response<DoffSaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed"
                    Toast.makeText(this@SpgDoffEntry1Activity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm()
                        loadSummary()
                    }
                }
                override fun onFailure(call: Call<DoffSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@SpgDoffEntry1Activity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun clearForm() {
        etTrollyNo.setText(""); etGrossWt.setText(""); etTareWt.setText(""); etNetWt.setText("")
        etGrossWt.setBackgroundResource(R.drawable.bg_input_rounded_light)
        resetTrolly()
        // Deselect mech code buttons
        for (i in 0 until llMechCodes.childCount) {
            val child = llMechCodes.getChildAt(i) as? Button ?: continue
            child.setTextColor(Color.WHITE)
            child.setBackgroundColor(Color.parseColor("#1565C0"))
        }
        selectedMcId     = null
        selectedMechCode = null
    }

    // ── Summary list ──────────────────────────────────────────────────────────
    private fun loadSummary() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) return

        pbSummary.visibility    = View.VISIBLE
        tvSummaryEmpty.visibility = View.GONE
        rvSummary.visibility    = View.GONE

        RetrofitClient.getApiService(this).getSpg1Summary(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId
        ).enqueue(object : Callback<Spg1SummaryResponse> {
            override fun onResponse(call: Call<Spg1SummaryResponse>, response: Response<Spg1SummaryResponse>) {
                pbSummary.visibility = View.GONE
                val list = response.body()?.summary.orEmpty()
                if (list.isEmpty()) {
                    tvSummaryEmpty.visibility = View.VISIBLE
                } else {
                    rvSummary.visibility = View.VISIBLE
                    summaryAdapter.update(list)
                }
            }
            override fun onFailure(call: Call<Spg1SummaryResponse>, t: Throwable) {
                pbSummary.visibility    = View.GONE
                tvSummaryEmpty.visibility = View.VISIBLE
                tvSummaryEmpty.text     = "Failed to load summary"
            }
        })
    }

    // ── Detail dialog on row click ────────────────────────────────────────────
    private fun showDetailDialog(row: Spg1SummaryRow) {
        val spellId = selectedSpellId() ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_spg1_detail, null)
        val tvTitle    = dialogView.findViewById<TextView>(R.id.tvDetailTitle)
        val rvDetail   = dialogView.findViewById<RecyclerView>(R.id.rvDetailList)

        val mcLabel = row.mechPostingCode?.toString()
            ?: row.mcCode?.takeIf { it.isNotBlank() }
            ?: "${row.mcId}"
        tvTitle.text = "Machine $mcLabel — Details"

        val detailAdapter = DetailRowAdapter()
        rvDetail.layoutManager = LinearLayoutManager(this)
        rvDetail.adapter = detailAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()
        dialog.show()

        // Fetch transactions for this mc_id / date / spell
        RetrofitClient.getApiService(this).getDoffTransactions(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId.takeIf { it > 0 },
            mcId     = row.mcId
        ).enqueue(object : Callback<DoffListResponse> {
            override fun onResponse(call: Call<DoffListResponse>, response: Response<DoffListResponse>) {
                val list = response.body()?.transactions
                    ?.filter { it.weightType == "SPG1" }
                    .orEmpty()
                detailAdapter.update(list)
            }
            override fun onFailure(call: Call<DoffListResponse>, t: Throwable) {}
        })
    }

    // ── Inner adapter for detail dialog ───────────────────────────────────────
    inner class DetailRowAdapter : RecyclerView.Adapter<DetailRowAdapter.DVH>() {
        private val items = mutableListOf<DoffTransaction>()
        fun update(list: List<DoffTransaction>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            DVH(LayoutInflater.from(parent.context).inflate(R.layout.item_spg1_detail, parent, false))
        override fun onBindViewHolder(h: DVH, pos: Int) = h.bind(pos + 1, items[pos])

        inner class DVH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvSeq     = v.findViewById<TextView>(R.id.tvSeq)
            private val tvTrolly  = v.findViewById<TextView>(R.id.tvTrollyNo)
            private val tvGross   = v.findViewById<TextView>(R.id.tvGrossWt)
            private val tvTare    = v.findViewById<TextView>(R.id.tvTareWt)
            private val tvNet     = v.findViewById<TextView>(R.id.tvNetWt)
            fun bind(seq: Int, t: DoffTransaction) {
                tvSeq.text    = seq.toString()
                tvTrolly.text = t.trollyName ?: "-"
                tvGross.text  = t.grossWeight?.let { fmt(it) } ?: "-"
                tvTare.text   = t.tareWeight?.let { fmt(it) } ?: "-"
                tvNet.text    = t.netWeight?.let { fmt(it) } ?: "-"
            }
        }
    }

    // ── Quality-wise Shift-wise Report ────────────────────────────────────────
    private fun showQualityShiftReport() {
        if (branchId <= 0) {
            Toast.makeText(this, "Invalid branch", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quality_shift_report, null)
        val tvReportTitle = dialogView.findViewById<TextView>(R.id.tvReportTitle)
        val rvReport = dialogView.findViewById<RecyclerView>(R.id.rvQualityShiftReport)
        val tvGrandTotalA = dialogView.findViewById<TextView>(R.id.tvGrandTotalA)
        val tvGrandTotalB = dialogView.findViewById<TextView>(R.id.tvGrandTotalB)
        val tvGrandTotalC = dialogView.findViewById<TextView>(R.id.tvGrandTotalC)
        val tvGrandTotal = dialogView.findViewById<TextView>(R.id.tvGrandTotal)
        val pbReport = dialogView.findViewById<ProgressBar>(R.id.pbReport)
        val tvReportEmpty = dialogView.findViewById<TextView>(R.id.tvReportEmpty)

        tvReportTitle.text = "SPG Doff Entry (1) - Quality Shift Report"

        val adapter = QualityShiftReportAdapter()
        rvReport.layoutManager = LinearLayoutManager(this)
        rvReport.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .setPositiveButton("Print PDF") { _, _ ->
                // Export to PDF will be implemented here
                exportReportToPdf(adapter.getReportData(), tvGrandTotalA.text.toString(),
                    tvGrandTotalB.text.toString(), tvGrandTotalC.text.toString(), tvGrandTotal.text.toString())
            }
            .create()

        dialog.show()

        // Fetch report data
        pbReport.visibility = View.VISIBLE
        rvReport.visibility = View.GONE
        tvReportEmpty.visibility = View.GONE

        RetrofitClient.getApiService(this).getSpg1QualityShiftReport(
            date = entryDate,
            branchId = branchId
        ).enqueue(object : Callback<QualityShiftReportResponse> {
            override fun onResponse(call: Call<QualityShiftReportResponse>, response: Response<QualityShiftReportResponse>) {
                pbReport.visibility = View.GONE
                if (response.isSuccessful && response.body()?.status == "success") {
                    val reportData = response.body()?.report.orEmpty()
                    val grandTotal = response.body()?.grandTotal
                    
                    if (reportData.isEmpty()) {
                        tvReportEmpty.visibility = View.VISIBLE
                        tvReportEmpty.text = "No data for selected date"
                    } else {
                        rvReport.visibility = View.VISIBLE
                        adapter.update(reportData)
                        
                        // Update grand totals
                        tvGrandTotalA.text = fmt(grandTotal?.shiftA ?: 0.0)
                        tvGrandTotalB.text = fmt(grandTotal?.shiftB ?: 0.0)
                        tvGrandTotalC.text = fmt(grandTotal?.shiftC ?: 0.0)
                        tvGrandTotal.text = fmt(grandTotal?.total ?: 0.0)
                    }
                } else {
                    tvReportEmpty.visibility = View.VISIBLE
                    tvReportEmpty.text = response.body()?.message ?: "Failed to load report"
                }
            }

            override fun onFailure(call: Call<QualityShiftReportResponse>, t: Throwable) {
                pbReport.visibility = View.GONE
                tvReportEmpty.visibility = View.VISIBLE
                tvReportEmpty.text = "Error: ${t.localizedMessage}"
            }
        })
    }

    // ── Export Report to PDF ──────────────────────────────────────────────────
    private fun exportReportToPdf(reportData: List<QualityShiftReportRow>, 
                                  totalA: String, totalB: String, totalC: String, grandTotal: String) {
        try {
            // Create a simple text file for now (can be enhanced to actual PDF later)
            val fileName = "SPG1_Report_${entryDate.replace("-", "_")}.txt"
            val fileContent = buildString {
                appendLine("SPG DOFF ENTRY (1) - QUALITY SHIFT REPORT")
                appendLine("Date: $entryDate")
                appendLine("=" .repeat(60))
                appendLine()
                appendLine(String.format("%-20s %10s %10s %10s %10s", 
                    "Quality", "Shift A", "Shift B", "Shift C", "Total"))
                appendLine("-".repeat(60))
                
                reportData.forEach { row ->
                    appendLine(String.format("%-20s %10s %10s %10s %10s",
                        row.qualityName ?: "Unknown",
                        fmt(row.shiftA ?: 0.0),
                        fmt(row.shiftB ?: 0.0),
                        fmt(row.shiftC ?: 0.0),
                        fmt(row.total ?: 0.0)
                    ))
                }
                
                appendLine("-".repeat(60))
                appendLine(String.format("%-20s %10s %10s %10s %10s",
                    "GRAND TOTAL", totalA, totalB, totalC, grandTotal))
                appendLine("=" .repeat(60))
            }

            // Save to Downloads folder
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val file = java.io.File(downloadsDir, fileName)
            file.writeText(fileContent)

            Toast.makeText(this, "Report saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Inner adapter for quality-shift report ────────────────────────────────
    inner class QualityShiftReportAdapter : RecyclerView.Adapter<QualityShiftReportAdapter.QSRVH>() {
        private val items = mutableListOf<QualityShiftReportRow>()
        
        fun update(list: List<QualityShiftReportRow>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }
        
        fun getReportData(): List<QualityShiftReportRow> = items.toList()
        
        override fun getItemCount() = items.size
        
        override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
            QSRVH(LayoutInflater.from(parent.context).inflate(R.layout.item_quality_shift_report, parent, false))
        
        override fun onBindViewHolder(h: QSRVH, pos: Int) = h.bind(items[pos])

        inner class QSRVH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvQuality = v.findViewById<TextView>(R.id.tvQuality)
            private val tvShiftA  = v.findViewById<TextView>(R.id.tvShiftA)
            private val tvShiftB  = v.findViewById<TextView>(R.id.tvShiftB)
            private val tvShiftC  = v.findViewById<TextView>(R.id.tvShiftC)
            private val tvTotal   = v.findViewById<TextView>(R.id.tvTotal)
            
            fun bind(row: QualityShiftReportRow) {
                tvQuality.text = row.qualityName ?: "Unknown"
                tvShiftA.text  = fmt(row.shiftA ?: 0.0)
                tvShiftB.text  = fmt(row.shiftB ?: 0.0)
                tvShiftC.text  = fmt(row.shiftC ?: 0.0)
                tvTotal.text   = fmt(row.total ?: 0.0)
            }
        }
    }
}
