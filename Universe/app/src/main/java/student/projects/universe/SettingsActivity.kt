package student.projects.universe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

/**
 * Activity to display and update user settings.
 * Loads cached settings first, then fetches from API.
 * Allows saving updated settings back to server and caches locally.
 *
 * Implements:
 * - SharedPreferences caching for faster UI load
 * - Spinner dropdowns for user preferences
 * - Error and confirmation dialogs
 * - Network API calls via Retrofit (Aram, 2023)
 * - Extensive logging for easier debugging (Patel, 2025)
 */
class SettingsActivity : AppCompatActivity() {

    // UI components for user input and settings toggles
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhone: EditText

    private lateinit var switchEmail: Switch
    private lateinit var switchPush: Switch
    private lateinit var switchInApp: Switch
    private lateinit var switchProfilePublic: Switch
    private lateinit var switchShareUsage: Switch

    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerItemsPerPage: Spinner
    private lateinit var spinnerTimeZone: Spinner
    private lateinit var spinnerLanguage: Spinner

    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    // SharedPreferences file name to cache user settings locally
    private val prefsName = "user_settings_cache"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        Log.d("SettingsActivity", "SettingsActivity started") // (Patel, 2025)

        // Initialize UI elements by binding to layout views
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etPhone = findViewById(R.id.etPhone)

        switchEmail = findViewById(R.id.switchEmailNotifications)
        switchPush = findViewById(R.id.switchPushNotifications)
        switchInApp = findViewById(R.id.switchInAppNotifications)
        switchProfilePublic = findViewById(R.id.switchProfilePublic)
        switchShareUsage = findViewById(R.id.switchShareUsage)

        spinnerTheme = findViewById(R.id.spinnerTheme)
        spinnerItemsPerPage = findViewById(R.id.spinnerItemsPerPage)
        spinnerTimeZone = findViewById(R.id.spinnerTimeZone)
        spinnerLanguage = findViewById(R.id.spinnerPreferredLanguage)

        btnSave = findViewById(R.id.btnSaveSettings)
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE

