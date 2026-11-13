package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CricketRepository
import com.example.data.DeliveryEntity
import com.example.data.JsonUtils
import com.example.data.MatchRecordEntity
import com.example.engine.CalculatedInnings
import com.example.engine.StreetCricketEngine
import com.example.model.BallDelivery
import com.example.model.DismissalType
import com.example.model.ExtraType
import com.example.model.MatchFormat
import com.example.model.PitchMapConfig
import com.example.model.Player
import com.example.model.RulePreset
import com.example.model.StreetRuleConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppScreen {
    object Dashboard : AppScreen
    object MatchSetup : AppScreen
    object LiveScoring : AppScreen
    data class MatchSummary(val matchId: String) : AppScreen
}

class CricketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CricketRepository = CricketRepository(
        AppDatabase.getDatabase(application).cricketDao()
    )

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active Match Record
    val activeMatchFlow: StateFlow<MatchRecordEntity?> = repository.activeMatchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMatchesFlow: StateFlow<List<MatchRecordEntity>> = repository.allMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presetsFlow: StateFlow<List<RulePreset>> = repository.presetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CricketRepository.getDefaultPresets())

    // Active match Deliveries
    private val _currentDeliveries = MutableStateFlow<List<BallDelivery>>(emptyList())
    val currentDeliveries: StateFlow<List<BallDelivery>> = _currentDeliveries.asStateFlow()

    // Redo Stack for instantaneous redo functionality
    private val _redoStack = MutableStateFlow<List<BallDelivery>>(emptyList())
    val redoStack: StateFlow<List<BallDelivery>> = _redoStack.asStateFlow()

    // Active players on pitch
    private val _selectedStriker = MutableStateFlow<String?>(null)
    val selectedStriker: StateFlow<String?> = _selectedStriker.asStateFlow()

    private val _selectedNonStriker = MutableStateFlow<String?>(null)
    val selectedNonStriker: StateFlow<String?> = _selectedNonStriker.asStateFlow()

    private val _selectedBowler = MutableStateFlow<String?>(null)
    val selectedBowler: StateFlow<String?> = _selectedBowler.asStateFlow()

    private val _currentInningsNumber = MutableIntStateFlow(1)
    val currentInningsNumber: StateFlow<Int> = _currentInningsNumber.asStateFlow()

    init {
        // Observe active match changes to load deliveries
        viewModelScope.launch {
            activeMatchFlow.collect { match ->
                if (match != null) {
                    val entities = repository.getDeliveriesSync(match.id)
                    _currentDeliveries.value = entities.map { it.toModel() }
                    _currentInningsNumber.value = if (match.status == "INNINGS_2") 2 else 1

                    val teamAPlayers = JsonUtils.deserializePlayers(match.teamAPlayersJson)
                    val teamBPlayers = JsonUtils.deserializePlayers(match.teamBPlayersJson)

                    val battingTeamPlayers = if (match.status == "INNINGS_2") {
                        if (match.tossElected == "BAT") teamBPlayers else teamAPlayers
                    } else {
                        if (match.tossElected == "BAT") teamAPlayers else teamBPlayers
                    }

                    val bowlingTeamPlayers = if (match.status == "INNINGS_2") {
                        if (match.tossElected == "BAT") teamAPlayers else teamBPlayers
                    } else {
                        if (match.tossElected == "BAT") teamBPlayers else teamAPlayers
                    }

                    if (_selectedStriker.value == null) {
                        _selectedStriker.value = battingTeamPlayers.firstOrNull()?.name
                    }
                    if (_selectedNonStriker.value == null) {
                        _selectedNonStriker.value = battingTeamPlayers.getOrNull(1)?.name
                    }
                    if (_selectedBowler.value == null) {
                        _selectedBowler.value = bowlingTeamPlayers.firstOrNull()?.name
                    }
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun startNewMatch(
        teamA: String,
        teamB: String,
        teamAPlayers: List<Player>,
        teamBPlayers: List<Player>,
        overs: Int,
        format: MatchFormat,
        location: String,
        date: String,
        tossWinner: String,
        tossElected: String,
        rules: StreetRuleConfig,
        pitchConfig: PitchMapConfig
    ) {
        val matchId = java.util.UUID.randomUUID().toString()
        val match = MatchRecordEntity(
            id = matchId,
            teamA = teamA,
            teamB = teamB,
            teamAPlayersJson = JsonUtils.serializePlayers(teamAPlayers),
            teamBPlayersJson = JsonUtils.serializePlayers(teamBPlayers),
            oversLimit = overs,
            matchFormat = format.name,
            environment = pitchConfig.environment.name,
            location = location,
            matchDate = date,
            tossWinner = tossWinner,
            tossElected = tossElected,
            status = "INNINGS_1",
            rulesJson = JsonUtils.serializeRules(rules),
            pitchConfigJson = JsonUtils.serializePitchConfig(pitchConfig)
        )

        val battingPlayers = if (tossElected == "BAT") teamAPlayers else teamBPlayers
        val bowlingPlayers = if (tossElected == "BAT") teamBPlayers else teamAPlayers

        _selectedStriker.value = battingPlayers.firstOrNull()?.name ?: "Striker"
        _selectedNonStriker.value = battingPlayers.getOrNull(1)?.name ?: "Non-Striker"
        _selectedBowler.value = bowlingPlayers.firstOrNull()?.name ?: "Bowler"
        _currentDeliveries.value = emptyList()
        _redoStack.value = emptyList()
        _currentInningsNumber.value = 1

        viewModelScope.launch {
            repository.saveMatch(match)
            _currentScreen.value = AppScreen.LiveScoring
        }
    }

    fun recordDelivery(
        runsOffBat: Int,
        extraType: ExtraType,
        extraRuns: Int,
        isWicket: Boolean,
        dismissalType: DismissalType,
        dismissedPlayer: String?,
        landmarkHit: String?,
        comment: String
    ) {
        val activeMatch = activeMatchFlow.value ?: return
        val currentDels = _currentDeliveries.value.filter { it.inningsNumber == _currentInningsNumber.value }

        val legalCount = currentDels.count { it.isLegal }
        val overNum = legalCount / 6
        val ballInOver = (legalCount % 6) + 1
        val isLegal = extraType == ExtraType.NONE || extraType == ExtraType.BYE || extraType == ExtraType.LEG_BYE

        val delivery = BallDelivery(
            inningsNumber = _currentInningsNumber.value,
            overNumber = overNum,
            ballInOver = if (isLegal) ballInOver else (legalCount % 6),
            isLegal = isLegal,
            strikerName = _selectedStriker.value ?: "Striker",
            nonStrikerName = _selectedNonStriker.value ?: "Non-Striker",
            bowlerName = _selectedBowler.value ?: "Bowler",
            runsOffBat = runsOffBat,
            extraRuns = extraRuns,
            extraType = extraType,
            isWicket = isWicket,
            dismissalType = dismissalType,
            dismissedPlayerName = dismissedPlayer ?: if (isWicket) _selectedStriker.value else null,
            landmarkHitName = landmarkHit,
            comment = comment
        )

        // Clear redo stack on new action
        _redoStack.value = emptyList()

        viewModelScope.launch {
            repository.recordDelivery(delivery.toEntity(activeMatch.id))
            _currentDeliveries.value = _currentDeliveries.value + delivery

            // Automatic strike rotation logic:
            // 1. Odd runs swap strike
            val runsRun = runsOffBat + (if (extraType == ExtraType.BYE || extraType == ExtraType.LEG_BYE) extraRuns else 0)
            if (runsRun % 2 != 0 && !isWicket) {
                swapStrike()
            }

            // 2. Over completed (6 legal deliveries)
            val updatedDels = _currentDeliveries.value.filter { it.inningsNumber == _currentInningsNumber.value }
            val newLegalCount = updatedDels.count { it.isLegal }
            if (isLegal && newLegalCount > 0 && newLegalCount % 6 == 0) {
                swapStrike()
            }
        }
    }

    /**
     * Revert the last ball's scoring update seamlessly.
     */
    fun undoLastDelivery() {
        val activeMatch = activeMatchFlow.value ?: return
        val currentList = _currentDeliveries.value
        if (currentList.isEmpty()) return

        val lastDelivery = currentList.last()
        val remaining = currentList.dropLast(1)

        _redoStack.value = _redoStack.value + lastDelivery
        _currentDeliveries.value = remaining

        viewModelScope.launch {
            repository.removeDelivery(lastDelivery.deliveryId)
        }
    }

    /**
     * Reapply the previously undone ball scoring update.
     */
    fun redoDelivery() {
        val activeMatch = activeMatchFlow.value ?: return
        val redoList = _redoStack.value
        if (redoList.isEmpty()) return

        val nextDelivery = redoList.last()
        _redoStack.value = redoList.dropLast(1)
        _currentDeliveries.value = _currentDeliveries.value + nextDelivery

        viewModelScope.launch {
            repository.recordDelivery(nextDelivery.toEntity(activeMatch.id))
        }
    }

    fun swapStrike() {
        val temp = _selectedStriker.value
        _selectedStriker.value = _selectedNonStriker.value
        _selectedNonStriker.value = temp
    }

    fun setBowler(bowlerName: String) {
        _selectedBowler.value = bowlerName
    }

    fun setBatter(playerName: String, isStriker: Boolean) {
        if (isStriker) {
            _selectedStriker.value = playerName
        } else {
            _selectedNonStriker.value = playerName
        }
    }

    fun savePreset(preset: RulePreset) {
        viewModelScope.launch {
            repository.savePreset(preset)
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            repository.deletePreset(presetId)
        }
    }

    fun deleteMatch(matchId: String) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
        }
    }

    fun completeInningsOrMatch() {
        val activeMatch = activeMatchFlow.value ?: return
        if (_currentInningsNumber.value == 1) {
            // Transition to Innings 2
            _currentInningsNumber.value = 2
            val teamAPlayers = JsonUtils.deserializePlayers(activeMatch.teamAPlayersJson)
            val teamBPlayers = JsonUtils.deserializePlayers(activeMatch.teamBPlayersJson)

            val newBattingTeam = if (activeMatch.tossElected == "BAT") teamBPlayers else teamAPlayers
            val newBowlingTeam = if (activeMatch.tossElected == "BAT") teamAPlayers else teamBPlayers

            _selectedStriker.value = newBattingTeam.firstOrNull()?.name
            _selectedNonStriker.value = newBattingTeam.getOrNull(1)?.name
            _selectedBowler.value = newBowlingTeam.firstOrNull()?.name

            viewModelScope.launch {
                repository.updateMatch(activeMatch.copy(status = "INNINGS_2"))
            }
        } else {
            // Match completed
            viewModelScope.launch {
                repository.updateMatch(activeMatch.copy(status = "COMPLETED"))
                _currentScreen.value = AppScreen.MatchSummary(activeMatch.id)
            }
        }
    }

    fun calculateInnings(inningsNum: Int): CalculatedInnings? {
        val match = activeMatchFlow.value ?: return null
        val teamAPlayers = JsonUtils.deserializePlayers(match.teamAPlayersJson)
        val teamBPlayers = JsonUtils.deserializePlayers(match.teamBPlayersJson)
        val rules = JsonUtils.deserializeRules(match.rulesJson)

        val isFirstInnings = (inningsNum == 1)
        val battingTeamName = if (isFirstInnings) {
            if (match.tossElected == "BAT") match.teamA else match.teamB
        } else {
            if (match.tossElected == "BAT") match.teamB else match.teamA
        }

        val bowlingTeamName = if (isFirstInnings) {
            if (match.tossElected == "BAT") match.teamB else match.teamA
        } else {
            if (match.tossElected == "BAT") match.teamA else match.teamB
        }

        val battingPlayers = if (isFirstInnings) {
            if (match.tossElected == "BAT") teamAPlayers else teamBPlayers
        } else {
            if (match.tossElected == "BAT") teamBPlayers else teamAPlayers
        }

        val bowlingPlayers = if (isFirstInnings) {
            if (match.tossElected == "BAT") teamBPlayers else teamAPlayers
        } else {
            if (match.tossElected == "BAT") teamAPlayers else teamBPlayers
        }

        val target = if (!isFirstInnings) {
            val inn1 = StreetCricketEngine.calculateInnings(
                1,
                if (match.tossElected == "BAT") match.teamA else match.teamB,
                if (match.tossElected == "BAT") match.teamB else match.teamA,
                if (match.tossElected == "BAT") teamAPlayers else teamBPlayers,
                if (match.tossElected == "BAT") teamBPlayers else teamAPlayers,
                match.oversLimit,
                null,
                rules,
                _currentDeliveries.value,
                null, null, null
            )
            inn1.totalRuns + 1
        } else null

        return StreetCricketEngine.calculateInnings(
            inningsNumber = inningsNum,
            battingTeamName = battingTeamName,
            bowlingTeamName = bowlingTeamName,
            battingPlayers = battingPlayers,
            bowlingPlayers = bowlingPlayers,
            oversLimit = match.oversLimit,
            target = target,
            rules = rules,
            deliveries = _currentDeliveries.value,
            selectedStrikerName = _selectedStriker.value,
            selectedNonStrikerName = _selectedNonStriker.value,
            selectedBowlerName = _selectedBowler.value
        )
    }
}

private fun BallDelivery.toEntity(matchId: String): DeliveryEntity {
    return DeliveryEntity(
        id = deliveryId,
        matchId = matchId,
        inningsNumber = inningsNumber,
        overNumber = overNumber,
        ballInOver = ballInOver,
        isLegal = isLegal,
        strikerName = strikerName,
        nonStrikerName = nonStrikerName,
        bowlerName = bowlerName,
        runsOffBat = runsOffBat,
        extraRuns = extraRuns,
        extraType = extraType.name,
        isWicket = isWicket,
        dismissalType = dismissalType.name,
        dismissedPlayerName = dismissedPlayerName,
        landmarkHitName = landmarkHitName,
        comment = comment,
        timestamp = timestamp
    )
}

private fun DeliveryEntity.toModel(): BallDelivery {
    return BallDelivery(
        deliveryId = id,
        inningsNumber = inningsNumber,
        overNumber = overNumber,
        ballInOver = ballInOver,
        isLegal = isLegal,
        strikerName = strikerName,
        nonStrikerName = nonStrikerName,
        bowlerName = bowlerName,
        runsOffBat = runsOffBat,
        extraRuns = extraRuns,
        extraType = try { ExtraType.valueOf(extraType) } catch (_: Exception) { ExtraType.NONE },
        isWicket = isWicket,
        dismissalType = try { DismissalType.valueOf(dismissalType) } catch (_: Exception) { DismissalType.NONE },
        dismissedPlayerName = dismissedPlayerName,
        landmarkHitName = landmarkHitName,
        comment = comment,
        timestamp = timestamp
    )
}

class MutableIntStateFlow(initial: Int) : MutableStateFlow<Int> by MutableStateFlow(initial)
