package student.projects.universe

data class UserEntity(
    val userID: Int,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String,
    val unreadCount: Int = 0
)

data class ChatMessageEntity(
    val messageId: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val timestamp: String?,
    val readStatus: String,
    val fileAttachment: String? = null
)
