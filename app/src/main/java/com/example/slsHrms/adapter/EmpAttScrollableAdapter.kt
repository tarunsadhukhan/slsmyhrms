package com.example.slsHrms.adapter

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.api.EmpAttRow

class EmpAttScrollableAdapter(
    private var rows: List<EmpAttRow>,
    private var columns: List<String>
) : RecyclerView.Adapter<EmpAttScrollableAdapter.VH>() {

    companion object {
        const val COL_WIDTH_DP = 46
        const val TOT_WIDTH_DP = 50
    }

    inner class VH(val container: LinearLayout) : RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val container = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(ctx, 36)
            )
            orientation = LinearLayout.HORIZONTAL
        }
        return VH(container)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = rows[position]
        val ctx = holder.container.context
        val bgColor = if (position % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFF5F5F5.toInt()
        holder.container.removeAllViews()
        holder.container.setBackgroundColor(bgColor)

        for (col in columns) {
            val raw = r.attendance?.get(col)
            val display = when {
                raw == null || raw == "" || raw.toString() == "0.0" || raw.toString() == "0" -> ""
                raw is Double -> if (raw == raw.toLong().toDouble()) raw.toLong().toString() else String.format("%.1f", raw)
                else -> raw.toString()
            }
            val tv = buildCell(ctx, display, dpToPx(ctx, COL_WIDTH_DP))
            tv.setTextColor(
                if (display.isEmpty()) Color.parseColor("#BDBDBD")
                else Color.parseColor("#1B5E20")
            )
            holder.container.addView(tv)
        }

        // Total Hours column
        val totalDisplay = if (r.totalHours == r.totalHours.toLong().toDouble())
            r.totalHours.toLong().toString()
        else String.format("%.1f", r.totalHours)

        val tvTot = buildCell(ctx, totalDisplay, dpToPx(ctx, TOT_WIDTH_DP))
        tvTot.setTextColor(Color.parseColor("#1565C0"))
        tvTot.setTypeface(null, android.graphics.Typeface.BOLD)
        tvTot.setBackgroundColor(Color.parseColor("#E3F2FD"))
        holder.container.addView(tvTot)
    }

    private fun buildCell(ctx: Context, text: String, widthPx: Int): TextView {
        return TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.MATCH_PARENT)
            this.text = text
            gravity = android.view.Gravity.CENTER
            textSize = 11f
            setBackgroundResource(com.example.slsHrms.R.drawable.table_cell_border)
        }
    }

    private fun dpToPx(ctx: Context, dp: Int): Int =
        (dp * ctx.resources.displayMetrics.density).toInt()

    fun update(data: List<EmpAttRow>, cols: List<String>) {
        rows = data
        columns = cols
        notifyDataSetChanged()
    }
}
