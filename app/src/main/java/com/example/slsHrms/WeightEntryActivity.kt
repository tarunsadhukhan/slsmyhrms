package com.example.slsHrms

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.adapter.WeightTransactionAdapter
import com.example.slsHrms.api.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeightEntryActivity : AppCompatActivity() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private var branchId  = 0
    private var userId    = 0
    private var entryDate = ""
    private var listDate  = ""

    private val spellList = mutableListOf<Spell>()
    private lateinit var spellAdapter: ArrayAdapter<Spell>
    private lateinit var wtAdapter: WeightTransactionAdapter

    private lateinit var tvEntryDate: TextView
    private lateinit var tvListDate:  TextView
    private lateinit var spSpell:     Spinner
    private lateinit var etGross:     EditText
    private lateinit var etTare:      EditText
    private lateinit var etNet:       EditText
    private lateinit var btnSave:     Button
    private lateinit var rvList:      RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty:     TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_entry)

        branchId = intent.getIntExtra("BRANCH_ID", 0)
        userId   = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        tvEntryDate = findViewById(R.id.tvWtDate)
        tvListDate  = findViewById(R.id.tvListDate)
        spSpell     = findViewById(R.id.spWtSpell)
        etGross     = findViewById(R.id.etWtGross)
        etTare      = findViewById(R.id.etWtTare)
        etNet       = findViewById(R.id.etWtNet)
        btnSave     = findViewById(R.id.btnWtSave)
        rvList      = findViewById(R.id.rvWeightTransactions)
        progressBar = findViewById(R.id.progressWt)
        tvEmpty     = findViewById(R.id.tvWtEmpty)

        val cal = Calendar.getInstance()
        entryDate       = apiDate.format(cal.time)
        listDate        = entryDate
        tvEntryDate.text = dispDate.format(cal.time)
        tvListDate.text  = dispDate.format(cal.time)

        findViewById<View>(R.id.btnWtDate).setOnClickListener { pickDate(isEntry = true) }
        findViewById<View>(R.id.btnListDate).setOnClickListener { pickDate(isEntry = false) }

        spellAdapter = ArrayAdapter(this, R.layout.spinner_item_black, spellList)
        spellAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        spSpell.adapter = spellAdapter

        wtAdapter = WeightTransactionAdapter(emptyList()) { confirmDelete(it) }
        rvList.layoutManager = LinearLayoutManager(this)
        rvList.adapter = wtAdapter
        rvList.isNestedScrollingEnabled = false

        // Auto-compute net weight
        val weightWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { computeNet() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        }
        etGross.addTextChangedListener(weightWatcher)
        etTare.addTextChangedListener(weightWatcher)

        btnSave.setOnClickListener { saveEntry() }

        loadSpells()
        reloadList()
    }

    private fun pickDate(isEntry: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            val api  = apiDate.format(cal.time)
            val disp = dispDate.format(cal.time)
            if (isEntry) { entryDate = api; tvEntryDate.text = disp }
            else         { listDate  = api; tvListDate.text  = disp; reloadList() }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadSpells() {
        RetrofitClient.getApiService(this).getSpells(branchId.takeIf { it > 0 })
            .enqueue(object : Callback<SpellResponse> {
                override fun onResponse(call: Call<SpellResponse>, response: Response<SpellResponse>) {
                    val spells = response.body()?.spells.orEmpty()
                    spellList.clear()
                    spellList.add(Spell(0, "-- Spell --"))
                    spellList.addAll(spells)
                    spellAdapter.notifyDataSetChanged()
                }
                override fun onFailure(call: Call<SpellResponse>, t: Throwable) {}
            })
    }

    private fun computeNet() {
        val gross = etGross.text.toString().toDoubleOrNull() ?: 0.0
        val tare  = etTare.text.toString().toDoubleOrNull()  ?: 0.0
        etNet.setText(String.format("%.3f", gross - tare))
    }

    private fun saveEntry() {
        val gross   = etGross.text.toString().toDoubleOrNull() ?: 0.0
        val tare    = etTare.text.toString().toDoubleOrNull()  ?: 0.0
        val net     = etNet.text.toString().toDoubleOrNull()   ?: (gross - tare)
        val spellId = (spSpell.selectedItem as? Spell)?.spellId?.takeIf { it > 0 }

        if (gross <= 0) {
            Toast.makeText(this, "Enter gross weight", Toast.LENGTH_SHORT).show()
            return
        }

        val req = WeightSaveRequest(
            tranDate    = entryDate,
            branchId    = branchId,
            spellId     = spellId,
            grossWeight = gross,
            tareWeight  = tare,
            netWeight   = net,
            userId      = userId
        )

        btnSave.isEnabled = false
        RetrofitClient.getApiService(this).saveWeightTransaction(req)
            .enqueue(object : Callback<WeightSaveResponse> {
                override fun onResponse(call: Call<WeightSaveResponse>, response: Response<WeightSaveResponse>) {
                    btnSave.isEnabled = true
                    if (response.isSuccessful && response.body()?.status == "success") {
                        Toast.makeText(this@WeightEntryActivity, "Saved successfully", Toast.LENGTH_SHORT).show()
                        etGross.setText("")
                        etTare.setText("")
                        etNet.setText("")
                        reloadList()
                    } else {
                        Toast.makeText(this@WeightEntryActivity, "Save failed", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<WeightSaveResponse>, t: Throwable) {
                    btnSave.isEnabled = true
                    Toast.makeText(this@WeightEntryActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun reloadList() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility     = View.GONE
        RetrofitClient.getApiService(this).getWeightTransactions(
            branchId = branchId.takeIf { it > 0 },
            date     = listDate.ifBlank { null }
        ).enqueue(object : Callback<WeightListResponse> {
            override fun onResponse(call: Call<WeightListResponse>, response: Response<WeightListResponse>) {
                progressBar.visibility = View.GONE
                val items = response.body()?.transactions.orEmpty()
                if (items.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvList.visibility  = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvList.visibility  = View.VISIBLE
                    wtAdapter.updateList(items)
                }
            }
            override fun onFailure(call: Call<WeightListResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@WeightEntryActivity, "Load error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun confirmDelete(item: WeightTransaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete this weight record?")
            .setPositiveButton("Delete") { _, _ ->
                val id = item.id ?: return@setPositiveButton
                RetrofitClient.getApiService(this).deleteWeightTransaction(id)
                    .enqueue(object : Callback<WeightSaveResponse> {
                        override fun onResponse(call: Call<WeightSaveResponse>, response: Response<WeightSaveResponse>) {
                            reloadList()
                        }
                        override fun onFailure(call: Call<WeightSaveResponse>, t: Throwable) {
                            Toast.makeText(this@WeightEntryActivity, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
