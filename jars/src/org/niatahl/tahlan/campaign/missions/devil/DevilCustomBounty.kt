package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.impl.campaign.missions.cb.*
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs.CB_CLEAN_KILL
import org.niatahl.tahlan.utils.Utils.txt


class DevilCustomBounty : BaseCustomBounty() {
    override fun getCreators(): MutableList<CustomBountyCreator> {
        return CREATORS
    }

    override fun updateInteractionDataImpl() {
        super.updateInteractionDataImpl()
        val id = getMissionId()
        if (showData != null && showCreator != null) {
            if (showData.fleet != null) {
                val p = showData.fleet.commander
                set("$" + id + "_targetRank", p.rank)
            }
        }
        // Exposed for rules rows that want to branch on how the last accepted contract was carried out.
        (data?.customMap?.get(CB_CLEAN_KILL) as? Boolean)?.let { set("$" + id + "_cleanKill", it) }
    }

    /**
     * Samples the clean-kill verdict while the information still exists, then hands off to vanilla.
     *
     * Custom bounties self-complete: [BaseCustomBounty.reportBattleOccurred] sets `$<id>_completed`,
     * the stage advances, and the reward resolves without a return trip to the giver. By the time
     * `notifyCompleted` runs the target fleet is gone and the surrounding fleets have moved, so the
     * verdict has to be taken here — sample, adjust the reward, stash the verdict, *then* `super`.
     *
     * Only [CBLegioPurge] contracts are eligible; every other creator on the table pays its base
     * reward regardless of transponder state. That includes the ones whose targets do carry real
     * reputation consequences (`CBPatrol` and `CBTrader` hit legitimate factions) — the bonus is
     * specifically payment for keeping *Blackwatch's* hand out of the record, not a general
     * stealth reward.
     */
    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        if (fleet != null && battle != null) sampleCleanKill(fleet, battle)
        super.reportBattleOccurred(fleet, primaryWinner, battle)
    }

    private fun sampleCleanKill(fleet: CampaignFleetAPI, battle: BattleAPI) {
        if (isDone() || result != null) return
        val data = this.data ?: return
        if (creator !is CBLegioPurge) return
        // One-shot: the verdict is recorded exactly once, on the battle that finished the target.
        if (data.customMap.containsKey(CB_CLEAN_KILL)) return
        if (fleet !== data.fleet) return
        if (!isKillingBlow(fleet, battle)) return

        val clean = isCleanKill(fleet)
        data.customMap[CB_CLEAN_KILL] = clean
        if (clean) {
            setCreditReward(BaseHubMission.getRoundNumber(creditsReward * (1f + CLEAN_KILL_BONUS)))
        }
    }

    /**
     * Mirrors [BaseCustomBounty.reportBattleOccurred]'s own completion condition, so the verdict is
     * sampled on exactly the battle that vanilla will complete the bounty on — not on an earlier
     * engagement the target survived.
     */
    private fun isKillingBlow(fleet: CampaignFleetAPI, battle: BattleAPI): Boolean {
        val playerFleet = Global.getSector().playerFleet ?: return false
        val playerInvolved = battle.isPlayerInvolved ||
            (fleet.isInCurrentLocation && Misc.getDistance(fleet, playerFleet) < 2000f)
        if (!playerInvolved || !battle.isInvolved(fleet) || battle.onPlayerSide(fleet)) return false

        return if (fleet.isStationMode) {
            fleet.flagship == null
        } else {
            fleet.flagship == null || fleet.flagship.captain !== target
        }
    }

    /**
     * The clean-kill predicate: the player is unidentified if the transponder is off *and* no other
     * fleet of the target's faction can see who it is fighting.
     *
     * This is deliberately [com.fs.starfarer.api.impl.campaign.TOffAlarm.notifyNearby]'s own loop
     * condition with the flag-setting swapped for a boolean, so one rule governs both halves of the
     * mechanic: if the witness alarm would have spread, there is no bonus.
     */
    private fun isCleanKill(targetFleet: CampaignFleetAPI): Boolean {
        val playerFleet = Global.getSector().playerFleet ?: return false
        if (playerFleet.isTransponderOn) return false
        val seen = targetFleet.containingLocation?.fleets?.any { other ->
            other !== targetFleet &&
                other.faction === targetFleet.faction &&
                other.getVisibilityLevelTo(targetFleet) == VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS
        } ?: false
        return !seen
    }

    /** Appends the verdict to the completed intel entry, so the payout is explicable. */
    override fun addDescriptionForCurrentStage(info: TooltipMakerAPI, width: Float, height: Float) {
        super.addDescriptionForCurrentStage(info, width, height)
        if (!isSucceeded()) return
        val clean = data?.customMap?.get(CB_CLEAN_KILL) as? Boolean ?: return
        info.addPara(if (clean) txt("devilcb_purge_clean") else txt("devilcb_purge_seen"), 10f)
    }

    companion object {
        /**
         * Fraction of the base reward added for a purge carried out unidentified. Additive rather than
         * a deduction from a higher quote: the intel entry must never show a number below what Louisa
         * offered. On top of [CBLegioPurge.PURGE_MULT] this lands a clean purge near a Remnant hunt.
         */
        const val CLEAN_KILL_BONUS = 0.35f

        val CREATORS = mutableListOf<CustomBountyCreator>(
            // Originally shipped four. Their frequencies are untouched.
            CBMerc(),
            CBPirate(),
            CBPather(),
            CBRemnant(),
            // Blackwatch's own work: disowned deserters at any trust level, internal purges at FRIENDLY.
            CBLegioDeserter(),
            CBLegioPurge(),
            // Vanilla creators added for variety, at reduced frequency so the four above stay a
            // regular part of her register — see DevilVarietyCreators.kt.
            CBTraderDevil(),
            CBDerelictDevil(),
            CBPatrolDevil(),
            CBRemnantPlusDevil()
        )
    }
}
