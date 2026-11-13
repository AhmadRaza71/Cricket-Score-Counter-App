package com.example.ui.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BowlingStyle
import com.example.model.MatchFormat
import com.example.model.PitchMapConfig
import com.example.model.Player
import com.example.model.RulePreset
import com.example.model.StreetRuleConfig
import com.example.ui.canvas.PitchBoundaryCanvas
import com.example.ui.theme.BallAmber
import com.example.ui.theme.CricketLeatherRed
import com.example.ui.theme.TurfNeonGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchSetupScreen(
    presets: List<RulePreset>,
    onSavePreset: (RulePreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onStartMatch: (
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
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var setupTab by remember { mutableIntStateOf(0) } // 0: Teams & Format, 1: Street Rules, 2: Pitch & Boundary Canvas

    // Teams state
    var teamAName by remember { mutableStateOf("Gali Warriors") }
    var teamBName by remember { mutableStateOf("Corner Tigers") }
    var teamAPlayers by remember {
        mutableStateOf(
            listOf(
                Player(name = "Ali", isCaptain = true),
                Player(name = "Hamza"),
                Player(name = "Bilal"),
                Player(name = "Usman"),
                Player(name = "Zaid")
            )
        )
    }
    var teamBPlayers by remember {
        mutableStateOf(
            listOf(
                Player(name = "Rohit", isCaptain = true),
                Player(name = "Sameer"),
                Player(name = "Arjun"),
                Player(name = "Sunny")
            )
        )
    }

    var newPlayerNameA by remember { mutableStateOf("") }
    var newPlayerNameB by remember { mutableStateOf("") }

    // Format & Venue state
    var selectedOvers by remember { mutableIntStateOf(6) }
    var selectedFormat by remember { mutableStateOf(MatchFormat.LIMITED_OVERS) }
    var matchLocation by remember { mutableStateOf("Gali No. 3, Near Electric Pole") }
    val currentDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var matchDate by remember { mutableStateOf(currentDateStr) }

    // Rules state
    var activePresetId by remember { mutableStateOf(presets.firstOrNull()?.id ?: "preset_gali_v1") }
    var currentRules by remember {
        mutableStateOf(presets.firstOrNull()?.config ?: StreetRuleConfig())
    }

    // Pitch config
    var pitchConfig by remember { mutableStateOf(PitchMapConfig()) }

    // Toss Dialog state
    var showTossDialog by remember { mutableStateOf(false) }
    var tossWinner by remember { mutableStateOf("Gali Warriors") }
    var tossElected by remember { mutableStateOf("BAT") }

    // Presets Management Dialogs
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showManagePresetsDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var newPresetDesc by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row navigation
        TabRow(selectedTabIndex = setupTab) {
            Tab(
                selected = setupTab == 0,
                onClick = { setupTab = 0 },
                text = { Text("Teams & Venue", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = setupTab == 1,
                onClick = { setupTab = 1 },
                text = { Text("Street Rules", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.SportsCricket, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = setupTab == 2,
                onClick = { setupTab = 2 },
                text = { Text("Pitch & Map", fontSize = 13.sp) },
                icon = { Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (setupTab) {
                0 -> {
                    // Teams & Match Format Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        "Match Setup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = matchLocation,
                                        onValueChange = { matchLocation = it },
                                        label = { Text("Match Location / Gali Spot") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = matchDate,
                                        onValueChange = { matchDate = it },
                                        label = { Text("Match Date (YYYY-MM-DD)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("Overs Per Innings:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(3, 4, 5, 6, 8, 10, 12, 16, 20).forEach { ov ->
                                            FilterChip(
                                                selected = selectedOvers == ov,
                                                onClick = { selectedOvers = ov },
                                                label = { Text("$ov Overs") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Team A Roster
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = teamAName,
                                            onValueChange = { teamAName = it },
                                            label = { Text("Team 1 Name") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${teamAPlayers.size} players",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = newPlayerNameA,
                                            onValueChange = { newPlayerNameA = it },
                                            placeholder = { Text("Add Player Name") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                if (newPlayerNameA.isNotBlank()) {
                                                    teamAPlayers = teamAPlayers + Player(name = newPlayerNameA.trim())
                                                    newPlayerNameA = ""
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Player")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        teamAPlayers.forEachIndexed { idx, p ->
                                            FilterChip(
                                                selected = p.isCaptain,
                                                onClick = {
                                                    teamAPlayers = teamAPlayers.mapIndexed { i, player ->
                                                        player.copy(isCaptain = (i == idx))
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = if (p.isCaptain) "${p.name} (C)" else p.name,
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (teamAPlayers.size > 2) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clickable {
                                                                    teamAPlayers = teamAPlayers.filterIndexed { i, _ -> i != idx }
                                                                }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Team B Roster
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = teamBName,
                                            onValueChange = { teamBName = it },
                                            label = { Text("Team 2 Name") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${teamBPlayers.size} players",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = newPlayerNameB,
                                            onValueChange = { newPlayerNameB = it },
                                            placeholder = { Text("Add Player Name") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                if (newPlayerNameB.isNotBlank()) {
                                                    teamBPlayers = teamBPlayers + Player(name = newPlayerNameB.trim())
                                                    newPlayerNameB = ""
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Player")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        teamBPlayers.forEachIndexed { idx, p ->
                                            FilterChip(
                                                selected = p.isCaptain,
                                                onClick = {
                                                    teamBPlayers = teamBPlayers.mapIndexed { i, player ->
                                                        player.copy(isCaptain = (i == idx))
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = if (p.isCaptain) "${p.name} (C)" else p.name,
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (teamBPlayers.size > 2) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clickable {
                                                                    teamBPlayers = teamBPlayers.filterIndexed { i, _ -> i != idx }
                                                                }
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    if (teamAPlayers.size != teamBPlayers.size) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Note: Uneven teams (${teamAPlayers.size} vs ${teamBPlayers.size}). Last Man Stands rule allows solo batting for the smaller team.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Street Rules Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Presets Management Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Custom Rule Presets", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text("Instant load street rule templates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row {
                                            IconButton(onClick = { showManagePresetsDialog = true }) {
                                                Icon(Icons.Default.Settings, contentDescription = "Manage Presets")
                                            }
                                            IconButton(onClick = { showSavePresetDialog = true }) {
                                                Icon(Icons.Default.Save, contentDescription = "Save Preset", tint = TurfNeonGreen)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Quick-Select Presets FlowRow
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        presets.forEach { preset ->
                                            val isSelected = activePresetId == preset.id
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    activePresetId = preset.id
                                                    currentRules = preset.config
                                                },
                                                label = {
                                                    Text(preset.presetName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                                },
                                                leadingIcon = {
                                                    if (isSelected) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    } else {
                                                        Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Six is Out Rule (Hit-and-Out)
                        item {
                            RuleSwitchCard(
                                title = "Direct Six is Out (Hit-and-Out)",
                                description = "Hitting a six directly outside the street or over neighbor walls counts as batter OUT immediately.",
                                checked = currentRules.sixIsOut,
                                onCheckedChange = { currentRules = currentRules.copy(sixIsOut = it) }
                            )
                        }

                        // One-Tip-One-Hand Catch
                        item {
                            RuleSwitchCard(
                                title = "One-Tip-One-Hand Catch",
                                description = "Catches taken cleanly with a single hand after exactly one bounce are declared OUT.",
                                checked = currentRules.oneTipOneHand,
                                onCheckedChange = { currentRules = currentRules.copy(oneTipOneHand = it) }
                            )
                        }

                        // Last Man Stands Rule
                        item {
                            RuleSwitchCard(
                                title = "Last Man Stands (Solo Batting)",
                                description = "When only 1 batter remains (or uneven teams), the last player continues batting alone.",
                                checked = currentRules.lastManStands,
                                onCheckedChange = { currentRules = currentRules.copy(lastManStands = it) }
                            )
                        }

                        // Bowling Style
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Bowling Style:", fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        BowlingStyle.values().forEach { bs ->
                                            FilterChip(
                                                selected = currentRules.bowlingStyle == bs,
                                                onClick = { currentRules = currentRules.copy(bowlingStyle = bs) },
                                                label = { Text(bs.displayName) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Car Penalty Rule
                        item {
                            RuleSwitchCard(
                                title = "Parked Car Penalty Rule",
                                description = "Ball hits parked car: -2 runs penalty to discourage vehicle damage.",
                                checked = currentRules.carRuleActive,
                                onCheckedChange = { currentRules = currentRules.copy(carRuleActive = it) }
                            )
                        }

                        // Drain Rule
                        item {
                            RuleSwitchCard(
                                title = "Ball in Drain / Lost Ball (OUT)",
                                description = "Ball going into drainage canal or sewer is declared OUT, batter must fetch the ball.",
                                checked = currentRules.drainRuleActive,
                                onCheckedChange = { currentRules = currentRules.copy(drainRuleActive = it) }
                            )
                        }

                        // Wall Rule
                        item {
                            RuleSwitchCard(
                                title = "Wall / Window Touch Rule",
                                description = "Hitting boundary wall directly awards runs or penalty as street local agreement.",
                                checked = currentRules.wallRuleActive,
                                onCheckedChange = { currentRules = currentRules.copy(wallRuleActive = it) }
                            )
                        }

                        // Bowler Over Limit
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Max Overs Per Bowler", fontWeight = FontWeight.SemiBold)
                                        Text("Fair bowling rotation for street matches", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = {
                                                if (currentRules.maxOversPerBowler > 1) {
                                                    currentRules = currentRules.copy(maxOversPerBowler = currentRules.maxOversPerBowler - 1)
                                                }
                                            }
                                        ) { Text("-") }
                                        Text(
                                            "${currentRules.maxOversPerBowler}",
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                currentRules = currentRules.copy(maxOversPerBowler = currentRules.maxOversPerBowler + 1)
                                            }
                                        ) { Text("+") }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Pitch & Boundary Canvas Tab
                    PitchBoundaryCanvas(
                        config = pitchConfig,
                        onConfigChanged = { pitchConfig = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Bottom Bar: Proceed / Toss / Start
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showTossDialog = true }
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Coin Toss")
                }

                Button(
                    onClick = {
                        onStartMatch(
                            teamAName.ifBlank { "Team A" },
                            teamBName.ifBlank { "Team B" },
                            teamAPlayers,
                            teamBPlayers,
                            selectedOvers,
                            selectedFormat,
                            matchLocation,
                            matchDate,
                            tossWinner,
                            tossElected,
                            currentRules,
                            pitchConfig
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfNeonGreen,
                        contentColor = Color(0xFF003912)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Match", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Toss Simulation Dialog
    if (showTossDialog) {
        var isFlipping by remember { mutableStateOf(false) }
        var tossResultText by remember { mutableStateOf<String?>(null) }
        val rotationAngle by animateFloatAsState(
            targetValue = if (isFlipping) 720f else 0f,
            animationSpec = tween(durationMillis = 600),
            finishedListener = {
                isFlipping = false
                val isHeads = Random.nextBoolean()
                val winner = if (Random.nextBoolean()) teamAName else teamBName
                tossWinner = winner
                tossResultText = "$winner won the toss! (${if (isHeads) "HEADS" else "TAILS"})"
            }
        )

        AlertDialog(
            onDismissRequest = { showTossDialog = false },
            title = { Text("Match Coin Toss", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .rotate(rotationAngle)
                            .background(Color(0xFFFFD700), CircleShape)
                            .clickable { isFlipping = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFlipping) "..." else "🪙 FLIP",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF5D4037),
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (tossResultText != null) {
                        Text(
                            text = tossResultText!!,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Elected to:", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = tossElected == "BAT",
                                onClick = { tossElected = "BAT" },
                                label = { Text("🏏 BAT FIRST") }
                            )
                            FilterChip(
                                selected = tossElected == "BOWL",
                                onClick = { tossElected = "BOWL" },
                                label = { Text("⚾ BOWL FIRST") }
                            )
                        }
                    } else {
                        Text("Tap the golden coin to flip for the toss!")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTossDialog = false }) {
                    Text("Confirm Toss")
                }
            }
        )
    }

    // Save Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Custom Rule Preset") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Preset Name (e.g. Rajput Park, Gali v1)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPresetDesc,
                        onValueChange = { newPresetDesc = it },
                        label = { Text("Short Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Includes: Six-Out (${if (currentRules.sixIsOut) "ON" else "OFF"}), One-Tip (${if (currentRules.oneTipOneHand) "ON" else "OFF"}), Last-Man (${if (currentRules.lastManStands) "ON" else "OFF"}), Style (${currentRules.bowlingStyle.displayName})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            val newPreset = RulePreset(
                                presetName = newPresetName.trim(),
                                description = newPresetDesc.trim().ifBlank { "Custom street rules" },
                                config = currentRules,
                                isDefault = false
                            )
                            onSavePreset(newPreset)
                            activePresetId = newPreset.id
                            showSavePresetDialog = false
                            newPresetName = ""
                            newPresetDesc = ""
                        }
                    }
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View & Manage All Presets Dialog
    if (showManagePresetsDialog) {
        AlertDialog(
            onDismissRequest = { showManagePresetsDialog = false },
            title = { Text("Saved Rule Presets", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(presets) { preset ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (activePresetId == preset.id) TurfNeonGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(preset.presetName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row {
                                        if (activePresetId == preset.id) {
                                            Surface(
                                                color = TurfNeonGreen,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "ACTIVE",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF003912)
                                                )
                                            }
                                        }

                                        if (!preset.isDefault) {
                                            IconButton(
                                                onClick = {
                                                    onDeletePreset(preset.id)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Preset",
                                                    tint = CricketLeatherRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Rule details tags
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (preset.config.sixIsOut) RuleTag("Six is OUT")
                                    if (preset.config.oneTipOneHand) RuleTag("One-Tip Catch")
                                    if (preset.config.lastManStands) RuleTag("Last Man Stands")
                                    RuleTag(preset.config.bowlingStyle.displayName)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        activePresetId = preset.id
                                        currentRules = preset.config
                                        showManagePresetsDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activePresetId == preset.id) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(if (activePresetId == preset.id) "Currently Selected" else "Load This Preset")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManagePresetsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun RuleTag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RuleSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

