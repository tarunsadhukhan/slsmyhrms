package com.example.slsHrms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.slsHrms.R
import com.example.slsHrms.api.Machine

class MachineSelectionAdapter(
    private var machines: List<Machine>,
    private val selectedMachineIds: MutableSet<Int>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<MachineSelectionAdapter.ViewHolder>() {

    private var filteredMachines: List<Machine> = machines

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkboxMachine)
        val tvMachineNo: TextView = view.findViewById(R.id.tvMachineNo)
        val tvMachineName: TextView = view.findViewById(R.id.tvMachineName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_machine_checkbox, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val machine = filteredMachines[position]

        holder.tvMachineNo.text = machine.machineNo ?: ""
        holder.tvMachineName.text = machine.getDisplayName()
        
        // Set the checkbox state (without listener to avoid loops)
        val machineId = machine.id ?: 0
        holder.checkbox.isChecked = selectedMachineIds.contains(machineId)
        
        // Make checkbox not clickable directly - only row click works
        holder.checkbox.isClickable = false
        holder.checkbox.isFocusable = false

        // Remove any existing click listeners to prevent multiple listeners
        holder.itemView.setOnClickListener(null)

        // Single click handler for the entire row only
        holder.itemView.setOnClickListener {
            // Skip if machine ID is invalid
            if (machineId <= 0) {
                android.widget.Toast.makeText(
                    holder.itemView.context, 
                    "Invalid machine data", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            
            val isCurrentlySelected = selectedMachineIds.contains(machineId)

            if (isCurrentlySelected) {
                selectedMachineIds.remove(machineId)
            } else {
                selectedMachineIds.add(machineId)
            }
            
            // Only update this specific item, not all items
            notifyItemChanged(position)
            onSelectionChanged(selectedMachineIds.size)
        }
    }

    override fun getItemCount(): Int = filteredMachines.size

    fun filter(query: String) {
        filteredMachines = if (query.isEmpty()) {
            machines
        } else {
            machines.filter {
                (it.machineNo?.contains(query, ignoreCase = true) == true) ||
                (it.name?.contains(query, ignoreCase = true) == true) ||
                (it.mechCode?.contains(query, ignoreCase = true) == true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateMachines(newMachines: List<Machine>) {
        machines = newMachines
        filteredMachines = newMachines
        notifyDataSetChanged()
    }
}

