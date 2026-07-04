package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.DoffSummaryRow
import java.util.Locale

class DoffSummaryAdapter(
    private var items: List<DoffSummaryRow> = emptyList()
) : RecyclerView.Adapter<DoffSummaryAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvMcNo: TextView  = v.findViewById(R.id.tvSumMcNo)
        val tvCount: TextView = v.findViewById(R.id.tvSumCount)
        val tvTotal: TextView = v.findViewById(R.id.tvSumTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_doff_summary, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = items[pos]
        h.tvMcNo.text  = r.mcNo?.takeIf { it.isNotBlank() }
            ?: r.mcName ?: ("MC ${r.mcId ?: "-"}")
        h.tvCount.text = (r.noOfDoff ?: 0).toString()
        h.tvTotal.text = String.format(Locale.getDefault(), "%.3f", r.totalWt ?: 0.0)
    }

    fun update(newItems: List<DoffSummaryRow>) {
        items = newItems
        notifyDataSetChanged()
    }
}

