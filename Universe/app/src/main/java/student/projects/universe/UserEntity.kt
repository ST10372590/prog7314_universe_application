package student.projects.universe

// (Appmaster, 2023)
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

/*

Reference List

Appmaster. 2023. Kotlin: A Step-by-Step Guide for First-time App Developers, 13
December 2023. [Online]. Available at: https://appmaster.io/blog/kotlin-a-step-by
step-guide [Accessed 27 October 2025]

 */