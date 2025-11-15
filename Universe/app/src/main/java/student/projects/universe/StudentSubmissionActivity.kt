package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentSubmissionsActivity : AppCompatActivity() {

    // UI components from enhanced layout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBackToCourses: Button
    private lateinit var btnRefresh: Button
    private lateinit var btnViewCourses: Button
    private lateinit var tvEmpty: View
    private lateinit var tvSubmissionCount: TextView
    private lateinit var tvTotalSubmissions: TextView
    private lateinit var tvGradedSubmissions: TextView
    private lateinit var tvPendingSubmissions: TextView

    private lateinit var adapter: SubmissionAdapter
    private var submissions: List<SubmissionResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_submissions)
        Log.d("StudentSubmissions", "Activity started")

        // Initialize all UI components
        initializeViews()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup click listeners
        setupClickListeners()

        // Load submissions
        loadSubmissions()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewSubmissions)
        progressBar = findViewById(R.id.progressBar)
        btnBackToCourses = findViewById(R.id.btnBackToCourses)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnViewCourses = findViewById(R.id.btnViewCourses)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvSubmissionCount = findViewById(R.id.tvSubmissionCount)
        tvTotalSubmissions = findViewById(R.id.tvTotalSubmissions)
        tvGradedSubmissions = findViewById(R.id.tvGradedSubmissions)
        tvPendingSubmissions = findViewById(R.id.tvPendingSubmissions)

        // Initialize stats with zeros
        updateStatsCards(0, 0, 0)
    }

    private fun setupRecyclerView() {
        adapter = SubmissionAdapter(submissions) { submission ->
            // Handle submission item click
            openSubmissionDetails(submission)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        Log.d("StudentSubmissions", "RecyclerView initialized")
    }

    private fun setupClickListeners() {
        // Back to courses button
        btnBackToCourses.setOnClickListener {
            Log.d("StudentSubmissions", "Back to courses button clicked")
            val intent = Intent(this, StudentEnrolledCoursesActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Refresh button
        btnRefresh.setOnClickListener {
            Log.d("StudentSubmissions", "Refresh button clicked")
            loadSubmissions()
        }

        // View courses button (in empty state)
        btnViewCourses.setOnClickListener {
            Log.d("StudentSubmissions", "View courses button clicked")
            val intent = Intent(this, StudentEnrolledCoursesActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadSubmissions() {
        val userId = ApiClient.currentUserId
        showLoading(true)

        Log.d("StudentSubmissions", "Loading submissions for user: $userId")

        ApiClient.studentApi.getUserSubmissions(userId)
            .enqueue(object : Callback<List<SubmissionResponse>> {
                override fun onResponse(
                    call: Call<List<SubmissionResponse>>,
                    response: Response<List<SubmissionResponse>>
                ) {
                    showLoading(false)

                    if (response.isSuccessful && response.body() != null) {
                        submissions = response.body()!!
                        Log.d("StudentSubmissions", "Loaded ${submissions.size} submissions")

                        // Update adapter
                        adapter.updateSubmissions(submissions)

                        // Update UI based on data
                        updateUIAfterDataLoad(submissions)

                    } else {
                        showEmptyState(true)
                        Toast.makeText(this@StudentSubmissionsActivity, "Failed to load submissions", Toast.LENGTH_SHORT).show()
                        Log.e("StudentSubmissions", "Failed to load submissions: HTTP ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<SubmissionResponse>>, t: Throwable) {
                    showLoading(false)
                    showEmptyState(true)
                    Toast.makeText(this@StudentSubmissionsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("StudentSubmissions", "API call failed", t)
                }
            })
    }

    private fun updateUIAfterDataLoad(submissions: List<SubmissionResponse>) {
        val totalSubmissions = submissions.size
        val gradedSubmissions = submissions.count { it.grade != null && it.grade >= 0 }
        val pendingSubmissions = totalSubmissions - gradedSubmissions

        // Update stats cards
        updateStatsCards(totalSubmissions, gradedSubmissions, pendingSubmissions)

        // Update header submission count
        tvSubmissionCount.text = if (totalSubmissions == 1) "1 Submission" else "$totalSubmissions Submissions"

        // Show/hide empty state
        showEmptyState(totalSubmissions == 0)

        // Show/hide submissions list
        recyclerView.visibility = if (totalSubmissions > 0) View.VISIBLE else View.GONE

        Log.d("StudentSubmissions", "UI updated - Total: $totalSubmissions, Graded: $gradedSubmissions, Pending: $pendingSubmissions")
    }

    private fun updateStatsCards(total: Int, graded: Int, pending: Int) {
        tvTotalSubmissions.text = total.toString()
        tvGradedSubmissions.text = graded.toString()
        tvPendingSubmissions.text = pending.toString()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.GONE
        }
    }

    private fun showEmptyState(show: Boolean) {
        tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun openSubmissionDetails(submission: SubmissionResponse) {
        Log.d("StudentSubmissions", "Opening submission details: ${submission.submissionID}")

        val intent = Intent(this, SubmissionDetailsActivity::class.java).apply {
            putExtra("submissionId", submission.submissionID)
            putExtra("assessmentTitle", submission.assessmentTitle)
            putExtra("fileLink", submission.fileLink)
            putExtra("submittedAt", submission.submittedAt)
            putExtra("grade", submission.grade ?: -1.0)
            putExtra("feedback", submission.feedback)
            // Add any additional fields you might need
            putExtra("maxMarks", submission.maxMarks ?: 100.0)
            putExtra("gradedBy", submission.gradedBy ?: "Instructor")
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Optional: Refresh data when returning to this activity
        // loadSubmissions()
    }
}