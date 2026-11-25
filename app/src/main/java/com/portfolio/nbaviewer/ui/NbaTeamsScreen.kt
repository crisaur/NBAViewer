package com.portfolio.nbaviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portfolio.nbaviewer.model.Conference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NbaTeamsScreen(
    modifier: Modifier = Modifier,
    viewModel: NbaViewModel = viewModel()
) {
    val allTeams = viewModel.allTeams
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    val currentTab by viewModel.viewMode.collectAsState()
    val selectedTeamDetailTab by viewModel.selectedTeamDetailTab.collectAsState()
    val selectedConferenceFilter by viewModel.conferenceFilter.collectAsState()
    val filteredTeams by viewModel.filteredTeams.collectAsState()
    val expandedTeamIds by viewModel.expandedTeamIds.collectAsState()
    val rostersByTeam by viewModel.rostersByTeam.collectAsState()
    val selectedPlayerForStats by viewModel.selectedPlayerForStats.collectAsState()
    val playerStatsUiState by viewModel.playerStatsUiState.collectAsState()

    // Player stats dialog modal when a player is tapped
    if (selectedPlayerForStats != null) {
        val (player, team) = selectedPlayerForStats!!
        PlayerStatsDialog(
            player = player,
            team = team,
            uiState = playerStatsUiState,
            onDismiss = { viewModel.dismissPlayerStats() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🏀 NBA Teams Viewer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        // View Mode Switcher Tab Row
        PrimaryTabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            ViewMode.entries.forEach { mode ->
                Tab(
                    selected = currentTab == mode,
                    onClick = { viewModel.setViewMode(mode) },
                    text = {
                        Text(
                            text = mode.title,
                            fontWeight = if (currentTab == mode) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        when (currentTab) {
            ViewMode.DROPDOWN_SELECTOR -> {
                // Dropdown selector mode: User picks any team from a dropdown menu,
                // and a detailed table of that team's data or live roster is displayed.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Choose an NBA team from the dropdown below to view specifications and live roster stats:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NbaTeamDropdownSelector(
                        teams = allTeams,
                        selectedTeam = selectedTeam,
                        onTeamSelected = { viewModel.selectTeam(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selected Team Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(selectedTeam.primaryColorHex)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedTeam.abbreviation,
                                    color = Color(selectedTeam.primaryColorHex),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = selectedTeam.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${selectedTeam.city} • ${selectedTeam.conference.displayName} Conference",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sub-Tabs: Specifications Table vs Live Roster (API)
                    SecondaryTabRow(
                        selectedTabIndex = selectedTeamDetailTab,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Tab(
                            selected = selectedTeamDetailTab == 0,
                            onClick = { viewModel.setSelectedTeamDetailTab(0) },
                            text = {
                                Text(
                                    text = "📋 Specs Table",
                                    fontWeight = if (selectedTeamDetailTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTeamDetailTab == 1,
                            onClick = { viewModel.setSelectedTeamDetailTab(1) },
                            text = {
                                Text(
                                    text = "👥 Roster & Stats (API)",
                                    fontWeight = if (selectedTeamDetailTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (selectedTeamDetailTab) {
                        0 -> {
                            // Team Specifications Table
                            NbaTeamDataTable(team = selectedTeam)
                        }
                        1 -> {
                            // Live Roster from API with clickable players for stats
                            TeamRosterSection(
                                team = selectedTeam,
                                rosterState = rostersByTeam[selectedTeam.id],
                                onPlayerClick = { player ->
                                    viewModel.selectPlayerForStats(player, selectedTeam)
                                },
                                onRetry = {
                                    viewModel.loadRosterForTeam(selectedTeam)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            ViewMode.EXPANDABLE_TABLE -> {
                // Expandable Table mode: Each team has a dropdown accordion row in the table
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column {
                            // Filter Chips Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = selectedConferenceFilter == null,
                                    onClick = { viewModel.setConferenceFilter(null) },
                                    label = { Text("All (${allTeams.size})") },
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                                FilterChip(
                                    selected = selectedConferenceFilter == Conference.EASTERN,
                                    onClick = { viewModel.setConferenceFilter(Conference.EASTERN) },
                                    label = { Text("Eastern") }
                                )
                                FilterChip(
                                    selected = selectedConferenceFilter == Conference.WESTERN,
                                    onClick = { viewModel.setConferenceFilter(Conference.WESTERN) },
                                    label = { Text("Western") }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Controls Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap any row to dropdown/expand details",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val isAllExpanded = filteredTeams.isNotEmpty() &&
                                        filteredTeams.all { it.id in expandedTeamIds }

                                TextButton(
                                    onClick = { viewModel.toggleAllExpanded() }
                                ) {
                                    Text(
                                        text = if (isAllExpanded) "Collapse All" else "Expand All",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    items(filteredTeams, key = { it.id }) { team ->
                        val isExpanded = team.id in expandedTeamIds
                        NbaTeamExpandableRow(
                            team = team,
                            isExpanded = isExpanded,
                            rosterState = rostersByTeam[team.id],
                            onRetryRoster = { viewModel.loadRosterForTeam(team) },
                            onToggleExpand = { viewModel.toggleTeamExpanded(team) },
                            onPlayerClick = { player, t ->
                                viewModel.selectPlayerForStats(player, t)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun NbaTeamsScreenTablePreview() {
    com.portfolio.nbaviewer.ui.theme.NBAViewerTheme {
        NbaTeamsScreen()
    }
}
