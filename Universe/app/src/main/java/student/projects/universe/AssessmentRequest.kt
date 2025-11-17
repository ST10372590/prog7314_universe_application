package student.projects.universe

import com.google.gson.annotations.SerializedName

// (Appmaster, 2023)
data class AssessmentRequest(
    val title: String,
    val description: String,
    val dueDate: String, // ISO 8601 format "yyyy-MM-dd'T'HH:mm:ss"
    val maxMarks: Int,
    val courseID: String
)

// (Appmaster, 2023)
data class AssessmentResponse(
    val assessmentID: Int,
    val courseID: String,
    val moduleID: String,        // NEW: added module ID
    val title: String,
    val description: String,
    val dueDate: String,
    val maxMarks: Int,
    val fileUrl: String? // Nullable because assessment may not have a file yet
)

// Local model used in RecyclerView
data class Assessment(
    val assessmentID: Int,
    val title: String,
    val description: String,
    val dueDate: String,
    val maxMarks: Int,
    val fileUrl: String? = null
)

data class ClassFileResponse(
    val fileID: Int,
    val uploaderID: Int,
    val fileName: String,
    val fileType: String,
    val filePath: String,
    val uploadDate: String,  // ISO 8601 date string
    val uploaderName: String? = null  // Optional: uploader display name
)
// (Appmaster, 2023)
data class ClassFileRequest(
    val uploaderID: Int,
    val fileName: String,
    val fileType: String,
    val filePath: String,
    val uploadDate: String
)

data class ModuleResponse(
    val moduleID: String,
    val courseID: String,
    val moduleTitle: String,
    val contentType: String,
    val contentLink: String,
    val completionStatus: String,
    val hasNewAssessment: Boolean
)

data class ApiResponse(
    val message: String
)

data class NotificationResponse(
    val notificationID: Int,
    val userID: Int,
    val type: String,
    val message: String,
    val priority: String,
    val status: String,
    val userName: String? // optional for display
)
 // (Appmaster, 2023)
data class CourseResponse(
    val courseID: String,
    val courseTitle: String,
    val courseDescription: String,
    val credits: Int,
    val startDate: String,
    val endDate: String,
    val lecturerID: Int,
    val isActive: Boolean? = true,
    val isEnrolled: Boolean? = false,
    val studentCount: Int? = 0,
    val moduleCount: Int? = 0,
    val modules: List<ModuleResponse> = emptyList(),
    val assessments: List<AssessmentResponse> = emptyList()
)

data class SubmissionResponse(
    val submissionID: Int,
    val assessmentID: Int,
    val assessmentTitle: String,
    val studentFirst: String?,
    val studentLast: String?,
    @SerializedName("studentFullName") val studentName: String?,
    val fileLink: String?,
    val submittedAt: String,
    val grade: Double,
    val feedback: String?,
    val maxMarks: Double? = 100.0,
    val gradedBy: String? = "Instructor"
)


/*

Reference List

Appmaster. 2023. Kotlin: A Step-by-Step Guide for First-time App Developers, 13
December 2023. [Online]. Available at: https://appmaster.io/blog/kotlin-a-step-by
step-guide [Accessed 27 October 2025]

 */