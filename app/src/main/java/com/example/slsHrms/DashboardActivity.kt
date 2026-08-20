package com.example.slsHrms

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.slsHrms.api.BranchData
import com.example.slsHrms.api.BranchResponse
import com.example.slsHrms.api.CardTrendResponse
import com.example.slsHrms.api.CompanyData
import com.example.slsHrms.api.CompanyResponse
import com.example.slsHrms.api.ApiConfig
import com.example.slsHrms.adapter.DeptWiseAdapter
import com.example.slsHrms.api.DashboardStatsResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.api.Shift
import com.example.slsHrms.api.ShiftResponse
import com.example.slsHrms.databinding.ActivityDashboardBinding
import com.example.slsHrms.permissions.PermissionManager
import com.example.slsHrms.sync.SyncEngine
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.app.AlertDialog
import android.graphics.Color
import android.widget.ProgressBar
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var deptWiseAdapter: DeptWiseAdapter
    private val calendar = Calendar.getInstance()
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private var selectedDateApi: String = ""   // yyyy-MM-dd for API
    private var selectedDateDisplay: String = "" // dd-MM-yyyy for UI
    private lateinit var companyAdapter: ArrayAdapter<CompanyData>
    private lateinit var branchAdapter: ArrayAdapter<BranchData>
    private lateinit var spellAdapter: ArrayAdapter<Shift>
    private var selectedCompanyId: Int = 0
    private var selectedBranchId: Int = 0
    private var selectedSpellId: Int? = null
    private var allDepartments: List<com.example.slsHrms.api.DeptWiseStat> = emptyList() // Store all departments
    private var showOnlyPresentDepts: Boolean = false // Toggle filter state
    private var isInitializing: Boolean = true // Track if spinners are being initialized

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedDateApi = apiDateFormat.format(calendar.time)
        selectedDateDisplay = displayDateFormat.format(calendar.time)

        // Show the build version (auto-incremented per APK build) in the toolbar.
        binding.tvAppVersion.text = try {
            "v" + packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) { "" }

        setupWelcome()
        setupLogout()
        setupDatePicker()
        setupSpellSpinner()
        setupCompanyAndBranchSpinners()
        setupAttendanceMenu()
        setupProductionMenu()
        setupDeptWiseRecyclerView()
        setupCardClicks()
        applyMenuPermissions()

        binding.root.post {
            isInitializing = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh stats when returning from other activities (only if branch already selected)
        if (selectedBranchId > 0) {
            loadDashboardStats()
        }
        showOfflineReadiness()
    }

    /**
     * Two things the operator has to know before walking away from Wi-Fi:
     * whether the permission cache has gone stale, and whether this branch's
     * employee data has been downloaded at all (the first-run gate — offline
     * face matching cannot work without it).
     */
    private fun showOfflineReadiness() {
        PermissionManager.stalenessNotice(this)?.let {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
        }
        SyncEngine.outdatedBuild(this)?.let { required ->
            Toast.makeText(
                this,
                "This app build is out of date — update to $required or newer. " +
                    "Old builds can fill the sync queue with rejected records.",
                Toast.LENGTH_LONG
            ).show()
        }
        if (selectedBranchId <= 0) return
        kotlin.concurrent.thread {
            val faces = com.example.slsHrms.face.FaceGallery.countFor(this, selectedBranchId)
            if (faces > 0) return@thread
            // "0 faces" has four different causes needing four different people
            // to act; SyncEngine works out which one and names the actual fix.
            // Nag only when the operator can do something about it — telling a
            // shop floor about a server backfill every time they open the app is
            // noise they cannot act on.
            val readiness = SyncEngine.faceReadiness(this, faces)
            if (readiness == SyncEngine.FaceReadiness.NEEDS_BACKFILL ||
                readiness == SyncEngine.FaceReadiness.SERVER_NOT_UPDATED
            ) return@thread
            val message = SyncEngine.faceReadinessMessage(this, faces, selectedBranchId)
            runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
        }
    }

    // ── Welcome Section ──────────────────────────────────────────

    private fun setupWelcome() {
        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        binding.tvUserName.text = userName
        binding.tvWelcome.text = "Welcome to MIS System"
    }

    private fun setupCompanyAndBranchSpinners() {
        companyAdapter = ArrayAdapter(this, R.layout.spinner_item_black, mutableListOf())
        companyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        binding.spCompany.adapter = companyAdapter

        branchAdapter = ArrayAdapter(this, R.layout.spinner_item_black, mutableListOf())
        branchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        binding.spBranch.adapter = branchAdapter

        binding.spCompany.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val company = companyAdapter.getItem(position)
                selectedCompanyId = company?.id ?: 0
                // The download needs this to warm the same department URL the
                // attendance screen asks for (?co_id=..&branch_id=..).
                ActiveBranch.setCompany(this@DashboardActivity, selectedCompanyId)
                updateCompanyLogo(company?.logo)
                // Clear the branch dropdown immediately, then repopulate for the
                // selected company (scoped to co_id + user_id).
                setBranchList(emptyList())
                if (selectedCompanyId > 0) {
                    loadBranches(selectedCompanyId)
                }
                // Only reload if not during initial setup
                if (!isInitializing) {
                    // Hide department section when company changes
                    binding.layoutDeptWise.visibility = View.GONE
                    // Reload dashboard with new company
                    loadDashboardStats()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.spBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBranchId = branchAdapter.getItem(position)?.id ?: 0
                // Tell the sync layer which branch we are working in — it decides
                // which face gallery and which masters to keep downloaded.
                ActiveBranch.set(this@DashboardActivity, selectedBranchId)
                // Clear the spell dropdown immediately, then repopulate for the branch.
                setSpellList(emptyList())
                if (selectedBranchId > 0) {
                    loadSpells(selectedBranchId)
                }
                // Only reload if not during initial setup
                if (!isInitializing) {
                    // Hide department section when branch changes
                    binding.layoutDeptWise.visibility = View.GONE
                    // Reload dashboard with new branch
                    loadDashboardStats()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        setCompanyList(emptyList())
        setBranchList(emptyList())
        loadCompanies()
    }

    /** Logged-in user id saved by LoginActivity; used to scope companies/branches. */
    private fun currentUserId(): Int =
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).getInt("user_id", 0)

    private fun loadCompanies() {
        RetrofitClient.getApiService(this).getCompanies(currentUserId()).enqueue(object : Callback<CompanyResponse> {
            override fun onResponse(call: Call<CompanyResponse>, response: Response<CompanyResponse>) {
                if (!response.isSuccessful) {
                    loadCompaniesLegacy()
                    return
                }

                val companies = response.body()?.companyList().orEmpty().filter { it.id != null }
                setCompanyList(companies)
                if (companies.isNotEmpty()) {
                    updateCompanyLogo(companies.first().logo)
                    companies.first().id?.let { loadBranches(it) }
                }
            }

            override fun onFailure(call: Call<CompanyResponse>, t: Throwable) {
                loadCompaniesLegacy()
            }
        })
    }

    private fun loadCompaniesLegacy() {
        RetrofitClient.getApiService(this).getCompaniesLegacy().enqueue(object : Callback<CompanyResponse> {
            override fun onResponse(call: Call<CompanyResponse>, response: Response<CompanyResponse>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@DashboardActivity, "Failed to load companies", Toast.LENGTH_SHORT).show()
                    return
                }

                val companies = response.body()?.companyList().orEmpty().filter { it.id != null }
                setCompanyList(companies)
                if (companies.isNotEmpty()) {
                    updateCompanyLogo(companies.first().logo)
                    companies.first().id?.let { loadBranches(it) }
                }
            }

            override fun onFailure(call: Call<CompanyResponse>, t: Throwable) {
                Toast.makeText(this@DashboardActivity, "Company load error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadBranches(companyId: Int) {
        RetrofitClient.getApiService(this).getBranches(companyId, currentUserId()).enqueue(object : Callback<BranchResponse> {
            override fun onResponse(call: Call<BranchResponse>, response: Response<BranchResponse>) {
                if (!response.isSuccessful) {
                    setBranchList(emptyList())
                    return
                }
                val branches = response.body()?.branchList().orEmpty().filter { it.id != null }
                if (branches.isNotEmpty()) {
                    setBranchList(branches)
                } else {
                    loadBranchesByCoId(companyId)
                }
            }

            override fun onFailure(call: Call<BranchResponse>, t: Throwable) {
                loadBranchesByCoId(companyId)
            }
        })
    }

    private fun loadBranchesByCoId(companyId: Int) {
        RetrofitClient.getApiService(this).getBranchesByCoId(companyId, currentUserId()).enqueue(object : Callback<BranchResponse> {
            override fun onResponse(call: Call<BranchResponse>, response: Response<BranchResponse>) {
                if (!response.isSuccessful) {
                    loadBranchesLegacy(companyId)
                    return
                }
                val branches = response.body()?.branchList().orEmpty().filter { it.id != null }
                if (branches.isNotEmpty()) {
                    setBranchList(branches)
                } else {
                    loadBranchesLegacy(companyId)
                }
            }

            override fun onFailure(call: Call<BranchResponse>, t: Throwable) {
                loadBranchesLegacy(companyId)
            }
        })
    }

    private fun loadBranchesLegacy(companyId: Int) {
        RetrofitClient.getApiService(this).getBranchesLegacy(companyId).enqueue(object : Callback<BranchResponse> {
            override fun onResponse(call: Call<BranchResponse>, response: Response<BranchResponse>) {
                if (!response.isSuccessful) {
                    setBranchList(emptyList())
                    return
                }
                val branches = response.body()?.branchList().orEmpty().filter { it.id != null }
                setBranchList(branches)
            }

            override fun onFailure(call: Call<BranchResponse>, t: Throwable) {
                setBranchList(emptyList())
            }
        })
    }

    private fun setCompanyList(companies: List<CompanyData>) {
        val finalList = if (companies.isNotEmpty()) companies else listOf(CompanyData(0, "No Company", null))
        companyAdapter.clear()
        companyAdapter.addAll(finalList)
        companyAdapter.notifyDataSetChanged()
        // Capture the company here as well as in the spinner listener:
        // notifyDataSetChanged does not re-fire onItemSelected, so repopulating
        // the adapter would otherwise leave the stored company at 0 — and the
        // download would warm the wrong departments URL. setBranchList below
        // takes the same belt-and-braces approach for the branch.
        ActiveBranch.setCompany(this, finalList.firstOrNull()?.id ?: 0)
    }

    private fun setBranchList(branches: List<BranchData>) {
        val finalList = if (branches.isNotEmpty()) branches else listOf(BranchData(0, null, "No Branch"))
        branchAdapter.clear()
        branchAdapter.addAll(finalList)
        branchAdapter.notifyDataSetChanged()
        // Immediately capture the first branch ID so it's ready before any user interaction
        val newBranchId = finalList.firstOrNull()?.id ?: 0
        val branchChanged = newBranchId != selectedBranchId
        selectedBranchId = newBranchId
        ActiveBranch.set(this, selectedBranchId)
        if (selectedBranchId > 0) {
            loadSpells(selectedBranchId)
        } else {
            setSpellList(emptyList())
        }
        // Reload dashboard with branch filter once a real branch is available
        if (selectedBranchId > 0 && branchChanged) {
            binding.layoutDeptWise.visibility = View.GONE
            loadDashboardStats()
        }
    }

    private fun setupSpellSpinner() {
        spellAdapter = ArrayAdapter(this, R.layout.spinner_item_black, mutableListOf())
        spellAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        binding.spSpell.adapter = spellAdapter

        binding.spSpell.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spell = spellAdapter.getItem(position)
                selectedSpellId = if ((spell?.id ?: 0) > 0) spell?.id else null
                if (!isInitializing) {
                    binding.layoutDeptWise.visibility = View.GONE
                    loadDashboardStats()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        setSpellList(emptyList())
    }

    private fun loadSpells(branchId: Int) {
        RetrofitClient.getApiService(this).getShifts(branchId).enqueue(object : Callback<ShiftResponse> {
            override fun onResponse(call: Call<ShiftResponse>, response: Response<ShiftResponse>) {
                val shifts = if (response.isSuccessful) response.body()?.shifts.orEmpty() else emptyList()
                setSpellList(shifts)
            }
            override fun onFailure(call: Call<ShiftResponse>, t: Throwable) {
                setSpellList(emptyList())
            }
        })
    }

    private fun setSpellList(spells: List<Shift>) {
        val allSpell = Shift(id = 0, name = "All Spells")
        val finalList = listOf(allSpell) + spells
        spellAdapter.clear()
        spellAdapter.addAll(finalList)
        spellAdapter.notifyDataSetChanged()
        selectedSpellId = null
    }

    private fun updateCompanyLogo(logoValue: String?) {
        if (logoValue.isNullOrBlank()) {
            binding.ivCompanyLogo.setImageResource(R.drawable.logo_vow)
            return
        }

        val logo = logoValue.trim()
        when {
            logo.startsWith("http://") || logo.startsWith("https://") -> loadLogoFromUrl(logo)
            logo.startsWith("/") || logo.startsWith("images/") || logo.startsWith("uploads/") -> {
                // Logo is fetched outside Retrofit, so it can't use the
                // X-Tenant header. Carry the tenant as a path prefix instead —
                // the backend still supports that style for static assets.
                val baseUrl = ApiConfig.getBaseUrl(this)
                val tenant = ApiConfig.getTenant(this)
                val tenantPrefix = if (tenant.isNotEmpty()) "$tenant/" else ""
                val fullUrl = baseUrl.trimEnd('/') + "/" + tenantPrefix + logo.trimStart('/')
                loadLogoFromUrl(fullUrl)
            }
            else -> {
                val base64Part = if (logo.contains(",")) logo.substringAfter(",") else logo
                try {
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeStream(ByteArrayInputStream(bytes))
                    if (bitmap != null) {
                        binding.ivCompanyLogo.setImageBitmap(bitmap)
                    } else {
                        binding.ivCompanyLogo.setImageResource(R.drawable.logo_vow)
                    }
                } catch (_: Exception) {
                    binding.ivCompanyLogo.setImageResource(R.drawable.logo_vow)
                }
            }
        }
    }

    private fun loadLogoFromUrl(url: String) {
        Thread {
            try {
                val stream = URL(url).openStream()
                val bitmap = BitmapFactory.decodeStream(stream)
                runOnUiThread {
                    if (bitmap != null) {
                        binding.ivCompanyLogo.setImageBitmap(bitmap)
                    } else {
                        binding.ivCompanyLogo.setImageResource(R.drawable.logo_vow)
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    binding.ivCompanyLogo.setImageResource(R.drawable.logo_vow)
                }
            }
        }.start()
    }

    // ── Logout ───────────────────────────────────────────────────

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            // Un-uploaded records live in app-private storage. Logging out (and
            // worse, clearing app data afterwards) would destroy work someone
            // actually did, so it is blocked until the queue drains.
            kotlin.concurrent.thread {
                val pending = runCatching { SyncEngine.pendingCount(this) }.getOrDefault(0) +
                    runCatching { SyncEngine.failedCount(this) }.getOrDefault(0)
                runOnUiThread {
                    if (pending > 0) {
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("$pending record(s) not uploaded")
                            .setMessage(
                                "These entries only exist on this device. Sync them " +
                                    "before logging out — do not clear app data until " +
                                    "the Sync Center reads zero."
                            )
                            .setPositiveButton("Open Sync Center") { _, _ ->
                                startActivity(Intent(this, SyncCenterActivity::class.java))
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
                        doLogout()
                    }
                }
            }
        }
    }

    private fun doLogout() {
        PermissionManager.clear(this)
        // Face embeddings are biometric data — they do not outlive the session.
        // The outbox is intentionally not touched (it is empty at this point).
        com.example.slsHrms.local.HrmsDatabase.wipeCaches(this)
        com.example.slsHrms.face.FaceGallery.invalidate()
        com.example.slsHrms.sync.OfflineEmployees.invalidate()
        RetrofitClient.clearCache(this)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ── Card Click Handlers ──────────────────────────────────────

    private fun setupCardClicks() {
        binding.cardPresent.setOnClickListener { showDepartmentsWithPresent() }
        binding.cardJute.setOnClickListener    { openWithBranch(JuteReceivedActivity::class.java) }
        binding.cardSpg.setOnClickListener     { openWithBranch(SpinningDoffActivity::class.java) }
        binding.cardWinding.setOnClickListener { openWithBranch(WindingEntryActivity::class.java) }
        binding.cardOthers.setOnClickListener  { openWithBranch(OtherEntriesActivity::class.java) }
        binding.cardBales.setOnClickListener   { openWithBranch(BalesProductionEntryActivity::class.java) }

        // 7-day trend icons
        binding.btnTrendHands.setOnClickListener   { showCardTrend("hands",   "No of Hands", 0xFF2E7D32.toInt()) }
        binding.btnTrendJute.setOnClickListener    { showCardTrend("jute",    "Jute",        0xFF1565C0.toInt()) }
        binding.btnTrendSpg.setOnClickListener     { showCardTrend("spg",     "Spg Prod",    0xFF6A1B9A.toInt()) }
        binding.btnTrendWinding.setOnClickListener { showCardTrend("winding", "Winding",     0xFF00838F.toInt()) }
        binding.btnTrendOthers.setOnClickListener  { showCardTrend("others",  "Others",      0xFFEF6C00.toInt()) }
        binding.btnTrendBales.setOnClickListener   { showCardTrend("bales",   "Bales",       0xFFC62828.toInt()) }
    }

    private fun openWithBranch(target: Class<*>) {
        val i = Intent(this, target)
        i.putExtra("CO_ID", selectedCompanyId)
        i.putExtra("BRANCH_ID", selectedBranchId)
        startActivity(i)
    }

    // ── 7-day trend bar chart dialog ─────────────────────────────
    private fun showCardTrend(metric: String, title: String, color: Int) {
        val view  = layoutInflater.inflate(R.layout.dialog_card_trend, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvTrendTitle)
        val chart   = view.findViewById<BarChart>(R.id.chartTrend)
        val pb      = view.findViewById<ProgressBar>(R.id.pbTrend)
        val tvEmpty = view.findViewById<TextView>(R.id.tvTrendEmpty)
        val btnClose = view.findViewById<ImageView>(R.id.btnTrendClose)
        val scrollChips = view.findViewById<android.widget.HorizontalScrollView>(R.id.scrollChips)
        val llChips     = view.findViewById<android.widget.LinearLayout>(R.id.llTrendChips)

        tvTitle.text = "$title — Last 7 Days"
        view.findViewById<android.view.View>(R.id.tvTrendTitle)
            .let { (it.parent as android.view.ViewGroup).setBackgroundColor(color) }

        val dlg = AlertDialog.Builder(this).setView(view).setCancelable(true).create()
        btnClose.setOnClickListener { dlg.dismiss() }

        chart.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        pb.visibility = View.VISIBLE
        scrollChips.visibility = View.GONE

        RetrofitClient.getApiService(this).getDashboardCardTrend(
            metric   = metric,
            branchId = if (selectedBranchId > 0) selectedBranchId else null,
            endDate  = selectedDateApi
        ).enqueue(object : Callback<CardTrendResponse> {
            override fun onResponse(call: Call<CardTrendResponse>, response: Response<CardTrendResponse>) {
                pb.visibility = View.GONE
                val body = response.body()
                val days = body?.days.orEmpty()
                val series = body?.series.orEmpty()
                val labels = days.map { it.date?.substring(5) ?: "" }
                val hasData = days.any {
                    (it.value ?: 0.0) != 0.0 || (it.values?.any { v -> v != 0.0 } == true)
                }
                if (days.isEmpty() || !hasData) {
                    tvEmpty.visibility = View.VISIBLE
                    return
                }
                chart.visibility = View.VISIBLE
                if (series.size > 1) {
                    val matrix = days.map { it.values ?: emptyList() }
                    setupSeriesChips(llChips, scrollChips, chart, labels, series, matrix, color)
                } else {
                    renderTrendChart(chart, labels.zip(days.map { it.value ?: 0.0 }), color)
                }
            }
            override fun onFailure(call: Call<CardTrendResponse>, t: Throwable) {
                pb.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = "Failed to load trend"
            }
        })
        dlg.show()
    }

    private fun setupSeriesChips(
        container: android.widget.LinearLayout,
        scroll: android.widget.HorizontalScrollView,
        chart: BarChart,
        labels: List<String>,
        seriesNames: List<String>,
        matrix: List<List<Double>>,
        primaryColor: Int
    ) {
        val palette = listOf(
            primaryColor,
            shade(primaryColor, 0.70f),
            shade(primaryColor, 0.45f),
            shade(primaryColor, 0.25f)
        )
        val seriesColors = seriesNames.mapIndexed { i, _ -> palette.getOrElse(i) { primaryColor } }

        // Selected series: -1 means "show all"
        var selected = -1
        scroll.visibility = View.VISIBLE
        container.removeAllViews()

        val chips = mutableListOf<TextView>()
        val dp = resources.displayMetrics.density
        val mkChip: (Int, String) -> TextView = { idx, name ->
            TextView(this).apply {
                text = if (idx == -1) "All" else name
                textSize = 12f
                setPadding((14 * dp).toInt(), (6 * dp).toInt(),
                           (14 * dp).toInt(), (6 * dp).toInt())
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, (6 * dp).toInt(), 0)
                layoutParams = lp
                isClickable = true
                isFocusable = true
            }
        }

        fun paintChips() {
            chips.forEachIndexed { i, chip ->
                val chipIdx = i - 1
                val active = (selected == chipIdx)
                val chipColor =
                    if (chipIdx == -1) primaryColor
                    else seriesColors.getOrElse(chipIdx) { primaryColor }
                chip.setBackgroundColor(if (active) chipColor else 0xFFE0E0E0.toInt())
                chip.setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xFF424242.toInt())
                chip.setTypeface(null,
                    if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }
        }

        // "All" chip first, then one per series
        chips += mkChip(-1, "All").also { container.addView(it) }
        seriesNames.forEachIndexed { idx, name ->
            chips += mkChip(idx, name).also { container.addView(it) }
        }
        chips.forEachIndexed { i, chip ->
            val chipIdx = i - 1
            chip.setOnClickListener {
                selected = if (selected == chipIdx) -1 else chipIdx
                paintChips()
                renderTrendByFilter(chart, labels, seriesNames, matrix, seriesColors, selected)
            }
        }
        paintChips()
        renderTrendByFilter(chart, labels, seriesNames, matrix, seriesColors, selected)
    }

    private fun renderTrendByFilter(
        chart: BarChart,
        labels: List<String>,
        seriesNames: List<String>,
        matrix: List<List<Double>>,
        seriesColors: List<Int>,
        selected: Int
    ) {
        if (selected == -1) {
            renderGroupedBars(chart, labels, seriesNames, matrix, seriesColors)
        } else {
            val data = labels.mapIndexed { i, lbl ->
                lbl to (matrix.getOrNull(i)?.getOrNull(selected) ?: 0.0)
            }
            chart.fitScreen()
            renderTrendChart(chart, data, seriesColors.getOrElse(selected) { 0xFF1565C0.toInt() })
        }
    }

    private fun renderTrendChart(chart: BarChart, data: List<Pair<String, Double>>, color: Int) {
        val labels  = data.map { it.first }
        val entries = data.mapIndexed { i, (_, v) -> BarEntry(i.toFloat(), v.toFloat()) }
        val dataSet = BarDataSet(entries, "").apply {
            this.color = color
            valueTextSize = 10f
        }
        chart.data = BarData(dataSet).apply { barWidth = 0.6f }
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setFitBars(true)
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.textSize = 10f
        // Clear any grouped-bar centering / axis bounds left from a previous render.
        chart.xAxis.setCenterAxisLabels(false)
        chart.xAxis.resetAxisMinimum()
        chart.xAxis.resetAxisMaximum()
        chart.animateY(700)
        chart.invalidate()
    }

    private fun renderGroupedBars(
        chart: BarChart,
        labels: List<String>,
        seriesNames: List<String>,
        matrix: List<List<Double>>,
        seriesColors: List<Int>
    ) {
        val dataSets = seriesNames.mapIndexed { sIdx, name ->
            val entries = matrix.mapIndexed { dIdx, row ->
                BarEntry(dIdx.toFloat(), (row.getOrNull(sIdx) ?: 0.0).toFloat())
            }
            BarDataSet(entries, name).apply {
                color = seriesColors.getOrElse(sIdx) { 0xFF1565C0.toInt() }
                valueTextSize = 9f
            }
        }
        val groupCount = labels.size
        val groupSpace = 0.18f
        val barSpace   = 0.04f
        val barWidth   = (1f - groupSpace) / seriesNames.size - barSpace

        chart.data = BarData(dataSets).apply { this.barWidth = barWidth }
        chart.description.isEnabled = false
        // Chips above the chart drive series selection — hide the built-in legend.
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.setCenterAxisLabels(true)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.textSize = 10f
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = groupCount.toFloat()
        chart.setFitBars(false)
        chart.groupBars(0f, groupSpace, barSpace)
        chart.animateY(700)
        chart.invalidate()
    }

    private fun shade(color: Int, factor: Float): Int {
        val r = (Color.red(color)   * factor + 255 * (1 - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor + 255 * (1 - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color)  * factor + 255 * (1 - factor)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun openAttendanceReport(departmentId: Int = -1) {
        val intent = Intent(this, AttendanceReportActivity::class.java)
        intent.putExtra("FROM_DATE", selectedDateDisplay)
        intent.putExtra("TO_DATE", selectedDateDisplay)
        intent.putExtra("AUTO_SEARCH", true)
        intent.putExtra("CO_ID", selectedCompanyId)
        intent.putExtra("BRANCH_ID", selectedBranchId)
        if (departmentId > 0) {
            intent.putExtra("DEPARTMENT_ID", departmentId)
        }
        startActivity(intent)
    }

    // ── Show / Hide Departments Where Employees Are Present (toggle) ──

    private fun showDepartmentsWithPresent() {
        // Toggle: if section is already visible, hide it
        if (binding.layoutDeptWise.visibility == View.VISIBLE) {
            binding.layoutDeptWise.visibility = View.GONE
            return
        }

        val filteredDepts = allDepartments.filter { it.present > 0 }
        android.util.Log.d("DashboardActivity",
            "showDepartmentsWithPresent: total=${allDepartments.size}, withPresent=${filteredDepts.size}")

        binding.layoutDeptWise.visibility = View.VISIBLE

        if (filteredDepts.isNotEmpty()) {
            binding.tvDeptWiseTitle.text =
                "Departments with Present Attendance (${filteredDepts.size})"
            deptWiseAdapter.updateList(filteredDepts)
            binding.rvDeptWise.visibility = View.VISIBLE
            binding.tvDeptWiseEmpty.visibility = View.GONE
        } else {
            binding.tvDeptWiseTitle.text = "Departments with Present Attendance"
            binding.rvDeptWise.visibility = View.GONE
            binding.tvDeptWiseEmpty.visibility = View.VISIBLE
            Toast.makeText(this, "No departments with present attendance", Toast.LENGTH_SHORT).show()
        }

        // Scroll the section into view
        binding.layoutDeptWise.post {
            binding.layoutDeptWise.parent?.requestChildFocus(binding.layoutDeptWise, binding.layoutDeptWise)
        }
    }

    // ── Date Picker ──────────────────────────────────────────────

    private fun setupDatePicker() {
        binding.tvSelectedDate.text = selectedDateDisplay
        binding.btnDatePicker.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedDateApi = apiDateFormat.format(calendar.time)
                    selectedDateDisplay = displayDateFormat.format(calendar.time)
                    binding.tvSelectedDate.text = selectedDateDisplay
                    loadDashboardStats()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // ── Load Dashboard Stats ─────────────────────────────────────

    private fun loadDashboardStats() {
        // TEMP: /dashboard-stats endpoint is disabled on the backend, so don't
        // call it. Hide the stats spinner and return early.
        binding.progressStats.visibility = View.GONE
        if (true) return

        binding.progressStats.visibility = View.VISIBLE

        RetrofitClient.getApiService(this).getDashboardStats(
            selectedDateApi,
            if (selectedBranchId > 0) selectedBranchId else null,
            if (selectedCompanyId > 0) selectedCompanyId else null,
            selectedSpellId
        )
            .enqueue(object : Callback<DashboardStatsResponse> {
                override fun onResponse(
                    call: Call<DashboardStatsResponse>,
                    response: Response<DashboardStatsResponse>
                ) {
                    binding.progressStats.visibility = View.GONE

                    if (response.isSuccessful) {
                        val stats = response.body() ?: return
                        android.util.Log.d("DashboardActivity", "API Response received: $stats")
                        
                        binding.tvStatPresent.text = stats.totalPresent.toString()
                        binding.tvStatFace.text    = stats.presentFace.toString()
                        binding.tvStatManual.text  = stats.presentManual.toString()

                        binding.tvJuteRecv.text    = stats.juteRecv.toString()
                        binding.tvJuteIssue.text   = stats.juteIssue.toString()
                        binding.tvJuteStock.text   = stats.juteStock.toString()

                        binding.tvSpgProd.text     = stats.spgProd.toString()
                        binding.tvSpgEff.text      = stats.spgEff.toString()
                        binding.tvSpgRunEff.text   = stats.spgRunEff.toString()
                        binding.tvSpgPrdFrame.text = stats.spgPrdFrame.toString()

                        binding.tvWdgProd.text     = stats.wdgProd.toString()
                        binding.tvWdgWinders.text  = stats.wdgWinders.toString()
                        binding.tvWdgAvgProd.text  = stats.wdgAvgProd.toString()

                        binding.tvOthWeaving.text  = stats.othWeaving.toString()
                        binding.tvOthHemming.text  = stats.othHemming.toString()
                        binding.tvOthHeracle.text  = stats.othHeracle.toString()
                        binding.tvOthHsewer.text   = stats.othHsewer.toString()

                        binding.tvBalesProd.text   = stats.balesProd.toString()
                        binding.tvBalesIssue.text  = stats.balesIssue.toString()
                        binding.tvBalesStock.text  = stats.balesStock.toString()

                        // Department-wise breakdown - use department_wise (all departments)
                        // and filter to present > 0 client-side. department_master for Master card.
                        val deptWiseList = stats.departmentWise ?: emptyList()
                        val deptPresentList = (stats.departmentPresent ?: emptyList())
                            .ifEmpty { deptWiseList.filter { it.present > 0 } }
                        val deptMasterList = stats.departmentMaster ?: emptyList()
                        
                        android.util.Log.d("DashboardActivity", "departmentWise size: ${deptWiseList.size}")
                        android.util.Log.d("DashboardActivity", "departmentPresent size: ${deptPresentList.size}")
                        android.util.Log.d("DashboardActivity", "departmentPresent: $deptPresentList")
                        android.util.Log.d("DashboardActivity", "departmentMaster size: ${deptMasterList.size}")

                        // Prefer the full department_wise list so all departments with present>0
                        // are available when user clicks the Present card.
                        allDepartments = if (deptWiseList.isNotEmpty()) deptWiseList else deptPresentList
                        showOnlyPresentDepts = false // Reset filter state

                        val initialList = allDepartments.filter { it.present > 0 }
                        if (initialList.isNotEmpty()) {
                            binding.rvDeptWise.visibility = View.VISIBLE
                            binding.tvDeptWiseEmpty.visibility = View.GONE
                            deptWiseAdapter.updateList(initialList)
                        } else {
                            binding.rvDeptWise.visibility = View.GONE
                            binding.tvDeptWiseEmpty.visibility = View.VISIBLE
                        }
                        // The parent layout (layoutDeptWise) remains hidden until user clicks Present card
                        binding.layoutDeptWise.visibility = View.GONE
                        android.util.Log.d("DashboardActivity", "Dashboard stats loaded successfully")
                    } else {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Failed to load stats",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<DashboardStatsResponse>, t: Throwable) {
                    binding.progressStats.visibility = View.GONE
                    Toast.makeText(
                        this@DashboardActivity,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // ── Department-Wise RecyclerView ─────────────────────────────

    private fun setupDeptWiseRecyclerView() {
        deptWiseAdapter = DeptWiseAdapter(emptyList()) { dept ->
            openAttendanceReport(dept.departmentId)
        }
        binding.rvDeptWise.layoutManager = LinearLayoutManager(this)
        binding.rvDeptWise.adapter = deptWiseAdapter
    }

    // ── Attendance Menu (expand/collapse) ────────────────────────

    private fun setupAttendanceMenu() {
        binding.headerAttendance.setOnClickListener {
            toggleSubMenu(binding.subMenuAttendance, binding.arrowAttendance)
        }

        binding.menuAttendanceDashboard.setOnClickListener {
            val intent = Intent(this, AttendanceDashboardActivity::class.java)
            intent.putExtra("USER_NAME", binding.tvUserName.text?.toString() ?: "")
            intent.putExtra("CO_ID", selectedCompanyId)
            intent.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(intent)
        }

        binding.menuOnBoarding.setOnClickListener {
            val intent = Intent(this, OnBoardingActivity::class.java)
            intent.putExtra("CO_ID", selectedCompanyId)
            intent.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(intent)
        }

        binding.menuAttendanceEntry.setOnClickListener {
            val intent = Intent(this, AttendanceActivity::class.java)
            intent.putExtra("CO_ID", selectedCompanyId)
            intent.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(intent)
        }

        binding.menuAttendanceReports.setOnClickListener {
            val intent = Intent(this, AttendanceReportsActivity::class.java)
            intent.putExtra("CO_ID", selectedCompanyId)
            intent.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(intent)
        }

        binding.headerOtherEntries.setOnClickListener {
            toggleSubMenu(binding.subMenuOtherEntries, binding.arrowOtherEntries)
        }

        binding.menuLeaveEntries.setOnClickListener {
            val intent = Intent(this, LeaveEntryActivity::class.java)
            intent.putExtra("CO_ID", selectedCompanyId)
            intent.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(intent)
        }
    }

    private fun setupProductionMenu() {
        binding.headerProduction.setOnClickListener {
            toggleSubMenu(binding.subMenuProduction, binding.arrowProduction)
        }

        // Jute expandable group
        binding.headerJute.setOnClickListener {
            toggleSubMenu(binding.subMenuJute, binding.arrowJute)
        }
        binding.menuJuteReceived.setOnClickListener {
            val i = Intent(this, JuteReceivedActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuAssortingEntry.setOnClickListener {
            val i = Intent(this, AssortingEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }

        // Spreader Entry expandable group
        binding.headerSpreaderEntry.setOnClickListener {
            toggleSubMenu(binding.subMenuSpreaderEntry, binding.arrowSpreaderEntry)
        }
        binding.menuProductionEntry.setOnClickListener {
            val i = Intent(this, SpreaderProdEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuIssueEntry.setOnClickListener {
            val i = Intent(this, SpreaderIssueEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }

        binding.menuDrawingMeterEntry.setOnClickListener {
            val i = Intent(this, DrawingMeterEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuSpinningDoffEntry.setOnClickListener {
            val i = Intent(this, SpinningDoffActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }

        // Doff Entry expandable group
        binding.headerDoffEntry.setOnClickListener {
            toggleSubMenu(binding.subMenuDoffEntry, binding.arrowDoffEntry)
        }
        binding.menuSpellWiseFrameEntry.setOnClickListener {
            val i = Intent(this, SpellWiseFrameEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuSpgDoffEntry.setOnClickListener {
            val i = Intent(this, NewDoffEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuSpgDoffEntry1.setOnClickListener {
            val i = Intent(this, SpgDoffEntry1Activity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuSpgRunningHours.setOnClickListener {
            val i = Intent(this, SpgRunningHoursActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        // Winding Entry expandable group
        binding.headerWindingEntry.setOnClickListener {
            toggleSubMenu(binding.subMenuWindingEntry, binding.arrowWindingEntry)
        }
        binding.menuWindingEntry.setOnClickListener {
            val i = Intent(this, WindingEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuContWindingEntry.setOnClickListener {
            val i = Intent(this, ContWindingEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuWeavingEntry.setOnClickListener {
            openProductionScreen("Weaving Entry")
        }
        binding.headerFinishingEntry.setOnClickListener {
            toggleSubMenu(binding.subMenuFinishingEntry, binding.arrowFinishingEntry)
        }
        binding.menuOtherEntries.setOnClickListener {
            val i = Intent(this, OtherEntriesActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuBalesProductionEntry.setOnClickListener {
            val i = Intent(this, BalesProductionEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuBalesIssueEntry.setOnClickListener {
            val i = Intent(this, BalesIssueEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
        binding.menuWeightEntry.setOnClickListener {
            val i = Intent(this, WeightEntryActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }

        // Stocks expandable group
        binding.headerStocks.setOnClickListener {
            toggleSubMenu(binding.subMenuStocks, binding.arrowStocks)
        }
        binding.menuRollStock.setOnClickListener {
            val i = Intent(this, RollStockActivity::class.java)
            i.putExtra("CO_ID", selectedCompanyId)
            i.putExtra("BRANCH_ID", selectedBranchId)
            startActivity(i)
        }
    }

    private fun openProductionScreen(title: String) {
        startActivity(Intent(this, MenuPlaceholderActivity::class.java).putExtra("SCREEN_TITLE", title))
    }

    // ── Dynamic menu permissions ─────────────────────────────────
    //
    // Hides each dashboard menu (cards, group headers, leaves) when
    // the logged-in user has no can_view on the matching menu_key.
    // The menu_key values map 1:1 to rows seeded into the `menus`
    // table by permissions_routes.init_permissions_db().
    private fun applyMenuPermissions() {
        // Group header + its sub-menu hidden together (no wrapping LinearLayout).
        data class GroupViews(val header: View?, val subMenu: View?)

        val groups = mapOf(
            "grp_attendance"      to GroupViews(binding.menuAttendance, null),
            "grp_other_entries"   to GroupViews(binding.headerOtherEntries, binding.subMenuOtherEntries),
            "grp_production"      to GroupViews(binding.menuProduction, null),
            "grp_jute"            to GroupViews(binding.headerJute, binding.subMenuJute),
            "grp_spreader_entry"  to GroupViews(binding.headerSpreaderEntry, binding.subMenuSpreaderEntry),
            "grp_doff_entry"      to GroupViews(binding.headerDoffEntry, binding.subMenuDoffEntry),
            "grp_winding_entry"   to GroupViews(binding.headerWindingEntry, binding.subMenuWindingEntry),
            "grp_finishing_entry" to GroupViews(binding.headerFinishingEntry, binding.subMenuFinishingEntry),
            "grp_stocks"          to GroupViews(binding.headerStocks, binding.subMenuStocks),
        )
        for ((key, gv) in groups) {
            val visible = PermissionManager.canView(this, key)
            if (visible) {
                gv.header?.visibility = View.VISIBLE
                // Sub-menu keeps its initial collapsed state; the existing
                // toggleSubMenu() click handler will expand it on demand.
            } else {
                gv.header?.visibility = View.GONE
                gv.subMenu?.visibility = View.GONE
            }
        }

        // Top-level dashboard quick-access cards.
        val cards = mapOf(
            "card_present" to binding.cardPresent,
            "card_jute"    to binding.cardJute,
            "card_spg"     to binding.cardSpg,
            "card_winding" to binding.cardWinding,
            "card_others"  to binding.cardOthers,
            "card_bales"   to binding.cardBales,
        )
        for ((key, view) in cards) {
            PermissionManager.applyVisibility(this, key, view)
        }

        // Leaf menu items (clickable rows under each group).
        val leaves = mapOf(
            "menu_attendance_dashboard"   to binding.menuAttendanceDashboard,
            "menu_onboarding"             to binding.menuOnBoarding,
            "menu_attendance_entry"       to binding.menuAttendanceEntry,
            "menu_attendance_reports"     to binding.menuAttendanceReports,
            "menu_leave_entries"          to binding.menuLeaveEntries,
            "menu_jute_received"          to binding.menuJuteReceived,
            "menu_assorting_entry"        to binding.menuAssortingEntry,
            "menu_production_entry"       to binding.menuProductionEntry,
            "menu_issue_entry"            to binding.menuIssueEntry,
            "menu_drawing_meter_entry"    to binding.menuDrawingMeterEntry,
            "menu_spinning_doff_entry"    to binding.menuSpinningDoffEntry,
            "menu_spellwise_frame_entry"  to binding.menuSpellWiseFrameEntry,
            "menu_spg_doff_entry"         to binding.menuSpgDoffEntry,
            "menu_spg_doff_entry1"        to binding.menuSpgDoffEntry1,
            "menu_spg_running_hours"      to binding.menuSpgRunningHours,
            "menu_winding_entry"          to binding.menuWindingEntry,
            "menu_cont_winding_entry"     to binding.menuContWindingEntry,
            "menu_weaving_entry"          to binding.menuWeavingEntry,
            "menu_other_entries"          to binding.menuOtherEntries,
            "menu_bales_production_entry" to binding.menuBalesProductionEntry,
            "menu_bales_issue_entry"      to binding.menuBalesIssueEntry,
            "menu_roll_stock"             to binding.menuRollStock,
            "menu_weight_entry"           to binding.menuWeightEntry,
        )
        for ((key, view) in leaves) {
            PermissionManager.applyVisibility(this, key, view)
        }
    }

    // ── Toggle Helper ────────────────────────────────────────────

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
