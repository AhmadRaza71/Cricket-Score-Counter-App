package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.JsonUtils
import com.example.model.PitchMapConfig
import com.example.model.StreetRuleConfig
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.scoring.LiveScoringScreen
import com.example.ui.setup.MatchSetupScreen
import com.example.ui.summary.MatchSummaryScreen
import com.example.ui.theme.GaliCricketTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CricketViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GaliCricketTheme {
                GaliCricketApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaliCricketApp(
    viewModel: CricketViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeMatch by viewModel.activeMatchFlow.collectAsStateWithLifecycle()
    val allMatches by viewModel.allMatchesFlow.collectAsStateWithLifecycle()
    val presets by viewModel.presetsFlow.collectAsStateWithLifecycle()
    val deliveries by viewModel.currentDeliveries.collectAsStateWithLifecycle()
    val redoStack by viewModel.redoStack.collectAsStateWithLifecycle()
    val currentInningsNum by viewModel.currentInningsNumber.collectAsStateWithLifecycle()

    // Handle system back navigation
    if (currentScreen !is AppScreen.Dashboard) {
        BackHandler {
            viewModel.navigateTo(AppScreen.Dashboard)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            AppScreen.Dashboard -> "Gali Cricket"
                            AppScreen.MatchSetup -> "New Match Setup"
                            AppScreen.LiveScoring -> activeMatch?.let { "${it.teamA} vs ${it.teamB}" } ?: "Live Scoring"
                            is AppScreen.MatchSummary -> "Match Summary"
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    if (currentScreen !is AppScreen.Dashboard) {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.Dashboard) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                AppScreen.Dashboard -> {
                    DashboardScreen(
                        activeMatch = activeMatch,
                        pastMatches = allMatches.filter { it.status == "COMPLETED" },
                        presets = presets,
                        onResumeMatch = { viewModel.navigateTo(AppScreen.LiveScoring) },
                        onStartNewMatch = { viewModel.navigateTo(AppScreen.MatchSetup) },
                        onViewMatchDetails = { matchId ->
                            viewModel.navigateTo(AppScreen.MatchSummary(matchId))
                        },
                        onDeleteMatch = { matchId ->
                            viewModel.deleteMatch(matchId)
                        },
                        onDeletePreset = { presetId ->
                            viewModel.deletePreset(presetId)
                        }
                    )
                }

                AppScreen.MatchSetup -> {
                    MatchSetupScreen(
                        presets = presets,
                        onSavePreset = { newPreset ->
                            viewModel.savePreset(newPreset)
                        },
                        onDeletePreset = { presetId ->
                            viewModel.deletePreset(presetId)
                        },
                        onStartMatch = { teamA, teamB, teamAPlayers, teamBPlayers, overs, format, location, date, tossWinner, tossElected, rules, pitchConfig ->
                            viewModel.startNewMatch(
                                teamA,
                                teamB,
                                teamAPlayers,
                                teamBPlayers,
                                overs,
                                format,
                                location,
                                date,
                                tossWinner,
                                tossElected,
                                rules,
                                pitchConfig
                            )
                        }
                    )
                }

                AppScreen.LiveScoring -> {
                    val currentInnings = viewModel.calculateInnings(currentInningsNum)
                    if (currentInnings != null && activeMatch != null) {
                        val teamAPlayers = remember(activeMatch) { JsonUtils.deserializePlayers(activeMatch!!.teamAPlayersJson) }
                        val teamBPlayers = remember(activeMatch) { JsonUtils.deserializePlayers(activeMatch!!.teamBPlayersJson) }
                        val rules = remember(activeMatch) { JsonUtils.deserializeRules(activeMatch!!.rulesJson) }
                        val pitchConfig = remember(activeMatch) { JsonUtils.deserializePitchConfig(activeMatch!!.pitchConfigJson) }

                        val isFirstInnings = (currentInningsNum == 1)
                        val battingRoster = if (isFirstInnings) {
                            if (activeMatch!!.tossElected == "BAT") teamAPlayers else teamBPlayers
                        } else {
                            if (activeMatch!!.tossElected == "BAT") teamBPlayers else teamAPlayers
                        }
                        val bowlingRoster = if (isFirstInnings) {
                            if (activeMatch!!.tossElected == "BAT") teamBPlayers else teamAPlayers
                        } else {
                            if (activeMatch!!.tossElected == "BAT") teamAPlayers else teamBPlayers
                        }

                        LiveScoringScreen(
                            innings = currentInnings,
                            recentDeliveries = deliveries,
                            canUndo = deliveries.isNotEmpty(),
                            canRedo = redoStack.isNotEmpty(),
                            battingRoster = battingRoster,
                            bowlingRoster = bowlingRoster,
                            rules = rules,
                            pitchConfig = pitchConfig,
                            onRecordDelivery = { runs, extraType, extraRuns, isWicket, dismissalType, dismissedPlayer, landmark, comment ->
                                viewModel.recordDelivery(
                                    runs,
                                    extraType,
                                    extraRuns,
                                    isWicket,
                                    dismissalType,
                                    dismissedPlayer,
                                    landmark,
                                    comment
                                )
                            },
                            onUndo = { viewModel.undoLastDelivery() },
                            onRedo = { viewModel.redoDelivery() },
                            onSwapStrike = { viewModel.swapStrike() },
                            onSelectNewBowler = { bowler -> viewModel.setBowler(bowler) },
                            onSelectNewBatter = { batter, isStriker -> viewModel.setBatter(batter, isStriker) },
                            onEndInningsOrMatch = { viewModel.completeInningsOrMatch() }
                        )
                    }
                }

                is AppScreen.MatchSummary -> {
                    val summaryMatch = allMatches.firstOrNull { it.id == screen.matchId } ?: activeMatch
                    if (summaryMatch != null) {
                        val inn1 = viewModel.calculateInnings(1)
                        val inn2 = viewModel.calculateInnings(2)

                        if (inn1 != null) {
                            MatchSummaryScreen(
                                match = summaryMatch,
                                firstInnings = inn1,
                                secondInnings = inn2,
                                onNewMatch = { viewModel.navigateTo(AppScreen.MatchSetup) },
                                onBackToHome = { viewModel.navigateTo(AppScreen.Dashboard) }
                            )
                        }
                    }
                }
            }
        }
    }
}
