package student.projects.universe

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter(
    private val courses: List<CourseResponse>,
    private val role: String,
    private val onClick: (CourseResponse) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvCourseTitle)
        val description: TextView = itemView.findViewById(R.id.tvCourseDescription)
        val actionBtn: Button = itemView.findViewById(R.id.btnCourseAction)
        val courseCode: TextView = itemView.findViewById(R.id.tvCourseCode)
        val studentCount: TextView = itemView.findViewById(R.id.tvStudentCount)
        val moduleCount: TextView = itemView.findViewById(R.id.tvModuleCount)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val statusIndicator: View = itemView.findViewById(R.id.viewStatusIndicator)
        val courseIcon: ImageView = itemView.findViewById(R.id.ivCourseIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        Log.d("CourseAdapter", "Creating new ViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        Log.d("CourseAdapter", "Binding course: ${course.courseTitle}, Role: $role")

        // Bind basic course data
        holder.title.text = course.courseTitle
        holder.description.text = course.courseDescription

        // Set course code (use first 6 characters of courseID or courseTitle)
        holder.courseCode.text = course.courseID.take(6).uppercase()

        // Set student count (you might need to fetch this from your API)
        holder.studentCount.text = "0" // Placeholder - replace with actual data

        // Set module count (you might need to fetch this from your API)
        holder.moduleCount.text = "0" // Placeholder - replace with actual data

        // Set status based on course data
        val isActive = course.isActive ?: true
        if (isActive) {
            holder.status.text = "Active"
            holder.statusIndicator.setBackgroundResource(R.drawable.status_active)
        } else {
            holder.status.text = "Inactive"
            holder.statusIndicator.setBackgroundResource(R.drawable.status_inactive)
        }

        // Set course icon based on course type or category
        setCourseIcon(holder.courseIcon, course)

        // Set button behavior based on user role
        when (role) {
            "Student" -> {
                // Check if student is already enrolled in this course
                val isEnrolled = course.isEnrolled ?: false

                if (isEnrolled) {
                    // Student is already enrolled - show disabled state
                    holder.actionBtn.text = "Enrolled"
                    holder.actionBtn.isEnabled = false
                    holder.actionBtn.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.colorDisabled))
                    holder.actionBtn.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorTextDisabled))
                } else {
                    // Student can enroll - show active state
                    holder.actionBtn.text = "Enroll"
                    holder.actionBtn.isEnabled = true
                    holder.actionBtn.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary))
                    holder.actionBtn.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
                }

                holder.actionBtn.visibility = View.VISIBLE

                holder.actionBtn.setOnClickListener {
                    Log.d("CourseAdapter", "Enroll button clicked for course: ${course.courseTitle}")
                    // Pass a callback to update the UI after successful enrollment
                    onClick(course)
                }

                // Student can click entire card to view course details
                holder.itemView.setOnClickListener {
                    Log.d("CourseAdapter", "Course card clicked by student: ${course.courseTitle}")
                    onClick(course)
                }
            }
            "Lecturer" -> {
                holder.actionBtn.text = "Manage Course"
                holder.actionBtn.visibility = View.VISIBLE
                holder.actionBtn.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.colorTeal))

                holder.actionBtn.setOnClickListener {
                    Log.d("CourseAdapter", "Manage Course clicked for: ${course.courseTitle}")
                    val intent = Intent(holder.itemView.context, CourseDetailActivity::class.java).apply {
                        putExtra("courseId", course.courseID)
                        putExtra("courseTitle", course.courseTitle)
                        putExtra("courseDescription", course.courseDescription)
                        putExtra("credits", course.credits)
                        putExtra("lecturerId", course.lecturerID)
                    }
                    holder.itemView.context.startActivity(intent)
                }

                // Lecturer can click entire card for quick access
                holder.itemView.setOnClickListener {
                    Log.d("CourseAdapter", "Course card clicked by lecturer: ${course.courseTitle}")
                    val intent = Intent(holder.itemView.context, ModuleTeachMenuActivity::class.java).apply {
                        putExtra("courseId", course.courseID)
                        putExtra("courseTitle", course.courseTitle)
                    }
                    holder.itemView.context.startActivity(intent)
                }
            }
            else -> {
                holder.actionBtn.visibility = View.GONE
                Log.d("CourseAdapter", "Unknown role: $role - hiding action button")
            }
        }
    }

    override fun getItemCount(): Int {
        Log.d("CourseAdapter", "Total courses displayed: ${courses.size}")
        return courses.size
    }

    // Method to update enrollment status for a specific course
    // Add this method to your CourseAdapter class
    fun updateEnrollmentStatus(courseId: String, isEnrolled: Boolean) {
        val updatedCourses = courses.map { course ->
            if (course.courseID == courseId) {
                // Create a new course object with updated enrollment status
                course.copy(isEnrolled = isEnrolled)
            } else {
                course
            }
        }

        // Update the courses list
        if (courses is MutableList) {
            (courses as MutableList<CourseResponse>).clear()
            (courses as MutableList<CourseResponse>).addAll(updatedCourses)
            notifyDataSetChanged()
            Log.d("CourseAdapter", "Updated enrollment status for course: $courseId to $isEnrolled")
        }
    }

    private fun setCourseIcon(imageView: ImageView, course: CourseResponse) {

        // Using a default icon
        imageView.setImageResource(R.drawable.ic_course_default)

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