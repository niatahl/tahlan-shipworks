package org.niatahl.tahlan.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_HULL
import org.niatahl.tahlan.utils.TahlanIDs.ILLUSTRIOUS_RESTORED
import kotlin.math.roundToInt

/**
 * Handles Louisa Ferre's one-time, discounted restoration of the recovered Illustrious.
 *
 * Offered (via rules.csv) only after the ship has been recovered and before the restoration has been
 * performed. Lives outside the mission because it is offered after IllustriousRecovery has completed.
 *
 * Sub-commands (first param):
 *  - "price"     : exposes price / ship-present / d-mod count into local memory for the offer text.
 *  - "doRestore" : strips all D-mods, deducts the discounted price, and latches the restored flag.
 */
class RestoreIllustrious : BaseCommandPlugin() {

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token?>,
        memoryMap: Map<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || memoryMap == null) return false
        val sub = params.getOrNull(0)?.getString(memoryMap) ?: return false
        val local = memoryMap[MemKeys.LOCAL] ?: return false

        val member = findIllustrious()
        val price = price()

        when (sub) {
            "price" -> {
                local.set("\$tahlan_illustrious_restorePrice", price, 0f)
                local.set("\$tahlan_illustrious_present", member != null, 0f)
                local.set("\$tahlan_illustrious_dmods", member?.let { DModManager.getNumDMods(it.variant) } ?: 0, 0f)
                return true
            }

            "doRestore" -> {
                if (member == null) {
                    dialog.textPanel.addPara(
                        "The Illustrious is no longer in your fleet.", Misc.getNegativeHighlightColor()
                    )
                    return true
                }
                val credits = Global.getSector().playerFleet.cargo.credits
                if (credits.get() < price) {
                    dialog.textPanel.addPara("You cannot afford the work.", Misc.getNegativeHighlightColor())
                    return true
                }
                val dmods = member.variant.hullMods.filter {
                    Global.getSettings().getHullModSpec(it).hasTag(Tags.HULLMOD_DMOD)
                }
                for (id in dmods) DModManager.removeDMod(member.variant, id)
                member.updateStats()
                credits.subtract(price.toFloat())
                AddRemoveCommodity.addCreditsLossText(price, dialog.textPanel)
                Global.getSector().memoryWithoutUpdate.set(ILLUSTRIOUS_RESTORED, true)
                return true
            }
        }
        return false
    }

    /** Discounted price: a fraction of the hull's base value (a significant credit sink, but a deal). */
    private fun price(): Int {
        val spec = Global.getSettings().getHullSpec(ILLUSTRIOUS_HULL)
        return (spec.baseValue * RESTORE_PRICE_MULT).roundToInt()
    }

    /** Finds the player's recovered Illustrious by hull id (gracefully null if it's gone). */
    private fun findIllustrious(): FleetMemberAPI? =
        Global.getSector().playerFleet.fleetData.membersListCopy
            .firstOrNull { it.hullSpec.hullId == ILLUSTRIOUS_HULL }

    companion object {
        // Significant discount: normal full restoration runs far higher than ~15% of base value.
        const val RESTORE_PRICE_MULT = 0.15f
    }
}
