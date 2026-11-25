package com.portfolio.nbaviewer.network

import com.portfolio.nbaviewer.model.NbaPlayer
import com.portfolio.nbaviewer.model.PlayerStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object NbaApiService {

    private const val ROSTER_URL_PREFIX =
        "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams"
    private const val ATHLETE_OVERVIEW_PREFIX =
        "https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/athletes"

    // In-memory cache for fast, reactive UI
    private val rosterCache = ConcurrentHashMap<String, List<NbaPlayer>>()
    private val statsCache = ConcurrentHashMap<String, PlayerStats>()

    /**
     * Fetches the roster of players for a given ESPN team ID.
     */
    suspend fun getTeamRoster(espnTeamId: String): Result<List<NbaPlayer>> =
        withContext(Dispatchers.IO) {
            rosterCache[espnTeamId]?.let { return@withContext Result.success(it) }

            try {
                val urlString = "$ROSTER_URL_PREFIX/$espnTeamId/roster"
                val jsonText = executeGetRequest(urlString)
                val rootJson = JSONObject(jsonText)
                val athletesArray = rootJson.optJSONArray("athletes")
                    ?: return@withContext Result.success(emptyList())

                val players = mutableListOf<NbaPlayer>()
                for (i in 0 until athletesArray.length()) {
                    val athleteObj = athletesArray.getJSONObject(i)
                    val id = athleteObj.optString("id")
                    if (id.isEmpty()) continue

                    val fullName = athleteObj.optString("fullName").ifEmpty {
                        athleteObj.optString("displayName", "Unknown")
                    }
                    val displayName = athleteObj.optString("displayName").ifEmpty { fullName }
                    val jersey = athleteObj.optString("jersey").takeIf { it.isNotBlank() }
                    val posObj = athleteObj.optJSONObject("position")
                    val position = posObj?.optString("displayName") ?: "Player"
                    val posAbbr = posObj?.optString("abbreviation") ?: ""
                    val displayHeight = athleteObj.optString("displayHeight").takeIf { it.isNotBlank() }
                    val displayWeight = athleteObj.optString("displayWeight").takeIf { it.isNotBlank() }
                    val age = if (athleteObj.has("age")) athleteObj.optInt("age") else null
                    val headshotUrl = athleteObj.optJSONObject("headshot")?.optString("href")
                    val college = athleteObj.optJSONObject("college")?.optString("name")
                    val experience = athleteObj.optJSONObject("experience")?.optString("displayValue")
                    val status = athleteObj.optJSONObject("status")?.optString("name")

                    players.add(
                        NbaPlayer(
                            id = id,
                            fullName = fullName,
                            displayName = displayName,
                            jersey = jersey,
                            position = position,
                            positionAbbr = posAbbr,
                            displayHeight = displayHeight,
                            displayWeight = displayWeight,
                            age = age,
                            headshotUrl = headshotUrl,
                            college = college,
                            experience = experience,
                            status = status
                        )
                    )
                }

                // Sort players: jerseys if available, else by name
                val sortedPlayers = players.sortedWith(
                    compareBy<NbaPlayer> { it.jersey?.toIntOrNull() ?: 999 }
                        .thenBy { it.fullName }
                )

                rosterCache[espnTeamId] = sortedPlayers
                Result.success(sortedPlayers)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Fetches basic and career statistics for an athlete by their ID.
     */
    suspend fun getPlayerStats(playerId: String): Result<PlayerStats> =
        withContext(Dispatchers.IO) {
            statsCache[playerId]?.let { return@withContext Result.success(it) }

            try {
                val urlString = "$ATHLETE_OVERVIEW_PREFIX/$playerId/overview"
                val jsonText = executeGetRequest(urlString)
                val rootJson = JSONObject(jsonText)

                val statsObj = rootJson.optJSONObject("statistics")
                if (statsObj == null) {
                    val emptyStats = PlayerStats(
                        playerId = playerId,
                        regularSeason = emptyMap(),
                        career = emptyMap(),
                        labels = emptyList(),
                        labelDescriptions = emptyMap()
                    )
                    statsCache[playerId] = emptyStats
                    return@withContext Result.success(emptyStats)
                }

                val labelsArray = statsObj.optJSONArray("labels")
                val namesArray = statsObj.optJSONArray("displayNames")
                val labels = mutableListOf<String>()
                val descriptions = mutableMapOf<String, String>()

                if (labelsArray != null) {
                    for (i in 0 until labelsArray.length()) {
                        val label = labelsArray.getString(i)
                        labels.add(label)
                        val desc = namesArray?.optString(i) ?: label
                        descriptions[label] = desc
                    }
                }

                val splitsArray = statsObj.optJSONArray("splits")
                val regularSeasonMap = mutableMapOf<String, String>()
                val careerMap = mutableMapOf<String, String>()

                if (splitsArray != null) {
                    for (i in 0 until splitsArray.length()) {
                        val split = splitsArray.getJSONObject(i)
                        val displayName = split.optString("displayName")
                        val statValuesArray = split.optJSONArray("stats") ?: continue

                        val targetMap = when {
                            displayName.contains("Regular", ignoreCase = true) -> regularSeasonMap
                            displayName.contains("Career", ignoreCase = true) -> careerMap
                            else -> null
                        }

                        if (targetMap != null) {
                            for (j in 0 until statValuesArray.length()) {
                                if (j < labels.size) {
                                    val label = labels[j]
                                    targetMap[label] = statValuesArray.getString(j)
                                }
                            }
                        }
                    }
                }

                val playerStats = PlayerStats(
                    playerId = playerId,
                    regularSeason = regularSeasonMap,
                    career = careerMap,
                    labels = labels,
                    labelDescriptions = descriptions
                )

                statsCache[playerId] = playerStats
                Result.success(playerStats)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun executeGetRequest(urlString: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
        conn.setRequestProperty("Accept", "application/json")

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("HTTP error response: $responseCode")
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        conn.disconnect()
        return response.toString()
    }
}
