package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.AttendanceRecord

class AttendanceUpdateAdapter(
    private var records: List<AttendanceRecord>,
    private val onItemClick: (AttendanceRecord) -> Unit
) : RecyclerView.Adapter<AttendanceUpdateAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmpCode: TextView = view.findViewById(R.id.tvEmpCode)
        val tvEmpName: TextView = view.findViewById(R.id.tvEmpName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvShift: TextView = view.findViewById(R.id.tvShift)
        val tvDesignation: TextView = view.findViewById(R.id.tvDesignation)
        val tvMachineNos: TextView = view.findViewById(R.id.tvMachineNos)
        val tvWorkingHours: TextView = view.findViewById(R.id.tvWorkingHours)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]

        holder.tvEmpCode.text = record.empCode
        holder.tvEmpName.text = record.empName
        holder.tvDate.text = record.attendanceDate
        holder.tvShift.text = record.shiftName ?: "No shift"
        holder.tvDesignation.text = record.designationName ?: "N/A"
        holder.tvMachineNos.text = if (record.machineNos.isNullOrEmpty()) "N/A" else record.machineNos
        holder.tvWorkingHours.text = String.format("%.1f", record.workingHours ?: 0.0)

        // Click listener to edit the record
        holder.itemView.setOnClickListener {
            onItemClick(record)
        }
    }

    override fun getItemCount(): Int = records.size

    fun updateData(newRecords: List<AttendanceRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}

