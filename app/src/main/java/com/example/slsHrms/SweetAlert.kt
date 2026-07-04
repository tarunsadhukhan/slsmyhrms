package com.example.slsHrms

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

// SweetAlert-style popup: red "✕" ring for errors, orange "!" ring for
// warnings, green "✓" ring for success. Card pops in, icon bounces in.
enum class AlertType(val color: Int, val glyph: String) {
    ERROR(0xFFF27474.toInt(), "✕"),
    WARNING(0xFFF8BB86.toInt(), "!"),
    SUCCESS(0xFFA5DC86.toInt(), "✓")
}

fun Context.showAlert(title: String, message: String, type: AlertType = AlertType.ERROR) {
    if ((this as? Activity)?.isFinishing == true) return
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_sweet_alert, null)

    val icon = view.findViewById<TextView>(R.id.tvSweetIcon)
    icon.text = type.glyph
    icon.setTextColor(type.color)
    icon.backgroundTintList = ColorStateList.valueOf(type.color)
    view.findViewById<TextView>(R.id.tvSweetTitle).text = title
    view.findViewById<TextView>(R.id.tvSweetMessage).text = message

    val dialog = AlertDialog.Builder(this).setView(view).create()
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    view.findViewById<Button>(R.id.btnSweetOk).setOnClickListener { dialog.dismiss() }
    dialog.show()

    // Pop-in card, then bounce the icon ring in.
    val overshoot = OvershootInterpolator(2f)
    view.scaleX = 0.7f; view.scaleY = 0.7f; view.alpha = 0f
    view.animate().scaleX(1f).scaleY(1f).alpha(1f)
        .setDuration(250).setInterpolator(overshoot).start()
    icon.scaleX = 0f; icon.scaleY = 0f
    icon.animate().scaleX(1f).scaleY(1f)
        .setStartDelay(150).setDuration(450).setInterpolator(overshoot).start()
}

// Backend errors arrive as JSON {"status":"error","message":"..."} —
// pull out the message; fall back to the raw body if it isn't JSON.
fun extractErrorMessage(errorBody: String): String = try {
    org.json.JSONObject(errorBody).optString("message").ifEmpty { errorBody }
} catch (e: Exception) {
    errorBody
}
