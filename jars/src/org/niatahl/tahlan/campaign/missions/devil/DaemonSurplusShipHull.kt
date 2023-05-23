package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipHullSpecAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.SurplusShipHull
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs.DAEMONS
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO
import kotlin.math.roundToInt

class DaemonSurplusShipHull : SurplusShipHull() {
    override fun create(createdAt: MarketAPI, barEvent: Boolean): Boolean {

        val person = person ?: return false
        val market = person.market ?: return false
        if (!Misc.getAllowedRecoveryTags().contains(Tags.AUTOMATED_RECOVERABLE)) return false
        if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered")) return false
        if (person.relToPlayer.rel < 0.5f) return false
        if (!setPersonMissionRef(person, "\$sShip_ref")) return false

        if (barEvent) {
            setGiverIsPotentialContactOnSuccess()
        }

        val params = ShipPickParams(ShipPickMode.PRIORITY_THEN_ALL)
        val daemonFac = Global.getSector().getFaction(DAEMONS)
        val role = pickRole(getQuality(), daemonFac, person.importance, genRandom)
        var variant: ShipVariantAPI? = null
        for (i in 0..9) {
            val picks = daemonFac.pickShip(role, params, null, genRandom)
            if (picks.isEmpty()) return false
            val variantId = picks[0].variantId
            variant = Global.getSettings().getVariant(variantId)
            variant = Global.getSettings().getVariant(variant.hullSpec.hullId + "_Hull").clone()
            val spec = variant.hullSpec
            if (spec.hints.contains(ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE) && !spec.tags.contains(Tags.AUTOMATED_RECOVERABLE)) {
                variant = null
                continue
            }
            break
        }
        if (variant == null) return false
        member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant)
        assignShipName(member, LEGIO)
        member.crewComposition.crew = 100000f
        member.repairTracker.cr = 0.7f
        price = if (BASE_PRICE_MULT_DMN == 1f) {
            variant.hullSpec.baseValue.roundToInt()
        } else {
            getRoundNumber(variant.hullSpec.baseValue * BASE_PRICE_MULT_DMN)
        }
        setRepFactionChangesNone()
        setRepPersonChangesNone()
        return true
    }

    companion object {
        var BASE_PRICE_MULT_DMN = 1f
    }
}