        // Setup Spinner adapters with options for user selection (Patel, 2025)
        spinnerTheme.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("light", "dark", "system")
        )
        spinnerItemsPerPage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("5", "10", "20", "50")
        )
        spinnerTimeZone.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            TimeZone.getAvailableIDs().sorted()
        )
        spinnerLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("English", "Afrikaans", "French", "Spanish", "German", "Zulu", "Xhosa", "Portuguese")
        )

        // Load cached settings first for quick UI population
        loadFromCache()

        // Then fetch latest settings from API to ensure fresh data (Aram, 2023)
        loadSettings()

        // Save button triggers settings update to backend
        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    /**
     * Load settings from local SharedPreferences cache
     */
    private fun loadFromCache() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        etFirstName.setText(prefs.getString("firstName", ""))
        etLastName.setText(prefs.getString("lastName", ""))
        etPhone.setText(prefs.getString("phoneNumber", ""))
        Log.d("SettingsActivity", "Loaded cached settings from SharedPreferences") // (Patel, 2025)
    }

    /**
     * Cache user settings locally in SharedPreferences
     */
    private fun cacheSettings(req: SettingsRequest) {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
        prefs.putString("firstName", req.firstName)
        prefs.putString("lastName", req.lastName)
        prefs.putString("phoneNumber", req.phoneNumber)
        prefs.apply()
        Log.d("SettingsActivity", "Settings cached locally") // (Patel, 2025)
    }

    /**
     * Fetch user settings from backend API asynchronously using Retrofit
     */
    private fun loadSettings() {
        Log.d("SettingsActivity", "Fetching settings from API") // (Aram, 2023)
        progressBar.visibility = View.VISIBLE

        ApiClient.settingsApi.getMySettings().enqueue(object : Callback<SettingsResponse> {
            override fun onResponse(call: Call<SettingsResponse>, response: Response<SettingsResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val s = response.body()!!
                    Log.d("SettingsActivity", "Loaded settings: $s")

                    // Populate UI with retrieved values, applying null safety defaults
                    etFirstName.setText(s.firstName ?: "")
                    etLastName.setText(s.lastName ?: "")
                    etPhone.setText(s.phoneNumber ?: "")

                    switchEmail.isChecked = s.emailNotifications
                    switchPush.isChecked = s.pushNotifications
                    switchInApp.isChecked = s.inAppNotifications
                    switchProfilePublic.isChecked = s.profilePublic
                    switchShareUsage.isChecked = s.shareUsageData

                    spinnerTheme.setSelection(
                        (spinnerTheme.adapter as ArrayAdapter<String>).getPosition(s.theme)
                    )
                    spinnerItemsPerPage.setSelection(
                        (spinnerItemsPerPage.adapter as ArrayAdapter<String>).getPosition(s.itemsPerPage.toString())
                    )
                    spinnerTimeZone.setSelection(
                        (spinnerTimeZone.adapter as ArrayAdapter<String>).getPosition(s.timeZone ?: TimeZone.getDefault().id)
                    )
                    spinnerLanguage.setSelection(
                        (spinnerLanguage.adapter as ArrayAdapter<String>).getPosition(s.preferredLanguage)
                    )

                    // Cache response for next launch speed
                    val req = SettingsRequest(
                        firstName = s.firstName ?: "",
                        lastName = s.lastName ?: "",
                        phoneNumber = s.phoneNumber ?: "",
                        emailNotifications = s.emailNotifications,
                        pushNotifications = s.pushNotifications,
                        inAppNotifications = s.inAppNotifications,
                        profilePublic = s.profilePublic,
                        shareUsageData = s.shareUsageData,
                        theme = s.theme,
                        itemsPerPage = s.itemsPerPage,
                        timeZone = s.timeZone ?: "",
                        preferredLanguage = s.preferredLanguage
                    )
                    cacheSettings(req)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("SettingsActivity", "Failed to load settings: Code ${response.code()} Message $errorBody")
                    showErrorDialog("Failed to load settings.\nCode: ${response.code()}\nMessage: $errorBody")
                }
            }

            override fun onFailure(call: Call<SettingsResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e("SettingsActivity", "Network error loading settings: ${t.localizedMessage}", t)
                showErrorDialog("Network Error: ${t.localizedMessage}")
            }
        })
    }

    /**
     * Save updated user settings to backend API asynchronously using Retrofit
     */
    private fun saveSettings() {
        val req = SettingsRequest(
            firstName = etFirstName.text.toString().ifBlank { "Unknown" },
            lastName = etLastName.text.toString().ifBlank { "Unknown" },
            phoneNumber = etPhone.text.toString(),
            emailNotifications = switchEmail.isChecked,
            pushNotifications = switchPush.isChecked,
            inAppNotifications = switchInApp.isChecked,
            profilePublic = switchProfilePublic.isChecked,
            shareUsageData = switchShareUsage.isChecked,
            theme = spinnerTheme.selectedItem as String,
            itemsPerPage = (spinnerItemsPerPage.selectedItem as String).toInt(),
            timeZone = spinnerTimeZone.selectedItem as String,
            preferredLanguage = spinnerLanguage.selectedItem as String
        )

        Log.d("SettingsActivity", "Saving settings: $req") // (Patel, 2025)
        progressBar.visibility = View.VISIBLE

        ApiClient.settingsApi.updateMySettings(req).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    Log.d("SettingsActivity", "Settings updated successfully")
                    Toast.makeText(this@SettingsActivity, "✅ Settings saved successfully", Toast.LENGTH_SHORT).show()

                    // Cache immediately after saving
                    cacheSettings(req)

                    // Show confirmation dialog with updated info
                    showConfirmationDialog(req.firstName, req.lastName, req.phoneNumber)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("SettingsAPI", "Save failed: ${response.code()} - $errorBody")
                    showErrorDialog("Failed to save settings.\nCode: ${response.code()}\nMessage: $errorBody")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e("SettingsAPI", "Network failure: ${t.message}", t)
                showErrorDialog("Network Error: ${t.localizedMessage}")
            }
        })
    }

    /**
     * Show a confirmation dialog with saved user settings summary
     */
    private fun showConfirmationDialog(firstName: String?, lastName: String?, phone: String?) {
        val message = """
            Your settings were saved successfully!

            👤 FirstName: $firstName 
               LastName: $lastName
            📞 Phone: $phone
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Settings Updated")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Show an error dialog displaying the given message
     */
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("⚠ Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}


/*

Reference List

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at:  https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

*/
