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

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Check if the message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Data Payload: ${remoteMessage.data}")

            val data = remoteMessage.data
            val type = data["type"]  // expecting "message" or "assessment"

            when (type) {
                "assessment" -> {
                    val title = data["title"] ?: "New Assessment"
                    val description = data["description"] ?: "A new assessment has been uploaded."
                    val moduleId = data["moduleId"] ?: ""
                    sendAssessmentNotification(title, description, moduleId)
                }
                "message" -> {
                    val senderName = data["senderName"] ?: "New Message"
                    val content = data["content"] ?: "You have received a new message."
                    sendMessageNotification(senderName, content)
                }
                else -> {
                    // fallback to notification payload if available
                    remoteMessage.notification?.let {
                        sendMessageNotification(it.title, it.body)
                    }
                }
            }
        } else {
            // Handle notification messages without data payload
            remoteMessage.notification?.let {
                Log.d("FCM", "Notification Body: ${it.body}")
                sendMessageNotification(it.title, it.body)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Refreshed token: $token")
        // TODO: Send this token to your backend if you manage device tokens
    }

    private fun sendMessageNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, DirectMessageActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "message_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(
            notificationManager,
            channelId,
            "Message Notifications"
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.unilogo)
            .setContentTitle(title ?: "New Message")
            .setContentText(messageBody ?: "You have received a new message.")
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(1)  // you can track badge count if you manage it externally

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun sendAssessmentNotification(title: String, description: String, moduleId: String) {
        val intent = Intent(this, StudentAssessmentActivity::class.java).apply {
            putExtra("MODULE_ID", moduleId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "assessment_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(
            notificationManager,
            channelId,
            "Assessment Notifications"
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.unilogo)
            .setContentTitle(title)
            .setContentText(description)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(1)  // Update badge count here if managed

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        channelId: String,
        channelName: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}