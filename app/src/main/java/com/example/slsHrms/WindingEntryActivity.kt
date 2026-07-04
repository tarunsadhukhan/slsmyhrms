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
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.QualityShiftReportAdapter
import com.example.slsHrms.adapter.We2DetailAdapter
import com.example.slsHrms.adapter.We2SummaryAdapter
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WindingEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate  : TextView
    private lateinit var spEntrySpell : Spinner
    private lateinit var llEmpButtons : LinearLayout
    private lateinit var tvEmpStatus  : TextView
    private lateinit var btnTypeS     : Button
    private lateinit var btnTypeC     : Button
    private lateinit var etTrollyNo   : EditText
    private lateinit var tvTrollyInfo : TextView
    private lateinit var etGrossWt    : EditText
    private lateinit var etTareWt     : EditText
    private lateinit var etNetWt      : EditText
    private lateinit var btnSave      : Button
    private lateinit var rvSummary    : RecyclerView
    private lateinit var tvSummaryEmpty : TextView
    private lateinit var pbSummary    : ProgressBar
    private lateinit var ivQualityShiftReport : ImageView

    private val spellList = mutableListOf<Spell>()
    private lateinit var summaryAdapter: We2SummaryAdapter

    // Selected employee
    private var resolvedEbId  : Long   = 0L
    private var resolvedEmpName: String = ""

    // S or C toggle
    private var selectedType: String = "S"

    // Validated trolly
    private var validatedTrollyId: Int?    = null
    private var validatedTareWt  : Double  = 0.0

    private val mainHandler  = Handler(Looper.getMainLooper())
    private var trollyDebounce: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_winding_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate    = findViewById(R.id.tvEntryDate)
        spEntrySpell   = findViewById(R.id.spEntrySpell)
        llEmpButtons   = findViewById(R.id.llEmpButtons)
        tvEmpStatus    = findViewById(R.id.tvEmpStatus)
        btnTypeS       = findViewById(R.id.btnTypeS)
        btnTypeC       = findViewById(R.id.btnTypeC)
        etTrollyNo     = findViewById(R.id.etTrollyNo)
        tvTrollyInfo   = findViewById(R.id.tvTrollyInfo)
        etGrossWt      = findViewById(R.id.etGrossWt)
        etTareWt       = findViewById(R.id.etTareWt)
        etNetWt        = findViewById(R.id.etNetWt)
        btnSave        = findViewById(R.id.btnSave)
        rvSummary      = findViewById(R.id.rvSummary)
        tvSummaryEmpty = findViewById(R.id.tvSummaryEmpty)
        pbSummary      = findViewById(R.id.pbSummary)
        ivQualityShiftReport = findViewById(R.id.ivQualityShiftReport)

        summaryAdapter = We2SummaryAdapter { row -> showDetailDialog(row) }
        rvSummary.layoutManager = LinearLayoutManager(this)
        rvSummary.adapter = summaryAdapter
        rvSummary.isNestedScrollingEnabled = false

        // Init date
        val cal = Calendar.getInstance()
        entryDate        = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)

        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickDate() }

        // S/C toggle (default S)
        btnTypeS.setOnClickListener { selectType("S") }
        btnTypeC.setOnClickListener { selectType("C") }
        selectType("S")

        // Trolly No debounced lookup (only when C)
        etTrollyNo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                resetTrolly()
                trollyDebounce?.let { mainHandler.removeCallbacks(it) }
                val v = s?.toString()?.trim().orEmpty()
                if (v.isEmpty() || selectedType != "S") return
                val r = Runnable { lookupTrolly(v) }
                trollyDebounce = r
                mainHandler.postDelayed(r, 500)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        etTrollyNo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && selectedType == "S") {
                val v = etTrollyNo.text.toString().trim()
                if (v.isNotEmpty() && validatedTrollyId == null) lookupTrolly(v)
            }
        }

        // Gross weight → recalc net
        etGrossWt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcNet() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        // Spell change → reload employees + summary
        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadEmpButtons()
                loadSummary()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveEntry() }

        ivQualityShiftReport.setOnClickListener { showQualityShiftReport() }

        loadSpells {
            loadEmpButtons()
            loadSummary()
        }
    }

    // ── Date picker ──────────────────────────────────────────────────────────
    private fun pickDate() {
        val cal = Calendar.getInstance()
        try { apiDate.parse(entryDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            entryDate        = apiDate.format(cal.time)
            tvEntryDate.text = dispDate.format(cal.time)
            loadEmpButtons()
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
                        this@WindingEntryActivity,
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

    // ── Employee buttons (mirrors loadMechCodes in SpgDoffEntry1Activity) ───
    private fun loadEmpButtons() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) return

        llEmpButtons.removeAllViews()
        resolvedEbId   = 0L
        resolvedEmpName = ""
        tvEmpStatus.visibility = View.VISIBLE
        tvEmpStatus.text       = "Loading…"

        RetrofitClient.getApiService(this).getWe2Employees(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId
        ).enqueue(object : Callback<We2EmployeesResponse> {
            override fun onResponse(call: Call<We2EmployeesResponse>, response: Response<We2EmployeesResponse>) {
                val emps = response.body()?.employees.orEmpty()
                if (emps.isEmpty()) {
                    tvEmpStatus.text       = "No employees found for selected date/spell"
                    tvEmpStatus.visibility = View.VISIBLE
                } else {
                    tvEmpStatus.visibility = View.GONE
                    llEmpButtons.removeAllViews()
                    val dp = resources.displayMetrics.density
                    for (emp in emps) {
                        val label = (emp.empName ?: emp.empCode ?: "?").take(7)
                        val btn   = Button(this@WindingEntryActivity)
                        btn.text     = label
                        btn.textSize = 11f
                        btn.setTypeface(null, Typeface.BOLD)
                        btn.setTextColor(Color.WHITE)
                        btn.setBackgroundColor(Color.parseColor("#1565C0"))
                        val lp = LinearLayout.LayoutParams(
                            (52 * dp).toInt(),
                            (36 * dp).toInt()
                        )
                        lp.setMargins(0, 0, (6 * dp).toInt(), 0)
                        btn.layoutParams = lp
                        btn.setPadding(0, 0, 0, 0)
                        btn.tag = emp
                        btn.setOnClickListener { onEmpSelected(btn, emp) }
                        llEmpButtons.addView(btn)
                    }
                }
            }
            override fun onFailure(call: Call<We2EmployeesResponse>, t: Throwable) {
                tvEmpStatus.text       = "Failed to load employees"
                tvEmpStatus.visibility = View.VISIBLE
            }
        })
    }

    private fun onEmpSelected(btn: Button, emp: We2Employee) {
        resolvedEbId   = emp.ebId ?: 0L
        resolvedEmpName = emp.empName ?: ""
        // Highlight selected green, reset others blue
        for (i in 0 until llEmpButtons.childCount) {
            val child = llEmpButtons.getChildAt(i) as? Button ?: continue
            child.setBackgroundColor(
                if (child == btn) Color.parseColor("#2E7D32")
                else              Color.parseColor("#1565C0")
            )
        }
    }

    // ── S/C toggle ───────────────────────────────────────────────────────────
    private fun selectType(type: String) {
        selectedType = type
        if (type == "S") {
            // (S) → Trolly required, enable field
            btnTypeS.setBackgroundColor(Color.parseColor("#2E7D32"))
            btnTypeC.setBackgroundColor(Color.parseColor("#1565C0"))
            etTrollyNo.isEnabled = true
            etTrollyNo.requestFocus()
        } else {
            // (C) → No trolly, disable and clear field, tare = 0
            btnTypeS.setBackgroundColor(Color.parseColor("#1565C0"))
            btnTypeC.setBackgroundColor(Color.parseColor("#2E7D32"))
            etTrollyNo.setText("")
            etTrollyNo.isEnabled = false
            tvTrollyInfo.text    = ""
            etTrollyNo.setBackgroundResource(R.drawable.bg_input_rounded_light)
            validatedTrollyId = null
            validatedTareWt   = 0.0
            etTareWt.setText("0.000")
            recalcNet()
        }
    }

    // ── Trolly lookup ────────────────────────────────────────────────────────
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
                    val info = (body.trollyName ?: "Valid") +
                        if (tare > 0) "  (Tare: ${fmt(tare)})" else ""
                    tvTrollyInfo.text = info
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

    // ── Net weight calculation ───────────────────────────────────────────────
    private fun recalcNet() {
        val gross = etGrossWt.text.toString().toDoubleOrNull() ?: 0.0
        val tare  = etTareWt.text.toString().toDoubleOrNull()  ?: 0.0
        val net   = gross - tare
        etNetWt.setText(fmt(net))
        etNetWt.setTextColor(
            if (net <= 0.0) Color.parseColor("#C62828") else Color.parseColor("#1E1E1E")
        )
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
        if (spellId == null) {
            Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show(); return
        }
        if (resolvedEbId <= 0L) {
            Toast.makeText(this, "Please select an Employee", Toast.LENGTH_SHORT).show(); return
        }
        if (selectedType == "S" && validatedTrollyId == null) {
            Toast.makeText(this, "Please enter a valid Trolly No (required when S)", Toast.LENGTH_SHORT).show(); return
        }
        val gross = etGrossWt.text.toString().toDoubleOrNull()
        if (gross == null || gross <= 0.0) {
            Toast.makeText(this, "Please enter Gross Weight", Toast.LENGTH_SHORT).show(); return
        }
        val tare = etTareWt.text.toString().toDoubleOrNull() ?: 0.0
        val net  = gross - tare
        if (net <= 0.0) {
            Toast.makeText(this, "Net Weight must be positive", Toast.LENGTH_LONG).show(); return
        }

        val req = We2SaveRequest(
            date        = entryDate,
            spellId     = spellId,
            branchId    = branchId,
            ebId        = resolvedEbId,
            scType      = selectedType,
            trollyId    = if (selectedType == "S") validatedTrollyId else null,
            grossWeight = gross,
            tareWeight  = tare,
            netWeight   = net,
            userId      = userId
        )
        btnSave.isEnabled = false
        RetrofitClient.getApiService(this).saveWe2(req)
            .enqueue(object : Callback<We2SaveResponse> {
                override fun onResponse(call: Call<We2SaveResponse>, response: Response<We2SaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed"
                    Toast.makeText(this@WindingEntryActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm()
                        loadSummary()
                    }
                }
                override fun onFailure(call: Call<We2SaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@WindingEntryActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun clearForm() {
        // Deselect all employee buttons
        for (i in 0 until llEmpButtons.childCount) {
            (llEmpButtons.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#1565C0"))
        }
        resolvedEbId    = 0L
        resolvedEmpName = ""
        etGrossWt.setText("")
        etTareWt.setText("")
        etNetWt.setText("")
        etGrossWt.setBackgroundResource(R.drawable.bg_input_rounded_light)
        resetTrolly()
        selectType("S")
    }

    // ── Summary list (grouped by employee) ──────────────────────────────────
    private fun loadSummary() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) return

        pbSummary.visibility      = View.VISIBLE
        tvSummaryEmpty.visibility = View.GONE
        rvSummary.visibility      = View.GONE

        RetrofitClient.getApiService(this).getWe2GroupedSummary(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId
        ).enqueue(object : Callback<We2GroupedResponse> {
            override fun onResponse(call: Call<We2GroupedResponse>, response: Response<We2GroupedResponse>) {
                pbSummary.visibility = View.GONE
                val list = response.body()?.summary.orEmpty()
                if (list.isEmpty()) {
                    tvSummaryEmpty.visibility = View.VISIBLE
                } else {
                    rvSummary.visibility = View.VISIBLE
                    summaryAdapter.update(list)
                }
            }
            override fun onFailure(call: Call<We2GroupedResponse>, t: Throwable) {
                pbSummary.visibility      = View.GONE
                tvSummaryEmpty.visibility = View.VISIBLE
                tvSummaryEmpty.text       = "Failed to load summary"
            }
        })
    }

    // ── Detail dialog (on row click) ─────────────────────────────────────────
    private fun showDetailDialog(row: We2GroupedRow) {
        val spellId = selectedSpellId() ?: return
        val ebId    = row.ebId ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_we2_detail, null)
        val tvTitle    = dialogView.findViewById<TextView>(R.id.tvDetailTitle)
        val rv         = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDetailList)

        tvTitle.text = "${row.empName ?: row.empCode}  –  ${row.noOfDoff} doffs  |  Total: ${
            String.format(Locale.getDefault(), "%.3f", row.totalWt)
        } kg"

        val detailAdapter = We2DetailAdapter()
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rv.adapter       = detailAdapter
        rv.isNestedScrollingEnabled = false

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()
        dialog.show()

        RetrofitClient.getApiService(this).getWe2Details(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId,
            ebId     = ebId
        ).enqueue(object : Callback<We2SummaryResponse> {
            override fun onResponse(call: Call<We2SummaryResponse>, response: Response<We2SummaryResponse>) {
                detailAdapter.update(response.body()?.summary.orEmpty())
            }
            override fun onFailure(call: Call<We2SummaryResponse>, t: Throwable) {}
        })
    }

    private fun confirmDelete(row: We2SummaryRow) {
        val id = row.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("Remove ${row.empName ?: row.empCode} (Net: ${row.netWeight} kg)?")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteWe2(id)
                    .enqueue(object : Callback<We2SaveResponse> {
                        override fun onResponse(call: Call<We2SaveResponse>, response: Response<We2SaveResponse>) {
                            Toast.makeText(this@WindingEntryActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            loadSummary()
                        }
                        override fun onFailure(call: Call<We2SaveResponse>, t: Throwable) {
                            Toast.makeText(this@WindingEntryActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Quality-wise Shift-wise Report ──────────────────────────────────────
    private fun showQualityShiftReport() {
        if (branchId <= 0) {
            Toast.makeText(this, "Branch ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_quality_shift_report, null)
        val tvReportTitle = dialogView.findViewById<TextView>(R.id.tvReportTitle)
        val rvReport = dialogView.findViewById<RecyclerView>(R.id.rvQualityShiftReport)
        val tvGrandTotalA = dialogView.findViewById<TextView>(R.id.tvGrandTotalA)
        val tvGrandTotalB = dialogView.findViewById<TextView>(R.id.tvGrandTotalB)
        val tvGrandTotalC = dialogView.findViewById<TextView>(R.id.tvGrandTotalC)
        val tvGrandTotal = dialogView.findViewById<TextView>(R.id.tvGrandTotal)
        val pbReport = dialogView.findViewById<ProgressBar>(R.id.pbReport)
        val tvReportEmpty = dialogView.findViewById<TextView>(R.id.tvReportEmpty)

        tvReportTitle.text = "Quality-wise Shift-wise Production Report\n${tvEntryDate.text}"

        val reportAdapter = QualityShiftReportAdapter()
        rvReport.layoutManager = LinearLayoutManager(this)
        rvReport.adapter = reportAdapter
        rvReport.isNestedScrollingEnabled = false

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .create()
        dialog.show()

        // Fetch report data
        pbReport.visibility = View.VISIBLE
        tvReportEmpty.visibility = View.GONE
        rvReport.visibility = View.GONE

        RetrofitClient.getApiService(this).getWe2QualityShiftReport(
            date = entryDate,
            branchId = branchId
        ).enqueue(object : Callback<QualityShiftReportResponse> {
            override fun onResponse(call: Call<QualityShiftReportResponse>, response: Response<QualityShiftReportResponse>) {
                pbReport.visibility = View.GONE
                val body = response.body()
                val report = body?.report.orEmpty()
                
                if (report.isEmpty()) {
                    tvReportEmpty.visibility = View.VISIBLE
                } else {
                    rvReport.visibility = View.VISIBLE
                    reportAdapter.update(report)
                    
                    // Update grand total
                    val grandTotal = body?.grandTotal
                    tvGrandTotalA.text = grandTotal?.shiftA?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "0.00"
                    tvGrandTotalB.text = grandTotal?.shiftB?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "0.00"
                    tvGrandTotalC.text = grandTotal?.shiftC?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "0.00"
                    tvGrandTotal.text = grandTotal?.total?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "0.00"
                }
            }
            override fun onFailure(call: Call<QualityShiftReportResponse>, t: Throwable) {
                pbReport.visibility = View.GONE
                tvReportEmpty.visibility = View.VISIBLE
                tvReportEmpty.text = "Failed to load report: ${t.localizedMessage}"
            }
        })
    }
}

