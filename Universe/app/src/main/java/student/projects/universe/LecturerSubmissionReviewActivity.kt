package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LecturerSubmissionReviewActivity : AppCompatActivity() {

    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var tvSubmittedAt: TextView
    private lateinit var tvFileLink: TextView
    private lateinit var etGrade: EditText
    private lateinit var etFeedback: EditText
    private lateinit var btnSubmitFeedback: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton

    private var submissionId: Int = -1

    companion object {
        private const val TAG = "LecturerSubmissionReview"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecturer_submission_review)

        // Initialize all UI elements including the new ones
        initializeViews()
        setupClickListeners()
        loadSubmissionData()
    }

    private fun initializeViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvSubmittedAt = findViewById(R.id.tvSubmittedAt)
        tvFileLink = findViewById(R.id.tvFileLink)
        etGrade = findViewById(R.id.etGrade)
        etFeedback = findViewById(R.id.etFeedback)
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback)
        progressBar = findViewById(R.id.progressBar)

        // Note: You'll need to add the ImageButton to your layout XML
        // If you add btnBack to your layout, uncomment the line below:
        // btnBack = findViewById(R.id.btnBack)
    }

    private fun setupClickListeners() {
        btnSubmitFeedback.setOnClickListener {
            updateFeedback()
        }

        // If you add back button to layout, uncomment this:
        // btnBack.setOnClickListener {
        //     onBackPressed()
        // }

        // Make file link clickable
        tvFileLink.setOnClickListener {
            val fileLink = intent.getStringExtra("fileLink") ?: ""
            if (fileLink.isNotEmpty()) {
                // Open the file link (you might want to use an Intent to open the URL)
                Toast.makeText(this, "Opening file: $fileLink", Toast.LENGTH_SHORT).show()
                // Example: openUrlInBrowser(fileLink)
            } else {
                Toast.makeText(this, "No file link available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSubmissionData() {
        // Retrieve intent data
        submissionId = intent.getIntExtra("submissionId", -1)
        val studentName = intent.getStringExtra("studentName") ?: "Unknown Student"
        val submittedAt = intent.getStringExtra("submittedAt") ?: "Unknown Date"
        val fileLink = intent.getStringExtra("fileLink") ?: "N/A"
        val grade = intent.getStringExtra("grade") ?: ""
        val feedback = intent.getStringExtra("feedback") ?: ""

        // Display data with better formatting
        tvStudentName.text = studentName
        tvSubmittedAt.text = "Submitted: $submittedAt"

        // Format file link to be more user-friendly
        val displayFileLink = if (fileLink.length > 30) {
            "${fileLink.take(30)}..."
        } else {
            fileLink
        }
        tvFileLink.text = displayFileLink
        tvFileLink.contentDescription = "Open file: $fileLink" // Accessibility

        // Set existing grade and feedback if available
        etGrade.setText(grade)
        etFeedback.setText(feedback)

        // Update header with student name for context
        tvHeaderTitle.text = "Feedback for $studentName"

        Log.d(TAG, "Activity started with submissionId: $submissionId")
        Log.d(TAG, "Loaded studentName: $studentName, submittedAt: $submittedAt")
        Log.d(TAG, "File link: $fileLink")
        Log.d(TAG, "Existing grade: '$grade', feedback: '$feedback'")
    }

    private fun updateFeedback() {
        val grade = etGrade.text.toString().trim()
        val feedback = etFeedback.text.toString().trim()

        Log.d(TAG, "Attempting to update feedback for submissionId: $submissionId")
        Log.d(TAG, "Entered Grade: '$grade'")
        Log.d(TAG, "Entered Feedback: '${feedback.take(50)}...'") // Log first 50 chars

        // Enhanced validation
        if (submissionId == -1) {
            showError("Invalid submission ID. Please try again.")
            Log.e(TAG, "Cannot submit feedback — submissionId is invalid (-1)!")
            return
        }

        if (grade.isEmpty()) {
            showError("Please enter a grade")
            etGrade.requestFocus()
            return
        }

        if (feedback.isEmpty()) {
            showError("Please enter feedback")
            etFeedback.requestFocus()
            return
        }

        // Validate grade format
        val gradeValue = try {
            grade.toInt()
        } catch (e: NumberFormatException) {
            showError("Please enter a valid numeric grade")
            etGrade.requestFocus()
            return
        }

        if (gradeValue < 0 || gradeValue > 100) {
            showError("Please enter a grade between 0 and 100")
            etGrade.requestFocus()
            return
        }

        showLoading(true)
        Log.d(TAG, "Sending feedback update request to server...")

        ApiClient.lecturerApi.updateSubmissionFeedback(submissionId, grade, feedback)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    showLoading(false)

                    Log.d(TAG, "Response received from server")
                    Log.d(TAG, "Response Code: ${response.code()}")
                    Log.d(TAG, "Response Message: ${response.message()}")

                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        showSuccess("Feedback submitted successfully")
                        Log.i(TAG, "Feedback submitted successfully for submissionId: $submissionId")

                        // Set result and finish
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val errorMessage = when (response.code()) {
                            400 -> "Invalid request data"
                            404 -> "Submission not found"
                            500 -> "Server error. Please try again later."
                            else -> "Failed to submit feedback"
                        }
                        showError(errorMessage)
                        Log.e(TAG, "Failed to submit feedback. Code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showLoading(false)
                    showError("Network error: ${t.message ?: "Unknown error"}")
                    Log.e(TAG, "🌐 Network error during feedback submission", t)
                }
            })
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSubmitFeedback.isEnabled = !show
        btnSubmitFeedback.alpha = if (show) 0.5f else 1.0f
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        // Check if there are unsaved changes
        val currentGrade = etGrade.text.toString().trim()
        val currentFeedback = etFeedback.text.toString().trim()

        val originalGrade = intent.getStringExtra("grade") ?: ""
        val originalFeedback = intent.getStringExtra("feedback") ?: ""

        if (currentGrade != originalGrade || currentFeedback != originalFeedback) {
            // Show confirmation dialog for unsaved changes
            android.app.AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Are you sure you want to leave?")
                .setPositiveButton("Leave") { _, _ ->
                    super.onBackPressed()
                }
                .setNegativeButton("Stay", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}