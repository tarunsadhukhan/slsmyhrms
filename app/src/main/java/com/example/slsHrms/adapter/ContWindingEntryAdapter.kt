package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.ContWindingEntry

class ContWindingEntryAdapter(
    private val onDelete: (ContWindingEntry) -> Unit
) : RecyclerView.Adapter<ContWindingEntryAdapter.VH>() {

    private var items: List<ContWindingEntry> = emptyList()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvSrNo   : TextView    = v.findViewById(R.id.tvCwSrNo)
        val tvQuality: TextView    = v.findViewById(R.id.tvCwQuality)
        val tvProdKgs: TextView    = v.findViewById(R.id.tvCwProdKgs)
        val btnDelete: ImageButton = v.findViewById(R.id.btnCwDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cont_winding_entry, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = items[pos]
        h.tvSrNo.text    = (pos + 1).toString()
        h.tvQuality.text = e.qualityName ?: e.qualityId?.toString() ?: ""
        h.tvProdKgs.text = e.prodKgs?.toString() ?: "0"
        h.btnDelete.setOnClickListener { onDelete(e) }
    }

    fun update(list: List<ContWindingEntry>) {
        items = list
        notifyDataSetChanged()
    }
}
