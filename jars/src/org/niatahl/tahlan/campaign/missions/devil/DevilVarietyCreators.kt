package org.niatahl.tahlan.campaign.missions.devil

import com.fs.starfarer.api.impl.campaign.missions.cb.CBDerelict
import com.fs.starfarer.api.impl.campaign.missions.cb.CBPatrol
import com.fs.starfarer.api.impl.campaign.missions.cb.CBRemnantPlus
import com.fs.starfarer.api.impl.campaign.missions.cb.CBTrader
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent

/**
 * Vanilla bounty creators added to Louisa Ferre's table purely for variety, at reduced frequency.
 *
 * Why the subclasses exist at all: [VARIETY_FREQ] cannot live in `CBStats`, whose frequency constants
 * are global and shared with every other bounty giver in the sector. Scaling here keeps the trim local
 * to Louisa's table.
 *
 * Why: four of these at full weight would have pushed the four creators Louisa originally shipped with
 * from 20–67% of a dossier slot down to 8–29%. CBTrader and CBPatrol are the worst offenders — both
 * sit in band 0 at frequency 1.0, alongside CBPirate. Halving the added creators keeps every
 * originally-shipped creator at roughly a fifth of the low band and a tenth of the high band, which
 * still reads as a regular part of her register. Tune this constant rather than removing entries.
 *
 * **Each subclass must override `getId()` back to the vanilla simple name.**
 * [com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBountyCreator.getId] defaults to
 * `getClass().getSimpleName()`, which drives both the `<id>OfferDesc` rules trigger and the
 * completion-count memory keys. Letting it return the subclass name would silently orphan the
 * `tahlan_CB*Devil` rules rows (the trigger they answer would never fire) and fork the completion
 * counters — including CBRemnantPlus's, which is what makes that hunt one-time per sector.
 */
private const val VARIETY_FREQ = 0.5f

class CBTraderDevil : CBTrader() {
    override fun getId(): String = "CBTrader"
    override fun getFrequency(mission: HubMissionWithBarEvent, difficulty: Int): Float =
        super.getFrequency(mission, difficulty) * VARIETY_FREQ
}

class CBPatrolDevil : CBPatrol() {
    override fun getId(): String = "CBPatrol"
    override fun getFrequency(mission: HubMissionWithBarEvent, difficulty: Int): Float =
        super.getFrequency(mission, difficulty) * VARIETY_FREQ
}

class CBDerelictDevil : CBDerelict() {
    override fun getId(): String = "CBDerelict"
    override fun getFrequency(mission: HubMissionWithBarEvent, difficulty: Int): Float =
        super.getFrequency(mission, difficulty) * VARIETY_FREQ
}

class CBRemnantPlusDevil : CBRemnantPlus() {
    override fun getId(): String = "CBRemnantPlus"
    override fun getFrequency(mission: HubMissionWithBarEvent, difficulty: Int): Float =
        super.getFrequency(mission, difficulty) * VARIETY_FREQ
}
