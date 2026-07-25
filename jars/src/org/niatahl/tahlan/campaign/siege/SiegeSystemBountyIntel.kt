package org.niatahl.tahlan.campaign.siege

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.SystemBountyIntel
import org.niatahl.tahlan.utils.TahlanIDs

/**
 * The desperation bounty a strangled market posts once the siege reaches Stranglehold: a plain
 * **vanilla** system bounty, deliberately so. Payout (per hull size, scaled by player involvement),
 * reputation gain with the poster, the "Bounty Posted" feed item, the 60-day duration, the
 * `MilitaryResponseScript` that stirs up the victim's own patrols, the auto-termination when the
 * market changes hands, and the constructor's refusal to post for player-faction markets are all
 * vanilla behavior that this subclass inherits untouched.
 *
 * Exactly two things are customized:
 *
 * 1. **Attribution.** Vanilla guesses the "likely cause" faction by scanning for the largest nearby
 *    hostile market — which would credit whichever neighbour happens to be big, since Legio's own
 *    markets sit far away at Lucifron. The guess runs in the superclass constructor (the method is
 *    private, so it cannot be overridden); we simply overwrite the protected result afterwards.
 * 2. **Reward.** [rewardMult] scales the vanilla base-bounty formula so a larger expedition puts
 *    more money on the table. `<= 0` leaves the vanilla amount exactly as computed.
 *
 * Registered through `SystemBountyManager.addActive` by [SiegeManager] so the vanilla manager's own
 * market-dedup accounts for it and never stacks a second bounty on the same market.
 */
class SiegeSystemBountyIntel(
    market: MarketAPI,
    rewardMult: Float
) : SystemBountyIntel(market, -1, false) {

    init {
        Global.getSector().getFaction(TahlanIDs.LEGIO)?.let { enemyFaction = it }
        if (rewardMult > 0f && rewardMult != 1f) {
            baseBounty *= rewardMult
        }
    }
}
