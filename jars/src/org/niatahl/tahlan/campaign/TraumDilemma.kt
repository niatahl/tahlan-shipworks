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
import org.niatahl.tahlan.utils.Utils.txt

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
 * set.
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

        text.addPara(txt("traum_intro1"))
        text.addPara(txt("traum_intro2"))

        // Optional questline-aware flavor only (identical options either way).
        if (Global.getSector().memoryWithoutUpdate.getBoolean(TRAUM_QUEST_COMPLETE)) {
            text.addPara(txt("traum_questAware"))
        }

        options.clearOptions()
        options.addOption(txt("traum_opt_ship"), OPT_SHIP)
        options.addOption(txt("traum_opt_crew"), OPT_CREW)
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
        dialog.textPanel.addPara(txt("traum_saveShip"))
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

        val text = dialog.textPanel
        text.addPara(txt("traum_crew_narration"))
        text.addPara(txt("traum_crew_appears"))
        text.addPara(txt("traum_crew_henrietta1"))
        text.addPara(txt("traum_crew_henrietta2"))
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
