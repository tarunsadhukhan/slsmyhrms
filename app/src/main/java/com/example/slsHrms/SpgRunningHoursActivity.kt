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

class SpgRunningHoursActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate     : TextView
    private lateinit var spEntrySpell    : Spinner
    private lateinit var llMachines      : LinearLayout
    private lateinit var tvMachinesStatus: TextView
    private lateinit var etRunningHours  : EditText
    private lateinit var btnSave         : Button
    private lateinit var rvEntries       : RecyclerView
    private lateinit var tvEntriesEmpty  : TextView
    private lateinit var pbEntries       : ProgressBar

    private val spellList    = mutableListOf<Spell>()
    private val machinesList = mutableListOf<Spg1MechCode>()

    private var selectedMcId: Int? = null
    private var editingEntryId: Int? = null

    private val adapter = EntriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spg_running_hours)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate      = findViewById(R.id.tvEntryDate)
        spEntrySpell     = findViewById(R.id.spEntrySpell)
        llMachines       = findViewById(R.id.llMachines)
        tvMachinesStatus = findViewById(R.id.tvMachinesStatus)
        etRunningHours   = findViewById(R.id.etRunningHours)
        btnSave          = findViewById(R.id.btnSave)
        rvEntries        = findViewById(R.id.rvEntries)
        tvEntriesEmpty   = findViewById(R.id.tvEntriesEmpty)
        pbEntries        = findViewById(R.id.pbEntries)

        rvEntries.layoutManager = LinearLayoutManager(this)
        rvEntries.adapter = adapter
        rvEntries.isNestedScrollingEnabled = false

        val cal = Calendar.getInstance()
        entryDate        = apiDate.format(cal.time)
        tvEntryDate.text = dispDate.format(cal.time)

        findViewById<View>(R.id.btnEntryDate).setOnClickListener { pickDate() }

        spEntrySpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadMachines()
                loadEntries()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveEntry() }

        loadSpells {
            loadMachines()
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
            loadMachines()
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
                        this@SpgRunningHoursActivity,
                        R.layout.spinner_item_black,
                        spellList.map { it.spellName ?: "" }
                    ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_black) }
                    if (spellList.isNotEmpty()) spEntrySpell.setSelection(0)
                    onDone()
                }
                override fun onFailure(call: Call<SpellResponse>, t: Throwable) { onDone() }
            })
    }

    private fun loadMachines() {
        val spellId = selectedSpellId() ?: return
        if (branchId <= 0) {
            tvMachinesStatus.text = "Branch not set"
            tvMachinesStatus.visibility = View.VISIBLE
            return
        }
        llMachines.removeAllViews()
        selectedMcId = null
        tvMachinesStatus.visibility = View.VISIBLE
        tvMachinesStatus.text = "Loading…"

        RetrofitClient.getApiService(this).getSpg1MechCodes(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId
        ).enqueue(object : Callback<Spg1MechCodesResponse> {
            override fun onResponse(call: Call<Spg1MechCodesResponse>, response: Response<Spg1MechCodesResponse>) {
                machinesList.clear()
                machinesList.addAll(response.body()?.codes.orEmpty())
                if (machinesList.isEmpty()) {
                    tvMachinesStatus.text = "No machines found for selected date/spell"
                    tvMachinesStatus.visibility = View.VISIBLE
                } else {
                    tvMachinesStatus.visibility = View.GONE
                    renderMachineButtons()
                }
            }
            override fun onFailure(call: Call<Spg1MechCodesResponse>, t: Throwable) {
                tvMachinesStatus.text = "Failed to load machines"
                tvMachinesStatus.visibility = View.VISIBLE
            }
        })
    }

    private fun renderMachineButtons() {
        llMachines.removeAllViews()
        val dp = resources.displayMetrics.density
        for (code in machinesList) {
            val rawLabel = code.mechPostingCode?.toString()
                ?: code.mcCode?.takeIf { it.isNotBlank() }
                ?: "MC${code.mcId}"
            val label = rawLabel.take(3)
            val btn = Button(this)
            btn.text = label
            btn.textSize = 11f
            btn.setTypeface(null, Typeface.BOLD)
            btn.setTextColor(Color.WHITE)
            btn.setBackgroundColor(Color.parseColor("#1565C0"))
            val lp = LinearLayout.LayoutParams((44 * dp).toInt(), (34 * dp).toInt())
            lp.setMargins(0, 0, (6 * dp).toInt(), 0)
            btn.layoutParams = lp
            btn.setPadding(0, 0, 0, 0)
            btn.tag = code
            btn.setOnClickListener { onMachineSelected(btn, code) }
            llMachines.addView(btn)
        }
    }

    private fun onMachineSelected(btn: Button, code: Spg1MechCode) {
        selectedMcId = code.mcId
        for (i in 0 until llMachines.childCount) {
            val child = llMachines.getChildAt(i) as? Button ?: continue
            child.setBackgroundColor(
                if (child == btn) Color.parseColor("#2E7D32")
                else              Color.parseColor("#1565C0")
            )
        }
    }

    private fun selectedSpellId(): Int? =
        spellList.getOrNull(spEntrySpell.selectedItemPosition)?.spellId

    private fun saveEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) {
            Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show(); return
        }
        val mcId = selectedMcId
        if (mcId == null) {
            Toast.makeText(this, "Please select a Machine", Toast.LENGTH_SHORT).show(); return
        }
        val hours = etRunningHours.text.toString().toDoubleOrNull()
        if (hours == null || hours <= 0.0) {
            Toast.makeText(this, "Please enter Running Hours", Toast.LENGTH_SHORT).show(); return
        }

        if (branchId <= 0) {
            Toast.makeText(this, "Branch not set", Toast.LENGTH_SHORT).show(); return
        }

        btnSave.isEnabled = false
        val editId = editingEntryId
        if (editId != null) {
            val updReq = SpgRunningUpdateRequest(
                mcId = mcId, branchId = branchId, mcRunsTime = hours,
                kwConsumption = null, userId = userId
            )
            RetrofitClient.getApiService(this).updateSpgRunningHours(editId, updReq)
                .enqueue(object : Callback<SpgRunningSaveResponse> {
                    override fun onResponse(call: Call<SpgRunningSaveResponse>, response: Response<SpgRunningSaveResponse>) {
                        btnSave.isEnabled = true
                        val msg = response.body()?.message ?: if (response.isSuccessful) "Updated" else "Update failed"
                        Toast.makeText(this@SpgRunningHoursActivity, msg, Toast.LENGTH_SHORT).show()
                        if (response.isSuccessful && response.body()?.status == "success") {
                            clearForm()
                            loadEntries()
                        }
                    }
                    override fun onFailure(call: Call<SpgRunningSaveResponse>, t: Throwable) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@SpgRunningHoursActivity,
                            "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
            return
        }

        val req = SpgRunningSaveRequest(
            date          = entryDate,
            spellId       = spellId,
            branchId      = branchId,
            mcId          = mcId,
            mcRunsTime    = hours,
            kwConsumption = null,
            userId        = userId
        )
        RetrofitClient.getApiService(this).saveSpgRunningHours(req)
            .enqueue(object : Callback<SpgRunningSaveResponse> {
                override fun onResponse(call: Call<SpgRunningSaveResponse>, response: Response<SpgRunningSaveResponse>) {
                    btnSave.isEnabled = true
                    val msg = response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed"
                    Toast.makeText(this@SpgRunningHoursActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm()
                        loadEntries()
                    }
                }
                override fun onFailure(call: Call<SpgRunningSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@SpgRunningHoursActivity,
                        "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun beginEdit(e: SpgRunningEntry) {
        editingEntryId = e.id
        btnSave.text = "Update"
        e.mcId?.let { id ->
            selectedMcId = id
            for (i in 0 until llMachines.childCount) {
                val child = llMachines.getChildAt(i) as? Button ?: continue
                val tag = child.tag as? Spg1MechCode
                child.setBackgroundColor(
                    if (tag?.mcId == id) Color.parseColor("#2E7D32")
                    else                 Color.parseColor("#1565C0")
                )
            }
        }
        etRunningHours.setText(e.mcRunsTime?.let { fmt(it) } ?: "")
        Toast.makeText(this, "Editing entry #${e.id}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(e: SpgRunningEntry) {
        val id = e.id ?: return
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("${e.mcName ?: "-"} | ${fmt(e.mcRunsTime ?: 0.0)} hrs")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteSpgRunningHours(id)
                    .enqueue(object : Callback<SpgRunningSaveResponse> {
                        override fun onResponse(call: Call<SpgRunningSaveResponse>, response: Response<SpgRunningSaveResponse>) {
                            Toast.makeText(this@SpgRunningHoursActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            if (editingEntryId == id) clearForm()
                            loadEntries()
                        }
                        override fun onFailure(call: Call<SpgRunningSaveResponse>, t: Throwable) {
                            Toast.makeText(this@SpgRunningHoursActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearForm() {
        etRunningHours.setText("")
        selectedMcId = null
        editingEntryId = null
        btnSave.text = "Save"
        for (i in 0 until llMachines.childCount) {
            (llMachines.getChildAt(i) as? Button)
                ?.setBackgroundColor(Color.parseColor("#1565C0"))
        }
    }

    private fun fmt(v: Double) = String.format(Locale.getDefault(), "%.3f", v)

    private fun loadEntries() {
        val spellId = selectedSpellId() ?: return

        pbEntries.visibility      = View.VISIBLE
        tvEntriesEmpty.visibility = View.GONE
        rvEntries.visibility      = View.GONE

        RetrofitClient.getApiService(this).getSpgRunningHours(
            date     = entryDate,
            spellId  = spellId,
            branchId = branchId.takeIf { it > 0 }
        ).enqueue(object : Callback<SpgRunningListResponse> {
            override fun onResponse(call: Call<SpgRunningListResponse>, response: Response<SpgRunningListResponse>) {
                pbEntries.visibility = View.GONE
                val list = response.body()?.entries.orEmpty()
                if (list.isEmpty()) {
                    tvEntriesEmpty.visibility = View.VISIBLE
                } else {
                    rvEntries.visibility = View.VISIBLE
                    adapter.update(list)
                }
            }
            override fun onFailure(call: Call<SpgRunningListResponse>, t: Throwable) {
                pbEntries.visibility      = View.GONE
                tvEntriesEmpty.visibility = View.VISIBLE
                tvEntriesEmpty.text       = "Failed to load entries"
            }
        })
    }

    inner class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.VH>() {
        private val items = mutableListOf<SpgRunningEntry>()
        fun update(list: List<SpgRunningEntry>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_spg_running_entry, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvMachine       = v.findViewById<TextView>(R.id.tvMachine)
            private val tvRunningHours  = v.findViewById<TextView>(R.id.tvRunningHours)
            private val ivEdit          = v.findViewById<ImageView>(R.id.ivEdit)
            private val ivDelete        = v.findViewById<ImageView>(R.id.ivDelete)
            fun bind(e: SpgRunningEntry) {
                tvMachine.text      = e.mcName ?: "-"
                tvRunningHours.text = fmt(e.mcRunsTime ?: 0.0)
                ivEdit.setOnClickListener   { beginEdit(e) }
                ivDelete.setOnClickListener { confirmDelete(e) }
            }
        }
    }
}
