package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DirectMessageActivity : AppCompatActivity() {

    private lateinit var recyclerUsers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnShowStudents: Button
    private lateinit var btnShowLecturers: Button
    private lateinit var searchViewUsers: SearchView

    private val usersApi = ApiClient.userApi
    private lateinit var adapter: StudentListAdapter

    private val allUsers = mutableListOf<UserResponse>()
    private val displayedUsers = mutableListOf<UserResponse>()
    private var currentUserId = 0
    private var showingStudents = true

    // Firebase root reference
    private val db = FirebaseDatabase.getInstance().reference

    // Keep track of Firebase listeners to avoid duplicates or leaks
    private val listenersMap = mutableMapOf<String, ValueEventListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direct_message)

        recyclerUsers = findViewById(R.id.recyclerUsers)
        progressBar = findViewById(R.id.progressBar)
        btnShowStudents = findViewById(R.id.btnShowStudents)
        btnShowLecturers = findViewById(R.id.btnShowLecturers)
        searchViewUsers = findViewById(R.id.searchViewUsers)

        recyclerUsers.layoutManager = LinearLayoutManager(this)
        recyclerUsers.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        currentUserId = intent.getIntExtra("currentUserId", ApiClient.currentUserId) // fallback
        Log.d("DirectMessageActivity", "Current User ID: $currentUserId")

        adapter = StudentListAdapter(displayedUsers) { selectedUser ->
            val intent = Intent(this@DirectMessageActivity, ChatActivity::class.java)
            intent.putExtra("currentUserId", currentUserId)
            intent.putExtra("receiverId", selectedUser.userID)
            intent.putExtra("receiverName", "${selectedUser.firstName} ${selectedUser.lastName}")
            startActivity(intent)
        }
        recyclerUsers.adapter = adapter

        btnShowStudents.setOnClickListener {
            showingStudents = true
            updateCategoryUI()
            filterUsers()
        }
        btnShowLecturers.setOnClickListener {
            showingStudents = false
            updateCategoryUI()
            filterUsers()
        }

        searchViewUsers.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText)
                return true
            }
        })

        loadAllUsers()
    }

    private fun loadAllUsers() {
        progressBar.visibility = View.VISIBLE
        usersApi.getAllUsers().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(
                call: Call<List<UserResponse>>,
                response: Response<List<UserResponse>>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    allUsers.clear()
                    allUsers.addAll(response.body()!!)
                    startListeningUnreadCounts()  // start listeners after users are loaded
                    filterUsers()
                } else {
                    Toast.makeText(this@DirectMessageActivity, "Failed to load users.", Toast.LENGTH_SHORT).show()
                    Log.e("DirectMessageActivity", "Error code: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@DirectMessageActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("DirectMessageActivity", "Network error", t)
            }
        })
    }

    private fun startListeningUnreadCounts() {
        // Remove previous listeners if any, to avoid duplicates
        listenersMap.forEach { (convId, listener) ->
            db.child("conversations").child(convId).child("meta").removeEventListener(listener)
        }
        listenersMap.clear()

        allUsers.forEach { other ->
            if (other.userID == currentUserId) return@forEach

            val convId = getConversationId(currentUserId, other.userID)
            val metaRef = db.child("conversations").child(convId).child("meta")

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val unreadKey = "unread_${currentUserId}"
                    val unread = snapshot.child(unreadKey).getValue(Int::class.java) ?: 0

                    // Update unread count in allUsers
                    val allIndex = allUsers.indexOfFirst { it.userID == other.userID }
                    if (allIndex >= 0) {
                        allUsers[allIndex] = allUsers[allIndex].copy(unreadCount = unread)
                    }

                    // Update unread count in displayedUsers if visible
                    val dispIndex = displayedUsers.indexOfFirst { it.userID == other.userID }
                    if (dispIndex >= 0) {
                        displayedUsers[dispIndex] = displayedUsers[dispIndex].copy(unreadCount = unread)
                        adapter.notifyItemChanged(dispIndex)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("DirectMessageActivity", "meta listener cancelled for $convId: ${error.message}")
                }
            }

            metaRef.addValueEventListener(listener)
            listenersMap[convId] = listener
        }
    }

    private fun filterUsers(query: String? = null) {
        displayedUsers.clear()
        val filteredByRole = if (showingStudents) {
            allUsers.filter { it.role.equals("Student", ignoreCase = true) }
        } else {
            allUsers.filter { it.role.equals("Lecturer", ignoreCase = true) }
        }

        val finalFiltered = query?.let {
            filteredByRole.filter { user ->
                user.firstName.contains(it, ignoreCase = true) ||
                        user.lastName.contains(it, ignoreCase = true) ||
                        user.username.contains(it, ignoreCase = true)
            }
        } ?: filteredByRole

        displayedUsers.addAll(finalFiltered)
        adapter.notifyDataSetChanged()
    }

    private fun updateCategoryUI() {
        if (showingStudents) {
            btnShowStudents.setBackgroundTintList(getColorStateList(R.color.maroon))
            btnShowStudents.setTextColor(getColor(android.R.color.white))
            btnShowLecturers.setBackgroundTintList(getColorStateList(android.R.color.darker_gray))
            btnShowLecturers.setTextColor(getColor(android.R.color.black))
        } else {
            btnShowLecturers.setBackgroundTintList(getColorStateList(R.color.maroon))
            btnShowLecturers.setTextColor(getColor(android.R.color.white))
            btnShowStudents.setBackgroundTintList(getColorStateList(android.R.color.darker_gray))
            btnShowStudents.setTextColor(getColor(android.R.color.black))
        }
    }

    private fun getConversationId(a: Int, b: Int): String {
        return if (a < b) "${a}_${b}" else "${b}_${a}"
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove all Firebase listeners on destroy to prevent leaks
        listenersMap.forEach { (convId, listener) ->
            db.child("conversations").child(convId).child("meta").removeEventListener(listener)
        }
        listenersMap.clear()
    }
}



