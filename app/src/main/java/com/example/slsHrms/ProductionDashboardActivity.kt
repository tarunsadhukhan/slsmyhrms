package com.example.slsHrms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProductionDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_production_dashboard)

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
    }

    private fun setupWelcome() {
        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        findViewById<android.widget.TextView>(R.id.tvUserName).text = userName
        findViewById<android.widget.TextView>(R.id.tvWelcome).text = "Welcome to Production Dashboard"
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

        findViewById<View>(R.id.menuAttendanceDashboard).setOnClickListener {
            val i = Intent(this, AttendanceDashboardActivity::class.java)
            i.putExtra("USER_NAME", findViewById<android.widget.TextView>(R.id.tvUserName).text)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }

        findViewById<View>(R.id.menuAttendanceEntry).setOnClickListener {
            val i = Intent(this, AttendanceActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }


        // Toggle Production Menu
        findViewById<View>(R.id.headerProduction).setOnClickListener {
            val subMenu = findViewById<View>(R.id.subMenuProduction)
            val arrow = findViewById<ImageView>(R.id.arrowProduction)
            toggleSubMenu(subMenu, arrow)
        }

        // Production Dashboard - Stay on this page
        findViewById<View>(R.id.menuProductionDashboard).setOnClickListener {
            val subMenu = findViewById<View>(R.id.subMenuProduction)
            if (subMenu.visibility == View.VISIBLE) {
                val arrow = findViewById<ImageView>(R.id.arrowProduction)
                toggleSubMenu(subMenu, arrow)
            }
            Toast.makeText(this, "Production Dashboard", Toast.LENGTH_SHORT).show()
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
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
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
            val arrow = findViewById<ImageView>(R.id.arrowDoffEntry)
            toggleSubMenu(subMenu, arrow)
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
        findViewById<View>(R.id.menuWindingEntry).setOnClickListener {
            val i = Intent(this, WindingEntryActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
        findViewById<View>(R.id.menuWeavingEntry).setOnClickListener {
            openProductionScreen("Weaving Entry")
        }
        findViewById<View>(R.id.headerFinishingEntry)?.setOnClickListener {
            val sub = findViewById<View>(R.id.subMenuFinishingEntry)
            sub?.visibility = if (sub?.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        findViewById<View>(R.id.menuOtherEntries)?.setOnClickListener {
            val i = Intent(this, OtherEntriesActivity::class.java)
            i.putExtra("CO_ID", intent.getIntExtra("CO_ID", 0))
            i.putExtra("BRANCH_ID", intent.getIntExtra("BRANCH_ID", 0))
            startActivity(i)
        }
    }


    private fun openProductionScreen(title: String) {
        startActivity(Intent(this, MenuPlaceholderActivity::class.java).putExtra("SCREEN_TITLE", title))
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
