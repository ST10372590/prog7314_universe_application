package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CommunicationHubActivity : AppCompatActivity() {

    // UI components for navigation buttons // (Patel, 2025)
    private lateinit var btnDirectMessages: Button
    private lateinit var btnAnnouncements: Button

    override fun onCreate(savedInstanceState: Bundle?) { // (Patel, 2025)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication_hub)

        // Initialize buttons from layout
        btnDirectMessages = findViewById(R.id.btnDirectMessages)
        btnAnnouncements = findViewById(R.id.btnAnnouncements)

        Log.d("CommunicationHub", "Activity created and buttons initialized")

        // Navigate to Direct Messages screen // (Patel, 2025)
        btnDirectMessages.setOnClickListener {
            Log.d("CommunicationHub", "Direct Messages button clicked")
            val intent = Intent(this, DirectMessageActivity::class.java)
            startActivity(intent)
            Log.d("CommunicationHub", "Navigating to DirectMessageActivity")
        }

        // Show coming soon message for Announcements // (Patel, 2025)
        btnAnnouncements.setOnClickListener {
            startActivity(Intent(this, StudentAnnouncementsActivity::class.java))
        }

        Log.d("CommunicationHub", "All click listeners set successfully") // (Patel, 2025)
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */