package com.example.data

import com.example.model.BoundaryPoint
import com.example.model.BowlingStyle
import com.example.model.LandmarkMarker
import com.example.model.LandmarkType
import com.example.model.MatchEnvironment
import com.example.model.PitchMapConfig
import com.example.model.PitchSize
import com.example.model.Player
import com.example.model.StreetRuleConfig
import org.json.JSONArray
import org.json.JSONObject

object JsonUtils {
    fun serializePlayers(players: List<Player>): String {
        val array = JSONArray()
        players.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("isCaptain", p.isCaptain)
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializePlayers(json: String): List<Player> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<Player>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Player(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Player ${i + 1}"),
                        isCaptain = obj.optBoolean("isCaptain", false)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun serializeRules(rules: StreetRuleConfig): String {
        val obj = JSONObject()
        obj.put("sixIsOut", rules.sixIsOut)
        obj.put("oneTipOneHand", rules.oneTipOneHand)
        obj.put("bowlingStyle", rules.bowlingStyle.name)
        obj.put("lastManStands", rules.lastManStands)
        obj.put("widePenaltyRuns", rules.widePenaltyRuns)
        obj.put("noBallPenaltyRuns", rules.noBallPenaltyRuns)
        obj.put("freeHitOnNoBall", rules.freeHitOnNoBall)
        obj.put("carRuleActive", rules.carRuleActive)
        obj.put("carPenaltyRuns", rules.carPenaltyRuns)
        obj.put("carIsOut", rules.carIsOut)
        obj.put("drainRuleActive", rules.drainRuleActive)
        obj.put("drainIsOut", rules.drainIsOut)
        obj.put("drainPenaltyRuns", rules.drainPenaltyRuns)
        obj.put("wallRuleActive", rules.wallRuleActive)
        obj.put("wallRunsAwarded", rules.wallRunsAwarded)
        obj.put("wallIsOut", rules.wallIsOut)
        obj.put("maxOversPerBowler", rules.maxOversPerBowler)
        return obj.toString()
    }

    fun deserializeRules(json: String): StreetRuleConfig {
        if (json.isBlank()) return StreetRuleConfig()
        return try {
            val obj = JSONObject(json)
            StreetRuleConfig(
                sixIsOut = obj.optBoolean("sixIsOut", true),
                oneTipOneHand = obj.optBoolean("oneTipOneHand", true),
                bowlingStyle = try {
                    BowlingStyle.valueOf(obj.optString("bowlingStyle", BowlingStyle.OVERARM.name))
                } catch (_: Exception) { BowlingStyle.OVERARM },
                lastManStands = obj.optBoolean("lastManStands", true),
                widePenaltyRuns = obj.optInt("widePenaltyRuns", 1),
                noBallPenaltyRuns = obj.optInt("noBallPenaltyRuns", 1),
                freeHitOnNoBall = obj.optBoolean("freeHitOnNoBall", false),
                carRuleActive = obj.optBoolean("carRuleActive", true),
                carPenaltyRuns = obj.optInt("carPenaltyRuns", -2),
                carIsOut = obj.optBoolean("carIsOut", false),
                drainRuleActive = obj.optBoolean("drainRuleActive", true),
                drainIsOut = obj.optBoolean("drainIsOut", true),
                drainPenaltyRuns = obj.optInt("drainPenaltyRuns", 0),
                wallRuleActive = obj.optBoolean("wallRuleActive", false),
                wallRunsAwarded = obj.optInt("wallRunsAwarded", 1),
                wallIsOut = obj.optBoolean("wallIsOut", false),
                maxOversPerBowler = obj.optInt("maxOversPerBowler", 2)
            )
        } catch (_: Exception) {
            StreetRuleConfig()
        }
    }

    fun serializePitchConfig(config: PitchMapConfig): String {
        val obj = JSONObject()
        obj.put("environment", config.environment.name)
        obj.put("pitchSize", config.pitchSize.name)
        obj.put("pitchCenterX", config.pitchCenterX.toDouble())
        obj.put("pitchCenterY", config.pitchCenterY.toDouble())
        obj.put("pitchRotation", config.pitchRotation.toDouble())

        val polyArray = JSONArray()
        config.boundaryPolygon.forEach { pt ->
            val ptObj = JSONObject()
            ptObj.put("x", pt.x.toDouble())
            ptObj.put("y", pt.y.toDouble())
            polyArray.put(ptObj)
        }
        obj.put("boundaryPolygon", polyArray)

        val marksArray = JSONArray()
        config.landmarks.forEach { lm ->
            val lmObj = JSONObject()
            lmObj.put("id", lm.id)
            lmObj.put("name", lm.name)
            lmObj.put("x", lm.x.toDouble())
            lmObj.put("y", lm.y.toDouble())
            lmObj.put("type", lm.type.name)
            lmObj.put("penaltyRuns", lm.penaltyRuns)
            lmObj.put("isOut", lm.isOut)
            marksArray.put(lmObj)
        }
        obj.put("landmarks", marksArray)

        return obj.toString()
    }

    fun deserializePitchConfig(json: String): PitchMapConfig {
        if (json.isBlank()) return PitchMapConfig()
        return try {
            val obj = JSONObject(json)
            val env = try {
                MatchEnvironment.valueOf(obj.optString("environment", MatchEnvironment.STREET.name))
            } catch (_: Exception) { MatchEnvironment.STREET }
            val pSize = try {
                PitchSize.valueOf(obj.optString("pitchSize", PitchSize.SHORT.name))
            } catch (_: Exception) { PitchSize.SHORT }

            val polyList = mutableListOf<BoundaryPoint>()
            val polyArray = obj.optJSONArray("boundaryPolygon")
            if (polyArray != null) {
                for (i in 0 until polyArray.length()) {
                    val p = polyArray.getJSONObject(i)
                    polyList.add(BoundaryPoint(p.getDouble("x").toFloat(), p.getDouble("y").toFloat()))
                }
            }

            val marksList = mutableListOf<LandmarkMarker>()
            val marksArray = obj.optJSONArray("landmarks")
            if (marksArray != null) {
                for (i in 0 until marksArray.length()) {
                    val m = marksArray.getJSONObject(i)
                    val lType = try {
                        LandmarkType.valueOf(m.optString("type", LandmarkType.CUSTOM.name))
                    } catch (_: Exception) { LandmarkType.CUSTOM }
                    marksList.add(
                        LandmarkMarker(
                            id = m.optString("id", java.util.UUID.randomUUID().toString()),
                            name = m.optString("name", "Landmark"),
                            x = m.getDouble("x").toFloat(),
                            y = m.getDouble("y").toFloat(),
                            type = lType,
                            penaltyRuns = m.optInt("penaltyRuns", 0),
                            isOut = m.optBoolean("isOut", false)
                        )
                    )
                }
            }

            PitchMapConfig(
                environment = env,
                pitchSize = pSize,
                pitchCenterX = obj.optDouble("pitchCenterX", 0.5).toFloat(),
                pitchCenterY = obj.optDouble("pitchCenterY", 0.5).toFloat(),
                pitchRotation = obj.optDouble("pitchRotation", 0.0).toFloat(),
                boundaryPolygon = if (polyList.isNotEmpty()) polyList else PitchMapConfig().boundaryPolygon,
                landmarks = marksList
            )
        } catch (_: Exception) {
            PitchMapConfig()
        }
    }
}
