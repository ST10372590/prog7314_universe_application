package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MakeAnnouncementActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var postButton: Button

    private var moduleId: String = ""
    private var moduleTitle: String = ""

    private val TAG = "MakeAnnouncementActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_announcement)

        titleEditText = findViewById(R.id.titleEditText)
        contentEditText = findViewById(R.id.contentEditText)
        postButton = findViewById(R.id.postButton)

        moduleId = intent.getStringExtra("moduleId") ?: ""
        moduleTitle = intent.getStringExtra("moduleTitle") ?: "Untitled Module"

        Log.d(TAG, "Received moduleId: '$moduleId'")
        Log.d(TAG, "Received moduleTitle: '$moduleTitle'")

        postButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val content = contentEditText.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Posting announcement with title: '$title', content: '$content', moduleId: '$moduleId', moduleTitle: '$moduleTitle'")

            val announcement = AnnouncementRequest(
                title = title,
                content = content,
                moduleId = moduleId,
                moduleTitle = moduleTitle
            )

            ApiClient.announcementApi.createAnnouncement(announcement)
                .enqueue(object : Callback<AnnouncementResponse> {
                    override fun onResponse(
                        call: Call<AnnouncementResponse>,
                        response: Response<AnnouncementResponse>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MakeAnnouncementActivity, "Announcement posted!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@MakeAnnouncementActivity, "Failed to post announcement", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AnnouncementResponse>, t: Throwable) {
                        Toast.makeText(this@MakeAnnouncementActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
}
