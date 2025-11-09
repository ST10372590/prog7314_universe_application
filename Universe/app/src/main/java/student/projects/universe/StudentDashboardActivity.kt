package student.projects.universe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * StudentDashboardActivity displays the student’s enrolled courses and all available courses.
 * Allows students to enroll in courses and view their modules.
 */
class StudentDashboardActivity : AppCompatActivity() { // (Patel, 2025)

    // UI components
    private lateinit var recyclerViewEnrolled: RecyclerView
    private lateinit var recyclerViewAll: RecyclerView
    private lateinit var adapterEnrolled: CourseAdapter
    private lateinit var adapterAll: CourseAdapter
    private val enrolledCourses = mutableListOf<CourseResponse>()
    private val allCourses = mutableListOf<CourseResponse>()
    private lateinit var tvNoEnrolled: TextView
    // (Patel, 2025)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)
        Log.d("StudentDashboard", "Activity started")

        // --- Initialize UI components ---
        tvNoEnrolled = findViewById(R.id.tvNoEnrolledCourses)
        recyclerViewEnrolled = findViewById(R.id.recyclerViewEnrolledCourses)
        recyclerViewAll = findViewById(R.id.recyclerViewAllCourses)
        val btnMySubmissions = findViewById<ImageButton>(R.id.btnMySubmissions)
        val btnNotifications = findViewById<ImageButton>(R.id.btnNotifications)

