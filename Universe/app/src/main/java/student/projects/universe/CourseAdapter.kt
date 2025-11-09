package student.projects.universe

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter( // (Patel, 2025)
    private val courses: List<CourseResponse>, // List of course data to display
    private val role: String, // User role: "Student" or "Lecturer"
    private val onClick: (CourseResponse) -> Unit // Callback for item interaction
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    // ViewHolder pattern for better RecyclerView performance // (Appmaster, 2023)
    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvCourseTitle)
        val description: TextView = itemView.findViewById(R.id.tvCourseDescription)
        val actionBtn: Button = itemView.findViewById(R.id.btnCourseAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder { // (Patel, 2025)
        Log.d("CourseAdapter", "Creating new ViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        Log.d("CourseAdapter", "Binding course: ${course.courseTitle}, Role: $role")

        // Bind course data to UI elements
        holder.title.text = course.courseTitle
        holder.description.text = course.courseDescription

        // Set button behavior based on user role // (Appmaster, 2023)
        when (role) {
            "Student" -> {
                holder.actionBtn.text = "Enroll"
                holder.actionBtn.visibility = View.VISIBLE
                Log.d("CourseAdapter", "Student role detected - showing Enroll button for ${course.courseTitle}")

                holder.actionBtn.setOnClickListener {
                    Log.d("CourseAdapter", "Enroll button clicked for course: ${course.courseTitle}")
                    // Trigger callback to handle enrollment logic externally
                    onClick(course)
                }
            }
            "Lecturer" -> { // (Patel, 2025)
                holder.actionBtn.text = "View/Manage"
                holder.actionBtn.visibility = View.VISIBLE
                Log.d("CourseAdapter", "Lecturer role detected - showing Manage button for ${course.courseTitle}")

                holder.actionBtn.setOnClickListener {
                    Log.d("CourseAdapter", "View/Manage clicked for course: ${course.courseTitle}")

                    // Open detailed course management activity for lecturers // (Patel, 2025)
                    val intent = Intent(holder.itemView.context, CourseDetailActivity::class.java)
                    intent.putExtra("courseId", course.courseID)
                    intent.putExtra("courseTitle", course.courseTitle)
                    intent.putExtra("courseDescription", course.courseDescription)
                    intent.putExtra("credits", course.credits)
                    holder.itemView.context.startActivity(intent)
                    // (Appmaster, 2023)
                    Log.d("CourseAdapter", "Navigating to CourseDetailActivity for course: ${course.courseTitle}")
                }
            }
            else -> {
                holder.actionBtn.visibility = View.GONE
                Log.d("CourseAdapter", "Unknown role: $role - hiding action button")
            }
        }
    }
// (Patel, 2025)
    override fun getItemCount(): Int {
        Log.d("CourseAdapter", "Total courses displayed: ${courses.size}")
        return courses.size
    }
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