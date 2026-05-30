package org.niatahl.tahlan.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageSpecialInteraction.SalvageSpecialData
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageSpecialInteraction.SalvageSpecialPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.utils.TahlanIDs.TRAUM_QUEST_COMPLETE
import org.niatahl.tahlan.utils.TahlanIDs.TRAUM_RESOLVED
import org.niatahl.tahlan.utils.TahlanIDs.TRAUM_VARIANT
import org.niatahl.tahlan.utils.TahlanPeople

/**
 * Self-describing salvage-special data for the Traumtänzer dilemma. Attached to the derelict via
 * Misc.setSalvageSpecial(...) in DerelictsSpawnScript's traum branch. No config registration needed —
 * SalvageSpecialData supplies its own plugin.
 */
class TraumDilemmaSpecialData : SalvageSpecialData {
    override fun createSpecialPlugin(): SalvageSpecialPlugin = TraumDilemma()
}

/**
 * The Rosenritter capstone dilemma, posed when the player salvages the (Remnant-guarded) Traumtänzer
 * derelict. Reserve power is keeping the crew's cryopods alive while the reactor fails; you cannot
 * save both. A hard binary, resolved exactly once.
 *
 *   SAVE SHIP : cut the cryopods (crew dies), reactor stabilizes -> recover the battered Traumtänzer.
 *   SAVE CREW : defrost them, reactor collapses -> ship destroyed -> recruit Henrietta + crew/marines.
 *
 * Reads cold (no questline state assumed); shows extra flavor only if the questline-complete flag is
 * set. ALL on-screen text is placeholder for hand-rewrite.
 *
 * NOTE: written against the API but not compiled in this environment; expect IntelliJ-compiler
 * iteration (see the change's task group 5).
 */
class TraumDilemma : BaseSalvageSpecial() {

    private var dlg: InteractionDialogAPI? = null

    override fun init(dialog: InteractionDialogAPI, specialData: Any?) {
        super.init(dialog, specialData)
        dlg = dialog

        // Defensive: if somehow re-entered after resolution, do nothing. (The entity is normally
        // consumed on resolution, so this should not trigger.)
        if (Global.getSector().memoryWithoutUpdate.getBoolean(TRAUM_RESOLVED)) {
            initNothing()
            return
        }
        setShowAgain(false)

        val text = dialog.textPanel
        val options = dialog.optionPanel

        // [PLACEHOLDER] situation: derelict Traumtänzer, reserve power sustaining the crew's cryopods,
        // reactor on the verge of collapse, the two cannot both be saved. Author rewrites.
        text.addPara("[PLACEHOLDER] The derelict still has reserve power — all of it routed to a bank of cryopods holding the surviving crew. The reactor is failing. You cannot save both the ship and the people.")

        // Optional questline-aware flavor only (identical options either way).
        if (Global.getSector().memoryWithoutUpdate.getBoolean(TRAUM_QUEST_COMPLETE)) {
            text.addPara("[PLACEHOLDER] Quest-aware context: you know whose ship this is, and who sleeps aboard.")
        }

        options.clearOptions()
        options.addOption("[PLACEHOLDER] Shut off the cryopods and stabilize the reactor — save the ship", OPT_SHIP)
        options.addOption("[PLACEHOLDER] Defrost the crew and let the reactor go — save the people", OPT_CREW)
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        val dialog = dlg ?: return
        when (optionData) {
            OPT_SHIP -> { saveShip(dialog); finish(dialog) }
            OPT_CREW -> { saveCrew(dialog); finish(dialog) }
            OPT_CONTINUE -> setDone(true)
        }
    }

    /** Save the ship: crew dies (no mechanical penalty — narrative only), recover the battered hull. */
    private fun saveShip(dialog: InteractionDialogAPI) {
        val playerFleet = Global.getSector().playerFleet
        val variant = Global.getSettings().getVariant(TRAUM_VARIANT).clone()
        // Battered recovery. (Exact parity with ShipCondition.BATTERED is a verify item; ~4 D-mods
        // is a representative battered hull.)
        DModManager.addDMods(variant, false, 4, Misc.random)
        val member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant)
        member.repairTracker.cr = 0.3f
        playerFleet.fleetData.addFleetMember(member)
        dialog.textPanel.addPara("[PLACEHOLDER] The cryopods go dark. The reactor steadies. The Traumtänzer is yours — battered, but whole.")
    }

    /** Save the crew: ship is destroyed, recruit the (already-tuned) Henrietta + a little crew/marines. */
    private fun saveCrew(dialog: InteractionDialogAPI) {
        val playerFleet = Global.getSector().playerFleet
        val henrietta = TahlanPeople.getPerson(TahlanPeople.HENRIETTA)
        if (henrietta != null) {
            playerFleet.fleetData.addOfficer(henrietta)
        }
        // Flavor bonus (cheap by design).
        playerFleet.cargo.addCommodity(Commodities.CREW, CREW_REWARD)
        playerFleet.cargo.addCommodity(Commodities.MARINES, MARINE_REWARD)
        dialog.textPanel.addPara("[PLACEHOLDER] The pods cycle open as the reactor tears itself apart. The Traumtänzer is gone — but Henrietta von Regenfels and her crew live, and throw in with you.")
    }

    /** Latch the one-time flag, clean up the questline intel, consume the derelict, and offer Continue. */
    private fun finish(dialog: InteractionDialogAPI) {
        Global.getSector().memoryWithoutUpdate.set(TRAUM_RESOLVED, true)
        dropStaleQuestlinePointer()
        setShouldAbortSalvageAndRemoveEntity(true) // remove the derelict; no further salvage
        dialog.optionPanel.clearOptions()
        dialog.optionPanel.addOption("Continue", OPT_CONTINUE)
    }

    /**
     * If the Rosenritter questline intel has reached its final (pointer) stage, drop it — the pointer
     * to this ship is now clutter. Mid-decryption intel is left intact (it still serves the blueprint
     * hunt); cold finds with no questline intel are a no-op. Resolution before the final stage is
     * handled on the questline side (the pointer text is suppressed via $tahlan_traumResolved).
     */
    private fun dropStaleQuestlinePointer() {
        val intelMgr = Global.getSector().intelManager
        val questIntel = intelMgr.getFirstIntel(regaliablueprintintel::class.java) as? regaliablueprintintel
        if (questIntel != null && questIntel.isAtFinalStage) {
            intelMgr.removeIntel(questIntel)
        }
    }

    override fun shouldShowAgain(): Boolean = false

    companion object {
        private const val OPT_SHIP = "tahlan_traum_saveShip"
        private const val OPT_CREW = "tahlan_traum_saveCrew"
        private const val OPT_CONTINUE = "tahlan_traum_continue"
        private const val CREW_REWARD = 50f
        private const val MARINE_REWARD = 20f
    }
}
