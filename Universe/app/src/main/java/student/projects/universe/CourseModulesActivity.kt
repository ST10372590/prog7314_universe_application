package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CourseModulesActivity : AppCompatActivity() {

    private lateinit var tvCourseTitle: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentModuleAdapter
    private val modules = mutableListOf<ModuleResponse>()
    private var courseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_modules)

        // Initialize UI components
        tvCourseTitle = findViewById(R.id.tvCourseTitle)
        recyclerView = findViewById(R.id.recyclerViewModules)

        // Setup RecyclerView with adapter
        adapter = StudentModuleAdapter(modules, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Get data passed from previous Activity
        val courseTitle = intent.getStringExtra("COURSE_TITLE")
        courseId = intent.getStringExtra("COURSE_ID")
        tvCourseTitle.text = courseTitle ?: "Modules"

        Log.d("CourseModulesActivity", "Opened CourseModulesActivity for: $courseTitle (ID: $courseId)")

        // Load modules
        courseId?.let {
            loadModules(it)
        } ?: Log.e("CourseModulesActivity", "No course ID found — cannot load modules")
    }

    /**
     * Loads modules from API.
     * If a module has a new assessment, it sets hasNewAssessment = true
     */
    private fun loadModules(courseId: String) {
        ApiClient.moduleApi.getModulesByCourse(courseId).enqueue(object : Callback<List<ModuleResponse>> {
            override fun onResponse(
                call: Call<List<ModuleResponse>>,
                response: Response<List<ModuleResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    modules.clear()
                    val apiModules = response.body()!!

                    // For each module, check if it has a new assessment asynchronously
                    apiModules.forEach { module ->
                        ApiClient.assessmentApi.getAssessmentsByModule(module.moduleID)
                            .enqueue(object : Callback<List<AssessmentResponse>> {
                                override fun onResponse(
                                    call: Call<List<AssessmentResponse>>,
                                    assessmentResponse: Response<List<AssessmentResponse>>
                                ) {
                                    val hasNew = !assessmentResponse.body().isNullOrEmpty()
                                    // Update module with hasNewAssessment flag
                                    val updatedModule = module.copy(hasNewAssessment = hasNew)

                                    // Remove old module if exists, then add updated one
                                    modules.removeAll { it.moduleID == updatedModule.moduleID }
                                    modules.add(updatedModule)

                                    // Notify adapter after each update
                                    adapter.notifyDataSetChanged()
                                }

                                override fun onFailure(call: Call<List<AssessmentResponse>>, t: Throwable) {
                                    Log.e("CourseModulesActivity", "Failed to check assessment for module ${module.moduleTitle}", t)
                                }
                            })
                    }

                    Log.i("CourseModulesActivity", "Loaded ${modules.size} modules successfully for courseId=$courseId")
                } else {
                    Toast.makeText(this@CourseModulesActivity, "No modules found", Toast.LENGTH_SHORT).show()
                    Log.w("CourseModulesActivity", "No modules found or empty response for courseId=$courseId")
                }
            }

            override fun onFailure(call: Call<List<ModuleResponse>>, t: Throwable) {
                Toast.makeText(this@CourseModulesActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("CourseModulesActivity", "Failed to load modules: ${t.message}", t)
            }
        })
    }

    /**
     * Utility function to determine if a module has a new assessment.
     * In a real app, you may call a dedicated API or check a flag from the response.
     */
    private fun checkIfNewAssessment(moduleId: String, callback: (Boolean) -> Unit) {
        ApiClient.assessmentApi.getAssessmentsByModule(moduleId).enqueue(object : Callback<List<AssessmentResponse>> {
            override fun onResponse(call: Call<List<AssessmentResponse>>, response: Response<List<AssessmentResponse>>) {
                callback(!response.body().isNullOrEmpty()) // true if at least one assessment exists
            }

            override fun onFailure(call: Call<List<AssessmentResponse>>, t: Throwable) {
                callback(false)
            }
        })
    }

    /**
     * Optional: Call this function to refresh modules when returning from an assessment submission
     */
    override fun onResume() {
        super.onResume()
        courseId?.let { loadModules(it) }
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