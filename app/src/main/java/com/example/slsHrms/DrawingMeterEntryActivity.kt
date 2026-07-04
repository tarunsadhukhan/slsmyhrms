package com.example.slsHrms

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.DrawingSummaryAdapter
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DrawingMeterEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate       : TextView
    private lateinit var spEntrySpell      : Spinner
    private lateinit var llShedButtons     : LinearLayout
    private lateinit var tvShedStatus      : TextView
    private lateinit var llMachineButtons  : LinearLayout
    private lateinit var tvMachineStatus   : TextView
    private lateinit var tvMeter           : TextView
    private lateinit var etOpening         : EditText
    private lateinit var etClosing         : EditText
    private lateinit var tvUnit            : TextView
    private lateinit var etHours           : EditText
    private lateinit var tvEff             : TextView
    private lateinit var btnSave           : Button
    private lateinit var rvSummary         : RecyclerView
    private lateinit var tvSummaryEmpty    : TextView
    private lateinit var pbSummary         : ProgressBar

    private val spellList    = mutableListOf<Spell>()
    private val shedList     = mutableListOf<String>()
    private val machineList  = mutableListOf<DrawingMachine>()
    private lateinit var summaryAdapter: DrawingSummaryAdapter

    // Selected values
    private var selectedShed: String? = null
    private var selectedMachine: DrawingMachine? = null
    private val constValue = 100.0  // Efficiency constant - adjust as needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing_meter_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate      = findViewById(R.id.tvEntryDate)
        spEntrySpell     = findViewById(R.id.spEntrySpell)
        llShedButtons    = findViewById(R.id.llShedButtons)
        tvShedStatus     = findViewById(R.id.tvShedStatus)
        llMachineButtons = findViewById(R.id.llMachineButtons)
        tvMachineStatus  = findViewById(R.id.tvMachineStatus)
        tvMeter          = findViewById(R.id.tvMeter)
        etOpening        = findViewById(R.id.etOpening)
        etClosing        = findViewById(R.id.etClosing)
        tvUnit           = findViewById(R.id.tvUnit)
        etHours          = findViewById(R.id.etHours)
        tvEff            = findViewById(R.id.tvEff)
        btnSave          = findViewById(R.id.btnSave)
        rvSummary        = findViewById(R.id.rvSummary)
        tvSummaryEmpty   = findViewById(R.id.tvSummaryEmpty)
        pbSummary        = findViewById(R.id.pbSummary)

        summaryAdapter = DrawingSummaryAdapter()
        rvSummary.layoutManager = LinearLayoutManager(this)
        rvSummary.adapter = summaryAdapter
        rvSummary.isNestedScrollingEnabled = false

        // Init date
        val cal = Calendar.getInstance()
        entryDate        = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)

        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickDate() }

        // Load spells
        loadSpells()

        // Load sheds
        loadSheds()

        // Spell change listener - Auto-fill hours from spell
        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                clearForm()
                // Set working hours from selected spell after clearing form
                val spell = spellList.getOrNull(position)
                spell?.workingHours?.let { hours ->
                    etHours.setText(hours.toInt().toString())
                }
                loadSummary()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Opening field watcher - recalculate when opening changes
        etOpening.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateUnitAndEff()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Closing field watcher - auto-calculate unit and eff
        etClosing.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateUnitAndEff()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Hours field watcher - auto-calculate eff
        etHours.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateUnitAndEff()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSave.setOnClickListener { saveEntry() }
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        try {
            cal.time = apiDate.parse(entryDate)!!
        } catch (_: Exception) {
            // use today if parse fails
        }
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            entryDate        = apiDate.format(cal.time)
            tvEntryDate.text = dispDate.format(cal.time)
            clearForm()
            loadSummary()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadSpells() {
        RetrofitClient.getApiService(this).getDrawingSpells(branchId)
            .enqueue(object : Callback<SpellResponse> {
                override fun onResponse(call: Call<SpellResponse>, response: Response<SpellResponse>) {
                    spellList.clear()
                    spellList.addAll(response.body()?.spells ?: emptyList())
                    val adapter = ArrayAdapter(this@DrawingMeterEntryActivity, android.R.layout.simple_spinner_item, spellList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spEntrySpell.adapter = adapter

                    // Auto-fill hours from first spell on initial load
                    if (spellList.isNotEmpty()) {
                        val firstSpell = spellList[0]
                        firstSpell.workingHours?.let { hours ->
                            etHours.setText(hours.toInt().toString())
                        }
                        loadSummary()
                    }
                }
                override fun onFailure(call: Call<SpellResponse>, t: Throwable) {
                    Toast.makeText(this@DrawingMeterEntryActivity, "Failed to load spells: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadSheds() {
        RetrofitClient.getApiService(this).getDrawingSheds(branchId)
            .enqueue(object : Callback<DrawingShedsResponse> {
                override fun onResponse(call: Call<DrawingShedsResponse>, response: Response<DrawingShedsResponse>) {
                    shedList.clear()
                    shedList.addAll(response.body()?.sheds ?: emptyList())
                    renderShedButtons()
                }
                override fun onFailure(call: Call<DrawingShedsResponse>, t: Throwable) {
                    Toast.makeText(this@DrawingMeterEntryActivity, "Failed to load sheds: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun renderShedButtons() {
        llShedButtons.removeAllViews()
        if (shedList.isEmpty()) {
            tvShedStatus.text = "No sheds found"
            tvShedStatus.visibility = View.VISIBLE
            return
        }
        tvShedStatus.visibility = View.GONE

        for (shed in shedList) {
            val btn = Button(this).apply {
                text = shed
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1565C0"))
                val dp80 = (80 * resources.displayMetrics.density).toInt()
                val dp36 = (36 * resources.displayMetrics.density).toInt()
                val dp6  = (6 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(dp80, dp36).apply { marginEnd = dp6 }
                setOnClickListener { selectShed(shed) }
            }
            llShedButtons.addView(btn)
        }
    }

    private fun selectShed(shed: String) {
        selectedShed = shed
        // Update all button colors
        for (i in 0 until llShedButtons.childCount) {
            val btn = llShedButtons.getChildAt(i) as? Button ?: continue
            if (shedList.getOrNull(i) == shed) {
                btn.setBackgroundColor(Color.parseColor("#2E7D32"))  // Green
            } else {
                btn.setBackgroundColor(Color.parseColor("#1565C0"))  // Blue
            }
        }
        // Load machines for selected shed
        loadMachines(shed)
    }

    private fun loadMachines(shedType: String) {
        RetrofitClient.getApiService(this).getDrawingMachines(shedType, branchId)
            .enqueue(object : Callback<DrawingMachinesResponse> {
                override fun onResponse(call: Call<DrawingMachinesResponse>, response: Response<DrawingMachinesResponse>) {
                    machineList.clear()
                    machineList.addAll(response.body()?.machines ?: emptyList())
                    renderMachineButtons()
                }
                override fun onFailure(call: Call<DrawingMachinesResponse>, t: Throwable) {
                    Toast.makeText(this@DrawingMeterEntryActivity, "Failed to load machines: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun renderMachineButtons() {
        llMachineButtons.removeAllViews()
        if (machineList.isEmpty()) {
            tvMachineStatus.text = "No machines found for selected shed"
            tvMachineStatus.visibility = View.VISIBLE
            return
        }
        tvMachineStatus.visibility = View.GONE

        for (mc in machineList) {
            val btn = Button(this).apply {
                text = mc.mcShortName ?: ""
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1565C0"))
                val dp52 = (52 * resources.displayMetrics.density).toInt()
                val dp36 = (36 * resources.displayMetrics.density).toInt()
                val dp6  = (6 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(dp52, dp36).apply { marginEnd = dp6 }
                setOnClickListener { selectMachine(mc) }
            }
            llMachineButtons.addView(btn)
        }
    }

    private fun selectMachine(mc: DrawingMachine) {
        selectedMachine = mc
        // Update all button colors
        for (i in 0 until llMachineButtons.childCount) {
            val btn = llMachineButtons.getChildAt(i) as? Button ?: continue
            if (machineList.getOrNull(i)?.mcId == mc.mcId) {
                btn.setBackgroundColor(Color.parseColor("#2E7D32"))  // Green
            } else {
                btn.setBackgroundColor(Color.parseColor("#1565C0"))  // Blue
            }
        }

        // Update meter display (integer)
        tvMeter.text = (mc.contMeter?.toInt() ?: 0).toString()

        // Fetch opening meter
        loadOpeningMeter(mc.mcId)
    }

    private fun loadOpeningMeter(mcId: Int) {
        val spellId = selectedSpellId() ?: return
        RetrofitClient.getApiService(this).getDrawingOpeningMeter(entryDate, spellId, mcId)
            .enqueue(object : Callback<DrawingOpeningMeterResponse> {
                override fun onResponse(call: Call<DrawingOpeningMeterResponse>, response: Response<DrawingOpeningMeterResponse>) {
                    val opening = response.body()?.openingMeter?.toInt() ?: 0
                    etOpening.setText(opening.toString())
                }
                override fun onFailure(call: Call<DrawingOpeningMeterResponse>, t: Throwable) {
                    Toast.makeText(this@DrawingMeterEntryActivity, "Failed to fetch opening meter", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun calculateUnitAndEff() {
        val opening = etOpening.text.toString().toIntOrNull() ?: 0
        val closing = etClosing.text.toString().toIntOrNull() ?: 0
        val hours   = etHours.text.toString().toDoubleOrNull() ?: 0.0
        val constValue=tvMeter.text.toString().toDoubleOrNull() ?: 0.0
        val unit = closing - opening
        tvUnit.text = unit.toString()

        val eff = if (hours > 0 && constValue > 0) {
             (unit / (constValue/8*hours)   * 100)
        } else {
            0.0
        }
        tvEff.text = String.format(Locale.getDefault(), "%.2f", eff)
    }

    private fun saveEntry() {
        val spellId = selectedSpellId()
        val shed = selectedShed
        val mc = selectedMachine

        if (spellId == null) {
            Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show()
            return
        }
        if (shed == null) {
            Toast.makeText(this, "Please select a shed", Toast.LENGTH_SHORT).show()
            return
        }
        if (mc == null) {
            Toast.makeText(this, "Please select a machine", Toast.LENGTH_SHORT).show()
            return
        }

        val opening = etOpening.text.toString().toIntOrNull() ?: 0
        val closing = etClosing.text.toString().toIntOrNull() ?: 0
        val hours   = etHours.text.toString().toDoubleOrNull() ?: 0.0

        if (closing == 0) {
            Toast.makeText(this, "Please enter closing meter", Toast.LENGTH_SHORT).show()
            return
        }

        val req = DrawingEntrySaveRequest(
            date = entryDate,
            spellId = spellId,
            shedType = shed,
            mcId = mc.mcId,
            openingMeter = opening.toDouble(),
            closingMeter = closing.toDouble(),
            hours = hours,
            constValue = constValue,
            branchId = branchId,
            userId = userId
        )

        btnSave.isEnabled = false
        RetrofitClient.getApiService(this).saveDrawingEntry(req)
            .enqueue(object : Callback<DrawingEntrySaveResponse> {
                override fun onResponse(call: Call<DrawingEntrySaveResponse>, response: Response<DrawingEntrySaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: "Entry saved"
                    Toast.makeText(this@DrawingMeterEntryActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        // Keep shed selection + machine buttons + hours.
                        // Only reset per-entry fields so user can pick the next machine.
                        clearEntryFieldsKeepShedAndMachines()
                        loadSummary()
                    }
                }
                override fun onFailure(call: Call<DrawingEntrySaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@DrawingMeterEntryActivity, "Save failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadSummary() {
        val spellId = selectedSpellId() ?: return
        pbSummary.visibility = View.VISIBLE
        tvSummaryEmpty.visibility = View.GONE

        RetrofitClient.getApiService(this).getDrawingSummary(entryDate, spellId, branchId)
            .enqueue(object : Callback<DrawingSummaryResponse> {
                override fun onResponse(call: Call<DrawingSummaryResponse>, response: Response<DrawingSummaryResponse>) {
                    pbSummary.visibility = View.GONE
                    val summary = response.body()?.summary ?: emptyList()
                    if (summary.isEmpty()) {
                        tvSummaryEmpty.visibility = View.VISIBLE
                        summaryAdapter.submitList(emptyList())
                    } else {
                        tvSummaryEmpty.visibility = View.GONE
                        summaryAdapter.submitList(summary)
                    }
                }
                override fun onFailure(call: Call<DrawingSummaryResponse>, t: Throwable) {
                    pbSummary.visibility = View.GONE
                    tvSummaryEmpty.visibility = View.VISIBLE
                    tvSummaryEmpty.text = "Error loading summary"
                }
            })
    }

    /**
     * Full reset – wipes shed/machine selection and entry fields.
     * Used on spell change and date change.
     */
    private fun clearForm() {
        selectedMachine = null
        selectedShed = null

        // Reset shed button colors
        for (i in 0 until llShedButtons.childCount) {
            val btn = llShedButtons.getChildAt(i) as? Button ?: continue
            btn.setBackgroundColor(Color.parseColor("#1565C0"))
        }

        llMachineButtons.removeAllViews()
        tvMeter.text = "—"
        etOpening.setText("")
        etClosing.setText("")
        // Don't clear hours - keep value from selected spell
        tvUnit.text = "0"
        tvEff.text = "0.0"
    }

    /**
     * After-save reset – keeps shed selection, the machine button list and hours.
     * Only deselects the current machine and clears the meter / opening / closing / unit / eff
     * so the user can immediately tap the next machine in the same shed.
     */
    private fun clearEntryFieldsKeepShedAndMachines() {
        selectedMachine = null

        // Reset only the machine button colors (do NOT remove buttons)
        for (i in 0 until llMachineButtons.childCount) {
            val btn = llMachineButtons.getChildAt(i) as? Button ?: continue
            btn.setBackgroundColor(Color.parseColor("#1565C0"))
        }

        tvMeter.text = "—"
        etOpening.setText("")
        etClosing.setText("")
        tvUnit.text = "0"
        tvEff.text = "0.0"
        // selectedShed, shed button colors, machine buttons and etHours are kept as-is.
    }

    private fun selectedSpellId(): Int? = spellList.getOrNull(spEntrySpell.selectedItemPosition)?.spellId
}
