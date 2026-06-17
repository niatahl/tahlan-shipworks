package org.niatahl.tahlan.campaign.customstart

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.CharacterCreationData
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.SharedUnlockData
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.NGCAddStandardStartingScript
import com.fs.starfarer.api.util.DelayedActionScript
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.ExerelinSetupData
import exerelin.campaign.PlayerFactionStore
import exerelin.campaign.backgrounds.CharacterBackgroundUtils
import exerelin.campaign.customstart.CustomStart
import exerelin.utilities.StringHelper

/**
 * Nexerelin custom start that hands the player the retooled Dreamweaver. Themed around (and gated on)
 * Secrets of the Frontier's "Child of the Lake" background: it stays disabled until the player has
 * unlocked that background by completing "The Haunted", using the exact same cross-save unlock flag
 * SotF checks (`sotf_haunted_completed` in the `sotf_persistent` [SharedUnlockData] set).
 *
 * Mod presence is gated by `requiredModId` in customStarts.json, so this only loads when SotF is on.
 *
 * The actual Child-of-the-Lake mechanics are NOT reimplemented here: setting [MEM_COTL_START] is
 * enough, because SotF's own SotfModPlugin keys all of it off that flag on every game load - it
 * grants the Cult of the Daydream ship/weapon/fighter blueprints, spawns the Sirius intel, and
 * enables Invoke Her Blessing. We just hand over the Dreamweaver on top and fire SotF's intro dialog.
 */
class DreamweaverStart : CustomStart() {

    override fun shouldShow(): Boolean =
        Global.getSettings().modManager.isModEnabled(SOTF_ID)

    /** Non-null disables the start in the picker and shows this as the reason. */
    override fun getDisabledTooltip(): String? =
        if (isUnlocked()) null
        else "Complete the finale of \"The Haunted\" in Secrets of the Frontier to unlock this start."

    override fun execute(dialog: InteractionDialogAPI, memoryMap: Map<String, MemoryAPI>) {
        val data = memoryMap[MemKeys.LOCAL]!!.get("\$characterData") as CharacterCreationData

        // Defensive: the picker already gates via getDisabledTooltip(), but never trust the UI.
        if (!isUnlocked()) {
            dialog.textPanel.addParagraph(
                "This start is locked. Complete the finale of \"The Haunted\" in Secrets of the " +
                    "Frontier to unlock it.", Misc.getNegativeHighlightColor()
            )
            dialog.optionPanel.addOption(StringHelper.getString("back", true), "nex_NGCStartBack")
            return
        }

        // The one line that matters for CotL: SotF's SotfModPlugin grants the Daydream blueprints,
        // spawns Sirius and enables Invoke Her Blessing whenever this flag is set on game load.
        data.addScriptBeforeTimePass {
            Global.getSector().memoryWithoutUpdate.set(MEM_COTL_START, true)
        }

        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER)
        ExerelinSetupData.getInstance().freeStart = true

        // Custom starts skip Nex's background picker, but Nex persists the last-picked background in
        // ExerelinSetupData (disk-backed) and never clears it on a new game - the clears in
        // ExerelinModPlugin.onNewGame are commented out. Left alone, onNewGame would graft that stale
        // background (e.g. a previous Child of the Lake run) onto this start: a second Sirius intro and
        // a silently-spent skill point. execute() runs before onNewGame, so wipe it here to pre-empt
        // that. (The intro guard further down is the version-independent backstop if Nex ever fixes
        // this and the timing shifts.)
        ExerelinSetupData.getInstance().backgroundId = null
        ExerelinSetupData.getInstance().selectedFactionForBackground = null

        // Display fleet for the right-hand panel; the actual fleet is built from addStartingFleetMember.
        val tempFleet = FleetFactoryV3.createEmptyFleet(
            PlayerFactionStore.getPlayerFactionIdNGC(), FleetTypes.PATROL_SMALL, null
        )

        addFleetMember(DREAMWEAVER_VARIANT, dialog, data, tempFleet, flagship = true)

        val credits = 25000
        data.startingCargo.credits.add(credits.toFloat())
        AddRemoveCommodity.addCreditsGainText(credits, dialog.textPanel)

        tempFleet.fleetData.setSyncNeeded()
        tempFleet.fleetData.syncIfNeeded()
        tempFleet.forceSync()

