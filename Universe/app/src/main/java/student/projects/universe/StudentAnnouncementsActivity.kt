package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Activity to display a list of announcements for students.
 * Loads announcements from backend API and displays them in a ListView.
 * Includes error handling and user feedback via Toast messages.
 *
 * Implements:
 * - Retrofit asynchronous API calls (Aram, 2023)
 * - Logs for network responses and errors (Patel, 2025)
 */
class StudentAnnouncementsActivity : AppCompatActivity() {

    // UI Components
    private lateinit var listView: ListView
    private lateinit var adapter: AnnouncementAdapter

    // Data source for announcements
    private var announcements: MutableList<AnnouncementResponse> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_announcements)
        Log.d("StudentAnnouncements", "Activity started") // (Patel, 2025)

        // Initialize ListView and Adapter
        listView = findViewById(R.id.announcementListView)
        adapter = AnnouncementAdapter(this, announcements)
        listView.adapter = adapter

        // Fetch announcements from API
        fetchAnnouncements()
    }

    /**
     * Fetch all announcements from the backend API asynchronously.
     * On success, updates the announcements list and refreshes the adapter.
     * On failure, shows a Toast and logs the error.
     */
    private fun fetchAnnouncements() {
        Log.d("StudentAnnouncements", "Fetching announcements from API") // (Aram, 2023)

        ApiClient.announcementApi.getAllAnnouncements().enqueue(object : Callback<List<AnnouncementResponse>> {
            override fun onResponse(
                call: Call<List<AnnouncementResponse>>,
                response: Response<List<AnnouncementResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("StudentAnnouncements", "Announcements loaded successfully: ${response.body()!!.size} items")
                    announcements.clear()
                    announcements.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                } else {
                    Log.w("StudentAnnouncements", "No announcements found or response unsuccessful. Code: ${response.code()}")
                    Toast.makeText(this@StudentAnnouncementsActivity, "No announcements found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<AnnouncementResponse>>, t: Throwable) {
                Log.e("StudentAnnouncements", "Failed to load announcements", t)
                Toast.makeText(this@StudentAnnouncementsActivity, "Error loading announcements: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

/*

Reference List

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at:  https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

*/
