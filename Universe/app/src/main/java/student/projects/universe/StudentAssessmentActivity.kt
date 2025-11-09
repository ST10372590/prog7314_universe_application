package student.projects.universe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.TextView

class StudentAssessmentActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentAssessmentAdapter
    private val assessmentList = mutableListOf<Assessment>()
    private var moduleId: String = ""

    private lateinit var firebaseRef: DatabaseReference

    // Badge TextView to show unread/new assessments count
    private lateinit var tvBadgeCount: TextView

    // Tracks how many new assessments arrived since last user refresh
    private var newAssessmentsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_assessment)

        // --- Initialize badge view ---
        tvBadgeCount = findViewById(R.id.tvBadgeCount)
        updateBadgeCount(0) // start hidden

        // --- Receive moduleId from previous Activity ---
        moduleId = intent.getStringExtra("MODULE_ID") ?: ""
        if (moduleId.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Module not found", Snackbar.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Setup RecyclerView and Adapter ---
        recyclerView = findViewById(R.id.rvAssessments)
        adapter = StudentAssessmentAdapter(
            assessmentList,
            onViewClick = { fileUrl -> openFile(fileUrl) },
            onSubmitClick = { assessment ->
                val intent = Intent(this, SubmitAssessmentActivity::class.java)
                intent.putExtra("assessmentId", assessment.assessmentID)
                intent.putExtra("moduleId", moduleId)
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // --- Load assessments dynamically ---
        loadAssessments()

        // --- Listen for new assessments via Firebase notifications ---
        listenForAssessmentNotifications()
    }

    /**
     * Load assessments for this module from the API.
     * Updates RecyclerView dynamically.
     * Resets badge count on load.
     */
    private fun loadAssessments() {
        ApiClient.assessmentApi.getAssessmentsByModule(moduleId)
            .enqueue(object : Callback<List<AssessmentResponse>> {
                override fun onResponse(
                    call: Call<List<AssessmentResponse>>,
                    response: Response<List<AssessmentResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val assessments = response.body()!!.map {
                            Assessment(
                                assessmentID = it.assessmentID,
                                title = it.title,
                                description = it.description,
                                dueDate = it.dueDate,
                                maxMarks = it.maxMarks,
                                fileUrl = it.fileUrl
                            )
                        }
                        assessmentList.clear()
                        assessmentList.addAll(assessments)
                        adapter.notifyDataSetChanged()
                        Log.d("StudentAssessment", "Loaded ${assessments.size} assessments for module $moduleId")

                        // Reset new assessments badge count as user refreshed list
                        newAssessmentsCount = 0
                        updateBadgeCount(0)

                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "No assessments found", Snackbar.LENGTH_SHORT).show()
                        Log.w("StudentAssessment", "No assessments found or empty response for module $moduleId")
                    }
                }

                override fun onFailure(call: Call<List<AssessmentResponse>>, t: Throwable) {
                    Snackbar.make(findViewById(android.R.id.content), "Error loading assessments", Snackbar.LENGTH_SHORT).show()
                    Log.e("StudentAssessment", "Failed to load assessments: ${t.message}", t)
                }
            })
    }

    /**
     * Listen to Firebase notifications for new assessments in this module.
     * Updates the list automatically in real-time.
     * Increments badge count on each new assessment notification.
     */
    private fun listenForAssessmentNotifications() {
        firebaseRef = FirebaseDatabase.getInstance().getReference("notifications").child(moduleId)
        firebaseRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val title = snapshot.child("title").value?.toString() ?: "New Assessment"
                val desc = snapshot.child("description").value?.toString() ?: ""

                // Show Snackbar notification
                Snackbar.make(findViewById(android.R.id.content), "$title: $desc", Snackbar.LENGTH_LONG).show()

                // Reload assessments list
                loadAssessments()

                // Increment new assessments badge count
                newAssessmentsCount++
                updateBadgeCount(newAssessmentsCount)

                Log.d("StudentAssessment", "New assessment notification received: $title")
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("StudentAssessment", "Firebase listener cancelled: ${error.message}")
            }
        })
    }

    /**
     * Update badge UI for unread/new assessments.
     * Hides badge if count == 0.
     */
    private fun updateBadgeCount(count: Int) {
        if (count > 0) {
            tvBadgeCount.visibility = View.VISIBLE
            tvBadgeCount.text = count.toString()
        } else {
            tvBadgeCount.visibility = View.GONE
        }
    }

    /**
     * Open assessment file in external app if URL is available.
     */
    private fun openFile(fileUrl: String?) {
        fileUrl?.let {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(it), "*/*")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } ?: Snackbar.make(findViewById(android.R.id.content), "No file available", Snackbar.LENGTH_SHORT).show()
    }
}