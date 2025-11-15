package student.projects.universe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FileSharingActivity : AppCompatActivity() {

    private lateinit var recyclerFiles: RecyclerView
    private lateinit var tvNoFiles: TextView
    private lateinit var btnUpload: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvModuleTitle: TextView
    private lateinit var tvFileCount: TextView
    private lateinit var tvTotalFiles: TextView
    private lateinit var tvRecentUploads: TextView
    private lateinit var tvStorageUsed: TextView
    private lateinit var tvFooter: TextView

    private lateinit var adapter: FileAdapter
    private val fileList = mutableListOf<ClassFileResponse>()

    private val PICK_FILE_REQUEST = 100
    private var selectedFileUri: Uri? = null

    private var currentUserId: Int? = null
    private lateinit var fileApi: FileApi
    private lateinit var userApi: UserApi

    private var moduleId: String = ""
    private var moduleTitle: String = ""
    private var courseTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_sharing)

        initializeViews()
        loadIntentData()
        setupUI()
        setupRecyclerView()
        setupClickListeners()

        fileApi = ApiClient.fileApi
        userApi = ApiClient.userApi

        // Load the current user first, then their files
        fetchCurrentUser()
    }

    private fun initializeViews() {
        // Header
        btnBack = findViewById(R.id.btnBack)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

        // Module info
        tvModuleTitle = findViewById(R.id.tvModuleTitle)

        // Upload section
        btnUpload = findViewById(R.id.btnUpload)
        progressBar = findViewById(R.id.progressBar)

        // Files section
        recyclerFiles = findViewById(R.id.recyclerFiles)
        tvNoFiles = findViewById(R.id.tvNoFiles)
        tvFileCount = findViewById(R.id.tvFileCount)

        // Statistics
        tvTotalFiles = findViewById(R.id.tvTotalFiles)
        tvRecentUploads = findViewById(R.id.tvRecentUploads)
        tvStorageUsed = findViewById(R.id.tvStorageUsed)

        // Footer
        tvFooter = findViewById(R.id.tvFooter)
    }

    private fun loadIntentData() {
        moduleId = intent.getStringExtra("moduleId") ?: ""
        moduleTitle = intent.getStringExtra("moduleTitle") ?: "Untitled Module"
        courseTitle = intent.getStringExtra("courseTitle") ?: "Unknown Course"
    }

    private fun setupUI() {
        // Update header
        tvHeaderTitle.text = "File Sharing - $moduleTitle"

        // Update module info
        tvModuleTitle.text = "Module: $moduleTitle\nCourse: $courseTitle"

        // Update footer
        tvFooter.text = "© 2025 Universe App - $moduleTitle"
    }

    private fun setupRecyclerView() {
        recyclerFiles.layoutManager = LinearLayoutManager(this)
        adapter = FileAdapter(fileList)
        recyclerFiles.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST)
        }
    }

    private fun fetchCurrentUser() {
        progressBar.visibility = View.VISIBLE

        userApi.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    currentUserId = response.body()!!.userID
                    loadFiles()
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@FileSharingActivity, "Failed to load user info", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadFiles() {
        val userId = currentUserId ?: return
        progressBar.visibility = View.VISIBLE

        fileApi.getFilesByUploader(userId).enqueue(object : Callback<List<ClassFileResponse>> {
            override fun onResponse(
                call: Call<List<ClassFileResponse>>,
                response: Response<List<ClassFileResponse>>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    fileList.clear()
                    fileList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                    updateFileStatistics()
                    tvNoFiles.visibility = if (fileList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@FileSharingActivity, "Failed to load files.", Toast.LENGTH_SHORT).show()
                    updateFileStatistics()
                }
            }

            override fun onFailure(call: Call<List<ClassFileResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                updateFileStatistics()
            }
        })
    }

    private fun updateFileStatistics() {
        // Update file count
        val totalFiles = fileList.size
        tvFileCount.text = "$totalFiles ${if (totalFiles == 1) "file" else "files"}"
        tvTotalFiles.text = totalFiles.toString()

        // Calculate recent uploads (files uploaded in the last 7 days)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val oneWeekAgo = calendar.time

        val recentUploads = fileList.count { file ->
            try {
                val uploadDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(file.uploadDate ?: "")
                uploadDate != null && uploadDate.after(oneWeekAgo)
            } catch (e: Exception) {
                false
            }
        }
        tvRecentUploads.text = recentUploads.toString()

        // Calculate storage used (approximate - you might want to get actual file sizes from your API)
        val storageUsed = fileList.size * 2 // Assuming average 2MB per file
        tvStorageUsed.text = "${storageUsed} MB"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let { uploadFile(it) }
        }
    }

    private fun uploadFile(uri: Uri) {
        val userId = currentUserId ?: return Toast.makeText(this, "User not loaded yet", Toast.LENGTH_SHORT).show()

        // Show loading state
        btnUpload.isEnabled = false
        btnUpload.text = "Uploading..."
        progressBar.visibility = View.VISIBLE

        val fileName = getFileName(uri)
        val fileType = contentResolver.getType(uri) ?: "application/octet-stream"

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            outputStream.close()

            val request = ClassFileRequest(
                uploaderID = userId,
                fileName = fileName,
                fileType = fileType,
                filePath = "uploads/$fileName",
                uploadDate = java.time.LocalDateTime.now().toString()
            )

            fileApi.uploadFile(request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    btnUpload.isEnabled = true
                    btnUpload.text = "Upload File"
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        Toast.makeText(this@FileSharingActivity, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                        loadFiles() // Reload files to show the new upload
                    } else {
                        Toast.makeText(this@FileSharingActivity, "Upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    btnUpload.isEnabled = true
                    btnUpload.text = "Upload File"
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            btnUpload.isEnabled = true
            btnUpload.text = "Upload File"
            progressBar.visibility = View.GONE
            Toast.makeText(this@FileSharingActivity, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) name = it.getString(nameIndex)
        }
        return name
    }

    override fun onResume() {
        super.onResume()
        // Refresh files when returning to this activity
        if (currentUserId != null) {
            loadFiles()
        }
    }
}
/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */