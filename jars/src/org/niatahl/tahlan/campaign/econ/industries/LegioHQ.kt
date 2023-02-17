package org.niatahl.tahlan.campaign.econ.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.Industry.IndustryTooltipMode
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.characters.FullName.Gender
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory.PatrolType
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.PatrolAssignmentAIV4
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.*
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Pair
import com.fs.starfarer.api.util.WeightedRandomPicker
import kotlin.math.roundToInt

class LegioHQ : BaseIndustry(), RouteFleetSpawner, FleetEventListener {
    override fun isHidden(): Boolean {
        return market.factionId != "tahlan_legioinfernalis"
    }

    override fun isFunctional(): Boolean {
        return super.isFunctional() && market.factionId == "tahlan_legioinfernalis"
    }

    override fun apply() {
        super.apply(true)
        val size = market.size
        demand(Commodities.SUPPLIES, size - 1)
        demand(Commodities.FUEL, size - 1)
        demand(Commodities.SHIPS, size - 1)
        supply(Commodities.CREW, size)
        demand(Commodities.HAND_WEAPONS, size)
        supply(Commodities.MARINES, size)
        val deficit = getMaxDeficit(Commodities.HAND_WEAPONS)
        applyDeficitToProduction(1, deficit, Commodities.MARINES)
        modifyStabilityWithBaseMod()
        val memory = market.memoryWithoutUpdate
        Misc.setFlagWithReason(memory, MemFlags.MARKET_PATROL, modId, true, -1f)
        Misc.setFlagWithReason(memory, MemFlags.MARKET_MILITARY, modId, true, -1f)
        if (!isFunctional) {
            supply.clear()
            unapply()
        }
    }

    override fun unapply() {
        super.unapply()
        val memory = market.memoryWithoutUpdate
        Misc.setFlagWithReason(memory, MemFlags.MARKET_PATROL, modId, false, -1f)
        Misc.setFlagWithReason(memory, MemFlags.MARKET_MILITARY, modId, false, -1f)
        unmodifyStabilityWithBaseMod()
    }

    override fun hasPostDemandSection(hasDemand: Boolean, mode: IndustryTooltipMode): Boolean {
        return mode != IndustryTooltipMode.NORMAL || isFunctional
    }

    override fun addPostDemandSection(tooltip: TooltipMakerAPI, hasDemand: Boolean, mode: IndustryTooltipMode) {
        if (mode != IndustryTooltipMode.NORMAL || isFunctional) {
            addStabilityPostDemandSection(tooltip, hasDemand, mode)
        }
    }

    override fun getBaseStabilityMod(): Int {
        return 2
    }

    override fun getNameForModifier(): String {
        return if (getSpec().name.contains("HQ")) {
            getSpec().name
        } else Misc.ucFirst(getSpec().name)
    }

    override fun getStabilityAffectingDeficit(): Pair<String, Int> {
        return getMaxDeficit(Commodities.SUPPLIES, Commodities.FUEL, Commodities.SHIPS, Commodities.HAND_WEAPONS)
    }

    override fun getCurrentImage(): String {
        return super.getCurrentImage()
    }

    override fun isDemandLegal(com: CommodityOnMarketAPI): Boolean {
        return true
    }

    override fun isSupplyLegal(com: CommodityOnMarketAPI): Boolean {
        return true
    }

    protected var tracker = IntervalUtil(
        Global.getSettings().getFloat("averagePatrolSpawnInterval") * 0.7f,
        Global.getSettings().getFloat("averagePatrolSpawnInterval") * 1.3f
    )
    protected var returningPatrolValue = 0f
    override fun buildingFinished() {
        super.buildingFinished()
        tracker.forceIntervalElapsed()
    }

    override fun upgradeFinished(previous: Industry) {
        super.upgradeFinished(previous)
        tracker.forceIntervalElapsed()
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        if (Global.getSector().economy.isSimMode) return
        if (!isFunctional) return
        val days = Global.getSector().clock.convertToDays(amount)
        var spawnRate = 1f
        val rateMult = market.stats.dynamic.getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifiedValue
        spawnRate *= rateMult
        var extraTime = 0f
        if (returningPatrolValue > 0) {
            // apply "returned patrols" to spawn rate, at a maximum rate of 1 interval per day
            val interval = tracker.intervalDuration
            extraTime = interval * days
            returningPatrolValue -= days
            if (returningPatrolValue < 0) returningPatrolValue = 0f
        }
        tracker.advance(days * spawnRate + extraTime)

        //tracker.advance(days * spawnRate * 100f);
        if (tracker.intervalElapsed()) {
            val sid = routeSourceId
            val light = getCount(PatrolType.FAST)
            val medium = getCount(PatrolType.COMBAT)
            val heavy = getCount(PatrolType.HEAVY)
            val maxLight = 3
            val maxMedium = 2
            val maxHeavy = 1
            val picker = WeightedRandomPicker<PatrolType>()
            picker.add(PatrolType.HEAVY, (maxHeavy - heavy).toFloat())
            picker.add(PatrolType.COMBAT, (maxMedium - medium).toFloat())
            picker.add(PatrolType.FAST, (maxLight - light).toFloat())
            if (picker.isEmpty) return
            val type = picker.pick()
            val custom = MilitaryBase.PatrolFleetData(type)
            val extra = OptionalFleetData(market)
            extra.fleetType = type.fleetType
            val route = RouteManager.getInstance().addRoute(sid, market, Misc.genRandomSeed(), extra, this, custom)
            val patrolDays = 35f + Math.random().toFloat() * 10f
            route.addSegment(RouteSegment(patrolDays, market.primaryEntity))
        }
    }

