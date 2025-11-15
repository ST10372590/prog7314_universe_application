package student.projects.universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val WEB_CLIENT_ID =
        "64158382237-oe44dvlh679n673iqtanlf9ifrkidvip.apps.googleusercontent.com"

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

        val username = findViewById<EditText>(R.id.etUsername)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogleLogin)
        val btnBiometric = findViewById<Button>(R.id.btnBiometricLogin)
        val btnGoRegister = findViewById<TextView>(R.id.btnGoRegister)

        val savedToken = SecurePrefs.getToken(this)
        if (savedToken != null) showBiometricChoiceDialog(savedToken)

        // 🔹 Username/Password Login
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

        // 🔹 Google Login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        btnGoogle.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        // 🔹 Biometric Login
        btnBiometric.setOnClickListener {
            val token = SecurePrefs.getToken(this)
            if (token != null) {
                showBiometricPrompt(token)
            } else {
                Toast.makeText(this, "No saved session found. Please login once first.", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔹 Navigate to Register
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

                    // ✅ Register FCM Token both in Firestore & backend
                    registerDeviceTokenWithBackend(user.userID.toString())

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

    // ✅ Register FCM token in both Firestore (for Cloud Functions) and Backend (for .NET API)
    private fun registerDeviceTokenWithBackend(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("LoginActivity", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val fcmToken = task.result
            Log.d("LoginActivity", "FCM Token: $fcmToken")

            // ✅ 1. Store token in Firestore (for Firebase Cloud Functions)
            FirebaseTokenHelper.uploadUserToken(userId)

            // ✅ 2. Send token to .NET backend
            val deviceTokenRequest = DeviceTokenRequest(fcmToken)
            ApiClient.userApi.registerDeviceToken(deviceTokenRequest).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("LoginActivity", "Device token registered successfully on backend")
                    } else {
                        Log.w("LoginActivity", "Failed to register device token on backend")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("LoginActivity", "Error registering device token: ${t.message}")
                }
            })
        }
    }

    // 🔹 Biometric authentication setup
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
                    Toast.makeText(applicationContext, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showBiometricChoiceDialog(savedToken: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Use Biometric Login?")
            .setMessage("Would you like to login using your fingerprint or face ID?")
            .setPositiveButton("Yes") { _, _ -> showBiometricPrompt(savedToken) }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .create()
        dialog.show()
    }
}

/*

Reference List

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

 */