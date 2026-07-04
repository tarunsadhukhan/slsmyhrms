package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.WeightTransaction

class WeightTransactionAdapter(
    private var list: List<WeightTransaction>,
    private val onDelete: (WeightTransaction) -> Unit
) : RecyclerView.Adapter<WeightTransactionAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvSrNo:    TextView    = v.findViewById(R.id.tvWtSrNo)
        val tvDate:    TextView    = v.findViewById(R.id.tvWtDate)
        val tvSpell:   TextView    = v.findViewById(R.id.tvWtSpell)
        val tvGross:   TextView    = v.findViewById(R.id.tvWtGross)
        val tvTare:    TextView    = v.findViewById(R.id.tvWtTare)
        val tvNet:     TextView    = v.findViewById(R.id.tvWtNet)
        val btnDelete: ImageButton = v.findViewById(R.id.btnWtDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weight_transaction, parent, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = list[pos]
        h.tvSrNo.text  = (pos + 1).toString()
        h.tvDate.text  = item.tranDate ?: ""
        h.tvSpell.text = item.spellName ?: "-"
        h.tvGross.text = String.format("%.3f", item.grossWeight ?: 0.0)
        h.tvTare.text  = String.format("%.3f", item.tareWeight  ?: 0.0)
        h.tvNet.text   = String.format("%.3f", item.netWeight   ?: 0.0)
        h.btnDelete.setOnClickListener { onDelete(item) }
    }

    fun updateList(newList: List<WeightTransaction>) {
        list = newList
        notifyDataSetChanged()
    }
}
