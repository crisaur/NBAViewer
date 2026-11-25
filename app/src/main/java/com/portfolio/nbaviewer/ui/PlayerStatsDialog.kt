package com.portfolio.nbaviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.portfolio.nbaviewer.model.NbaPlayer
import com.portfolio.nbaviewer.model.NbaTeam
import com.portfolio.nbaviewer.model.PlayerStats
import com.portfolio.nbaviewer.network.NbaApiService

@Composable
fun PlayerStatsDialog(
    player: NbaPlayer,
    team: NbaTeam,
    uiState: PlayerStatsUiState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Banner with Team Color
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(team.primaryColorHex))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Headshot or fallback circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!player.headshotUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = player.headshotUrl,
                                    contentDescription = player.fullName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = player.jersey?.let { "#$it" } ?: player.positionAbbr,
                                    color = Color(team.primaryColorHex),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val bioDetails = listOfNotNull(
                                player.jersey?.let { "#$it" },
                                player.position.ifEmpty { null },
                                player.displayHeight
                            ).joinToString(" • ")

                            Text(
                                text = bioDetails,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = team.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Player Bio Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BioItem(label = "Age", value = player.age?.toString() ?: "-")
                        BioItem(label = "Weight", value = player.displayWeight ?: "-")
                        BioItem(label = "Exp", value = player.experience ?: "-")
                        BioItem(label = "College", value = player.college ?: "-")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "BASIC STATISTICS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    when (uiState) {
                        is PlayerStatsUiState.Idle,
                        is PlayerStatsUiState.Loading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = Color(team.primaryColorHex)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Connecting to ESPN API for stats...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is PlayerStatsUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        is PlayerStatsUiState.Success -> {
                            val stats = uiState.stats

                            // KPI Highlights Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatHighlightCard(
                                    label = "PTS",
                                    value = stats.getPoints(),
                                    modifier = Modifier.weight(1f),
                                    highlightColor = Color(team.primaryColorHex)
                                )
                                StatHighlightCard(
                                    label = "REB",
                                    value = stats.getRebounds(),
                                    modifier = Modifier.weight(1f),
                                    highlightColor = Color(team.primaryColorHex)
                                )
                                StatHighlightCard(
                                    label = "AST",
                                    value = stats.getAssists(),
                                    modifier = Modifier.weight(1f),
                                    highlightColor = Color(team.primaryColorHex)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatHighlightCard(
                                    label = "FG%",
                                    value = "${stats.getFgPct()}%",
                                    modifier = Modifier.weight(1f),
                                    highlightColor = MaterialTheme.colorScheme.secondary
                                )
                                StatHighlightCard(
                                    label = "3P%",
                                    value = "${stats.getThreePtPct()}%",
                                    modifier = Modifier.weight(1f),
                                    highlightColor = MaterialTheme.colorScheme.secondary
                                )
                                StatHighlightCard(
                                    label = "FT%",
                                    value = "${stats.getFtPct()}%",
                                    modifier = Modifier.weight(1f),
                                    highlightColor = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Full Stats Table
                            Text(
                                text = "REGULAR SEASON VS CAREER TABLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                // Table Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "STAT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    Text(
                                        text = "SEASON",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "CAREER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 1.dp
                                )

                                val displayMetrics = listOf(
                                    "GP" to "Games Played",
                                    "MIN" to "Minutes / Game",
                                    "PTS" to "Points / Game",
                                    "REB" to "Rebounds / Game",
                                    "AST" to "Assists / Game",
                                    "STL" to "Steals / Game",
                                    "BLK" to "Blocks / Game",
                                    "FG%" to "Field Goal %",
                                    "3P%" to "3-Point %",
                                    "FT%" to "Free Throw %",
                                    "TO" to "Turnovers / Game"
                                )

                                displayMetrics.forEachIndexed { index, (label, description) ->
                                    val seasonVal = stats.regularSeason[label] ?: "-"
                                    val careerVal = stats.career[label] ?: "-"
                                    val rowBg = if (index % 2 == 0) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(rowBg)
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 9.sp
                                            )
                                        }
                                        Text(
                                            text = seasonVal,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = careerVal,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.End,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (index < displayMetrics.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(team.primaryColorHex)
                        )
                    ) {
                        Text(text = "Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun BioItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatHighlightCard(
    label: String,
    value: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = highlightColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, highlightColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = highlightColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PlayerStatsDialogPreview() {
    val samplePlayer = NbaPlayer(
        id = "4065648",
        fullName = "Jayson Tatum",
        displayName = "Jayson Tatum",
        jersey = "0",
        position = "Small Forward",
        positionAbbr = "SF",
        displayHeight = "6' 8\"",
        displayWeight = "210 lbs",
        age = 26,
        headshotUrl = null,
        college = "Duke",
        experience = "8th Season",
        status = "Active"
    )
    val sampleTeam = com.portfolio.nbaviewer.model.NbaTeamsData.teams.first()
    val sampleStats = PlayerStats(
        playerId = "4065648",
        regularSeason = mapOf("PTS" to "26.4", "REB" to "6.9", "AST" to "6.7", "FG%" to "42.6", "3P%" to "33.6", "FT%" to "86.6", "GP" to "15", "MIN" to "34.3"),
        career = mapOf("PTS" to "23.6", "REB" to "7.4", "AST" to "3.9", "FG%" to "45.7", "3P%" to "36.8", "FT%" to "84.0", "GP" to "616", "MIN" to "34.5"),
        labels = listOf("GP", "MIN", "FG%", "3P%", "FT%", "REB", "AST", "PTS"),
        labelDescriptions = emptyMap()
    )

    com.portfolio.nbaviewer.ui.theme.NBAViewerTheme {
        PlayerStatsDialog(
            player = samplePlayer,
            team = sampleTeam,
            uiState = PlayerStatsUiState.Success(sampleStats),
            onDismiss = {}
        )
    }
}

