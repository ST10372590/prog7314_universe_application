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

class StudentAssessmentActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentAssessmentAdapter
    private val assessmentList = mutableListOf<Assessment>()
    private var moduleId: String = ""

    private lateinit var firebaseRef: DatabaseReference

    // New UI elements from enhanced layout
    private lateinit var tvBadgeCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoAssessments: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilter: ImageButton
    private lateinit var tvTotalCount: TextView
    private lateinit var tvGradedCount: TextView
    private lateinit var tvPendingCount: TextView

    private var newAssessmentsCount = 0
    private var totalAssessments = 0
    private var gradedAssessments = 0
    private var pendingAssessments = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_assessment)

        // Initialize all views
        initializeViews()

        // Receive moduleId from previous Activity
        moduleId = intent.getStringExtra("MODULE_ID") ?: ""
        if (moduleId.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Module not found", Snackbar.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup RecyclerView and Adapter
        setupRecyclerView()

        // Setup search and filter functionality
        setupSearchAndFilter()

        // Load assessments dynamically
        loadAssessments()

        // Listen for new assessments via Firebase notifications
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

        // Initialize stats with zeros
        updateStatsCards(0, 0, 0)
        updateBadgeCount(0) // start hidden
    }

    /**
     * Setup RecyclerView and adapter
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
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    /**
     * Setup search and filter functionality
     */
    private fun setupSearchAndFilter() {
        // Search functionality
        etSearch.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }

        // Filter button click
        btnFilter.setOnClickListener {
            showFilterOptions()
        }
    }

    /**
     * Load assessments for this module from the API.
     * Updates RecyclerView dynamically.
     * Resets badge count on load.
     */
    private fun loadAssessments() {
        showLoading(true)

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

                        // Update stats (you'll need to implement logic for graded/pending)
                        totalAssessments = assessments.size
                        gradedAssessments = assessments.count { /* Add your graded logic here */ false }
                        pendingAssessments = totalAssessments - gradedAssessments

                        updateStatsCards(totalAssessments, gradedAssessments, pendingAssessments)

                        // Show/hide empty state
                        tvNoAssessments.visibility = if (assessments.isEmpty()) View.VISIBLE else View.GONE

                        // Reset new assessments badge count as user refreshed list
                        newAssessmentsCount = 0
                        updateBadgeCount(0)

                        Log.d("StudentAssessment", "Loaded ${assessments.size} assessments for module $moduleId")

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
            tvBadgeCount.text = if (count > 99) "99+" else count.toString()
        } else {
            tvBadgeCount.visibility = View.GONE
        }
    }

    /**
     * Update statistics cards with current counts
     */
    private fun updateStatsCards(total: Int, graded: Int, pending: Int) {
        tvTotalCount.text = total.toString()
        tvGradedCount.text = graded.toString()
        tvPendingCount.text = pending.toString()
    }

    /**
     * Show or hide loading progress bar
     */
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }

    /**
     * Perform search based on user input
     */
    private fun performSearch() {
        val query = etSearch.text.toString().trim()
        // Implement your search logic here
        // You might want to filter the assessmentList and update the adapter
        if (query.isNotEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "Searching for: $query", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Show filter options dialog
     */
    private fun showFilterOptions() {
        // Implement filter options (by date, status, etc.)
        Snackbar.make(findViewById(android.R.id.content), "Filter options would appear here", Snackbar.LENGTH_SHORT).show()
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