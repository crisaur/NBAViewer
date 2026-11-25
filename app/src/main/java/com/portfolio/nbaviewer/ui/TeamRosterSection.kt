package com.portfolio.nbaviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.portfolio.nbaviewer.model.NbaPlayer
import com.portfolio.nbaviewer.model.NbaTeam
import com.portfolio.nbaviewer.network.NbaApiService

@Composable
fun TeamRosterSection(
    team: NbaTeam,
    rosterState: RosterUiState?,
    onPlayerClick: (NbaPlayer) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val players = (rosterState as? RosterUiState.Success)?.players

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ACTIVE ROSTER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                if (players != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(team.primaryColorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${players.size} Players",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(team.primaryColorHex),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "Tap player for stats 📊",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (rosterState) {
            null,
            is RosterUiState.Loading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(team.primaryColorHex)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Connecting to ESPN API for ${team.name} roster...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is RosterUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to load roster: ${rosterState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(team.primaryColorHex)
                            )
                        ) {
                            Text(text = "Retry", color = Color.White)
                        }
                    }
                }
            }
            is RosterUiState.Success -> {
                if (players.isNullOrEmpty()) {
                    Text(
                        text = "No players currently listed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        players.forEach { player ->
                            PlayerCardItem(
                                player = player,
                                team = team,
                                onClick = { onPlayerClick(player) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerCardItem(
    player: NbaPlayer,
    team: NbaTeam,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Headshot / Jersey circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(team.primaryColorHex).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (!player.headshotUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = player.headshotUrl,
                        contentDescription = player.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = player.jersey?.let { "#$it" } ?: (player.positionAbbr.ifEmpty { "•" }),
                        color = Color(team.primaryColorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Player Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (player.jersey != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "#${player.jersey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val subInfo = listOfNotNull(
                    player.position.ifEmpty { null },
                    player.displayHeight,
                    player.displayWeight
                ).joinToString(" • ")

                Text(
                    text = subInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Stats 📊",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PlayerCardItemPreview() {
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

    com.portfolio.nbaviewer.ui.theme.NBAViewerTheme {
        PlayerCardItem(
            player = samplePlayer,
            team = sampleTeam,
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

