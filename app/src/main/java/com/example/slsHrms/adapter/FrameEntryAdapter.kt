package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.DoffMachine
import com.example.slsHrms.api.FrameEntry
import com.example.slsHrms.api.SpinningQuality

class FrameEntryAdapter(
    private var items: List<DoffMachine> = emptyList(),
    private val selected: MutableSet<Int> = mutableSetOf(),
    private val onSelectionChanged: (Int) -> Unit = {}
) : RecyclerView.Adapter<FrameEntryAdapter.VH>() {

    private var qualities: List<SpinningQuality> = emptyList()
    /** mc_id → quality_id chosen for that machine (persists across check toggles). */
    private val qualityByMcId: MutableMap<Int, Int> = mutableMapOf()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cb: CheckBox       = v.findViewById(R.id.cbFrame)
        val tvCode: TextView   = v.findViewById(R.id.tvFrameCode)
        val tvName: TextView   = v.findViewById(R.id.tvFrameName)
        val spQuality: Spinner = v.findViewById(R.id.spQuality)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_frame_entry, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = items[pos]
        h.tvCode.text = m.mcCode?.takeIf { it.isNotBlank() } ?: ""
        h.tvName.text = (m.mcName ?: "").trim()

        // Checkbox
        h.cb.setOnCheckedChangeListener(null)
        h.cb.isChecked = selected.contains(m.mcId)
        h.cb.setOnCheckedChangeListener { _, checked ->
            if (checked) selected.add(m.mcId) else selected.remove(m.mcId)
            onSelectionChanged(selected.size)
        }
        h.itemView.setOnClickListener { h.cb.isChecked = !h.cb.isChecked }

        // Quality spinner
        if (qualities.isEmpty()) {
            h.spQuality.onItemSelectedListener = null
            h.spQuality.adapter = null
        } else {
            val ctx = h.itemView.context
            val spinAdapter = ArrayAdapter(ctx, R.layout.spinner_item_black, qualities).also {
                it.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
            }
            h.spQuality.onItemSelectedListener = null
            h.spQuality.adapter = spinAdapter

            val currentQid = qualityByMcId[m.mcId]
            val idx = if (currentQid != null) qualities.indexOfFirst { it.qualityId == currentQid } else -1
            val safeIdx = if (idx >= 0) idx else 0
            h.spQuality.setSelection(safeIdx, false)
            // Make sure the map reflects what is actually displayed (so save works
            // even if user never opens the dropdown).
            qualityByMcId[m.mcId] = qualities[safeIdx].qualityId

            h.spQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    qualities.getOrNull(position)?.let { qualityByMcId[m.mcId] = it.qualityId }
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
    }

    fun update(newItems: List<DoffMachine>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setQualities(list: List<SpinningQuality>) {
        qualities = list
        notifyDataSetChanged()
    }

    /** Pre-fill quality selection per machine (last-used per mc_id). */
    fun setQualityDefaults(map: Map<Int, Int>, replaceAll: Boolean = false) {
        if (replaceAll) qualityByMcId.clear()
        for ((mc, qid) in map) qualityByMcId[mc] = qid
        notifyDataSetChanged()
    }

    fun setSelected(ids: Collection<Int>) {
        selected.clear(); selected.addAll(ids)
        notifyDataSetChanged()
        onSelectionChanged(selected.size)
    }

    fun selectAll() { selected.clear(); selected.addAll(items.map { it.mcId })
        notifyDataSetChanged(); onSelectionChanged(selected.size) }

    fun clearAll() { selected.clear(); notifyDataSetChanged(); onSelectionChanged(0) }

    fun selectedIds(): List<Int> = selected.toList()

    /** Build entries payload for currently checked machines using the per-row
     *  chosen quality (falls back to first available quality). */
    fun selectedEntries(): List<FrameEntry> {
        val fallback = qualities.firstOrNull()?.qualityId
        return items.asSequence()
            .filter { selected.contains(it.mcId) }
            .map { FrameEntry(it.mcId, qualityByMcId[it.mcId] ?: fallback) }
            .toList()
    }

    fun totalCount(): Int = items.size
}

