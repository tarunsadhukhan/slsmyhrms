package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.Occupation

class OccupationAdapter(
    private var occupations: List<Occupation>,
    private val onEdit: (Occupation) -> Unit,
    private val onDelete: (Occupation) -> Unit
) : RecyclerView.Adapter<OccupationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSerial: TextView = view.findViewById(R.id.tvSerial)
        val tvOccupationName: TextView = view.findViewById(R.id.tvOccupationName)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_occupation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val occ = occupations[position]
        holder.tvSerial.text = (position + 1).toString()
        holder.tvOccupationName.text = occ.name

        holder.btnEdit.setOnClickListener { onEdit(occ) }
        holder.btnDelete.setOnClickListener { onDelete(occ) }
    }

    override fun getItemCount(): Int = occupations.size

    fun updateList(newList: List<Occupation>) {
        occupations = newList
        notifyDataSetChanged()
    }
}

