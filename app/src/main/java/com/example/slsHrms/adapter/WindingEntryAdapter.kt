package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.WindingEntry

class WindingEntryAdapter(
    private val onEdit:   (WindingEntry) -> Unit,
    private val onDelete: (WindingEntry) -> Unit
) : RecyclerView.Adapter<WindingEntryAdapter.VH>() {

    private var items: List<WindingEntry> = emptyList()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvSrNo   : TextView    = v.findViewById(R.id.tvWeSrNo)
        val tvEbId   : TextView    = v.findViewById(R.id.tvWeEbId)
        val tvName   : TextView    = v.findViewById(R.id.tvWeName)
        val tvQuality: TextView    = v.findViewById(R.id.tvWeQuality)
        val btnEdit  : ImageButton = v.findViewById(R.id.btnWeEdit)
        val btnDelete: ImageButton = v.findViewById(R.id.btnWeDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_winding_entry, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = items[pos]
        h.tvSrNo.text    = (pos + 1).toString()
        h.tvEbId.text    = e.empCode ?: e.ebId?.toString() ?: ""
        h.tvName.text    = e.empName ?: ""
        h.tvQuality.text = e.qualityName ?: ""
        h.btnEdit.setOnClickListener   { onEdit(e) }
        h.btnDelete.setOnClickListener { onDelete(e) }
    }

    fun update(list: List<WindingEntry>) {
        items = list
        notifyDataSetChanged()
    }
}

