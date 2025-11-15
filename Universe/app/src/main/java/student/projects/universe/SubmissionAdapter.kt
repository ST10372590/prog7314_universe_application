package student.projects.universe

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class SubmissionAdapter(
    private var submissions: List<SubmissionResponse>,
    private val onItemClick: (SubmissionResponse) -> Unit
) : RecyclerView.Adapter<SubmissionAdapter.SubmissionViewHolder>() {

    inner class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvSubmissionTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvSubmissionDate)
        val tvGrade: TextView = itemView.findViewById(R.id.tvSubmissionGrade)
        val tvFeedback: TextView = itemView.findViewById(R.id.tvSubmissionFeedback)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_submission, parent, false)
        return SubmissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        val submission = submissions[position]

        holder.tvTitle.text = submission.assessmentTitle
        holder.tvDate.text = "Submitted: ${submission.submittedAt}"

        // Handle grade display
        if (submission.grade != null && submission.grade >= 0) {
            holder.tvGrade.text = "Grade: ${submission.grade}"
            holder.tvGrade.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark))
        } else {
            holder.tvGrade.text = "Grade: Pending"
            holder.tvGrade.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_orange_dark))
        }

        // Handle feedback display
        holder.tvFeedback.text = if (!submission.feedback.isNullOrEmpty()) {
            "Feedback: ${submission.feedback}"
        } else {
            "No feedback yet"
        }

        // View details button
        holder.btnViewDetails.setOnClickListener {
            onItemClick(submission)
        }

        // Entire item click
        holder.itemView.setOnClickListener {
            onItemClick(submission)
        }
    }

    override fun getItemCount(): Int = submissions.size

    fun updateSubmissions(newSubmissions: List<SubmissionResponse>) {
        this.submissions = newSubmissions
        notifyDataSetChanged()
    }
}