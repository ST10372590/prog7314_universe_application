package student.projects.universe

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Activity for registering new users (Student or Lecturer) with validation and API integration // (Patel, 2025)
 */
class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        Log.d("RegisterActivity", "RegisterActivity started") // (Patel, 2025)

        // Bind views
        val firstName = findViewById<EditText>(R.id.etFirstName)
        val lastName = findViewById<EditText>(R.id.etLastName)
        val email = findViewById<EditText>(R.id.etEmail)
        val phone = findViewById<EditText>(R.id.etPhone)
        val username = findViewById<EditText>(R.id.etUsername)
        val password = findViewById<EditText>(R.id.etPassword)
        val role = findViewById<Spinner>(R.id.spinnerRole)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        Log.d("RegisterActivity", "Views initialized")

        // Spinner setup // (Patel, 2025)
        val roles = arrayOf("Student", "Lecturer")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        role.adapter = adapter
        Log.d("RegisterActivity", "Role spinner initialized with roles: ${roles.joinToString()}")

        // --- REAL-TIME VALIDATION AND FORMATTING ---

        // 1️⃣ PHONE: auto-format +27 XX XXX XXXX // (Patel, 2025)
        phone.addTextChangedListener(object : TextWatcher {
            var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return

                var digits = s.toString().replace("[^\\d+]".toRegex(), "") // (Patel, 2025)
                digits = if (digits.startsWith("+27")) digits else "+27" + digits.removePrefix("27").removePrefix("+")
                if (digits.length > 12) digits = digits.substring(0, 12)

                val formatted = StringBuilder()
                for (i in digits.indices) {
                    formatted.append(digits[i])
                    if (i == 2 || i == 4 || i == 7) formatted.append(" ") // (Patel, 2025)
                }

                isUpdating = true
                phone.setText(formatted.toString())
                phone.setSelection(formatted.length)
                isUpdating = false
                Log.d("RegisterActivity", "Phone formatted: ${formatted}") // (Patel, 2025)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 2️⃣ EMAIL: must end with @gmail.com // (Patel, 2025)
        email.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val emailText = s.toString().trim()
                if (!emailText.endsWith("@gmail.com")) {
                    email.setBackgroundColor(Color.parseColor("#FFCDD2")) // Light red // (Patel, 2025)
                    Log.d("RegisterActivity", "Invalid Gmail: $emailText")
                } else {
                    email.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 3️⃣ PASSWORD: must have uppercase, lowercase, number, special character // (Patel, 2025)
        password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s.toString()
                val pattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{6,}$")
                if (!pattern.matches(pass)) {
                    password.setBackgroundColor(Color.parseColor("#FFCDD2")) // Light red // (Patel, 2025)
                    Log.d("RegisterActivity", "Weak password entered")
                } else {
                    password.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- REGISTER BUTTON LOGIC --- // (Patel, 2025)
        btnRegister.setOnClickListener {
            val first = firstName.text.toString().trim()
            val last = lastName.text.toString().trim()
            val emailText = email.text.toString().trim()
            val phoneText = phone.text.toString().trim()
            val user = username.text.toString().trim()
            val pass = password.text.toString().trim()
            val selectedRole = role.selectedItem.toString()
            Log.d("RegisterActivity", "Register button clicked with role: $selectedRole")

            // Reset colors // (Patel, 2025)
            val fields = listOf(firstName, lastName, email, phone, username, password)
            fields.forEach { it.setBackgroundColor(Color.TRANSPARENT) }

            // Validate required fields // (Patel, 2025)
            if (first.isEmpty() || last.isEmpty() || emailText.isEmpty() ||
                phoneText.isEmpty() || user.isEmpty() || pass.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                fields.filter { it.text.isEmpty() }.forEach {
                    it.setBackgroundColor(Color.parseColor("#FFCDD2"))
                }
                Log.w("RegisterActivity", "Validation failed: missing fields")
                return@setOnClickListener
            }

            // Validate email format // (Patel, 2025)
            if (!emailText.endsWith("@gmail.com") || !Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Email must be a valid Gmail address", Toast.LENGTH_SHORT).show()
                email.setBackgroundColor(Color.parseColor("#FFCDD2"))
                Log.w("RegisterActivity", "Validation failed: invalid email - $emailText")
                return@setOnClickListener
            }

            // Validate phone // (Patel, 2025)
            val phoneRegex = Regex("^\\+27 \\d{2} \\d{3} \\d{4}\$")
            if (!phoneRegex.matches(phoneText)) {
                Toast.makeText(this, "Phone must be in format +27 XX XXX XXXX", Toast.LENGTH_SHORT).show()
                phone.setBackgroundColor(Color.parseColor("#FFCDD2"))
                Log.w("RegisterActivity", "Validation failed: invalid phone - $phoneText")
                return@setOnClickListener
            }

            // Validate password strength // (Patel, 2025)
            val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!]).{6,}$")
            if (!passwordRegex.matches(pass)) {
                Toast.makeText(
                    this,
                    "Password must contain at least:\n- 1 uppercase\n- 1 lowercase\n- 1 number\n- 1 special character",
                    Toast.LENGTH_LONG
                ).show()
                password.setBackgroundColor(Color.parseColor("#FFCDD2"))
                Log.w("RegisterActivity", "Validation failed: weak password")
                return@setOnClickListener
            }

            // ✅ Create Register Request // (Patel, 2025)
            val request = RegisterRequest(first, last, emailText, phoneText, selectedRole, user, pass)
            Log.d("RegisterActivity", "RegisterRequest created for user: $user")

            // --- API CALL --- // (Patel, 2025)
            ApiClient.authApi.register(request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Registered successfully!", Toast.LENGTH_SHORT).show()
                        Log.d("RegisterActivity", "User registered successfully: $user")
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                        Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e("RegisterActivity", "Registration failed: $errorMsg")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) { // (Patel, 2025)
                    Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "API call failed", t)
                }
            })
        }
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */