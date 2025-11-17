package student.projects.universe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Activity to display student assessments for a specific module.
 * Supports:
 * - Dynamic loading of assessments from API (Aram, 2023)
 * - Real-time updates via Firebase notifications (Patel, 2025)
 * - UI updates with badges and statistics
 * - Search and filter functionalities
 * - Opening assessment files with external apps
 */
class StudentAssessmentActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentAssessmentAdapter
    private val assessmentList = mutableListOf<Assessment>()
    private var moduleId: String = ""

    private lateinit var firebaseRef: DatabaseReference

    // UI elements for enhanced layout
    private lateinit var tvBadgeCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoAssessments: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilter: ImageButton
    private lateinit var tvTotalCount: TextView
    private lateinit var tvGradedCount: TextView
    private lateinit var tvPendingCount: TextView

    // Counters for assessments status
    private var newAssessmentsCount = 0
    private var totalAssessments = 0
    private var gradedAssessments = 0
    private var pendingAssessments = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_assessment)
        Log.d("StudentAssessment", "Activity started") // (Patel, 2025)

        initializeViews()

        // Receive moduleId from intent extras
        moduleId = intent.getStringExtra("MODULE_ID") ?: ""
        if (moduleId.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Module not found", Snackbar.LENGTH_SHORT).show()
            Log.e("StudentAssessment", "No module ID provided, finishing activity")
            finish()
            return
        }
        Log.d("StudentAssessment", "Module ID received: $moduleId")

        setupRecyclerView()
        setupSearchAndFilter()
        loadAssessments()
        listenForAssessmentNotifications()
    }

    /**
     * Initialize all UI views from the layout
     */
    private fun initializeViews() {
        tvBadgeCount = findViewById(R.id.tvBadgeCount)
        progressBar = findViewById(R.id.progressBar)
        tvNoAssessments = findViewById(R.id.tvNoAssessments)
        etSearch = findViewById(R.id.etSearch)
        btnFilter = findViewById(R.id.btnFilter)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        tvGradedCount = findViewById(R.id.tvGradedCount)
        tvPendingCount = findViewById(R.id.tvPendingCount)

        updateStatsCards(0, 0, 0)
        updateBadgeCount(0) // Hide badge initially
        Log.d("StudentAssessment", "UI components initialized") // (Patel, 2025)
    }

    /**
     * Setup RecyclerView and adapter for assessment list
     */
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvAssessments)
        adapter = StudentAssessmentAdapter(
            assessmentList,
            onViewClick = { fileUrl -> openFile(fileUrl) },
            onSubmitClick = { assessment ->
                val intent = Intent(this, SubmitAssessmentActivity::class.java)
                intent.putExtra("assessmentId", assessment.assessmentID)
                intent.putExtra("moduleId", moduleId)
                startActivity(intent)
                Log.d("StudentAssessment", "Starting SubmitAssessmentActivity for assessment ID: ${assessment.assessmentID}")
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        Log.d("StudentAssessment", "RecyclerView and Adapter set up") // (Aram, 2023)
    }

    /**
     * Setup search and filter functionality listeners
     */
    private fun setupSearchAndFilter() {
        etSearch.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }
        btnFilter.setOnClickListener {
            showFilterOptions()
        }
        Log.d("StudentAssessment", "Search and filter listeners configured") // (Patel, 2025)
    }

    /**
     * Load assessments for this module from API asynchronously.
     * Updates UI, badge counts, and stats.
     */
    private fun loadAssessments() {
        showLoading(true)
        Log.d("StudentAssessment", "Loading assessments for module $moduleId") // (Aram, 2023)

        ApiClient.assessmentApi.getAssessmentsByModule(moduleId)
            .enqueue(object : Callback<List<AssessmentResponse>> {
                override fun onResponse(
                    call: Call<List<AssessmentResponse>>,
                    response: Response<List<AssessmentResponse>>
                ) {
                    showLoading(false)
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

                        totalAssessments = assessments.size
                        gradedAssessments = assessments.count { /* Add graded logic here */ false }
                        pendingAssessments = totalAssessments - gradedAssessments

                        updateStatsCards(totalAssessments, gradedAssessments, pendingAssessments)
                        tvNoAssessments.visibility = if (assessments.isEmpty()) View.VISIBLE else View.GONE

                        newAssessmentsCount = 0
                        updateBadgeCount(0)

                        Log.d("StudentAssessment", "Loaded ${assessments.size} assessments successfully")
                    } else {
                        tvNoAssessments.visibility = View.VISIBLE
                        Snackbar.make(findViewById(android.R.id.content), "No assessments found", Snackbar.LENGTH_SHORT).show()
                        Log.w("StudentAssessment", "No assessments found or empty response for module $moduleId")
                    }
                }

                override fun onFailure(call: Call<List<AssessmentResponse>>, t: Throwable) {
                    showLoading(false)
                    tvNoAssessments.visibility = View.VISIBLE
                    Snackbar.make(findViewById(android.R.id.content), "Error loading assessments", Snackbar.LENGTH_SHORT).show()
                    Log.e("StudentAssessment", "Failed to load assessments: ${t.message}", t)
                }
            })
    }

    /**
     * Listen to Firebase for new assessments notifications in real-time.
     * On new notification, reload list and update badge count.
     */
    private fun listenForAssessmentNotifications() {
        firebaseRef = FirebaseDatabase.getInstance().getReference("notifications").child(moduleId)
        firebaseRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val title = snapshot.child("title").value?.toString() ?: "New Assessment"
                val desc = snapshot.child("description").value?.toString() ?: ""

                Snackbar.make(findViewById(android.R.id.content), "$title: $desc", Snackbar.LENGTH_LONG).show()

                loadAssessments()
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
        Log.d("StudentAssessment", "Firebase listener registered for new assessments") // (Aram, 2023)
    }

    /**
     * Update badge UI for new assessments count.
     * Hides badge if count is zero.
     */
    private fun updateBadgeCount(count: Int) {
        if (count > 0) {
            tvBadgeCount.visibility = View.VISIBLE
            tvBadgeCount.text = if (count > 99) "99+" else count.toString()
        } else {
            tvBadgeCount.visibility = View.GONE
        }
        Log.d("StudentAssessment", "Badge count updated: $count") // (Patel, 2025)
    }

    /**
     * Update statistics cards for total, graded, and pending assessments.
     */
    private fun updateStatsCards(total: Int, graded: Int, pending: Int) {
        tvTotalCount.text = total.toString()
        tvGradedCount.text = graded.toString()
        tvPendingCount.text = pending.toString()
        Log.d("StudentAssessment", "Stats updated: total=$total, graded=$graded, pending=$pending") // (Patel, 2025)
    }

    /**
     * Show or hide loading indicator and list visibility accordingly.
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.INVISIBLE else View.VISIBLE
        Log.d("StudentAssessment", "Loading indicator visibility: $show") // (Patel, 2025)
    }

    /**
     * Perform search based on user input.
     * Placeholder implementation; add actual filtering logic.
     */
    private fun performSearch() {
        val query = etSearch.text.toString().trim()
        if (query.isNotEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Searching for: $query", Snackbar.LENGTH_SHORT).show()
            Log.d("StudentAssessment", "Performing search for query: $query") // (Patel, 2025)
            // TODO: Add filtering logic to update adapter list based on query
        }
    }

    /**
     * Show filter options dialog.
     * Placeholder implementation.
     */
    private fun showFilterOptions() {
        Snackbar.make(findViewById(android.R.id.content), "Filter options would appear here", Snackbar.LENGTH_SHORT).show()
        Log.d("StudentAssessment", "Filter options requested") // (Patel, 2025)
    }

    /**
     * Open assessment file using external application if URL exists.
     */
    private fun openFile(fileUrl: String?) {
        if (!fileUrl.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(fileUrl), "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Log.d("StudentAssessment", "Opening file URL: $fileUrl")
        } else {
            Snackbar.make(findViewById(android.R.id.content), "No file available", Snackbar.LENGTH_SHORT).show()
            Log.w("StudentAssessment", "No file URL to open")
        }
    }
}

/*

Reference List

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at: https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

*/
