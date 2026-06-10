package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.apache.log4j.Logger
import org.niatahl.tahlan.plugins.TahlanModPlugin
import org.niatahl.tahlan.utils.TahlanIDs
import org.niatahl.tahlan.utils.Utils.txt

/**
 * The payoff for handing a Domain-era planetkiller to the Legio Infernalis (see the
 * `tahlanPKGiveToLegio*` rows in rules.csv + [org.niatahl.tahlan.campaign.rulecmd.ArmPlanetkillerStrike]).
 *
 * Giving the Legio the weapon quietly enables their daemons (WITHOUT yet turning them hostile) and arms
 * a delayed strike: after ~3 months a Legio fleet sets out under a false independent transponder for a
 * player planetary colony — or, failing that, a world belonging to a faction the player is friendly with
 * and the Legio already hates — and detonates the planetkiller over it. That detonation is when the
 * Legio finally drops the mask and turns hostile (the delayed betrayal — see awakenLegioHostility). The
 * disguise unmasks to a positive Legio ID at close range, so an attentive player can intercept it first.
 *
 * Two pieces: a sector-level [PlanetkillerStrikeWatcher] that counts down and launches the fleet, and
 * a fleet-attached [PlanetkillerStrikeFleetAI] that drives the disguise / reveal / strike. Both are
 * serialized with the save (watcher via addScript, AI as part of the fleet), so no re-registration is
 * needed on load.
 */
object PKStrikeConfig {
    var DELAY_DAYS_MIN = 80f          // ~3 in-game months, randomized
    var DELAY_DAYS_MAX = 100f
    var RECHECK_DAYS = 30f            // no valid target at deadline → wait this long and retry rather than waste it
    var STRIKE_FP_BASE = 120f
    var STRIKE_FP_PER_CYCLE = 15f     // grows with elapsed campaign cycles past the standard start (206)
    var STRIKE_FP_MAX = 320f
    var REVEAL_RANGE = 700f           // player proximity that unmasks the disguised fleet
    var ARRIVAL_RANGE = 250f          // distance to the target colony that triggers the detonation
    var FULL_DESTROY = true           // a planetkiller annihilates; not a mere decivilization
    var SCORCHED_PLANET_TYPE = "barren-bombarded"  // the world itself is burned down to this
}

/**
 * Sector-level countdown. Re-appears on load via serialization; removes itself once the strike fleet
 * has launched (or is permanently un-launchable). State that must survive load lives in its own fields.
 */
class PlanetkillerStrikeWatcher(
    private var daysRemaining: Float = PKStrikeConfig.DELAY_DAYS_MIN
) : EveryFrameScript {

    private var done = false

    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (done) return
        daysRemaining -= Misc.getDays(amount)
        if (daysRemaining > 0f) return

        val target = pickTargetMarket()
        if (target == null) {
            // Nothing worth hitting yet — bide time rather than firing the one-shot into the void.
            daysRemaining = PKStrikeConfig.RECHECK_DAYS
            return
        }
        if (!launchStrike(target)) {
            daysRemaining = PKStrikeConfig.RECHECK_DAYS
            return
        }
        Global.getSector().memoryWithoutUpdate.set(TahlanIDs.PK_STRIKE_FIRED, true)
        done = true
    }

    private fun pickTargetMarket(): MarketAPI? {
        val sector = Global.getSector()

        // First choice: the player's own planetary colonies (the "you brought this on yourself" outcome).
        val playerPlanets = Misc.getPlayerMarkets(false).filter { isScorchableTarget(it) }
        if (playerPlanets.isNotEmpty()) return playerPlanets[Misc.random.nextInt(playerPlanets.size)]

        // No player colony to hit — the Legio picks a target of opportunity. Prioritize worlds of
        // factions the player is closest to AND that the Legio already hates (maximally spiteful).
        val legio = sector.getFaction(TahlanIDs.LEGIO)
        val player = sector.playerFaction
        val picker = WeightedRandomPicker<MarketAPI>()
        for (market in sector.economy.marketsCopy) {
            if (market.isPlayerOwned) continue
            if (!isScorchableTarget(market)) continue
            val faction = market.faction ?: continue
            if (faction.isPlayerFaction) continue
            if (!legio.isHostileTo(faction)) continue
            val rep = player.getRelationship(faction.id)           // -1..1
            val weight = (rep + 1f).coerceAtLeast(0.05f) * market.size.toFloat()
            picker.add(market, weight)
        }
        return picker.pick()
    }

    /** A valid planetkiller target: a non-hidden, non-story-protected world we can actually scorch. */
    private fun isScorchableTarget(market: MarketAPI): Boolean {
        if (market.isHidden) return false
        if (market.memoryWithoutUpdate.getBoolean(DecivTracker.NO_DECIV_KEY)) return false
        val planet = market.primaryEntity as? PlanetAPI ?: return false
        return !planet.isStar && !planet.isGasGiant
    }

    private fun launchStrike(target: MarketAPI): Boolean {
        val sector = Global.getSector()
        val cyclesElapsed = (sector.clock.cycle - 206).coerceAtLeast(0)
        val fp = (PKStrikeConfig.STRIKE_FP_BASE + PKStrikeConfig.STRIKE_FP_PER_CYCLE * cyclesElapsed)
            .coerceAtMost(PKStrikeConfig.STRIKE_FP_MAX)

        // Prefer launching from a Legio market; fall back to hyperspace near the target if Legio is fleet-only.
        val source = sector.economy.marketsCopy
            .filter { it.factionId == TahlanIDs.LEGIO && !it.isHidden }
            .maxByOrNull { it.size }

        val params = FleetParamsV3(
            source,
            source?.locationInHyperspace ?: target.locationInHyperspace,
            TahlanIDs.LEGIO, null, FleetTypes.TASK_FORCE,
            fp, 0f, 0f, 0f, 0f, 0f, 0.25f
        )
        params.averageSMods = 1

        val fleet = FleetFactoryV3.createFleet(params) ?: return false

        // Spawn into the world.
        val origin = source?.primaryEntity
        if (origin != null) {
            origin.containingLocation.addEntity(fleet)
            fleet.setLocation(origin.location.x, origin.location.y)
        } else {
            val hyper = sector.hyperspace
            val loc = target.locationInHyperspace
            hyper.addEntity(fleet)
            fleet.setLocation(loc.x, loc.y)
        }

        // Don the disguise: independent colors, transponder on, faceless.
        fleet.setFaction(Factions.INDEPENDENT, false)
        fleet.name = "Independent Convoy"
        fleet.setNoFactionInName(true)
        fleet.isTransponderOn = true
        fleet.memoryWithoutUpdate.set(TahlanIDs.PK_STRIKE_FLEET, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true)
        fleet.memoryWithoutUpdate.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true)
        fleet.memoryWithoutUpdate.set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true)
        // Deniable / off-the-books: Legio-aligned but without official mandate (like a colony-crisis
        // expedition), so fighting it doesn't turn the Legio faction hostile — intercepting only delays
        // the inevitable.
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true)

        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target.primaryEntity, 1000f,
            "transiting to ${target.starSystem?.nameWithLowercaseType ?: "the core"}")
        fleet.addScript(PlanetkillerStrikeFleetAI(fleet, target.id))
        fleet.addEventListener(PlanetkillerStrikeFleetListener())   // catches interception before the strike

        sector.campaignUI.addMessage(txt("pkstrike_warning"), Misc.getNegativeHighlightColor())
        LOG.info("Tahlan: planetkiller strike fleet launched at ${target.name} (${target.faction?.id}) (fp=${"%.0f".format(fp)})")
        return true
    }

    companion object {
        val LOG: Logger = Global.getLogger(PlanetkillerStrikeWatcher::class.java)!!
    }
}

/**
 * Drives a launched strike fleet: disguised transit → close-range reveal → detonation on arrival.
 * Serialized as part of the fleet, so it survives save/load without re-registration.
 */
