package student.projects.universe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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

    // UI components
    private lateinit var recyclerUsers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnShowStudents: Button
    private lateinit var btnShowLecturers: Button
    private lateinit var btnShowAll: Button
    private lateinit var searchViewUsers: SearchView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var ivUserAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvUnreadCount: TextView
    private lateinit var tvUserCount: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvTotalContacts: TextView
    private lateinit var tvOnlineNow: TextView
    private lateinit var tvTotalUnread: TextView
    private lateinit var tvFooter: TextView

    private val usersApi = ApiClient.userApi
    private lateinit var adapter: StudentListAdapter

    // Data containers
    private val allUsers = mutableListOf<UserResponse>()
    private val displayedUsers = mutableListOf<UserResponse>()
    private var currentUserId = 0
    private var currentUser: UserResponse? = null
    private var currentFilter = "all" // Possible values: "all", "students", "lecturers"

    // Firebase reference and listeners map for realtime unread counts
    private val db = FirebaseDatabase.getInstance().reference
    private val listenersMap = mutableMapOf<String, ValueEventListener>()

    // SQLite database helper for caching users locally
    private lateinit var dbHelper: DatabaseHelper

    companion object {
        private const val TAG = "DirectMessageActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direct_message)

        Log.d(TAG, "onCreate: Initializing activity")

        dbHelper = DatabaseHelper(this)

        initializeViews()
        setupUI()
        setupRecyclerView()
        setupClickListeners()

        // Get current user ID from intent or fallback to ApiClient's currentUserId
        currentUserId = intent.getIntExtra("currentUserId", ApiClient.currentUserId)
        Log.d(TAG, "Current user ID: $currentUserId")

        loadCurrentUserInfo()
        loadAllUsers()
    }

    /**
     * Binds UI elements to their corresponding views
     */
    private fun initializeViews() {
        Log.d(TAG, "Initializing views")
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        ivUserAvatar = findViewById(R.id.ivUserAvatar)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserRole = findViewById(R.id.tvUserRole)
        tvUnreadCount = findViewById(R.id.tvUnreadCount)
        searchViewUsers = findViewById(R.id.searchViewUsers)
        btnShowAll = findViewById(R.id.btnShowAll)
        btnShowStudents = findViewById(R.id.btnShowStudents)
        btnShowLecturers = findViewById(R.id.btnShowLecturers)
        recyclerUsers = findViewById(R.id.recyclerUsers)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvUserCount = findViewById(R.id.tvUserCount)
        tvTotalContacts = findViewById(R.id.tvTotalContacts)
        tvOnlineNow = findViewById(R.id.tvOnlineNow)
        tvTotalUnread = findViewById(R.id.tvTotalUnread)
        tvFooter = findViewById(R.id.tvFooter)
    }

    /**
     * Sets footer text and any other UI setups
     */
    private fun setupUI() {
        Log.d(TAG, "Setting up UI")
        tvFooter.text = "© 2025 Universe App - Direct Messages"
    }

    /**
     * Sets up RecyclerView with layout manager, decoration, and adapter.
     * Adapter handles user clicks to open ChatActivity.
     */
    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        recyclerUsers.layoutManager = LinearLayoutManager(this)
        recyclerUsers.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        adapter = StudentListAdapter(displayedUsers) { selectedUser ->
            Log.d(TAG, "User selected: ${selectedUser.userID} - launching ChatActivity")
            val intent = Intent(this@DirectMessageActivity, ChatActivity::class.java)
            intent.putExtra("currentUserId", currentUserId)
            intent.putExtra("receiverId", selectedUser.userID)
            intent.putExtra("receiverName", "${selectedUser.firstName} ${selectedUser.lastName}")
            startActivity(intent)
        }
        recyclerUsers.adapter = adapter
    }

    /**
     * Set click listeners for filter buttons and search view
     */
    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")

        btnShowAll.setOnClickListener {
            Log.d(TAG, "Filter: Show all users")
            currentFilter = "all"
            updateCategoryUI()
            filterUsers()
        }

        btnShowStudents.setOnClickListener {
            Log.d(TAG, "Filter: Show students")
            currentFilter = "students"
            updateCategoryUI()
            filterUsers()
        }

        btnShowLecturers.setOnClickListener {
            Log.d(TAG, "Filter: Show lecturers")
            currentFilter = "lecturers"
            updateCategoryUI()
            filterUsers()
        }

        searchViewUsers.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(TAG, "Search submitted: $query")
                filterUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d(TAG, "Search text changed: $newText")
                filterUsers(newText)
                return true
            }
        })
    }

    /**
     * Loads current user's details from API and updates UI accordingly
     */
    private fun loadCurrentUserInfo() {
        Log.d(TAG, "Loading current user info from API")
        usersApi.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    currentUser = response.body()!!
                    Log.d(TAG, "Current user info loaded: ${currentUser?.userID}")
                    updateUserInfoUI()
                } else {
                    Log.w(TAG, "Failed to load current user info - response unsuccessful")
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e(TAG, "Failed to load current user info", t)
            }
        })
    }

    /**
     * Updates the UI elements related to the current user info like name, role, avatar
     */
    private fun updateUserInfoUI() {
        currentUser?.let { user ->
            tvUserName.text = "${user.firstName} ${user.lastName}"
            tvUserRole.text = user.role ?: "User"

            // Set avatar icon based on role
            when (user.role?.lowercase()) {
                "student" -> ivUserAvatar.setImageResource(android.R.drawable.ic_menu_my_calendar)
                "lecturer" -> ivUserAvatar.setImageResource(android.R.drawable.ic_menu_edit)
                else -> ivUserAvatar.setImageResource(android.R.drawable.ic_menu_my_calendar)
            }
            Log.d(TAG, "Updated current user UI with name and role")
        }
    }

    /**
     * Loads all users from API if online, otherwise loads from local SQLite cache.
     * Updates UI accordingly.
     */
    private fun loadAllUsers() {
        Log.d(TAG, "Loading all users from API or cache")
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        if (isOnline()) {
            Log.d(TAG, "Network available - loading users from API")
            usersApi.getAllUsers().enqueue(object : Callback<List<UserResponse>> {
                override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        allUsers.clear()
                        val otherUsers = response.body()!!.filter { it.userID != currentUserId }
                        allUsers.addAll(otherUsers)

                        Log.d(TAG, "Loaded ${allUsers.size} users from API, caching locally")
                        val userEntities = allUsers.map { userResp ->
                            UserEntity(
                                userID = userResp.userID,
                                firstName = userResp.firstName,
                                lastName = userResp.lastName,
                                username = userResp.username,
                                role = userResp.role,
                                unreadCount = userResp.unreadCount ?: 0
                            )
                        }
                        dbHelper.insertOrUpdateUsers(userEntities)

                        startListeningUnreadCounts()
                        filterUsers()
                        updateStatistics()
                    } else {
                        Log.w(TAG, "API response unsuccessful or empty - loading from cache")
                        Toast.makeText(this@DirectMessageActivity, "Failed to load users.", Toast.LENGTH_SHORT).show()
                        loadUsersFromCache()
                    }
                }

                override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e(TAG, "Network error loading users: ${t.localizedMessage}", t)
                    Toast.makeText(this@DirectMessageActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                    loadUsersFromCache()
                }
            })
        } else {
            Log.d(TAG, "No network - loading users from cache")
            loadUsersFromCache()
            progressBar.visibility = View.GONE
        }
    }

    /**
     * Loads users from local SQLite cache and updates UI
     */
    private fun loadUsersFromCache() {
        Log.d(TAG, "Loading users from local SQLite cache")
        allUsers.clear()
        val userEntities = dbHelper.getAllUsers()

        // Map entities to UserResponse and exclude current user
        val userResponses = userEntities.map { entity ->
            UserResponse(
                userID = entity.userID,
                firstName = entity.firstName,
                lastName = entity.lastName,
                email = entity.lastName, // Note: looks like a typo? kept as original
                username = entity.username,
                role = entity.role,
                unreadCount = entity.unreadCount
            )
        }.filter { it.userID != currentUserId }

        allUsers.addAll(userResponses)

        startListeningUnreadCounts()
        filterUsers()
        updateStatistics()
    }

    /**
     * Starts Firebase realtime listeners for unread message counts on conversations with each user
     */
    private fun startListeningUnreadCounts() {
        Log.d(TAG, "Starting Firebase listeners for unread counts")

        // Remove existing listeners to avoid duplicates
        listenersMap.forEach { (convId, listener) ->
            db.child("conversations").child(convId).child("meta").removeEventListener(listener)
            Log.d(TAG, "Removed listener for conversation $convId")
        }
        listenersMap.clear()

        allUsers.forEach { other ->
            val convId = getConversationId(currentUserId, other.userID)
            val metaRef = db.child("conversations").child(convId).child("meta")

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val unreadKey = "unread_${currentUserId}"
                    val unread = snapshot.child(unreadKey).getValue(Int::class.java) ?: 0

                    Log.d(TAG, "Unread messages for conversation $convId: $unread")

                    // Update unread count in allUsers list and local DB
                    val allIndex = allUsers.indexOfFirst { it.userID == other.userID }
                    if (allIndex >= 0) {
                        val updatedUser = allUsers[allIndex].copy(unreadCount = unread)
                        allUsers[allIndex] = updatedUser
                        dbHelper.updateUnreadCount(updatedUser.userID, unread)
                    }

                    // Update displayedUsers list and notify adapter if visible
                    val dispIndex = displayedUsers.indexOfFirst { it.userID == other.userID }
                    if (dispIndex >= 0) {
                        displayedUsers[dispIndex] = displayedUsers[dispIndex].copy(unreadCount = unread)
                        adapter.notifyItemChanged(dispIndex)
                    }

                    updateStatistics()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Firebase listener cancelled for conversation $convId: ${error.message}")
                }
            }

            metaRef.addValueEventListener(listener)
            listenersMap[convId] = listener
            Log.d(TAG, "Added Firebase listener for conversation $convId")
        }
    }

    /**
     * Filters users by current filter (role) and search query, updates displayed list and UI
     */
    private fun filterUsers(query: String? = null) {
        Log.d(TAG, "Filtering users with query='$query' and filter='$currentFilter'")
        displayedUsers.clear()

        // Filter users by selected role
        val filteredByRole = when (currentFilter) {
            "students" -> allUsers.filter { it.role.equals("Student", ignoreCase = true) }
            "lecturers" -> allUsers.filter { it.role.equals("Lecturer", ignoreCase = true) }
            else -> allUsers // Show all
        }

        // Further filter by search query if provided
        val finalFiltered = query?.let {
            filteredByRole.filter { user ->
                user.firstName.contains(it, ignoreCase = true) ||
                        user.lastName.contains(it, ignoreCase = true) ||
                        user.username.contains(it, ignoreCase = true)
            }
        } ?: filteredByRole

        displayedUsers.addAll(finalFiltered)
        adapter.notifyDataSetChanged()

        updateUserCount()
        updateEmptyState()
        updateStatistics()
    }

    /**
     * Updates UI to reflect which filter button is active
     */
    private fun updateCategoryUI() {
        Log.d(TAG, "Updating category button UI for filter: $currentFilter")

        // Reset all buttons to default colors
        btnShowAll.setBackgroundColor(getColor(android.R.color.darker_gray))
        btnShowAll.setTextColor(getColor(android.R.color.black))
        btnShowStudents.setBackgroundColor(getColor(android.R.color.darker_gray))
        btnShowStudents.setTextColor(getColor(android.R.color.black))
        btnShowLecturers.setBackgroundColor(getColor(android.R.color.darker_gray))
        btnShowLecturers.setTextColor(getColor(android.R.color.black))

        // Set active button colors
        when (currentFilter) {
            "all" -> {
                btnShowAll.setBackgroundColor(getColor(R.color.maroon))
                btnShowAll.setTextColor(getColor(android.R.color.white))
            }
            "students" -> {
                btnShowStudents.setBackgroundColor(getColor(R.color.maroon))
                btnShowStudents.setTextColor(getColor(android.R.color.white))
            }
            "lecturers" -> {
                btnShowLecturers.setBackgroundColor(getColor(R.color.maroon))
                btnShowLecturers.setTextColor(getColor(android.R.color.white))
            }
        }
    }

    /**
     * Updates the user count text based on currently displayed users
     */
    private fun updateUserCount() {
        val countText = "${displayedUsers.size} ${if (displayedUsers.size == 1) "user" else "users"}"
        tvUserCount.text = countText
        Log.d(TAG, "Updated user count: $countText")
    }

    /**
     * Shows or hides empty state text based on whether displayedUsers list is empty
     */
    private fun updateEmptyState() {
        if (displayedUsers.isEmpty()) {
            Log.d(TAG, "No users to display - showing empty state")
            tvEmptyState.visibility = View.VISIBLE
            recyclerUsers.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerUsers.visibility = View.VISIBLE
        }
    }

    /**
     * Updates statistics shown in the UI including total contacts, online users, total unread messages
     */
    private fun updateStatistics() {
        Log.d(TAG, "Updating statistics")

        // Total contacts count
        tvTotalContacts.text = allUsers.size.toString()

        // Placeholder for online users count (should implement actual online status)
        val onlineUsers = allUsers.size / 3
        tvOnlineNow.text = onlineUsers.toString()

        // Sum unread messages from all users
        val totalUnread = allUsers.sumOf { it.unreadCount ?: 0 }
        tvTotalUnread.text = totalUnread.toString()

        // Update unread badge for current user UI
        tvUnreadCount.text = totalUnread.toString()
        tvUnreadCount.visibility = if (totalUnread > 0) View.VISIBLE else View.INVISIBLE
    }

    /**
     * Generates a conversation ID string based on user IDs in consistent order
     */
    private fun getConversationId(userId1: Int, userId2: Int): String =
        "${minOf(userId1, userId2)}_${maxOf(userId1, userId2)}"

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Removing Firebase listeners")
        listenersMap.forEach { (convId, listener) ->
            db.child("conversations").child(convId).child("meta").removeEventListener(listener)
            Log.d(TAG, "Removed listener for conversation $convId")
        }
        listenersMap.clear()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refreshing statistics if users loaded")
        if (allUsers.isNotEmpty()) {
            updateStatistics()
        }
    }

    /**
     * Checks network connectivity state to determine online status
     */
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val net = cm.activeNetworkInfo
        val online = net?.isConnectedOrConnecting == true
        Log.d(TAG, "Network connectivity check: $online")
        return online
    }
}
