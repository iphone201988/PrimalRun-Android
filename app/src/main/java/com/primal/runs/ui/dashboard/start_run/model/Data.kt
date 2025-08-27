package com.primal.runs.ui.dashboard.start_run.model
data class DataStartModule(
    val _id: String?,
    val badgeId: BadgeId?,
    val description: String?,
    val distance: Float?,
    val durationInMin: Int?,
    val gender: Int?,
    val image: String?,
    val isPremium: Boolean?,
    val isSprint: Boolean?,
    val level: Int?,
    val planId: String?,
    val previousResults: List<PreviousResult?>?,
    val speed: Double?,
    val stageVideoLink: String?,
    val title: String?,
    val type: Int?,
    val unlockedByDefault: Boolean?,
    var measureUnits: String? = "kilometers",
)
data class BadgeId(
    val _id: String?,
    val backgroundSound: String?,
    val femaleAttackSound: String?,
    val maleAttackSound: String?,
    val startSound: String?
)








