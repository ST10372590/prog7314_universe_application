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

    // Adapter for displaying uploaded assessments
    private lateinit var assessmentAdapter: AssessmentAdapter

    // Variables for form data
    private var selectedDueDate: String = ""
    private var selectedFileUri: Uri? = null
    private var courseId: String = ""
    private var moduleId: String = ""

    private val PICK_FILE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_assessment)

        // Initialize UI elements
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etMaxMarks = findViewById(R.id.etMaxMarks)
        tvDueDate = findViewById(R.id.tvDueDate)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnAttachFile = findViewById(R.id.btnAttachFile)
        rvAssessments = findViewById(R.id.rvAssessments)
        tvSelectedFileName = findViewById(R.id.tvSelectedFileName)

        // Retrieve the course and module IDs passed from the previous activity
        courseId = intent.getStringExtra("courseId") ?: ""
        moduleId = intent.getStringExtra("moduleId") ?: ""

        // Set up RecyclerView for displaying uploaded assessments
        assessmentAdapter = AssessmentAdapter(mutableListOf()) { fileUrl ->
            openFile(fileUrl)
        }
        rvAssessments.adapter = assessmentAdapter
        rvAssessments.layoutManager = LinearLayoutManager(this)

        // Load assessments that have already been uploaded for this module
        loadAssessments()

        // Button click listeners
        btnSelectDate.setOnClickListener { showDatePicker() }
        btnAttachFile.setOnClickListener { pickFile() }
        btnSubmit.setOnClickListener { submitAssessment() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                selectedDueDate = sdf.format(calendar.time)
                tvDueDate.text = "Due Date: $selectedDueDate"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let {
                val fileName = getFileName(it)
                tvSelectedFileName.text = fileName
                Toast.makeText(this, "Selected: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    private fun loadAssessments() {
        Log.d("UploadAssessment", "Loading assessments for moduleId: '$moduleId'")

        ApiClient.assessmentApi.getAssessmentsByModule(moduleId)
            .enqueue(object : Callback<List<AssessmentResponse>> {
                override fun onResponse(
                    call: Call<List<AssessmentResponse>>,
                    response: Response<List<AssessmentResponse>>
                ) {
                    Log.d("UploadAssessment", "Response code: ${response.code()}")
                    Log.d("UploadAssessment", "Response body: ${response.body()}")

                    if (response.isSuccessful && response.body() != null) {
                        val modules = response.body()!!

                        if (modules.isEmpty()) {
                            Log.w("UploadAssessment", "No assessments found for moduleId: $moduleId")
                            Toast.makeText(
                                this@UploadAssessmentActivity,
                                "No Assessments Uploaded Yet for this module.",
                                Toast.LENGTH_LONG
                            ).show()
                            assessmentAdapter.setAssessments(emptyList())
                            showNoAssessmentsMessage(true)
                        } else {
                            Log.d("UploadAssessment", "Assessments loaded: ${modules.size}")
                            val assessments = modules.map { resp ->
                                Assessment(
                                    assessmentID = resp.assessmentID,
                                    title = resp.title,
                                    description = resp.description,
                                    dueDate = resp.dueDate,
                                    maxMarks = resp.maxMarks,
                                    fileUrl = resp.fileUrl
                                )
                            }
                            assessmentAdapter.setAssessments(assessments)
                            showNoAssessmentsMessage(false)
                        }

                    } else {
                        Toast.makeText(
                            this@UploadAssessmentActivity,
                            "No Uploaded Assessments",
                            Toast.LENGTH_LONG
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

    private fun showNoAssessmentsMessage(show: Boolean) {
        val noDataTextView = findViewById<TextView>(R.id.tvNoAssessments)
        noDataTextView.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun submitAssessment() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val maxMarksText = etMaxMarks.text.toString().trim()

        if (title.isEmpty() || description.isEmpty() || maxMarksText.isEmpty() || selectedDueDate.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        val maxMarks = maxMarksText.toIntOrNull()
        if (maxMarks == null || maxMarks <= 0) {
            Toast.makeText(this, "Max Marks must be a positive number", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedDueDate = try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDueDate)
            val sdfUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdfUtc.timeZone = TimeZone.getTimeZone("UTC")
            sdfUtc.format(parsedDate ?: Date())
        } catch (e: Exception) {
            selectedDueDate
        }

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
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to read selected file", Toast.LENGTH_SHORT).show()
                return
            }
        }

        ApiClient.assessmentApi.createAssessment(
            titlePart, descPart, duePart, marksPart, coursePart, modulePart, filePart
        ).enqueue(object : Callback<AssessmentResponse> {
            override fun onResponse(call: Call<AssessmentResponse>, response: Response<AssessmentResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@UploadAssessmentActivity, "Assessment uploaded!", Toast.LENGTH_SHORT).show()
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

                    // ✅ NEW: Push notification data to Firebase Realtime Database
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

                    // Reset form after successful upload
                    etTitle.text.clear()
                    etDescription.text.clear()
                    etMaxMarks.text.clear()
                    tvDueDate.text = "Select Due Date"
                    tvSelectedFileName.text = "No file selected"
                    selectedDueDate = ""
                    selectedFileUri = null
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UploadAssessment", "Failed to upload assessment: ${response.code()} - $errorBody")
                    Toast.makeText(this@UploadAssessmentActivity, "Failed to upload. Check logs.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AssessmentResponse>, t: Throwable) {
                Log.e("UploadAssessment", "Error uploading assessment", t)
                Toast.makeText(this@UploadAssessmentActivity, "Error uploading assessment", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openFile(fileUrl: String?) {
        fileUrl?.let {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(it), "*/*")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
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
