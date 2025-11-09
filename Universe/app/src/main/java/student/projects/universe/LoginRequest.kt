package student.projects.universe

// (Appmaster, 2023)
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String
)

// (Appmaster, 2023)
data class GoogleLoginRequest(
    val IdToken: String
)

/*

Reference List

Appmaster. 2023. Kotlin: A Step-by-Step Guide for First-time App Developers, 13
December 2023. [Online]. Available at: https://appmaster.io/blog/kotlin-a-step-by
step-guide [Accessed 27 October 2025]

 */

