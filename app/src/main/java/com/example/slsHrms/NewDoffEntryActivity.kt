package com.example.slsHrms

import android.app.DatePickerDialog
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
import com.example.slsHrms.adapter.DoffSummaryAdapter
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NewDoffEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId = 0
    private var userId   = 0
    private var entryDate = ""

    private lateinit var tvEntryDate: TextView
    private lateinit var spEntrySpell: Spinner
    private lateinit var etMcNo: EditText
    private lateinit var etTrollyNo: EditText
    private lateinit var tvMcInfo: TextView
    private lateinit var tvTrollyInfo: TextView
    private lateinit var etGross: EditText
    private lateinit var etTare: EditText
    private lateinit var etNet: EditText
    private lateinit var etTotalDoffWt: EditText
    private lateinit var etNoOfDoff: EditText
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button

    private lateinit var rvSummary: RecyclerView
    private lateinit var summaryAdapter: DoffSummaryAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private val spellList = mutableListOf<Spell>()

    // Validated values
    private var validatedMcId: Int? = null
    private var validatedMcNo: String? = null
    private var validatedTrollyId: Int? = null
    private var validatedTrollyWeight: Double = 0.0
    private var validatedBucketWeight: Double = 0.0

    // Running totals for selected machine/date/spell
    private var runningTotalWt: Double = 0.0
    private var runningCount: Int = 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private var mcValidateRunnable: Runnable? = null
    private var trollyValidateRunnable: Runnable? = null
    private var suppressTrollyWatcher: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_doff_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate    = findViewById(R.id.tvEntryDate)
        spEntrySpell   = findViewById(R.id.spEntrySpell)
        etMcNo         = findViewById(R.id.etMcNo)
        etTrollyNo     = findViewById(R.id.etTrollyNo)
        tvMcInfo       = findViewById(R.id.tvMcInfo)
        tvTrollyInfo   = findViewById(R.id.tvTrollyInfo)
        etGross        = findViewById(R.id.etGross)
        etTare         = findViewById(R.id.etTare)
        etNet          = findViewById(R.id.etNet)
        etTotalDoffWt  = findViewById(R.id.etTotalDoffWt)
        etNoOfDoff     = findViewById(R.id.etNoOfDoff)
        btnSave        = findViewById(R.id.btnSave)
        btnClear       = findViewById(R.id.btnClear)
        rvSummary      = findViewById(R.id.rvDoffSummary)
        progressBar    = findViewById(R.id.progressBar)
        tvEmpty        = findViewById(R.id.tvEmpty)

        val cal = Calendar.getInstance()
        entryDate = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)

        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickEntryDate() }

        summaryAdapter = DoffSummaryAdapter()
        rvSummary.layoutManager = LinearLayoutManager(this)
        rvSummary.adapter = summaryAdapter
        rvSummary.isNestedScrollingEnabled = false

        // Auto-recalculate net weight + total when gross/tare changes
        val weightWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { recalcNet() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        }
        etGross.addTextChangedListener(weightWatcher)
        etTare.addTextChangedListener(weightWatcher)

        // MC No: validate (debounced) on text change + on focus loss
        etMcNo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                resetMcValidation()
                mcValidateRunnable?.let { mainHandler.removeCallbacks(it) }
                val v = s?.toString()?.trim().orEmpty()
                if (v.isEmpty()) return
                val r = Runnable { validateMcNo(v) }
                mcValidateRunnable = r
                mainHandler.postDelayed(r, 450)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        etMcNo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = etMcNo.text.toString().trim()
                if (v.isNotEmpty() && validatedMcId == null) validateMcNo(v)
            }
        }

        // Trolly No: validate on focus loss + debounced text change
        etTrollyNo.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (suppressTrollyWatcher) return
                resetTrollyValidation()
                trollyValidateRunnable?.let { mainHandler.removeCallbacks(it) }
                val v = s?.toString()?.trim().orEmpty()
                if (v.isEmpty()) return
                val r = Runnable { validateTrollyNo(v) }
                trollyValidateRunnable = r
                mainHandler.postDelayed(r, 450)
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        etTrollyNo.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val v = etTrollyNo.text.toString().trim()
                if (v.isNotEmpty() && validatedTrollyId == null) validateTrollyNo(v)
            }
        }

        // Spell change → reload summary + (if mc validated) re-fetch running totals
        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                reloadSummary()
                if (validatedMcId != null) fetchRunningTotalsForMachine()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveEntry() }
        btnClear.setOnClickListener { clearAfterSave(keepDateSpell = true) }

        loadSpells { reloadSummary() }
    }

    // ── Date picker ────────────────────────────────────────────────
    private fun pickEntryDate() {
        val cal = Calendar.getInstance()
        try { apiDate.parse(entryDate)?.let { cal.time = it } } catch (_: Exception) {}
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            entryDate = apiDate.format(cal.time)
            tvEntryDate.text = dispDate.format(cal.time)
            reloadSummary()
            if (validatedMcId != null) fetchRunningTotalsForMachine()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ── Spell loading ──────────────────────────────────────────────
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
                        this@NewDoffEntryActivity,
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

    // ── Validation ─────────────────────────────────────────────────
    private fun resetMcValidation() {
        validatedMcId = null
        validatedMcNo = null
        etMcNo.setBackgroundResource(R.drawable.bg_input_rounded_light)
        tvMcInfo.text = ""
        etTotalDoffWt.setText("")
        etNoOfDoff.setText("")
        runningTotalWt = 0.0
        runningCount = 0
    }

    private fun resetTrollyValidation() {
        validatedTrollyId = null
        validatedTrollyWeight = 0.0
        validatedBucketWeight = 0.0
        etTrollyNo.setBackgroundResource(R.drawable.bg_input_rounded_light)
        tvTrollyInfo.text = ""
    }

    private fun validateMcNo(mcNo: String) {
        RetrofitClient.getApiService(this).validateDoffMachine(
            mcNo = mcNo,
            branchId = if (branchId > 0) branchId else null
        ).enqueue(object : Callback<DoffMachineValidateResponse> {
            override fun onResponse(call: Call<DoffMachineValidateResponse>, response: Response<DoffMachineValidateResponse>) {
                val body = response.body()
                if (response.isSuccessful && body?.status == "success" && body.mcId != null) {
                    validatedMcId = body.mcId
                    validatedMcNo = body.mcNo ?: mcNo
                    etMcNo.setBackgroundResource(R.drawable.bg_input_valid)
                    tvMcInfo.text = listOfNotNull(body.mcCode, body.mcName)
                        .joinToString(" - ").ifBlank { "Valid" }
                    tvMcInfo.setTextColor(0xFF2E7D32.toInt())
                    fetchRunningTotalsForMachine()
                } else {
                    etMcNo.setBackgroundResource(R.drawable.bg_input_invalid)
                    tvMcInfo.text = body?.message ?: "Invalid MC No"
                    tvMcInfo.setTextColor(0xFFC62828.toInt())
                }
            }
            override fun onFailure(call: Call<DoffMachineValidateResponse>, t: Throwable) {
                etMcNo.setBackgroundResource(R.drawable.bg_input_invalid)
                tvMcInfo.text = "Validation failed"
                tvMcInfo.setTextColor(0xFFC62828.toInt())
            }
        })
    }

    private fun validateTrollyNo(trollyNo: String) {
        RetrofitClient.getApiService(this).validateDoffTrolly(
            trollyNo = trollyNo,
            branchId = if (branchId > 0) branchId else null
        ).enqueue(object : Callback<DoffTrollyValidateResponse> {
            override fun onResponse(call: Call<DoffTrollyValidateResponse>, response: Response<DoffTrollyValidateResponse>) {
                val body = response.body()
                if (response.isSuccessful && body?.status == "success" && body.trollyId != null) {
                    validatedTrollyId = body.trollyId
                    validatedTrollyWeight = body.trollyWeight ?: 0.0
                    validatedBucketWeight = body.bucketWeight ?: 0.0
                    etTrollyNo.setBackgroundResource(R.drawable.bg_input_valid)
                    val tare = validatedTrollyWeight + validatedBucketWeight
                    tvTrollyInfo.text = (body.trollyName ?: "Valid") +
                        if (tare > 0) "  (Tare: ${String.format(Locale.getDefault(), "%.3f", tare)})" else ""
                    tvTrollyInfo.setTextColor(0xFF2E7D32.toInt())
                    if (tare > 0) etTare.setText(String.format(Locale.getDefault(), "%.3f", tare))
                } else {
                    etTrollyNo.setBackgroundResource(R.drawable.bg_input_invalid)
                    tvTrollyInfo.text = body?.message ?: "Invalid Trolly No"
                    tvTrollyInfo.setTextColor(0xFFC62828.toInt())
                }
            }
            override fun onFailure(call: Call<DoffTrollyValidateResponse>, t: Throwable) {
                etTrollyNo.setBackgroundResource(R.drawable.bg_input_invalid)
                tvTrollyInfo.text = "Validation failed"
                tvTrollyInfo.setTextColor(0xFFC62828.toInt())
            }
        })
    }

    // ── Apply trolly returned inline by /doff/validate-machine ─────
    // Backend joins trolly_mst on trolly_posting_code = machine.mech_posting_code
    // and returns the matching trolly_id / posting_code / name / weights.
    // We write the posting code into the trolly input (so the user sees the
    // number that matches the machine), then short-circuit trolly validation
    // by using the inline trolly_id + tare directly — no second HTTP call.
    private fun applyTrollyFromMachineValidation(body: DoffMachineValidateResponse) {
        // Don't overwrite if user already typed something
        if (etTrollyNo.text.toString().trim().isNotEmpty()) return
        val trollyId = body.trollyId ?: return
        val display  = body.trollyPostingCode?.toString()
            ?: body.trollyName?.trim().orEmpty()
        if (display.isEmpty()) return

        validatedTrollyId     = trollyId
        validatedTrollyWeight = body.trollyWeight ?: 0.0
        validatedBucketWeight = body.bucketWeight ?: 0.0

        // Suppress the TextWatcher's debounced revalidation while we
        // populate the field; we already have the validation result.
        suppressTrollyWatcher = true
        etTrollyNo.setText(display)
        etTrollyNo.setSelection(etTrollyNo.text.length)
        suppressTrollyWatcher = false

        etTrollyNo.setBackgroundResource(R.drawable.bg_input_valid)
        val tare = validatedTrollyWeight + validatedBucketWeight
        tvTrollyInfo.text = (body.trollyName?.trim()?.ifBlank { null } ?: "Valid") +
            if (tare > 0) "  (Tare: ${String.format(Locale.getDefault(), "%.3f", tare)})" else ""
        tvTrollyInfo.setTextColor(0xFF2E7D32.toInt())
        if (tare > 0) etTare.setText(String.format(Locale.getDefault(), "%.3f", tare))
    }

    // ── Per-machine running totals ─────────────────────────────────
    private fun fetchRunningTotalsForMachine() {
        val mcId = validatedMcId ?: return
        val spellId = selectedSpellId()
        RetrofitClient.getApiService(this).getDoffSummary(
            date = entryDate,
            spellId = spellId,
            branchId = if (branchId > 0) branchId else null,
            mcId = mcId
        ).enqueue(object : Callback<DoffSummaryResponse> {
            override fun onResponse(call: Call<DoffSummaryResponse>, response: Response<DoffSummaryResponse>) {
                val row = response.body()?.summary?.firstOrNull { it.mcId == mcId }
                runningTotalWt = row?.totalWt ?: 0.0
                runningCount   = row?.noOfDoff ?: 0
                refreshTotalsBox()
            }
            override fun onFailure(call: Call<DoffSummaryResponse>, t: Throwable) {
                runningTotalWt = 0.0
                runningCount = 0
                refreshTotalsBox()
            }
        })
    }

    private fun refreshTotalsBox() {
        val net = etNet.text.toString().toDoubleOrNull() ?: 0.0
        etTotalDoffWt.setText(String.format(Locale.getDefault(), "%.3f", runningTotalWt + net))
        // No of doff = current count + 1 (next doff number for the new entry)
        etNoOfDoff.setText((runningCount + 1).toString())
    }

    // ── Form helpers ───────────────────────────────────────────────
    private fun recalcNet() {
        val gross = etGross.text.toString().toDoubleOrNull() ?: 0.0
        val tare  = etTare.text.toString().toDoubleOrNull()  ?: 0.0
        etNet.setText(String.format(Locale.getDefault(), "%.3f", gross - tare))
        if (validatedMcId != null) refreshTotalsBox()
    }

    private fun clearAfterSave(keepDateSpell: Boolean) {
        // Clear mc, trolly, weights, totals; optionally keep date+spell
        etMcNo.setText("")
        etTrollyNo.setText("")
        etGross.setText("")
        etTare.setText("")
        etNet.setText("")
        etTotalDoffWt.setText("")
        etNoOfDoff.setText("")
        resetMcValidation()
        resetTrollyValidation()
        if (!keepDateSpell) {
            // No-op for now (date/spell defaults reapplied elsewhere)
        }
    }

    // ── Save ───────────────────────────────────────────────────────
    private fun saveEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) {
            Toast.makeText(this, "Please select spell", Toast.LENGTH_SHORT).show(); return
        }
        val mcId = validatedMcId
        if (mcId == null) {
            Toast.makeText(this, "Please enter a valid Machine No", Toast.LENGTH_SHORT).show(); return
        }
        val trollyId = validatedTrollyId
        if (trollyId == null) {
            Toast.makeText(this, "Please enter a valid Trolly No", Toast.LENGTH_SHORT).show(); return
        }
        val gross = etGross.text.toString().toDoubleOrNull()
        if (gross == null || gross <= 0.0) {
            Toast.makeText(this, "Please enter gross weight", Toast.LENGTH_SHORT).show(); return
        }
        val tare = etTare.text.toString().toDoubleOrNull() ?: 0.0
        val net  = etNet.text.toString().toDoubleOrNull() ?: (gross - tare)

        val req = DoffSaveRequest(
            id           = null,
            doffDate     = entryDate,
            spellId      = spellId,
            mcId         = mcId,
            qualityId    = null,
            trollyId     = trollyId,
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
                    Toast.makeText(this@NewDoffEntryActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearAfterSave(keepDateSpell = true)
                        reloadSummary()
                    }
                }
                override fun onFailure(call: Call<DoffSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@NewDoffEntryActivity,
                        "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ── Summary list ───────────────────────────────────────────────
    private fun reloadSummary() {
        val spellId = selectedSpellId()
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility     = View.GONE
        rvSummary.visibility   = View.GONE
        RetrofitClient.getApiService(this).getDoffSummary(
            date     = entryDate,
            spellId  = spellId,
            branchId = if (branchId > 0) branchId else null,
            mcId     = null
        ).enqueue(object : Callback<DoffSummaryResponse> {
            override fun onResponse(call: Call<DoffSummaryResponse>, response: Response<DoffSummaryResponse>) {
                progressBar.visibility = View.GONE
                val list = response.body()?.summary.orEmpty()
                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    summaryAdapter.update(emptyList())
                } else {
                    rvSummary.visibility = View.VISIBLE
                    summaryAdapter.update(list)
                }
            }
            override fun onFailure(call: Call<DoffSummaryResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                summaryAdapter.update(emptyList())
            }
        })
    }
}

