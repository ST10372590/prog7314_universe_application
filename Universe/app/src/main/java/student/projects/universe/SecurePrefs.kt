package student.projects.universe

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecurePrefs {
    private const val PREF_NAME = "secure_prefs"

    private fun getPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            PREF_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString("jwt_token", token).apply()
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString("jwt_token", null)
    }

    fun clearToken(context: Context) {
        val sharedPref = context.getSharedPreferences("SecurePrefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("jwt_token").apply()
    }
}
