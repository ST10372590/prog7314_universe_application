package student.projects.universe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Service to handle incoming Firebase Cloud Messaging (FCM) messages.
 * Differentiates between message types (assessment, message) and builds appropriate notifications.
 * Also handles token refresh and uploads token for push notifications.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d(TAG, "Data Payload: $data")
            val type = data["type"]

            when (type) {
                "assessment" -> {
                    val title = data["title"] ?: "New Assessment"
                    val description = data["description"] ?: "A new assessment has been uploaded."
                    val moduleId = data["moduleId"] ?: ""
                    Log.d(TAG, "Handling assessment notification for moduleId=$moduleId")
                    sendAssessmentNotification(title, description, moduleId, data)
                }
                "message" -> {
                    val senderName = data["senderName"] ?: "New Message"
                    val content = data["content"] ?: "You have received a new message."
                    Log.d(TAG, "Handling message notification from sender=$senderName")
                    sendMessageNotification(senderName, content, data)
                }
                else -> {
                    // Unknown or no type specified, fallback to notification payload or generic notification
                    val title = data["title"] ?: remoteMessage.notification?.title ?: "Notification"
                    val body = data["body"] ?: remoteMessage.notification?.body ?: ""
                    Log.d(TAG, "Handling generic notification: title=$title, body=$body")
                    sendMessageNotification(title, body, data)
                }
            }
        } else {
            // No data payload, fallback to notification payload only
            remoteMessage.notification?.let {
                Log.d(TAG, "Notification Body: ${it.body}")
                // Open a generic message screen
                sendMessageNotification(it.title ?: "New Message", it.body ?: "", emptyMap())
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        try {
            val currentUserId = ApiClient.currentUserId // Client-side stored current user id
            if (currentUserId != 0) {
                Log.d(TAG, "Uploading refreshed token for userId=$currentUserId")
                FirebaseTokenHelper.uploadUserToken(currentUserId.toString())
            } else {
                Log.w(TAG, "No current user id available to upload token")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error uploading new token", ex)
        }
    }

    /**
     * Builds and displays a notification for chat messages.
     *
     * @param title The title to display on the notification (usually sender's name)
     * @param messageBody The content of the message
     * @param data Additional data to assist with navigation or logic
     */
    private fun sendMessageNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        val senderId = data["senderId"]?.toIntOrNull() ?: data["senderID"]?.toIntOrNull() ?: 0
        val receiverId = data["receiverId"]?.toIntOrNull() ?: data["receiverID"]?.toIntOrNull() ?: 0
        val convId = data["conversationId"] ?: computeConversationIdSafely(senderId, receiverId)
        val senderName = data["senderName"] ?: title ?: "Message"

        Log.d(TAG, "Preparing message notification for conversationId=$convId, senderId=$senderId, receiverId=$receiverId")

        // Intent to open ChatActivity and open the correct conversation
        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("openFromNotification", true)
            putExtra("conversationId", convId)
            putExtra("senderId", senderId)
            putExtra("receiverId", receiverId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            convId.hashCode(), // Unique request code per conversation
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "message_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager, channelId, "Message Notifications")

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.unilogo)
            .setContentTitle(senderName)
            .setContentText(messageBody ?: "(attachment)")
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(1)

        notificationManager.notify(convId.hashCode() and 0xfffffff, notificationBuilder.build())
        Log.d(TAG, "Message notification sent for convId=$convId")
    }

    /**
     * Builds and displays a notification for assessments or similar events.
     *
     * @param title The notification title
     * @param description The notification content text
     * @param moduleId The related module ID, used for intent and notification ID
     * @param data Additional data if needed
     */
    private fun sendAssessmentNotification(title: String, description: String, moduleId: String, data: Map<String, String>) {
        Log.d(TAG, "Preparing assessment notification for moduleId=$moduleId")

        val intent = Intent(this, StudentAssessmentActivity::class.java).apply {
            putExtra("MODULE_ID", moduleId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            moduleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "assessment_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager, channelId, "Assessment Notifications")

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.unilogo)
            .setContentTitle(title)
            .setContentText(description)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(1)

        notificationManager.notify(moduleId.hashCode() and 0xfffffff, notificationBuilder.build())
        Log.d(TAG, "Assessment notification sent for moduleId=$moduleId")
    }

    /**
     * Creates notification channel for Android Oreo and above.
     *
     * @param notificationManager NotificationManager instance
     * @param channelId Unique channel ID string
     * @param channelName User visible channel name
     */
    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $channelName ($channelId)")
        }
    }

    /**
     * Safely compute conversation ID string based on sender and receiver IDs.
     * Used for unique identification of chat conversations.
     *
     * @param a senderId
     * @param b receiverId
     * @return conversation ID string in "min_max" format or "0_0" if invalid
     */
    private fun computeConversationIdSafely(a: Int, b: Int): String {
        return if (a <= 0 && b <= 0) {
            "0_0"
        } else {
            val first = if (a > 0 && b > 0) minOf(a, b) else maxOf(a, b)
            val second = if (a > 0 && b > 0) maxOf(a, b) else maxOf(a, b)
            "${first}_${second}"
        }
    }
}
