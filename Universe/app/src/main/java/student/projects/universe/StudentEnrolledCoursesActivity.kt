package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * StudentEnrolledCoursesActivity displays all courses that the student is currently enrolled in.
 * Uses a RecyclerView to list courses and handles API loading and error handling.
 */
class StudentEnrolledCoursesActivity : AppCompatActivity() {

    // UI components
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EnrolledCourseAdapter
    private val enrolledCourses = mutableListOf<CourseResponse>()

    // New UI elements from enhanced layout
    private lateinit var tvNoCourses: View
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCourseCount: TextView
    private lateinit var tvTotalCourses: TextView
    private lateinit var tvActiveCourses: TextView
    private lateinit var tvCompletedCourses: TextView
    private lateinit var btnBrowseCourses: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_enrolled_courses)
        Log.d("EnrolledCourses", "Activity started")

        // --- Initialize all UI components ---
        initializeViews()

        // --- Setup Adapter ---
        adapter = EnrolledCourseAdapter(enrolledCourses, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        Log.d("EnrolledCourses", "RecyclerView and Adapter initialized")

        // --- Setup click listeners ---
        setupClickListeners()

        // --- Load enrolled courses from API ---
        loadEnrolledCourses()
    }

    /**
     * Initialize all views from the enhanced layout
     */
    private fun initializeViews() {
        tvNoCourses = findViewById(R.id.tvNoEnrolledCourses)
        recyclerView = findViewById(R.id.recyclerViewEnrolledCourses)
        progressBar = findViewById(R.id.progressBar)
        tvCourseCount = findViewById(R.id.tvCourseCount)
        tvTotalCourses = findViewById(R.id.tvTotalCourses)
        tvActiveCourses = findViewById(R.id.tvActiveCourses)
        tvCompletedCourses = findViewById(R.id.tvCompletedCourses)
        btnBrowseCourses = findViewById(R.id.btnBrowseCourses)

        // Initialize stats with zeros
        updateStatsCards(0, 0, 0)
    }

    /**
     * Setup click listeners for interactive elements
     */
    private fun setupClickListeners() {
        // Browse Courses button in empty state
        btnBrowseCourses.setOnClickListener {
            Log.d("EnrolledCourses", "Browse Courses button clicked")
            val intent = Intent(this, StudentDashboardActivity::class.java)
            startActivity(intent)
            finish() // Optional: close this activity when going back to browse
        }
    }

    /**
     * Loads the list of enrolled courses via API call.
     * Updates RecyclerView and handles empty state.
     */
    private fun loadEnrolledCourses() {
        Log.d("EnrolledCourses", "Loading enrolled courses from API")
        showLoading(true)

        ApiClient.courseApi.getEnrolledCourses().enqueue(object : Callback<List<CourseResponse>> {
            override fun onResponse(
                call: Call<List<CourseResponse>>,
                response: Response<List<CourseResponse>>
            ) {
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    enrolledCourses.clear()
                    enrolledCourses.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()

                    // Update UI based on data
                    updateUIAfterDataLoad(enrolledCourses)

                    Log.d("EnrolledCourses", "Loaded ${enrolledCourses.size} enrolled courses")
                } else {
                    showEmptyState(true)
                    Toast.makeText(this@StudentEnrolledCoursesActivity, "Failed to load enrolled courses", Toast.LENGTH_SHORT).show()
                    Log.e("EnrolledCourses", "Failed to load enrolled courses: HTTP ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<CourseResponse>>, t: Throwable) {
                showLoading(false)
                showEmptyState(true)
                Toast.makeText(this@StudentEnrolledCoursesActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("EnrolledCourses", "API call failed", t)
            }
        })
    }

    /**
     * Update UI after data is loaded
     */
    private fun updateUIAfterDataLoad(courses: List<CourseResponse>) {
        val totalCourses = courses.size
        val activeCourses = courses.count { it.isActive ?: true } // Count active courses
        val completedCourses = courses.count { !(it.isActive ?: true) } // Count completed courses

        // Update stats cards
        updateStatsCards(totalCourses, activeCourses, completedCourses)

        // Update header course count
        tvCourseCount.text = if (totalCourses == 1) "1 Course" else "$totalCourses Courses"

        // Show/hide empty state
        showEmptyState(totalCourses == 0)

        // Show/hide courses list
        recyclerView.visibility = if (totalCourses > 0) View.VISIBLE else View.GONE
    }

    /**
     * Update statistics cards with current counts
     */
    private fun updateStatsCards(total: Int, active: Int, completed: Int) {
        tvTotalCourses.text = total.toString()
        tvActiveCourses.text = active.toString()
        tvCompletedCourses.text = completed.toString()

        Log.d("EnrolledCourses", "Stats updated - Total: $total, Active: $active, Completed: $completed")
    }

    /**
     * Show or hide loading progress bar
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            tvNoCourses.visibility = View.GONE
        }
    }

    /**
     * Show or hide empty state
     */
    private fun showEmptyState(show: Boolean) {
        tvNoCourses.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    /**
     * Refresh data when activity resumes (optional)
     */
    override fun onResume() {
        super.onResume()
        // Optional: Refresh data when returning to this activity
        // loadEnrolledCourses()
    }
}

/*

Reference List

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at:  https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */