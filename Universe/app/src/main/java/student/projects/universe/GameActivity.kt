package student.projects.universe

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var spinnerGames: Spinner
    private lateinit var btnPlay: Button
    private lateinit var tvResult: TextView
    private lateinit var tvGameTitle: TextView
    private lateinit var tvGameInfo: TextView
    private lateinit var ivGameImage: ImageView
    private val api = ApiClient.gamificationApi
    private var selectedGame: String = "Spin for Points"

    companion object {
        private const val TAG = "GameActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        Log.d(TAG, "onCreate: Initializing UI components")

        spinnerGames = findViewById(R.id.spinnerGames)
        btnPlay = findViewById(R.id.btnPlay)
        tvResult = findViewById(R.id.tvGameResult)
        tvGameTitle = findViewById(R.id.tvGameTitle)
        tvGameInfo = findViewById(R.id.tvGameInfo)
        ivGameImage = findViewById(R.id.ivGameImage)

        // List of available games (Patel, 2025)
        val games = listOf("Spin for Points", "Quick Math Challenge", "Guess the Number")

        // Setup Spinner adapter for game selection (Patel, 2025)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, games)
        spinnerGames.adapter = adapter

        spinnerGames.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long
            ) {
                selectedGame = games[position]
                Log.d(TAG, "onItemSelected: Selected game = $selectedGame")
                updateGameInfo(selectedGame)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        btnPlay.setOnClickListener {
            Log.d(TAG, "Play button clicked for game: $selectedGame")
            when (selectedGame) {
                "Spin for Points" -> playSpinGame()
                "Quick Math Challenge" -> playMathGame()
                "Guess the Number" -> playGuessGame()
                else -> Log.w(TAG, "Unknown game selected")
            }
        }
    }

    /**
     * Update game description and image based on selection
     */
    private fun updateGameInfo(game: String) {
        when (game) {
            "Spin for Points" -> {
                tvGameTitle.text = "Spin for Points"
                tvGameInfo.text = "Press Play to spin and earn random points!"
                ivGameImage.setImageResource(R.drawable.ic_spin_wheel)
            }
            "Quick Math Challenge" -> {
                tvGameTitle.text = "Quick Math Challenge"
                tvGameInfo.text = "Answer the math question correctly to earn points!"
                ivGameImage.setImageResource(R.drawable.ic_math)
            }
            "Guess the Number" -> {
                tvGameTitle.text = "Guess the Number"
                tvGameInfo.text = "Guess the correct number between 1 and 5!"
                ivGameImage.setImageResource(R.drawable.ic_guess)
            }
            else -> {
                tvGameTitle.text = "Unknown Game"
                tvGameInfo.text = ""
                ivGameImage.setImageResource(0)
            }
        }
        tvResult.text = "🎮 Ready to play $game!"
        Log.d(TAG, "updateGameInfo: Updated UI for game: $game")
    }

    // --- Game 1: Spin Game ---
    private fun playSpinGame() {
        val userId = ApiClient.currentUserId
        if (userId == 0) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "playSpinGame: User not logged in")
            return
        }

        btnPlay.isEnabled = false
        tvResult.text = "🎰 Spinning the wheel..."

        // Load spin animation (Patel, 2025)
        val spinAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.spin_animation)
        ivGameImage.startAnimation(spinAnimation)

        // Delay to simulate spinning (2.5 seconds)
        ivGameImage.postDelayed({
            val randomPoints = listOf(5, 10, 15, 20, 50, 100).random()
            val message = "🎉 You won $randomPoints points!"
            tvResult.text = message
            Log.d(TAG, "playSpinGame: User won $randomPoints points")

            addPointsToServer(randomPoints)
            btnPlay.isEnabled = true
        }, 2500)
    }

    // --- Game 2: Interactive Math Challenge ---
    private fun playMathGame() {
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        val correctAnswer = a + b

        val input = EditText(this)
        input.hint = "Enter your answer"

        AlertDialog.Builder(this)
            .setTitle("Quick Math Challenge")
            .setMessage("What is $a + $b ?")
            .setView(input)
            .setPositiveButton("Submit") { dialog, _ ->
                val answerText = input.text.toString()
                if (answerText.isEmpty()) {
                    Toast.makeText(this, "Please enter an answer", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "playMathGame: Empty answer submitted")
                    return@setPositiveButton
                }

                val userAnswer = answerText.toIntOrNull()
                if (userAnswer == null) {
                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "playMathGame: Invalid number input")
                    return@setPositiveButton
                }

                if (userAnswer == correctAnswer) {
                    val points = 20
                    tvResult.text = "✅ Correct! You earned $points points 🎉"
                    Log.d(TAG, "playMathGame: User answered correctly and earned $points points")
                    addPointsToServer(points)
                } else {
                    tvResult.text = "❌ Oops! The correct answer was $correctAnswer."
                    Log.d(TAG, "playMathGame: User answered incorrectly (was $userAnswer)")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG, "playMathGame: User cancelled the dialog")
                dialog.dismiss()
            }
            .show()
    }

    // --- Game 3: Interactive Guess Game ---
    private fun playGuessGame() {
        val correctNumber = Random.nextInt(1, 6)
        val input = EditText(this)
        input.hint = "Guess a number (1-5)"

        AlertDialog.Builder(this)
            .setTitle("Guess the Number")
            .setMessage("Enter your guess (1 to 5):")
            .setView(input)
            .setPositiveButton("Submit") { dialog, _ ->
                val guessText = input.text.toString()
                val userGuess = guessText.toIntOrNull()

                if (userGuess == null || userGuess !in 1..5) {
                    Toast.makeText(this, "Please enter a valid number between 1 and 5", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "playGuessGame: Invalid guess input")
                    return@setPositiveButton
                }

                if (userGuess == correctNumber) {
                    val points = 15
                    tvResult.text = "🎯 Correct! The number was $correctNumber. +$points points!"
                    Log.d(TAG, "playGuessGame: User guessed correctly and earned $points points")
                    addPointsToServer(points)
                } else {
                    tvResult.text = "😢 Wrong! You guessed $userGuess. The correct number was $correctNumber."
                    Log.d(TAG, "playGuessGame: User guessed incorrectly ($userGuess)")
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d(TAG, "playGuessGame: User cancelled the dialog")
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Send earned points to the server API to update user stats
     * Handles response and failure with toast notifications (Patel, 2025)
     */
    private fun addPointsToServer(points: Int) {
        val userId = ApiClient.currentUserId
        if (userId == 0) {
            Log.w(TAG, "addPointsToServer: User not logged in, skipping API call")
            return
        }

        Log.d(TAG, "addPointsToServer: Adding $points points for user $userId")
        api.addPoints(userId, points).enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "addPointsToServer: Points added successfully")
                    Toast.makeText(this@GameActivity, "Points added successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "addPointsToServer: API call unsuccessful - code ${response.code()}")
                    Toast.makeText(this@GameActivity, "Failed to add points.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                Log.e(TAG, "addPointsToServer: Failed to add points", t)
                Toast.makeText(this@GameActivity, "Failed to sync points: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
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