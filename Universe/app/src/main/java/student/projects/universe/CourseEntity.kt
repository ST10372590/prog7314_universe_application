// CourseEntity.kt
package student.projects.universe

data class CourseEntity(
    val courseId: String,
    val title: String,
    val description: String,
    val credits: Int,
    val createdAt: String
)

data class ModuleEntity(
    val moduleId: String,
    val title: String,
    val description: String,
    val courseId: String,
    val createdAt: String
)