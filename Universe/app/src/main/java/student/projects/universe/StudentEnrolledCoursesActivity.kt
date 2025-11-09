package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.view.View
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
class StudentEnrolledCoursesActivity : AppCompatActivity() { // (Patel, 2025)

    // UI components
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EnrolledCourseAdapter
    private val enrolledCourses = mutableListOf<CourseResponse>()
    private lateinit var tvNoCourses: TextView
    // (Patel, 2025)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_enrolled_courses)
        Log.d("EnrolledCourses", "Activity started")

        // --- Initialize UI components ---
        tvNoCourses = findViewById(R.id.tvNoEnrolledCourses)
        recyclerView = findViewById(R.id.recyclerViewEnrolledCourses)

        // --- Setup Adapter ---
        adapter = EnrolledCourseAdapter(enrolledCourses, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        Log.d("EnrolledCourses", "RecyclerView and Adapter initialized")

        // --- Load enrolled courses from API ---
        loadEnrolledCourses()
    }

    /**
     * Loads the list of enrolled courses via API call.
     * Updates RecyclerView and handles empty state.
     */
    private fun loadEnrolledCourses() {
        Log.d("EnrolledCourses", "Loading enrolled courses from API") // (Patel, 2025)
        ApiClient.courseApi.getEnrolledCourses().enqueue(object : Callback<List<CourseResponse>> {
            override fun onResponse(
                call: Call<List<CourseResponse>>,
                response: Response<List<CourseResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    enrolledCourses.clear()
                    enrolledCourses.addAll(response.body()!!)
                    adapter.notifyDataSetChanged() // (Patel, 2025)
                    tvNoCourses.visibility = if (enrolledCourses.isEmpty()) View.VISIBLE else View.GONE
                    Log.d("EnrolledCourses", "Loaded ${enrolledCourses.size} enrolled courses")
                } else {
                    tvNoCourses.visibility = View.VISIBLE
                    Toast.makeText(this@StudentEnrolledCoursesActivity, "Failed to load enrolled courses", Toast.LENGTH_SHORT).show()
                    Log.e("EnrolledCourses", "Failed to load enrolled courses: HTTP ${response.code()}")
                }
            }

            // (Patel, 2025)
            override fun onFailure(call: Call<List<CourseResponse>>, t: Throwable) {
                tvNoCourses.visibility = View.VISIBLE
                Toast.makeText(this@StudentEnrolledCoursesActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("EnrolledCourses", "API call failed", t)
            }
        })
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