package student.projects.universe

import android.app.AlertDialog
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        spinnerGames = findViewById(R.id.spinnerGames)
        btnPlay = findViewById(R.id.btnPlay)
        tvResult = findViewById(R.id.tvGameResult)
        tvGameTitle = findViewById(R.id.tvGameTitle)
        tvGameInfo = findViewById(R.id.tvGameInfo)
        ivGameImage = findViewById(R.id.ivGameImage)

        val games = listOf("Spin for Points", "Quick Math Challenge", "Guess the Number")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, games)
        spinnerGames.adapter = adapter

        spinnerGames.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedGame = games[position]
                updateGameInfo(selectedGame)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnPlay.setOnClickListener {
            when (selectedGame) {
                "Spin for Points" -> playSpinGame()
                "Quick Math Challenge" -> playMathGame()
                "Guess the Number" -> playGuessGame()
            }
        }
    }

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
        }
        tvResult.text = "🎮 Ready to play $game!"
    }

    // --- Game 1: Spin Game ---
    private fun playSpinGame() {
        val userId = ApiClient.currentUserId
        if (userId == 0) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button during spin
        btnPlay.isEnabled = false
        tvResult.text = "🎰 Spinning the wheel..."

        // Load animation
        val spinAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.spin_animation)
        ivGameImage.startAnimation(spinAnimation)

        // Run result after the spin completes (2.5s delay)
        ivGameImage.postDelayed({
            val randomPoints = listOf(5, 10, 15, 20, 50, 100).random()
            val message = "🎉 You won $randomPoints points!"
            tvResult.text = message

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
                    return@setPositiveButton
                }

                val userAnswer = answerText.toIntOrNull()
                if (userAnswer == null) {
                    Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (userAnswer == correctAnswer) {
                    val points = 20
                    tvResult.text = "✅ Correct! You earned $points points 🎉"
                    addPointsToServer(points)
                } else {
                    tvResult.text = "❌ Oops! The correct answer was $correctAnswer."
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
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
                    return@setPositiveButton
                }

                if (userGuess == correctNumber) {
                    val points = 15
                    tvResult.text = "🎯 Correct! The number was $correctNumber. +$points points!"
                    addPointsToServer(points)
                } else {
                    tvResult.text = "😢 Wrong! You guessed $userGuess. The correct number was $correctNumber."
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // --- API Call to Add Points ---
    private fun addPointsToServer(points: Int) {
        val userId = ApiClient.currentUserId
        if (userId == 0) return

        api.addPoints(userId, points).enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@GameActivity, "Points added successfully!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
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