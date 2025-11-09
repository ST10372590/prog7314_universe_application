package student.projects.universe

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SubmissionDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submission_details)

        val title = intent.getStringExtra("assessmentTitle")
        val fileLink = intent.getStringExtra("fileLink")
        val submittedAt = intent.getStringExtra("submittedAt")
        val grade = intent.getDoubleExtra("grade", 0.0)
        val feedback = intent.getStringExtra("feedback")

        findViewById<TextView>(R.id.tvDetailsTitle).text = title
        findViewById<TextView>(R.id.tvDetailsFileLink).text = "File: $fileLink"
        findViewById<TextView>(R.id.tvDetailsSubmittedAt).text = "Submitted: $submittedAt"
        findViewById<TextView>(R.id.tvDetailsGrade).text = "Grade: $grade"
        findViewById<TextView>(R.id.tvDetailsFeedback).text =
            if (!feedback.isNullOrEmpty()) feedback else "No feedback yet."
    }
}
