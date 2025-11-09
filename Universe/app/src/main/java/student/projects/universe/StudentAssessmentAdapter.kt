package student.projects.universe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentAssessmentAdapter(
    private val assessments: List<Assessment>,
    private val onViewClick: (String?) -> Unit,
    private val onSubmitClick: (Assessment) -> Unit
) : RecyclerView.Adapter<StudentAssessmentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvAssessmentTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvAssessmentDesc)
        val tvDue: TextView = view.findViewById(R.id.tvDueDate)
        val btnView: Button = view.findViewById(R.id.btnView)
        val btnSubmit: Button = view.findViewById(R.id.btnSubmit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_assessment, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = assessments.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val assessment = assessments[position]
        holder.tvTitle.text = assessment.title
        holder.tvDesc.text = assessment.description
        holder.tvDue.text = "Due: ${assessment.dueDate}"

        holder.btnView.setOnClickListener { onViewClick(assessment.fileUrl) }
        holder.btnSubmit.setOnClickListener { onSubmitClick(assessment) }
    }
}
