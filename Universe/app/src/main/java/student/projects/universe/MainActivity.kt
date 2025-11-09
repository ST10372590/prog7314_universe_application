package student.projects.universe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "MainActivity started")

        // Bind buttons from layout // (Patel, 2025)
        val btnGoRegister: Button = findViewById(R.id.btnGoRegister)
        val btnGoLogin: Button = findViewById(R.id.btnGoLogin)
        Log.d("MainActivity", "Buttons initialized: btnGoRegister & btnGoLogin")

        // Navigate to RegisterActivity when register button is clicked // (Patel, 2025)
        btnGoRegister.setOnClickListener {
            Log.d("MainActivity", "Register button clicked")
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Navigate to LoginActivity when login button is clicked // (Patel, 2025)
        btnGoLogin.setOnClickListener {
            Log.d("MainActivity", "Login button clicked")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */