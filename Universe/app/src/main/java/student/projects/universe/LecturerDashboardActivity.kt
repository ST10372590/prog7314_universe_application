
package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LecturerDashboardActivity : AppCompatActivity() {

    // RecyclerViews for displaying courses // (Patel, 2025)
    private lateinit var recyclerViewAssigned: RecyclerView
    private lateinit var recyclerViewAll: RecyclerView

    // Adapters for RecyclerViews
    private lateinit var adapterAssigned: CourseAdapter
    private lateinit var adapterAll: CourseAdapter

    // Lists to hold course data // (Patel, 2025)
    private val assignedCourses = mutableListOf<CourseResponse>()
    private val allCourses = mutableListOf<CourseResponse>()

    // TextViews to show messages when no courses are available // (Patel, 2025)
    private lateinit var tvNoAssigned: TextView
    private lateinit var tvNoAllCourses: TextView
    private lateinit var tvLecturerName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecturer_dashboard)

        // Bind views from layout // (Patel, 2025)
        recyclerViewAssigned = findViewById(R.id.recyclerViewAssignedCourses)
        recyclerViewAll = findViewById(R.id.recyclerViewAllCourses)
        tvNoAssigned = findViewById(R.id.tvNoAssignedCourses)
        tvNoAllCourses = findViewById(R.id.tvNoCoursesAvailable)
        tvLecturerName = findViewById(R.id.tvLecturerName)

        // Set lecturer name dynamically using Intent extra
        val lecturerName = intent.getStringExtra("lastName") ?: "Lecturer"
        tvLecturerName.text = lecturerName
        Log.d("LecturerDashboard", "Lecturer name set: $lecturerName")

        // Adapter for assigned courses - clicking opens ModuleTeachMenuActivity // (Patel, 2025)
        adapterAssigned = CourseAdapter(assignedCourses, "Lecturer") { course ->
            Log.d("LecturerDashboard", "Selected assigned course: ID=${course.courseID}, Title=${course.courseTitle}")
            val intent = Intent(this, ModuleTeachMenuActivity::class.java)
            intent.putExtra("courseId", course.courseID)
            intent.putExtra("courseTitle", course.courseTitle)
            startActivity(intent)
        }

        // Adapter for all courses - clicking shows a simple Toast // (Patel, 2025)
        adapterAll = CourseAdapter(allCourses, "Lecturer") { course ->
            Log.d("LecturerDashboard", "Viewing all course: ID=${course.courseID}, Title=${course.courseTitle}")
            Toast.makeText(this, "Viewing course: ${course.courseTitle}", Toast.LENGTH_SHORT).show()
        }

        // Setup RecyclerViews with LinearLayoutManager and adapters // (Patel, 2025)
        recyclerViewAssigned.layoutManager = LinearLayoutManager(this)
        recyclerViewAssigned.adapter = adapterAssigned
        Log.d("LecturerDashboard", "Assigned courses RecyclerView initialized")

        recyclerViewAll.layoutManager = LinearLayoutManager(this)
        recyclerViewAll.adapter = adapterAll
        Log.d("LecturerDashboard", "All courses RecyclerView initialized")

        // Load courses from API
        loadCourses()

        // Bottom navigation setup // (Patel, 2025)
        // --- Bottom Navigation // (Patel, 2025)
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show()
                    Log.d("StudentDashboard", "Bottom nav: Home selected")
                    true
                }
                R.id.nav_courses -> {
                    val intent = Intent(this, StudentEnrolledCoursesActivity::class.java)
                    startActivity(intent)
                    Log.d("StudentDashboard", "Bottom nav: Courses selected")
                    true
                } // (Patel, 2025)
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    Log.d("StudentDashboard", "Bottom nav: Settings selected")
                    true
                } // (Patel, 2025)
                R.id.nav_gamification -> {
                    val intent = Intent(this, GamificationActivity::class.java)
                    startActivity(intent)
                    Log.d("StudentDashboard", "Bottom nav: Gamification selected")
                    true
                } // (Patel, 2025)
                R.id.nav_messages -> {
                    val intent = Intent(this, CommunicationHubActivity::class.java)
                    startActivity(intent)
                    Log.d("StudentDashboard", "Bottom nav: Messages selected")
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Loads courses from the API.
     * Filters courses assigned to the current lecturer and updates RecyclerViews. // (Patel, 2025)
     */
    private fun loadCourses() {
        Log.d("LecturerDashboard", "Loading courses from API...")
        ApiClient.courseApi.getAllCourses().enqueue(object : retrofit2.Callback<List<CourseResponse>> {
            override fun onResponse(
                call: retrofit2.Call<List<CourseResponse>>,
                response: retrofit2.Response<List<CourseResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val all = response.body()!!
                    val currentLecturerId = ApiClient.currentUserId
                    Log.d("LecturerDashboard", "API response received: ${all.size} courses")

                    // Clear previous data // (Patel, 2025)
                    assignedCourses.clear()
                    allCourses.clear()

                    // Filter courses assigned to the current lecturer
                    val assigned = all.filter { it.lecturerID == currentLecturerId }
                    assignedCourses.addAll(assigned)
                    adapterAssigned.notifyDataSetChanged()
                    Log.d("LecturerDashboard", "Assigned courses updated: ${assignedCourses.size} courses")

                    // Add all courses to allCourses list // (Patel, 2025)
                    allCourses.addAll(all)
                    adapterAll.notifyDataSetChanged()
                    Log.d("LecturerDashboard", "All courses list updated: ${allCourses.size} courses")

                    // Show or hide "no courses" messages // (Patel, 2025)
                    tvNoAssigned.visibility = if (assigned.isEmpty()) View.VISIBLE else View.GONE
                    tvNoAllCourses.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE

                    if (assigned.isEmpty()) {
                        Log.d("LecturerDashboard", "No assigned courses for lecturer")
                        Toast.makeText(
                            this@LecturerDashboardActivity,
                            "You currently have no assigned courses.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else { // (Patel, 2025)
                    Log.e("LecturerDashboard", "Failed to load courses. Response code: ${response.code()}")
                    Toast.makeText(
                        this@LecturerDashboardActivity,
                        "Failed to load courses.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<List<CourseResponse>>, t: Throwable) { // (Patel, 2025)
                Log.e("LecturerDashboard", "Error loading courses: ${t.message}", t)
                Toast.makeText(
                    this@LecturerDashboardActivity, // (Patel, 2025)
                    "Error: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */