package student.projects.universe

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter to display a list of student assessments.
 * Supports viewing assessment files and submitting assessments.
 */
class StudentAssessmentAdapter(
    private val assessments: List<Assessment>,
    private val onViewClick: (String?) -> Unit,
    private val onSubmitClick: (Assessment) -> Unit
) : RecyclerView.Adapter<StudentAssessmentAdapter.ViewHolder>() {

    /**
     * ViewHolder class holds UI elements for each assessment item.
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvAssessmentTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvAssessmentDesc)
        val tvDue: TextView = view.findViewById(R.id.tvDueDate)
        val btnView: Button = view.findViewById(R.id.btnView)
        val btnSubmit: Button = view.findViewById(R.id.btnSubmit)
    }

    /**
     * Inflates the layout for each item in the RecyclerView.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("StudentAssessmentAdapter", "Creating ViewHolder for position $viewType") // (Patel, 2025)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_assessment, parent, false)
        return ViewHolder(view)
    }

    /**
     * Returns the total number of assessments to display.
     */
    override fun getItemCount(): Int = assessments.size

    /**
     * Binds data to each ViewHolder's views based on the current assessment item.
     * Sets up click listeners for View and Submit buttons.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val assessment = assessments[position]
        holder.tvTitle.text = assessment.title
        holder.tvDesc.text = assessment.description
        holder.tvDue.text = "Due: ${assessment.dueDate}"

        // Log binding event
        Log.d("StudentAssessmentAdapter", "Binding assessment at position $position with title: ${assessment.title}") // (Aram, 2023)

        // Setup click listener for View button to open file URL
        holder.btnView.setOnClickListener {
            Log.d("StudentAssessmentAdapter", "View clicked for assessment: ${assessment.title}") // (Patel, 2025)
            onViewClick(assessment.fileUrl)
        }

        // Setup click listener for Submit button to start submission flow
        holder.btnSubmit.setOnClickListener {
            Log.d("StudentAssessmentAdapter", "Submit clicked for assessment: ${assessment.title}") // (Patel, 2025)
            onSubmitClick(assessment)
        }
    }
}

/*

Reference List

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at: https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

*/
