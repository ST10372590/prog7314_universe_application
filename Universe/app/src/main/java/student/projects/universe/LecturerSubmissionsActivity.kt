package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Activity for lecturers to view all submissions for a specific assessment.
 *
 * Features:
 * - Loads submissions from backend via Retrofit API
 * - Displays submission stats (total, pending grading, graded)
 * - Allows lecturer to review individual submissions by navigating to detailed review
 * - Provides refresh functionality and displays loading & empty states
 *
 * Implements efficient UI updates and error handling with Toast feedback and logging.
 */
class LecturerSubmissionsActivity : AppCompatActivity() {

    // UI elements
    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvTotalSubmissions: TextView
    private lateinit var tvPendingSubmissions: TextView
    private lateinit var tvGradedSubmissions: TextView
    private lateinit var tvRefresh: TextView
    private lateinit var tvFooter: TextView

    // Data variables
    private var submissions: List<SubmissionResponse> = emptyList()
    private var assessmentId: Int = 0
    private var assessmentTitle: String? = null

    companion object {
        private const val TAG = "LecturerSubmissions"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecturer_submissions)

        Log.d(TAG, "onCreate: Initializing views and loading data")

        initializeViews()
        loadIntentData()
        setupClickListeners()

        if (assessmentId == 0) {
            Toast.makeText(this, "Invalid assessment ID.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Invalid assessment ID (0) received; finishing activity.")
            finish()
            return
        }

        loadSubmissions()
    }

    /**
     * Binds UI elements from the layout to properties.
     */
    private fun initializeViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

        tvTotalSubmissions = findViewById(R.id.tvTotalSubmissions)
        tvPendingSubmissions = findViewById(R.id.tvPendingSubmissions)
        tvGradedSubmissions = findViewById(R.id.tvGradedSubmissions)

        tvRefresh = findViewById(R.id.tvRefresh)
        listView = findViewById(R.id.listLecturerSubmissions)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        tvFooter = findViewById(R.id.tvFooter)
    }

    /**
     * Retrieves the assessment details passed from previous activity via intent.
     * Updates UI header accordingly.
     */
    private fun loadIntentData() {
        assessmentId = intent.getIntExtra("assessmentId", 0)
        assessmentTitle = intent.getStringExtra("assessmentTitle")

        if (!assessmentTitle.isNullOrEmpty()) {
            tvHeaderTitle.text = "Submissions: $assessmentTitle"
        }

        Log.d(TAG, "Loaded intent data: assessmentId=$assessmentId, assessmentTitle=$assessmentTitle")
    }

    /**
     * Sets click listeners, including refresh button to reload submissions.
     */
    private fun setupClickListeners() {
        tvRefresh.setOnClickListener {
            Log.d(TAG, "Refresh clicked; reloading submissions.")
            loadSubmissions()
        }
    }

    /**
     * Loads submissions for the current assessment from the API.
     * Shows loading indicator and manages empty states.
     */
    private fun loadSubmissions() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        ApiClient.lecturerApi.getSubmissionsByAssessment(assessmentId)
            .enqueue(object : Callback<List<SubmissionResponse>> {
                override fun onResponse(
                    call: Call<List<SubmissionResponse>>,
                    response: Response<List<SubmissionResponse>>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        submissions = response.body()!!
                        Log.d(TAG, "Loaded ${submissions.size} submissions from API.")
                        updateStatistics()

                        if (submissions.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No student submissions yet for this assessment."
                        } else {
                            listView.adapter = SubmissionAdapter(this@LecturerSubmissionsActivity, submissions)
                        }
                    } else {
                        Log.e(TAG, "Failed to load submissions: HTTP ${response.code()}")
                        Toast.makeText(this@LecturerSubmissionsActivity, "Failed to load submissions", Toast.LENGTH_SHORT).show()
                        updateStatistics() // Update stats even if loading failed to show zeros
                    }
                }

                override fun onFailure(call: Call<List<SubmissionResponse>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Network error while loading submissions", t)
                    Toast.makeText(this@LecturerSubmissionsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    updateStatistics() // Update stats even on failure to show zeros
                }
            })
    }

    /**
     * Updates submission statistics on UI based on current submissions list.
     */
    private fun updateStatistics() {
        val total = submissions.size
        val pending = submissions.count { it.grade == null }
        val graded = total - pending

        tvTotalSubmissions.text = total.toString()
        tvPendingSubmissions.text = pending.toString()
        tvGradedSubmissions.text = graded.toString()

        updateStatColors(pending, graded)
        Log.d(TAG, "Statistics updated - Total: $total, Pending: $pending, Graded: $graded")
    }

    /**
     * Optionally updates the color of statistics TextViews based on values.
     * (Colors are commented out but can be enabled if desired.)
     */
    private fun updateStatColors(pending: Int, graded: Int) {
        if (pending > 0) {
            // tvPendingSubmissions.setTextColor(Color.parseColor("#FF9800")) // Orange for pending
        }

        if (graded > 0) {
            // tvGradedSubmissions.setTextColor(Color.parseColor("#4CAF50")) // Green for graded
        }
    }

    /**
     * Refreshes submissions when returning to activity (e.g. after review).
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called - refreshing submissions list")
        loadSubmissions()
    }

    /**
     * Custom ArrayAdapter to display submissions in ListView.
     * Uses ViewHolder pattern to efficiently bind data.
     */
    inner class SubmissionAdapter(
        private val context: LecturerSubmissionsActivity,
        private val submissions: List<SubmissionResponse>
    ) : ArrayAdapter<SubmissionResponse>(context, 0, submissions) {

        override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_lecturer_submission, parent, false)

            val submission = submissions[position]

            val tvStudentName = view.findViewById<TextView>(R.id.tvStudentName)
            val tvSubmittedAt = view.findViewById<TextView>(R.id.tvSubmittedAt)
            val tvGrade = view.findViewById<TextView>(R.id.tvGrade)
            val btnReview = view.findViewById<Button>(R.id.btnReviewSubmission)

            // Compose student name with fallback if full name is missing
            val studentName = submission.studentName ?: "${submission.studentFirst ?: ""} ${submission.studentLast ?: ""}".trim()
            tvStudentName.text = "Student: $studentName"

            tvSubmittedAt.text = "Submitted: ${formatDate(submission.submittedAt)}"

            // Show grade or indicate not graded, with color coding
            if (submission.grade != null) {
                tvGrade.text = "Grade: ${submission.grade}" // Automatically converts Double to string
                tvGrade.setTextColor(context.getColor(android.R.color.holo_green_dark))
            } else {
                tvGrade.text = "Not graded yet"
                tvGrade.setTextColor(context.getColor(android.R.color.holo_red_dark))
            }

            btnReview.setOnClickListener {
                Log.d(TAG, "Review button clicked for submissionId: ${submission.submissionID}")

                val intent = Intent(context, LecturerSubmissionReviewActivity::class.java).apply {
                    putExtra("submissionId", submission.submissionID)
                    putExtra("studentName", studentName)
                    putExtra("fileLink", submission.fileLink)
                    putExtra("submittedAt", submission.submittedAt)
                    putExtra("grade", submission.grade) // Pass as Double
                    putExtra("feedback", submission.feedback)
                }
                context.startActivity(intent)
            }

            return view
        }

        /**
         * Simple date formatting: extracts YYYY-MM-DD if available, else returns 'Unknown date'.
         */
        private fun formatDate(dateString: String?): String {
            return if (dateString.isNullOrEmpty()) {
                "Unknown date"
            } else {
                if (dateString.length >= 10) dateString.substring(0, 10) else dateString
            }
        }
    }
}
