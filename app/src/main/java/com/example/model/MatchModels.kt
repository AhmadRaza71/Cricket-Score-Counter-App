package com.example.model

enum class MatchEnvironment(val displayName: String, val iconLabel: String) {
    STREET("Gali / Alley", "Narrow alley with building walls"),
    ROAD("Road / Street", "Paved street with parked cars"),
    ROOF("Rooftop / Terrace", "Enclosed roof with net/wall bounds"),
    GROUND("Park / Ground", "Open grassy or dirt area")
}

enum class PitchSize(val displayName: String, val distanceDescription: String) {
    SHORT("Short Pitch", "12-14 yards (Tight street)"),
    STANDARD("Standard Pitch", "18-20 yards (Medium ground)"),
    LONG("Full Pitch", "22 yards (Regulation)")
}

enum class BowlingStyle(val displayName: String) {
    OVERARM("Overarm Fast / Spin"),
    UNDERARM("Underarm Only (Street Style)")
}

enum class MatchFormat(val displayName: String) {
    LIMITED_OVERS("Limited Overs"),
    TEST_MATCH("Unlimited (All Out)")
}

enum class ExtraType(val displayName: String, val shortLabel: String, val isLegal: Boolean) {
    NONE("None", "", true),
    WIDE("Wide", "WD", false),
    NO_BALL("No Ball", "NB", false),
    BYE("Bye", "B", true),
    LEG_BYE("Leg Bye", "LB", true)
}

enum class DismissalType(val displayName: String, val isStreetSpecial: Boolean) {
    NONE("None", false),
    BOWLED("Bowled", false),
    CAUGHT("Caught", false),
    ONE_TIP_CATCH("One-Tip-One-Hand Catch", true),
    DIRECT_SIX_OUT("Direct Six Hit-and-Out", true),
    RUN_OUT("Run Out", false),
    CAR_PENALTY_OUT("Hit Car (Penalty Out)", true),
    WALL_PENALTY_OUT("Hit Neighbor Wall / Window", true),
    DRAIN_OUT("Ball into Drain (Lost Ball)", true),
    STUMPED("Stumped", false),
    HIT_WICKET("Hit Wicket", false),
    LBW("LBW", false)
}

enum class LandmarkType(val displayName: String, val defaultPenalty: Int, val defaultIsOut: Boolean) {
    CAR("Parked Car", -2, false),
    WALL("House Wall", 0, false),
    WINDOW("Glass Window", -5, true),
    DRAIN("Open Drain / Nala", 0, true),
    TREE("Tree Branch", 1, false),
    CUSTOM("Custom Obstacle", 0, false)
}

data class LandmarkMarker(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val x: Float, // Normalized 0f..1f
    val y: Float, // Normalized 0f..1f
    val type: LandmarkType,
    val penaltyRuns: Int = 0,
    val isOut: Boolean = false
)

data class BoundaryPoint(
    val x: Float, // Normalized 0f..1f
    val y: Float  // Normalized 0f..1f
)

data class PitchMapConfig(
    val environment: MatchEnvironment = MatchEnvironment.STREET,
    val pitchSize: PitchSize = PitchSize.SHORT,
    val pitchCenterX: Float = 0.5f,
    val pitchCenterY: Float = 0.5f,
    val pitchRotation: Float = 0f,
    val boundaryPolygon: List<BoundaryPoint> = listOf(
        BoundaryPoint(0.15f, 0.15f),
        BoundaryPoint(0.85f, 0.15f),
        BoundaryPoint(0.90f, 0.85f),
        BoundaryPoint(0.10f, 0.85f)
    ),
    val landmarks: List<LandmarkMarker> = emptyList()
)

data class StreetRuleConfig(
    val sixIsOut: Boolean = true,
    val oneTipOneHand: Boolean = true,
    val bowlingStyle: BowlingStyle = BowlingStyle.OVERARM,
    val lastManStands: Boolean = true, // Last batter can continue batting alone
    val widePenaltyRuns: Int = 1,
    val noBallPenaltyRuns: Int = 1,
    val freeHitOnNoBall: Boolean = false,
    val carRuleActive: Boolean = true,
    val carPenaltyRuns: Int = -2,
    val carIsOut: Boolean = false,
    val drainRuleActive: Boolean = true,
    val drainIsOut: Boolean = true,
    val drainPenaltyRuns: Int = 0,
    val wallRuleActive: Boolean = false,
    val wallRunsAwarded: Int = 1,
    val wallIsOut: Boolean = false,
    val maxOversPerBowler: Int = 2
)

data class Player(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val isCaptain: Boolean = false
)

data class BallDelivery(
    val deliveryId: String = java.util.UUID.randomUUID().toString(),
    val inningsNumber: Int,
    val overNumber: Int,
    val ballInOver: Int,
    val isLegal: Boolean,
    val strikerName: String,
    val nonStrikerName: String,
    val bowlerName: String,
    val runsOffBat: Int,
    val extraRuns: Int,
    val extraType: ExtraType,
    val isWicket: Boolean,
    val dismissalType: DismissalType,
    val dismissedPlayerName: String? = null,
    val landmarkHitName: String? = null,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class BatterScorecard(
    val playerName: String,
    val runs: Int = 0,
    val ballsFaced: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val dismissalText: String = "not out"
) {
    val strikeRate: Double
        get() = if (ballsFaced > 0) (runs.toDouble() / ballsFaced) * 100.0 else 0.0
}

data class BowlerScorecard(
    val bowlerName: String,
    val legalBalls: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val maidens: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0
) {
    val oversString: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val economyRate: Double
        get() = if (legalBalls > 0) (runsConceded.toDouble() / (legalBalls / 6.0)) else 0.0
}

data class FallOfWicket(
    val wicketNumber: Int,
    val teamRuns: Int,
    val playerName: String,
    val overString: String
)

data class RulePreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val presetName: String,
    val description: String,
    val config: StreetRuleConfig,
    val isDefault: Boolean = false
)
