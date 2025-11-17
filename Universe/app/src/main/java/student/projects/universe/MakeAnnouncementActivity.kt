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

        // Initialize views
        titleEditText = findViewById(R.id.titleEditText)
        contentEditText = findViewById(R.id.contentEditText)
        postButton = findViewById(R.id.postButton)

        // Retrieve module info
        moduleId = intent.getStringExtra("moduleId") ?: ""
        moduleTitle = intent.getStringExtra("moduleTitle") ?: "Unknown Module"

        Log.d(TAG, "Received moduleId: '$moduleId'")
        Log.d(TAG, "Received moduleTitle: '$moduleTitle'")

        postButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val content = contentEditText.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val announcement = AnnouncementRequest(
                title = title,
                content = content,
                moduleId = moduleId,
                moduleTitle = moduleTitle
            )

            val db = DatabaseHelper(this)

            // API call
            ApiClient.announcementApi.createAnnouncement(announcement)
                .enqueue(object : Callback<AnnouncementResponse> {
                    override fun onResponse(
                        call: Call<AnnouncementResponse>,
                        response: Response<AnnouncementResponse>
                    ) {
                        if (response.isSuccessful) {
                            val postedAnnouncement = response.body()

                            if (postedAnnouncement != null) {
                                db.insertOrUpdateAnnouncement(postedAnnouncement)
                            }

                            Toast.makeText(
                                this@MakeAnnouncementActivity,
                                "Announcement posted!",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()
                        } else {
                            Toast.makeText(
                                this@MakeAnnouncementActivity,
                                "Failed to post announcement",
                                Toast.LENGTH_SHORT
                            ).show()

                            Log.e(TAG, "Error: ${response.code()} ${response.message()}")
                        }
                    }

                    override fun onFailure(call: Call<AnnouncementResponse>, t: Throwable) {
                        Toast.makeText(
                            this@MakeAnnouncementActivity,
                            "Offline mode: Saved locally",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.e(TAG, "Network error, saving offline")

                        val offlineAnnouncement = AnnouncementResponse(
                            id = System.currentTimeMillis().toInt(), // local temporary ID
                            title = title,
                            content = content,
                            moduleId = moduleId,
                            moduleTitle = moduleTitle,
                            date = java.time.LocalDate.now().toString()
                        )

                        db.insertOrUpdateAnnouncement(offlineAnnouncement)

                        finish()
                    }
                })
        }
    }
}







/*
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

/**
 * Activity for lecturers/admins to create and post announcements for a specific module.
 * Includes input validation, API call with Retrofit, and user feedback.
 */
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

        // Initialize views
        titleEditText = findViewById(R.id.titleEditText)
        contentEditText = findViewById(R.id.contentEditText)
        postButton = findViewById(R.id.postButton)

        // Retrieve module info from Intent extras with defaults
        moduleId = intent.getStringExtra("moduleId") ?: ""
        moduleTitle = intent.getStringExtra("moduleTitle") ?: "Untitled Module"

        Log.d(TAG, "Received moduleId: '$moduleId'")
        Log.d(TAG, "Received moduleTitle: '$moduleTitle'")

        postButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val content = contentEditText.text.toString().trim()

            // Validate input fields
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Post attempt failed due to empty title or content")
                return@setOnClickListener
            }

            Log.d(TAG, "Posting announcement with title: '$title', content length: ${content.length}, moduleId: '$moduleId', moduleTitle: '$moduleTitle'")

            val announcement = AnnouncementRequest(
                title = title,
                content = content,
                moduleId = moduleId,
                moduleTitle = moduleTitle
            )

            // Make API call to create announcement
            ApiClient.announcementApi.createAnnouncement(announcement)
                .enqueue(object : Callback<AnnouncementResponse> {
                    override fun onResponse(
                        call: Call<AnnouncementResponse>,
                        response: Response<AnnouncementResponse>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@MakeAnnouncementActivity, "Announcement posted!", Toast.LENGTH_SHORT).show()
                            Log.i(TAG, "Announcement posted successfully for moduleId: $moduleId")
                            finish()
                        } else {
                            Toast.makeText(this@MakeAnnouncementActivity, "Failed to post announcement", Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Failed to post announcement. Response code: ${response.code()}, message: ${response.message()}")
                        }
                    }

                    override fun onFailure(call: Call<AnnouncementResponse>, t: Throwable) {
                        Toast.makeText(this@MakeAnnouncementActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Network error while posting announcement", t)
                    }
                })
        }
    }
}
*/