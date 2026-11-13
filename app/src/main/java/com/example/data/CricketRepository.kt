package com.example.data

import com.example.model.BowlingStyle
import com.example.model.RulePreset
import com.example.model.StreetRuleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CricketRepository(private val dao: CricketDao) {

    val activeMatchFlow: Flow<MatchRecordEntity?> = dao.getLatestActiveMatch()
    val allMatchesFlow: Flow<List<MatchRecordEntity>> = dao.getAllMatches()
    val presetsFlow: Flow<List<RulePreset>> = dao.getAllPresets().map { list ->
        if (list.isEmpty()) {
            getDefaultPresets()
        } else {
            list.map { entity ->
                RulePreset(
                    id = entity.id,
                    presetName = entity.presetName,
                    description = entity.description,
                    config = JsonUtils.deserializeRules(entity.configJson),
                    isDefault = entity.isDefault
                )
            }
        }
    }

    suspend fun saveMatch(match: MatchRecordEntity) {
        dao.insertMatch(match)
    }

    suspend fun updateMatch(match: MatchRecordEntity) {
        dao.updateMatch(match)
    }

    suspend fun getMatch(id: String): MatchRecordEntity? {
        return dao.getMatchById(id)
    }

    suspend fun deleteMatch(id: String) {
        dao.deleteDeliveriesForMatch(id)
        dao.deleteMatch(id)
    }

    suspend fun recordDelivery(delivery: DeliveryEntity) {
        dao.insertDelivery(delivery)
    }

    suspend fun removeDelivery(deliveryId: String) {
        dao.deleteDeliveryById(deliveryId)
    }

    fun getDeliveriesForMatch(matchId: String): Flow<List<DeliveryEntity>> {
        return dao.getDeliveriesForMatch(matchId)
    }

    suspend fun getDeliveriesSync(matchId: String): List<DeliveryEntity> {
        return dao.getDeliveriesForMatchSync(matchId)
    }

    suspend fun savePreset(preset: RulePreset) {
        val entity = PresetEntity(
            id = preset.id,
            presetName = preset.presetName,
            description = preset.description,
            configJson = JsonUtils.serializeRules(preset.config),
            isDefault = preset.isDefault
        )
        dao.insertPreset(entity)
    }

    suspend fun deletePreset(id: String) {
        dao.deletePresetById(id)
    }

    companion object {
        fun getDefaultPresets(): List<RulePreset> {
            return listOf(
                RulePreset(
                    id = "preset_gali_v1",
                    presetName = "Gali Rules v1",
                    description = "Direct Six is Out, One-Tip catch allowed, Hit parked car = -2 runs, Drain is Out",
                    config = StreetRuleConfig(
                        sixIsOut = true,
                        oneTipOneHand = true,
                        bowlingStyle = BowlingStyle.OVERARM,
                        lastManStands = true,
                        carRuleActive = true,
                        carPenaltyRuns = -2,
                        carIsOut = false,
                        drainRuleActive = true,
                        drainIsOut = true,
                        wallRuleActive = false
                    ),
                    isDefault = true
                ),
                RulePreset(
                    id = "preset_rooftop",
                    presetName = "Rooftop / Terrace",
                    description = "Strict hit-and-out, Underarm only, Ball over ledge is out & lost ball",
                    config = StreetRuleConfig(
                        sixIsOut = true,
                        oneTipOneHand = true,
                        bowlingStyle = BowlingStyle.UNDERARM,
                        lastManStands = true,
                        carRuleActive = false,
                        drainRuleActive = true,
                        drainIsOut = true,
                        wallRuleActive = true,
                        wallRunsAwarded = 1
                    ),
                    isDefault = true
                ),
                RulePreset(
                    id = "preset_park_standard",
                    presetName = "Park / Ground Standard",
                    description = "Overarm bowling, Six is 6 runs, Normal ICC catch rules, uneven team handicap",
                    config = StreetRuleConfig(
                        sixIsOut = false,
                        oneTipOneHand = false,
                        bowlingStyle = BowlingStyle.OVERARM,
                        lastManStands = true,
                        carRuleActive = false,
                        drainRuleActive = false,
                        wallRuleActive = false
                    ),
                    isDefault = true
                )
            )
        }
    }
}
