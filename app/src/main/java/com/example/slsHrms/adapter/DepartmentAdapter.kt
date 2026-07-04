package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.Department

class DepartmentAdapter(
    private var departments: List<Department>,
    private val onEdit: (Department) -> Unit,
    private val onDelete: (Department) -> Unit
) : RecyclerView.Adapter<DepartmentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSerial: TextView = view.findViewById(R.id.tvSerial)
        val tvDepartmentName: TextView = view.findViewById(R.id.tvDepartmentName)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_department, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dept = departments[position]
        holder.tvSerial.text = (position + 1).toString()
        holder.tvDepartmentName.text = dept.name

        holder.btnEdit.setOnClickListener { onEdit(dept) }
        holder.btnDelete.setOnClickListener { onDelete(dept) }
    }

    override fun getItemCount(): Int = departments.size

    fun updateList(newList: List<Department>) {
        departments = newList
        notifyDataSetChanged()
    }
}

