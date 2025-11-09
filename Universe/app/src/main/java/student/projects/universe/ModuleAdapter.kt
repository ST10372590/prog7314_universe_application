package student.projects.universe

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter for displaying modules.
 *
 * @param modules List of ModuleResponse objects to display
 * @param onTeachClick Lambda function called when the "Teach" button is clicked
 */
class ModuleAdapter( // (Patel, 2025)
    private val modules: List<ModuleResponse>,
    private val onTeachClick: (ModuleResponse) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

    /**
     * ViewHolder class for a single module item // (Patel, 2025)
     */
    class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvModuleTitle: TextView = itemView.findViewById(R.id.tvModuleTitle)
        val tvContentType: TextView = itemView.findViewById(R.id.tvContentType)
        val btnTeach: Button = itemView.findViewById(R.id.btnTeach)
    }

    /**
     * Inflates the module item layout and creates a ViewHolder // (Patel, 2025)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_module, parent, false)
        Log.d("ModuleAdapter", "Inflated item_module layout for ViewHolder")
        return ModuleViewHolder(view)
    }

    /**
     * Binds module data to the ViewHolder and sets click listener for "Teach" button // (Patel, 2025)
     */
    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = modules[position]
        holder.tvModuleTitle.text = module.moduleTitle
        holder.tvContentType.text = "Type: ${module.contentType}"
        Log.d(
            "ModuleAdapter",
            "Binding module at position $position: Title=${module.moduleTitle}, Type=${module.contentType}"
        )

        // Set click listener for "Teach" button // (Patel, 2025)
        holder.btnTeach.setOnClickListener {
            Log.d("ModuleAdapter", "Teach button clicked for module: ${module.moduleTitle}")
            onTeachClick(module)
        }
    }

    /**
     * Returns the total number of modules // (Patel, 2025)
     */
    override fun getItemCount(): Int {
        Log.d("ModuleAdapter", "Total modules: ${modules.size}")
        return modules.size
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */