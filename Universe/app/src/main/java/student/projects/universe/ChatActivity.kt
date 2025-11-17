package student.projects.universe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var tvChatHeader: TextView
    private lateinit var tvTypingIndicator: TextView
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton

    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<MessageResponse>()

    private val messageApi = ApiClient.messageApi
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var dbHelper: DatabaseHelper

    private var currentUserId = 0
    private var receiverId = 0
    private var receiverName: String? = null

    private lateinit var convId: String
    private var isChatVisible = false

    private val CHANNEL_ID = "chat_messages_channel"
    private val NOTIF_ID = 1001

    // Typing indicator variables
    private var typing = false
    private val typingTimeout = 1500L
    private val typingHandler = Handler(Looper.getMainLooper())

    private var messagesListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize local database helper (SQLite)
        dbHelper = DatabaseHelper(this)

        // Bind UI components
        tvChatHeader = findViewById(R.id.tvChatHeader)
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator)
        recyclerMessages = findViewById(R.id.recyclerMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        // Retrieve user IDs and receiver info from intent extras or fallback defaults
        currentUserId = intent.getIntExtra("currentUserId", ApiClient.currentUserId)
        receiverId = intent.getIntExtra("receiverId", 0)
        receiverName = intent.getStringExtra("receiverName")

        if (currentUserId == 0 || receiverId == 0) {
            Toast.makeText(this, "Invalid chat IDs", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Set chat header text
        tvChatHeader.text = "Chat with ${receiverName ?: "User"}"
        tvTypingIndicator.visibility = View.GONE

        // Setup RecyclerView with linear layout and adapter
        recyclerMessages.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(messages, currentUserId)
        recyclerMessages.adapter = adapter

        // Generate unique conversation ID based on user IDs
        convId = getConversationId(currentUserId, receiverId)

        // Create notification channel for local notifications.
        createNotificationChannel()

        // Send button click listener: send message if not empty
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                Log.d("ChatActivity", "Send button clicked with message: $text")
                sendMessage(text)
            }
        }

        // Listen for typing changes in the EditText to update typing indicator
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!typing) {
                    typing = true
                    setTypingStatus(true)
                    Log.d("ChatActivity", "User started typing")
                }
                typingHandler.removeCallbacksAndMessages(null)
                typingHandler.postDelayed({
                    typing = false
                    setTypingStatus(false)
                    Log.d("ChatActivity", "User stopped typing")
                }, typingTimeout)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Start listening for typing status from other user
        listenToTypingStatus()
    }

    override fun onResume() {
        super.onResume()
        isChatVisible = true
        Log.d("ChatActivity", "Chat visible - loading conversation history")
        loadConversationHistory()
        resetUnreadCounterFor(currentUserId)
    }

    override fun onPause() {
        super.onPause()
        isChatVisible = false
        Log.d("ChatActivity", "Chat not visible - stopping listeners and resetting typing status")
        setTypingStatus(false)
        messagesListener?.let {
            db.child("conversations").child(convId).child("messages").removeEventListener(it)
        }
        messagesListener = null
    }

    /**
     * Loads conversation history from server or local cache.
     * If online, fetches from API and caches locally.
     * If offline or error, loads from SQLite cache.
     */
    private fun loadConversationHistory() {
        if (isOnline()) {
            messageApi.getConversation(currentUserId, receiverId).enqueue(object : Callback<List<MessageResponse>> {
                override fun onResponse(call: Call<List<MessageResponse>>, response: Response<List<MessageResponse>>) {
                    if (response.isSuccessful && response.body() != null) {
                        Log.d("ChatActivity", "Conversation loaded from API, message count: ${response.body()!!.size}")
                        messages.clear()
                        messages.addAll(response.body()!!)

                        // Map API responses to local SQLite entities
                        val chatMessageEntities = messages.map { msg ->
                            ChatMessageEntity(
                                messageId = msg.messageID,
                                senderId = msg.senderID,
                                receiverId = msg.receiverID,
                                content = msg.content,
                                timestamp = msg.timestamp,
                                readStatus = msg.readStatus,
                                fileAttachment = msg.fileAttachment
                            )
                        }

                        // Fix null or blank timestamps before saving
                        val safeChatMessages = chatMessageEntities.map {
                            if (it.timestamp.isNullOrBlank()) it.copy(timestamp = getCurrentTimestamp()) else it
                        }

                        // Cache messages locally
                        dbHelper.insertOrUpdateMessages(safeChatMessages)

                        adapter.notifyDataSetChanged()
                        recyclerMessages.scrollToPosition(messages.size - 1)
                        startListeningForMessages()
                    } else {
                        Log.w("ChatActivity", "No previous messages found on server")
                        Toast.makeText(this@ChatActivity, "No previous messages found.", Toast.LENGTH_SHORT).show()
                        loadMessagesFromCache()
                        startListeningForMessages()
                    }
                }

                private fun getCurrentTimestamp(): String {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    return sdf.format(Date())
                }

                override fun onFailure(call: Call<List<MessageResponse>>, t: Throwable) {
                    Log.e("ChatActivity", "Failed to load chat history: ${t.localizedMessage}")
                    Toast.makeText(this@ChatActivity, "Failed to load chat history: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                    loadMessagesFromCache()
                    startListeningForMessages()
                }
            })
        } else {
            Log.d("ChatActivity", "Offline mode - loading messages from cache")
            loadMessagesFromCache()
            startListeningForMessages()
        }
    }

    /**
     * Loads cached messages from local SQLite database
     */
    private fun loadMessagesFromCache() {
        messages.clear()
        val cachedMessages = dbHelper.getConversationMessages(currentUserId, receiverId)
        val convertedMessages = cachedMessages.map { entity ->
            MessageResponse(
                messageID = entity.messageId,
                senderID = entity.senderId,
                receiverID = entity.receiverId,
                content = entity.content,
                timestamp = entity.timestamp,
                readStatus = entity.readStatus,
                fileAttachment = entity.fileAttachment
            )
        }
        messages.addAll(convertedMessages)
        adapter.notifyDataSetChanged()
        recyclerMessages.scrollToPosition(messages.size - 1)
        Log.d("ChatActivity", "Loaded ${convertedMessages.size} messages from cache")
    }

    /**
     * Sets up Firebase Realtime Database listener for new messages
     */
    private fun startListeningForMessages() {
        val messagesRef = db.child("conversations").child(convId).child("messages")
        if (messagesListener != null) return

        messagesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.getValue(FirebaseMessage::class.java) ?: return
                val resp = MessageResponse(
                    messageID = msg.messageId ?: 0,
                    senderID = msg.senderId,
                    receiverID = msg.receiverId,
                    content = msg.content ?: "",
                    readStatus = if (msg.read) "Read" else "Unread",
                    timestamp = msg.timestamp ?: "",
                    fileAttachment = msg.fileUrl ?: null
                )

                if (messages.any { it.messageID == resp.messageID }) return

                messages.add(resp)
                adapter.notifyItemInserted(messages.size - 1)
                recyclerMessages.scrollToPosition(messages.size - 1)

                // Cache message locally
                val chatEntity = ChatMessageEntity(
                    messageId = resp.messageID,
                    senderId = resp.senderID,
                    receiverId = resp.receiverID,
                    content = resp.content,
                    timestamp = resp.timestamp,
                    readStatus = resp.readStatus,
                    fileAttachment = resp.fileAttachment
                )
                dbHelper.insertOrUpdateMessage(chatEntity)

                if (msg.senderId != currentUserId) {
                    // Removed local notification here to rely on FCM

                    if (isChatVisible) {
                        snapshot.ref.child("read").setValue(true)
                        resetUnreadCounterFor(currentUserId)
                        Log.d("ChatActivity", "Marked message as read and reset unread counter")
                    }
                }
                Log.d("ChatActivity", "Received new message with ID: ${resp.messageID}")
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Firebase messages listener cancelled: ${error.message}")
            }
        }

        messagesRef.addChildEventListener(messagesListener as ChildEventListener)
        Log.d("ChatActivity", "Started listening for new Firebase messages")
    }

    /**
     * Sends a chat message through the REST API and updates UI/cache accordingly
     */
    private fun sendMessage(text: String) {
        val msgRequest = MessageRequest(
            senderID = currentUserId,
            receiverID = receiverId,
            content = text,
            fileAttachment = null,
            readStatus = "Unread"
        )

        // Insert message locally with temporary ID for instant UI feedback
        val localMessage = MessageResponse(
            messageID = 0, // temporary ID
            senderID = currentUserId,
            receiverID = receiverId,
            content = text,
            readStatus = "Unread",
            timestamp = isoNow(),
            fileAttachment = null
        )
        messages.add(localMessage)
        adapter.notifyItemInserted(messages.size - 1)
        recyclerMessages.scrollToPosition(messages.size - 1)
        Log.d("ChatActivity", "Local message inserted with temp ID 0")

        val chatEntity = ChatMessageEntity(
            messageId = localMessage.messageID,
            senderId = localMessage.senderID,
            receiverId = localMessage.receiverID,
            content = localMessage.content,
            timestamp = localMessage.timestamp,
            readStatus = localMessage.readStatus,
            fileAttachment = localMessage.fileAttachment
        )
        dbHelper.insertOrUpdateMessage(chatEntity)

        // Send message to server
        messageApi.sendMessage(msgRequest).enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Update local cache with server response replacing temp message
                    dbHelper.updateMessageWithServerResponse(localMessage.messageID, body)

                    // Update message in UI list
                    val index = messages.indexOf(localMessage)
                    if (index >= 0) {
                        messages[index] = body
                        adapter.notifyItemChanged(index)
                    }

                    pushMessageToFirebase(body)
                    etMessage.text.clear()
                    Log.d("ChatActivity", "Message sent successfully and pushed to Firebase")
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to send (server)", Toast.LENGTH_SHORT).show()
                    Log.e("ChatActivity", "Failed to send message: server error")
                }
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Toast.makeText(this@ChatActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                Log.e("ChatActivity", "Failed to send message: network error ${t.localizedMessage}")
            }
        })
    }

    /**
     * Pushes message to Firebase Realtime Database for realtime sync
     */
    private fun pushMessageToFirebase(body: MessageResponse) {
        val messagesRef = db.child("conversations").child(convId).child("messages")
        val key = messagesRef.push().key ?: return

        val ts = body.timestamp?.takeIf { it.isNotBlank() } ?: isoNow()
        val fm = FirebaseMessage(
            messageId = body.messageID,
            senderId = body.senderID,
            receiverId = body.receiverID,
            content = body.content,
            timestamp = ts,
            fileUrl = body.fileAttachment,
            read = false
        )

        messagesRef.child(key).setValue(fm)

        val metaRef = db.child("conversations").child(convId).child("meta")
        metaRef.child("lastMessage").setValue(body.content)
        metaRef.child("lastSenderId").setValue(currentUserId)
        metaRef.child("timestamp").setValue(ts)

        val unreadKey = "unread_${body.receiverID}"
        metaRef.child(unreadKey).runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                val v = current.getValue(Int::class.java) ?: 0
                current.value = v + 1
                return Transaction.success(current)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) Log.e("ChatActivity", "Transaction failed: ${error.message}")
            }
        })
        Log.d("ChatActivity", "Pushed message to Firebase with messageId: ${body.messageID}")
    }

    /**
     * Resets the unread message counter for a user in Firebase metadata
     */
    private fun resetUnreadCounterFor(userId: Int) {
        val metaRef = db.child("conversations").child(convId).child("meta")
        val key = "unread_$userId"
        metaRef.child(key).setValue(0)
        Log.d("ChatActivity", "Reset unread message counter for userId: $userId")
    }

    /**
     * Generates a unique conversation ID based on user IDs
     */
    private fun getConversationId(userId1: Int, userId2: Int): String {
        val first = minOf(userId1, userId2)
        val second = maxOf(userId1, userId2)
        return "${first}_${second}"
    }

    /**
     * Returns current time in ISO 8601 UTC format
     */
    private fun isoNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Shows a local notification for a new incoming message (not currently used - relies on FCM)
     */
    private fun showLocalNotification(msg: FirebaseMessage) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("currentUserId", currentUserId)
            putExtra("receiverId", msg.senderId)
            putExtra("receiverName", receiverName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.unilogo)
            .setContentTitle("New message from ${receiverName ?: "Contact"}")
            .setContentText(msg.content ?: "(attachment)")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID + (msg.senderId % 100), notif)
        Log.d("ChatActivity", "Displayed local notification for new message")
    }

    /**
     * Creates notification channel for Android 8+ notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat messages"
            val desc = "Notifications for new chat messages"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = desc }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
            Log.d("ChatActivity", "Notification channel created")
        }
    }

    /**
     * Updates typing status for current user in Firebase
     */
    private fun setTypingStatus(isTyping: Boolean) {
        val typingRef = db.child("conversations").child(convId).child("typing_$currentUserId")
        typingRef.setValue(isTyping)
        Log.d("ChatActivity", "Set typing status to $isTyping")
    }

    /**
     * Listens to typing status changes of the other user
     */
    private fun listenToTypingStatus() {
        val otherUserTypingRef = db.child("conversations").child(convId).child("typing_$receiverId")
        otherUserTypingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                tvTypingIndicator.visibility = if (isTyping) View.VISIBLE else View.GONE
                Log.d("ChatActivity", "Other user typing status changed: $isTyping")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Typing status listener cancelled: ${error.message}")
            }
        })
        Log.d("ChatActivity", "Started listening for other user's typing status")
    }

    /**
     * Checks if device has active internet connection
     */
    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val net = cm.activeNetworkInfo
        val online = net?.isConnectedOrConnecting == true
        Log.d("ChatActivity", "Network connectivity checked: $online")
        return online
    }
}

/*

Reference List

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at: https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

Patel, B. 2025. 12 Top Kotlin Features to Enhance Android App
Development Process, 20 May 2025, spaceotechnologies. [Blog]. Available at:
https://www.spaceotechnologies.com/blog/kotlin-features/ [27 October 2025]

*/

// End of ChatActivity.kt
