package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Activity to allow lecturers to review and provide feedback on student submissions.
 * This includes viewing submission details, entering grades, and writing feedback.
 * Network requests update the backend with submitted feedback.
 *
 * Implements:
 * - Input validation for grade and feedback
 * - Network call with Retrofit for updating feedback
 * - User-friendly loading indicators and error handling
 * - Confirmation dialog on unsaved changes before navigating away
 *
 * Logging added for easier debugging and traceability (Patel, 2025).
 */
class LecturerSubmissionReviewActivity : AppCompatActivity() {

    // UI elements
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var tvSubmittedAt: TextView
    private lateinit var tvFileLink: TextView
    private lateinit var etGrade: EditText
    private lateinit var etFeedback: EditText
    private lateinit var btnSubmitFeedback: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton

    // ID of the submission being reviewed
    private var submissionId: Int = -1

    companion object {
        private const val TAG = "LecturerSubmissionReview"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecturer_submission_review)

        Log.d(TAG, "onCreate: Initializing views and loading submission data")

        initializeViews()
        setupClickListeners()
        loadSubmissionData()
    }

    /**
     * Initializes UI components by finding them in the layout.
     * This improves code readability and maintenance (Patel, 2025).
     */
    private fun initializeViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvSubmittedAt = findViewById(R.id.tvSubmittedAt)
        tvFileLink = findViewById(R.id.tvFileLink)
        etGrade = findViewById(R.id.etGrade)
        etFeedback = findViewById(R.id.etFeedback)
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback)
        progressBar = findViewById(R.id.progressBar)

        // Optional back button; enable if added to layout
        // btnBack = findViewById(R.id.btnBack)
    }

    /**
     * Sets up event listeners for UI components.
     * Handles submission of feedback and opening file links (Patel, 2025).
     */
    private fun setupClickListeners() {
        btnSubmitFeedback.setOnClickListener {
            Log.d(TAG, "Submit feedback button clicked")
            updateFeedback()
        }

        // Uncomment if back button is implemented in layout
        // btnBack.setOnClickListener { onBackPressed() }

        // Make file link clickable and show a toast or open in browser
        tvFileLink.setOnClickListener {
            val fileLink = intent.getStringExtra("fileLink") ?: ""
            if (fileLink.isNotEmpty()) {
                Toast.makeText(this, "Opening file: $fileLink", Toast.LENGTH_SHORT).show()
                // TODO: Implement intent to open URL in browser
            } else {
                Toast.makeText(this, "No file link available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Loads submission data passed via intent and populates UI elements.
     * Truncates file link for display and sets accessibility content description.
     */
    private fun loadSubmissionData() {
        submissionId = intent.getIntExtra("submissionId", -1)
        val studentName = intent.getStringExtra("studentName") ?: "Unknown Student"
        val submittedAt = intent.getStringExtra("submittedAt") ?: "Unknown Date"
        val fileLink = intent.getStringExtra("fileLink") ?: "N/A"
        val grade = intent.getStringExtra("grade") ?: ""
        val feedback = intent.getStringExtra("feedback") ?: ""

        tvStudentName.text = studentName
        tvSubmittedAt.text = "Submitted: $submittedAt"

        // Truncate file link for better UX if too long
        val displayFileLink = if (fileLink.length > 30) {
            "${fileLink.take(30)}..."
        } else fileLink
        tvFileLink.text = displayFileLink
        tvFileLink.contentDescription = "Open file: $fileLink" // Accessibility

        etGrade.setText(grade)
        etFeedback.setText(feedback)

        tvHeaderTitle.text = "Feedback for $studentName"

        Log.d(TAG, "Loaded submission data for ID $submissionId, student $studentName")
        Log.d(TAG, "File link: $fileLink")
        Log.d(TAG, "Grade prefilled: '$grade', Feedback prefilled length: ${feedback.length}")
    }

    /**
     * Validates input and sends updated feedback to server.
     * Shows loading indicator during network call.
     * Handles success and failure responses with user feedback.
     */
    private fun updateFeedback() {
        val grade = etGrade.text.toString().trim()
        val feedback = etFeedback.text.toString().trim()

        Log.d(TAG, "updateFeedback: submissionId=$submissionId, grade='$grade', feedback='${feedback.take(50)}...'")

        if (submissionId == -1) {
            showError("Invalid submission ID. Please try again.")
            Log.e(TAG, "Invalid submission ID: -1")
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

        // Validate grade is numeric and between 0 and 100
        val gradeValue = try {
            grade.toInt()
        } catch (e: NumberFormatException) {
            showError("Please enter a valid numeric grade")
            etGrade.requestFocus()
            return
        }

        if (gradeValue !in 0..100) {
            showError("Please enter a grade between 0 and 100")
            etGrade.requestFocus()
            return
        }

        showLoading(true)
        Log.d(TAG, "Sending feedback update to server for submissionId: $submissionId")

        ApiClient.lecturerApi.updateSubmissionFeedback(submissionId, grade, feedback)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    showLoading(false)
                    Log.d(TAG, "Received response: code=${response.code()}, message=${response.message()}")

                    if (response.isSuccessful && response.body() != null) {
                        showSuccess("Feedback submitted successfully")
                        Log.i(TAG, "Feedback updated successfully for submissionId: $submissionId")

                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val errorMsg = when (response.code()) {
                            400 -> "Invalid request data"
                            404 -> "Submission not found"
                            500 -> "Server error. Please try again later."
                            else -> "Failed to submit feedback"
                        }
                        showError(errorMsg)
                        Log.e(TAG, "Failed to submit feedback: HTTP ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    showLoading(false)
                    showError("Network error: ${t.message ?: "Unknown error"}")
                    Log.e(TAG, "Network failure during feedback update", t)
                }
            })
    }

    /**
     * Shows or hides the loading indicator and disables/enables the submit button accordingly.
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        btnSubmitFeedback.isEnabled = !show
        btnSubmitFeedback.alpha = if (show) 0.5f else 1.0f
    }

    /**
     * Displays a short success message to the user.
     */
    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Displays a long error message to the user.
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Overrides the back button to warn user about unsaved changes.
     * Prompts confirmation dialog if changes detected (Patel, 2025).
     */
    override fun onBackPressed() {
        val currentGrade = etGrade.text.toString().trim()
        val currentFeedback = etFeedback.text.toString().trim()

        val originalGrade = intent.getStringExtra("grade") ?: ""
        val originalFeedback = intent.getStringExtra("feedback") ?: ""

        if (currentGrade != originalGrade || currentFeedback != originalFeedback) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Are you sure you want to leave?")
                .setPositiveButton("Leave") { _, _ -> super.onBackPressed() }
                .setNegativeButton("Stay", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}
