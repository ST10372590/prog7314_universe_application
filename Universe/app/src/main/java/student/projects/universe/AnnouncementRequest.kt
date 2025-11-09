package student.projects.universe

import com.google.gson.annotations.SerializedName

data class MessageRequest(
    @SerializedName("SenderID") val senderID: Int,
    @SerializedName("ReceiverID") val receiverID: Int,
    @SerializedName("Content") val content: String,
    @SerializedName("FileAttachment") val fileAttachment: String? = null,
    @SerializedName("ReadStatus") val readStatus: String = "Unread"
)


data class MessageResponse(
    val messageID: Int,
    val senderID: Int,
    val receiverID: Int,
    val content: String,
    var readStatus: String,
    val timestamp: String? = null,
    val fileAttachment: String? = null
)

data class AnnouncementRequest(
    val title: String,
    val content: String
)

data class AnnouncementResponse(
    val id: Int,
    val title: String,
    val content: String,
    val date: String
)

data class GroupMessageRequest(
    val senderID: Int,
    val content: String
)

data class GroupMessageResponse(
    val id: Int,
    val senderID: Int,
    val groupID: Int,
    val content: String,
    val sentAt: String
)

data class GroupResponse(
    val groupID: Int,
    val name: String
)

data class UserStatsResponse(
    val userID: Int,
    val points: Int,
    val streak: Int,
    val badges: List<BadgeResponse>
)

data class BadgeResponse(
    val badgeId: Int,
    val code: String,
    val name: String,
    val description: String,
    val iconUrl: String?
)

data class LeaderboardEntryResponse(
    val userID: Int,
    val username: String,
    val points: Int
)

data class GamePlayResultResponse(
    val success: Boolean,
    val pointsAwarded: Int,
    val message: String,
    val newStats: UserStatsResponse
)

data class RedeemResponse(
    val success: Boolean,
    val message: String,
    val newStats: UserStatsResponse
)

data class RewardResponse(
    val rewardId: Int,
    val title: String,
    val costPoints: Int,
    val redeemedAt: String? = null
)

