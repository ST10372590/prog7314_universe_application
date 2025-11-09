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
    private var submissions: List<SubmissionResponse> = emptyList()
    private var assessmentId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lecturer_submissions)

        listView = findViewById(R.id.listLecturerSubmissions)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        // ✅ Get assessmentId as an integer (default 0 if missing)
        assessmentId = intent.getIntExtra("assessmentId", 0)

        if (assessmentId == 0) {
            Toast.makeText(this, "Invalid assessment ID.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadSubmissions()
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
                        if (submissions.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No student submissions yet."
                        } else {
                            val adapter = SubmissionAdapter(this@LecturerSubmissionsActivity, submissions)
                            listView.adapter = adapter
                        }
                    } else {
                        Toast.makeText(this@LecturerSubmissionsActivity, "Failed to load submissions", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<SubmissionResponse>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@LecturerSubmissionsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
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

            tvStudentName.text = "Student: ${submission.studentName ?: "${submission.studentFirst ?: ""} ${submission.studentLast ?: ""}".trim()}"
            tvSubmittedAt.text = "Submitted: ${submission.submittedAt}"
            tvGrade.text = if (submission.grade != null) "Grade: ${submission.grade}" else "Not graded yet"

            btnReview.setOnClickListener {
                val intent = Intent(context, LecturerSubmissionReviewActivity::class.java)
                intent.putExtra("submissionId", submission.submissionID)
                intent.putExtra("studentName", submission.studentName)
                intent.putExtra("fileLink", submission.fileLink)
                intent.putExtra("submittedAt", submission.submittedAt)
                context.startActivity(intent)
            }

            return view
        }
    }
}
