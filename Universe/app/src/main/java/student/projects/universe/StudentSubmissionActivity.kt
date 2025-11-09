package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentSubmissionsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBackToCourses: Button
    private lateinit var tvEmpty: TextView
    private lateinit var submissions: List<SubmissionResponse>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_submissions)

        listView = findViewById(R.id.listSubmissions)
        progressBar = findViewById(R.id.progressBar)
        btnBackToCourses = findViewById(R.id.btnBackToCourses)
        tvEmpty = findViewById(R.id.tvEmpty)

        btnBackToCourses.setOnClickListener {
            val intent = Intent(this, StudentEnrolledCoursesActivity::class.java)
            startActivity(intent)
            finish()
        }

        loadSubmissions()
    }

    private fun loadSubmissions() {
        val userId = ApiClient.currentUserId
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        ApiClient.studentApi.getUserSubmissions(userId)
            .enqueue(object : Callback<List<SubmissionResponse>> {
                override fun onResponse(
                    call: Call<List<SubmissionResponse>>,
                    response: Response<List<SubmissionResponse>>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        submissions = response.body()!!
                        if (submissions.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No submissions yet."
                        } else {
                            val adapter = SubmissionAdapter(this@StudentSubmissionsActivity, submissions)
                            listView.adapter = adapter
                        }
                    } else {
                        Toast.makeText(this@StudentSubmissionsActivity, "Failed to load submissions", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<SubmissionResponse>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@StudentSubmissionsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    inner class SubmissionAdapter(
        private val context: StudentSubmissionsActivity,
        private val submissions: List<SubmissionResponse>
    ) : ArrayAdapter<SubmissionResponse>(context, 0, submissions) {

        override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_submission, parent, false)

            val submission = submissions[position]
            val tvTitle = view.findViewById<TextView>(R.id.tvSubmissionTitle)
            val tvDate = view.findViewById<TextView>(R.id.tvSubmissionDate)
            val tvGrade = view.findViewById<TextView>(R.id.tvSubmissionGrade)
            val tvFeedback = view.findViewById<TextView>(R.id.tvSubmissionFeedback)
            val btnViewDetails = view.findViewById<Button>(R.id.btnViewDetails)

            tvTitle.text = submission.assessmentTitle
            tvDate.text = "Submitted: ${submission.submittedAt}"
            tvGrade.text = "Grade: ${submission.grade}"
            tvFeedback.text = if (!submission.feedback.isNullOrEmpty()) "Feedback: ${submission.feedback}" else "No feedback yet"

            btnViewDetails.setOnClickListener {
                val intent = Intent(context, SubmissionDetailsActivity::class.java)
                intent.putExtra("submissionId", submission.submissionID)
                intent.putExtra("assessmentTitle", submission.assessmentTitle)
                intent.putExtra("fileLink", submission.fileLink)
                intent.putExtra("submittedAt", submission.submittedAt)
                intent.putExtra("grade", submission.grade)
                intent.putExtra("feedback", submission.feedback)
                context.startActivity(intent)
            }

            return view
        }
    }
}