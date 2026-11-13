package com.example.engine

import com.example.model.BallDelivery
import com.example.model.BatterScorecard
import com.example.model.BowlerScorecard
import com.example.model.DismissalType
import com.example.model.ExtraType
import com.example.model.FallOfWicket
import com.example.model.Player
import com.example.model.StreetRuleConfig

data class CalculatedInnings(
    val inningsNumber: Int,
    val battingTeam: String,
    val bowlingTeam: String,
    val totalRuns: Int,
    val wickets: Int,
    val legalBalls: Int,
    val oversString: String,
    val currentRunRate: Double,
    val target: Int? = null,
    val ballsRemaining: Int = 0,
    val runsNeeded: Int = 0,
    val requiredRunRate: Double = 0.0,
    val striker: BatterScorecard?,
    val nonStriker: BatterScorecard?,
    val currentBowler: BowlerScorecard?,
    val batters: List<BatterScorecard>,
    val bowlers: List<BowlerScorecard>,
    val fallOfWickets: List<FallOfWicket>,
    val wides: Int,
    val noBalls: Int,
    val byes: Int,
    val legByes: Int,
    val penaltyRuns: Int,
    val isOverComplete: Boolean,
    val isInningsFinished: Boolean,
    val finishReason: String = "",
    val isSoloBatting: Boolean = false // Last Man Standing solo
)

object StreetCricketEngine {

