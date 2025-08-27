package com.primal.runs.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable



data class ThisMonths(
    val title: String, val rate: Int
)

data class Achievement(
    val rate: Int
)

data class ActivitiesData(
    val title: String
)

data class OnBoardPage(val imageResId: Int)

data class FreeRunModel(
    val stageId: String?,
    val planId: String?,
    val badgeId: String?,
    val distance: Double?,
    val durationInMin: Int?,
    val gender: Int?,
    val speed: Double?,
    val pace: String? = "0:0",
    val budgeType: Int,
    val attackSound: String?,
    val backgroundSound: String?,
    val voiceOver: Boolean?,
    val startSound: String?,
    var difficultyLevel: Int?  = 1,
) :Serializable

data class SaveResultModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("resultId")
    var resultId: String?,
    @SerializedName("success")
    var success: Boolean?
)


data class SaveVideoModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("success")
    var success: Boolean?
)