        // Scale starting logistics off the fleet, same shape as the vanilla/SotF starts.
        var crew = 0
        var fuel = 0
        var supplies = 0
        for (member in tempFleet.fleetData.membersListCopy) {
            crew += (member.minCrew + (member.maxCrew - member.minCrew) * 0.1f).toInt()
            fuel += (member.fuelCapacity * 0.35f).toInt()
            supplies += (member.baseDeploymentCostSupplies * 2).toInt()
        }
        val cargo = data.startingCargo
        cargo.addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.CREW, crew.toFloat())
        cargo.addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.FUEL, fuel.toFloat())
        cargo.addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.SUPPLIES, supplies.toFloat())
        cargo.addItems(CargoAPI.CargoItemType.RESOURCES, Commodities.HEAVY_MACHINERY, 15f)
        AddRemoveCommodity.addCommodityGainText(Commodities.CREW, crew, dialog.textPanel)
        AddRemoveCommodity.addCommodityGainText(Commodities.FUEL, fuel, dialog.textPanel)
        AddRemoveCommodity.addCommodityGainText(Commodities.SUPPLIES, supplies, dialog.textPanel)
        AddRemoveCommodity.addCommodityGainText(Commodities.HEAVY_MACHINERY, 15, dialog.textPanel)

        data.addScript {
            val fleet = Global.getSector().playerFleet
            NGCAddStandardStartingScript.adjustStartingHulls(fleet)
            fleet.fleetData.ensureHasFlagship()
            for (member in fleet.fleetData.membersListCopy) {
                member.repairTracker.setCR(member.repairTracker.maxCR)
            }
            fleet.fleetData.setSyncNeeded()

            // SotF fires its intro from its own start; mirror that so the player meets Sirius.
            // BUT the "Child of the Lake" background fires the exact same intro (with its own VFX)
            // from SotfChildOfTheLakeBackground.onNewGameAfterTimePass. If the player took both this
            // start and that background, skip ours so Sirius isn't introduced twice — let the
            // background's (nicer, fade-in/out) version play. By the time this runs on the campaign
            // clock, Nex has long since written $nex_selected_background (in ExerelinModPlugin.onNewGame).
            Global.getSector().addScript(object : DelayedActionScript(1.25f) {
                override fun doAction() {
                    if (CharacterBackgroundUtils.isBackgroundActive(SOTF_COTL_BACKGROUND)) return
                    Misc.showRuleDialog(Global.getSector().playerFleet, "sotfCOTLIntro")
                }
            })
        }

        dialog.textPanel.addParagraph(
            "Awakened as a Child of the Lake — Sirius and the Cult of the Daydream's blueprints " +
                "come with the inheritance.", Misc.getPositiveHighlightColor()
        )

        dialog.visualPanel.showFleetInfo(
            StringHelper.getString("exerelin_ngc", "playerFleet", true), tempFleet, null, null
        )
        dialog.optionPanel.addOption(StringHelper.getString("done", true), "nex_NGCDone")
        dialog.optionPanel.addOption(StringHelper.getString("back", true), "nex_NGCStartBack")
    }

    private fun addFleetMember(
        vid: String,
        dialog: InteractionDialogAPI,
        data: CharacterCreationData,
        fleet: CampaignFleetAPI,
        flagship: Boolean
    ) {
        data.addStartingFleetMember(vid, FleetMemberType.SHIP)
        val temp = Global.getFactory().createFleetMember(FleetMemberType.SHIP, vid)
        fleet.fleetData.addFleetMember(temp)
        temp.repairTracker.setCR(0.7f)
        if (flagship) {
            fleet.fleetData.setFlagship(temp)
            temp.captain = data.person
        }
        AddRemoveCommodity.addFleetMemberGainText(temp.variant, dialog.textPanel)
    }

    private fun isUnlocked(): Boolean =
        SharedUnlockData.get()?.getSet(SOTF_UNLOCK_SET)?.contains(SOTF_HAUNTED_DONE) == true

    companion object {
        // Legacy variant id retained for save compatibility (ship is now the "Dreamweaver").
        private const val DREAMWEAVER_VARIANT = "tahlan_nxa_experimental"
        private const val SOTF_ID = "secretsofthefrontier"
        // Mirrors SotfChildOfTheLakeStart / SotfChildOfTheLakeBackground's unlock check.
        private const val SOTF_UNLOCK_SET = "sotf_persistent"
        private const val SOTF_HAUNTED_DONE = "sotf_haunted_completed"
        // Nex background spec id (data/config/exerelin/character_backgrounds.csv) for "Child of the
        // Lake" - if it's the selected background, it already fires sotfCOTLIntro itself.
        private const val SOTF_COTL_BACKGROUND = "sotf_cotl_bg"
        // SotfIDs.MEM_COTL_START - the flag SotF's machinery reads to set up the whole CotL run.
        private const val MEM_COTL_START = "\$sotf_cotlStart"
    }
}
