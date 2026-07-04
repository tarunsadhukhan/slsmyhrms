package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.QualityShiftReportRow
import java.util.Locale

class QualityShiftReportAdapter : RecyclerView.Adapter<QualityShiftReportAdapter.VH>() {

    private val items = mutableListOf<QualityShiftReportRow>()

    fun update(list: List<QualityShiftReportRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quality_shift_report, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(items[position])

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQuality: TextView = itemView.findViewById(R.id.tvQuality)
        private val tvShiftA: TextView = itemView.findViewById(R.id.tvShiftA)
        private val tvShiftB: TextView = itemView.findViewById(R.id.tvShiftB)
        private val tvShiftC: TextView = itemView.findViewById(R.id.tvShiftC)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)

        fun bind(row: QualityShiftReportRow) {
            tvQuality.text = row.qualityName ?: "-"
            tvShiftA.text = row.shiftA?.let { fmt(it) } ?: "0.00"
            tvShiftB.text = row.shiftB?.let { fmt(it) } ?: "0.00"
            tvShiftC.text = row.shiftC?.let { fmt(it) } ?: "0.00"
            tvTotal.text = row.total?.let { fmt(it) } ?: "0.00"
        }

        private fun fmt(v: Double) = String.format(Locale.getDefault(), "%.2f", v)
    }
}

