package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.DoffTransaction

class DoffTransactionAdapter(
    private var items: List<DoffTransaction>,
    private val onEdit: (DoffTransaction) -> Unit,
    private val onDelete: (DoffTransaction) -> Unit
) : RecyclerView.Adapter<DoffTransactionAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvEmpCode: TextView   = v.findViewById(R.id.tvEmpCode)
        val tvEmpName: TextView   = v.findViewById(R.id.tvEmpName)
        val tvMachine: TextView   = v.findViewById(R.id.tvMachine)
        val tvDateSpell: TextView = v.findViewById(R.id.tvDateSpell)
        val tvQuality: TextView   = v.findViewById(R.id.tvQuality)
        val tvTrolly: TextView    = v.findViewById(R.id.tvTrolly)
        val tvGross: TextView     = v.findViewById(R.id.tvGross)
        val tvTare: TextView      = v.findViewById(R.id.tvTare)
        val tvNet: TextView       = v.findViewById(R.id.tvNet)
        val btnEdit: Button       = v.findViewById(R.id.btnEdit)
        val btnDelete: Button     = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_doff_transaction, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val tx = items[pos]
        h.tvEmpCode.text  = tx.empCode ?: ""
        h.tvEmpName.text  = tx.empName ?: ""
        val mcCodeStr = tx.mcCode?.takeIf { s -> s.isNotBlank() }
        val mcNameStr = tx.mcName?.takeIf { s -> s.isNotBlank() }
        h.tvMachine.text = listOfNotNull(mcCodeStr, mcNameStr)
            .joinToString(" ")
            .ifBlank { "MC ${tx.mcId ?: ""}" }
        h.tvDateSpell.text = "${tx.doffDate ?: "-"}   •   ${tx.spellName ?: "-"}"
        h.tvQuality.text   = "Qual: ${tx.qualityName ?: "-"}"
        h.tvTrolly.text    = "Trolly: ${tx.trollyName ?: "-"}"
        h.tvGross.text     = "G: ${fmt(tx.grossWeight)}"
        h.tvTare.text      = "T: ${fmt(tx.tareWeight)}"
        h.tvNet.text       = "Net: ${fmt(tx.netWeight)}"

        h.btnEdit.setOnClickListener   { onEdit(tx) }
        h.btnDelete.setOnClickListener { onDelete(tx) }
    }

    private fun fmt(d: Double?): String =
        if (d == null) "-" else String.format(java.util.Locale.getDefault(), "%.3f", d)

    fun update(newItems: List<DoffTransaction>) {
        items = newItems
        notifyDataSetChanged()
    }
}