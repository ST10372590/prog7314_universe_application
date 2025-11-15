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
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Prefer data payload (it gives us control). If not present, use notification payload.
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            Log.d("FCM", "Data Payload: $data")
            val type = data["type"]

            when (type) {
                "assessment" -> {
                    val title = data["title"] ?: "New Assessment"
                    val description = data["description"] ?: "A new assessment has been uploaded."
                    val moduleId = data["moduleId"] ?: ""
                    sendAssessmentNotification(title, description, moduleId, data)
                }
                "message" -> {
                    val senderName = data["senderName"] ?: "New Message"
                    val content = data["content"] ?: "You have received a new message."
                    sendMessageNotification(senderName, content, data)
                }
                else -> {
                    // Unknown type — show something generic
                    val title = data["title"] ?: remoteMessage.notification?.title ?: "Notification"
                    val body = data["body"] ?: remoteMessage.notification?.body ?: ""
                    sendMessageNotification(title, body, data)
                }
            }
        } else {
            // Fallback to notification-only payload
            remoteMessage.notification?.let {
                Log.d("FCM", "Notification Body: ${it.body}")
                // We can't guarantee data here; open a generic screen (DirectMessageActivity)
                sendMessageNotification(it.title ?: "New Message", it.body ?: "", emptyMap())
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")

        // Best-effort: upload to Firestore if we know current user id
        try {
            val currentUserId = ApiClient.currentUserId // your client-side tracked user id
            if (currentUserId != 0) {
                FirebaseTokenHelper.uploadUserToken(currentUserId.toString())
            } else {
                Log.w("MyFirebaseMsgService", "No current user id available to upload token")
            }
        } catch (ex: Exception) {
            Log.e("MyFirebaseMsgService", "Error uploading new token", ex)
        }
    }

    /**
     * Builds and shows a notification for a chat message.
     * data: map containing keys such as senderId, receiverId, conversationId, senderName, content
     */
    private fun sendMessageNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        // Extract useful data for navigation
        val senderId = data["senderId"]?.toIntOrNull() ?: data["senderID"]?.toIntOrNull() ?: 0
        val receiverId = data["receiverId"]?.toIntOrNull() ?: data["receiverID"]?.toIntOrNull() ?: 0
        val convId = data["conversationId"] ?: computeConversationIdSafely(senderId, receiverId)
        val senderName = data["senderName"] ?: title ?: "Message"

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
            convId.hashCode(), // unique per conversation
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

        notificationManager.notify((convId.hashCode() and 0xfffffff), notificationBuilder.build())
    }

    /**
     * Builds and shows a notification for assessments or other events.
     * Also includes optional data for navigation.
     */
    private fun sendAssessmentNotification(title: String, description: String, moduleId: String, data: Map<String, String>) {
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

        notificationManager.notify((moduleId.hashCode() and 0xfffffff), notificationBuilder.build())
    }

    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Helper to compute conversation id if the sender/receiver ids are present.
     * Returns "0_0" if no valid ids.
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
