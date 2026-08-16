package com.example.slsHrms.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.EmpAttRow

class EmpAttScrollableAdapter(
    private var rows: List<EmpAttRow>,
    private var columns: List<String>
) : RecyclerView.Adapter<EmpAttScrollableAdapter.VH>() {

    companion object {
        const val COL_WIDTH_DP = 46
        const val TOT_WIDTH_DP = 50

        /** The per-type tallies that close every row, in render order.
         *  Kept here so the header and the cells cannot drift apart. */
        val TYPE_COLS = listOf("R", "O", "C", "L", "H")
        const val TYPE_WIDTH_DP = 34

        // Parsed once. Color.parseColor on every cell of every bind was its own
        // slice of the scroll cost.
        private val EMPTY_GREY  = Color.parseColor("#BDBDBD")
        private val DAY_GREEN   = Color.parseColor("#1B5E20")
        private val TALLY_GREY  = Color.parseColor("#37474F")
        private val TOTAL_BLUE  = Color.parseColor("#1565C0")
        private val TOTAL_PANEL = Color.parseColor("#E3F2FD")
    }

    inner class VH(val container: LinearLayout) : RecyclerView.ViewHolder(container) {
        /** Cells are built once and re-textured on bind. Rebuilding them per
         *  bind — removeAllViews() plus a TextView per column — meant a month
         *  view allocated ~37 views for every row that scrolled past. Over a
         *  couple of thousand muster rows that stops being recycling at all and
         *  the GC pressure freezes the list. */
        val cells = mutableListOf<TextView>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val container = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(ctx, 36))
            orientation = LinearLayout.HORIZONTAL
        }
        return VH(container).also { ensureCells(it) }
    }

    /** No-op once the holder already has a cell per column; rebuilds only when
     *  the column set changed (a new date range). */
    private fun ensureCells(vh: VH) {
        val need = columns.size + TYPE_COLS.size + 1
        if (vh.cells.size == need) return
        val ctx = vh.container.context
        vh.container.removeAllViews()
        vh.cells.clear()
        fun add(widthDp: Int) {
            val tv = buildCell(ctx, dpToPx(ctx, widthDp))
            vh.container.addView(tv)
            vh.cells.add(tv)
        }
        repeat(columns.size) { add(COL_WIDTH_DP) }
        repeat(TYPE_COLS.size) { add(TYPE_WIDTH_DP) }
        add(TOT_WIDTH_DP)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = rows[position]
        val sub = r.isSubtotal
        ensureCells(holder)
        holder.container.setBackgroundColor(
            when {
                sub -> EmpAttFixedAdapter.SUBTOTAL_BG
                position % 2 == 0 -> 0xFFFFFFFF.toInt()
                else -> 0xFFF5F5F5.toInt()
            }
        )

        val style = if (sub) Typeface.BOLD else Typeface.NORMAL
        var i = 0

        // Day columns — values are day counts, so render as plain integers.
        for (col in columns) {
            val display = asCount(r.attendance?.get(col))
            holder.cells[i++].apply {
                text = display
                setTextColor(if (display.isEmpty()) EMPTY_GREY else DAY_GREEN)
                setTypeface(null, style)
            }
        }

        // R | O | C | L | H tallies. On a type row exactly one is non-zero; on
        // a subtotal they combine.
        for (n in intArrayOf(r.totR, r.totO, r.totC, r.totL, r.totH)) {
            holder.cells[i++].apply {
                text = if (n == 0) "" else n.toString()
                setTextColor(if (n == 0) EMPTY_GREY else TALLY_GREY)
                setTypeface(null, style)
            }
        }

        holder.cells[i].apply {
            text = if (r.totAll == 0) "" else r.totAll.toString()
            setTextColor(TOTAL_BLUE)
            setTypeface(null, Typeface.BOLD)
            // Reset explicitly: the holder is recycled, so a subtotal row that
            // reused a normal row's cell would otherwise keep its tinted panel.
            if (sub) setBackgroundResource(R.drawable.table_cell_border)
            else setBackgroundColor(TOTAL_PANEL)
        }
    }

    /** Day cells arrive as Int, Double or "" depending on how Gson typed the
     *  JSON number; show a bare integer and treat zero as empty. */
    private fun asCount(raw: Any?): String {
        val d = when (raw) {
            null, "" -> return ""
            is Number -> raw.toDouble()
            else -> raw.toString().toDoubleOrNull() ?: return raw.toString()
        }
        return if (d == 0.0) "" else d.toLong().toString()
    }

    private fun buildCell(ctx: Context, widthPx: Int): TextView =
        TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.MATCH_PARENT)
            gravity = android.view.Gravity.CENTER
            textSize = 11f
            maxLines = 1
            setBackgroundResource(R.drawable.table_cell_border)
        }

    private fun dpToPx(ctx: Context, dp: Int): Int =
        (dp * ctx.resources.displayMetrics.density).toInt()

    fun update(data: List<EmpAttRow>, cols: List<String>) {
        rows = data
        columns = cols
        notifyDataSetChanged()
    }
}
