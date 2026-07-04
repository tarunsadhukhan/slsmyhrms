package com.example.slsHrms

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import java.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import com.example.slsHrms.api.AbsentBuckets
import com.example.slsHrms.api.AttendanceDashboardResponse
import com.example.slsHrms.api.DayPresent
import com.example.slsHrms.api.ManMachineDay
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.api.Shift
import com.example.slsHrms.api.TodayAttendance
import com.example.slsHrms.api.WagesDay
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class AttendanceDashboardActivity : AppCompatActivity() {

    private val TAG = "AttDashboard"

    private val apiDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFmt = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    private val selectedCalendar: Calendar = Calendar.getInstance()

    private var selectedSpellId: Int? = null
    private val spells = mutableListOf<Shift>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_dashboard)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        setupWelcome()
        setupLogout()
        setupMenus()
        setupDatePicker()
        setupSpellSpinner()
        setupCharts()
    }

    private fun setupSpellSpinner() {
        val branchId = intent.getIntExtra("BRANCH_ID", 0).takeIf { it > 0 }
        val spinner = findViewById<android.widget.Spinner>(R.id.spDashboardSpell)
        RetrofitClient.getApiService(this).getShifts(branchId)
            .enqueue(object : retrofit2.Callback<com.example.slsHrms.api.ShiftResponse> {
                override fun onResponse(call: retrofit2.Call<com.example.slsHrms.api.ShiftResponse>,
                                        response: retrofit2.Response<com.example.slsHrms.api.ShiftResponse>) {
                    spells.clear()
                    spells.add(com.example.slsHrms.api.Shift(0, name = "All Spells"))
                    spells.addAll(response.body()?.shifts.orEmpty())
                    val adapter = android.widget.ArrayAdapter(
                        this@AttendanceDashboardActivity,
                        R.layout.spinner_item_black, spells)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
                    spinner.adapter = adapter
                    spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                            val s = spells.getOrNull(pos)
                            selectedSpellId = if ((s?.id ?: 0) > 0) s?.id else null
                            loadDashboardData()
                        }
                        override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                    }
                }
                override fun onFailure(call: retrofit2.Call<com.example.slsHrms.api.ShiftResponse>, t: Throwable) {
                    Log.w(TAG, "Failed to load spells: ${t.message}")
                }
            })
    }

    private fun setupDatePicker() {
        val etDate = findViewById<EditText>(R.id.etDashboardDate)
        val btnRefresh = findViewById<Button>(R.id.btnRefreshDashboard)

        // Default to today
        etDate.setText(displayDateFmt.format(selectedCalendar.time))

        val showPicker = {
            val dlg = DatePickerDialog(
                this,
                { _, y, m, d ->
                    selectedCalendar.set(Calendar.YEAR, y)
                    selectedCalendar.set(Calendar.MONTH, m)
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, d)
                    etDate.setText(displayDateFmt.format(selectedCalendar.time))
                    loadDashboardData()
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            )
            dlg.datePicker.maxDate = System.currentTimeMillis()
            dlg.show()
        }

        etDate.setOnClickListener { showPicker() }
        btnRefresh.setOnClickListener { loadDashboardData() }
    }

    private fun setupWelcome() {
        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        findViewById<android.widget.TextView>(R.id.tvUserName).text = userName
        findViewById<android.widget.TextView>(R.id.tvWelcome).text = "Welcome to Attendance Dashboard"
    }

    private fun setupLogout() {
        findViewById<android.widget.Button>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupMenus() {
        // Toggle Attendance Menu
        findViewById<View>(R.id.headerAttendance).setOnClickListener {
            val subMenu = findViewById<View>(R.id.subMenuAttendance)
            val arrow = findViewById<ImageView>(R.id.arrowAttendance)
            toggleSubMenu(subMenu, arrow)
        }

        // Attendance Dashboard - Stay on this page
        findViewById<View>(R.id.menuAttendanceDashboard).setOnClickListener {
            val subMenu = findViewById<View>(R.id.subMenuAttendance)
            if (subMenu.visibility == View.VISIBLE) {
                val arrow = findViewById<ImageView>(R.id.arrowAttendance)
                toggleSubMenu(subMenu, arrow)
            }
            Toast.makeText(this, "Attendance Dashboard", Toast.LENGTH_SHORT).show()
        }

        // Attendance Entry
        findViewById<View>(R.id.menuAttendanceEntry).setOnClickListener {
            val intent = Intent(this, AttendanceActivity::class.java)
            intent.putExtra("CO_ID", getIntent().getIntExtra("CO_ID", 0))
            intent.putExtra("BRANCH_ID", getIntent().getIntExtra("BRANCH_ID", 0))
            startActivity(intent)
        }

        // On Boarding moved to main Dashboard's Attendance submenu

        // Attendance Update menu removed

        // Toggle Production Menu
        findViewById<View>(R.id.headerProduction).setOnClickListener {
            val subMenu = findViewById<View>(R.id.subMenuProduction)
            val arrow = findViewById<ImageView>(R.id.arrowProduction)
            toggleSubMenu(subMenu, arrow)
        }

        // Production Dashboard
        findViewById<View>(R.id.menuProductionDashboard).setOnClickListener {
            startActivity(Intent(this, ProductionDashboardActivity::class.java).putExtra("USER_NAME", findViewById<android.widget.TextView>(R.id.tvUserName).text))
        }

        // Spreader Entry expandable group
        findViewById<View>(R.id.headerSpreaderEntry).setOnClickListener {
            val subMenu = findViewById<View>(R.id.subMenuSpreaderEntry)
            val arrow = findViewById<ImageView>(R.id.arrowSpreaderEntry)
            toggleSubMenu(subMenu, arrow)
        }
        findViewById<View>(R.id.menuProductionEntry).setOnClickListener {
            val i = Intent(this, SpreaderProdEntryActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
        findViewById<View>(R.id.menuIssueEntry).setOnClickListener {
            val i = Intent(this, SpreaderIssueEntryActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }

        findViewById<View>(R.id.menuDrawingMeterEntry).setOnClickListener {
            val i = Intent(this, DrawingMeterEntryActivity::class.java)
            i.putExtra("CO_ID", getIntent().getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", getIntent().getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
        findViewById<View>(R.id.menuSpinningDoffEntry).setOnClickListener {
            val i = Intent(this, SpinningDoffActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }

        // Doff Entry expandable group
        findViewById<View>(R.id.headerDoffEntry).setOnClickListener {
            val subMenu = findViewById<View>(R.id.subMenuDoffEntry)
            subMenu.visibility = if (subMenu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        findViewById<View>(R.id.menuSpellWiseFrameEntry).setOnClickListener {
            val i = Intent(this, SpellWiseFrameEntryActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
        findViewById<View>(R.id.menuSpgDoffEntry).setOnClickListener {
            val i = Intent(this, NewDoffEntryActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
        findViewById<View>(R.id.menuSpgDoffEntry1).setOnClickListener {
            val i = Intent(this, SpgDoffEntry1Activity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
        findViewById<View>(R.id.menuSpgRunningHours).setOnClickListener {
            val i = Intent(this, SpgRunningHoursActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
        findViewById<View>(R.id.menuWindingEntry).setOnClickListener {
            val i = Intent(this, WindingEntryActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
        findViewById<View>(R.id.menuWeavingEntry).setOnClickListener {
            openProductionScreen("Weaving Entry")
        }
        findViewById<View>(R.id.headerFinishingEntry).setOnClickListener {
            val sub = findViewById<View>(R.id.subMenuFinishingEntry)
            sub.visibility = if (sub.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        findViewById<View>(R.id.menuOtherEntries).setOnClickListener {
            val i = Intent(this, OtherEntriesActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
    }


    private fun openProductionScreen(title: String) {
        startActivity(Intent(this, MenuPlaceholderActivity::class.java).putExtra("SCREEN_TITLE", title))
    }

    private fun setupCharts() {
        // Render with empty/zero data first so chart frames are visible immediately
        renderAttendancePieChart(TodayAttendance())
        renderWagesBarChart(emptyList())
        renderLast7DaysLineChart(emptyList())
        renderAbsentBucketsChart(AbsentBuckets())
        renderManMachineChart(emptyList())

        loadDashboardData()
    }

    private fun loadDashboardData() {
        val date = apiDateFmt.format(selectedCalendar.time)
        val coId = intent.getIntExtra("CO_ID", 0).takeIf { it > 0 }
        val branchId = intent.getIntExtra("BRANCH_ID", 0).takeIf { it > 0 }

        RetrofitClient.getApiService(this)
            .getAttendanceDashboard(date, branchId, coId, selectedSpellId)
            .enqueue(object : Callback<AttendanceDashboardResponse> {
                override fun onResponse(
                    call: Call<AttendanceDashboardResponse>,
                    response: Response<AttendanceDashboardResponse>
                ) {
                    val body = response.body()
                    if (!response.isSuccessful || body == null || body.status != "success") {
                        Log.w(TAG, "dashboard load failed: code=${response.code()} msg=${body?.message}")
                        Toast.makeText(
                            this@AttendanceDashboardActivity,
                            "Failed to load dashboard",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    renderAttendancePieChart(body.todayAttendance ?: TodayAttendance())
                    renderWagesBarChart(body.wagesLast7Days ?: emptyList())
                    renderLast7DaysLineChart(body.last7DaysPresent ?: emptyList())
                    renderAbsentBucketsChart(body.absentBuckets ?: AbsentBuckets())
                    renderManMachineChart(body.manMachineLast7Days ?: emptyList())
                }

                override fun onFailure(call: Call<AttendanceDashboardResponse>, t: Throwable) {
                    Log.e(TAG, "dashboard request failed", t)
                    Toast.makeText(
                        this@AttendanceDashboardActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ── Chart 1: Today's Attendance Pie ──────────────────────────────────────
    private fun renderAttendancePieChart(data: TodayAttendance) {
        val chart = findViewById<PieChart>(R.id.chartAttendance)

        val entries = mutableListOf<PieEntry>()
        if (data.present > 0) entries.add(PieEntry(data.present.toFloat(), "Present"))
        if (data.absent  > 0) entries.add(PieEntry(data.absent.toFloat(),  "Absent"))
        if (data.leave   > 0) entries.add(PieEntry(data.leave.toFloat(),   "Leave"))
        if (entries.isEmpty()) entries.add(PieEntry(1f, "No data"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#F44336"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#BDBDBD")
        )
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        chart.data = PieData(dataSet)
        chart.description.isEnabled = false
        chart.setUsePercentValues(false)
        chart.setEntryLabelColor(Color.WHITE)
        chart.setEntryLabelTextSize(11f)
        chart.setHoleColor(Color.WHITE)
        chart.holeRadius = 45f
        chart.transparentCircleRadius = 50f
        chart.centerText = "Today\n(${data.totalEmployees})"
        chart.setCenterTextSize(12f)
        chart.legend.isEnabled = true
        chart.legend.textSize = 11f
        chart.animateY(800)
        chart.invalidate()
    }

    // ── Chart 2: Wages last 7 days ───────────────────────────────────────────
    private fun renderWagesBarChart(data: List<WagesDay>) {
        val chart = findViewById<BarChart>(R.id.chartWages)

        val labels = data.map { it.label ?: "" }
        val entries = data.mapIndexed { i, w -> BarEntry(i.toFloat(), w.amount) }
        val dataSet = BarDataSet(entries, "Wages (amount)")
        dataSet.color = Color.parseColor("#1976D2")
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        chart.data = barData

        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.textSize = 9f
        chart.legend.textSize = 11f
        chart.animateY(800)
        chart.invalidate()
    }

    // ── Chart 3: Last 7 days present ─────────────────────────────────────────
    private fun renderLast7DaysLineChart(data: List<DayPresent>) {
        val chart = findViewById<LineChart>(R.id.chartLast7Days)

        val labels = data.map { it.label ?: "" }
        val entries = data.mapIndexed { i, d -> Entry(i.toFloat(), d.present.toFloat()) }
        val dataSet = LineDataSet(entries, "Present Count")
        dataSet.color = Color.parseColor("#2E7D32")
        dataSet.setCircleColor(Color.parseColor("#2E7D32"))
        dataSet.lineWidth = 2.5f
        dataSet.circleRadius = 4f
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#A5D6A7")

        chart.data = LineData(dataSet)
        chart.description.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.legend.textSize = 11f
        chart.animateX(800)
        chart.invalidate()
    }

    // ── Chart 4: Absent buckets ──────────────────────────────────────────────
    private fun renderAbsentBucketsChart(data: AbsentBuckets) {
        val chart = findViewById<BarChart>(R.id.chartAbsentBuckets)

        val labels = listOf("1-7 days", "8-15 days", "16-30 days", ">30 days")
        val values = floatArrayOf(
            data.range1to7.toFloat(),
            data.range8to15.toFloat(),
            data.range16to30.toFloat(),
            data.over30Days.toFloat()
        )
        val colors = listOf(
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#D32F2F")
        )

        val entries = values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val dataSet = BarDataSet(entries, "Employees")
        dataSet.colors = colors
        dataSet.valueTextSize = 11f

        val barData = BarData(dataSet)
        barData.barWidth = 0.55f
        chart.data = barData

        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.textSize = 10f
        chart.legend.textSize = 11f
        chart.animateY(800)
        chart.invalidate()
    }

    // ── Chart 5: Man vs Machine (last 7 days) – grouped bar ──────────────────
    private fun renderManMachineChart(data: List<ManMachineDay>) {
        val chart = findViewById<BarChart>(R.id.chartManMachine)

        val labels = data.map { it.label ?: "" }

        val handsEntries  = data.mapIndexed { i, d -> BarEntry(i.toFloat(), d.totalHands) }
        val targetEntries = data.mapIndexed { i, d -> BarEntry(i.toFloat(), d.totalTarget) }

        val handsSet = BarDataSet(handsEntries, "Actual").apply {
            color = Color.parseColor("#1976D2")
            valueTextSize = 9f
        }
        val targetSet = BarDataSet(targetEntries, "Target").apply {
            color = Color.parseColor("#FF9800")
            valueTextSize = 9f
        }

        val groupSpace = 0.20f
        val barSpace   = 0.05f
        val barWidth   = 0.35f   // (barWidth + barSpace) * 2 + groupSpace = 1.00

        val barData = BarData(handsSet, targetSet)
        barData.barWidth = barWidth
        chart.data = barData

        chart.description.isEnabled = false
        chart.setFitBars(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.axisMinimum = 0f
        // Ensure left axis still shows a sensible range when all values are zero
        val maxVal = (data.maxOfOrNull { maxOf(it.totalHands, it.totalTarget) } ?: 0f)
        if (maxVal <= 0f) chart.axisLeft.axisMaximum = 10f else chart.axisLeft.resetAxisMaximum()
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.setCenterAxisLabels(true)
        chart.xAxis.textSize = 10f
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.legend.textSize = 11f

        if (labels.isNotEmpty()) {
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = labels.size.toFloat()
            barData.groupBars(0f, groupSpace, barSpace)
        }

        chart.animateY(800)
        chart.invalidate()
    }

    private fun toggleSubMenu(subMenu: View, arrow: View) {
        if (subMenu.visibility == View.VISIBLE) {
            subMenu.visibility = View.GONE
            (arrow as? ImageView)?.setImageResource(R.drawable.ic_expand_more)
        } else {
            subMenu.visibility = View.VISIBLE
            (arrow as? ImageView)?.setImageResource(R.drawable.ic_expand_less)
        }
    }
}
