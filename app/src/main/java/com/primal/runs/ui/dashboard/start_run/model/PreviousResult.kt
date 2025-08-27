package com.primal.runs.ui.dashboard.start_run.model

data class PreviousResult(
    val _id: String?,
    val createdAt: String?,
    val distance: Double?,
    val duration: Double?,
    val score: Int?,
    val averageSpeed: Double,
    var measureUnits: String? = "km",
)