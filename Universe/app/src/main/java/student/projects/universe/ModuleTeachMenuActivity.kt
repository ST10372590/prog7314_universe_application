package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ModuleTeachMenuActivity : AppCompatActivity() {

    private lateinit var menuListView: ListView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvCoursesCount: TextView
    private lateinit var tvMessagesCount: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvFooter: TextView

    private val menuItems = listOf(
        "Upload Assessments",
        "Announcements",
        "File Sharing",
        "Submissions"
    )

    private var courseId: String = ""
    private var moduleId: String = ""
    private var moduleTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module_teach_menu)

        initializeViews()
        loadIntentData()
        setupUI()
        setupListView()
    }

    private fun initializeViews() {
        // Header
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

        // Menu section
        tvUserRole = findViewById(R.id.tvUserRole)
        menuListView = findViewById(R.id.menuListView)

        // Footer
        tvFooter = findViewById(R.id.tvFooter)
    }

    private fun loadIntentData() {
        courseId = intent.getStringExtra("courseId") ?: ""
        moduleId = intent.getStringExtra("moduleId") ?: ""
        moduleTitle = intent.getStringExtra("moduleTitle") ?: "Untitled Module"

        Log.d("ModuleTeachMenu", "Received courseId: '$courseId', moduleId: '$moduleId', title: '$moduleTitle'")
    }

    private fun setupUI() {
        // Update header with module title
        tvHeaderTitle.text = moduleTitle

        // Set user role (assuming lecturer/teacher)
        tvUserRole.text = "Lecturer"

        // Update footer if needed
        tvFooter.text = "© 2025 Universe App - $moduleTitle"
    }

    private fun setupListView() {
        val adapter = MenuAdapter(this, menuItems)
        menuListView.adapter = adapter

        menuListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> { // Upload Assessments
                    startActivity(Intent(this, UploadAssessmentActivity::class.java).apply {
                        putExtra("courseId", courseId)
                        putExtra("moduleId", moduleId)
                        putExtra("moduleTitle", moduleTitle)
                    })
                }
                1 -> { // Announcements
                    startActivity(Intent(this, MakeAnnouncementActivity::class.java).apply {
                        putExtra("moduleId", moduleId)
                        putExtra("moduleTitle", moduleTitle)
                        putExtra("courseId", courseId)
                    })
                }
                2 -> { // File Sharing
                    startActivity(Intent(this, FileSharingActivity::class.java).apply {
                        putExtra("courseId", courseId)
                        putExtra("moduleId", moduleId)
                        putExtra("moduleTitle", moduleTitle)
                    })
                }
                3 -> { // Submissions
                    fetchLatestAssessmentAndOpenSubmissions(moduleId)
                }
            }
        }
    }

    // Fetch latest assessment for this module, then navigate using its Int ID
    private fun fetchLatestAssessmentAndOpenSubmissions(moduleId: String) {
        ApiClient.assessmentApi.getAssessmentsByModule(moduleId)
            .enqueue(object : Callback<List<AssessmentResponse>> {
                override fun onResponse(
                    call: Call<List<AssessmentResponse>>,
                    response: Response<List<AssessmentResponse>>
                ) {
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val latestAssessment = response.body()!!.last() // or .first() depending on logic
                        val assessmentId = latestAssessment.assessmentID
                        val assessmentTitle = latestAssessment.title ?: "Assessment"

                        Log.d("ModuleTeachMenu", "Opening submissions for assessmentId: $assessmentId")

                        startActivity(Intent(this@ModuleTeachMenuActivity, LecturerSubmissionsActivity::class.java).apply {
                            putExtra("assessmentId", assessmentId) // pass as Int
                            putExtra("assessmentTitle", assessmentTitle)
                            putExtra("moduleTitle", moduleTitle)
                        })
                    } else {
                        Toast.makeText(this@ModuleTeachMenuActivity, "No assessments found for this module", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<AssessmentResponse>>, t: Throwable) {
                    Toast.makeText(this@ModuleTeachMenuActivity, "Failed to fetch assessments: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ModuleTeachMenu", "Error fetching assessments", t)
                }
            })
    }
}