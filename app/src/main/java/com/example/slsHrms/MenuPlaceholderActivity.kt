package com.example.slsHrms

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MenuPlaceholderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_placeholder)

        val screenTitle = intent.getStringExtra("SCREEN_TITLE") ?: "Screen"
        findViewById<TextView>(R.id.tvTitle).text = screenTitle

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}

