package com.example.slsHrms.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.LeaveTransaction

class LeaveTransactionAdapter(
    private var items: List<LeaveTransaction>,
    private val onEdit: (LeaveTransaction) -> Unit,
    private val onDelete: (LeaveTransaction) -> Unit
) : RecyclerView.Adapter<LeaveTransactionAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvEmpCode: TextView  = v.findViewById(R.id.tvEmpCode)
        val tvEmpName: TextView  = v.findViewById(R.id.tvEmpName)
        val tvStatus: TextView   = v.findViewById(R.id.tvStatus)
        val tvLeaveType: TextView = v.findViewById(R.id.tvLeaveType)
        val tvNoOfDays: TextView = v.findViewById(R.id.tvNoOfDays)
        val tvDateRange: TextView = v.findViewById(R.id.tvDateRange)
        val tvReason: TextView   = v.findViewById(R.id.tvReason)
        val btnEdit: Button      = v.findViewById(R.id.btnEdit)
        val btnDelete: Button    = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_leave_transaction, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.tvEmpCode.text  = item.empCode ?: ""
        h.tvEmpName.text  = item.empName ?: ""
        h.tvLeaveType.text = item.leaveType ?: "-"
        val days = item.noOfDays?.toLong() ?: try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val d1 = sdf.parse(item.fromDate ?: "")
            val d2 = sdf.parse(item.toDate ?: "")
            if (d1 != null && d2 != null) ((d2.time - d1.time) / 86400000L + 1) else 0L
        } catch (_: Exception) { 0L }
        h.tvNoOfDays.text = "$days day(s)"
        h.tvDateRange.text = "${item.fromDate ?: "-"}  →  ${item.toDate ?: "-"}"
        h.tvReason.text   = item.reason?.takeIf { it.isNotBlank() } ?: ""

        val status = item.status ?: "Pending"
        h.tvStatus.text = status
        h.tvStatus.setBackgroundColor(when (status.lowercase()) {
            "approved"  -> Color.parseColor("#2E7D32")
            "rejected"  -> Color.parseColor("#C62828")
            "cancelled" -> Color.parseColor("#555555")
            else        -> Color.parseColor("#1565C0")
        })

        h.btnEdit.setOnClickListener   { onEdit(item) }
        h.btnDelete.setOnClickListener { onDelete(item) }
    }

    fun update(newItems: List<LeaveTransaction>) {
        items = newItems
        notifyDataSetChanged()
    }
}

