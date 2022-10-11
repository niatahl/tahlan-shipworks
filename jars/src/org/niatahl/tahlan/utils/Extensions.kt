package org.niatahl.tahlan.utils

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.events.CampaignEventTarget
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Misc.FleetFilter
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*

/**
 * Original by Wisp
 * Additions by Nia Tahl
 */

val Vector2f.ZERO
    get() = Misc.ZERO

fun String.ucFirst() = Misc.ucFirst(this)
fun String.lcFirst() = Misc.lcFirst(this)

fun String.replaceTokensFromMemory(memoryMap: Map<String, MemoryAPI>) =
    Misc.replaceTokensFromMemory(this, memoryMap)

fun SectorEntityToken.getDistance(to: SectorEntityToken) =
    Misc.getDistance(this, to)

fun SectorEntityToken.getDistanceLY(to: SectorEntityToken) =
    Misc.getDistanceLY(this, to)

fun Vector2f.getDistanceSq(to: Vector2f) =
    Misc.getDistanceSq(this, to)

fun Vector2f.getDistanceToPlayerLY() =
    Misc.getDistanceToPlayerLY(this)

fun SectorEntityToken.getDistanceToPlayerLY() =
    Misc.getDistanceToPlayerLY(this)

fun Vector2f.getDistanceLY(to: Vector2f) =
    Misc.getDistanceLY(this, to)

fun Float.getRounded() =
    Misc.getRounded(this)

fun Float.getRoundedValue() =
    Misc.getRoundedValue(this)

fun Float.getRoundedValueFloat() =
    Misc.getRoundedValueFloat(this)

fun Float.getRoundedValueMaxOneAfterDecimal() =
    Misc.getRoundedValueMaxOneAfterDecimal(this)

fun Float.logOfBase(num: Float) =
    Misc.logOfBase(this, num)

fun Vector2f.getPointAtRadius(r: Float) =
    Misc.getPointAtRadius(this, r)

fun Vector2f.getPointAtRadius(r: Float, random: Random) =
    Misc.getPointAtRadius(this, r, random)

fun Vector2f.getPointWithinRadius(r: Float, random: Random = Misc.random) =
    Misc.getPointWithinRadius(this, r, random)

fun Vector2f.getPointWithinRadiusUniform(r: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, r, random)

fun Vector2f.getPointWithinRadiusUniform(minR: Float, maxR: Float, random: Random) =
    Misc.getPointWithinRadiusUniform(this, minR, maxR, random)

fun CampaignFleetAPI.getSnapshotFPLost() =
    Misc.getSnapshotFPLost(this)

fun CampaignFleetAPI.getSnapshotMembersLost() =
    Misc.getSnapshotMembersLost(this)

fun CampaignEventTarget.startEvent(eventId: String, params: Any) =
    Misc.startEvent(this, eventId, params)

fun String.getAndJoined(strings: List<String>) =
    Misc.getAndJoined(strings)

fun String.getAndJoined(vararg strings: String) =
    Misc.getAndJoined(*strings)

fun String.getJoined(joiner: String, strings: List<String>) =
    Misc.getJoined(joiner, strings)

fun String.getJoined(joiner: String, vararg strings: String) =
    Misc.getJoined(joiner, *strings)

fun SectorEntityToken.findNearbyFleets(maxRange: Float, filter: FleetFilter) =
    Misc.findNearbyFleets(this, maxRange, filter)

fun StarSystemAPI.getFleetsInOrNearSystem() =
    Misc.getFleetsInOrNearSystem(this)

fun LocationAPI.getMarketsInLocation(factionId: String? = null) =
    if (factionId == null)
        Misc.getMarketsInLocation(this)
    else
        Misc.getMarketsInLocation(this, factionId)

fun LocationAPI.getBiggestMarketInLocation() =
    Misc.getBiggestMarketInLocation(this)

fun FactionAPI.getFactionMarkets(econGroup: String? = null) =
    if (econGroup == null)
        Misc.getFactionMarkets(this)
    else
        Misc.getFactionMarkets(this, econGroup)

fun Vector2f.getNearbyMarkets(distLY: Float) =
    Misc.getNearbyMarkets(this, distLY)

fun FactionAPI.getNumHostileMarkets(from: SectorEntityToken, maxDist: Float) =
    Misc.getNumHostileMarkets(this, from, maxDist)

fun SectorEntityToken.getNearbyStarSystems(maxRangeLY: Float) =
    Misc.getNearbyStarSystems(this, maxRangeLY)

fun SectorEntityToken.getNearbyStarSystem(maxRangeLY: Float) =
    Misc.getNearbyStarSystem(this, maxRangeLY)

fun SectorEntityToken.getNearestStarSystem() =
    Misc.getNearestStarSystem(this)

fun SectorEntityToken.getNearbyStarSystem() =
    Misc.getNearbyStarSystem(this)

fun SectorEntityToken.showRuleDialog(initialTrigger: String) =
    Misc.showRuleDialog(this, initialTrigger)

fun Vector2f.getAngleInDegreesStrict() =
    Misc.getAngleInDegreesStrict(this)

fun Vector2f.getAngleInDegreesStrict(to: Vector2f) =
    Misc.getAngleInDegreesStrict(this, to)

fun ClosedFloatingPointRange<Float>.random(): Float =
    (this.start + (this.endInclusive - this.start) * kotlin.random.Random.nextFloat())

fun Color.modify(red: Int = this.red, green: Int = this.green, blue: Int = this.blue, alpha: Int = this.alpha) =
    Color(red, green, blue, alpha)

fun Float.adjustToward(target: Float, byAmount: Float) =
    if (target > this) (this + byAmount).coerceAtMost(target) else (this - byAmount).coerceAtLeast(target)