/*
package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DirectMessageActivity : AppCompatActivity() {

    private lateinit var recyclerUsers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnShowStudents: Button
    private lateinit var btnShowLecturers: Button
    private lateinit var searchViewUsers: SearchView

    private val usersApi = ApiClient.userApi
    private lateinit var adapter: StudentListAdapter

    private val allUsers = mutableListOf<UserResponse>()
    private val displayedUsers = mutableListOf<UserResponse>()
    private var currentUserId = 0
    private var showingStudents = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direct_message)

        // Initialize UI
        recyclerUsers = findViewById(R.id.recyclerUsers)
        progressBar = findViewById(R.id.progressBar)
        btnShowStudents = findViewById(R.id.btnShowStudents)
        btnShowLecturers = findViewById(R.id.btnShowLecturers)
        searchViewUsers = findViewById(R.id.searchViewUsers)

        recyclerUsers.layoutManager = LinearLayoutManager(this)
        recyclerUsers.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        currentUserId = intent.getIntExtra("currentUserId", 0)
        Log.d("DirectMessageActivity", "Current User ID: $currentUserId")

        // Initialize adapter
        adapter = StudentListAdapter(displayedUsers) { selectedUser ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("currentUserId", currentUserId)
            intent.putExtra("receiverId", selectedUser.userID)
            intent.putExtra("receiverName", "${selectedUser.firstName} ${selectedUser.lastName}")
            startActivity(intent)
        }
        recyclerUsers.adapter = adapter

        // Category buttons
        btnShowStudents.setOnClickListener {
            showingStudents = true
            updateCategoryUI()
            filterUsers()
        }
        btnShowLecturers.setOnClickListener {
            showingStudents = false
            updateCategoryUI()
            filterUsers()
        }

        // Search functionality
        searchViewUsers.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText)
                return true
            }
        })

        // Load all users from API
        loadAllUsers()
    }

    private fun loadAllUsers() {
        progressBar.visibility = View.VISIBLE
        usersApi.getAllUsers().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(
                call: Call<List<UserResponse>>,
                response: Response<List<UserResponse>>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    allUsers.clear()
                    allUsers.addAll(response.body()!!)
                    filterUsers()
                } else {
                    Toast.makeText(this@DirectMessageActivity, "Failed to load users.", Toast.LENGTH_SHORT).show()
                    Log.e("DirectMessageActivity", "Error code: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@DirectMessageActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("DirectMessageActivity", "Network error", t)
            }
        })
    }

    private fun filterUsers(query: String? = null) {
        displayedUsers.clear()

        val filteredByRole = if (showingStudents) {
            allUsers.filter { it.role.equals("Student", ignoreCase = true) }
        } else {
            allUsers.filter { it.role.equals("Lecturer", ignoreCase = true) }
        }

        val finalFiltered = query?.let {
            filteredByRole.filter { user ->
                user.firstName.contains(it, ignoreCase = true) ||
                        user.lastName.contains(it, ignoreCase = true) ||
                        user.username.contains(it, ignoreCase = true)
            }
        } ?: filteredByRole

        displayedUsers.addAll(finalFiltered)
        adapter.notifyDataSetChanged()
    }

    private fun updateCategoryUI() {
        if (showingStudents) {
            btnShowStudents.backgroundTintList = getColorStateList(R.color.maroon)
            btnShowStudents.setTextColor(getColor(android.R.color.white))
            btnShowLecturers.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            btnShowLecturers.setTextColor(getColor(android.R.color.black))
        } else {
            btnShowLecturers.backgroundTintList = getColorStateList(R.color.maroon)
            btnShowLecturers.setTextColor(getColor(android.R.color.white))
            btnShowStudents.backgroundTintList = getColorStateList(android.R.color.darker_gray)
            btnShowStudents.setTextColor(getColor(android.R.color.black))
        }
    }
}
*/