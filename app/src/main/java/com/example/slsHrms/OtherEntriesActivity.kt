package com.example.slsHrms

import android.app.AlertDialog
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

class OtherEntriesActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""

    private lateinit var tvEntryDate    : TextView
    private lateinit var spEntrySpell   : Spinner
    private lateinit var etLooms        : EditText
    private lateinit var etCuts         : EditText
    private lateinit var etHemming      : EditText
    private lateinit var etHeracle      : EditText
    private lateinit var etCutting      : EditText
    private lateinit var etBranding     : EditText
    private lateinit var etHandSewer    : EditText
    private lateinit var etBales        : EditText
    private lateinit var etIssueBales   : EditText
    private lateinit var btnSave        : Button
    private lateinit var rvEntries      : RecyclerView
    private lateinit var tvEntriesEmpty : TextView
    private lateinit var pbEntries      : ProgressBar

    private val spellList = mutableListOf<Spell>()
    private var editingEntryId: Int? = null
    private val adapter = EntriesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_entries)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate    = findViewById(R.id.tvEntryDate)
        spEntrySpell   = findViewById(R.id.spEntrySpell)
        etLooms        = findViewById(R.id.etLooms)
        etCuts         = findViewById(R.id.etCuts)
        etHemming      = findViewById(R.id.etHemming)
        etHeracle      = findViewById(R.id.etHeracle)
        etCutting      = findViewById(R.id.etCutting)
        etBranding     = findViewById(R.id.etBranding)
        etHandSewer    = findViewById(R.id.etHandSewer)
        etBales        = findViewById(R.id.etBales)
        etIssueBales   = findViewById(R.id.etIssueBales)
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
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadEntries()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener { saveEntry() }

        loadSpells { loadEntries() }
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
                        this@OtherEntriesActivity,
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

    private fun textInt(et: EditText): Int? = et.text.toString().trim().toIntOrNull()

    private fun saveEntry() {
        val spellId = selectedSpellId()
        if (spellId == null) {
            Toast.makeText(this, "Please select a spell", Toast.LENGTH_SHORT).show(); return
        }

        btnSave.isEnabled = false
        val editId = editingEntryId
        if (editId != null) {
            val req = FinishingUpdateRequest(
                date       = entryDate,
                spellId    = spellId,
                looms      = textInt(etLooms),
                cuts       = textInt(etCuts),
                hemming    = textInt(etHemming),
                heracle    = textInt(etHeracle),
                cutting    = textInt(etCutting),
                branding   = textInt(etBranding),
                handSewer  = textInt(etHandSewer),
                bales      = textInt(etBales),
                issueBales = textInt(etIssueBales),
                userId     = userId
            )
            RetrofitClient.getApiService(this).updateFinishingEntry(editId, req)
                .enqueue(object : Callback<FinishingSaveResponse> {
                    override fun onResponse(call: Call<FinishingSaveResponse>, response: Response<FinishingSaveResponse>) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@OtherEntriesActivity,
                            response.body()?.message ?: if (response.isSuccessful) "Updated" else "Update failed",
                            Toast.LENGTH_SHORT).show()
                        if (response.isSuccessful && response.body()?.status == "success") {
                            clearForm(); loadEntries()
                        }
                    }
                    override fun onFailure(call: Call<FinishingSaveResponse>, t: Throwable) {
                        btnSave.isEnabled = true
                        Toast.makeText(this@OtherEntriesActivity,
                            "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                })
            return
        }

        val req = FinishingSaveRequest(
            date       = entryDate,
            spellId    = spellId,
            looms      = textInt(etLooms),
            cuts       = textInt(etCuts),
            hemming    = textInt(etHemming),
            heracle    = textInt(etHeracle),
            cutting    = textInt(etCutting),
            branding   = textInt(etBranding),
            handSewer  = textInt(etHandSewer),
            bales      = textInt(etBales),
            issueBales = textInt(etIssueBales),
            userId     = userId
        )
        RetrofitClient.getApiService(this).saveFinishingEntry(req)
            .enqueue(object : Callback<FinishingSaveResponse> {
                override fun onResponse(call: Call<FinishingSaveResponse>, response: Response<FinishingSaveResponse>) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@OtherEntriesActivity,
                        response.body()?.message ?: if (response.isSuccessful) "Saved" else "Save failed",
                        Toast.LENGTH_SHORT).show()
                    if (response.isSuccessful && response.body()?.status == "success") {
                        clearForm(); loadEntries()
                    }
                }
                override fun onFailure(call: Call<FinishingSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@OtherEntriesActivity,
                        "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun clearForm() {
        etLooms.setText(""); etCuts.setText("")
        etHemming.setText(""); etHeracle.setText(""); etCutting.setText("")
        etBranding.setText(""); etHandSewer.setText("")
        etBales.setText(""); etIssueBales.setText("")
        editingEntryId = null
        btnSave.text = "Save"
    }

    private fun beginEdit(e: FinishingEntry) {
        editingEntryId = e.id
        btnSave.text = "Update"
        // pre-select spell
        e.spellId?.let { sid ->
            val pos = spellList.indexOfFirst { it.spellId == sid }
            if (pos >= 0) spEntrySpell.setSelection(pos)
        }
        etLooms.setText(e.looms?.toString() ?: "")
        etCuts.setText(e.cuts?.toString() ?: "")
        etHemming.setText(e.hemming?.toString() ?: "")
        etHeracle.setText(e.heracle?.toString() ?: "")
        etCutting.setText(e.cutting?.toString() ?: "")
        etBranding.setText(e.branding?.toString() ?: "")
        etHandSewer.setText(e.handSewer?.toString() ?: "")
        etBales.setText(e.bales?.toString() ?: "")
        etIssueBales.setText(e.issueBales?.toString() ?: "")
        Toast.makeText(this, "Editing entry #${e.id}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDelete(e: FinishingEntry) {
        val id = e.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete entry?")
            .setMessage("Spell ${e.spellName ?: "-"} | Looms ${e.looms ?: 0} | Bales ${e.bales ?: 0}")
            .setPositiveButton("Delete") { _, _ ->
                RetrofitClient.getApiService(this).deleteFinishingEntry(id)
                    .enqueue(object : Callback<FinishingSaveResponse> {
                        override fun onResponse(call: Call<FinishingSaveResponse>, response: Response<FinishingSaveResponse>) {
                            Toast.makeText(this@OtherEntriesActivity,
                                response.body()?.message ?: "Deleted", Toast.LENGTH_SHORT).show()
                            if (editingEntryId == id) clearForm()
                            loadEntries()
                        }
                        override fun onFailure(call: Call<FinishingSaveResponse>, t: Throwable) {
                            Toast.makeText(this@OtherEntriesActivity,
                                "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadEntries() {
        pbEntries.visibility      = View.VISIBLE
        tvEntriesEmpty.visibility = View.GONE
        rvEntries.visibility      = View.GONE
        RetrofitClient.getApiService(this).getFinishingEntries(date = entryDate)
            .enqueue(object : Callback<FinishingEntryListResponse> {
                override fun onResponse(call: Call<FinishingEntryListResponse>, response: Response<FinishingEntryListResponse>) {
                    pbEntries.visibility = View.GONE
                    val list = response.body()?.entries.orEmpty()
                    if (list.isEmpty()) {
                        tvEntriesEmpty.visibility = View.VISIBLE
                    } else {
                        rvEntries.visibility = View.VISIBLE
                        adapter.update(list)
                    }
                }
                override fun onFailure(call: Call<FinishingEntryListResponse>, t: Throwable) {
                    pbEntries.visibility      = View.GONE
                    tvEntriesEmpty.visibility = View.VISIBLE
                    tvEntriesEmpty.text       = "Failed to load entries"
                }
            })
    }

    inner class EntriesAdapter : RecyclerView.Adapter<EntriesAdapter.VH>() {
        private val items = mutableListOf<FinishingEntry>()
        fun update(list: List<FinishingEntry>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_finishing_entry, parent, false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos])

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvSpell     = v.findViewById<TextView>(R.id.tvSpell)
            private val tvLooms     = v.findViewById<TextView>(R.id.tvLooms)
            private val tvCuts      = v.findViewById<TextView>(R.id.tvCuts)
            private val tvHemming   = v.findViewById<TextView>(R.id.tvHemming)
            private val tvHeracle   = v.findViewById<TextView>(R.id.tvHeracle)
            private val tvCutting   = v.findViewById<TextView>(R.id.tvCutting)
            private val tvBranding  = v.findViewById<TextView>(R.id.tvBranding)
            private val tvHandSewer = v.findViewById<TextView>(R.id.tvHandSewer)
            private val tvBales      = v.findViewById<TextView>(R.id.tvBales)
            private val tvIssueBales = v.findViewById<TextView>(R.id.tvIssueBales)
            private val ivEdit       = v.findViewById<ImageView>(R.id.ivEdit)
            private val ivDelete     = v.findViewById<ImageView>(R.id.ivDelete)
            fun bind(e: FinishingEntry) {
                tvSpell.text      = e.spellName ?: e.spellId?.toString() ?: "-"
                tvLooms.text      = e.looms?.toString() ?: "-"
                tvCuts.text       = e.cuts?.toString() ?: "-"
                tvHemming.text    = e.hemming?.toString() ?: "-"
                tvHeracle.text    = e.heracle?.toString() ?: "-"
                tvCutting.text    = e.cutting?.toString() ?: "-"
                tvBranding.text   = e.branding?.toString() ?: "-"
                tvHandSewer.text  = e.handSewer?.toString() ?: "-"
                tvBales.text      = e.bales?.toString() ?: "-"
                tvIssueBales.text = e.issueBales?.toString() ?: "-"
                ivEdit.setOnClickListener   { beginEdit(e) }
                ivDelete.setOnClickListener { confirmDelete(e) }
            }
        }
    }
}