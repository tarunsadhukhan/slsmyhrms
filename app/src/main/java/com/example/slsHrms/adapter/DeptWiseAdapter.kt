package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.DeptWiseStat

class DeptWiseAdapter(
    private var items: List<DeptWiseStat>,
    private val onItemClick: ((DeptWiseStat) -> Unit)? = null
) : RecyclerView.Adapter<DeptWiseAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDeptName: TextView      = view.findViewById(R.id.tvDeptName)
        val tvTotalEmployees: TextView = view.findViewById(R.id.tvDeptTotal)
        val tvPresent: TextView       = view.findViewById(R.id.tvDeptPresent)
        val badgeTotal: View          = view.findViewById(R.id.badgeTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dept_wise, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvDeptName.text = item.departmentName
        holder.tvTotalEmployees.text = item.totalEmployees.toString()
        holder.tvPresent.text = item.present.toString()
        holder.badgeTotal.visibility = if (item.totalEmployees > 0) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<DeptWiseStat>) {
        items = newItems
        notifyDataSetChanged()
    }
}

