package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.DrawingSummaryItem
import java.util.Locale

class DrawingSummaryAdapter : RecyclerView.Adapter<DrawingSummaryAdapter.ViewHolder>() {

    private val items = mutableListOf<DrawingSummaryItem>()

    fun submitList(newItems: List<DrawingSummaryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drawing_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMcName: TextView = view.findViewById(R.id.tvMcName)
        private val tvUnit: TextView = view.findViewById(R.id.tvUnit)
        private val tvEff: TextView = view.findViewById(R.id.tvEff)

        fun bind(item: DrawingSummaryItem) {
            tvMcName.text = item.mcShortName ?: "—"
            tvUnit.text = (item.unit?.toInt() ?: 0).toString()  // Show as integer
            tvEff.text = String.format(Locale.getDefault(), "%.2f%%", item.eff ?: 0.0)
        }
    }
}

