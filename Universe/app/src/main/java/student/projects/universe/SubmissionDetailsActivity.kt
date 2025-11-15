package student.projects.universe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class SubmissionDetailsActivity : AppCompatActivity() {

    private lateinit var tvDetailsTitle: TextView
    private lateinit var tvDetailsFileLink: TextView
    private lateinit var tvDetailsSubmittedAt: TextView
    private lateinit var tvDetailsGrade: TextView
    private lateinit var tvGradePercentage: TextView
    private lateinit var tvDetailsFeedback: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvFeedbackDate: TextView
    private lateinit var tvGradedBy: TextView
    private lateinit var gradeProgress: ProgressBar
    private lateinit var btnDownload: ImageButton
    private lateinit var btnBack: Button
    private lateinit var btnViewOriginal: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submission_details)

        // Initialize all views
        initializeViews()

        // Get data from intent
        val title = intent.getStringExtra("assessmentTitle") ?: "Unknown Assessment"
        val fileLink = intent.getStringExtra("fileLink") ?: "No file available"
        val submittedAt = intent.getStringExtra("submittedAt") ?: "Unknown date"
        val grade = intent.getDoubleExtra("grade", -1.0)
        val feedback = intent.getStringExtra("feedback") ?: "No feedback provided yet."
        val gradedBy = intent.getStringExtra("gradedBy") ?: "Instructor"
        val maxMarks = intent.getDoubleExtra("maxMarks", 100.0)

        // Populate data
        populateData(title, fileLink, submittedAt, grade, feedback, gradedBy, maxMarks)

        // Setup click listeners
        setupClickListeners(fileLink)
    }

    private fun initializeViews() {
        tvDetailsTitle = findViewById(R.id.tvDetailsTitle)
        tvDetailsFileLink = findViewById(R.id.tvDetailsFileLink)
        tvDetailsSubmittedAt = findViewById(R.id.tvDetailsSubmittedAt)
        tvDetailsGrade = findViewById(R.id.tvDetailsGrade)
        tvGradePercentage = findViewById(R.id.tvGradePercentage)
        tvDetailsFeedback = findViewById(R.id.tvDetailsFeedback)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvFeedbackDate = findViewById(R.id.tvFeedbackDate)
        tvGradedBy = findViewById(R.id.tvGradedBy)
        gradeProgress = findViewById(R.id.gradeProgress)
        btnDownload = findViewById(R.id.btnDownload)
        btnBack = findViewById(R.id.btnBack)
        btnViewOriginal = findViewById(R.id.btnViewOriginal)
    }

    private fun populateData(
        title: String,
        fileLink: String,
        submittedAt: String,
        grade: Double,
        feedback: String,
        gradedBy: String,
        maxMarks: Double
    ) {
        // Set basic information
        tvDetailsTitle.text = title
        tvDetailsFileLink.text = getFileNameFromUrl(fileLink)
        tvDetailsSubmittedAt.text = formatDate(submittedAt)
        tvDetailsFeedback.text = feedback
        tvGradedBy.text = "Graded by: $gradedBy"
        tvFeedbackDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())

        // Handle grade display
        if (grade >= 0) {
            // Submission is graded
            val percentage = (grade / maxMarks * 100).toInt()
            tvDetailsGrade.text = "${grade.toInt()}/${maxMarks.toInt()}"
            tvGradePercentage.text = getGradeText(percentage)
            gradeProgress.progress = percentage

            // Set status badge
            tvStatusBadge.text = "GRADED"
            tvStatusBadge.setBackgroundResource(R.drawable.badge_graded)

        } else {
            // Submission is not graded yet
            tvDetailsGrade.text = "Not Graded"
            tvGradePercentage.text = "Pending evaluation"
            gradeProgress.progress = 0
            gradeProgress.progressTintList = ContextCompat.getColorStateList(this, R.color.colorPending)

            // Set status badge for pending
            tvStatusBadge.text = "PENDING"
            tvStatusBadge.setBackgroundResource(R.drawable.badge_pending)
        }

        // Show/hide download button based on file availability
        if (fileLink == "No file available" || fileLink.isEmpty()) {
            btnDownload.visibility = View.GONE
            tvDetailsFileLink.text = "No file submitted"
            tvDetailsFileLink.setTextColor(ContextCompat.getColor(this, R.color.colorError))
        }
    }

    private fun setupClickListeners(fileLink: String) {
        // Back button - close activity
        btnBack.setOnClickListener {
            finish()
        }

        // View Original button - open the submitted file
        btnViewOriginal.setOnClickListener {
            if (fileLink != "No file available" && fileLink.isNotEmpty()) {
                openFile(fileLink)
            } else {
                Toast.makeText(this, "No file available to view", Toast.LENGTH_SHORT).show()
            }
        }

        // Download button - download the file
        btnDownload.setOnClickListener {
            if (fileLink != "No file available" && fileLink.isNotEmpty()) {
                downloadFile(fileLink)
            } else {
                Toast.makeText(this, "No file available to download", Toast.LENGTH_SHORT).show()
            }
        }

        // File link text click - also opens the file
        tvDetailsFileLink.setOnClickListener {
            if (fileLink != "No file available" && fileLink.isNotEmpty()) {
                openFile(fileLink)
            }
        }
    }

    private fun openFile(fileUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(fileUrl), getMimeType(fileUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Verify that there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No app available to open this file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadFile(fileUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
            startActivity(intent)
            Toast.makeText(this, "Downloading file...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error downloading file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileNameFromUrl(url: String): String {
        return try {
            Uri.parse(url).lastPathSegment ?: "document.pdf"
        } catch (e: Exception) {
            "document.pdf"
        }
    }

    private fun getMimeType(url: String): String {
        return when {
            url.endsWith(".pdf") -> "application/pdf"
            url.endsWith(".doc") || url.endsWith(".docx") -> "application/msword"
            url.endsWith(".ppt") || url.endsWith(".pptx") -> "application/vnd.ms-powerpoint"
            url.endsWith(".xls") || url.endsWith(".xlsx") -> "application/vnd.ms-excel"
            url.endsWith(".zip") -> "application/zip"
            url.endsWith(".jpg") || url.endsWith(".jpeg") -> "image/jpeg"
            url.endsWith(".png") -> "image/png"
            else -> "*/*"
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            // Try to parse the date and format it nicely
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM d, yyyy 'at' HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString // Return original if parsing fails
        }
    }

    private fun getGradeText(percentage: Int): String {
        return when {
            percentage >= 90 -> "$percentage% - Excellent"
            percentage >= 80 -> "$percentage% - Very Good"
            percentage >= 70 -> "$percentage% - Good"
            percentage >= 60 -> "$percentage% - Satisfactory"
            percentage >= 50 -> "$percentage% - Pass"
            else -> "$percentage% - Needs Improvement"
        }
    }
}