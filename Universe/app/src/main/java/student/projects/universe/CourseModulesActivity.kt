package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
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
    private lateinit var tvCourseCode: TextView
    private lateinit var tvModuleCount: TextView
    private lateinit var tvSort: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentModuleAdapter
    private val modules = mutableListOf<ModuleResponse>()
    private var courseId: String? = null
    private var courseTitle: String? = null

    companion object {
        private const val TAG = "CourseModulesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_modules)

        initializeViews()
        setupRecyclerView()
        loadIntentData()
        setupClickListeners()

        courseId?.let {
            loadModules(it)
        } ?: run {
            Log.e(TAG, "No course ID found — cannot load modules")
            Toast.makeText(this, "Error: No course information", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews() {
        // Header and navigation
        btnBack = findViewById(R.id.btnBack)

        // Course info card
        tvCourseTitle = findViewById(R.id.tvCourseTitle)
        tvCourseCode = findViewById(R.id.tvCourseCode)
        tvModuleCount = findViewById(R.id.tvModuleCount)

        // Modules section
        tvSort = findViewById(R.id.tvSort)
        recyclerView = findViewById(R.id.recyclerViewModules)
    }

    private fun setupRecyclerView() {
        adapter = StudentModuleAdapter(modules, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadIntentData() {
        courseTitle = intent.getStringExtra("COURSE_TITLE")
        courseId = intent.getStringExtra("COURSE_ID")

        updateCourseInfoUI()

        Log.d(TAG, "Opened CourseModulesActivity for: $courseTitle (ID: $courseId)")
    }

    private fun updateCourseInfoUI() {
        tvCourseTitle.text = courseTitle ?: "Course Title"
        tvCourseCode.text = "Course Code: $courseId"

        // Update module count (will be updated when modules load)
        tvModuleCount.text = "${modules.size} Modules"
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            onBackPressed()
        }

        tvSort.setOnClickListener {
            showSortOptions()
        }
    }

    private fun loadModules(courseId: String) {
        ApiClient.moduleApi.getModulesByCourse(courseId).enqueue(object : Callback<List<ModuleResponse>> {
            override fun onResponse(
                call: Call<List<ModuleResponse>>,
                response: Response<List<ModuleResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    modules.clear()
                    val apiModules = response.body()!!

                    if (apiModules.isEmpty()) {
                        Toast.makeText(this@CourseModulesActivity, "No modules available for this course", Toast.LENGTH_SHORT).show()
                        updateModuleCount()
                        return
                    }

                    // Load modules and check for assessments
                    var modulesProcessed = 0
                    apiModules.forEach { module ->
                        checkForNewAssessments(module) { updatedModule ->
                            // Use synchronized block to prevent race conditions
                            synchronized(modules) {
                                // Remove any existing module with same ID before adding
                                modules.removeAll { it.moduleID == updatedModule.moduleID }
                                modules.add(updatedModule)
                            }

                            modulesProcessed++

                            // Update UI when all modules are processed
                            if (modulesProcessed == apiModules.size) {
                                updateModuleCount()
                                adapter.notifyDataSetChanged()
                                Log.i(TAG, "Loaded ${modules.size} modules successfully for courseId=$courseId")
                            }
                        }
                    }

                } else {
                    Toast.makeText(this@CourseModulesActivity, "Failed to load modules", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "No modules found or empty response for courseId=$courseId")
                }
            }

            override fun onFailure(call: Call<List<ModuleResponse>>, t: Throwable) {
                Toast.makeText(this@CourseModulesActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to load modules: ${t.message}", t)
            }
        })
    }

    private fun checkForNewAssessments(module: ModuleResponse, callback: (ModuleResponse) -> Unit) {
        ApiClient.assessmentApi.getAssessmentsByModule(module.moduleID)
            .enqueue(object : Callback<List<AssessmentResponse>> {
                override fun onResponse(
                    call: Call<List<AssessmentResponse>>,
                    assessmentResponse: Response<List<AssessmentResponse>>
                ) {
                    val hasNewAssessment = !assessmentResponse.body().isNullOrEmpty()
                    val updatedModule = module.copy(hasNewAssessment = hasNewAssessment)
                    callback(updatedModule)
                }

                override fun onFailure(call: Call<List<AssessmentResponse>>, t: Throwable) {
                    Log.e(TAG, "Failed to check assessment for module ${module.moduleTitle}", t)
                    callback(module.copy(hasNewAssessment = false))
                }
            })
    }

    private fun updateModuleCount() {
        tvModuleCount.text = "${modules.size} Modules"
    }

    private fun showSortOptions() {
        // Create a simple sort dialog or dropdown
        val sortOptions = arrayOf("By Name (A-Z)", "By Name (Z-A)")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sort Modules")
            .setItems(sortOptions) { _, which ->
                when (which) {
                    0 -> sortModulesByName(ascending = true)
                    1 -> sortModulesByName(ascending = false)
                }
                Toast.makeText(this, "Sorted: ${sortOptions[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sortModulesByName(ascending: Boolean) {
        modules.sortBy { it.moduleTitle }
        if (!ascending) {
            modules.reverse()
        }
        adapter.notifyDataSetChanged()
    }

    /**
     * Optional: Call this function to refresh modules when returning from an assessment submission
     */
    override fun onResume() {
        super.onResume()
        courseId?.let {
            // Only refresh if needed - you might want to add a refresh condition
            if (modules.isEmpty()) {
                loadModules(it)
            }
        }
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