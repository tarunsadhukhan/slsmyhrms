package com.example.slsHrms.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.Employee

class EmployeeAdapter(
    private var employees: List<Employee>,
    private val onEdit: (Employee) -> Unit,
    private val onDelete: (Employee) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSerial: TextView = view.findViewById(R.id.tvSerial)
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val tvEmpName: TextView = view.findViewById(R.id.tvEmpName)
        val tvEmpCode: TextView = view.findViewById(R.id.tvEmpCode)
        val tvDeptShift: TextView = view.findViewById(R.id.tvDeptShift)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employee, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val emp = employees[position]
        holder.tvSerial.text = (position + 1).toString()
        holder.tvEmpName.text = emp.name
        holder.tvEmpCode.text = "Code: ${emp.empCode}"

        val details = mutableListOf<String>()
        if (!emp.departmentName.isNullOrBlank()) details.add(emp.departmentName)
        if (!emp.designationName.isNullOrBlank()) details.add(emp.designationName)
        if (!emp.shiftName.isNullOrBlank()) details.add(emp.shiftName)
        holder.tvDeptShift.text = details.joinToString(" • ")

        // Load photo from base64 (server extracts from photo_html)
        if (!emp.photoBase64.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(emp.photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.ivPhoto.setImageBitmap(bitmap)
                holder.ivPhoto.setPadding(0, 0, 0, 0)
                holder.ivPhoto.imageTintList = null
            } catch (e: Exception) {
                holder.ivPhoto.setImageResource(R.drawable.ic_face)
            }
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_face)
        }

        holder.btnEdit.setOnClickListener { onEdit(emp) }
        holder.btnDelete.setOnClickListener { onDelete(emp) }
    }

    override fun getItemCount(): Int = employees.size

    fun updateList(newList: List<Employee>) {
        employees = newList
        notifyDataSetChanged()
    }
}

