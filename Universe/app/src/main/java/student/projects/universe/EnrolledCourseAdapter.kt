package student.projects.universe

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EnrolledCourseAdapter( // (Appmaster, 2023)
    private val courses: List<CourseResponse>,
    private val context: Context
) : RecyclerView.Adapter<EnrolledCourseAdapter.CourseViewHolder>() {

    // ViewHolder class that holds references to UI components in each list item (Patel, 2025)
    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCourseTitle: TextView = itemView.findViewById(R.id.tvCourseTitle)
        val tvCourseDescription: TextView = itemView.findViewById(R.id.tvCourseDescription)
        val tvCredits: TextView = itemView.findViewById(R.id.tvCredits)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val btnLearn: Button = itemView.findViewById(R.id.btnLearn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        // Inflate the layout for each enrolled course item (Patel, 2025)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_enrolled_course, parent, false)
        Log.d("EnrolledCourseAdapter", "Created ViewHolder for enrolled course item")
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]

        // Bind data from the course object to the UI components (Patel, 2025)
        holder.tvCourseTitle.text = course.courseTitle
        holder.tvCourseDescription.text = course.courseDescription
        holder.tvCredits.text = "Credits: ${course.credits}"
        holder.tvDuration.text = "${course.startDate} - ${course.endDate}"

        Log.d(
            "EnrolledCourseAdapter",
            "Binding course: ${course.courseTitle}, ID: ${course.courseID}, Credits: ${course.credits}"
        )

        // Handle click on "Learn" button — navigate to CourseModulesActivity // (Appmaster, 2023)
        holder.btnLearn.setOnClickListener {
            Log.i(
                "EnrolledCourseAdapter",
                "Learn button clicked for course: ${course.courseTitle} (ID: ${course.courseID})"
            )

            // (Patel, 2025)
            val intent = Intent(context, CourseModulesActivity::class.java)
            intent.putExtra("COURSE_TITLE", course.courseTitle)
            intent.putExtra("COURSE_ID", course.courseID)
            context.startActivity(intent)

            Log.d("EnrolledCourseAdapter", "Navigating to CourseModulesActivity with course ID: ${course.courseID}")
        }
    }

    override fun getItemCount(): Int {
        // Log the number of courses being displayed // (Appmaster, 2023)
        Log.d("EnrolledCourseAdapter", "Total enrolled courses: ${courses.size}")
        return courses.size
    }

    // Utility function to show a dialog with a list of module titles for a selected course (Patel, 2025)
    private fun showModulesDialog(courseTitle: String, modules: List<ModuleResponse>) {
        Log.d("EnrolledCourseAdapter", "Displaying module dialog for course: $courseTitle")
        // (Appmaster, 2023)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Modules in $courseTitle") // (Patel, 2025)

        val moduleNames = if (modules.isEmpty()) {
            Log.w("EnrolledCourseAdapter", "No modules available for $courseTitle")
            "No modules available."
        } else {
            modules.joinToString("\n") { "• ${it.moduleTitle}" }
        }

        builder.setMessage(moduleNames) // (Patel, 2025)
        builder.setPositiveButton("OK") { dialog, _ ->
            Log.d("EnrolledCourseAdapter", "Modules dialog dismissed for $courseTitle")
            dialog.dismiss()
        }
        builder.show()
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