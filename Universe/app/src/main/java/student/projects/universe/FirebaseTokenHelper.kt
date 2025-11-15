package student.projects.universe

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object FirebaseTokenHelper {

    /**
     * Uploads the current FCM token to Firestore under collection "user_tokens".
     * Stores tokens in an array field "tokens" using arrayUnion so multiple devices are supported.
     * userId should be the string representation of the authenticated user's ID.
     */
    fun uploadUserToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FirebaseTokenHelper", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            if (token == null) {
                Log.w("FirebaseTokenHelper", "Token result was null")
                return@addOnCompleteListener
            }

            Log.d("FirebaseTokenHelper", "FCM Token: $token")

            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("user_tokens").document(userId)

            // Try to add the token to the tokens array (arrayUnion is atomic)
            docRef.update("tokens", FieldValue.arrayUnion(token))
                .addOnSuccessListener {
                    Log.d("FirebaseTokenHelper", "Token appended for user $userId")
                }
                .addOnFailureListener { e ->
                    // If update fails (likely because doc doesn't exist) create it
                    Log.w("FirebaseTokenHelper", "Update failed, attempting to create doc", e)
                    val data = mapOf("tokens" to listOf(token))
                    docRef.set(data)
                        .addOnSuccessListener { Log.d("FirebaseTokenHelper", "Token created for $userId") }
                        .addOnFailureListener { ex ->
                            Log.e("FirebaseTokenHelper", "Error creating token doc for $userId", ex)
                        }
                }
        }
    }
}
