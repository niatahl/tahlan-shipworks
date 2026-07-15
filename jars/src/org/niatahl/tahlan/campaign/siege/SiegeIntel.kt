package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.StageIconSize
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs
import org.niatahl.tahlan.utils.Utils.txt
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Colony-crisis-style event renderer for a Legio siege. Extends [BaseEventIntel] for the progress
 * bar + stage markers + factor tables UI, but does NOT use the framework's progress driver:
 * [SiegeManager] is authoritative and pushes the bar value each besieging tick via [syncProgress].
 * Every factor returns `getProgress() == 0` and [reportEconomyTick] is a no-op, so the framework
 * never mutates `progress` on its own; [getMonthlyProgress] is overridden purely to feed the bar's
 * projected-monthly tooltip.
 *
 * The bar measures the siege's subjugation progress (0..100): rising progress is bad for the player
 * (red), and the player's fleet kills knock it back (green one-time factors). Command CR is demoted
 * from the headline stat to an all-progress brake factor.
 */
class SiegeIntel(
    private val targetSystem: StarSystemAPI,
    private val primaryTarget: MarketAPI?,
    private val hasNex: Boolean
) : BaseEventIntel() {

    // Kept (name + values) for save-compat: SiegeOutcome is referenced by SiegeManager, and the
    // stage ids below are serialized as EventStageData.id. SiegeStage's constants are the new
    // colony-crisis stage model (old ENTRENCHED/STRAINED/FALTERING are gone; a siege in flight across
    // the superclass change is migrated by SiegeManager.reconcileIntels, which replaces the stale
    // intel with a freshly-constructed one rather than trying to repair it in place).
    enum class SiegeStage { START, FOOTHOLD, STRANGLEHOLD, CLIMAX }

    enum class SiegeOutcome { BROKEN, LIFTED, SUCCEEDED }

    private var outcome: SiegeOutcome? = null
    private var playerBountyEarned = 0f
    private var bountyPaid = false

    // --- Display snapshot, pushed by syncProgress (manager state at the last tick) ---
    private var dispPressureMult = 1f
    private var dispCommandCR     = 1f
    private var dispIntensity     = 1f
    private var dispRaidFleets    = 0

    init {
        // Manager owns lifecycle (it calls intelManager.addIntel) — do NOT addIntel here.
        setup()
    }

    fun setup() {
        setMaxProgress(MAX_PROGRESS)
        getStages().clear()
        getFactors().clear()

        // START at 0 is mandatory: the bar dereferences the last active stage with no null guard.
        addStage(SiegeStage.START, 0)
        addStage(SiegeStage.FOOTHOLD, 33, StageIconSize.MEDIUM)
        addStage(SiegeStage.STRANGLEHOLD, 66, StageIconSize.MEDIUM)
        addStage(SiegeStage.CLIMAX, 100, true, StageIconSize.LARGE)

        addFactor(BlockadePressureFactor())
        addFactor(RaidSortiesFactor())
        addFactor(SiegeIntensityFactor())
        addFactor(CommandReadinessFactor())
    }

    // --- Called by SiegeManager ---

    /** Snapshot manager state and push the bar value. Cheap; called every besieging tick. */
    fun syncProgress(siege: SiegeManager.SiegeData) {
        dispPressureMult = siege.lastPressureMult
        dispCommandCR    = siege.commandCR
        dispIntensity    = siege.intensity
        dispRaidFleets   = siege.raidFleets.count { it.isAlive }
        setProgress(siege.captureProgress.roundToInt().coerceIn(0, MAX_PROGRESS))
    }

    fun addPlayerBounty(amount: Float) {
        playerBountyEarned += amount
    }

    /** Push a one-time fleet-kill factor (display-only; the manager already knocked the meter back). */
    fun addFleetKill(knockback: Float, isCommand: Boolean) {
        addFactor(FleetKillFactor(knockback, isCommand))
    }

    /** Resolve as one of BROKEN / LIFTED / SUCCEEDED. Pays out accrued bounty. Safe to call once. */
    fun resolve(how: SiegeOutcome) {
        if (outcome != null) return
        outcome = how
        if (how == SiegeOutcome.SUCCEEDED) setProgress(maxProgress)
        if (playerBountyEarned > 0f && !bountyPaid) {
            bountyPaid = true
            Global.getSector().playerFleet.cargo.credits.add(playerBountyEarned)
            // addMessage colors literal substrings (no %s subst), so format first then highlight.
            val creditStr = Misc.getDGSCredits(playerBountyEarned)
            Global.getSector().campaignUI.addMessage(
                txt("siege_bounty_message").format(creditStr),
                Misc.getTextColor(), creditStr, "", Misc.getPositiveHighlightColor(), Misc.getTextColor()
            )
        }
        sendUpdateIfPlayerHasIntel(null, false)
        endAfterDelay()
    }

    // --- BaseEventIntel overrides ---

    // Rising progress is the siege advancing — adverse to the player.
    override fun isEventProgressANegativeThingForThePlayer(): Boolean = true

    // The manager is authoritative; neutralize the framework's economy-tick auto-driver.
    override fun reportEconomyTick(iterIndex: Int) { /* no-op */ }

    // Feeds the bar's projected-monthly tooltip only (reportEconomyTick is a no-op, so this never
    // drives progress). Net monthly subjugation = base rate * strangle * CR brake over ~a month.
    override fun getMonthlyProgress(): Int =
        (SiegeConfig.CAPTURE_PROGRESS_PER_DAY_BASE * DAYS_PER_MONTH * dispPressureMult * dispCommandCR).roundToInt()

    override fun getFactionForUIColors(): FactionAPI =
        Global.getSector().getFaction(TahlanIDs.LEGIO) ?: super.getFactionForUIColors()

    override fun getIcon(): String =
        Global.getSettings().getSpriteName("intel", "hostilities")

    override fun getName(): String {
        val sysName = targetSystem.baseName
        return when (outcome) {
            SiegeOutcome.BROKEN    -> "${txt("siege_intel_title_broken")} ($sysName)"
            SiegeOutcome.LIFTED    -> "${txt("siege_intel_title_lifted")} ($sysName)"
            SiegeOutcome.SUCCEEDED -> "${txt("siege_intel_title_succeeded")} ($sysName)"
            null                   -> "${txt("siege_intel_title_active")} ($sysName)"
        }
    }

    override fun getStageIconImpl(stageId: Any?): String = when (stageId) {
        SiegeStage.CLIMAX -> Global.getSettings().getSpriteName("events", "hostile_activity")
        else              -> Global.getSettings().getSpriteName("events", "stage_unknown_bad")
    }

    override fun getStageLabel(stageId: Any?): String? = when (stageId) {
        SiegeStage.FOOTHOLD     -> txt("siege_stage2_foothold")
        SiegeStage.STRANGLEHOLD -> txt("siege_stage2_stranglehold")
        SiegeStage.CLIMAX       -> if (hasNex) txt("siege_stage2_climax_nex") else txt("siege_stage2_climax_nonex")
        else                    -> null
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipCreator? {
        val label = getStageLabel(stageId) ?: return null
        val tip = stageTip(stageId) ?: return null
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(label, Misc.getHighlightColor(), 0f)
                tooltip.addPara(tip, 10f)
            }
        }
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        stageDesc(stageId)?.let { info.addPara(it, 0f) }
    }

    /** Overall siege blurb, target, resolution line, and bounty — rendered under the stage prose. */
    override fun afterStageDescriptions(info: TooltipMakerAPI) {
        val opad = 10f
        val legioColor: Color = Global.getSector().getFaction(TahlanIDs.LEGIO)?.baseUIColor
            ?: Misc.getHighlightColor()
        info.addPara(txt("siege_intel_desc"), legioColor, opad)

        primaryTarget?.let { market ->
            info.addPara(txt("siege_intel_target"), opad,
                Misc.getHighlightColor(),
                market.name, market.faction?.displayName ?: market.factionId)
        }

        when (outcome) {
            null -> {}
            SiegeOutcome.BROKEN    -> info.addPara(txt("siege_intel_resolved_broken"), opad,
                Misc.getPositiveHighlightColor(), "broken")
            SiegeOutcome.LIFTED    -> info.addPara(txt("siege_intel_resolved_lifted"), opad,
                Misc.getHighlightColor(), "lifted")
            SiegeOutcome.SUCCEEDED -> {
                val key = if (hasNex) "siege_intel_resolved_succeeded" else "siege_intel_resolved_succeeded_nonex"
                info.addPara(txt(key), opad, Misc.getNegativeHighlightColor(), "succeeded")
            }
        }

        if (playerBountyEarned > 0f) {
            val key = if (bountyPaid) "siege_intel_bounty_paid" else "siege_intel_bounty_accrued"
            info.addPara(txt(key), opad, Misc.getPositiveHighlightColor(), Misc.getDGSCredits(playerBountyEarned))
        }
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? = targetSystem.center

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_FLEET_DEPARTURES)
        tags.add(TahlanIDs.LEGIO)
        return tags
    }

    // --- Save-compat for the BaseIntelPlugin -> BaseEventIntel superclass change ---

    /**
     * Whether this instance came through save-migration with BaseEventIntel's framework collections
     * left null. A siege serialized before this class extended BaseEventIntel has no <stages>/<factors>
     * in its XML, and XStream reconstructs the object without running the constructor's field
     * initializers — so the collections are null. They CANNOT be seeded from here: mod code runs under
     * a classloader that forbids reflection (SecurityException), the fields are protected with no setter,
     * and every mutator (addStage/addFactor) assumes a non-null list. The only fix is wholesale
     * replacement, which [SiegeManager.reconcileIntels] does on load. Reading the field is allowed, so
     * this is used both there (to detect the broken instance) and below (to fail safe until replaced).
     */
    fun isUninitialized(): Boolean = getStages() == null || getFactors() == null

    /** The system this intel tracks — used by [SiegeManager.reconcileIntels] to re-link sieges to
     *  their intel by a stable, serialized key rather than by a fragile object reference. */
    fun getTargetSystem(): StarSystemAPI = targetSystem

    // The engine's render entry point for the large event UI iterates `stages`/`factors` with no null
    // guard. If this is a not-yet-replaced migrated instance, skip rendering rather than NPE; the
    // reconcile pass on the first campaign tick removes/replaces it right after load.
    override fun createLargeDescription(panel: CustomPanelAPI, width: Float, height: Float) {
        if (isUninitialized()) return
        super.createLargeDescription(panel, width, height)
    }

    // --- Stage prose helpers ---

    private fun stageTip(stageId: Any?): String? = when (stageId) {
        SiegeStage.FOOTHOLD     -> txt("siege_stagetip_foothold")
        SiegeStage.STRANGLEHOLD -> txt("siege_stagetip_stranglehold")
        SiegeStage.CLIMAX       -> if (hasNex) txt("siege_stagetip_climax_nex") else txt("siege_stagetip_climax_nonex")
        else                    -> null
    }

    private fun stageDesc(stageId: Any?): String? = when (stageId) {
        SiegeStage.START        -> txt("siege_stagedesc_start")
        SiegeStage.FOOTHOLD     -> txt("siege_stagedesc_foothold")
        SiegeStage.STRANGLEHOLD -> txt("siege_stagedesc_stranglehold")
        SiegeStage.CLIMAX       -> if (hasNex) txt("siege_stagedesc_climax_nex") else txt("siege_stagedesc_climax_nonex")
        else                    -> null
    }

    private fun factorTip(key: String): TooltipCreator = object : BaseFactorTooltip() {
        override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
            tooltip.addPara(txt(key), 0f)
        }
    }

    // --- Factors (all display-only: getProgress() == 0; the manager pushes the bar) ---

    /** Base monthly push: the blockade strangling the market is the engine of the siege. */
    inner class BlockadePressureFactor : BaseEventFactor() {
        override fun getProgress(intel: BaseEventIntel): Int = 0
        override fun shouldShow(intel: BaseEventIntel): Boolean = true
        override fun getDesc(intel: BaseEventIntel): String = txt("siege_factor_blockade")
        override fun getProgressStr(intel: BaseEventIntel): String =
            "+" + (SiegeConfig.CAPTURE_PROGRESS_PER_DAY_BASE * DAYS_PER_MONTH * dispPressureMult).roundToInt()
        override fun getMainRowTooltip(intel: BaseEventIntel): TooltipCreator = factorTip("siege_factortip_blockade")
    }

    /** Raid sorties hammering the market — contextual pressure indicator (active raid wings). */
    inner class RaidSortiesFactor : BaseEventFactor() {
        override fun getProgress(intel: BaseEventIntel): Int = 0
        override fun shouldShow(intel: BaseEventIntel): Boolean = true
        override fun getDesc(intel: BaseEventIntel): String = txt("siege_factor_raids")
        override fun getProgressStr(intel: BaseEventIntel): String = "+$dispRaidFleets"
        override fun getMainRowTooltip(intel: BaseEventIntel): TooltipCreator = factorTip("siege_factortip_raids")
    }

    /** Overall expedition scale (intensity 0.5..2.0) — context, not a direct progress multiplier. */
    inner class SiegeIntensityFactor : BaseEventFactor() {
        override fun getProgress(intel: BaseEventIntel): Int = 0
        override fun shouldShow(intel: BaseEventIntel): Boolean = true
        override fun getDesc(intel: BaseEventIntel): String = txt("siege_factor_intensity")
        override fun getProgressStr(intel: BaseEventIntel): String = "×" + "%.1f".format(dispIntensity)
        override fun getMainRowTooltip(intel: BaseEventIntel): TooltipCreator = factorTip("siege_factortip_intensity")
    }

    /** Command CR brake: a battered command fleet subjugates more slowly (green when < 1). */
    inner class CommandReadinessFactor : BaseEventFactor() {
        override fun getProgress(intel: BaseEventIntel): Int = 0
        override fun shouldShow(intel: BaseEventIntel): Boolean = true
        override fun getAllProgressMult(intel: BaseEventIntel): Float = dispCommandCR
        override fun getDesc(intel: BaseEventIntel): String = txt("siege_factor_cr")
        override fun getProgressStr(intel: BaseEventIntel): String = "×" + "%.2f".format(dispCommandCR)
        override fun getProgressColor(intel: BaseEventIntel): Color =
            if (dispCommandCR < 1f) Misc.getPositiveHighlightColor() else Misc.getHighlightColor()
        override fun getMainRowTooltip(intel: BaseEventIntel): TooltipCreator = factorTip("siege_factortip_cr")
    }

    /**
     * One-time fleet-kill factor (auto-expires after [BaseOneTimeFactor.SHOW_DURATION_DAYS]). Passes
     * `super(0)` so it never mutates `progress` on add — the manager already knocked the meter back.
     * Shown green as a favourable knock-back; a command kill shows no number (it freezes the meter).
     */
    inner class FleetKillFactor(
        private val knockback: Float,
        private val isCommand: Boolean
    ) : BaseOneTimeFactor(0) {
        override fun getDesc(intel: BaseEventIntel): String =
            if (isCommand) txt("siege_factor_kill_command") else txt("siege_factor_kill_escort")
        override fun getProgressStr(intel: BaseEventIntel): String =
            if (isCommand) "" else "-" + knockback.roundToInt()
        override fun getProgressColor(intel: BaseEventIntel): Color = Misc.getPositiveHighlightColor()
        override fun getDescColor(intel: BaseEventIntel): Color = Misc.getTextColor()
        override fun getMainRowTooltip(intel: BaseEventIntel): TooltipCreator =
            factorTip(if (isCommand) "siege_factortip_kill_command" else "siege_factortip_kill_escort")
        override fun addBulletPointForOneTimeFactor(intel: BaseEventIntel, info: TooltipMakerAPI, tc: Color, initPad: Float) {
            if (isCommand) {
                info.addPara(txt("siege_killbullet_command"), tc, initPad)
            } else {
                info.addPara("${txt("siege_killbullet_escort")}: %s", initPad,
                    Misc.getPositiveHighlightColor(), "-" + knockback.roundToInt())
            }
        }
    }

    companion object {
        const val MAX_PROGRESS = 100
        const val DAYS_PER_MONTH = 30f
    }
}
