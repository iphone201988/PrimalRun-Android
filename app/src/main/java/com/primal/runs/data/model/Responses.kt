package com.primal.runs.data.model

import java.io.Serializable


/**************************  login & signup  api response*********************/
data class LoginResponseAPI(
    val success: Boolean, val user: LoginUser?
)

data class LoginUser(
    val _id: String?,
    val dob: String?,
    val isDeleted: Boolean?,
    val isFreeTrail: Boolean?,
    val isPremium: Boolean?,
    val isUserExists: Boolean?,
    val lat: Double?,
    val lng: Double?,
    val name: String?,
    var profileImage: String?,
    val role: Int?,
    val socialId: String?,
    val socialType: Int?,
    var unitOfMeasure: Int?,
    val token: String?,
    var gender: Int?,
    val voiceOver: Boolean?,
    val audioEffects: Int?
)


data class GetActivities(
    val data: GetActivitiesData?, val success: Boolean?
)


data class DisplayItem(
    val isHeader: Boolean, // Whether this is a month header or activity item
    val header: String? = null, // The month as a string (e.g., "December 2024")
    val activity: PreviousResult? = null // Activity data
)


data class GetActivitiesData(
    val _id: String?,
    val activities: Int?,
    val previousResults: List<PreviousResult>?,
    val totalDistance: Double?,
    val totalDuration: Int?
)

data class PreviousResult(
    val __v: Int?,
    val _id: String?,
    val averageSpeed: Double?,
    val createdAt: String?,
    val distance: Double?,
    val duration: Int?,
    val isBestScore: Boolean?,
    val planId: String?,
    val resultStatus: Int?,
    val resultType: Int?,
    val score: Int?,
    val stageId: String?,
    val updatedAt: String?,
    val userId: String?,
    val videoLink: String?
)

/**************************  get all badges api response******************/
data class GetAllBadges(
    val data: List<GetAllBadgesData>?, val success: Boolean?
)

data class GetAllBadgesData(
    val _id: String?,
    val backgroundSound: String?,
    val badgeImage: String?,
    val badgeType: Int?,
    val femaleAttackSound: String?,
    val maleAttackSound: String?,
    val startSound: String?,
    val videoUrl: String?,
    var isSelected: Boolean = false
)

data class FreeRunStages(
    val _id: String?,
    val name: String?,
    val stageImage: String?,
    var isSelected: Boolean = false
)





/**************************  update user api response******************/
data class UserUpdateResponse(
    val message: String?, val success: Boolean?, val user: UserUpdate?
)

data class UserUpdate(
    val _id: String?,
    val audioEffects: Int?,
    val dob: String?,
    val gender: Int?,
    val isFreeTrail: Boolean?,
    val isPremium: Boolean?,
    val lat: Double?,
    val lng: Double?,
    val name: String?,
    val profileImage: String?,
    val role: Int?,
    val socialId: String?,
    val unitOfMeasure: Int?,
    val voiceOver: Boolean?
)

/**************************  get my activities  api response******************/
data class GetMyActivities(
    val data: List<GetMyActivitiesData>?, val success: Boolean?
): Serializable

data class GetMyActivitiesData(
    val _id: String?, val badgeId: BadgeId?, val score: Int?, val userId: String?
)

data class BadgeId(
    val _id: String?, val badgeImage: String?
)













