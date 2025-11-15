package student.projects.universe

import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentAnnouncementsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: AnnouncementAdapter
    private var announcements: MutableList<AnnouncementResponse> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_announcements)

        listView = findViewById(R.id.announcementListView)
        adapter = AnnouncementAdapter(this, announcements)
        listView.adapter = adapter

        fetchAnnouncements()
    }

    private fun fetchAnnouncements() {
        ApiClient.announcementApi.getAllAnnouncements().enqueue(object : Callback<List<AnnouncementResponse>> {
            override fun onResponse(
                call: Call<List<AnnouncementResponse>>,
                response: Response<List<AnnouncementResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    announcements.clear()
                    announcements.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@StudentAnnouncementsActivity, "No announcements found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<AnnouncementResponse>>, t: Throwable) {
                Toast.makeText(this@StudentAnnouncementsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("Announcements", "Failed to load announcements", t)
            }
        })
    }
}
