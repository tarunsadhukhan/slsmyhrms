package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.We2SummaryRow
import java.util.Locale

class We2DetailAdapter : RecyclerView.Adapter<We2DetailAdapter.VH>() {

    private val items = mutableListOf<We2SummaryRow>()

    fun update(list: List<We2SummaryRow>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_we2_detail, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(position + 1, items[position])

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSeq     : TextView = itemView.findViewById(R.id.tvSeq)
        private val tvScType  : TextView = itemView.findViewById(R.id.tvScType)
        private val tvTrollyNo: TextView = itemView.findViewById(R.id.tvTrollyNo)
        private val tvGrossWt : TextView = itemView.findViewById(R.id.tvGrossWt)
        private val tvTareWt  : TextView = itemView.findViewById(R.id.tvTareWt)
        private val tvNetWt   : TextView = itemView.findViewById(R.id.tvNetWt)

        fun bind(seq: Int, row: We2SummaryRow) {
            tvSeq.text      = seq.toString()
            tvScType.text   = row.scType ?: "-"
            tvTrollyNo.text = row.trollyName ?: (row.trollyId?.toString() ?: "-")
            tvGrossWt.text  = row.grossWeight?.let { fmt(it) } ?: "-"
            tvTareWt.text   = row.tareWeight?.let  { fmt(it) } ?: "-"
            tvNetWt.text    = row.netWeight?.let   { fmt(it) } ?: "-"
        }

        private fun fmt(v: Double) = String.format(Locale.getDefault(), "%.3f", v)
    }
}

