package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.Spg1SummaryRow
import java.util.Locale

class Spg1SummaryAdapter(
    private val onRowClick: (Spg1SummaryRow) -> Unit
) : RecyclerView.Adapter<Spg1SummaryAdapter.VH>() {

    private val items = mutableListOf<Spg1SummaryRow>()

    fun update(list: List<Spg1SummaryRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spg1_summary, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onRowClick)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMachineNo : TextView = itemView.findViewById(R.id.tvMachineNo)
        private val tvWeights   : TextView = itemView.findViewById(R.id.tvWeights)
        private val tvNoOfDoff  : TextView = itemView.findViewById(R.id.tvNoOfDoff)

        fun bind(row: Spg1SummaryRow, onClick: (Spg1SummaryRow) -> Unit) {
            tvMachineNo.text = row.mechPostingCode?.toString()
                ?: row.mcCode?.takeIf { it.isNotBlank() }
                ?: "${row.mcId}"

            val wtParts = row.weights?.map { fmt(it) } ?: emptyList()
            val total   = fmt(row.totalWt)
            tvWeights.text = if (wtParts.isEmpty()) total
            else "${wtParts.joinToString("+")} = $total"

            tvNoOfDoff.text = row.noOfDoff.toString()

            itemView.setOnClickListener { onClick(row) }
        }

        private fun fmt(v: Double) = String.format(Locale.getDefault(), "%.0f", v)
    }
}

