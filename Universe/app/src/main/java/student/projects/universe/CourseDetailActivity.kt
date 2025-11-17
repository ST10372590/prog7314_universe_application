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

class CourseDetailActivity : AppCompatActivity() {

    // UI components
    private lateinit var tvCourseTitle: TextView
    private lateinit var tvCourseDescription: TextView
    private lateinit var tvCredits: TextView
    private lateinit var tvNoModules: TextView
    private lateinit var recyclerModules: RecyclerView
    private lateinit var tvOfflineIndicator: TextView
    private lateinit var tvLastSync: TextView

    // Adapter and list for displaying modules
    private lateinit var adapter: ModuleAdapter
    private val moduleList = mutableListOf<ModuleResponse>()

    // Database helper for offline storage
    private lateinit var databaseHelper: DatabaseHelper

    // Store course ID globally in this activity
    private var courseId: String = ""
    private var courseTitle: String = ""
    private var courseDescription: String = ""
    private var courseCredits: Int = 0

    // Network state tracking
    private var isOnline: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        Log.d("CourseDetailActivity", "Activity created - initializing views")

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)

        // Bind views from layout
        tvCourseTitle = findViewById(R.id.tvCourseTitle)
        tvCourseDescription = findViewById(R.id.tvCourseDescription)
        tvCredits = findViewById(R.id.tvCredits)
        tvNoModules = findViewById(R.id.tvNoModules)
        recyclerModules = findViewById(R.id.recyclerModules)
        tvOfflineIndicator = findViewById(R.id.tvOfflineIndicator)
        tvLastSync = findViewById(R.id.tvLastSync)

        // Retrieve course data passed from previous activity
        courseId = intent.getStringExtra("courseId") ?: ""
        courseTitle = intent.getStringExtra("courseTitle") ?: "No Title"
        courseDescription = intent.getStringExtra("courseDescription") ?: "No Description"
        courseCredits = intent.getIntExtra("credits", 0)

        Log.d("CourseDetailActivity", "Received courseId: $courseId, title: $courseTitle, credits: $courseCredits")

        // Validate course ID
        if (courseId.isBlank()) {
            Log.e("CourseDetailActivity", "Invalid course ID passed to activity")
            Toast.makeText(this, "Invalid course ID passed!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Check network state
        checkNetworkState()

        // Display course details
        displayCourseDetails()

        Log.d("CourseDetailActivity", "Displayed course details successfully")

        // Setup RecyclerView for modules
        recyclerModules.layoutManager = LinearLayoutManager(this)
        adapter = ModuleAdapter(moduleList) { module ->
            // Added detailed logging for module click
            Log.d("CourseDetailActivity", "Module clicked: ${module.moduleTitle}")
            Log.d("CourseDetailActivity", "courseID for module: ${module.courseID}")

            // Navigate to ModuleTeachMenuActivity with course/module details
            val intent = Intent(this, ModuleTeachMenuActivity::class.java)
            intent.putExtra("courseId", module.courseID)
            intent.putExtra("moduleId", module.moduleID)
            intent.putExtra("moduleTitle", module.moduleTitle)
            startActivity(intent)

            Log.d("CourseDetailActivity", "Navigating to ModuleTeachMenuActivity with courseId=${module.courseID}, moduleId=${module.moduleID}, moduleTitle=${module.moduleTitle}")
        }
        recyclerModules.adapter = adapter

        Log.d("CourseDetailActivity", "RecyclerView setup complete, loading modules...")

        // Load modules associated with this course (both online and offline)
        loadModules()
    }

    /**
     * Check network state and update UI accordingly
     */
    private fun checkNetworkState() {
        isOnline = NetworkUtils.isOnline(this)
        updateOfflineIndicator()
    }

    /**
     * Update offline indicator visibility and text
     */
    private fun updateOfflineIndicator() {
        if (isOnline) {
            tvOfflineIndicator.visibility = View.GONE
            tvLastSync.text = "Last synced: Just now"
        } else {
            tvOfflineIndicator.visibility = View.VISIBLE
            tvLastSync.text = "Offline mode - using cached data"
            Toast.makeText(this, "You are offline. Showing cached course data.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Display course details from intent or local database
     */
    private fun displayCourseDetails() {
        // First try to use data from intent (most recent)
        tvCourseTitle.text = courseTitle
        tvCourseDescription.text = courseDescription
        tvCredits.text = "Credits: $courseCredits"

        // Also save course to local database for offline access
        saveCourseToLocalDatabase()
    }

    /**
     * Save course details to local database for offline access
     */
    private fun saveCourseToLocalDatabase() {
        try {
            val courseEntity = CourseEntity(
                courseId = courseId,
                title = courseTitle,
                description = courseDescription,
                credits = courseCredits,
                createdAt = getCurrentTimestamp()
            )
            databaseHelper.insertOrUpdateCourse(courseEntity)
            Log.d("CourseDetailActivity", "Course saved to local database: $courseTitle")
        } catch (e: Exception) {
            Log.e("CourseDetailActivity", "Error saving course to local database", e)
        }
    }

    /**
     * Load modules from both online API and local database
     */
    private fun loadModules() {
        Log.d("CourseDetailActivity", "Loading modules for courseId: '$courseId'")

        // First load from local database (fast, works offline)
        loadModulesFromLocal()

        // Then try to sync with online (if available)
        if (isOnline) {
            loadModulesFromOnline()
        }
    }

    /**
     * Load modules from local database
     */
    private fun loadModulesFromLocal() {
        try {
            val localModules = databaseHelper.getModulesByCourse(courseId)

            // Convert ModuleEntity to ModuleResponse for the adapter - FIXED MAPPING
            val moduleResponses = localModules.map { entity ->
                ModuleResponse(
                    moduleID = entity.moduleId,
                    courseID = entity.courseId,
                    moduleTitle = entity.title,
                    contentType = "", // Default value for offline data
                    contentLink = "", // Default value for offline data
                    completionStatus = "incomplete", // Default value for offline data
                    hasNewAssessment = false // Default value for offline data
                    // Note: moduleDescription and createdAt are not in the original ModuleResponse
                    // If you need these, you'll have to add them to ModuleResponse class
                )
            }

            if (moduleResponses.isNotEmpty()) {
                moduleList.clear()
                moduleList.addAll(moduleResponses)
                adapter.notifyDataSetChanged()
                showNoModulesMessage(false)
                Log.d("CourseDetailActivity", "Loaded ${moduleResponses.size} modules from local database")

                // Update last sync time
                tvLastSync.text = "Last synced: ${getCurrentTimeFormatted()}"
            } else {
                showNoModulesMessage(true)
                Log.w("CourseDetailActivity", "No modules found in local database for course: $courseId")
            }

        } catch (e: Exception) {
            Log.e("CourseDetailActivity", "Error loading modules from local database", e)
            showNoModulesMessage(true)
        }
    }

    /**
     * Load modules from online API and sync with local database
     */
    private fun loadModulesFromOnline() {
        Log.d("CourseDetailActivity", "Requesting modules from online API for courseId=$courseId")

        ApiClient.moduleApi.getModulesByCourse(courseId).enqueue(object : Callback<List<ModuleResponse>> {
            override fun onResponse(
                call: Call<List<ModuleResponse>>,
                response: Response<List<ModuleResponse>>
            ) {
                Log.d("CourseDetailActivity", "API response received: HTTP ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val onlineModules = response.body()!!
                    Log.d("CourseDetailActivity", "Modules loaded successfully from online: ${onlineModules.size} found")

                    // Save modules to local database
                    saveModulesToLocalDatabase(onlineModules)

                    // Update UI with fresh data
                    moduleList.clear()
                    moduleList.addAll(onlineModules)
                    adapter.notifyDataSetChanged()

                    // Show "No Modules" message if none exist
                    showNoModulesMessage(onlineModules.isEmpty())

                    // Update last sync time
                    tvLastSync.text = "Last synced: ${getCurrentTimeFormatted()}"

                    if (onlineModules.isEmpty()) {
                        Log.w("CourseDetailActivity", "No modules available for this course")
                    }

                } else {
                    Log.e("CourseDetailActivity", "Failed to load modules from online. HTTP code: ${response.code()}")

                    // If we have local data, continue showing it
                    if (moduleList.isEmpty()) {
                        Toast.makeText(
                            this@CourseDetailActivity,
                            "Failed to load modules: HTTP ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@CourseDetailActivity,
                            "Using cached data (online sync failed)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<List<ModuleResponse>>, t: Throwable) {
                Log.e("CourseDetailActivity", "Error loading modules from online: ${t.localizedMessage}", t)

                // If we have local data, continue showing it without error message
                if (moduleList.isEmpty()) {
                    Toast.makeText(
                        this@CourseDetailActivity,
                        "Error loading modules: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.d("CourseDetailActivity", "Online failed but using cached modules")
                }
            }
        })
    }

    /**
     * Save modules to local database for offline access
     */
    private fun saveModulesToLocalDatabase(modules: List<ModuleResponse>) {
        try {
            val moduleEntities = modules.map { response ->
                ModuleEntity(
                    moduleId = response.moduleID,
                    title = response.moduleTitle,
                    description = "", // Use empty string since ModuleResponse doesn't have description
                    courseId = response.courseID,
                    createdAt = getCurrentTimestamp() // Use current time since ModuleResponse doesn't have createdAt
                )
            }
            databaseHelper.insertOrUpdateModules(moduleEntities)
            Log.d("CourseDetailActivity", "Saved ${moduleEntities.size} modules to local database")
        } catch (e: Exception) {
            Log.e("CourseDetailActivity", "Error saving modules to local database", e)
        }
    }

    /**
     * Shows or hides the "no modules" message.
     */
    private fun showNoModulesMessage(show: Boolean) {
        runOnUiThread {
            tvNoModules.visibility = if (show) View.VISIBLE else View.GONE
            recyclerModules.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    /**
     * Get current timestamp in ISO format
     */
    private fun getCurrentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    /**
     * Get formatted current time for display
     */
    private fun getCurrentTimeFormatted(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    override fun onResume() {
        super.onResume()
        // Refresh network state and data when returning to activity
        checkNetworkState()
        loadModules()
    }
}

/*
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

class CourseDetailActivity : AppCompatActivity() {

    // UI components
    private lateinit var tvCourseTitle: TextView
    private lateinit var tvCourseDescription: TextView
    private lateinit var tvCredits: TextView
    private lateinit var tvNoModules: TextView
    private lateinit var recyclerModules: RecyclerView
    private lateinit var tvOfflineIndicator: TextView
    private lateinit var tvLastSync: TextView

    // Adapter and list for displaying modules
    private lateinit var adapter: ModuleAdapter
    private val moduleList = mutableListOf<ModuleResponse>()

    // Database helper for offline storage
    private lateinit var databaseHelper: DatabaseHelper

    // Store course ID globally in this activity
    private var courseId: String = ""
    private var courseTitle: String = ""
    private var courseDescription: String = ""
    private var courseCredits: Int = 0

    // Network state tracking
    private var isOnline: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        Log.d("CourseDetailActivity", "Activity created - initializing views")

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)

        // Bind views from layout
        tvCourseTitle = findViewById(R.id.tvCourseTitle)
        tvCourseDescription = findViewById(R.id.tvCourseDescription)
        tvCredits = findViewById(R.id.tvCredits)
        tvNoModules = findViewById(R.id.tvNoModules)
        recyclerModules = findViewById(R.id.recyclerModules)
        tvOfflineIndicator = findViewById(R.id.tvOfflineIndicator)
        tvLastSync = findViewById(R.id.tvLastSync)

        // Retrieve course data passed from previous activity
        courseId = intent.getStringExtra("courseId") ?: ""
        courseTitle = intent.getStringExtra("courseTitle") ?: "No Title"
        courseDescription = intent.getStringExtra("courseDescription") ?: "No Description"
        courseCredits = intent.getIntExtra("credits", 0)

        Log.d("CourseDetailActivity", "Received courseId: $courseId, title: $courseTitle, credits: $courseCredits")

        // Validate course ID
        if (courseId.isBlank()) {
            Log.e("CourseDetailActivity", "Invalid course ID passed to activity")
            Toast.makeText(this, "Invalid course ID passed!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Check network state
        checkNetworkState()

        // Display course details
        displayCourseDetails()

        Log.d("CourseDetailActivity", "Displayed course details successfully")

        // Setup RecyclerView for modules
        recyclerModules.layoutManager = LinearLayoutManager(this)
        adapter = ModuleAdapter(moduleList) { module ->
            // Added detailed logging for module click
            Log.d("CourseDetailActivity", "Module clicked: ${module.moduleTitle}")
            Log.d("CourseDetailActivity", "courseID for module: ${module.courseID}")

            // Navigate to ModuleTeachMenuActivity with course/module details
            val intent = Intent(this, ModuleTeachMenuActivity::class.java)
            intent.putExtra("courseId", module.courseID)
            intent.putExtra("moduleId", module.moduleID)
            intent.putExtra("moduleTitle", module.moduleTitle)
            startActivity(intent)

            Log.d("CourseDetailActivity", "Navigating to ModuleTeachMenuActivity with courseId=${module.courseID}, moduleId=${module.moduleID}, moduleTitle=${module.moduleTitle}")
        }
        recyclerModules.adapter = adapter

        Log.d("CourseDetailActivity", "RecyclerView setup complete, loading modules...")

        // Load modules associated with this course (both online and offline)
        loadModules()
    }

    /**
     * Check network state and update UI accordingly
     */
    private fun checkNetworkState() {
        isOnline = NetworkUtils.isOnline(this)
        updateOfflineIndicator()
    }

    /**
     * Update offline indicator visibility and text
     */
    private fun updateOfflineIndicator() {
        if (isOnline) {
            tvOfflineIndicator.visibility = View.GONE
            tvLastSync.text = "Last synced: Just now"
        } else {
            tvOfflineIndicator.visibility = View.VISIBLE
            tvLastSync.text = "Offline mode - using cached data"
            Toast.makeText(this, "You are offline. Showing cached course data.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Display course details from intent or local database
     */
    private fun displayCourseDetails() {
        // First try to use data from intent (most recent)
        tvCourseTitle.text = courseTitle
        tvCourseDescription.text = courseDescription
        tvCredits.text = "Credits: $courseCredits"

        // Also save course to local database for offline access
        saveCourseToLocalDatabase()
    }

    /**
     * Save course details to local database for offline access
     */
    private fun saveCourseToLocalDatabase() {
        try {
            val courseEntity = CourseEntity(
                courseId = courseId,
                title = courseTitle,
                description = courseDescription,
                credits = courseCredits,
                createdAt = getCurrentTimestamp()
            )
            databaseHelper.insertOrUpdateCourse(courseEntity)
            Log.d("CourseDetailActivity", "Course saved to local database: $courseTitle")
        } catch (e: Exception) {
            Log.e("CourseDetailActivity", "Error saving course to local database", e)
        }
    }

    /**
     * Load modules from both online API and local database
     */
    private fun loadModules() {
        Log.d("CourseDetailActivity", "Loading modules for courseId: '$courseId'")

        // First load from local database (fast, works offline)
        loadModulesFromLocal()

        // Then try to sync with online (if available)
        if (isOnline) {
            loadModulesFromOnline()
        }
    }

    /**
     * Load modules from local database
     */
    private fun loadModulesFromLocal() {
        try {
            val localModules = databaseHelper.getModulesByCourse(courseId)

            // Convert ModuleEntity to ModuleResponse for the adapter - FIXED MAPPING
            val moduleResponses = localModules.map { entity ->
                ModuleResponse(
                    moduleID = entity.moduleId,
                    courseID = entity.courseId,
                    moduleTitle = entity.title,
                    contentType = "", // Default value for offline data
                    contentLink = "", // Default value for offline data
                    completionStatus = "incomplete", // Default value for offline data
                    hasNewAssessment = false // Default value for offline data
                    // Note: moduleDescription and createdAt are not in the original ModuleResponse
                    // If you need these, you'll have to add them to ModuleResponse class
                )
            }

            if (moduleResponses.isNotEmpty()) {
                moduleList.clear()
                moduleList.addAll(moduleResponses)
                adapter.notifyDataSetChanged()
                showNoModulesMessage(false)
                Log.d("CourseDetailActivity", "Loaded ${moduleResponses.size} modules from local database")

                // Update last sync time
                tvLastSync.text = "Last synced: ${getCurrentTimeFormatted()}"
            } else {
                showNoModulesMessage(true)
                Log.w("CourseDetailActivity", "No modules found in local database for course: $courseId")
            }

        } catch (e: Exception) {
            Log.e("CourseDetailActivity", "Error loading modules from local database", e)
            showNoModulesMessage(true)
        }
    }

    /**
     * Load modules from online API and sync with local database
     */
    private fun loadModulesFromOnline() {
        Log.d("CourseDetailActivity", "Requesting modules from online API for courseId=$courseId")

        ApiClient.moduleApi.getModulesByCourse(courseId).enqueue(object : Callback<List<ModuleResponse>> {
            override fun onResponse(
                call: Call<List<ModuleResponse>>,
                response: Response<List<ModuleResponse>>
            ) {
                Log.d("CourseDetailActivity", "API response received: HTTP ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val onlineModules = response.body()!!
                    Log.d("CourseDetailActivity", "Modules loaded successfully from online: ${onlineModules.size} found")

                    // Save modules to local database
                    saveModulesToLocalDatabase(onlineModules)

                    // Update UI with fresh data
                    moduleList.clear()
                    moduleList.addAll(onlineModules)
                    adapter.notifyDataSetChanged()

                    // Show "No Modules" message if none exist
                    showNoModulesMessage(onlineModules.isEmpty())

                    // Update last sync time
                    tvLastSync.text = "Last synced: ${getCurrentTimeFormatted()}"

                    if (onlineModules.isEmpty()) {
                        Log.w("CourseDetailActivity", "No modules available for this course")
                    }

                } else {
                    Log.e("CourseDetailActivity", "Failed to load modules from online. HTTP code: ${response.code()}")

                    // If we have local data, continue showing it
                    if (moduleList.isEmpty()) {
                        Toast.makeText(
                            this@CourseDetailActivity,
                            "Failed to load modules: HTTP ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@CourseDetailActivity,
                            "Using cached data (online sync failed)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<List<ModuleResponse>>, t: Throwable) {
                Log.e("CourseDetailActivity", "Error loading modules from online: ${t.localizedMessage}", t)

                // If we have local data, continue showing it without error message
                if (moduleList.isEmpty()) {
                    Toast.makeText(
                        this@CourseDetailActivity,
                        "Error loading modules: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.d("CourseDetailActivity", "Online failed but using cached modules")
                }
            }
        })
    }

    /**
     * Save modules to local database for offline access
     */
    private fun saveModulesToLocalDatabase(modules: List<ModuleResponse>) {
        try {
            val moduleEntities = modules.map { response ->
                ModuleEntity(
                    moduleId = response.moduleID,
                    title = response.moduleTitle,
                    description = "", // Use empty string since ModuleResponse doesn't have description
                    courseId = response.courseID,
                    createdAt = getCurrentTimestamp() // Use current time since ModuleResponse doesn't have createdAt
                )
            }
            databaseHelper.insertOrUpdateModules(moduleEntities)
            Log.d("CourseDetailActivity", "Saved ${moduleEntities.size} modules to local database")
        } catch (e: Exception) {
            Log.e("CourseDetailActivity", "Error saving modules to local database", e)
        }
    }

    /**
     * Shows or hides the "no modules" message.
     */
    private fun showNoModulesMessage(show: Boolean) {
        runOnUiThread {
            tvNoModules.visibility = if (show) View.VISIBLE else View.GONE
            recyclerModules.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    /**
     * Get current timestamp in ISO format
     */
    private fun getCurrentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    /**
     * Get formatted current time for display
     */
    private fun getCurrentTimeFormatted(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    override fun onResume() {
        super.onResume()
        // Refresh network state and data when returning to activity
        checkNetworkState()
        loadModules()
    }
}

/*
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
            // Added detailed logging for module click
            Log.d("CourseDetailActivity", "Module clicked: ${module.moduleTitle}")
            Log.d("CourseDetailActivity", "courseID for module: ${module.courseID}") // 👈 Added line

            // Navigate to ModuleTeachMenuActivity with course/module details // (Patel, 2025)
            val intent = Intent(this, ModuleTeachMenuActivity::class.java)
            intent.putExtra("courseId", module.courseID)
            intent.putExtra("moduleId", module.moduleID) // FIX: pass moduleId correctly
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