package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CricketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecordEntity)

    @Update
    suspend fun updateMatch(match: MatchRecordEntity)

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: String): MatchRecordEntity?

    @Query("SELECT * FROM matches WHERE status != 'COMPLETED' ORDER BY createdAt DESC LIMIT 1")
    fun getLatestActiveMatch(): Flow<MatchRecordEntity?>

    @Query("SELECT * FROM matches ORDER BY createdAt DESC")
    fun getAllMatches(): Flow<List<MatchRecordEntity>>

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity)

    @Query("DELETE FROM deliveries WHERE id = :deliveryId")
    suspend fun deleteDeliveryById(deliveryId: String)

    @Query("SELECT * FROM deliveries WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getDeliveriesForMatch(matchId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE matchId = :matchId ORDER BY timestamp ASC")
    suspend fun getDeliveriesForMatchSync(matchId: String): List<DeliveryEntity>

    @Query("DELETE FROM deliveries WHERE matchId = :matchId")
    suspend fun deleteDeliveriesForMatch(matchId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Query("DELETE FROM rule_presets WHERE id = :presetId")
    suspend fun deletePresetById(presetId: String)

    @Query("SELECT * FROM rule_presets ORDER BY presetName ASC")
    fun getAllPresets(): Flow<List<PresetEntity>>
}
