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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gamification)

        tvPoints = findViewById(R.id.tvPoints)
        tvStreak = findViewById(R.id.tvStreak)
        progressPoints = findViewById(R.id.progressPoints)
        layoutBadges = findViewById(R.id.layoutBadges)
        rvLeaderboard = findViewById(R.id.rvLeaderboard)
        btnPlayGame = findViewById(R.id.btnPlayGame)
        btnRedeem = findViewById(R.id.btnRedeem)

        rvLeaderboard.layoutManager = LinearLayoutManager(this)

        btnPlayGame.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        btnRedeem.setOnClickListener {
            showRedeemDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserStats()
        loadLeaderboard()
    }

    private fun loadUserStats() {
        val userId = ApiClient.currentUserId
        if (userId == 0) return

        api.getUserStats(userId).enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val s = response.body()!!
                    tvPoints.text = "Points: ${s.points}"
                    progressPoints.progress = (s.points % 100)
                    tvStreak.text = "Streak: ${s.streak}"

                    layoutBadges.removeAllViews()
                    s.badges.forEach { badge ->
                        val iv = ImageView(this@GamificationActivity)
                        iv.layoutParams = LinearLayout.LayoutParams(140, 140).apply { setMargins(8,8,8,8) }
                        iv.setImageResource(R.drawable.ic_home)
                        iv.setOnClickListener { Toast.makeText(this@GamificationActivity, badge.name, Toast.LENGTH_SHORT).show() }
                        layoutBadges.addView(iv)
                    }
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                Toast.makeText(this@GamificationActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun loadLeaderboard() {
        api.getLeaderboard().enqueue(object : Callback<List<LeaderboardEntryResponse>> {
            override fun onResponse(call: Call<List<LeaderboardEntryResponse>>, response: Response<List<LeaderboardEntryResponse>>) {
                if (response.isSuccessful) {
                    val leaderboard = response.body()
                    if (!leaderboard.isNullOrEmpty()) rvLeaderboard.adapter = LeaderboardAdapter(leaderboard)
                }
            }

            override fun onFailure(call: Call<List<LeaderboardEntryResponse>>, t: Throwable) {
                Toast.makeText(this@GamificationActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ------------------ Redeem Feature ------------------
    private fun showRedeemDialog() {
        val userId = ApiClient.currentUserId
        if (userId == 0) return

        api.getAvailableRewards().enqueue(object : Callback<List<RewardResponse>> {
            override fun onResponse(call: Call<List<RewardResponse>>, response: Response<List<RewardResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val rewards = response.body()!!
                    if (rewards.isEmpty()) {
                        Toast.makeText(this@GamificationActivity, "No rewards available.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val rewardNames = rewards.map { "${it.title} - ${it.costPoints} pts" }.toTypedArray()

                    AlertDialog.Builder(this@GamificationActivity)
                        .setTitle("Select Reward")
                        .setItems(rewardNames) { dialog, which ->
                            redeemReward(rewards[which].rewardId)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .show()

                } else {
                    // Enhanced error logging
                    val errorMsg = StringBuilder()
                    errorMsg.append("Failed to load rewards.\n")
                    errorMsg.append("HTTP Code: ${response.code()}\n")
                    errorMsg.append("Message: ${response.message()}\n")
                    response.errorBody()?.let { body ->
                        errorMsg.append("Error Body: ${body.string()}")
                    }
                    Log.e("Gamification", errorMsg.toString())
                    Toast.makeText(this@GamificationActivity, errorMsg.toString(), Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<RewardResponse>>, t: Throwable) {
                val errorMsg = "Network error: ${t.localizedMessage}"
                Log.e("Gamification", errorMsg, t)
                Toast.makeText(this@GamificationActivity, errorMsg, Toast.LENGTH_LONG).show()
            }
        })
    }

    /*
    private fun showRedeemDialog() {
        val userId = ApiClient.currentUserId
        if (userId == 0) return

        api.getAvailableRewards().enqueue(object : Callback<List<RewardResponse>> {
            override fun onResponse(call: Call<List<RewardResponse>>, response: Response<List<RewardResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val rewards = response.body()!!
                    if (rewards.isEmpty()) {
                        Toast.makeText(this@GamificationActivity, "No rewards available.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val rewardNames = rewards.map { "${it.title} - ${it.costPoints} pts" }.toTypedArray()

                    AlertDialog.Builder(this@GamificationActivity)
                        .setTitle("Select Reward")
                        .setItems(rewardNames) { dialog, which ->
                            redeemReward(rewards[which].rewardId)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .show()

                } else {
                    Toast.makeText(this@GamificationActivity, "Failed to load rewards", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<RewardResponse>>, t: Throwable) {
                Toast.makeText(this@GamificationActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
*/
    private fun redeemReward(rewardId: Int) {
        val userId = ApiClient.currentUserId
        api.redeemReward(userId, rewardId).enqueue(object : Callback<RedeemResponse> {
            override fun onResponse(call: Call<RedeemResponse>, response: Response<RedeemResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.success) {
                        Toast.makeText(this@GamificationActivity, "Redeemed: ${result.message}", Toast.LENGTH_LONG).show()
                        loadUserStats()
                    } else {
                        Toast.makeText(this@GamificationActivity, "Redeem failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@GamificationActivity, "Redeem failed: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RedeemResponse>, t: Throwable) {
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