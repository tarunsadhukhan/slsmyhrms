package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.Locale

class AttendanceChecklistAdapter(private var rows: List<AttendanceRecord>) :
    RecyclerView.Adapter<AttendanceChecklistAdapter.VH>() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    inner class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_checklist_row, parent, false)
    ) {
        val tvDate: TextView         = itemView.findViewById(R.id.tvDate)
        val tvSpell: TextView        = itemView.findViewById(R.id.tvSpell)
        val tvEbNo: TextView         = itemView.findViewById(R.id.tvEbNo)
        val tvEmpName: TextView      = itemView.findViewById(R.id.tvEmpName)
        val tvDept: TextView         = itemView.findViewById(R.id.tvDept)
        val tvDesignation: TextView  = itemView.findViewById(R.id.tvDesignation)
        val tvSource: TextView       = itemView.findViewById(R.id.tvSource)
        val tvAttType: TextView      = itemView.findViewById(R.id.tvAttType)
        val tvWorkingHours: TextView = itemView.findViewById(R.id.tvWorkingHours)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(parent)

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = rows[position]
        holder.tvDate.text         = formatDate(r.attendanceDate)
        holder.tvSpell.text        = r.shiftName ?: ""
        holder.tvEbNo.text         = r.empCode
        holder.tvEmpName.text      = r.empName
        holder.tvDept.text         = r.departmentName ?: ""
        holder.tvDesignation.text  = r.designationName ?: ""
        holder.tvSource.text       = r.status ?: ""
        holder.tvAttType.text      = r.attType ?: ""
        holder.tvWorkingHours.text = String.format(Locale.getDefault(), "%.2f", r.workingHours ?: 0.0)
        // Alternate row color
        val bg = if (position % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFF5F5F5.toInt()
        holder.itemView.setBackgroundColor(bg)
    }

    private fun formatDate(raw: String): String =
        try { dispDate.format(apiDate.parse(raw)!!) } catch (_: Exception) { raw }

    fun update(data: List<AttendanceRecord>) {
        rows = data
        notifyDataSetChanged()
    }
}
