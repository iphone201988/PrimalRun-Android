package com.primal.runs.ui.dashboard.home_details.model

import com.google.gson.annotations.SerializedName

data class PlanDetailsModel(
    @SerializedName("data")
    val `data`: PlanDetailData?,
    val success: Boolean?
)

data class PlanDetailData(
    val _id: String?,
    val description: String?,
    val distancePlan: Float?,
    val easyStages: List<EasyStage?>?,
    val hardStages: List<EasyStage?>?,
    val image: String?,
    val isPremium: Boolean?,
    val normalStages: List<EasyStage?>?,
    val progress: Int?,
    val title: String?,
    @SerializedName("stageType")
    val stageType: Int?,
)




data class EasyStage(
    val _id: String?,
    val badgeId: String?,
    val distance: Double?,
    val image: String?,
    val isPremium: Boolean?,
    val isSprint: Boolean?,
    val level: Int?,
    val speed: Double?,
    val sprintCount: Int?,
    val sprintDistanceInMeter: Int?,
    val title: String?,
    val durationInMin: String?,
    @SerializedName("isUnlocked")
    var stageLock: Boolean?,
    @SerializedName("difficultyLevel")
    var difficultyLevel: Int? = 1,
    var measureUnits: String? = "km",
)