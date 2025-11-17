package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GamificationActivity : AppCompatActivity() {

    private lateinit var tvPoints: TextView
    private lateinit var tvStreak: TextView
    private lateinit var progressPoints: ProgressBar
    private lateinit var layoutBadges: LinearLayout
    private lateinit var rvLeaderboard: androidx.recyclerview.widget.RecyclerView
    private lateinit var btnPlayGame: Button
    private lateinit var btnRedeem: Button

    private val api = ApiClient.gamificationApi

    companion object {
        private const val TAG = "GamificationActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamification)

        Log.d(TAG, "onCreate: Initializing UI components")

        // Initialize views from layout (Patel, 2025)
        tvPoints = findViewById(R.id.tvPoints)
        tvStreak = findViewById(R.id.tvStreak)
        progressPoints = findViewById(R.id.progressPoints)
        layoutBadges = findViewById(R.id.layoutBadges)
        rvLeaderboard = findViewById(R.id.rvLeaderboard)
        btnPlayGame = findViewById(R.id.btnPlayGame)
        btnRedeem = findViewById(R.id.btnRedeem)

        // Set up leaderboard RecyclerView with linear layout manager
        rvLeaderboard.layoutManager = LinearLayoutManager(this)

        // Set up play game button to launch GameActivity (Patel, 2025)
        btnPlayGame.setOnClickListener {
            Log.d(TAG, "Play Game button clicked")
            startActivity(Intent(this, GameActivity::class.java))
        }

        // Set up redeem button to open rewards dialog
        btnRedeem.setOnClickListener {
            Log.d(TAG, "Redeem button clicked")
            showRedeemDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refreshing user stats and leaderboard")
        loadUserStats()
        loadLeaderboard()
    }

    /**
     * Loads user stats such as points, streak, and badges from API
     * Updates UI accordingly (Patel, 2025)
     */
    private fun loadUserStats() {
        val userId = ApiClient.currentUserId
        if (userId == 0) {
            Log.w(TAG, "loadUserStats: User not logged in")
            return
        }

        api.getUserStats(userId).enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val s = response.body()!!
                    Log.d(TAG, "loadUserStats: User stats loaded - points: ${s.points}, streak: ${s.streak}")

                    // Update points and progress bar
                    tvPoints.text = "Points: ${s.points}"
                    progressPoints.progress = (s.points % 100)

                    // Update streak
                    tvStreak.text = "Streak: ${s.streak}"

                    // Update badges dynamically (Patel, 2025)
                    layoutBadges.removeAllViews()
                    s.badges.forEach { badge ->
                        val iv = ImageView(this@GamificationActivity)
                        iv.layoutParams = LinearLayout.LayoutParams(140, 140).apply { setMargins(8, 8, 8, 8) }
                        iv.setImageResource(R.drawable.ic_home)  // Placeholder icon for badge
                        iv.setOnClickListener {
                            Toast.makeText(this@GamificationActivity, badge.name, Toast.LENGTH_SHORT).show()
                        }
                        layoutBadges.addView(iv)
                    }
                } else {
                    Log.w(TAG, "loadUserStats: API response unsuccessful or empty body")
                    Toast.makeText(this@GamificationActivity, "Failed to load user stats.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                Log.e(TAG, "loadUserStats: API call failed", t)
                Toast.makeText(this@GamificationActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Loads leaderboard entries from API and populates the RecyclerView
     */
    private fun loadLeaderboard() {
        api.getLeaderboard().enqueue(object : Callback<List<LeaderboardEntryResponse>> {
            override fun onResponse(
                call: Call<List<LeaderboardEntryResponse>>,
                response: Response<List<LeaderboardEntryResponse>>
            ) {
                if (response.isSuccessful) {
                    val leaderboard = response.body()
                    Log.d(TAG, "loadLeaderboard: Loaded leaderboard with ${leaderboard?.size ?: 0} entries")
                    if (!leaderboard.isNullOrEmpty()) {
                        rvLeaderboard.adapter = LeaderboardAdapter(leaderboard)
                    }
                } else {
                    Log.w(TAG, "loadLeaderboard: API response unsuccessful")
                    Toast.makeText(this@GamificationActivity, "Failed to load leaderboard.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<LeaderboardEntryResponse>>, t: Throwable) {
                Log.e(TAG, "loadLeaderboard: API call failed", t)
                Toast.makeText(this@GamificationActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ------------------ Redeem Feature ------------------

    /**
     * Shows a dialog listing available rewards to redeem
     * Includes enhanced error logging for troubleshooting API failures
     */
    private fun showRedeemDialog() {
        val userId = ApiClient.currentUserId
        if (userId == 0) {
            Log.w(TAG, "showRedeemDialog: User not logged in")
            return
        }

        api.getAvailableRewards().enqueue(object : Callback<List<RewardResponse>> {
            override fun onResponse(call: Call<List<RewardResponse>>, response: Response<List<RewardResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val rewards = response.body()!!
                    Log.d(TAG, "showRedeemDialog: Retrieved ${rewards.size} rewards")

                    if (rewards.isEmpty()) {
                        Toast.makeText(this@GamificationActivity, "No rewards available.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val rewardNames = rewards.map { "${it.title} - ${it.costPoints} pts" }.toTypedArray()

                    AlertDialog.Builder(this@GamificationActivity)
                        .setTitle("Select Reward")
                        .setItems(rewardNames) { dialog, which ->
                            Log.d(TAG, "showRedeemDialog: User selected reward ${rewards[which].title}")
                            redeemReward(rewards[which].rewardId)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            Log.d(TAG, "showRedeemDialog: User cancelled reward selection")
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    // Enhanced error logging for debugging
                    val errorMsg = StringBuilder()
                    errorMsg.append("Failed to load rewards.\n")
                    errorMsg.append("HTTP Code: ${response.code()}\n")
                    errorMsg.append("Message: ${response.message()}\n")
                    response.errorBody()?.let { body ->
                        errorMsg.append("Error Body: ${body.string()}")
                    }
                    Log.e(TAG, errorMsg.toString())
                    Toast.makeText(this@GamificationActivity, errorMsg.toString(), Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<RewardResponse>>, t: Throwable) {
                val errorMsg = "Network error: ${t.localizedMessage}"
                Log.e(TAG, errorMsg, t)
                Toast.makeText(this@GamificationActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Sends redeem request to API and updates user stats on success
     */
    private fun redeemReward(rewardId: Int) {
        val userId = ApiClient.currentUserId
        Log.d(TAG, "redeemReward: Attempting to redeem reward $rewardId for user $userId")

        api.redeemReward(userId, rewardId).enqueue(object : Callback<RedeemResponse> {
            override fun onResponse(call: Call<RedeemResponse>, response: Response<RedeemResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.success) {
                        Log.d(TAG, "redeemReward: Redeem successful - ${result.message}")
                        Toast.makeText(this@GamificationActivity, "Redeemed: ${result.message}", Toast.LENGTH_LONG).show()
                        loadUserStats()  // Refresh stats after redeeming
                    } else {
                        Log.w(TAG, "redeemReward: Redeem failed - ${result.message}")
                        Toast.makeText(this@GamificationActivity, "Redeem failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.w(TAG, "redeemReward: Redeem failed with HTTP code ${response.code()}")
                    Toast.makeText(this@GamificationActivity, "Redeem failed: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RedeemResponse>, t: Throwable) {
                Log.e(TAG, "redeemReward: Redeem API call failed", t)
                Toast.makeText(this@GamificationActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */