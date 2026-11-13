package com.example.ui.scoring

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CalculatedInnings
import com.example.model.BallDelivery
import com.example.model.DismissalType
import com.example.model.ExtraType
import com.example.model.LandmarkMarker
import com.example.model.PitchMapConfig
import com.example.model.Player
import com.example.model.StreetRuleConfig
import com.example.ui.theme.BallAmber
import com.example.ui.theme.CricketLeatherRed
import com.example.ui.theme.CricketRedDark
import com.example.ui.theme.TurfGreenDark
import com.example.ui.theme.TurfNeonGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveScoringScreen(
    innings: CalculatedInnings,
    recentDeliveries: List<BallDelivery>,
    canUndo: Boolean,
    canRedo: Boolean,
    battingRoster: List<Player>,
    bowlingRoster: List<Player>,
    rules: StreetRuleConfig,
    pitchConfig: PitchMapConfig,
    onRecordDelivery: (
        runsOffBat: Int,
        extraType: ExtraType,
        extraRuns: Int,
        isWicket: Boolean,
        dismissalType: DismissalType,
        dismissedPlayer: String?,
        landmarkHit: String?,
        comment: String
    ) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSwapStrike: () -> Unit,
    onSelectNewBowler: (String) -> Unit,
    onSelectNewBatter: (String, isStriker: Boolean) -> Unit,
    onEndInningsOrMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWicketDialog by remember { mutableStateOf(false) }
    var showExtrasDialog by remember { mutableStateOf(false) }
    var showBowlerPicker by remember { mutableStateOf(false) }
    var showBatterPicker by remember { mutableStateOf(false) }
    var isPickingStriker by remember { mutableStateOf(true) }

    // Camera Snapshot state
    var capturedSnapshot by remember { mutableStateOf<Bitmap?>(null) }
    var showPhotoModal by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedSnapshot = bitmap
            showPhotoModal = true
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. TOP SCORE HUD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Batting Team Header & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${innings.battingTeam} (Innings ${innings.inningsNumber})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (innings.target != null) {
                                Text(
                                    text = "Target: ${innings.target} • Need ${innings.runsNeeded} from ${innings.ballsRemaining} balls",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Camera Snapshot button
                        IconButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera Match Snapshot",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Huge Score Display (High Outdoor Contrast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${innings.totalRuns}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "/${innings.wickets}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = CricketLeatherRed,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Overs: ${innings.oversString}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "CRR: ${String.format("%.2f", innings.currentRunRate)}" +
                                        if (innings.target != null) " • RRR: ${String.format("%.2f", innings.requiredRunRate)}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Last Man Stands indicator banner
                    if (innings.isSoloBatting) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = BallAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚡ LAST MAN STANDS: Solo Batting Mode Active",
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8F00)
                            )
                        }
                    }
                }
            }
        }

        // 2. ACTIVE BATTERS & BOWLER CARDS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Batters section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Batters", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onSwapStrike, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Strike", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Striker row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FiberManualRecord,
                                contentDescription = "On Strike",
                                tint = TurfNeonGreen,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${innings.striker?.playerName ?: "Striker"} *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.clickable {
                                    isPickingStriker = true
                                    showBatterPicker = true
                                }
                            )
                        }
                        Text(
                            text = "${innings.striker?.runs ?: 0} (${innings.striker?.ballsFaced ?: 0}b • 4s:${innings.striker?.fours ?: 0} 6s:${innings.striker?.sixes ?: 0})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    // Non-Striker row
                    if (!innings.isSoloBatting) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "  ${innings.nonStriker?.playerName ?: "Non-Striker"}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.clickable {
                                    isPickingStriker = false
                                    showBatterPicker = true
                                }
                            )
                            Text(
                                text = "${innings.nonStriker?.runs ?: 0} (${innings.nonStriker?.ballsFaced ?: 0}b)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Bowler section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚾ Bowler: ${innings.currentBowler?.bowlerName ?: "Select Bowler"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = { showBowlerPicker = true },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Change", fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = "${innings.currentBowler?.wickets ?: 0}-${innings.currentBowler?.runsConceded ?: 0} (${innings.currentBowler?.oversString ?: "0.0"} ov)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 3. RECENT BALLS TICKER + UNDO / REDO CONTROLS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recent balls
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("This Over:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    val currentOverBalls = recentDeliveries.filter { it.inningsNumber == innings.inningsNumber }.takeLast(6)
                    if (currentOverBalls.isEmpty()) {
                        Text("-", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        currentOverBalls.forEach { del ->
                            BallTickerCircle(del)
                        }
                    }
                }

                // Instant Undo / Redo controls
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier
                            .background(
                                if (canUndo) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                CircleShape
                            )
                            .size(36.dp)
                            .testTag("undo_button")
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo Last Delivery",
                            tint = if (canUndo) TurfNeonGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier
                            .background(
                                if (canRedo) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                CircleShape
                            )
                            .size(36.dp)
                            .testTag("redo_button")
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Redo Delivery",
                            tint = if (canRedo) BallAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 4. LANDMARK QUICK-ACTION PINS (Wall, Car, Drain shortcuts)
        if (pitchConfig.landmarks.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Street Obstacle Quick-Tags:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            pitchConfig.landmarks.forEach { lm ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        onRecordDelivery(
                                            0,
                                            ExtraType.NONE,
                                            lm.penaltyRuns,
                                            lm.isOut,
                                            if (lm.isOut) DismissalType.CAR_PENALTY_OUT else DismissalType.NONE,
                                            innings.striker?.playerName,
                                            lm.name,
                                            "Hit ${lm.name} (${lm.penaltyRuns} runs${if (lm.isOut) ", OUT" else ""})"
                                        )
                                    },
                                    label = {
                                        Text(
                                            "${lm.name} (${if (lm.isOut) "OUT" else "${lm.penaltyRuns}r"})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. MAIN MANUAL SCORING PAD (Runs & Street Rules)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top run row: 0, 1, 2, 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScoreButton("0", modifier = Modifier.weight(1f)) {
                        onRecordDelivery(0, ExtraType.NONE, 0, false, DismissalType.NONE, null, null, "Dot ball")
                    }
                    ScoreButton("1", modifier = Modifier.weight(1f)) {
                        onRecordDelivery(1, ExtraType.NONE, 0, false, DismissalType.NONE, null, null, "Single run")
                    }
                    ScoreButton("2", modifier = Modifier.weight(1f)) {
                        onRecordDelivery(2, ExtraType.NONE, 0, false, DismissalType.NONE, null, null, "Two runs")
                    }
                    ScoreButton("3", modifier = Modifier.weight(1f)) {
                        onRecordDelivery(3, ExtraType.NONE, 0, false, DismissalType.NONE, null, null, "Three runs")
                    }
                }

                // Boundaries and Dismissal row: 4, 6 (or Six-Out), WICKET, EXTRAS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // FOUR
                    ScoreButton(
                        "4",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF1E3A8A),
                        textColor = Color.White
                    ) {
                        onRecordDelivery(4, ExtraType.NONE, 0, false, DismissalType.NONE, null, null, "FOUR!")
                    }

                    // SIX (Handles Six-is-Out automatically if toggled!)
                    if (rules.sixIsOut) {
                        ScoreButton(
                            "6 (OUT)",
                            modifier = Modifier.weight(1.2f),
                            backgroundColor = CricketRedDark,
                            textColor = Color.White
                        ) {
                            onRecordDelivery(
                                0,
                                ExtraType.NONE,
                                0,
                                true,
                                DismissalType.DIRECT_SIX_OUT,
                                innings.striker?.playerName,
                                null,
                                "Direct Six Hit-and-Out!"
                            )
                        }
                    } else {
                        ScoreButton(
                            "6",
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color(0xFF0D9488),
                            textColor = Color.White
                        ) {
                            onRecordDelivery(6, ExtraType.NONE, 0, false, DismissalType.NONE, null, null, "SIX!")
                        }
                    }

                    // WICKET POPUP TRIGGER
                    ScoreButton(
                        "OUT",
                        modifier = Modifier.weight(1.2f),
                        backgroundColor = CricketLeatherRed,
                        textColor = Color.White,
                        testTag = "wicket_button"
                    ) {
                        showWicketDialog = true
                    }

                    // EXTRAS POPUP TRIGGER
                    ScoreButton(
                        "EXTRAS",
                        modifier = Modifier.weight(1.2f),
                        backgroundColor = BallAmber,
                        textColor = Color(0xFF451A03),
                        testTag = "extras_button"
                    ) {
                        showExtrasDialog = true
                    }
                }

                // Street One-Tip-One-Hand Catch quick button
                if (rules.oneTipOneHand) {
                    OutlinedButton(
                        onClick = {
                            onRecordDelivery(
                                0,
                                ExtraType.NONE,
                                0,
                                true,
                                DismissalType.ONE_TIP_CATCH,
                                innings.striker?.playerName,
                                null,
                                "One-Tip-One-Hand Catch!"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CricketLeatherRed)
                    ) {
                        Text("🖐️ ONE-TIP-ONE-HAND CATCH (OUT)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Innings Completed Action
        if (innings.isInningsFinished) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TurfGreenDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🏁 Innings Completed!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = innings.finishReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onEndInningsOrMatch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurfNeonGreen,
                                contentColor = Color(0xFF003912)
                            )
                        ) {
                            Text(
                                text = if (innings.inningsNumber == 1) "Start 2nd Innings" else "View Match Result & Card",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // 6. STREET DISMISSAL DIALOG
    if (showWicketDialog) {
        AlertDialog(
            onDismissRequest = { showWicketDialog = false },
            title = { Text("Select Dismissal Type", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DismissalType.values().filter { it != DismissalType.NONE }.forEach { dType ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRecordDelivery(
                                        0,
                                        ExtraType.NONE,
                                        0,
                                        true,
                                        dType,
                                        innings.striker?.playerName,
                                        null,
                                        dType.displayName
                                    )
                                    showWicketDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (dType.isStreetSpecial) CricketLeatherRed.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dType.displayName,
                                    fontWeight = if (dType.isStreetSpecial) FontWeight.Bold else FontWeight.Normal,
                                    color = if (dType.isStreetSpecial) CricketLeatherRed else MaterialTheme.colorScheme.onSurface
                                )
                                if (dType.isStreetSpecial) {
                                    Text("STREET", style = MaterialTheme.typography.labelSmall, color = CricketLeatherRed)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWicketDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 7. EXTRAS DIALOG (Wide, No Ball, Byes)
    if (showExtrasDialog) {
        var selectedExtra by remember { mutableStateOf(ExtraType.WIDE) }
        var extraRunsAmount by remember { mutableIntStateOf(1) }
        var runsOffBatWithExtra by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { showExtrasDialog = false },
            title = { Text("Record Extras", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Extra Type Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(ExtraType.WIDE, ExtraType.NO_BALL, ExtraType.BYE, ExtraType.LEG_BYE).forEach { et ->
                            FilterChip(
                                selected = selectedExtra == et,
                                onClick = { selectedExtra = et },
                                label = { Text(et.shortLabel, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Extra Penalty Runs (default 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Extra Runs:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { if (extraRunsAmount > 1) extraRunsAmount-- }) { Text("-") }
                            Text("$extraRunsAmount", modifier = Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = { extraRunsAmount++ }) { Text("+") }
                        }
                    }

                    // Additional runs off bat or overthrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Runs Run / Bat:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { if (runsOffBatWithExtra > 0) runsOffBatWithExtra-- }) { Text("-") }
                            Text("$runsOffBatWithExtra", modifier = Modifier.padding(horizontal = 10.dp), fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = { runsOffBatWithExtra++ }) { Text("+") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordDelivery(
                            runsOffBatWithExtra,
                            selectedExtra,
                            extraRunsAmount,
                            false,
                            DismissalType.NONE,
                            null,
                            null,
                            "${selectedExtra.displayName} + $runsOffBatWithExtra runs"
                        )
                        showExtrasDialog = false
                    }
                ) {
                    Text("Add Delivery")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtrasDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 8. CHANGE BOWLER PICKER
    if (showBowlerPicker) {
        AlertDialog(
            onDismissRequest = { showBowlerPicker = false },
            title = { Text("Select Bowler", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bowlingRoster) { p ->
                        val bStats = innings.bowlers.firstOrNull { it.bowlerName == p.name }
                        val isCurrent = innings.currentBowler?.bowlerName == p.name
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectNewBowler(p.name)
                                    showBowlerPicker = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) TurfNeonGreen.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${bStats?.wickets ?: 0}-${bStats?.runsConceded ?: 0} (${bStats?.oversString ?: "0.0"} ov)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBowlerPicker = false }) { Text("Close") }
            }
        )
    }

    // 9. CHANGE BATTER PICKER
    if (showBatterPicker) {
        AlertDialog(
            onDismissRequest = { showBatterPicker = false },
            title = { Text(if (isPickingStriker) "Select Striker" else "Select Non-Striker", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(battingRoster) { p ->
                        val bStats = innings.batters.firstOrNull { it.playerName == p.name }
                        val isOut = bStats?.isOut == true
                        val isCurrent = (isPickingStriker && innings.striker?.playerName == p.name) ||
                                (!isPickingStriker && innings.nonStriker?.playerName == p.name)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isOut) {
                                    onSelectNewBatter(p.name, isPickingStriker)
                                    showBatterPicker = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isOut -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                    isCurrent -> TurfNeonGreen.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = p.name + if (isOut) " (OUT)" else "",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOut) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${bStats?.runs ?: 0} (${bStats?.ballsFaced ?: 0}b)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatterPicker = false }) { Text("Close") }
            }
        )
    }

    // 10. PHOTO SNAPSHOT PREVIEW MODAL
    if (showPhotoModal && capturedSnapshot != null) {
        AlertDialog(
            onDismissRequest = { showPhotoModal = false },
            title = { Text("Match Snapshot Saved", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = capturedSnapshot!!.asImageBitmap(),
                        contentDescription = "Match Snapshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Snapshot captured! Saved with the match history.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPhotoModal = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun ScoreButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    testTag: String = "score_button_${text.lowercase()}",
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}

@Composable
fun BallTickerCircle(delivery: BallDelivery) {
    val bg = when {
        delivery.isWicket -> CricketLeatherRed
        delivery.runsOffBat == 4 -> Color(0xFF1E3A8A)
        delivery.runsOffBat == 6 -> Color(0xFF0D9488)
        delivery.extraType != ExtraType.NONE -> BallAmber
        delivery.runsOffBat == 0 -> Color(0xFF374151)
        else -> TurfGreenDark
    }

    val label = when {
        delivery.isWicket -> "W"
        delivery.extraType == ExtraType.WIDE -> "WD"
        delivery.extraType == ExtraType.NO_BALL -> "NB"
        delivery.extraType == ExtraType.BYE -> "B"
        delivery.extraType == ExtraType.LEG_BYE -> "LB"
        else -> "${delivery.runsOffBat}"
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
