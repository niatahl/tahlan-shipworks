package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.niatahl.tahlan.utils.TahlanIDs.CORE_ARCHDAEMON
import org.niatahl.tahlan.utils.TahlanIDs.CORE_DAEMON
import kotlin.math.roundToInt

class DaemonCoreSale : HubMissionWithBarEvent() {
    enum class Stage {
        TALK_TO_PERSON, COMPLETED, FAILED
    }

    enum class Variation {
        BETA, BETA_WITH_GAMMA, ALPHA
    }

    var variation: Variation? = null
    var commodityId: String? = null
    var commodityId2: String? = null
    var price = 0
    override fun create(createdAt: MarketAPI, barEvent: Boolean): Boolean {
        val person = person ?: return false
        val market = person.market ?: return false
        if (!setPersonMissionRef(person, "\$tahlan_DaemonCoreSale_ref")) {
            return false
        }
        if (barEvent) {
            setGiverIsPotentialContactOnSuccess()
        }
        variation = pickVariation()
        if (variation == null) return false
        if (variation == Variation.ALPHA) {
            setRepPersonChangesLow()
            setRepFactionChangesVeryLow()
        } else {
            setRepPersonChangesVeryLow()
            setRepFactionChangesTiny()
        }
        return true
    }

    fun pickVariation(): Variation? {
        val rep = person.relToPlayer.level
        val picker = WeightedRandomPicker<Variation>(genRandom)
        when (rep) {
            RepLevel.COOPERATIVE -> {
                picker.add(Variation.ALPHA, 1f)
                picker.add(Variation.BETA_WITH_GAMMA, 2f)
            }

            RepLevel.FRIENDLY -> picker.add(Variation.BETA_WITH_GAMMA, 1f)
            RepLevel.WELCOMING -> picker.add(Variation.BETA, 1f)
            else -> return null
        }
        val variation = picker.pick()
        when (variation) {
            Variation.ALPHA -> commodityId = CORE_ARCHDAEMON
            Variation.BETA -> commodityId = CORE_DAEMON
            Variation.BETA_WITH_GAMMA -> {
                commodityId = CORE_DAEMON
                commodityId2 = Commodities.GAMMA_CORE
            }
            else -> return null
        }
        price = getSpec(commodityId).basePrice.roundToInt()
        if (commodityId2 != null) {
            price += getSpec(commodityId2).basePrice.roundToInt()
        }
        price *= COST_MULT.toInt()
        return variation
    }

    override fun callEvent(
        ruleId: String, dialog: InteractionDialogAPI,
        params: List<Misc.Token>, memoryMap: Map<String, MemoryAPI>
    ): Boolean {
        val action = params[0].getString(memoryMap)
        if ("transact" == action) {
            val cargo = Global.getSector().playerFleet.cargo
            cargo.addCommodity(commodityId, 1f)
            AddRemoveCommodity.addCommodityGainText(commodityId, 1, dialog.textPanel)
            if (commodityId2 != null) {
                cargo.addCommodity(commodityId2, 1f)
                AddRemoveCommodity.addCommodityGainText(commodityId2, 1, dialog.textPanel)
            }
            cargo.credits.subtract(price.toFloat())
            AddRemoveCommodity.addCreditsLossText(price, dialog.textPanel)
            return true
        }
        return super.callEvent(ruleId, dialog, params, memoryMap)
    }

    override fun updateInteractionDataImpl() {
        set("\$tahlan_devilDaemonCore_ref2", this)
        set("\$tahlan_devilDaemonCore_commodityId", commodityId)
        set("\$tahlan_devilDaemonCore_commodityName", getSpec(commodityId).name)
        if (commodityId2 != null) {
            set("\$tahlan_devilDaemonCore_commodityId2", commodityId2)
            set("\$tahlan_devilDaemonCore_commodityName2", getSpec(commodityId2).name)
        }
        set("\$tahlan_devilDaemonCore_price", price)
        set("\$tahlan_devilDaemonCore_variation", variation)
        set("\$tahlan_devilDaemonCore_manOrWoman", person.manOrWoman)
    }

    override fun accept(dialog: InteractionDialogAPI, memoryMap: Map<String, MemoryAPI>) {
        // if it's the local variation, there's no intel item and the commodity/credits etc is handled
        // in the rules csv. Need to abort here, though, so that mission ref is unset from person memory
        currentStage = Any() // so that the abort() assumes the mission was successful
        abort()
    }

    fun getSpec(commodityId: String?): CommoditySpecAPI {
        return Global.getSettings().getCommoditySpec(commodityId)
    }

    companion object {
        var COST_MULT = 2f
    }
}