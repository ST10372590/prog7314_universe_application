package student.projects.universe

// (Appmaster, 2023)
data class UserResponse(
    val userID: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val unreadCount: Int = 0
)

/*

Reference List

Appmaster. 2023. Kotlin: A Step-by-Step Guide for First-time App Developers, 13
December 2023. [Online]. Available at: https://appmaster.io/blog/kotlin-a-step-by
step-guide [Accessed 27 October 2025]

 */
