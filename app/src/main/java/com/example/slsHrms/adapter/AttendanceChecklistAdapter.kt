package com.example.slsHrms.adapter

import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.Locale

class AttendanceChecklistAdapter(
    private var rows: List<AttendanceRecord>,
    private val onRowDoubleTap: ((AttendanceRecord) -> Unit)? = null
) : RecyclerView.Adapter<AttendanceChecklistAdapter.VH>() {

    private val apiDate  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dispDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    // Double-tap via two timed clicks rather than a GestureDetector: a touch
    // listener on the row has to consume ACTION_DOWN to see the second tap,
    // which fights the RecyclerView and the HorizontalScrollView for the
    // scroll gesture. Click listeners don't interfere with either.
    private val doubleTapMs = ViewConfiguration.getDoubleTapTimeout().toLong()
    private var lastTapPos  = RecyclerView.NO_POSITION
    private var lastTapAt   = 0L

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
        // Wk Hrs is net: working minus idle — the same figure the designation
        // summary divides by spell hours to get "no of hands".
        val netHours = (r.workingHours ?: 0.0) - (r.idleHours ?: 0.0)
        holder.tvWorkingHours.text = String.format(Locale.getDefault(), "%.2f", netHours)
        // Attendance type sets the row colour; only plain Regular rows fall back
        // to the alternating stripe. Material 50-level tints — light enough that
        // the dark cell text stays readable.
        val bg = when ((r.attType ?: "R").trim().uppercase()) {
            "L"  -> 0xFFE8F5E9.toInt()   // leave        — light green
            "O"  -> 0xFFFFF3E0.toInt()   // over time    — light orange
            "C"  -> 0xFFFFEBEE.toInt()   // cash         — light red
            else -> if (position % 2 == 0) 0xFFFFFFFF.toInt() else 0xFFF5F5F5.toInt()
        }
        holder.itemView.setBackgroundColor(bg)

        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val now = SystemClock.elapsedRealtime()
            if (pos == lastTapPos && now - lastTapAt <= doubleTapMs) {
                lastTapPos = RecyclerView.NO_POSITION   // so a third tap starts over
                rows.getOrNull(pos)?.let { rec -> onRowDoubleTap?.invoke(rec) }
            } else {
                lastTapPos = pos
                lastTapAt  = now
            }
        }
    }

    private fun formatDate(raw: String): String =
        try { dispDate.format(apiDate.parse(raw)!!) } catch (_: Exception) { raw }

    fun update(data: List<AttendanceRecord>) {
        rows = data
        notifyDataSetChanged()
    }
}
