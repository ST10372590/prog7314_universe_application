package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ModuleTeachMenuActivity : AppCompatActivity() {

    private lateinit var menuListView: ListView
    private val menuItems = listOf(
        "Upload Assessments",
        "Announcements",
        "File Sharing",
        "Submissions"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module_teach_menu)

        menuListView = findViewById(R.id.menuListView)
        val adapter = MenuAdapter(this, menuItems)
        menuListView.adapter = adapter

        val courseId = intent.getStringExtra("courseId") ?: ""
        val moduleId = intent.getStringExtra("moduleId") ?: ""
        val moduleTitle = intent.getStringExtra("moduleTitle") ?: "Untitled Module"

        Log.d("ModuleTeachMenu", "Received courseId: '$courseId', moduleId: '$moduleId', title: '$moduleTitle'")

        menuListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> { // Upload Assessments
                    startActivity(Intent(this, UploadAssessmentActivity::class.java).apply {
                        putExtra("courseId", courseId)
                        putExtra("moduleId", moduleId)
                    })
                }
                /*
                1 -> { // File Sharing
                    startActivity(Intent(this, FileSharingActivity::class.java).apply {
                        putExtra("courseId", courseId)
                        putExtra("moduleId", moduleId)
                    })
                }

                 */
                2 -> { // File Sharing
                    startActivity(Intent(this, FileSharingActivity::class.java).apply {
                        putExtra("courseId", courseId)
                        putExtra("moduleId", moduleId)
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

                        Log.d("ModuleTeachMenu", "Opening submissions for assessmentId: $assessmentId")

                        startActivity(Intent(this@ModuleTeachMenuActivity, LecturerSubmissionsActivity::class.java).apply {
                            putExtra("assessmentId", assessmentId) // ✅ pass as Int
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
