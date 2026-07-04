package com.example.slsHrms

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView

class AttendanceReportsActivity : AppCompatActivity() {

    private var coId: Int = 0
    private var branchId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_reports)

        coId = intent.getIntExtra("CO_ID", 0)
        branchId = intent.getIntExtra("BRANCH_ID", 0)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        findViewById<CardView>(R.id.cardEmpWiseAttendance).setOnClickListener {
            val intent = Intent(this, EmpWiseAttendanceActivity::class.java)
            intent.putExtra("CO_ID", coId)
            intent.putExtra("BRANCH_ID", branchId)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardAttendanceChecklist).setOnClickListener {
            val intent = Intent(this, AttendanceChecklistActivity::class.java)
            intent.putExtra("CO_ID", coId)
            intent.putExtra("BRANCH_ID", branchId)
            startActivity(intent)
        }

        findViewById<CardView>(R.id.cardEmpWiseWages).setOnClickListener {
            openPlaceholder("Emp Wise Wages")
        }

        findViewById<CardView>(R.id.cardEmpWiseProdEff).setOnClickListener {
            openPlaceholder("Emp Wise Prod and Eff")
        }

        findViewById<CardView>(R.id.cardEmpWiseAbsentism).setOnClickListener {
            openPlaceholder("Emp Wise Absentism")
        }
    }

    private fun openPlaceholder(title: String) {
        val intent = Intent(this, MenuPlaceholderActivity::class.java)
        intent.putExtra("SCREEN_TITLE", title)
        intent.putExtra("CO_ID", coId)
        intent.putExtra("BRANCH_ID", branchId)
        startActivity(intent)
    }
}

