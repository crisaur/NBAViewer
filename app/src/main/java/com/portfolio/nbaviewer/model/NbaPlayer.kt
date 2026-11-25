package com.portfolio.nbaviewer.model

data class NbaPlayer(
    val id: String,
    val fullName: String,
    val displayName: String,
    val jersey: String?,
    val position: String,
    val positionAbbr: String,
    val displayHeight: String?,
    val displayWeight: String?,
    val age: Int?,
    val headshotUrl: String?,
    val college: String?,
    val experience: String?,
    val status: String?
)

data class PlayerStats(
    val playerId: String,
    val regularSeason: Map<String, String>,
    val career: Map<String, String>,
    val labels: List<String>,
    val labelDescriptions: Map<String, String>
) {
    // Quick accessors for core basic stats
    fun getPoints(): String = regularSeason["PTS"] ?: career["PTS"] ?: "-"
    fun getRebounds(): String = regularSeason["REB"] ?: career["REB"] ?: "-"
    fun getAssists(): String = regularSeason["AST"] ?: career["AST"] ?: "-"
    fun getFgPct(): String = regularSeason["FG%"] ?: career["FG%"] ?: "-"
    fun getThreePtPct(): String = regularSeason["3P%"] ?: career["3P%"] ?: "-"
    fun getFtPct(): String = regularSeason["FT%"] ?: career["FT%"] ?: "-"
    fun getGamesPlayed(): String = regularSeason["GP"] ?: career["GP"] ?: "-"
    fun getMinutes(): String = regularSeason["MIN"] ?: career["MIN"] ?: "-"
}
