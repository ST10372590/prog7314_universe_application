package student.projects.universe

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for uploading assessments to a specific module.
 * Allows user to select due date, attach files, and upload assessments.
 * Displays list of existing assessments with ability to view files.
 */
class UploadAssessmentActivity : AppCompatActivity() {

    // UI components
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etMaxMarks: EditText
    private lateinit var tvDueDate: TextView
    private lateinit var tvSelectedFileName: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnAttachFile: Button
    private lateinit var rvAssessments: RecyclerView
    private lateinit var tvNoAssessments: TextView
    private lateinit var tvModuleTitle: TextView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvFooter: TextView

    // Adapter for displaying uploaded assessments
    private lateinit var assessmentAdapter: AssessmentAdapter

    // Variables for form data
    private var selectedDueDate: String = ""
    private var selectedFileUri: Uri? = null
    private var courseId: String = ""
    private var moduleId: String = ""
    private var moduleTitle: String = ""
    private var courseTitle: String = ""

    private val PICK_FILE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_assessment)

        initializeViews()
        loadIntentData()
        setupUI()
        setupRecyclerView()
        setupClickListeners()
        loadAssessments()
    }

    /**
     * Initialize all UI views from the layout.
     */
    private fun initializeViews() {
        // Header
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

        // Module info card
        tvModuleTitle = findViewById(R.id.tvModuleTitle)

        // Form elements
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etMaxMarks = findViewById(R.id.etMaxMarks)
        tvDueDate = findViewById(R.id.tvDueDate)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnAttachFile = findViewById(R.id.btnAttachFile)
        tvSelectedFileName = findViewById(R.id.tvSelectedFileName)

        // Assessments section
        rvAssessments = findViewById(R.id.rvAssessments)
        tvNoAssessments = findViewById(R.id.tvNoAssessments)

        // Footer
        tvFooter = findViewById(R.id.tvFooter)
    }

    /**
     * Load course and module data passed from previous Activity.
     */
    private fun loadIntentData() {
        courseId = intent.getStringExtra("courseId") ?: ""
        moduleId = intent.getStringExtra("moduleId") ?: ""
        moduleTitle = intent.getStringExtra("moduleTitle") ?: "Untitled Module"
        courseTitle = intent.getStringExtra("courseTitle") ?: "Unknown Course"

        Log.d("UploadAssessment", "Received courseId: '$courseId', moduleId: '$moduleId', title: '$moduleTitle'") // (Patel, 2025)
    }

    /**
     * Setup UI texts and footer with module and course information.
     */
    private fun setupUI() {
        tvHeaderTitle.text = "Upload Assessment - $moduleTitle"
        tvModuleTitle.text = "Module: $moduleTitle"
        tvFooter.text = "© 2025 Universe App - $moduleTitle"
    }

    /**
     * Setup RecyclerView with adapter to display list of assessments.
     */
    private fun setupRecyclerView() {
        assessmentAdapter = AssessmentAdapter(mutableListOf()) { fileUrl ->
            openFile(fileUrl)
        }
        rvAssessments.adapter = assessmentAdapter
        rvAssessments.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Setup button click listeners for date selection, file picking, and submission.
     */
    private fun setupClickListeners() {
        btnSelectDate.setOnClickListener { showDatePicker() }
        btnAttachFile.setOnClickListener { pickFile() }
        btnSubmit.setOnClickListener { submitAssessment() }
    }

    /**
     * Show a date picker dialog for user to select due date.
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                selectedDueDate = sdf.format(calendar.time)
                tvDueDate.text = selectedDueDate
                Log.d("UploadAssessment", "Due date selected: $selectedDueDate") // (Aram, 2023)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Launches file picker intent to select any file.
     */
    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST)
    }

    /**
     * Handles result of file picker and updates UI with selected file name.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let {
                val fileName = getFileName(it)
                tvSelectedFileName.text = fileName
                Toast.makeText(this, "Selected: $fileName", Toast.LENGTH_SHORT).show()
                Log.d("UploadAssessment", "File selected: $fileName") // (Patel, 2025)
            }
        }
    }

    /**
     * Retrieves the display name of a file given its Uri.
     */
    private fun getFileName(uri: Uri): String {
        var result = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) result = cursor.getString(idx)
            }
        }
        return result
    }

    /**
     * Loads existing assessments for this module from the API.
     */
    private fun loadAssessments() {
        Log.d("UploadAssessment", "Loading assessments for moduleId: '$moduleId'") // (Aram, 2023)

        ApiClient.assessmentApi.getAssessmentsByModule(moduleId)
            .enqueue(object : Callback<List<AssessmentResponse>> {
                override fun onResponse(
                    call: Call<List<AssessmentResponse>>,
                    response: Response<List<AssessmentResponse>>
                ) {
                    Log.d("UploadAssessment", "Response code: ${response.code()}")
                    Log.d("UploadAssessment", "Response body: ${response.body()}")

                    if (response.isSuccessful && response.body() != null) {
                        val assessments = response.body()!!

                        if (assessments.isEmpty()) {
                            Log.w("UploadAssessment", "No assessments found for moduleId: $moduleId")
                            showNoAssessmentsMessage(true)
                        } else {
                            Log.d("UploadAssessment", "Assessments loaded: ${assessments.size}")
                            val assessmentList = assessments.map { resp ->
                                Assessment(
                                    assessmentID = resp.assessmentID,
                                    title = resp.title,
                                    description = resp.description,
                                    dueDate = resp.dueDate,
                                    maxMarks = resp.maxMarks,
                                    fileUrl = resp.fileUrl
                                )
                            }
                            assessmentAdapter.setAssessments(assessmentList)
                            showNoAssessmentsMessage(false)
                        }

                    } else {
                        Toast.makeText(
                            this@UploadAssessmentActivity,
                            "Failed to load assessments",
                            Toast.LENGTH_SHORT
                        ).show()
                        showNoAssessmentsMessage(true)
                    }
                }

                override fun onFailure(call: Call<List<AssessmentResponse>>, t: Throwable) {
                    Log.e("UploadAssessment", "Error loading assessments", t)
                    Toast.makeText(
                        this@UploadAssessmentActivity,
                        "Error loading assessments: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showNoAssessmentsMessage(true)
                }
            })
    }

    /**
     * Shows or hides the "no assessments" message.
     */
    private fun showNoAssessmentsMessage(show: Boolean) {
        tvNoAssessments.visibility = if (show) View.VISIBLE else View.GONE
        rvAssessments.visibility = if (show) View.GONE else View.VISIBLE
    }

    /**
     * Validates form input and submits the assessment with optional file attachment.
     */
    private fun submitAssessment() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val maxMarksText = etMaxMarks.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            etTitle.error = "Assessment title is required"
            etTitle.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Description is required"
            etDescription.requestFocus()
            return
        }

        if (maxMarksText.isEmpty()) {
            etMaxMarks.error = "Maximum marks is required"
            etMaxMarks.requestFocus()
            return
        }

        if (selectedDueDate.isEmpty()) {
            Toast.makeText(this, "Please select a due date", Toast.LENGTH_SHORT).show()
            return
        }

        val maxMarks = maxMarksText.toIntOrNull()
        if (maxMarks == null || maxMarks <= 0) {
            etMaxMarks.error = "Max Marks must be a positive number"
            etMaxMarks.requestFocus()
            return
        }

        // Disable submit button and show uploading state
        btnSubmit.isEnabled = false
        btnSubmit.text = "Uploading..."

        // Format due date to UTC ISO string
        val formattedDueDate = try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDueDate)
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdfUtc.timeZone = TimeZone.getTimeZone("UTC")
            sdfUtc.format(parsedDate ?: Date())
        } catch (e: Exception) {
            selectedDueDate
        }

        // Prepare multipart form data parts
        val titlePart = RequestBody.create("text/plain".toMediaTypeOrNull(), title)
        val descPart = RequestBody.create("text/plain".toMediaTypeOrNull(), description)
        val duePart = RequestBody.create("text/plain".toMediaTypeOrNull(), formattedDueDate)
        val marksPart = RequestBody.create("text/plain".toMediaTypeOrNull(), maxMarks.toString())
        val coursePart = RequestBody.create("text/plain".toMediaTypeOrNull(), courseId)
        val modulePart = RequestBody.create("text/plain".toMediaTypeOrNull(), moduleId)

        var filePart: MultipartBody.Part? = null
        selectedFileUri?.let { uri ->
            try {
                val file = File(cacheDir, getFileName(uri))
                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                Log.d("UploadAssessment", "File part created for upload: ${file.name}") // (Patel, 2025)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to read selected file", Toast.LENGTH_SHORT).show()
                btnSubmit.isEnabled = true
                btnSubmit.text = "Create Assessment"
                Log.e("UploadAssessment", "Error preparing file for upload", e)
                return
            }
        }

        // Make API call to upload assessment with optional file
        ApiClient.assessmentApi.createAssessment(
            titlePart, descPart, duePart, marksPart, coursePart, modulePart, filePart
        ).enqueue(object : Callback<AssessmentResponse> {
            override fun onResponse(call: Call<AssessmentResponse>, response: Response<AssessmentResponse>) {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Create Assessment"

                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@UploadAssessmentActivity, "Assessment uploaded successfully!", Toast.LENGTH_SHORT).show()
                    val body = response.body()!!
                    val uploaded = Assessment(
                        assessmentID = body.assessmentID,
                        title = title,
                        description = description,
                        dueDate = formattedDueDate,
                        maxMarks = maxMarks,
                        fileUrl = body.fileUrl
                    )
                    assessmentAdapter.addAssessment(uploaded)
                    showNoAssessmentsMessage(false)

                    // Push notification to Firebase
                    val notifRef = FirebaseDatabase.getInstance()
                        .getReference("notifications")
                        .child(moduleId)

                    val notifData = mapOf(
                        "title" to "New Assessment Uploaded",
                        "description" to title,
                        "assessmentId" to body.assessmentID,
                        "timestamp" to System.currentTimeMillis()
                    )

                    notifRef.push().setValue(notifData)
                        .addOnSuccessListener {
                            Log.d("UploadAssessment", "Notification pushed to Firebase for moduleId: $moduleId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("UploadAssessment", "Failed to push notification: ${e.message}")
                        }

                    resetForm()
                    loadAssessments()

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UploadAssessment", "Failed to upload assessment: ${response.code()} - $errorBody")
                    Toast.makeText(this@UploadAssessmentActivity, "Failed to upload assessment", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AssessmentResponse>, t: Throwable) {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Create Assessment"
                Log.e("UploadAssessment", "Error uploading assessment", t)
                Toast.makeText(this@UploadAssessmentActivity, "Error uploading assessment: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Resets the form fields and clears selections.
     */
    private fun resetForm() {
        etTitle.text.clear()
        etDescription.text.clear()
        etMaxMarks.text.clear()
        tvDueDate.text = "No date selected"
        tvSelectedFileName.text = "No file selected"
        selectedDueDate = ""
        selectedFileUri = null
        Log.d("UploadAssessment", "Form reset") // (Aram, 2023)
    }

    /**
     * Opens an assessment file using an external app if available.
     */
    private fun openFile(fileUrl: String?) {
        fileUrl?.let {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(it), "*/*")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log.d("UploadAssessment", "Opening file URL: $fileUrl")
            } else {
                Toast.makeText(this, "No app available to open this file", Toast.LENGTH_SHORT).show()
                Log.w("UploadAssessment", "No app found to open file URL: $fileUrl")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAssessments() // Refresh assessments when returning to this activity
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
