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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CourseDetailActivity : AppCompatActivity() { // (Patel, 2025)

    // UI components
    private lateinit var tvCourseTitle: TextView
    private lateinit var tvCourseDescription: TextView
    private lateinit var tvCredits: TextView
    private lateinit var tvNoModules: TextView
    private lateinit var recyclerModules: RecyclerView

    // Adapter and list for displaying modules // (Patel, 2025)
    private lateinit var adapter: ModuleAdapter
    private val moduleList = mutableListOf<ModuleResponse>()

    // Store course ID globally in this activity (Aram, 2023)
    private var courseId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        Log.d("CourseDetailActivity", "Activity created - initializing views")

        // Bind views from layout (Aram, 2023)
        tvCourseTitle = findViewById(R.id.tvCourseTitle)
        tvCourseDescription = findViewById(R.id.tvCourseDescription)
        tvCredits = findViewById(R.id.tvCredits)
        tvNoModules = findViewById(R.id.tvNoModules)
        recyclerModules = findViewById(R.id.recyclerModules)

        // Retrieve course data passed from previous activity (Aram, 2023)
        courseId = intent.getStringExtra("courseId") ?: ""
        val courseTitle = intent.getStringExtra("courseTitle") ?: "No Title"
        val courseDesc = intent.getStringExtra("courseDescription") ?: "No Description"
        val credits = intent.getIntExtra("credits", 0)

        Log.d("CourseDetailActivity", "Received courseId: $courseId, title: $courseTitle, credits: $credits")

        // Validate course ID (Aram, 2023)
        if (courseId.isBlank()) {
            Log.e("CourseDetailActivity", "Invalid course ID passed to activity")
            Toast.makeText(this, "Invalid course ID passed!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Display course details (Aram, 2023)
        tvCourseTitle.text = courseTitle
        tvCourseDescription.text = courseDesc
        tvCredits.text = "Credits: $credits"

        Log.d("CourseDetailActivity", "Displayed course details successfully")

        // Setup RecyclerView for modules (Aram, 2023)
        recyclerModules.layoutManager = LinearLayoutManager(this)
        adapter = ModuleAdapter(moduleList) { module ->
            // ✅ Added detailed logging for module click
            Log.d("CourseDetailActivity", "Module clicked: ${module.moduleTitle}")
            Log.d("CourseDetailActivity", "courseID for module: ${module.courseID}") // 👈 Added line

            // Navigate to ModuleTeachMenuActivity with course/module details // (Patel, 2025)
            val intent = Intent(this, ModuleTeachMenuActivity::class.java)
            intent.putExtra("courseId", module.courseID)
            intent.putExtra("moduleId", module.moduleID) // ✅ FIX: pass moduleId correctly
            intent.putExtra("moduleTitle", module.moduleTitle)
            startActivity(intent)

            Log.d("CourseDetailActivity", "Navigating to ModuleTeachMenuActivity with courseId=${module.courseID}, moduleId=${module.moduleID}, moduleTitle=${module.moduleTitle}")

        }
        recyclerModules.adapter = adapter

        Log.d("CourseDetailActivity", "RecyclerView setup complete, loading modules...")

        // Load modules associated with this course // (Patel, 2025)
        loadModules(courseId)
    }

    // Fetch modules from API based on course ID (Aram, 2023)
    private fun loadModules(courseId: String) {
        Log.d("CourseDetailActivity", "Requesting modules for courseId=$courseId")

        ApiClient.moduleApi.getModulesByCourse(courseId).enqueue(object : Callback<List<ModuleResponse>> {
            override fun onResponse(
                call: Call<List<ModuleResponse>>,
                response: Response<List<ModuleResponse>> // (Aram, 2023)
            ) {
                Log.d("CourseDetailActivity", "API response received: HTTP ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val modules = response.body()!!
                    Log.d("CourseDetailActivity", "Modules loaded successfully: ${modules.size} found")

                    moduleList.clear() // (Patel, 2025)
                    moduleList.addAll(modules)
                    adapter.notifyDataSetChanged()

                    // Show "No Modules" message if none exist (Aram, 2023)
                    tvNoModules.visibility = if (modules.isEmpty()) View.VISIBLE else View.GONE
                    if (modules.isEmpty()) {
                        Log.w("CourseDetailActivity", "No modules available for this course")
                    }
                } else {
                    Log.e("CourseDetailActivity", "Failed to load modules. HTTP code: ${response.code()}")
                    Toast.makeText(
                        this@CourseDetailActivity,
                        "Failed to load modules: HTTP ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<ModuleResponse>>, t: Throwable) { // (Patel, 2025)
                Log.e("CourseDetailActivity", "Error loading modules: ${t.localizedMessage}", t)
                Toast.makeText(
                    this@CourseDetailActivity,
                    "Error loading modules: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
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