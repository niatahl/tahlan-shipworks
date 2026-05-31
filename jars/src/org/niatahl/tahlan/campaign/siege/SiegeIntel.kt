package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs
import org.niatahl.tahlan.utils.Utils.txt
import java.awt.Color

class SiegeIntel(
    private val targetSystem: StarSystemAPI,
    private val primaryTarget: MarketAPI?
) : BaseIntelPlugin() {

    enum class SiegeStage { ENTRENCHED, STRAINED, FALTERING }

    enum class SiegeOutcome { BROKEN, LIFTED, SUCCEEDED }

    private var stage = SiegeStage.ENTRENCHED
    private var outcome: SiegeOutcome? = null
    private var playerBountyEarned = 0f
    private var bountyPaid = false

    // --- Called by SiegeManager ---

    fun updateStage(commandCR: Float) {
        val newStage = when {
            commandCR >= SiegeConfig.STAGE_ENTRENCHED_MIN_CR -> SiegeStage.ENTRENCHED
            commandCR >= SiegeConfig.STAGE_STRAINED_MIN_CR   -> SiegeStage.STRAINED
            else                                              -> SiegeStage.FALTERING
        }
        if (newStage != stage) {
            stage = newStage
            sendUpdateIfPlayerHasIntel(null, false)
        }
    }

    fun addPlayerBounty(amount: Float) {
        playerBountyEarned += amount
    }

    /** Resolve as one of BROKEN / LIFTED / SUCCEEDED. Pays out accrued bounty. Safe to call once. */
    fun resolve(how: SiegeOutcome) {
        if (outcome != null) return
        outcome = how
        if (playerBountyEarned > 0f && !bountyPaid) {
            bountyPaid = true
            Global.getSector().playerFleet.cargo.credits.add(playerBountyEarned)
        }
        sendUpdateIfPlayerHasIntel(null, false)
        endAfterDelay()
    }

    // --- BaseIntelPlugin overrides ---

    override fun isEnded(): Boolean = outcome != null

    override fun getIcon(): String =
        Global.getSettings().getSpriteName("intel", "raid") ?: "graphics/intel/raid.png"

    override fun getName(): String {
        val sysName = targetSystem.baseName
        return when (outcome) {
            SiegeOutcome.BROKEN    -> "${txt("siege_intel_title_broken")} ($sysName)"
            SiegeOutcome.LIFTED    -> "${txt("siege_intel_title_lifted")} ($sysName)"
            SiegeOutcome.SUCCEEDED -> "${txt("siege_intel_title_succeeded")} ($sysName)"
            null                   -> "${txt("siege_intel_title_active")} ($sysName)"
        }
    }

    override fun getSmallDescriptionTitle(): String = getName()

    private fun addNextStepText(info: TooltipMakerAPI, tc: Color, initPad: Float) {
        if (outcome != null) return
        val stageLabel = when (stage) {
            SiegeStage.ENTRENCHED -> txt("siege_stage_entrenched")
            SiegeStage.STRAINED   -> txt("siege_stage_strained")
            SiegeStage.FALTERING  -> txt("siege_stage_faltering")
        }
        info.addPara("${txt("siege_intel_status")}: $stageLabel", initPad,
            Misc.getHighlightColor(), stageLabel)
        if (playerBountyEarned > 0f) {
            info.addPara(txt("siege_intel_bounty_accrued"), 3f,
                Misc.getPositiveHighlightColor(), Misc.getDGSCredits(playerBountyEarned))
        }
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val legioColor: Color = Global.getSector().getFaction(TahlanIDs.LEGIO)?.baseUIColor
            ?: Misc.getHighlightColor()

        info.addPara(txt("siege_intel_desc"), legioColor, 0f)

        primaryTarget?.let { market ->
            info.addPara(txt("siege_intel_target"), 10f,
                Misc.getHighlightColor(),
                market.name,
                market.faction?.displayName ?: market.factionId)
        }

        when (outcome) {
            null -> addNextStepText(info, Misc.getTextColor(), 10f)
            SiegeOutcome.BROKEN    -> info.addPara(txt("siege_intel_resolved_broken"), 10f,
                Misc.getPositiveHighlightColor(), "broken")
            SiegeOutcome.LIFTED    -> info.addPara(txt("siege_intel_resolved_lifted"), 10f,
                Misc.getHighlightColor(), "lifted")
            SiegeOutcome.SUCCEEDED -> info.addPara(txt("siege_intel_resolved_succeeded"), 10f,
                Misc.getNegativeHighlightColor(), "succeeded")
        }

        if (bountyPaid && playerBountyEarned > 0f) {
            info.addPara(txt("siege_intel_bounty_paid"), 10f,
                Misc.getPositiveHighlightColor(), Misc.getDGSCredits(playerBountyEarned))
        }
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? = targetSystem.center

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_FLEET_DEPARTURES)
        tags.add(TahlanIDs.LEGIO)
        return tags
    }
}
