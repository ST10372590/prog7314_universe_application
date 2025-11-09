package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LecturerSubmissionReviewActivity : AppCompatActivity() {

    private lateinit var tvStudentName: TextView
    private lateinit var tvSubmittedAt: TextView
    private lateinit var tvFileLink: TextView
    private lateinit var etGrade: EditText
    private lateinit var etFeedback: EditText
    private lateinit var btnSubmitFeedback: Button
    private lateinit var progressBar: ProgressBar

    private var submissionId: Int = -1  // ✅ Now an Int with default invalid value

    companion object {
        private const val TAG = "LecturerSubmissionReview"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecturer_submission_review)

        // Initialize UI elements
        tvStudentName = findViewById(R.id.tvStudentName)
        tvSubmittedAt = findViewById(R.id.tvSubmittedAt)
        tvFileLink = findViewById(R.id.tvFileLink)
        etGrade = findViewById(R.id.etGrade)
        etFeedback = findViewById(R.id.etFeedback)
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback)
        progressBar = findViewById(R.id.progressBar)

        // Retrieve intent data
        submissionId = intent.getIntExtra("submissionId", -1)  // ✅ Use getIntExtra
        val studentName = intent.getStringExtra("studentName") ?: "Unknown Student"
        val submittedAt = intent.getStringExtra("submittedAt") ?: "Unknown Date"
        val fileLink = intent.getStringExtra("fileLink") ?: "N/A"
        val grade = intent.getStringExtra("grade") ?: ""
        val feedback = intent.getStringExtra("feedback") ?: ""

        // Display data
        tvStudentName.text = "Student: $studentName"
        tvSubmittedAt.text = "Submitted: $submittedAt"
        tvFileLink.text = "File: $fileLink"
        etGrade.setText(grade)
        etFeedback.setText(feedback)

        Log.d(TAG, "Activity started with submissionId: $submissionId")
        Log.d(TAG, "Loaded studentName: $studentName, submittedAt: $submittedAt, fileLink: $fileLink")

        btnSubmitFeedback.setOnClickListener {
            updateFeedback()
        }
    }

    private fun updateFeedback() {
        val grade = etGrade.text.toString().trim()
        val feedback = etFeedback.text.toString().trim()

        Log.d(TAG, "Attempting to update feedback for submissionId: $submissionId")
        Log.d(TAG, "Entered Grade: '$grade'")
        Log.d(TAG, "Entered Feedback: '$feedback'")

        // Validate submissionId
        if (submissionId == -1) {  // ✅ Int check
            Toast.makeText(this, "Invalid submission ID. Please try again.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Cannot submit feedback — submissionId is invalid (-1)!")
            return
        }

        // Validate input fields
        if (grade.isEmpty() || feedback.isEmpty()) {
            Toast.makeText(this, "Please enter both grade and feedback", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Grade or feedback field is empty.")
            return
        }

        progressBar.visibility = android.view.View.VISIBLE

        Log.d(TAG, "Sending feedback update request to server...")

        // ✅ Ensure your API expects Int for submissionId
        ApiClient.lecturerApi.updateSubmissionFeedback(submissionId, grade, feedback)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    progressBar.visibility = android.view.View.GONE

                    Log.d(TAG, "Response received from server")
                    Log.d(TAG, "Response Code: ${response.code()}")
                    Log.d(TAG, "Response Message: ${response.message()}")
                    Log.d(TAG, "Raw Response: ${response.raw()}")
                    Log.d(TAG, "Response Body: ${response.body()}")
                    Log.d(TAG, "Error Body: ${response.errorBody()?.string()}")

                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(
                            this@LecturerSubmissionReviewActivity,
                            "Feedback submitted successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.i(TAG, "Feedback submitted successfully for submissionId: $submissionId")
                        finish()
                    } else {
                        Toast.makeText(
                            this@LecturerSubmissionReviewActivity,
                            "Failed to submit feedback",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Failed to submit feedback. Code: ${response.code()}, Message: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(
                        this@LecturerSubmissionReviewActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "🌐 Network error during feedback submission", t)
                }
            })
    }
}