        // --- Add Button Functionality ---
        btnMySubmissions.setOnClickListener {
            Log.d("StudentDashboard", "My Submissions button clicked")
            val intent = Intent(this, StudentSubmissionsActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Opening My Submissions...", Toast.LENGTH_SHORT).show()
        }

        // --- Setup Adapters // (Patel, 2025)
        adapterEnrolled = CourseAdapter(enrolledCourses, "Student") { course ->
            Toast.makeText(this, "Viewing course: ${course.courseTitle}", Toast.LENGTH_SHORT).show()
            Log.d("StudentDashboard", "Clicked enrolled course: ${course.courseTitle}")
        }

        adapterAll = CourseAdapter(allCourses, "Student") { course ->
            Log.d("StudentDashboard", "Clicked available course: ${course.courseTitle}")
            showEnrollmentDialog(this, course)
        }

        // (Patel, 2025)

        recyclerViewEnrolled.layoutManager = LinearLayoutManager(this)
        recyclerViewEnrolled.adapter = adapterEnrolled

        recyclerViewAll.layoutManager = LinearLayoutManager(this)
        recyclerViewAll.adapter = adapterAll

        // --- Load Courses from API ---
        loadEnrolledCourses()
        loadAllCourses()

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
     * Load courses the student is already enrolled in
     */
    private fun loadEnrolledCourses() { // (Patel, 2025)
        Log.d("StudentDashboard", "Loading enrolled courses")
        ApiClient.courseApi.getEnrolledCourses().enqueue(object : Callback<List<CourseResponse>> {
            override fun onResponse(
                call: Call<List<CourseResponse>>,
                response: Response<List<CourseResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    enrolledCourses.clear()
                    enrolledCourses.addAll(response.body()!!)
                    adapterEnrolled.notifyDataSetChanged()
                    tvNoEnrolled.visibility = if (enrolledCourses.isEmpty()) View.VISIBLE else View.GONE
                    Log.d("StudentDashboard", "Enrolled courses loaded: ${enrolledCourses.size}")
                } else { // (Patel, 2025)
                    tvNoEnrolled.visibility = View.VISIBLE
                    Toast.makeText(this@StudentDashboardActivity, "Failed to load enrolled courses", Toast.LENGTH_SHORT).show()
                    Log.e("StudentDashboard", "Failed to load enrolled courses: HTTP ${response.code()}")
                }
            }

            // (Patel, 2025)
            override fun onFailure(call: Call<List<CourseResponse>>, t: Throwable) {
                tvNoEnrolled.visibility = View.VISIBLE
                Toast.makeText(this@StudentDashboardActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("StudentDashboard", "Error loading enrolled courses", t)
            }
        })
    }

    /**
     * Load all available courses
     */
    private fun loadAllCourses() { // (Patel, 2025)
        Log.d("StudentDashboard", "Loading all courses")
        ApiClient.courseApi.getAllCourses().enqueue(object : Callback<List<CourseResponse>> {
            override fun onResponse(call: Call<List<CourseResponse>>, response: Response<List<CourseResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    allCourses.clear()
                    allCourses.addAll(response.body()!!)
                    adapterAll.notifyDataSetChanged()
                    Log.d("StudentDashboard", "All courses loaded: ${allCourses.size}")
                } else { // (Patel, 2025)
                    Toast.makeText(this@StudentDashboardActivity, "Failed to load courses", Toast.LENGTH_SHORT).show()
                    Log.e("StudentDashboard", "Failed to load all courses: HTTP ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<CourseResponse>>, t: Throwable) { // (Patel, 2025)
                Toast.makeText(this@StudentDashboardActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("StudentDashboard", "Error loading all courses", t)
            }
        })
    }

    /**
     * Show dialog to confirm enrollment in a course
     */
    private fun showEnrollmentDialog(context: Context, course: CourseResponse) { // (Patel, 2025)
        Log.d("StudentDashboard", "Showing enrollment dialog for course: ${course.courseTitle}")
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Confirm Enrollment") // (Patel, 2025)
        builder.setMessage("Are you sure you want to enroll in ${course.courseTitle}?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            enrollInCourse(course)
            Toast.makeText(context, "Processing enrollment...", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show() // (Patel, 2025)
    }

    /**
     * Enroll student in the selected course via API
     */
    private fun enrollInCourse(course: CourseResponse) { // (Patel, 2025)
        Log.d("EnrollmentDebug", "Attempting to enroll in courseID: ${course.courseID}")

        if (course.courseID.isBlank()) { // (Patel, 2025)
            Toast.makeText(this, "CourseID is blank! Cannot enroll.", Toast.LENGTH_SHORT).show()
            Log.e("EnrollmentDebug", "CourseID is blank")
            return
        }

        ApiClient.courseApi.enrollCourse(course.courseID).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) { // (Patel, 2025)
                    Toast.makeText(this@StudentDashboardActivity, "Successfully enrolled in ${course.courseTitle}", Toast.LENGTH_SHORT).show()
                    Log.d("EnrollmentDebug", "Enrollment successful for courseID: ${course.courseID}")
                    loadEnrolledCourses()
                    loadModulesForCourse(course.courseID, course.courseTitle)
                } else { // (Patel, 2025)
                    val errorBody = response.errorBody()?.string()
                    Log.e("EnrollmentError", "Code: ${response.code()}, Body: $errorBody")
                    Toast.makeText(this@StudentDashboardActivity, "Enrollment failed: $errorBody", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) { // (Patel, 2025)
                Toast.makeText(this@StudentDashboardActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("EnrollmentError", "API call failed", t)
            }
        })
    }

    /**
     * Load modules for the enrolled course and show a dialog listing them
     */
    private fun loadModulesForCourse(courseId: String, courseTitle: String) { // (Patel, 2025)
        Log.d("StudentDashboard", "Loading modules for course: $courseTitle")
        ApiClient.moduleApi.getModulesByCourse(courseId).enqueue(object : Callback<List<ModuleResponse>> {
            override fun onResponse(call: Call<List<ModuleResponse>>, response: Response<List<ModuleResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val modules = response.body()!! // (Patel, 2025)
                    Log.d("StudentDashboard", "Modules loaded: ${modules.size}")
                    showModulesDialog(courseTitle, modules)
                } else {
                    Toast.makeText(this@StudentDashboardActivity, "No modules found for this course", Toast.LENGTH_SHORT).show()
                    Log.e("StudentDashboard", "No modules found for course: $courseTitle") // (Patel, 2025)
                }
            }

            override fun onFailure(call: Call<List<ModuleResponse>>, t: Throwable) { // (Patel, 2025)
                Toast.makeText(this@StudentDashboardActivity, "Error loading modules: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("StudentDashboard", "Error loading modules", t)
            }
        })
    }

    /**
     * Show dialog listing modules of the enrolled course
     */
    private fun showModulesDialog(courseTitle: String, modules: List<ModuleResponse>) { // (Patel, 2025)
        Log.d("StudentDashboard", "Showing modules dialog for course: $courseTitle")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Modules in $courseTitle") // (Patel, 2025)
        val moduleNames = modules.joinToString("\n") { "• ${it.moduleTitle}" }
        builder.setMessage("You are now enrolled in $courseTitle.\n\nModules:\n$moduleNames")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
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