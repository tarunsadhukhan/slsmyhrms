package com.example.slsHrms.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.EmpAttRow

class EmpAttFixedAdapter(private var rows: List<EmpAttRow>) :
    RecyclerView.Adapter<EmpAttFixedAdapter.VH>() {

    companion object {
        /** Shared with the scrollable side so a subtotal line reads as one row
         *  across the frozen/scrolling seam. */
        val SUBTOTAL_BG = 0xFFE3F2FD.toInt()
    }

    inner class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_emp_att_fixed, parent, false)
    ) {
        val tvCode: TextView  = itemView.findViewById(R.id.tvCode)
        val tvName: TextView  = itemView.findViewById(R.id.tvName)
        val tvDept: TextView  = itemView.findViewById(R.id.tvDept)
        val tvShift: TextView = itemView.findViewById(R.id.tvShift)
        val tvType: TextView  = itemView.findViewById(R.id.tvType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent)

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = rows[position]
        val sub = r.isSubtotal

        holder.tvCode.text  = if (sub) "" else r.empCode
        // The subtotal repeats the name so the line still reads on its own once
        // the employee's first row has scrolled off.
        holder.tvName.text  = if (sub) r.empName else r.empName
        holder.tvDept.text  = r.dept ?: ""
        holder.tvShift.text = r.shift ?: ""
        holder.tvType.text  = r.attType ?: ""

        val style = if (sub) Typeface.BOLD else Typeface.NORMAL
        listOf(holder.tvCode, holder.tvName, holder.tvDept, holder.tvShift, holder.tvType)
            .forEach { it.setTypeface(null, style) }

        holder.itemView.setBackgroundColor(
            when {
                sub -> SUBTOTAL_BG
                position % 2 == 0 -> 0xFFFFFFFF.toInt()
                else -> 0xFFF5F5F5.toInt()
            }
        )
    }

    fun update(data: List<EmpAttRow>) {
        rows = data
        notifyDataSetChanged()
    }
}
