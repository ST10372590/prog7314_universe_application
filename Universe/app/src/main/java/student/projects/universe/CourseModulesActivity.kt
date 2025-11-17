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

/**
 * Activity to display a list of modules for a selected course.
 * Features loading modules from API, checking if modules have new assessments,
 * sorting modules, and displaying course details.
 *
 * References:
 * - Retrofit API usage per Aram (2023)
 * - Kotlin collection operations and callbacks (Patel, 2025)
 */
class CourseModulesActivity : AppCompatActivity() {

    // UI elements for displaying course info and modules list
    private lateinit var tvCourseTitle: TextView
    private lateinit var tvCourseCode: TextView
    private lateinit var tvModuleCount: TextView
    private lateinit var tvSort: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentModuleAdapter

    // Data containers
    private val modules = mutableListOf<ModuleResponse>()
    private var courseId: String? = null
    private var courseTitle: String? = null

    companion object {
        private const val TAG = "CourseModulesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_modules)

        // Step 1: Initialize views (UI binding) — Patel (2025) promotes clean separation of UI init
        initializeViews()

        // Step 2: Setup RecyclerView with adapter and layout manager
        setupRecyclerView()

        // Step 3: Load incoming Intent data passed from previous Activity
        loadIntentData()

        // Step 4: Setup button and other click listeners
        setupClickListeners()

        // Step 5: Load modules if courseId is available, else log error and notify user
        courseId?.let {
            loadModules(it)
        } ?: run {
            Log.e(TAG, "No course ID found — cannot load modules")
            Toast.makeText(this, "Error: No course information", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Initializes UI components by binding them to layout views
     * This keeps onCreate cleaner and modular (Patel, 2025)
     */
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)
        tvCourseTitle = findViewById(R.id.tvCourseTitle)
        tvCourseCode = findViewById(R.id.tvCourseCode)
        tvModuleCount = findViewById(R.id.tvModuleCount)
        tvSort = findViewById(R.id.tvSort)
        recyclerView = findViewById(R.id.recyclerViewModules)
    }

    /**
     * Sets up RecyclerView with linear vertical layout and initializes adapter
     * For efficient display and handling of large data sets (Aram, 2023)
     */
    private fun setupRecyclerView() {
        adapter = StudentModuleAdapter(modules, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    /**
     * Loads course information passed via Intent extras and updates UI accordingly.
     * Defensive null checks and default values are applied to prevent crashes.
     */
    private fun loadIntentData() {
        courseTitle = intent.getStringExtra("COURSE_TITLE")
        courseId = intent.getStringExtra("COURSE_ID")

        updateCourseInfoUI()

        Log.d(TAG, "Opened CourseModulesActivity for: $courseTitle (ID: $courseId)")
    }

    /**
     * Updates course-related UI elements.
     * The module count is updated after modules load (initially zero)
     */
    private fun updateCourseInfoUI() {
        tvCourseTitle.text = courseTitle ?: "Course Title"
        tvCourseCode.text = "Course Code: $courseId"
        tvModuleCount.text = "${modules.size} Modules"
    }

    /**
     * Sets up click listeners for back navigation and sorting options.
     * Separation of concerns improves readability and maintainability.
     */
    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            onBackPressed()
        }

        tvSort.setOnClickListener {
            showSortOptions()
        }
    }

    /**
     * Loads modules for the given courseId by calling the API.
     * Each module is checked asynchronously for the presence of new assessments.
     * Updates UI and logs throughout for clear traceability.
     *
     * @param courseId ID of the course to load modules for
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

                    if (apiModules.isEmpty()) {
                        Toast.makeText(this@CourseModulesActivity, "No modules available for this course", Toast.LENGTH_SHORT).show()
                        updateModuleCount()
                        return
                    }

                    // Track processed modules to know when all callbacks complete
                    var modulesProcessed = 0

                    // For each module, check for new assessments asynchronously
                    apiModules.forEach { module ->
                        checkForNewAssessments(module) { updatedModule ->
                            synchronized(modules) {
                                // Remove duplicates and add updated module info with assessment flag
                                modules.removeAll { it.moduleID == updatedModule.moduleID }
                                modules.add(updatedModule)
                            }

                            modulesProcessed++

                            // Once all modules processed, update UI and notify adapter
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

    /**
     * Checks asynchronously if a module has new assessments by calling the API.
     * Calls back with an updated ModuleResponse including hasNewAssessment flag.
     *
     * @param module Module to check assessments for
     * @param callback Function to receive updated module with assessment flag
     */
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

    /**
     * Updates the module count display in the UI.
     * Called after module list changes.
     */
    private fun updateModuleCount() {
        tvModuleCount.text = "${modules.size} Modules"
    }

    /**
     * Displays a dialog with sort options for modules.
     * Currently supports sorting by module title ascending/descending.
     */
    private fun showSortOptions() {
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

    /**
     * Sorts the modules list by module title alphabetically.
     * @param ascending true for A-Z, false for Z-A
     */
    private fun sortModulesByName(ascending: Boolean) {
        modules.sortBy { it.moduleTitle }
        if (!ascending) {
            modules.reverse()
        }
        adapter.notifyDataSetChanged()
    }

    /**
     * Refreshes module list when the activity resumes,
     * only loading modules if the list is empty to avoid redundant calls.
     */
    override fun onResume() {
        super.onResume()
        courseId?.let {
            if (modules.isEmpty()) {
                loadModules(it)
            }
        }
    }
}

/*

Reference List

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at: https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App Development Process, 20 May 2025, spaceotechnologies.
[Blog]. Available at: https://www.spaceotechnologies.com/blog/kotlin-features/ [Accessed 27 October 2025]

*/
