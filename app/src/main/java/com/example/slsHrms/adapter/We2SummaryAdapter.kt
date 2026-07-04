package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.We2GroupedRow
import java.util.Locale

class We2SummaryAdapter(
    private val onClick: (We2GroupedRow) -> Unit = {}
) : RecyclerView.Adapter<We2SummaryAdapter.VH>() {

    private val items = mutableListOf<We2GroupedRow>()

    fun update(list: List<We2GroupedRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_we2_summary, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onClick)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmpName  : TextView = itemView.findViewById(R.id.tvEmpName)
        private val tvWeights  : TextView = itemView.findViewById(R.id.tvWeights)
        private val tvNoOfDoff : TextView = itemView.findViewById(R.id.tvNoOfDoff)

        fun bind(row: We2GroupedRow, onClick: (We2GroupedRow) -> Unit) {
            tvEmpName.text  = row.empName?.takeIf { it.isNotBlank() } ?: row.empCode ?: "-"

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
