package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchRecordEntity(
    @PrimaryKey val id: String,
    val teamA: String,
    val teamB: String,
    val teamAPlayersJson: String,
    val teamBPlayersJson: String,
    val oversLimit: Int,
    val matchFormat: String,
    val environment: String,
    val location: String,
    val matchDate: String,
    val tossWinner: String,
    val tossElected: String,
    val status: String,
    val rulesJson: String,
    val pitchConfigJson: String,
    val firstInningsScore: Int = 0,
    val firstInningsWickets: Int = 0,
    val firstInningsBalls: Int = 0,
    val secondInningsScore: Int = 0,
    val secondInningsWickets: Int = 0,
    val secondInningsBalls: Int = 0,
    val winnerSummary: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "deliveries",
    indices = [Index(value = ["matchId", "inningsNumber"])]
)
data class DeliveryEntity(
    @PrimaryKey val id: String,
    val matchId: String,
    val inningsNumber: Int,
    val overNumber: Int,
    val ballInOver: Int,
    val isLegal: Boolean,
    val strikerName: String,
    val nonStrikerName: String,
    val bowlerName: String,
    val runsOffBat: Int,
    val extraRuns: Int,
    val extraType: String,
    val isWicket: Boolean,
    val dismissalType: String,
    val dismissedPlayerName: String?,
    val landmarkHitName: String?,
    val comment: String,
    val timestamp: Long
)

@Entity(tableName = "rule_presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val presetName: String,
    val description: String,
    val configJson: String,
    val isDefault: Boolean
)
