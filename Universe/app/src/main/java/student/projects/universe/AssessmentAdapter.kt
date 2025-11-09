package student.projects.universe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AssessmentAdapter(
    private val assessments: MutableList<Assessment>,
    private val onFileClick: (String) -> Unit // Lambda for handling file clicks
) : RecyclerView.Adapter<AssessmentAdapter.AssessmentViewHolder>() {

    class AssessmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        val tvMaxMarks: TextView = itemView.findViewById(R.id.tvMaxMarks)
        val btnOpenFile: TextView = itemView.findViewById(R.id.btnOpenFile) // Make sure your layout has this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssessmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assessment, parent, false)
        return AssessmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssessmentViewHolder, position: Int) {
        val assessment = assessments[position]
        holder.tvTitle.text = assessment.title
        holder.tvDescription.text = assessment.description
        holder.tvDueDate.text = "Due: ${assessment.dueDate}"
        holder.tvMaxMarks.text = "Max Marks: ${assessment.maxMarks}"

        // Show/hide file button depending on fileUrl
        if (!assessment.fileUrl.isNullOrEmpty()) {
            holder.btnOpenFile.visibility = View.VISIBLE
            holder.btnOpenFile.setOnClickListener { onFileClick(assessment.fileUrl) }
        } else {
            holder.btnOpenFile.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = assessments.size

    fun addAssessment(assessment: Assessment) {
        assessments.add(0, assessment)
        notifyItemInserted(0)
    }

    fun setAssessments(list: List<Assessment>) {
        assessments.clear()
        assessments.addAll(list)
        notifyDataSetChanged()
    }
}



/*

Reference List

Hussain, A. 2019. Android Fragments Tutorial: An Introduction with Kotlin, 10
April 2019, kodeco. [Blog]. Available at: https://www.kodeco.com/1364094 [Accessed 21 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */