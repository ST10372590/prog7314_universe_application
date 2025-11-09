package student.projects.universe

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import android.os.CountDownTimer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SubmitAssessmentActivity : AppCompatActivity() {

    private lateinit var etNotes: EditText
    private lateinit var btnAttachFile: Button
    private lateinit var btnSubmit: Button
    private lateinit var tvDueDate: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var tvSelectedFile: TextView

    private var selectedFileUri: Uri? = null
    private var assessmentId: Int = 0
    private var userId: Int = 0
    private var dueDate: String? = null
    private var countDownTimer: CountDownTimer? = null

    private val PICK_FILE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_assessment)

        etNotes = findViewById(R.id.etNotes)
        btnAttachFile = findViewById(R.id.btnSelectFile)
        btnSubmit = findViewById(R.id.btnSubmitWork)
        tvDueDate = findViewById(R.id.tvDueDate)
        tvCountdown = findViewById(R.id.tvCountdown)
        tvSelectedFile = findViewById(R.id.tvSelectedFile)

        assessmentId = intent.getIntExtra("assessmentId", 0)
        userId = ApiClient.currentUserId

        btnAttachFile.setOnClickListener { pickFile() }
        btnSubmit.setOnClickListener { submitAssessment() }

        loadAssessmentDetails()
    }

    private fun loadAssessmentDetails() {
        ApiClient.studentApi.getAssessmentById(assessmentId)
            .enqueue(object : Callback<AssessmentResponse> {
                override fun onResponse(call: Call<AssessmentResponse>, response: Response<AssessmentResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        dueDate = response.body()!!.dueDate
                        tvDueDate.text = "Due Date: $dueDate"
                        startCountdownTimer(dueDate!!)
                    } else {
                        tvDueDate.text = "Due Date: Not Available"
                        tvCountdown.text = "Time remaining: --"
                    }
                }

                override fun onFailure(call: Call<AssessmentResponse>, t: Throwable) {
                    tvDueDate.text = "Due Date: Not Available"
                    tvCountdown.text = "Time remaining: --"
                }
            })
    }

    private fun startCountdownTimer(dueDateStr: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val dueDate = dateFormat.parse(dueDateStr)
            val currentTime = Date()

            if (dueDate != null && dueDate.time > currentTime.time) {
                val remainingTime = dueDate.time - currentTime.time
                countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished)
                        val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                        tvCountdown.text = String.format(
                            Locale.getDefault(),
                            "Time remaining: %d days %02d:%02d:%02d",
                            days, hours, minutes, seconds
                        )
                    }

                    override fun onFinish() {
                        tvCountdown.text = "⚠️ Submission closed — late penalty applies."
                        tvCountdown.setTextColor(Color.RED)
                    }
                }.start()
            } else {
                tvCountdown.text = "⚠️ Submission closed — late penalty applies."
                tvCountdown.setTextColor(Color.RED)
            }
        } catch (e: Exception) {
            tvCountdown.text = "Time remaining: --"
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let {
                Toast.makeText(this, "File selected: ${it.lastPathSegment}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitAssessment() {
        if (assessmentId == 0 || userId == 0) {
            Toast.makeText(this, "Invalid assessment or user", Toast.LENGTH_SHORT).show()
            return
        }

        val idPart = RequestBody.create("text/plain".toMediaTypeOrNull(), assessmentId.toString())
        val userPart = RequestBody.create("text/plain".toMediaTypeOrNull(), userId.toString())

        var filePart: MultipartBody.Part? = null
        selectedFileUri?.let { uri ->
            val file = File(cacheDir, uri.lastPathSegment ?: "tempfile")
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            filePart = MultipartBody.Part.createFormData("File", file.name, requestFile)
        }

        ApiClient.studentApi.submitAssessment(idPart, userPart, filePart)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@SubmitAssessmentActivity, "Submission uploaded successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@SubmitAssessmentActivity, StudentSubmissionsActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@SubmitAssessmentActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

