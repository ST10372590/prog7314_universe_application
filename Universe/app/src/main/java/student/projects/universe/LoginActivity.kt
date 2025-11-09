package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val WEB_CLIENT_ID = "64158382237-oe44dvlh679n673iqtanlf9ifrkidvip.apps.googleusercontent.com"

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                loginWithGoogle(idToken)
            } else {
                Toast.makeText(this, "Google ID token is null", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("LoginActivity", "Google Sign-In failed", e)
            Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d("LoginActivity", "LoginActivity started")

        val username = findViewById<EditText>(R.id.etUsername)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogleLogin)
        val btnBiometric = findViewById<Button>(R.id.btnBiometricLogin)
        val btnGoRegister = findViewById<Button>(R.id.btnGoRegister)

        // ✅ If a saved token exists, offer biometric login automatically
        val savedToken = SecurePrefs.getToken(this)
        if (savedToken != null) {
            showBiometricPrompt(savedToken)
        }

        // Username/password login
        btnLogin.setOnClickListener {
            val request = LoginRequest(username.text.toString(), password.text.toString())
            ApiClient.authApi.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        handleLoginSuccess(response.body()!!.token)
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Google login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        btnGoogle.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        // ✅ Manual biometric login button
        btnBiometric.setOnClickListener {
            val token = SecurePrefs.getToken(this)
            if (token != null) {
                showBiometricPrompt(token)
            } else {
                Toast.makeText(this, "No saved session found. Please login once first.", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to RegisterActivity
        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginWithGoogle(idToken: String) {
        val googleLoginRequest = GoogleLoginRequest(IdToken = idToken)
        ApiClient.authApi.loginWithGoogle(googleLoginRequest)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        handleLoginSuccess(response.body()!!.token)
                    } else {
                        Toast.makeText(this@LoginActivity, "Google login failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun handleLoginSuccess(token: String) {
        SecurePrefs.saveToken(this, token)
        ApiClient.setToken(token)
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        ApiClient.userApi.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    ApiClient.currentUserId = user.userID

                    when (user.role.lowercase()) {
                        "student" -> startActivity(Intent(this@LoginActivity, StudentDashboardActivity::class.java))
                        "lecturer" -> startActivity(Intent(this@LoginActivity, LecturerDashboardActivity::class.java))
                        else -> Toast.makeText(this@LoginActivity, "Unknown role", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ✅ Biometric Authentication Setup
    private fun showBiometricPrompt(savedToken: String) {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric authentication not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Authenticate with fingerprint or face")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    ApiClient.setToken(savedToken)
                    fetchCurrentUser()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}



/*
package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    // Google Sign-In client // (Patel, 2025)
    private lateinit var googleSignInClient: GoogleSignInClient
    private val WEB_CLIENT_ID = "64158382237-oe44dvlh679n673iqtanlf9ifrkidvip.apps.googleusercontent.com"

    // Modern Activity Result Launcher for Google Sign-In // (Patel, 2025)
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                Log.d("LoginActivity", "Google ID token received: $idToken")
                loginWithGoogle(idToken)
            } else {
                Log.w("LoginActivity", "Google ID token is null")
                Toast.makeText(this, "Google ID token is null", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("LoginActivity", "Google Sign-In failed", e) // (Patel, 2025)
            Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { // (Patel, 2025)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Log.d("LoginActivity", "LoginActivity started")

        // 🔐 Check if a saved token exists and prompt biometric login
        val savedToken = SecurePrefs.getToken(this)
        if (savedToken != null) {
            Log.d("LoginActivity", "Saved JWT token found, prompting biometric authentication")
            showBiometricPrompt(savedToken)
        }

        // Bind views // (Patel, 2025)
        val username = findViewById<EditText>(R.id.etUsername)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogleLogin)
        val btnGoRegister = findViewById<Button>(R.id.btnGoRegister)

        // Normal username/password login // (Patel, 2025)
        btnLogin.setOnClickListener {
            val request = LoginRequest(username.text.toString(), password.text.toString())
            Log.d("LoginActivity", "Attempting login with username: ${username.text}")
            ApiClient.authApi.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d("LoginActivity", "Login successful with username/password")
                        handleLoginSuccess(response.body()!!.token)
                    } else {
                        Log.w("LoginActivity", "Login failed with username/password. HTTP ${response.code()}")
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("LoginActivity", "Login request failed", t)
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Google SSO setup with ID token // (Patel, 2025)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID) // Important for backend authentication // (Patel, 2025)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d("LoginActivity", "GoogleSignInClient initialized")

        btnGoogle.setOnClickListener {
            Log.d("LoginActivity", "Google Sign-In button clicked")

            // Force Google account picker
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d("LoginActivity", "Signed out from previous Google session")
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
        // Navigate to Register screen // (Patel, 2025)
        btnGoRegister.setOnClickListener {
            Log.d("LoginActivity", "Navigating to RegisterActivity")
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Handles login using Google ID token // (Patel, 2025)
     */
    private fun loginWithGoogle(idToken: String) {
        Log.d("LoginActivity", "Logging in with Google ID token...")
        val googleLoginRequest = GoogleLoginRequest(IdToken = idToken)
        ApiClient.authApi.loginWithGoogle(googleLoginRequest)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d("LoginActivity", "Google login successful")
                        handleLoginSuccess(response.body()!!.token)
                    } else {
                        // Log detailed error information // (Patel, 2025)
                        val errorBody = response.errorBody()?.string()
                        val code = response.code()
                        val message = response.message()
                        Log.e(
                            "LoginActivity",
                            "Google login failed. HTTP $code: $message. Error body: $errorBody"
                        )
                        Toast.makeText(
                            this@LoginActivity,
                            "Google login failed: HTTP $code. See log for details.", // (Patel, 2025)
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("LoginActivity", "Google login request failed", t) // (Patel, 2025)
                    Toast.makeText(
                        this@LoginActivity,
                        "Network or server error: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    /**
     * Handles successful login by storing token and fetching user info // (Patel, 2025)
     */
    private fun handleLoginSuccess(token: String) {
        Log.d("LoginActivity", "Login successful. JWT Token: $token")
        SecurePrefs.saveToken(this, token) // Save for biometric login
        ApiClient.setToken(token)
        fetchCurrentUser()
    }

    /**
     * Fetches the current user and navigates to the correct dashboard based on role // (Patel, 2025)
     */
    private fun fetchCurrentUser() {
        Log.d("LoginActivity", "Fetching current user info...")
        ApiClient.userApi.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    Log.d("LoginActivity", "User info received: ID=${user.userID}, Role=${user.role}")
                    ApiClient.currentUserId = user.userID

                    // Navigate based on user role // (Patel, 2025)
                    when (user.role.lowercase()) {
                        "student" -> {
                            Log.d("LoginActivity", "Navigating to StudentDashboardActivity")
                            startActivity(Intent(this@LoginActivity, StudentDashboardActivity::class.java))
                        }
                        "lecturer" -> {
                            Log.d("LoginActivity", "Navigating to LecturerDashboardActivity") // (Patel, 2025)
                            startActivity(Intent(this@LoginActivity, LecturerDashboardActivity::class.java))
                        }
                        else -> {
                            Log.w("LoginActivity", "Unknown role: ${user.role}")
                            Toast.makeText(this@LoginActivity, "Unknown role", Toast.LENGTH_SHORT).show()
                        }
                    }
                    finish()
                } else {
                    Log.w("LoginActivity", "Failed to fetch user info. HTTP ${response.code()}") // (Patel, 2025)
                    Toast.makeText(this@LoginActivity, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) { // (Patel, 2025)
                Log.e("LoginActivity", "Failed to fetch user info", t)
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showBiometricPrompt(savedToken: String) {
        val executor = ContextCompat.getMainExecutor(this)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Authenticate using your fingerprint or face")
            .setNegativeButtonText("Use password instead")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("LoginActivity", "Biometric authentication succeeded")
                    ApiClient.setToken(savedToken)
                    fetchCurrentUser()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w("LoginActivity", "Biometric error: $errString")
                    Toast.makeText(applicationContext, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w("LoginActivity", "Biometric authentication failed")
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */