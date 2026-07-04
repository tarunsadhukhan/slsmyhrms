package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.EmpAttRow

class EmpAttFixedAdapter(private var rows: List<EmpAttRow>) :
    RecyclerView.Adapter<EmpAttFixedAdapter.VH>() {

    inner class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_emp_att_fixed, parent, false)
    ) {
        val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDept: TextView = itemView.findViewById(R.id.tvDept)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent)

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = rows[position]
        holder.tvCode.text = r.empCode
        holder.tvName.text = r.empName
        holder.tvDept.text = r.dept ?: ""
        // Alternate row color
        val bg = if (position % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFF5F5F5.toInt()
        holder.itemView.setBackgroundColor(bg)
    }

    fun update(data: List<EmpAttRow>) {
        rows = data
        notifyDataSetChanged()
    }
}

