package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LecturerSubmissionsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvTotalSubmissions: TextView
    private lateinit var tvPendingSubmissions: TextView
    private lateinit var tvGradedSubmissions: TextView
    private lateinit var tvRefresh: TextView
    private lateinit var tvFooter: TextView

    private var submissions: List<SubmissionResponse> = emptyList()
    private var assessmentId: Int = 0
    private var assessmentTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecturer_submissions)

        initializeViews()
        loadIntentData()
        setupClickListeners()

        if (assessmentId == 0) {
            Toast.makeText(this, "Invalid assessment ID.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadSubmissions()
    }

    private fun initializeViews() {
        // Header
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

        // Stats card
        tvTotalSubmissions = findViewById(R.id.tvTotalSubmissions)
        tvPendingSubmissions = findViewById(R.id.tvPendingSubmissions)
        tvGradedSubmissions = findViewById(R.id.tvGradedSubmissions)

        // Submissions section
        tvRefresh = findViewById(R.id.tvRefresh)
        listView = findViewById(R.id.listLecturerSubmissions)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        // Footer
        tvFooter = findViewById(R.id.tvFooter)
    }

    private fun loadIntentData() {
        assessmentId = intent.getIntExtra("assessmentId", 0)
        assessmentTitle = intent.getStringExtra("assessmentTitle")

        // Update header with assessment title if available
        if (!assessmentTitle.isNullOrEmpty()) {
            tvHeaderTitle.text = "Submissions: $assessmentTitle"
        }
    }

    private fun setupClickListeners() {
        tvRefresh.setOnClickListener {
            loadSubmissions()
        }
    }

    private fun loadSubmissions() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        // ✅ API now expects Int
        ApiClient.lecturerApi.getSubmissionsByAssessment(assessmentId)
            .enqueue(object : Callback<List<SubmissionResponse>> {
                override fun onResponse(
                    call: Call<List<SubmissionResponse>>,
                    response: Response<List<SubmissionResponse>>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        submissions = response.body()!!
                        updateStatistics()

                        if (submissions.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No student submissions yet for this assessment."
                        } else {
                            val adapter = SubmissionAdapter(this@LecturerSubmissionsActivity, submissions)
                            listView.adapter = adapter
                        }
                    } else {
                        Toast.makeText(this@LecturerSubmissionsActivity, "Failed to load submissions", Toast.LENGTH_SHORT).show()
                        updateStatistics() // Update stats even on failure to show zeros
                    }
                }

                override fun onFailure(call: Call<List<SubmissionResponse>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@LecturerSubmissionsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    updateStatistics() // Update stats even on failure to show zeros
                }
            })
    }

    private fun updateStatistics() {
        val total = submissions.size
        val pending = submissions.count { it.grade == null }
        val graded = total - pending

        tvTotalSubmissions.text = total.toString()
        tvPendingSubmissions.text = pending.toString()
        tvGradedSubmissions.text = graded.toString()

        // Optional: Update colors based on status
        updateStatColors(pending, graded)
    }

    private fun updateStatColors(pending: Int, graded: Int) {
        // You can add color changes based on the statistics if desired
        // For example, change pending count to orange if there are many pending submissions
        if (pending > 0) {
            // tvPendingSubmissions.setTextColor(Color.parseColor("#FF9800"))
        }

        if (graded > 0) {
            // tvGradedSubmissions.setTextColor(Color.parseColor("#4CAF50"))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from submission review
        loadSubmissions()
    }

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

            // Set student name with fallback
            val studentName = submission.studentName ?: "${submission.studentFirst ?: ""} ${submission.studentLast ?: ""}".trim()
            tvStudentName.text = "Student: $studentName"

            // Format submitted date
            tvSubmittedAt.text = "Submitted: ${formatDate(submission.submittedAt)}"

            // Set grade status with color coding - FIXED FOR DOUBLE
            if (submission.grade != null) {
                tvGrade.text = "Grade: ${submission.grade}" // Double will be automatically converted to string
                tvGrade.setTextColor(context.getColor(android.R.color.holo_green_dark))
            } else {
                tvGrade.text = "Not graded yet"
                tvGrade.setTextColor(context.getColor(android.R.color.holo_red_dark))
            }

            btnReview.setOnClickListener {
                val intent = Intent(context, LecturerSubmissionReviewActivity::class.java)
                intent.putExtra("submissionId", submission.submissionID)
                intent.putExtra("studentName", studentName)
                intent.putExtra("fileLink", submission.fileLink)
                intent.putExtra("submittedAt", submission.submittedAt)
                intent.putExtra("grade", submission.grade) // Keep as Double
                intent.putExtra("feedback", submission.feedback)
                context.startActivity(intent)
            }

            return view
        }

        private fun formatDate(dateString: String?): String {
            return if (dateString.isNullOrEmpty()) {
                "Unknown date"
            } else {
                // Simple formatting - you can enhance this with proper date parsing
                if (dateString.length > 10) dateString.substring(0, 10) else dateString
            }
        }
    }
}
