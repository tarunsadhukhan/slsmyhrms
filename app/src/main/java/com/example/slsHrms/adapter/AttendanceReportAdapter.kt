package com.example.slsHrms.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.AlertType
import com.example.slsHrms.R
import com.example.slsHrms.extractErrorMessage
import com.example.slsHrms.showAlert
import com.example.slsHrms.api.AttendanceRecord
import com.example.slsHrms.api.Department
import com.example.slsHrms.api.Designation
import com.example.slsHrms.api.DesignationResponse
import com.example.slsHrms.api.Machine
import com.example.slsHrms.api.MachineResponse
import com.example.slsHrms.api.RetrofitClient
import com.example.slsHrms.api.Shift
import com.example.slsHrms.api.UpdateAttendanceRequest
import com.example.slsHrms.api.UpdateAttendanceResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Inline-editable attendance row.
 *
 * Row 1 : dept | desig                    – EDITABLE (Spinner)
 * Row 2 : hours | mcnos                   – EDITABLE
 * Row 3 : date | spell | in | exit        – READ-ONLY
 * Row 4 : att source | att type           – READ-ONLY (color coded)
 */
class AttendanceReportAdapter(
    private var records: List<AttendanceRecord>,
    private val departments: List<Department>,
    private val shifts: List<Shift>,
    private val branchId: Int,
    private val showMachineSelector: (
        designationId: Int,
        currentSelectedIds: MutableSet<Int>,
        machinesCache: MutableList<Machine>,
        onClosed: () -> Unit
    ) -> Unit,
    private val onSaved: () -> Unit = {}
) : RecyclerView.Adapter<AttendanceReportAdapter.ViewHolder>() {

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    private data class RowState(
        var departmentId: Int? = null,
        var designationId: Int? = null,
        var designationName: String? = null,
        var shiftId: Int? = null,
        var hours: Double = 0.0,
        var attType: String = "R",
        var attSource: String = "Manual",
        var designations: MutableList<Designation> = mutableListOf(),
        var machines: MutableList<Machine> = mutableListOf(),
        var selectedMachineIds: MutableSet<Int> = mutableSetOf(),
        var initialMachineCodes: MutableSet<String> = mutableSetOf()
    )

    private val rowStates = mutableMapOf<Int, RowState>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val tvEmpName: TextView = view.findViewById(R.id.tvEmpName)
        val btnSave: ImageView = view.findViewById(R.id.btnSave)
        val spDepartment: Spinner = view.findViewById(R.id.spDepartment)
        val spDesignation: Spinner = view.findViewById(R.id.spDesignation)
        val etHours: EditText = view.findViewById(R.id.etHours)
        val tvMachineNos: TextView = view.findViewById(R.id.tvMachineNos)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvSpell: TextView = view.findViewById(R.id.tvSpell)
        val tvInTime: TextView = view.findViewById(R.id.tvInTime)
        val tvExitTime: TextView = view.findViewById(R.id.tvExitTime)
        val tvAttSource: TextView = view.findViewById(R.id.tvAttSource)
        val tvAttType: TextView = view.findViewById(R.id.tvAttType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_report, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = records.size

    fun updateList(newList: List<AttendanceRecord>) {
        records = newList
        rowStates.clear()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val ctx = holder.itemView.context
        val state = rowStates.getOrPut(record.id) { initStateFromRecord(record) }

        // Header
        bindPhoto(holder, record)
        holder.ivPhoto.setOnClickListener { showPhotoPreview(holder.itemView, record) }
        holder.tvEmpName.text = "${record.empCode} - ${record.empName}"

        // ── Row 1: Dept (editable) ──
        val deptNames = mutableListOf("Select")
        deptNames.addAll(departments.map { it.name })
        val deptAdapter = ArrayAdapter(ctx, R.layout.spinner_item_black, deptNames)
        deptAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        holder.spDepartment.adapter = deptAdapter
        val deptIdx = state.departmentId?.let { id ->
            departments.indexOfFirst { it.id == id }
        } ?: -1
        holder.spDepartment.setSelection(if (deptIdx >= 0) deptIdx + 1 else 0)
        holder.spDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var first = true
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (first) { first = false; return }
                val newId = if (pos > 0) departments[pos - 1].id else null
                if (newId != state.departmentId) {
                    state.departmentId = newId
                    state.designationId = null
                    state.designations.clear()
                    state.machines.clear()
                    state.selectedMachineIds.clear()
                    refreshDesignationSpinner(holder, state)
                    refreshMachineDisplay(holder, state)
                    if (newId != null) loadDesignations(holder, state, newId, null)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // ── Row 1: Designation (editable) ──
        if (state.designations.isEmpty() && state.departmentId != null) {
            loadDesignations(holder, state, state.departmentId!!, state.designationId)
        } else {
            refreshDesignationSpinner(holder, state)
        }
        holder.spDesignation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var first = true
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (first) { first = false; return }
                val newId = if (pos > 0 && state.designations.size >= pos)
                    state.designations[pos - 1].id else null
                if (newId != state.designationId) {
                    state.designationId = newId
                    state.machines.clear()
                    state.selectedMachineIds.clear()
                    refreshMachineDisplay(holder, state)
                    if (newId != null) loadMachines(holder, state, newId)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // ── Row 2: Hours (editable) ──
        holder.etHours.setText(String.format(Locale.US, "%.1f", state.hours))

        // ── Row 2: Mc Nos (editable via dialog) ──
        refreshMachineDisplay(holder, state)
        holder.tvMachineNos.setOnClickListener {
            val desigId = state.designationId
            if (desigId == null) {
                Toast.makeText(ctx, "Select designation first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showMachineSelector(desigId, state.selectedMachineIds, state.machines) {
                refreshMachineDisplay(holder, state)
            }
        }

        // ── Row 3: Date | Spell | In | Exit (READ-ONLY) ──
        holder.tvDate.text = formatDisplayDate(record.attendanceDate)
        holder.tvSpell.text = record.shiftName ?: "-"
        holder.tvInTime.text = if (record.attendanceTime.isBlank()) "-" else record.attendanceTime
        holder.tvExitTime.text = if (record.exitTime.isNullOrBlank()) "-" else record.exitTime

        // ── Row 4: Att Source (READ-ONLY, colored) ──
        val source = record.status ?: "-"
        holder.tvAttSource.text = source
        when (source.uppercase(Locale.US)) {
            "FACE"   -> holder.tvAttSource.setBackgroundResource(R.drawable.bg_badge_face)
            "MANUAL" -> holder.tvAttSource.setBackgroundResource(R.drawable.bg_badge_manual)
            "BIO"    -> holder.tvAttSource.setBackgroundResource(R.drawable.bg_badge_total)
            else     -> holder.tvAttSource.setBackgroundResource(R.drawable.bg_badge_manual)
        }

        // ── Row 4: Att Type (READ-ONLY, colored) ──
        val attTypeText = when (record.attType) {
            "R" -> "Regular"
            "O" -> "OT"
            "C" -> "Cash"
            else -> record.attType ?: "-"
        }
        holder.tvAttType.text = attTypeText
        when (record.attType) {
            "R" -> holder.tvAttType.setBackgroundResource(R.drawable.bg_badge_regular)
            "O" -> holder.tvAttType.setBackgroundResource(R.drawable.bg_badge_ot)
            "C" -> holder.tvAttType.setBackgroundResource(R.drawable.bg_badge_cash)
            else -> holder.tvAttType.setBackgroundResource(R.drawable.bg_badge_regular)
        }

        // ── Save ──
        holder.btnSave.setOnClickListener {
            state.hours = holder.etHours.text.toString().toDoubleOrNull() ?: 0.0
            saveRow(ctx, record, state, holder)
        }
    }

    // ─────────────────────────────────────────────────────────────
    private fun initStateFromRecord(record: AttendanceRecord): RowState {
        val s = RowState()
        s.hours = record.workingHours ?: 0.0
        s.attType = record.attType ?: "R"
        s.attSource = record.status ?: "Manual"
        s.departmentId = departments.firstOrNull { it.name == record.departmentName }?.id
        s.designationName = record.designationName
        s.shiftId = shifts.firstOrNull { it.name.equals(record.shiftName, true) }?.id
        s.initialMachineCodes = record.machineNos
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toMutableSet() ?: mutableSetOf()
        return s
    }

    private fun bindPhoto(holder: ViewHolder, record: AttendanceRecord) {
        val base64 = extractBase64(record.photoAtt)
        if (!base64.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    holder.ivPhoto.setImageBitmap(bitmap)
                    holder.ivPhoto.clearColorFilter()
                    return
                }
            } catch (_: Exception) { /* fall through */ }
        }
        when (record.status) {
            "Face" -> {
                holder.ivPhoto.setImageResource(R.drawable.ic_face)
                holder.ivPhoto.setColorFilter(Color.parseColor("#2E7D32"))
            }
            else -> {
                holder.ivPhoto.setImageResource(R.drawable.ic_person)
                holder.ivPhoto.setColorFilter(Color.parseColor("#F57C00"))
            }
        }
    }

    /**
     * Accepts either:
     *  - raw base64 string (e.g. "/9j/4AAQSk...")
     *  - HTML wrapper: <img src="data:image/jpeg;base64,XXXX" />
     * Returns the raw base64 portion, or null if not extractable.
     */
    private fun extractBase64(photoHtml: String?): String? {
        if (photoHtml.isNullOrBlank()) return null
        val trimmed = photoHtml.trim()
        return when {
            trimmed.contains("base64,") ->
                Regex("""base64,([A-Za-z0-9+/=]+)""").find(trimmed)?.groupValues?.get(1)?.trim()
            !trimmed.contains('<') -> trimmed.replace("\\s".toRegex(), "")
            else -> null
        }
    }

    private fun refreshDesignationSpinner(holder: ViewHolder, state: RowState) {
        val ctx = holder.itemView.context
        val items = mutableListOf("Select")
        items.addAll(state.designations.map { it.name })
        val adapter = ArrayAdapter(ctx, R.layout.spinner_item_black, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        holder.spDesignation.adapter = adapter
        val idx = state.designationId?.let { id ->
            state.designations.indexOfFirst { it.id == id }
        } ?: -1
        holder.spDesignation.setSelection(if (idx >= 0) idx + 1 else 0)
    }

    private fun refreshMachineDisplay(holder: ViewHolder, state: RowState) {
        if (state.selectedMachineIds.isNotEmpty() && state.machines.isNotEmpty()) {
            val sel = state.machines.filter { (it.id ?: 0) in state.selectedMachineIds }
            holder.tvMachineNos.text = if (sel.isNotEmpty())
                sel.joinToString(", ") { it.mechCode ?: it.getDisplayName() }
            else
                state.initialMachineCodes.joinToString(", ").ifBlank { "Tap to select" }
        } else if (state.initialMachineCodes.isNotEmpty()) {
            holder.tvMachineNos.text = state.initialMachineCodes.joinToString(", ")
        } else {
            holder.tvMachineNos.text = "Tap to select"
        }
    }

    private fun loadDesignations(
        holder: ViewHolder,
        state: RowState,
        subDeptId: Int,
        preselectId: Int?
    ) {
        if (branchId <= 0) return
        RetrofitClient.getApiService(holder.itemView.context)
            .getDesignations(branchId, subDeptId)
            .enqueue(object : Callback<DesignationResponse> {
                override fun onResponse(
                    call: Call<DesignationResponse>,
                    response: Response<DesignationResponse>
                ) {
                    if (response.isSuccessful) {
                        state.designations.clear()
                        state.designations.addAll(response.body()?.designations ?: emptyList())
                        if (preselectId != null) {
                            state.designationId = preselectId
                        } else if (state.designationId == null && !state.designationName.isNullOrBlank()) {
                            // Resolve designationId from the original name (record had no id)
                            val match = state.designations.firstOrNull {
                                it.name.equals(state.designationName, ignoreCase = true)
                            }
                            if (match != null) state.designationId = match.id
                        }
                        refreshDesignationSpinner(holder, state)
                        state.designationId?.let { loadMachines(holder, state, it) }
                    }
                }
                override fun onFailure(call: Call<DesignationResponse>, t: Throwable) {}
            })
    }

    private fun loadMachines(holder: ViewHolder, state: RowState, designationId: Int) {
        RetrofitClient.getApiService(holder.itemView.context)
            .getMachines(designationId)
            .enqueue(object : Callback<MachineResponse> {
                override fun onResponse(
                    call: Call<MachineResponse>,
                    response: Response<MachineResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val valid = (response.body()?.data ?: emptyList())
                            .filter { (it.id ?: 0) > 0 }
                        state.machines.clear()
                        state.machines.addAll(valid)
                        if (state.selectedMachineIds.isEmpty() && state.initialMachineCodes.isNotEmpty()) {
                            valid.forEach { m ->
                                val code = m.mechCode?.trim().orEmpty()
                                if (code.isNotEmpty() && state.initialMachineCodes.contains(code)) {
                                    m.id?.let { state.selectedMachineIds.add(it) }
                                }
                            }
                        }
                        refreshMachineDisplay(holder, state)
                    }
                }
                override fun onFailure(call: Call<MachineResponse>, t: Throwable) {}
            })
    }

    private fun saveRow(
        ctx: Context,
        record: AttendanceRecord,
        state: RowState,
        holder: ViewHolder
    ) {
        holder.btnSave.isEnabled = false
        val request = UpdateAttendanceRequest(
            empCode = record.empCode,
            attendanceDate = record.attendanceDate,
            attType = state.attType,
            departmentId = state.departmentId,
            shiftId = state.shiftId,
            designationId = state.designationId,
            shiftHours = record.shiftHours,
            workingHours = state.hours,
            idleHours = record.idleHours,
            machineIds = state.selectedMachineIds.toList()
        )
        RetrofitClient.getApiService(ctx).updateAttendance(record.id, request)
            .enqueue(object : Callback<UpdateAttendanceResponse> {
                override fun onResponse(
                    call: Call<UpdateAttendanceResponse>,
                    response: Response<UpdateAttendanceResponse>
                ) {
                    holder.btnSave.isEnabled = true
                    val body = response.body()
                    if (response.isSuccessful && body?.status == "success") {
                        ctx.showAlert("Success", body.message ?: "Attendance updated",
                            AlertType.SUCCESS)
                        onSaved()
                    } else {
                        // On a 400 (leave / spell-hours conflict) the message
                        // is in errorBody, not body.
                        val errMsg = body?.message
                            ?: response.errorBody()?.string()
                                ?.let { extractErrorMessage(it) }
                            ?: "Save failed (${response.code()})"
                        ctx.showAlert("Update Failed", errMsg)
                    }
                }
                override fun onFailure(call: Call<UpdateAttendanceResponse>, t: Throwable) {
                    holder.btnSave.isEnabled = true
                    Toast.makeText(
                        ctx,
                        "Network error: ${t.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun formatDisplayDate(apiDate: String): String = try {
        val parsed = apiDateFormat.parse(apiDate)
        if (parsed != null) displayDateFormat.format(parsed) else apiDate
    } catch (_: Exception) { apiDate }

    private fun showPhotoPreview(itemView: View, record: AttendanceRecord) {
        val base64 = extractBase64(record.photoAtt)
        if (base64.isNullOrBlank()) {
            Toast.makeText(itemView.context, "No captured photo available", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
            val dialog = Dialog(itemView.context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_photo_preview)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#CC000000")))
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.findViewById<TextView>(R.id.tvPhotoTitle).text =
                "${record.empName} (${record.empCode})"
            dialog.findViewById<ImageView>(R.id.ivFullPhoto).apply {
                setImageBitmap(bitmap)
                setOnClickListener { dialog.dismiss() }
            }
            dialog.findViewById<TextView>(R.id.tvPhotoInfo).text =
                "${formatDisplayDate(record.attendanceDate)}  •  ${record.attendanceTime}  •  ${record.status ?: ""}"
            dialog.setCancelable(true)
            dialog.show()
        } catch (_: Exception) {
            Toast.makeText(itemView.context, "Error displaying photo", Toast.LENGTH_SHORT).show()
        }
    }
}

