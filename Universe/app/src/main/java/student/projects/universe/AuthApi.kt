package student.projects.universe

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// ==============================
// Authentication-related endpoints // (Appmaster, 2023)
// ==============================
interface AuthApi {

    // Login with standard credentials (email/username + password)
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // Register a new user (student, lecturer, etc.)
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<Void>

    // Login using Google SSO (OAuth)
    @POST("auth/login-with-google")
    fun loginWithGoogle(@Body request: GoogleLoginRequest): Call<LoginResponse>
}

// ==============================
// User management endpoints (Aram, 2023)
// ==============================
interface UserApi {

    // ✅ Fetch all users (requires admin or authorized access)
    @GET("users")
    fun getAllUsers(): Call<List<UserResponse>>

    // ✅ Fetch details of the currently logged-in user
    @GET("users/me")
    fun getCurrentUser(): Call<UserResponse>

    // ✅ Retrieve a list of all registered students
    @GET("users/students")
    fun getAllStudents(): Call<List<UserResponse>>
}


// ==============================
// Course management endpoints (Aram, 2023)
// ==============================
interface CourseApi {

    // Retrieve all available courses
    @GET("courses")
    fun getAllCourses(): Call<List<CourseResponse>>

    // Retrieve courses the current user is enrolled in (Appmaster, 2023)
    @GET("courses/enrolled")
    fun getEnrolledCourses(): Call<List<CourseResponse>>

    // Enroll the current user in a specific course
    @POST("courses/{courseId}/enroll")
    fun enrollCourse(@Path("courseId") courseId: String): Call<Void>
}

// ==============================
// Messaging endpoints (Aram, 2023)
// ==============================
interface MessageApi {

    // Get a conversation between two users (sender and receiver)
    @GET("messages/conversation/{senderId}/{receiverId}")
    fun getConversation(
        @Path("senderId") senderId: Int,
        @Path("receiverId") receiverId: Int
    ): Call<List<MessageResponse>>

    // Send a message to another user (Appmaster, 2023)
    @POST("messages")
    fun sendMessage(@Body message: MessageRequest): Call<MessageResponse>

    // Mark a specific message as read (Appmaster, 2023)
    @PUT("messages/{id}/read")
    fun markAsRead(@Path("id") id: Int): Call<Void>
}

// ==============================
// Module endpoints (linked to courses) (Aram, 2023)
// ==============================
interface ModuleApi {

    // Get all modules under a given course
    @GET("modules/byCourse/{courseId}")
    fun getModulesByCourse(@Path("courseId") courseId: String): Call<List<ModuleResponse>>
}

// ==============================
// Assessment (Assignments, Tests) endpoints
// ==============================
interface AssessmentApi {

    @Multipart
    @POST("assessments/create")
    fun createAssessment(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("dueDate") dueDate: RequestBody,
        @Part("maxMarks") maxMarks: RequestBody,
        @Part("courseID") courseID: RequestBody,
        @Part("moduleID") moduleID: RequestBody, // added moduleId
        @Part file: MultipartBody.Part? // optional
    ): Call<AssessmentResponse>

    @GET("assessments/course/{courseId}")
    fun getAssessmentsByCourse(@Path("courseId") courseId: String): Call<List<AssessmentResponse>>

    @GET("assessments/module/{moduleId}")
    fun getAssessmentsByModule(@Path("moduleId") moduleId: String): Call<List<AssessmentResponse>>


}

interface StudentApi {

    @Multipart
    @POST("submissions/submit")
    fun submitAssessment(
        @Part("AssessmentID") assessmentId: RequestBody,
        @Part("UserID") userId: RequestBody,
        @Part file: MultipartBody.Part?
    ): Call<Void>

    @GET("submissions/user/{userId}")
    fun getUserSubmissions(@Path("userId") userId: Int): Call<List<SubmissionResponse>>

    @GET("assessments/{assessmentId}")
    fun getAssessmentById(@Path("assessmentId") assessmentId: Int): Call<AssessmentResponse>
}

interface LecturerApi {

    // Get all submissions for a given assessment
    @GET("submissions/assessment/{assessmentId}")
    fun getSubmissionsByAssessment(
        @Path("assessmentId") assessmentId: Int
    ): Call<List<SubmissionResponse>>