    override fun reportAboutToBeDespawnedByRouteManager(route: RouteData) {}
    override fun shouldRepeat(route: RouteData): Boolean {
        return false
    }

    fun getCount(vararg types: PatrolType): Int {
        var count = 0
        for (data in RouteManager.getInstance().getRoutesForSource(routeSourceId)) {
            if (data.custom is MilitaryBase.PatrolFleetData) {
                val custom = data.custom as MilitaryBase.PatrolFleetData
                for (type in types) {
                    if (type == custom.type) {
                        count++
                        break
                    }
                }
            }
        }
        return count
    }

    fun getMaxPatrols(type: PatrolType): Int {
        if (type == PatrolType.FAST) {
            return market.stats.dynamic.getMod(Stats.PATROL_NUM_LIGHT_MOD).computeEffective(0f).toInt()
        }
        if (type == PatrolType.COMBAT) {
            return market.stats.dynamic.getMod(Stats.PATROL_NUM_MEDIUM_MOD).computeEffective(0f).toInt()
        }
        return if (type == PatrolType.HEAVY) {
            market.stats.dynamic.getMod(Stats.PATROL_NUM_HEAVY_MOD).computeEffective(0f).toInt()
        } else 0
    }

    override fun shouldCancelRouteAfterDelayCheck(route: RouteData): Boolean {
        return false
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI, primaryWinner: CampaignFleetAPI, battle: BattleAPI) {}
    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI, reason: FleetDespawnReason, param: Any) {
        if (!isFunctional) return
        if (reason == FleetDespawnReason.REACHED_DESTINATION) {
            val route = RouteManager.getInstance().getRoute(routeSourceId, fleet)
            if (route.custom is MilitaryBase.PatrolFleetData) {
                val custom = route.custom as MilitaryBase.PatrolFleetData
                if (custom.spawnFP > 0) {
                    val fraction = (fleet.fleetPoints / custom.spawnFP).toFloat()
                    returningPatrolValue += fraction
                }
            }
        }
    }

    override fun spawnFleet(route: RouteData): CampaignFleetAPI? {
        val custom = route.custom as MilitaryBase.PatrolFleetData
        val type = custom.type
        val random = route.random
        var combat = 0f
        var tanker = 0f
        var freighter = 0f
        val fleetType = type.fleetType
        when (type) {
            PatrolType.FAST -> combat = (3f + random.nextFloat() * 2f).roundToInt() * 5f
            PatrolType.COMBAT -> {
                combat = (6f + random.nextFloat() * 3f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 5f
            }

            PatrolType.HEAVY -> {
                combat = (10f + random.nextFloat() * 5f).roundToInt() * 5f
                tanker = random.nextFloat().roundToInt() * 10f
                freighter = random.nextFloat().roundToInt() * 10f
            }
        }
        val params = FleetParamsV3(
            market,
            null,  // loc in hyper; don't need if have market
            "tahlan_legioelite",
            route.qualityOverride,  // quality override
            fleetType,
            combat,  // combatPts
            freighter,  // freighterPts 
            tanker,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f // qualityMod - since the Lion's Guard is in a different-faction market, counter that penalty
        )
        params.timestamp = route.timestamp
        params.random = random
        params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL
        val fleet = FleetFactoryV3.createFleet(params)
        if (fleet == null || fleet.isEmpty) return null
        fleet.setFaction(market.factionId, true)
        fleet.isNoFactionInName = true
        fleet.addEventListener(this)
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_PATROL_FLEET] = true
        if (type == PatrolType.FAST || type == PatrolType.COMBAT) {
            fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_CUSTOMS_INSPECTOR] = true
        }
        val postId = Ranks.POST_PATROL_COMMANDER
        val rankId = when (type) {
            PatrolType.FAST -> Ranks.SPACE_LIEUTENANT
            PatrolType.COMBAT -> Ranks.SPACE_COMMANDER
            PatrolType.HEAVY -> Ranks.SPACE_CAPTAIN
            else -> Ranks.SPACE_CAPTAIN
        }
        fleet.commander.postId = postId
        fleet.commander.rankId = rankId
        market.containingLocation.addEntity(fleet)
        fleet.facing = Math.random().toFloat() * 360f

        // Blackwatch is dommy mommies as Selkie would put it
        val blackwatch = Global.getSector().getFaction("tahlan_legioelite")
        fleet.membersWithFightersCopy
            .filter { !it.captain.isDefault && it.captain.gender == Gender.MALE }
            .forEach { member ->
                val newGal = blackwatch.createRandomPerson(Gender.FEMALE)
                member.captain.apply {
                    gender = Gender.FEMALE
                    portraitSprite = newGal.portraitSprite
                    name = newGal.name
                }
            }

        // this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
        fleet.setLocation(market.primaryEntity.location.x, market.primaryEntity.location.x)
        fleet.addScript(PatrolAssignmentAIV4(fleet, route))
        if (custom.spawnFP <= 0) {
            custom.spawnFP = fleet.fleetPoints
        }
        return fleet
    }

    private val routeSourceId: String
        get() = getMarket().id + "_" + "legioelite"

    override fun isAvailableToBuild(): Boolean {
        return false
    }

    override fun showWhenUnavailable(): Boolean {
        return false
    }
}