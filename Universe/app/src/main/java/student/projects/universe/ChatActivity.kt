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

    private var currentUserId = 0
    private var receiverId = 0
    private var receiverName: String? = null

    private lateinit var convId: String
    private var isChatVisible = false

    private val CHANNEL_ID = "chat_messages_channel"
    private val NOTIF_ID = 1001

    // Typing indicator related
    private var typing = false
    private val typingTimeout = 1500L // 1.5 seconds
    private val typingHandler = Handler(Looper.getMainLooper())

    // Firebase listener reference to detach later if needed
    private var messagesListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        tvChatHeader = findViewById(R.id.tvChatHeader)
        tvTypingIndicator = findViewById(R.id.tvTypingIndicator)
        recyclerMessages = findViewById(R.id.recyclerMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        currentUserId = intent.getIntExtra("currentUserId", ApiClient.currentUserId)
        receiverId = intent.getIntExtra("receiverId", 0)
        receiverName = intent.getStringExtra("receiverName")

        if (currentUserId == 0 || receiverId == 0) {
            Toast.makeText(this, "Invalid chat IDs", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvChatHeader.text = "Chat with ${receiverName ?: "User"}"
        tvTypingIndicator.visibility = View.GONE

        recyclerMessages.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(messages, currentUserId)
        recyclerMessages.adapter = adapter

        convId = getConversationId(currentUserId, receiverId)

        createNotificationChannel()

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        // Typing status update on text change
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!typing) {
                    typing = true
                    setTypingStatus(true)
                }
                typingHandler.removeCallbacksAndMessages(null)
                typingHandler.postDelayed({
                    typing = false
                    setTypingStatus(false)
                }, typingTimeout)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        listenToTypingStatus()
    }

    override fun onResume() {
        super.onResume()
        isChatVisible = true
        loadConversationHistory()
        resetUnreadCounterFor(currentUserId)
    }

    override fun onPause() {
        super.onPause()
        isChatVisible = false
        setTypingStatus(false)  // Reset typing indicator on leaving chat
        // Optionally remove Firebase listener if you want to avoid duplicates on resume
        messagesListener?.let { db.child("conversations").child(convId).child("messages").removeEventListener(it) }
        messagesListener = null
    }

    private fun loadConversationHistory() {
        messageApi.getConversation(currentUserId, receiverId).enqueue(object : Callback<List<MessageResponse>> {
            override fun onResponse(call: Call<List<MessageResponse>>, response: Response<List<MessageResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    messages.clear()
                    messages.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                    recyclerMessages.scrollToPosition(messages.size - 1)
                    startListeningForMessages() // Start Firebase listener after loading history
                } else {
                    Toast.makeText(this@ChatActivity, "No previous messages found.", Toast.LENGTH_SHORT).show()
                    // Still start listener for new messages
                    startListeningForMessages()
                }
            }

            override fun onFailure(call: Call<List<MessageResponse>>, t: Throwable) {
                Toast.makeText(this@ChatActivity, "Failed to load chat history: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                // Start listener anyway so new messages come in
                startListeningForMessages()
            }
        })
    }

    private fun startListeningForMessages() {
        val messagesRef = db.child("conversations").child(convId).child("messages")
        if (messagesListener != null) return // already listening

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

                // Avoid adding duplicates that came from backend loading
                if (messages.any { it.messageID == resp.messageID }) return

                messages.add(resp)
                adapter.notifyItemInserted(messages.size - 1)
                recyclerMessages.scrollToPosition(messages.size - 1)

                if (msg.senderId != currentUserId && !isChatVisible) {
                    showLocalNotification(msg)
                }

                if (msg.senderId != currentUserId && isChatVisible) {
                    snapshot.ref.child("read").setValue(true)
                    resetUnreadCounterFor(currentUserId)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Firebase messages listener cancelled: ${error.message}")
            }
        }

        messagesRef.addChildEventListener(messagesListener as ChildEventListener)
    }

    private fun sendMessage(text: String) {
        val msgRequest = MessageRequest(
            senderID = currentUserId,
            receiverID = receiverId,
            content = text,
            fileAttachment = null,
            readStatus = "Unread"
        )

        messageApi.sendMessage(msgRequest).enqueue(object : Callback<MessageResponse> {
            override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    pushMessageToFirebase(body)
                    etMessage.text.clear()
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to send (server)", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                Toast.makeText(this@ChatActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

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
    }

    private fun resetUnreadCounterFor(userId: Int) {
        val metaRef = db.child("conversations").child(convId).child("meta")
        val key = "unread_${userId}"
        metaRef.child(key).setValue(0)
    }

    private fun getConversationId(a: Int, b: Int): String {
        return if (a < b) "${a}_${b}" else "${b}_${a}"
    }

    private fun isoNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

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
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Chat messages"
            val desc = "Notifications for new chat messages"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = desc }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    // --- TYPING STATUS HANDLING ---

    private fun setTypingStatus(isTyping: Boolean) {
        val typingRef = db.child("conversations").child(convId).child("typing_$currentUserId")
        typingRef.setValue(isTyping)
    }

    private fun listenToTypingStatus() {
        val otherUserTypingRef = db.child("conversations").child(convId).child("typing_$receiverId")
        otherUserTypingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                tvTypingIndicator.visibility = if (isTyping) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Typing status listener cancelled: ${error.message}")
            }
        })
    }
}




/*
package student.projects.universe

import android.os.Bundle
import android.util.Log // <-- Added for debugging logs
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatActivity : AppCompatActivity() { //(Aram, 2023)

    // UI components
    private lateinit var tvChatHeader: TextView
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton

    // Adapter for message display
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<MessageResponse>() //(Aram, 2023)

    // Retrofit message API
    private val messageApi = ApiClient.messageApi

    // Current user and receiver info // (Patel, 2025)
    private var currentUserId = 0
    private var receiverId = 0
    private var receiverName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) { //(Aram, 2023)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        Log.d("ChatActivity", "onCreate: Initializing ChatActivity")

        // Initialize views // (Patel, 2025)
        tvChatHeader = findViewById(R.id.tvChatHeader)
        recyclerMessages = findViewById(R.id.recyclerMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        // Retrieve IDs from API client and Intent
        currentUserId = ApiClient.currentUserId
        receiverId = intent.getIntExtra("receiverId", 0)
        receiverName = intent.getStringExtra("receiverName")

        // Validate IDs // (Patel, 2025)
        if (currentUserId == 0 || receiverId == 0) {
            Log.e("ChatActivity", "Invalid chat IDs -> currentUserId=$currentUserId, receiverId=$receiverId")
            Toast.makeText(this, "Invalid chat IDs", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvChatHeader.text = "Chat with ${receiverName ?: "Student"}"
        Log.d("ChatActivity", "Chat header set for receiver: ${receiverName ?: "Student"}") //(Aram, 2023)

        // RecyclerView setup for chat messages // (Patel, 2025)
        recyclerMessages.layoutManager = LinearLayoutManager(this)
        adapter = MessageAdapter(messages, currentUserId)
        recyclerMessages.adapter = adapter
        Log.d("ChatActivity", "RecyclerView initialized with MessageAdapter")

        // Load all messages from API
        loadMessages()

        // Handle send button click
        btnSend.setOnClickListener { //(Aram, 2023)
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                Log.d("ChatActivity", "Send button clicked with message: $text")
                sendMessage(text)
            } else {
                Log.w("ChatActivity", "Empty message attempted to send") //(Aram, 2023)
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Loads chat conversation messages from server //(Aram, 2023)
    private fun loadMessages() {
        Log.d("ChatActivity", "Loading messages between $currentUserId and $receiverId") // (Patel, 2025)

        messageApi.getConversation(currentUserId, receiverId)
            .enqueue(object : Callback<List<MessageResponse>> {
                override fun onResponse(
                    call: Call<List<MessageResponse>>,
                    response: Response<List<MessageResponse>> //(Aram, 2023)
                ) {
                    Log.d("ChatActivity", "Load messages response: ${response.code()}")
                    if (response.isSuccessful && response.body() != null) {
                        messages.clear()
                        messages.addAll(response.body()!!)
                        adapter.notifyDataSetChanged()
                        recyclerMessages.scrollToPosition(messages.size - 1)
                        Log.d("ChatActivity", "Messages loaded successfully: ${messages.size} messages") //(Aram, 2023)
                    } else {
                        Log.e("ChatActivity", "Failed to load messages. Code: ${response.code()}")
                        Toast.makeText(
                            this@ChatActivity,
                            "No messages found. Response code: ${response.code()}", //(Aram, 2023)
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<MessageResponse>>, t: Throwable) { //(Aram, 2023)
                    Log.e("ChatActivity", "Error loading messages: ${t.localizedMessage}")
                    Toast.makeText(
                        this@ChatActivity,
                        "Failed to load messages: ${t.localizedMessage}", // (Patel, 2025)
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    // Sends a new message via API // (Patel, 2025)
    private fun sendMessage(content: String) {
        Log.d("ChatActivity", "Preparing to send message: \"$content\"")

        // Create message request // (Patel, 2025)
        val msgRequest = MessageRequest(
            senderID = currentUserId,   // Sender user ID
            receiverID = receiverId,    // Receiver user ID
            content = content,          // Message text
            fileAttachment = null,      // No file attached
            readStatus = "Unread"       // Default read status
        )

        Log.d("ChatActivity", "MessageRequest created: senderID=${msgRequest.senderID}, receiverID=${msgRequest.receiverID}")

        // Send message through API // (Patel, 2025)
        messageApi.sendMessage(msgRequest)
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(
                    call: Call<MessageResponse>,
                    response: Response<MessageResponse>
                ) {
                    Log.d("ChatActivity", "Send message response code: ${response.code()}") //(Aram, 2023)
                    if (response.isSuccessful && response.body() != null) {
                        // Add new message to list and refresh UI // (Patel, 2025)
                        messages.add(response.body()!!)
                        adapter.notifyItemInserted(messages.size - 1)
                        recyclerMessages.scrollToPosition(messages.size - 1)
                        etMessage.text.clear()
                        Log.d("ChatActivity", "Message sent successfully and added to chat") //(Aram, 2023)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("ChatActivity", "Failed to send message. Code: ${response.code()}, Error: $errorBody")
                        Toast.makeText(
                            this@ChatActivity,
                            "Failed to send message. Code: ${response.code()} Body: $errorBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) { //(Aram, 2023)
                    Log.e("ChatActivity", "Error sending message: ${t.localizedMessage}")
                    Toast.makeText(
                        this@ChatActivity,
                        "Error sending message: ${t.localizedMessage}",
                        Toast.LENGTH_LONG // (Patel, 2025)
                    ).show()
                }
            })
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