    // Update feedback and grade for a specific submission
    @FormUrlEncoded
    @PUT("submissions/{submissionId}/feedback")
    fun updateSubmissionFeedback(
        @Path("submissionId") submissionId: Int,
        @Field("grade") grade: String,
        @Field("feedback") feedback: String
    ): Call<ApiResponse>
}


// ==============================
// Announcement endpoints (Aram, 2023)
// ==============================
interface AnnouncementApi {

    // Retrieve all announcements visible to the current user (Appmaster, 2023)
    @GET("announcements")
    fun getAnnouncements(): Call<List<AnnouncementResponse>>

    // Create a new announcement (lecturer or coordinator)
    @POST("announcements")
    fun createAnnouncement(@Body announcement: AnnouncementRequest): Call<AnnouncementResponse>
}

// ==============================
// Group chat and collaboration endpoints (Appmaster, 2023)
// ==============================
interface GroupApi {

    // Retrieve all existing discussion groups
    @GET("groups")
    fun getGroups(): Call<List<GroupResponse>>

    // Get messages within a specific group
    @GET("groups/{groupId}/messages")
    fun getGroupMessages(@Path("groupId") groupId: Int): Call<List<GroupMessageResponse>>

    // Send a message to a group
    @POST("groups/{groupId}/messages")
    fun sendGroupMessage(
        @Path("groupId") groupId: Int,
        @Body message: GroupMessageRequest
    ): Call<GroupMessageResponse>
}

// ==============================
// File upload and download endpoints (Appmaster, 2023)
// ==============================
interface FileApi {

    // Upload a class, assignment, or study file
    @POST("files/upload")
    fun uploadFile(@Body fileRequest: ClassFileRequest): Call<ApiResponse>

    // Retrieve a file by its unique ID
    @GET("files/{id}")
    fun getFileById(@Path("id") id: Int): Call<ClassFileResponse>

    // Retrieve all files uploaded by a specific user
    @GET("files/uploader/{userId}")
    fun getFilesByUploader(@Path("userId") userId: Int): Call<List<ClassFileResponse>>
}

// ==============================
// Notification endpoints (Appmaster, 2023)
// ==============================
interface NotificationApi {

    // Retrieve all notifications for the logged-in user
    @GET("notifications")
    fun getNotifications(): Call<List<NotificationResponse>>
}

// ==============================
// User Settings endpoints (Appmaster, 2023)
// ==============================
interface SettingsApi {

    // Get personal settings for the logged-in user
    @GET("settings/me")
    fun getMySettings(): Call<SettingsResponse>

    // Update current user settings
    @PUT("settings/me")
    fun updateMySettings(@Body request: SettingsRequest): Call<Void>
}

// ==============================
// Gamification & Leaderboard endpoints (Aram, 2023)
// ==============================
interface GamificationApi {

    // Existing endpoints
    @GET("api/gamification/rewards")
    fun getAvailableRewards(): Call<List<RewardResponse>>

    // Retrieve gamification statistics for a user (Aram, 2023)
    @GET("api/gamification/user/{userId}")
    fun getUserStats(@Path("userId") userId: Int): Call<UserStatsResponse>

    // Allow a user to play a mini-game
    @POST("api/gamification/user/{userId}/play")
    fun playMiniGame(@Path("userId") userId: Int): Call<GamePlayResultResponse>

    // Add reward or activity points to a user (Aram, 2023)
    @POST("api/gamification/user/{userId}/addpoints")
    fun addPoints(@Path("userId") userId: Int, @Query("points") points: Int): Call<UserStatsResponse>

    // Retrieve the leaderboard of top users
    @GET("api/gamification/leaderboard")
    fun getLeaderboard(): Call<List<LeaderboardEntryResponse>>

    // Redeem a specific reward using a reward ID
    @POST("api/gamification/user/{userId}/redeem/{rewardId}")
    fun redeemReward(@Path("userId") userId: Int, @Path("rewardId") rewardId: Int): Call<RedeemResponse>
}


/*

Reference List

Appmaster. 2023. Kotlin: A Step-by-Step Guide for First-time App Developers, 13
December 2023. [Online]. Available at: https://appmaster.io/blog/kotlin-a-step-by
step-guide [Accessed 27 October 2025]

Aram. 2023. Refit – The Retrofit of .NET, 27 September 2023, Codingsonata.
[Blog]. Available at:  https://codingsonata.com/refit-the-retrofit-of-net/ [Accessed 22 October 2025]

 */