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

/**
 * LoginActivity handles authentication for the app including:
 * - Username/password login via backend API
 * - Google Sign-In authentication with token exchange
 * - Biometric login for saved sessions
 * - Registration navigation
 *
 * Implements secure token storage, API token management, and FCM token registration.
 *
 * This class follows Android best practices for authentication flows and user feedback.
 *
 * Reference:
 * Patel (2025) emphasizes Kotlin features enhancing Android app development
 * including use of concise lambdas, coroutines, and secure storage patterns.
 */
class LoginActivity : AppCompatActivity() {

    // Google Sign-In client for OAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Web client ID for Google OAuth (replace with your own)
    private val WEB_CLIENT_ID =
        "64158382237-oe44dvlh679n673iqtanlf9ifrkidvip.apps.googleusercontent.com"

    /**
     * Launcher for Google Sign-In activity result.
     * Handles success by extracting ID token and initiating backend login.
     */
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
                Log.w(TAG, "Google ID token is null after sign-in")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed", e)
            Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Bind UI elements
        val username = findViewById<EditText>(R.id.etUsername)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<Button>(R.id.btnGoogleLogin)
        val btnBiometric = findViewById<Button>(R.id.btnBiometricLogin)
        val btnGoRegister = findViewById<TextView>(R.id.btnGoRegister)

        // Check if token saved from previous session to prompt biometric login
        val savedToken = SecurePrefs.getToken(this)
        if (savedToken != null) {
            showBiometricChoiceDialog(savedToken)
        }

        // Username/password login click listener
        btnLogin.setOnClickListener {
            val user = username.text.toString()
            val pass = password.text.toString()

            if (user.isBlank() || pass.isBlank()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(user, pass)
            ApiClient.authApi.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d(TAG, "Username/password login successful")
                        handleLoginSuccess(response.body()!!.token)
                    } else {
                        Log.w(TAG, "Username/password login failed with response code: ${response.code()}")
                        Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e(TAG, "Network error during login", t)
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Setup Google Sign-In options and client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogle.setOnClickListener {
            // Always sign out before new sign-in to avoid cached accounts
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "Launching Google Sign-In intent")
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        // Biometric login click listener
        btnBiometric.setOnClickListener {
            val token = SecurePrefs.getToken(this)
            if (token != null) {
                showBiometricPrompt(token)
            } else {
                Toast.makeText(this, "No saved session found. Please login once first.", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to registration screen
        btnGoRegister.setOnClickListener {
            Log.d(TAG, "Navigating to RegisterActivity")
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Handles backend login using Google ID token.
     */
    private fun loginWithGoogle(idToken: String) {
        Log.d(TAG, "Attempting Google login with ID token")
        val googleLoginRequest = GoogleLoginRequest(IdToken = idToken)
        ApiClient.authApi.loginWithGoogle(googleLoginRequest)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d(TAG, "Google login successful")
                        handleLoginSuccess(response.body()!!.token)
                    } else {
                        Log.w(TAG, "Google login failed with code: ${response.code()}")
                        Toast.makeText(this@LoginActivity, "Google login failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e(TAG, "Network error during Google login", t)
                    Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * After successful login, save token securely, update API client, and fetch user info.
     */
    private fun handleLoginSuccess(token: String) {
        Log.i(TAG, "Login successful, saving token and fetching user info")
        SecurePrefs.saveToken(this, token)
        ApiClient.setToken(token)
        fetchCurrentUser()
    }

    /**
     * Fetches current logged-in user's profile and navigates to appropriate dashboard.
     * Also registers device token for push notifications.
     */
    private fun fetchCurrentUser() {
        ApiClient.userApi.getCurrentUser().enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    ApiClient.currentUserId = user.userID
                    Log.d(TAG, "Fetched current user: ID=${user.userID}, Role=${user.role}")

                    // Register FCM token in Firestore and backend for push notifications
                    registerDeviceTokenWithBackend(user.userID.toString())

                    // Navigate based on user role (case-insensitive)
                    when (user.role.lowercase()) {
                        "student" -> startActivity(Intent(this@LoginActivity, StudentDashboardActivity::class.java))
                        "lecturer" -> startActivity(Intent(this@LoginActivity, LecturerDashboardActivity::class.java))
                        else -> {
                            Log.w(TAG, "Unknown user role: ${user.role}")
                            Toast.makeText(this@LoginActivity, "Unknown role", Toast.LENGTH_SHORT).show()
                        }
                    }
                    finish()
                } else {
                    Log.w(TAG, "Failed to fetch user info, response code: ${response.code()}")
                    Toast.makeText(this@LoginActivity, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e(TAG, "Network error fetching user info", t)
                Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Registers the Firebase Cloud Messaging (FCM) device token both in Firestore
     * (for Firebase Cloud Functions) and in the backend API for push notifications.
     */
    private fun registerDeviceTokenWithBackend(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val fcmToken = task.result
            Log.d(TAG, "FCM Token retrieved: $fcmToken")

            // 1. Upload token to Firestore for Cloud Functions trigger
            FirebaseTokenHelper.uploadUserToken(userId)

            // 2. Register token with backend API
            val deviceTokenRequest = DeviceTokenRequest(fcmToken)
            ApiClient.userApi.registerDeviceToken(deviceTokenRequest).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Device token registered successfully on backend")
                    } else {
                        Log.w(TAG, "Failed to register device token on backend, code: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e(TAG, "Error registering device token on backend: ${t.message}")
                }
            })
        }
    }

    /**
     * Shows biometric authentication prompt to the user to authenticate and
     * use saved token for login if authentication succeeds.
     */
    private fun showBiometricPrompt(savedToken: String) {
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric authentication not available on this device", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Biometric authentication not supported")
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
                    Log.d(TAG, "Biometric authentication succeeded")
                    ApiClient.setToken(savedToken)
                    fetchCurrentUser()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(applicationContext, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Biometric authentication error: $errString")
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "Biometric authentication failed")
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Displays a dialog asking user whether to login using biometric authentication.
     */
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