    fun calculateInnings(
        inningsNumber: Int,
        battingTeamName: String,
        bowlingTeamName: String,
        battingPlayers: List<Player>,
        bowlingPlayers: List<Player>,
        oversLimit: Int,
        target: Int?,
        rules: StreetRuleConfig,
        deliveries: List<BallDelivery>,
        selectedStrikerName: String?,
        selectedNonStrikerName: String?,
        selectedBowlerName: String?
    ): CalculatedInnings {
        val filteredDeliveries = deliveries.filter { it.inningsNumber == inningsNumber }

        var totalRuns = 0
        var totalWickets = 0
        var legalBalls = 0
        var wides = 0
        var noBalls = 0
        var byes = 0
        var legByes = 0
        var penaltyRuns = 0

        // Maps to track stats
        val batterStatsMap = mutableMapOf<String, BatterScorecard>()
        val bowlerStatsMap = mutableMapOf<String, BowlerScorecard>()
        val fowList = mutableListOf<FallOfWicket>()

        // Initialize all batting players in scorecard
        battingPlayers.forEach { p ->
            batterStatsMap[p.name] = BatterScorecard(playerName = p.name)
        }

        // Active pair and bowler tracking through timeline
        var curStriker = selectedStrikerName ?: battingPlayers.firstOrNull()?.name ?: "Striker"
        var curNonStriker: String? = selectedNonStrikerName ?: battingPlayers.getOrNull(1)?.name
        var curBowler = selectedBowlerName ?: bowlingPlayers.firstOrNull()?.name ?: "Bowler"

        for (del in filteredDeliveries) {
            val strikerName = del.strikerName
            val bowlerName = del.bowlerName

            // Batter scoring
            val curBatter = batterStatsMap[strikerName] ?: BatterScorecard(playerName = strikerName)
            var newRuns = curBatter.runs
            var newBallsFaced = curBatter.ballsFaced
            var newFours = curBatter.fours
            var newSixes = curBatter.sixes
            var newIsOut = curBatter.isOut
            var newDismissalText = curBatter.dismissalText

            // Bowler scoring
            val curBowl = bowlerStatsMap[bowlerName] ?: BowlerScorecard(bowlerName = bowlerName)
            var bLegal = curBowl.legalBalls
            var bRuns = curBowl.runsConceded
            var bWickets = curBowl.wickets
            var bWides = curBowl.wides
            var bNoBalls = curBowl.noBalls

            // Runs off bat
            if (del.runsOffBat > 0) {
                newRuns += del.runsOffBat
                totalRuns += del.runsOffBat
                bRuns += del.runsOffBat
                if (del.runsOffBat == 4) newFours++
                if (del.runsOffBat == 6) newSixes++
            }

            // Extras
            when (del.extraType) {
                ExtraType.WIDE -> {
                    wides += del.extraRuns
                    totalRuns += del.extraRuns
                    bRuns += del.extraRuns
                    bWides++
                }
                ExtraType.NO_BALL -> {
                    noBalls += del.extraRuns
                    totalRuns += del.extraRuns
                    bRuns += del.extraRuns
                    bNoBalls++
                }
                ExtraType.BYE -> {
                    byes += del.extraRuns
                    totalRuns += del.extraRuns
                }
                ExtraType.LEG_BYE -> {
                    legByes += del.extraRuns
                    totalRuns += del.extraRuns
                }
                ExtraType.NONE -> {}
            }

            // Legal ball count
            if (del.isLegal) {
                legalBalls++
                bLegal++
                newBallsFaced++
            }

            // Wicket
            if (del.isWicket) {
                totalWickets++
                val dismissedName = del.dismissedPlayerName ?: strikerName
                val dismissedBatter = batterStatsMap[dismissedName] ?: BatterScorecard(playerName = dismissedName)

                val dismissalDesc = when (del.dismissalType) {
                    DismissalType.BOWLED -> "b $bowlerName"
                    DismissalType.CAUGHT -> "c fielder b $bowlerName"
                    DismissalType.ONE_TIP_CATCH -> "c (one-tip) b $bowlerName"
                    DismissalType.DIRECT_SIX_OUT -> "Hit-and-Out (Direct Six)"
                    DismissalType.RUN_OUT -> "run out"
                    DismissalType.CAR_PENALTY_OUT -> "Penalty Out (Hit Car)"
                    DismissalType.WALL_PENALTY_OUT -> "Penalty Out (Hit Wall)"
                    DismissalType.DRAIN_OUT -> "Ball into Drain (Out)"
                    DismissalType.STUMPED -> "st fielder b $bowlerName"
                    DismissalType.HIT_WICKET -> "hit wicket b $bowlerName"
                    DismissalType.LBW -> "lbw b $bowlerName"
                    DismissalType.NONE -> "out"
                }

                batterStatsMap[dismissedName] = dismissedBatter.copy(
                    isOut = true,
                    dismissalText = dismissalDesc
                )

                if (del.dismissalType != DismissalType.RUN_OUT) {
                    bWickets++
                }

                val overStr = "${legalBalls / 6}.${legalBalls % 6}"
                fowList.add(
                    FallOfWicket(
                        wicketNumber = totalWickets,
                        teamRuns = totalRuns,
                        playerName = dismissedName,
                        overString = overStr
                    )
                )
            } else {
                batterStatsMap[strikerName] = curBatter.copy(
                    runs = newRuns,
                    ballsFaced = newBallsFaced,
                    fours = newFours,
                    sixes = newSixes,
                    isOut = newIsOut,
                    dismissalText = newDismissalText
                )
            }

            bowlerStatsMap[bowlerName] = curBowl.copy(
                legalBalls = bLegal,
                runsConceded = bRuns,
                wickets = bWickets,
                wides = bWides,
                noBalls = bNoBalls
            )
        }

        // Active striker / non-striker determination
        val totalPlayers = battingPlayers.size
        val maxWickets = if (rules.lastManStands) totalPlayers else (totalPlayers - 1).coerceAtLeast(1)
        val isAllOut = totalWickets >= maxWickets
        val totalOversBalls = oversLimit * 6
        val isOversDone = oversLimit > 0 && legalBalls >= totalOversBalls
        val isTargetReached = target != null && totalRuns >= target

        val isSoloBatting = rules.lastManStands && (totalWickets == totalPlayers - 1) && !isAllOut

        val isFinished = isAllOut || isOversDone || isTargetReached
        val finishReason = when {
            isTargetReached -> "$battingTeamName won by ${maxWickets - totalWickets} wickets"
            isAllOut -> "All Out ($totalRuns runs)"
            isOversDone -> "Overs completed ($totalRuns runs)"
            else -> ""
        }

        val crr = if (legalBalls > 0) (totalRuns.toDouble() / (legalBalls / 6.0)) else 0.0
        val ballsRemaining = (totalOversBalls - legalBalls).coerceAtLeast(0)
        val runsNeeded = if (target != null) (target - totalRuns).coerceAtLeast(0) else 0
        val rrr = if (target != null && ballsRemaining > 0) {
            (runsNeeded.toDouble() / (ballsRemaining / 6.0))
        } else 0.0

        // Find currently active striker & non-striker
        val activeStriker = batterStatsMap[curStriker]
        val activeNonStriker = if (isSoloBatting) null else curNonStriker?.let { batterStatsMap[it] }
        val activeBowler = bowlerStatsMap[curBowler] ?: BowlerScorecard(bowlerName = curBowler)

        return CalculatedInnings(
            inningsNumber = inningsNumber,
            battingTeam = battingTeamName,
            bowlingTeam = bowlingTeamName,
            totalRuns = totalRuns,
            wickets = totalWickets,
            legalBalls = legalBalls,
            oversString = "${legalBalls / 6}.${legalBalls % 6}",
            currentRunRate = crr,
            target = target,
            ballsRemaining = ballsRemaining,
            runsNeeded = runsNeeded,
            requiredRunRate = rrr,
            striker = activeStriker,
            nonStriker = activeNonStriker,
            currentBowler = activeBowler,
            batters = battingPlayers.map { p -> batterStatsMap[p.name] ?: BatterScorecard(p.name) },
            bowlers = bowlerStatsMap.values.toList(),
            fallOfWickets = fowList,
            wides = wides,
            noBalls = noBalls,
            byes = byes,
            legByes = legByes,
            penaltyRuns = penaltyRuns,
            isOverComplete = legalBalls > 0 && (legalBalls % 6 == 0),
            isInningsFinished = isFinished,
            finishReason = finishReason,
            isSoloBatting = isSoloBatting
        )
    }
}
