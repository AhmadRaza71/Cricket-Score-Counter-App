package com.example.ui.summary

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchRecordEntity
import com.example.engine.CalculatedInnings
import com.example.ui.theme.BallAmber
import com.example.ui.theme.CricketLeatherRed
import com.example.ui.theme.TurfGreenDark
import com.example.ui.theme.TurfNeonGreen

@Composable
fun MatchSummaryScreen(
    match: MatchRecordEntity,
    firstInnings: CalculatedInnings,
    secondInnings: CalculatedInnings?,
    onNewMatch: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedInningsTab by remember { mutableIntStateOf(0) }

    // Determine Winner and Result Text
    val resultSummary = remember(firstInnings, secondInnings) {
        if (secondInnings != null) {
            val target = firstInnings.totalRuns + 1
            if (secondInnings.totalRuns >= target) {
                "${secondInnings.battingTeam} won by ${secondInnings.batters.size - secondInnings.wickets} wickets! 🏆"
            } else if (secondInnings.isInningsFinished) {
                if (firstInnings.totalRuns > secondInnings.totalRuns) {
                    val margin = firstInnings.totalRuns - secondInnings.totalRuns
                    "${firstInnings.battingTeam} won by $margin runs! 🏆"
                } else {
                    "Match Tied! 🤝"
                }
            } else {
                "2nd Innings in Progress"
            }
        } else {
            "1st Innings Completed"
        }
    }

    // Top performers
    val topBatter = remember(firstInnings, secondInnings) {
        val allBatters = firstInnings.batters + (secondInnings?.batters ?: emptyList())
        allBatters.maxByOrNull { it.runs }
    }
    val topBowler = remember(firstInnings, secondInnings) {
        val allBowlers = firstInnings.bowlers + (secondInnings?.bowlers ?: emptyList())
        allBowlers.maxByOrNull { it.wickets }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. RESULT BANNER & SHARE CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TurfNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = BallAmber,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = resultSummary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "📍 ${match.location.ifBlank { "Street Pitch" }} • 📅 ${match.matchDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Team Scores Side by Side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(firstInnings.battingTeam, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "${firstInnings.totalRuns}/${firstInnings.wickets}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("(${firstInnings.oversString} ov)", style = MaterialTheme.typography.labelSmall)
                        }

                        Text("VS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(secondInnings?.battingTeam ?: match.teamB, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (secondInnings != null) "${secondInnings.totalRuns}/${secondInnings.wickets}" else "--/--",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (secondInnings != null) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                            Text(
                                if (secondInnings != null) "(${secondInnings.oversString} ov)" else "Yet to bat",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shareable Summary Button
                    Button(
                        onClick = {
                            shareMatchScorecard(context, match, resultSummary, firstInnings, secondInnings)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurfNeonGreen,
                            contentColor = Color(0xFF003912)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Scorecard (WhatsApp / Text)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. TOP PERFORMERS HIGHLIGHT CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (topBatter != null && topBatter.runs > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏏 Top Batter", style = MaterialTheme.typography.labelMedium, color = TurfNeonGreen, fontWeight = FontWeight.Bold)
                            Text(topBatter.playerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${topBatter.runs} runs (${topBatter.ballsFaced}b)", style = MaterialTheme.typography.bodySmall)
                            Text("SR: ${String.format("%.1f", topBatter.strikeRate)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (topBowler != null && topBowler.wickets > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚾ Top Bowler", style = MaterialTheme.typography.labelMedium, color = BallAmber, fontWeight = FontWeight.Bold)
                            Text(topBowler.bowlerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${topBowler.wickets} wkts (${topBowler.runsConceded}r)", style = MaterialTheme.typography.bodySmall)
                            Text("Econ: ${String.format("%.1f", topBowler.economyRate)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 3. INNINGS DETAILED TABS
        item {
            TabRow(selectedTabIndex = selectedInningsTab) {
                Tab(
                    selected = selectedInningsTab == 0,
                    onClick = { selectedInningsTab = 0 },
                    text = { Text("1st Innings: ${firstInnings.battingTeam}") }
                )
                if (secondInnings != null) {
                    Tab(
                        selected = selectedInningsTab == 1,
                        onClick = { selectedInningsTab = 1 },
                        text = { Text("2nd Innings: ${secondInnings.battingTeam}") }
                    )
                }
            }
        }

        val displayedInnings = if (selectedInningsTab == 0) firstInnings else secondInnings ?: firstInnings

        // 4. BATTING SCORECARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Batting - ${displayedInnings.battingTeam}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Batter", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("R", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("B", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("4s", modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("6s", modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("SR", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    displayedInnings.batters.forEach { b ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(b.playerName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(b.dismissalText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${b.runs}", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${b.ballsFaced}", modifier = Modifier.weight(0.7f), fontSize = 12.sp)
                            Text("${b.fours}", modifier = Modifier.weight(0.6f), fontSize = 12.sp)
                            Text("${b.sixes}", modifier = Modifier.weight(0.6f), fontSize = 12.sp)
                            Text(String.format("%.1f", b.strikeRate), modifier = Modifier.weight(1f), fontSize = 11.sp)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    // Extras
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Extras:", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "Total: ${displayedInnings.wides + displayedInnings.noBalls + displayedInnings.byes + displayedInnings.legByes} " +
                                    "(wd ${displayedInnings.wides}, nb ${displayedInnings.noBalls}, b ${displayedInnings.byes}, lb ${displayedInnings.legByes})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // 5. BOWLING SCORECARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Bowling - ${displayedInnings.bowlingTeam}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bowler", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("O", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("R", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("W", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Econ", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    displayedInnings.bowlers.forEach { bowl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bowl.bowlerName, modifier = Modifier.weight(2f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(bowl.oversString, modifier = Modifier.weight(0.8f), fontSize = 12.sp)
                            Text("${bowl.runsConceded}", modifier = Modifier.weight(0.8f), fontSize = 12.sp)
                            Text("${bowl.wickets}", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CricketLeatherRed)
                            Text(String.format("%.1f", bowl.economyRate), modifier = Modifier.weight(1f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 6. ACTION BUTTONS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dashboard")
                }

                Button(
                    onClick = onNewMatch,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("New Match")
                }
            }
        }
    }
}

private fun shareMatchScorecard(
    context: Context,
    match: MatchRecordEntity,
    resultSummary: String,
    firstInnings: CalculatedInnings,
    secondInnings: CalculatedInnings?
) {
    val shareText = buildString {
        appendLine("🏏 GALI CRICKET MATCH SCORECARD 🏏")
        appendLine("📍 Location: ${match.location.ifBlank { "Street Pitch" }}")
        appendLine("📅 Date: ${match.matchDate}")
        appendLine("🏆 Result: $resultSummary")
        appendLine()
        appendLine("--- 1st Innings: ${firstInnings.battingTeam} ---")
        appendLine("Total: ${firstInnings.totalRuns}/${firstInnings.wickets} (${firstInnings.oversString} ov)")
        firstInnings.batters.filter { it.ballsFaced > 0 }.forEach { b ->
            appendLine("• ${b.playerName}: ${b.runs} (${b.ballsFaced}b, 4s:${b.fours}, 6s:${b.sixes}) - ${b.dismissalText}")
        }
        appendLine()
        if (secondInnings != null) {
            appendLine("--- 2nd Innings: ${secondInnings.battingTeam} ---")
            appendLine("Total: ${secondInnings.totalRuns}/${secondInnings.wickets} (${secondInnings.oversString} ov)")
            secondInnings.batters.filter { it.ballsFaced > 0 }.forEach { b ->
                appendLine("• ${b.playerName}: ${b.runs} (${b.ballsFaced}b, 4s:${b.fours}, 6s:${b.sixes}) - ${b.dismissalText}")
            }
            appendLine()
        }
        appendLine("Scored offline on Gali Cricket App ⚡")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Gali Cricket Match Result")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Match Scorecard"))
}