class PlanetkillerStrikeFleetAI(
    private val fleet: CampaignFleetAPI,
    private val targetMarketId: String
) : EveryFrameScript {

    private var revealed = false
    private var struck = false
    private var done = false
    private val tick = IntervalUtil(0.2f, 0.3f)

    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (done) return
        if (!fleet.isAlive) { done = true; return }

        val target = Global.getSector().economy.getMarket(targetMarketId)
        // Target lost (already destroyed/removed, or gained story protection) → call off the run.
        if (target == null || target.primaryEntity == null
            || target.memoryWithoutUpdate.getBoolean(DecivTracker.NO_DECIV_KEY)) {
            abort(); return
        }

        tick.advance(Misc.getDays(amount))
        if (!tick.intervalElapsed()) return
        if (fleet.battle != null) return   // never re-task mid-combat

        // Close-range positive ID drops the disguise (and makes it interceptable).
        if (!revealed) {
            val player = Global.getSector().playerFleet
            if (player != null && player.containingLocation == fleet.containingLocation
                && Misc.getDistance(player, fleet) < PKStrikeConfig.REVEAL_RANGE) {
                reveal()
            }
        }

        // Arrival over the colony → detonation.
        val tgt = target.primaryEntity
        if (tgt != null && fleet.containingLocation == tgt.containingLocation
            && Misc.getDistance(fleet, tgt) < PKStrikeConfig.ARRIVAL_RANGE) {
            executeStrike(target)
        }
    }

    private fun reveal() {
        revealed = true
        fleet.setFaction(TahlanIDs.LEGIO, true)
        fleet.setNoFactionInName(false)
        fleet.name = "Strike Group"
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true)
        Global.getSector().campaignUI.addMessage(txt("pkstrike_revealed"), Misc.getNegativeHighlightColor())
    }

    private fun executeStrike(target: MarketAPI) {
        if (struck) return
        struck = true
        // Mark BEFORE the kill so the interception listener can tell "struck" from "stopped in time".
        fleet.memoryWithoutUpdate.set(STRUCK_KEY, true)
        if (!revealed) reveal()
        DecivTracker.decivilize(target, PKStrikeConfig.FULL_DESTROY, true)
        scorchPlanet(target)
        // The gift is spent — now the Legio drops the mask for good and turns on everyone.
        TahlanModPlugin.awakenLegioHostility()
        Global.getSector().campaignUI.addMessage(
            String.format(txt("pkstrike_struck"), target.name), Misc.getNegativeHighlightColor())
        abort()
    }

    /**
     * A planetkiller does not merely depopulate — it kills the world. [DecivTracker.decivilize] leaves
     * the planet's type untouched (a terran world keeps its blue oceans, just with ruins), so burn it
     * down to an irradiated, airless husk. Skips stations / stars / gas giants (nothing to scorch).
     */
    private fun scorchPlanet(market: MarketAPI) {
        val planet = market.primaryEntity as? PlanetAPI ?: return
        if (planet.isStar || planet.isGasGiant) return
        planet.changeType(PKStrikeConfig.SCORCHED_PLANET_TYPE, Misc.random)
        market.removeCondition(Conditions.HABITABLE)
        if (!market.hasCondition(Conditions.IRRADIATED)) market.addCondition(Conditions.IRRADIATED)
        if (!market.hasCondition(Conditions.NO_ATMOSPHERE)) market.addCondition(Conditions.NO_ATMOSPHERE)
    }

    /** Send the fleet off to despawn (post-strike retreat, or aborted run). */
    private fun abort() {
        done = true
        fleet.clearAssignments()
        val anchor = fleet.starSystem?.jumpPoints?.firstOrNull()
            ?: fleet.starSystem?.center
            ?: fleet.containingLocation?.createToken(fleet.location.x, fleet.location.y)
        if (anchor != null) {
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, anchor, 1000f)
        } else {
            fleet.containingLocation?.removeEntity(fleet)
        }
    }

    companion object {
        const val STRUCK_KEY = "\$tahlan_pkStruck"
    }
}

/**
 * Attached to the strike fleet at launch. If the fleet is destroyed in battle BEFORE its planetkiller
 * detonates, the strike has been intercepted — acknowledge it. (Only the player can engage it: it runs
 * disguised/ignored by other fleets, and reveals to hostile-Legio only at the player's proximity.)
 */
class PlanetkillerStrikeFleetListener : FleetEventListener {
    override fun reportBattleOccurred(fleet: CampaignFleetAPI, primaryWinner: CampaignFleetAPI?, battle: BattleAPI) {
        if (fleet.isAlive) return  // survived this battle — still inbound
        if (!fleet.memoryWithoutUpdate.getBoolean(PlanetkillerStrikeFleetAI.STRUCK_KEY)) {
            // Intercepted in time. Mark the strike resolved so the natural incursion suppression lifts -
            // the betrayal is delayed, never cancelled (TahlanModPlugin.reportEconomyMonthEnd).
            Global.getSector().memoryWithoutUpdate.set(TahlanIDs.PK_STRIKE_RESOLVED, true)
            Global.getSector().campaignUI.addMessage(txt("pkstrike_averted"), Misc.getPositiveHighlightColor())
        }
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI, reason: FleetDespawnReason, param: Any?) {}
}
