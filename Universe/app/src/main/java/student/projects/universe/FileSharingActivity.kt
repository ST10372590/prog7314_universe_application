package student.projects.universe

import android.app.Activity
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
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FileSharingActivity : AppCompatActivity() {

    // UI Components
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

    // Adapter and file list to display
    private lateinit var adapter: FileAdapter
    private val fileList = mutableListOf<ClassFileResponse>()

    private val PICK_FILE_REQUEST = 100
    private var selectedFileUri: Uri? = null

    private var currentUserId: Int? = null
    private lateinit var fileApi: FileApi
    private lateinit var userApi: UserApi

    // Module/course info passed via Intent
    private var moduleId: String = ""
    private var moduleTitle: String = ""
    private var courseTitle: String = ""

    companion object {
        private const val TAG = "FileSharingActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_sharing)

        Log.d(TAG, "onCreate: Initializing views and APIs")

        initializeViews()
        loadIntentData()
        setupUI()
        setupRecyclerView()
        setupClickListeners()

        fileApi = ApiClient.fileApi
        userApi = ApiClient.userApi

        // Start by fetching current user info (Patel, 2025)
        fetchCurrentUser()
    }

    /**
     * Bind UI components to views
     */
    private fun initializeViews() {
        Log.d(TAG, "initializeViews: Binding views to variables")
        btnBack = findViewById(R.id.btnBack)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvModuleTitle = findViewById(R.id.tvModuleTitle)
        btnUpload = findViewById(R.id.btnUpload)
        progressBar = findViewById(R.id.progressBar)
        recyclerFiles = findViewById(R.id.recyclerFiles)
        tvNoFiles = findViewById(R.id.tvNoFiles)
        tvFileCount = findViewById(R.id.tvFileCount)
        tvTotalFiles = findViewById(R.id.tvTotalFiles)
        tvRecentUploads = findViewById(R.id.tvRecentUploads)
        tvStorageUsed = findViewById(R.id.tvStorageUsed)
        tvFooter = findViewById(R.id.tvFooter)
    }

    /**
     * Extract module and course info from the Intent extras
     */
    private fun loadIntentData() {
        moduleId = intent.getStringExtra("moduleId") ?: ""
        moduleTitle = intent.getStringExtra("moduleTitle") ?: "Untitled Module"
        courseTitle = intent.getStringExtra("courseTitle") ?: "Unknown Course"
        Log.d(TAG, "loadIntentData: moduleId=$moduleId, moduleTitle=$moduleTitle, courseTitle=$courseTitle")
    }

    /**
     * Setup initial UI text and headers
     */
    private fun setupUI() {
        Log.d(TAG, "setupUI: Setting headers and footer texts")
        tvHeaderTitle.text = "File Sharing - $moduleTitle"
        tvModuleTitle.text = "Module: $moduleTitle\nCourse: $courseTitle"
        tvFooter.text = "© 2025 Universe App - $moduleTitle"
    }

    /**
     * Setup RecyclerView with linear layout and adapter
     * Uses Kotlin’s concise syntax for setting layout manager (Patel, 2025)
     */
    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Initializing RecyclerView and adapter")
        recyclerFiles.layoutManager = LinearLayoutManager(this)
        adapter = FileAdapter(fileList)
        recyclerFiles.adapter = adapter
    }

    /**
     * Setup button click listeners, including back button and file picker intent
     */
    private fun setupClickListeners() {
        Log.d(TAG, "setupClickListeners: Assigning click listeners")

        btnBack.setOnClickListener {
            Log.d(TAG, "Back button clicked - finishing activity")
            onBackPressed()
        }

        btnUpload.setOnClickListener {
            Log.d(TAG, "Upload button clicked - launching file picker")
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
            }
            startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST)
        }
    }

    /**
     * Fetch the currently logged-in user information from API
     * Handles success and failure cases with proper UI feedback (Patel, 2025)
     */
    private fun fetchCurrentUser() {
        Log.d(TAG, "fetchCurrentUser: Fetching current user info from API")
        progressBar.visibility = View.VISIBLE

        userApi.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    currentUserId = response.body()!!.userID
                    Log.d(TAG, "fetchCurrentUser: Current user ID = $currentUserId")
                    loadFiles()
                } else {
                    Log.w(TAG, "fetchCurrentUser: Failed to get current user info")
                    Toast.makeText(this@FileSharingActivity, "Failed to load user info", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e(TAG, "fetchCurrentUser: API call failed", t)
                Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Load files uploaded by the current user from the API
     * Shows loading progress and handles success/failure responses (Patel, 2025)
     */
    private fun loadFiles() {
        val userId = currentUserId ?: run {
            Log.w(TAG, "loadFiles: currentUserId is null, skipping load")
            return
        }

        Log.d(TAG, "loadFiles: Loading files for userId=$userId")
        progressBar.visibility = View.VISIBLE

        fileApi.getFilesByUploader(userId).enqueue(object : Callback<List<ClassFileResponse>> {
            override fun onResponse(call: Call<List<ClassFileResponse>>, response: Response<List<ClassFileResponse>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    fileList.clear()
                    fileList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                    Log.d(TAG, "loadFiles: Loaded ${fileList.size} files")
                    updateFileStatistics()
                    tvNoFiles.visibility = if (fileList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Log.w(TAG, "loadFiles: API response unsuccessful")
                    Toast.makeText(this@FileSharingActivity, "Failed to load files.", Toast.LENGTH_SHORT).show()
                    updateFileStatistics()
                }
            }

            override fun onFailure(call: Call<List<ClassFileResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e(TAG, "loadFiles: API call failed", t)
                Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                updateFileStatistics()
            }
        })
    }

    /**
     * Update UI to show file counts, recent uploads (last 7 days), and approximate storage used
     * Demonstrates Kotlin collection operations and date parsing (Patel, 2025)
     */
    private fun updateFileStatistics() {
        Log.d(TAG, "updateFileStatistics: Calculating stats for UI")

        // Total files count
        val totalFiles = fileList.size
        tvFileCount.text = "$totalFiles ${if (totalFiles == 1) "file" else "files"}"
        tvTotalFiles.text = totalFiles.toString()

        // Calculate recent uploads: files uploaded within last 7 days
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val oneWeekAgo = calendar.time

        val recentUploads = fileList.count { file ->
            try {
                val uploadDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(file.uploadDate ?: "")
                uploadDate != null && uploadDate.after(oneWeekAgo)
            } catch (e: Exception) {
                Log.w(TAG, "updateFileStatistics: Failed to parse upload date", e)
                false
            }
        }
        tvRecentUploads.text = recentUploads.toString()

        // Approximate storage used (assumed 2 MB per file)
        val storageUsed = fileList.size * 2
        tvStorageUsed.text = "$storageUsed MB"
    }

    /**
     * Handle result from file picker intent, start upload if file selected
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            Log.d(TAG, "onActivityResult: File selected $selectedFileUri")
            selectedFileUri?.let { uploadFile(it) }
        }
    }

    /**
     * Upload the selected file to the server
     * Reads file content from Uri and constructs API request
     * Shows progress and handles API callbacks with UI updates (Patel, 2025)
     */
    private fun uploadFile(uri: Uri) {
        val userId = currentUserId ?: run {
            Toast.makeText(this, "User not loaded yet", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "uploadFile: currentUserId null, aborting upload")
            return
        }

        Log.d(TAG, "uploadFile: Starting upload for user $userId")

        // Disable upload button and show progress
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

            // Prepare request object
            val request = ClassFileRequest(
                uploaderID = userId,
                fileName = fileName,
                fileType = fileType,
                filePath = "uploads/$fileName",  // Adjust as per your backend storage path
                uploadDate = java.time.LocalDateTime.now().toString()
            )

            fileApi.uploadFile(request).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    btnUpload.isEnabled = true
                    btnUpload.text = "Upload File"
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        Log.d(TAG, "uploadFile: Upload successful")
                        Toast.makeText(this@FileSharingActivity, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                        loadFiles() // Refresh file list
                    } else {
                        Log.w(TAG, "uploadFile: Upload failed - response code ${response.code()}")
                        Toast.makeText(this@FileSharingActivity, "Upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    btnUpload.isEnabled = true
                    btnUpload.text = "Upload File"
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "uploadFile: Upload failed", t)
                    Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            btnUpload.isEnabled = true
            btnUpload.text = "Upload File"
            progressBar.visibility = View.GONE
            Log.e(TAG, "uploadFile: Exception reading file", e)
            Toast.makeText(this@FileSharingActivity, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Helper to get the file name from Uri using ContentResolver query (Patel, 2025)
     */
    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) name = it.getString(nameIndex)
        }
        Log.d(TAG, "getFileName: Retrieved file name '$name' from Uri")
        return name
    }

    /**
     * Refresh file list when the activity resumes
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refreshing file list if user loaded")
        if (currentUserId != null) {
            loadFiles()
        }
    }
}
