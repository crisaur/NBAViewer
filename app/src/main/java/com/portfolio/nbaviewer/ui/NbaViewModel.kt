package com.portfolio.nbaviewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.nbaviewer.model.Conference
import com.portfolio.nbaviewer.model.NbaPlayer
import com.portfolio.nbaviewer.model.NbaTeam
import com.portfolio.nbaviewer.model.NbaTeamsData
import com.portfolio.nbaviewer.model.PlayerStats
import com.portfolio.nbaviewer.network.NbaApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RosterUiState {
    data object Loading : RosterUiState
    data class Success(val players: List<NbaPlayer>) : RosterUiState
    data class Error(val message: String) : RosterUiState
}

sealed interface PlayerStatsUiState {
    data object Idle : PlayerStatsUiState
    data object Loading : PlayerStatsUiState
    data class Success(val stats: PlayerStats) : PlayerStatsUiState
    data class Error(val message: String) : PlayerStatsUiState
}

enum class ViewMode(val title: String) {
    DROPDOWN_SELECTOR("Select Team Dropdown"),
    EXPANDABLE_TABLE("All Teams Table")
}

class NbaViewModel(
    private val apiService: NbaApiService = NbaApiService
) : ViewModel() {

    val allTeams: List<NbaTeam> = NbaTeamsData.teams

    private val _selectedTeam = MutableStateFlow(allTeams.first())
    val selectedTeam: StateFlow<NbaTeam> = _selectedTeam.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.DROPDOWN_SELECTOR)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _selectedTeamDetailTab = MutableStateFlow(0) // 0: Specs Table, 1: Roster
    val selectedTeamDetailTab: StateFlow<Int> = _selectedTeamDetailTab.asStateFlow()

    private val _conferenceFilter = MutableStateFlow<Conference?>(null)
    val conferenceFilter: StateFlow<Conference?> = _conferenceFilter.asStateFlow()

    private val _expandedTeamIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedTeamIds: StateFlow<Set<String>> = _expandedTeamIds.asStateFlow()

    // Filtered teams based on conference filter
    val filteredTeams: StateFlow<List<NbaTeam>> = _conferenceFilter
        .combine(_selectedTeam) { filter, _ ->
            if (filter == null) allTeams else allTeams.filter { it.conference == filter }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = allTeams
        )

    // Map of teamId -> RosterUiState so each team maintains its own roster state across rotations
    private val _rostersByTeam = MutableStateFlow<Map<String, RosterUiState>>(emptyMap())
    val rostersByTeam: StateFlow<Map<String, RosterUiState>> = _rostersByTeam.asStateFlow()

    // Player Stats Dialog State
    private val _selectedPlayerForStats = MutableStateFlow<Pair<NbaPlayer, NbaTeam>?>(null)
    val selectedPlayerForStats: StateFlow<Pair<NbaPlayer, NbaTeam>?> = _selectedPlayerForStats.asStateFlow()

    private val _playerStatsUiState = MutableStateFlow<PlayerStatsUiState>(PlayerStatsUiState.Idle)
    val playerStatsUiState: StateFlow<PlayerStatsUiState> = _playerStatsUiState.asStateFlow()

    init {
        // Automatically prefetch roster for initially selected team
        loadRosterForTeam(allTeams.first())
    }

    fun selectTeam(team: NbaTeam) {
        _selectedTeam.value = team
        loadRosterForTeam(team)
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun setSelectedTeamDetailTab(tabIndex: Int) {
        _selectedTeamDetailTab.value = tabIndex
        if (tabIndex == 1) {
            loadRosterForTeam(_selectedTeam.value)
        }
    }

    fun setConferenceFilter(conference: Conference?) {
        _conferenceFilter.value = conference
    }

    fun toggleTeamExpanded(team: NbaTeam) {
        val current = _expandedTeamIds.value
        val isExpanding = team.id !in current
        _expandedTeamIds.value = if (isExpanding) current + team.id else current - team.id
        if (isExpanding) {
            loadRosterForTeam(team)
        }
    }

    fun toggleAllExpanded() {
        val currentFiltered = filteredTeams.value
        val isAllExpanded = currentFiltered.isNotEmpty() &&
                currentFiltered.all { it.id in _expandedTeamIds.value }

        _expandedTeamIds.value = if (isAllExpanded) {
            emptySet()
        } else {
            currentFiltered.map { it.id }.toSet()
        }
    }

    fun loadRosterForTeam(team: NbaTeam) {
        // If already loaded successfully, don't re-fetch
        val currentRoster = _rostersByTeam.value[team.id]
        if (currentRoster is RosterUiState.Success) return

        _rostersByTeam.value = _rostersByTeam.value + (team.id to RosterUiState.Loading)

        viewModelScope.launch {
            val result = apiService.getTeamRoster(team.espnId)
            result.onSuccess { players ->
                _rostersByTeam.value = _rostersByTeam.value + (team.id to RosterUiState.Success(players))
            }.onFailure { error ->
                val message = error.localizedMessage ?: "Failed to load roster"
                _rostersByTeam.value = _rostersByTeam.value + (team.id to RosterUiState.Error(message))
            }
        }
    }

    fun selectPlayerForStats(player: NbaPlayer, team: NbaTeam) {
        _selectedPlayerForStats.value = Pair(player, team)
        _playerStatsUiState.value = PlayerStatsUiState.Loading

        viewModelScope.launch {
            val result = apiService.getPlayerStats(player.id)
            result.onSuccess { stats ->
                _playerStatsUiState.value = PlayerStatsUiState.Success(stats)
            }.onFailure { error ->
                val message = error.localizedMessage ?: "Failed to load stats"
                _playerStatsUiState.value = PlayerStatsUiState.Error(message)
            }
        }
    }

    fun dismissPlayerStats() {
        _selectedPlayerForStats.value = null
        _playerStatsUiState.value = PlayerStatsUiState.Idle
    }
}
