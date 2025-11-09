package student.projects.universe

data class FirebaseMessage(
    val messageId: Int? = null,
    val senderId: Int = 0,
    val receiverId: Int = 0,
    val content: String? = null,
    val timestamp: String? = null,
    val fileUrl: String? = null,
    val read: Boolean = false
)

