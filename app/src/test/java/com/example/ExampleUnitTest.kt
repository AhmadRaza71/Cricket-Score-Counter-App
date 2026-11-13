package com.example

import com.example.engine.StreetCricketEngine
import com.example.model.BallDelivery
import com.example.model.DismissalType
import com.example.model.ExtraType
import com.example.model.Player
import com.example.model.StreetRuleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSixIsOutRule() {
        val players = listOf(Player(name = "Ali"), Player(name = "Hamza"))
        val bowlers = listOf(Player(name = "Rohit"))
        val rules = StreetRuleConfig(sixIsOut = true)

        val deliveries = listOf(
            BallDelivery(
                inningsNumber = 1,
                overNumber = 0,
                ballInOver = 1,
                isLegal = true,
                strikerName = "Ali",
                nonStrikerName = "Hamza",
                bowlerName = "Rohit",
                runsOffBat = 0,
                extraRuns = 0,
                extraType = ExtraType.NONE,
                isWicket = true,
                dismissalType = DismissalType.DIRECT_SIX_OUT,
                dismissedPlayerName = "Ali"
            )
        )

        val result = StreetCricketEngine.calculateInnings(
            inningsNumber = 1,
            battingTeamName = "Warriors",
            bowlingTeamName = "Tigers",
            battingPlayers = players,
            bowlingPlayers = bowlers,
            oversLimit = 5,
            target = null,
            rules = rules,
            deliveries = deliveries,
            selectedStrikerName = "Ali",
            selectedNonStrikerName = "Hamza",
            selectedBowlerName = "Rohit"
        )

        assertEquals(1, result.wickets)
        val ali = result.batters.first { it.playerName == "Ali" }
        assertTrue(ali.isOut)
        assertEquals("Hit-and-Out (Direct Six)", ali.dismissalText)
    }

    @Test
    fun testLegalDeliveriesAndExtras() {
        val players = listOf(Player(name = "Ali"), Player(name = "Hamza"))
        val bowlers = listOf(Player(name = "Rohit"))
        val rules = StreetRuleConfig(sixIsOut = false)

        val deliveries = listOf(
            BallDelivery(
                inningsNumber = 1,
                overNumber = 0,
                ballInOver = 1,
                isLegal = true,
                strikerName = "Ali",
                nonStrikerName = "Hamza",
                bowlerName = "Rohit",
                runsOffBat = 4,
                extraRuns = 0,
                extraType = ExtraType.NONE,
                isWicket = false,
                dismissalType = DismissalType.NONE
            ),
            BallDelivery(
                inningsNumber = 1,
                overNumber = 0,
                ballInOver = 2,
                isLegal = false,
                strikerName = "Ali",
                nonStrikerName = "Hamza",
                bowlerName = "Rohit",
                runsOffBat = 0,
                extraRuns = 1,
                extraType = ExtraType.WIDE,
                isWicket = false,
                dismissalType = DismissalType.NONE
            )
        )

        val result = StreetCricketEngine.calculateInnings(
            inningsNumber = 1,
            battingTeamName = "Warriors",
            bowlingTeamName = "Tigers",
            battingPlayers = players,
            bowlingPlayers = bowlers,
            oversLimit = 5,
            target = null,
            rules = rules,
            deliveries = deliveries,
            selectedStrikerName = "Ali",
            selectedNonStrikerName = "Hamza",
            selectedBowlerName = "Rohit"
        )

        assertEquals(5, result.totalRuns) // 4 + 1
        assertEquals(1, result.legalBalls)
        assertEquals(1, result.wides)
    }
}
