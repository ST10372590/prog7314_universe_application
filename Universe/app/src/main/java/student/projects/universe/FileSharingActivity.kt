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

class FileSharingActivity : AppCompatActivity() { // (Patel, 2025)

    private lateinit var recyclerFiles: RecyclerView
    private lateinit var tvNoFiles: TextView
    private lateinit var btnUpload: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: FileAdapter
    private val fileList = mutableListOf<ClassFileResponse>() // (Patel, 2025)

    private val PICK_FILE_REQUEST = 100
    private var selectedFileUri: Uri? = null

    private var currentUserId: Int? = null
    private lateinit var fileApi: FileApi
    private lateinit var userApi: UserApi

    override fun onCreate(savedInstanceState: Bundle?) { // (Patel, 2025)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_sharing)

        recyclerFiles = findViewById(R.id.recyclerFiles)
        tvNoFiles = findViewById(R.id.tvNoFiles)
        btnUpload = findViewById(R.id.btnUpload)
        progressBar = findViewById(R.id.progressBar)

        recyclerFiles.layoutManager = LinearLayoutManager(this)
        adapter = FileAdapter(fileList)
        recyclerFiles.adapter = adapter

        fileApi = ApiClient.fileApi
        userApi = ApiClient.userApi

        btnUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST)
        }

        // Load the current user first, then their files // (Patel, 2025)
        fetchCurrentUser()
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

            override fun onFailure(call: Call<UserResponse>, t: Throwable) { // (Patel, 2025)
                progressBar.visibility = View.GONE
                Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadFiles() { // (Patel, 2025)
        val userId = currentUserId ?: return
        progressBar.visibility = View.VISIBLE

        fileApi.getFilesByUploader(userId).enqueue(object : Callback<List<ClassFileResponse>> {
            override fun onResponse(
                call: Call<List<ClassFileResponse>>,
                response: Response<List<ClassFileResponse>>
            ) { // (Patel, 2025)
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    fileList.clear()
                    fileList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                    tvNoFiles.visibility = if (fileList.isEmpty()) View.VISIBLE else View.GONE // (Patel, 2025)
                } else {
                    Toast.makeText(this@FileSharingActivity, "Failed to load files.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<ClassFileResponse>>, t: Throwable) { // (Patel, 2025)
                progressBar.visibility = View.GONE
                Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { // (Patel, 2025)
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let { uploadFile(it) }
        }
    }

    private fun uploadFile(uri: Uri) { // (Patel, 2025)
        val userId = currentUserId ?: return Toast.makeText(this, "User not loaded yet", Toast.LENGTH_SHORT).show()
        val fileName = getFileName(uri)
        val fileType = contentResolver.getType(uri) ?: "application/octet-stream"
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        outputStream.close()

        val request = ClassFileRequest( // (Patel, 2025)
            uploaderID = userId,
            fileName = fileName,
            fileType = fileType,
            filePath = "uploads/$fileName",
            uploadDate = java.time.LocalDateTime.now().toString()
        )

        progressBar.visibility = View.VISIBLE // (Patel, 2025)

        fileApi.uploadFile(request).enqueue(object : Callback<ApiResponse> { // (Patel, 2025)
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@FileSharingActivity, "File uploaded!", Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    Toast.makeText(this@FileSharingActivity, "Upload failed.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) { // (Patel, 2025)
                progressBar.visibility = View.GONE
                Toast.makeText(this@FileSharingActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getFileName(uri: Uri): String { // (Patel, 2025)
        var name = "unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) name = it.getString(nameIndex)
        }
        return name
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */