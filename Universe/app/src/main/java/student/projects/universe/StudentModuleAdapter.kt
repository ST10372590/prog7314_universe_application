package student.projects.universe

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentModuleAdapter(
    private val modules: List<ModuleResponse>,
    private val context: Context
) : RecyclerView.Adapter<StudentModuleAdapter.ModuleViewHolder>() {

    inner class ModuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvModuleTitle: TextView = itemView.findViewById(R.id.tvModuleTitle)
        val tvContentType: TextView = itemView.findViewById(R.id.tvContentType)
        val tvCompletionStatus: TextView = itemView.findViewById(R.id.tvCompletionStatus)
        val tvAssessmentStatus: TextView = itemView.findViewById(R.id.tvAssessmentStatus)
        val btnViewAssessment: Button = itemView.findViewById(R.id.btnViewAssessment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_module, parent, false)
        return ModuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = modules[position]

        holder.tvModuleTitle.text = module.moduleTitle
        holder.tvContentType.text = "Type: ${module.contentType}"
        holder.tvCompletionStatus.text = "Status: ${module.completionStatus}"

        // Show assessment status if a new assessment exists
        if (module.hasNewAssessment) {
            holder.tvAssessmentStatus.text = "New assessment available!"
            holder.tvAssessmentStatus.visibility = View.VISIBLE
            holder.btnViewAssessment.visibility = View.VISIBLE
        } else {
            holder.tvAssessmentStatus.visibility = View.GONE
            holder.btnViewAssessment.visibility = View.GONE
        }

        // Open ModuleContentActivity on item click
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ModuleContentActivity::class.java)
            intent.putExtra("MODULE_TITLE", module.moduleTitle)
            intent.putExtra("CONTENT_TYPE", module.contentType)
            intent.putExtra("CONTENT_LINK", module.contentLink)
            context.startActivity(intent)
        }

        // Open StudentAssessmentActivity on button click
        holder.btnViewAssessment.setOnClickListener {
            val intent = Intent(context, StudentAssessmentActivity::class.java)
            intent.putExtra("MODULE_ID", module.moduleID)
            intent.putExtra("MODULE_TITLE", module.moduleTitle)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = modules.size
}


/*

Reference List

Appmaster. 2023. Kotlin: A Step-by-Step Guide for First-time App Developers, 13
December 2023. [Online]. Available at: https://appmaster.io/blog/kotlin-a-step-by
step-guide [Accessed 27 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */