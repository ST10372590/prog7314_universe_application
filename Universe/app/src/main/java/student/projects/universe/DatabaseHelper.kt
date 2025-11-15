package student.projects.universe

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "universe.db"
        private const val DB_VERSION = 1

        // Tables
        private const val TABLE_USERS = "users"
        private const val TABLE_MESSAGES = "chat_messages"

        // USERS columns
        private const val USER_ID = "userID"
        private const val FIRST_NAME = "firstName"
        private const val LAST_NAME = "lastName"
        private const val USERNAME = "username"
        private const val ROLE = "role"
        private const val UNREAD_COUNT = "unreadCount"

        // MESSAGES columns
        private const val MESSAGE_ID = "messageId"
        private const val SENDER_ID = "senderId"
        private const val RECEIVER_ID = "receiverId"
        private const val CONTENT = "content"
        private const val TIMESTAMP = "timestamp"
        private const val READ_STATUS = "readStatus"
        private const val FILE_ATTACHMENT = "fileAttachment"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $USER_ID INTEGER PRIMARY KEY,
                $FIRST_NAME TEXT NOT NULL,
                $LAST_NAME TEXT NOT NULL,
                $USERNAME TEXT NOT NULL,
                $ROLE TEXT NOT NULL,
                $UNREAD_COUNT INTEGER DEFAULT 0
            );
        """.trimIndent()

        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $MESSAGE_ID INTEGER PRIMARY KEY,
                $SENDER_ID INTEGER NOT NULL,
                $RECEIVER_ID INTEGER NOT NULL,
                $CONTENT TEXT NOT NULL,
                $TIMESTAMP TEXT NOT NULL,
                $READ_STATUS TEXT NOT NULL,
                $FILE_ATTACHMENT TEXT
            );
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createMessagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // For now, simple drop and recreate on upgrade
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    // --- USER METHODS ---

    fun insertOrUpdateUser(user: UserEntity): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(USER_ID, user.userID)
            put(FIRST_NAME, user.firstName)
            put(LAST_NAME, user.lastName)
            put(USERNAME, user.username)
            put(ROLE, user.role)
            put(UNREAD_COUNT, user.unreadCount)
        }
        val id = db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return id != -1L
    }

    fun insertOrUpdateUsers(users: List<UserEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            users.forEach { insertOrUpdateUser(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getUsersByRole(role: String): List<UserEntity> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$ROLE = ?",
            arrayOf(role),
            null, null,
            "$FIRST_NAME ASC"
        )
        return cursorToUserList(cursor)
    }

    fun searchUsers(query: String): List<UserEntity> {
        val db = readableDatabase
        val likeQuery = "%$query%"
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$FIRST_NAME LIKE ? OR $LAST_NAME LIKE ? OR $USERNAME LIKE ?",
            arrayOf(likeQuery, likeQuery, likeQuery),
            null, null,
            "$FIRST_NAME ASC"
        )
        return cursorToUserList(cursor)
    }

    fun getAllUsers(): List<UserEntity> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null, // all columns
            null, // no WHERE clause, so get all rows
            null,
            null,
            null,
            "$FIRST_NAME ASC"
        )
        return cursorToUserList(cursor)
    }


    fun updateUnreadCount(userId: Int, count: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply { put(UNREAD_COUNT, count) }
        val rows = db.update(TABLE_USERS, values, "$USER_ID = ?", arrayOf(userId.toString()))
        return rows > 0
    }

    private fun cursorToUserList(cursor: Cursor): List<UserEntity> {
        val users = mutableListOf<UserEntity>()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val user = UserEntity(
                        userID = it.getInt(it.getColumnIndexOrThrow(USER_ID)),
                        firstName = it.getString(it.getColumnIndexOrThrow(FIRST_NAME)),
                        lastName = it.getString(it.getColumnIndexOrThrow(LAST_NAME)),
                        username = it.getString(it.getColumnIndexOrThrow(USERNAME)),
                        role = it.getString(it.getColumnIndexOrThrow(ROLE)),
                        unreadCount = it.getInt(it.getColumnIndexOrThrow(UNREAD_COUNT))
                    )
                    users.add(user)
                } while (it.moveToNext())
            }
        }
        return users
    }

    // --- MESSAGE METHODS ---

    fun insertOrUpdateMessage(message: ChatMessageEntity): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(MESSAGE_ID, message.messageId)
            put(SENDER_ID, message.senderId)
            put(RECEIVER_ID, message.receiverId)
            put(CONTENT, message.content)
            put(TIMESTAMP, message.timestamp)
            put(READ_STATUS, message.readStatus)
            put(FILE_ATTACHMENT, message.fileAttachment)
        }
        val id = db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return id != -1L
    }

    fun insertOrUpdateMessages(messages: List<ChatMessageEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            messages.forEach { insertOrUpdateMessage(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getConversationMessages(userId: Int, otherId: Int): List<ChatMessageEntity> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "($SENDER_ID = ? AND $RECEIVER_ID = ?) OR ($SENDER_ID = ? AND $RECEIVER_ID = ?)",
            arrayOf(userId.toString(), otherId.toString(), otherId.toString(), userId.toString()),
            null, null,
            "$TIMESTAMP ASC"
        )
        return cursorToMessageList(cursor)
    }

    fun updateMessageWithServerResponse(localMessageId: Int, updatedMessage: MessageResponse): Boolean {
        val db = writableDatabase

        val timestamp = updatedMessage.timestamp?.takeIf { it.isNotBlank() } ?: isoNow() // fallback to current time

        val values = ContentValues().apply {
            put(MESSAGE_ID, updatedMessage.messageID)
            put(SENDER_ID, updatedMessage.senderID)
            put(RECEIVER_ID, updatedMessage.receiverID)
            put(CONTENT, updatedMessage.content)
            put(TIMESTAMP, timestamp)  // use non-null timestamp here
            put(READ_STATUS, updatedMessage.readStatus)
            put(FILE_ATTACHMENT, updatedMessage.fileAttachment)
        }

        val rowsUpdated = db.update(
            TABLE_MESSAGES,
            values,
            "$MESSAGE_ID = ?",
            arrayOf(localMessageId.toString())
        )
        return rowsUpdated > 0
    }

    private fun isoNow(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    fun deleteMessageById(messageId: Int): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_MESSAGES, "$MESSAGE_ID = ?", arrayOf(messageId.toString()))
        return rows > 0
    }

    fun deleteConversation(userId: Int, otherId: Int): Boolean {
        val db = writableDatabase
        val rows = db.delete(
            TABLE_MESSAGES,
            "($SENDER_ID = ? AND $RECEIVER_ID = ?) OR ($SENDER_ID = ? AND $RECEIVER_ID = ?)",
            arrayOf(userId.toString(), otherId.toString(), otherId.toString(), userId.toString())
        )
        return rows > 0
    }

    private fun cursorToMessageList(cursor: Cursor): List<ChatMessageEntity> {
        val messages = mutableListOf<ChatMessageEntity>()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val message = ChatMessageEntity(
                        messageId = it.getInt(it.getColumnIndexOrThrow(MESSAGE_ID)),
                        senderId = it.getInt(it.getColumnIndexOrThrow(SENDER_ID)),
                        receiverId = it.getInt(it.getColumnIndexOrThrow(RECEIVER_ID)),
                        content = it.getString(it.getColumnIndexOrThrow(CONTENT)),
                        timestamp = it.getString(it.getColumnIndexOrThrow(TIMESTAMP)),
                        readStatus = it.getString(it.getColumnIndexOrThrow(READ_STATUS)),
                        fileAttachment = it.getString(it.getColumnIndexOrThrow(FILE_ATTACHMENT))
                    )
                    messages.add(message)
                } while (it.moveToNext())
            }
        }
        return messages
    }
}
