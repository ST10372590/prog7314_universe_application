package student.projects.universe

// (Appmaster, 2023)
data class SettingsRequest(
    val preferredLanguage: String? = null,
    val timeZone: String? = null,
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val inAppNotifications: Boolean = true,
    val profilePublic: Boolean = true,
    val shareUsageData: Boolean = false,
    val theme: String = "light",
    val itemsPerPage: Int = 10,
    // optional profile updates
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null
)

// (Appmaster, 2023)
data class SettingsResponse(
    val preferredLanguage: String?,
    val timeZone: String?,
    val emailNotifications: Boolean,
    val pushNotifications: Boolean,
    val inAppNotifications: Boolean,
    val profilePublic: Boolean,
    val shareUsageData: Boolean,
    val theme: String,
    val itemsPerPage: Int,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?
)

/*

Reference List

Appmaster. 2023. Kotlin: A Step-by-Step Guide for First-time App Developers, 13
December 2023. [Online]. Available at: https://appmaster.io/blog/kotlin-a-step-by
step-guide [Accessed 27 October 2025]

 */