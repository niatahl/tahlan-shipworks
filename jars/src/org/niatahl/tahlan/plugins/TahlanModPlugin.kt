package org.niatahl.tahlan.plugins

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignPlugin
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions.PLAYER
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.SectorManager
import exerelin.utilities.NexConfig
import org.apache.log4j.Level
import org.dark.shaders.light.LightData
import org.dark.shaders.util.ShaderLib
import org.dark.shaders.util.TextureData
import org.json.JSONException
import org.niatahl.tahlan.campaign.*
import org.niatahl.tahlan.campaign.siege.SiegeManager
import org.niatahl.tahlan.listeners.LegioFleetInflationListener
import org.niatahl.tahlan.listeners.SuccListener
import org.niatahl.tahlan.utils.IndEvoIntegrations.addArtillery
import org.niatahl.tahlan.utils.IndEvoIntegrations.addMines
import org.niatahl.tahlan.utils.IndEvoIntegrations.upgradeDefenses
import org.niatahl.tahlan.utils.TahlanIDs.BLACKWATCH
import org.niatahl.tahlan.utils.TahlanIDs.DAEMONIC_HEART
import org.niatahl.tahlan.utils.TahlanIDs.TAG_DAEMONIZE
import org.niatahl.tahlan.utils.TahlanIDs.HEL_CARAPACE
import org.niatahl.tahlan.utils.TahlanIDs
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO
import org.niatahl.tahlan.utils.ModCompat
import org.niatahl.tahlan.utils.TahlanSettings
import org.niatahl.tahlan.utils.TahlanRegistry
import org.niatahl.tahlan.utils.ModCompat.HAS_NEX
import org.niatahl.tahlan.utils.ModCompat.HAS_INDEVO
import org.niatahl.tahlan.utils.ModCompat.HAS_LUNA
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_LETHIA
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_LEGIO
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_FASTMODE
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_LIFELESS
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_LEGIOBPS
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_DAEMONS
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_SIEGE
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_HARDMODE
import org.niatahl.tahlan.utils.TahlanSettings.ENABLE_ADAPTIVEMODE
import org.niatahl.tahlan.utils.TahlanSettings.INDEVO_MINES
import org.niatahl.tahlan.utils.TahlanSettings.INDEVO_ARTY
import org.niatahl.tahlan.utils.TahlanRegistry.DAEMON_SHIPS
import org.niatahl.tahlan.utils.TahlanRegistry.DAEMON_WINGS
import org.niatahl.tahlan.utils.TahlanRegistry.DAEMON_WEAPONS
import org.niatahl.tahlan.utils.TahlanRegistry.BLACKWATCH_DAEMONS
import org.niatahl.tahlan.utils.TahlanIDs.FOUNTAIN_MISSILE_ID
import org.niatahl.tahlan.utils.TahlanIDs.KRIEGSMESSER_MISSILE_ID
import org.niatahl.tahlan.utils.TahlanIDs.DOLCH_MISSILE_ID
import org.niatahl.tahlan.utils.TahlanIDs.OMNA_MISSILE_ID
import org.niatahl.tahlan.utils.TahlanPeople
import org.niatahl.tahlan.weapons.ai.FountainAI
import org.niatahl.tahlan.weapons.ai.KriegsmesserAI
import org.niatahl.tahlan.weapons.ai.OmnaAI
import org.niatahl.tahlan.weapons.ai.TwoStageMissileAI
import org.niatahl.tahlan.world.FactionRelationPlugin
import org.niatahl.tahlan.world.Lethia
import org.niatahl.tahlan.world.Rubicon
import java.io.IOException

class TahlanModPlugin : BaseModPlugin() {

    override fun onApplicationLoad() {
        // should no longer be needed but may aswell keep these
        val hasLazyLib = Global.getSettings().modManager.isModEnabled("lw_lazylib")
        if (!hasLazyLib) {
            throw RuntimeException(
                "Tahlan Shipworks requires LazyLib by LazyWizard\nGet it at http://fractalsoftworks.com/forum/index.php?topic=5444"
            )
        }
        val hasMagicLib = Global.getSettings().modManager.isModEnabled("MagicLib")
        if (!hasMagicLib) {
            throw RuntimeException(
                "Tahlan Shipworks requires MagicLib!\nGet it at http://fractalsoftworks.com/forum/index.php?topic=13718"
            )
        }
        ModCompat.detectAtAppLoad()
        if (ModCompat.HAS_GRAPHICSLIB) {
            ShaderLib.init()
            LightData.readLightDataCSV("data/lights/tahlan_lights.csv")
            TextureData.readTextureDataCSV("data/lights/tahlan_texture.csv")
        }

        try {
            TahlanSettings.loadFromJson()
        } catch (e: IOException) {
            Global.getLogger(TahlanModPlugin::class.java).log(Level.ERROR, "tahlan_settings.json loading failed! ;....; " + e.message)
        } catch (e: JSONException) {
            Global.getLogger(TahlanModPlugin::class.java).log(Level.ERROR, "tahlan_settings.json loading failed! ;....; " + e.message)
        }


        // Collect shield hullmods + tagged daemon ships/wings
        TahlanRegistry.collectFromSpecs()

        // Stamp daemon built-ins onto daemonize-tagged hulls
        Global.getSettings().allShipHullSpecs
            .filter { it.hasTag(TAG_DAEMONIZE) && it.hullSize != ShipAPI.HullSize.FIGHTER }
            .forEach { ship ->
                ship.addBuiltInMod(DAEMONIC_HEART)
                ship.addBuiltInMod(HEL_CARAPACE)
            }
    }

    //New game stuff
    override fun onNewGame() {
        val sector = Global.getSector()

        if (HAS_LUNA) TahlanSettings.loadFromLuna()

        //If we have Nexerelin and random worlds enabled, don't spawn our manual systems
        ModCompat.detectNexerelin()
        if (!HAS_NEX || SectorManager.getManager().isCorvusMode) {
            if (ENABLE_LETHIA) {
                Lethia().generate(sector)
                sector.memoryWithoutUpdate["\$tahlan_haslethia"] = true
            }
            if (ENABLE_LEGIO) {
                Rubicon().generate(sector)
            }
        }

        //Legio things
        if (ENABLE_LEGIO) {
            sector.getFaction(LEGIO).isShowInIntelTab = true

            //Legio Infernalis relations
            FactionRelationPlugin.initFactionRelationships(sector)

            //Adding Legio to bounty system
            SharedData.getData().personBountyEventData.addParticipatingFaction(LEGIO)

            sector.memoryWithoutUpdate["\$tahlan_haslegio"] = true
        } else {
            sector.getFaction(LEGIO).isShowInIntelTab = false
            if (HAS_NEX) {
                sector.getFaction(LEGIO).memoryWithoutUpdate["\$nex_respawn_cooldown"] = true
            }
        }

        // Because apparently I have to do this
        removeDaemons(sector)

        //Rosenritter Blueprint Script
        Global.getSector().addScript(regaliablueprintscript())

    }

    override fun onNewGameAfterProcGen() {
        //Spawning hidden things
        HalbmondSpawnScript.spawnHalbmond(Global.getSector())
        DerelictsSpawnScript.spawnDerelicts(Global.getSector())
        LostechSpawnScript.spawnLostech(Global.getSector())
    }

    override fun onGameLoad(newGame: Boolean) {
        val sector = Global.getSector()

        if (HAS_LUNA) TahlanSettings.loadFromLuna()

        sector.registerPlugin(CampaignPluginImpl())

        if (ENABLE_ADAPTIVEMODE)
            sector.listenerManager.addListener(LegioFleetInflationListener(), true)

        TahlanPeople.synchronise()

        CieveScript.register()
        DigitalSoulScript.register()
        SuccListener.register()

        if (!ENABLE_LIFELESS && sector.getFaction("remnant").knowsShip("tahlan_Timeless")) {
            sector.getFaction("remnant").apply {
                removeKnownShip("tahlan_Timeless")
                removeKnownShip("tahlan_Nameless")
                removeKnownWeapon("tahlan_disparax")
                removeKnownWeapon("tahlan_relparax")
                removeKnownWeapon("tahlan_nenparax")
            }
        } else if (!sector.getFaction("remnant").knowsShip("tahlan_Timeless")) {
            sector.getFaction("remnant").apply {
                addKnownShip("tahlan_Timeless", false)
                addKnownShip("tahlan_Nameless", false)
                addKnownWeapon("tahlan_disparax", false)
                addKnownWeapon("tahlan_relparax", false)
                addKnownWeapon("tahlan_nenparax", false)
            }
        }
        // fallback - If Lucifron exists, so does Legio, probably
        if (sector.economy.getMarket("tahlan_rubicon_p03_market") != null) {
            sector.memoryWithoutUpdate["\$tahlan_haslegio"] = true
        }

        // fallback - Check for Kassadar market for older saves etc
        if (sector.economy.getMarket("tahlan_lethia_p05_market") != null) {
            sector.memoryWithoutUpdate["\$tahlan_haslethia"] = true
        }

        if (ENABLE_LETHIA && !sector.memoryWithoutUpdate.getBoolean("\$tahlan_haslethia")) {
            if (!HAS_NEX || SectorManager.getManager().isCorvusMode) {
                Lethia().generate(sector)
            }
        }

        if (ENABLE_LEGIO && !sector.memoryWithoutUpdate.getBoolean("\$tahlan_haslegio")) {
            sector.getFaction(LEGIO).isShowInIntelTab = true

            if (!HAS_NEX || SectorManager.getManager().isCorvusMode) {
                Rubicon().generate(sector)
            }

            //Legio Infernalis relations
            FactionRelationPlugin.initFactionRelationships(sector)

            //Adding Legio to bounty system
            SharedData.getData().personBountyEventData.addParticipatingFaction(LEGIO)

            sector.memoryWithoutUpdate["\$tahlan_haslegio"] = true
        }

        if (sector.memoryWithoutUpdate.getBoolean("\$tahlan_haslegio")) {
            // Legio siege manager — register once; permanent listener survives save/load
            if (ENABLE_SIEGE && sector.memoryWithoutUpdate.get(TahlanIDs.SIEGE_MANAGER_KEY) == null) {
                val siegeManager = SiegeManager()
                sector.listenerManager.addListener(siegeManager, true)  // for reportFleetDespawned
                sector.addScript(siegeManager)                          // for advance() (the siege loop)
                sector.memoryWithoutUpdate.set(TahlanIDs.SIEGE_MANAGER_KEY, siegeManager, 0f)
            }

            // Legio stealing pirates homework
            if (ENABLE_LEGIOBPS) {
                sector.addTransientScript(LegioStealingHomework())
            }

            // Add our listener for stuff
            if (ENABLE_DAEMONS) {
                sector.addTransientListener(TahlanTrigger())
            } else {
                removeDaemons(sector)
            }
            // If somehow the Daemons are missing, add them (also covers the quiet planetkiller-gift state)
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered")
                || Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_gavePKtoLegio")) {
                addDaemons(sector)
            }
            val legio = Global.getSector().getFaction(LEGIO)
            DAEMON_SHIPS.run { if (ENABLE_HARDMODE) forEach { legio.addPriorityShip(it) } else forEach { legio.removePriorityShip(it) } }

            // Adding new fun(tm) to existing saves
            if (HAS_INDEVO) {
                if (INDEVO_ARTY) addArtillery()
                if (INDEVO_MINES) addMines()
                // Daemon upgrade now also upgrades defenses
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered")
                    || Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_gavePKtoLegio"))
                    upgradeDefenses()
            }
        }
        if (HAS_NEX) {
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered")) {
                if (!NexConfig.getFactionConfig(LEGIO).diplomacyTraits.contains("monstrous")) {
                    NexConfig.getFactionConfig(LEGIO).diplomacyTraits.add("monstrous")
                }
                NexConfig.getFactionConfig(LEGIO).diplomacyPositiveChance["default"] = 0.1f
                NexConfig.getFactionConfig(LEGIO).diplomacyNegativeChance["default"] = 2f
            } else {
                NexConfig.getFactionConfig(LEGIO).diplomacyTraits.remove("monstrous")
                NexConfig.getFactionConfig(LEGIO).diplomacyPositiveChance["default"] = 0.5f
                NexConfig.getFactionConfig(LEGIO).diplomacyNegativeChance["default"] = 1f
            }
        }

        // Just for me :)
        if (Global.getSettings().modManager.isModEnabled("portrait_nia")) {
            sector.getFaction(PLAYER).getPortraits(FullName.Gender.FEMALE).also {
                it.remove("graphics/portraits/npp_001.png")
                it.remove("graphics/portraits/npp_002.png")
                it.remove("graphics/portraits/npp_003.png")
                it.remove("graphics/portraits/npp_004.png")
                it.remove("graphics/portraits/npp_005.png")
                it.remove("graphics/portraits/npp_006.png")
                it.remove("graphics/portraits/npp_cieve.png")
            }
        }
    }

    private class TahlanTrigger : BaseCampaignEventListener(false) {
        override fun reportEconomyMonthEnd() {
            val sector = Global.getSector()
            // Keep daemons topped up whenever they're enabled — full awakening OR a planetkiller gift.
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered")
                || Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_gavePKtoLegio")) {
                LOGGER.info("Daemons lurk")
                addDaemons(sector)  // retroactively add new daemons for mid-campaign updates
            }
            // Already fully awoken → nothing left to roll.
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered")) return
            // A gifted planetkiller strike is still pending → hold the betrayal back; the Legio stays
            // friendly until it lands (the delayed betrayal — see PlanetkillerStrike). Once the strike
            // resolves by interception, that suppression lifts and the natural incursion resumes below:
            // a gift only delays the reckoning, it never cancels it. Without a gift, this flips as normal.
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_gavePKtoLegio")
                && !Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_pkStrikeResolved")) return

            var iLegioStartingCondition = 0

            if (Global.getSector().clock.cycle >= 210) {
                iLegioStartingCondition++
                LOGGER.info("Daemonic Incursion - Cycle")
            } //Choose cycle
            for (market in Misc.getPlayerMarkets(true)) {
                if (market.size >= 5) {
                    iLegioStartingCondition++
                    LOGGER.info("Daemonic Incursion - Market")
                    break
                }
            } //Ok size 6
            val mem = Global.getSector().memoryWithoutUpdate
            if (mem.getBoolean(GateEntityPlugin.CAN_SCAN_GATES) && mem.getBoolean(GateEntityPlugin.GATES_ACTIVE)) {
                iLegioStartingCondition++ //Follow Histidine's "Skip Story format"
                LOGGER.info("Daemonic Incursion - Gates")
            }
            if (sector.playerStats.level >= 13) {
                iLegioStartingCondition++
                LOGGER.info("Daemonic Incursion - Level")
            } // Two capitals or Metafalica
            var caps = 0
            for (bote in sector.playerFleet.membersWithFightersCopy) {
                if (bote.hullSpec.hullSize == ShipAPI.HullSize.CAPITAL_SHIP) {
                    caps++
                }
                // Metafalica counts double
                if (bote.hullSpec.hullId.contains("Metafalica")) {
                    caps++
                }
                // got Daemons? instant trigger
                if (bote.hullSpec.hullId.contains("_dmn")) {
                    iLegioStartingCondition = 99
                }
            }
            if (caps >= 2) {
                iLegioStartingCondition++
                LOGGER.info("Daemonic Incursion - Ships")
            }
            if (Global.getSector().playerFleet.cargo.credits.get() > 5000000f) {
                iLegioStartingCondition++
                LOGGER.info("Daemonic Incursion - Wealth")
            }
            if (Misc.getNumNonMercOfficers(Global.getSector().playerFleet) > 7) {
                iLegioStartingCondition++
                LOGGER.info("Daemonic Incursion - Officers")
            }
            val trigger = if (ENABLE_FASTMODE) 2 else 4
            if (iLegioStartingCondition >= trigger) {
                triggerDaemonicIncursion()
            }
        }
    }

    override fun pickMissileAI(missile: MissileAPI, launchingShip: ShipAPI): PluginPick<MissileAIPlugin>? {
        return when (missile.projectileSpecId) {
            FOUNTAIN_MISSILE_ID -> PluginPick(FountainAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            KRIEGSMESSER_MISSILE_ID -> PluginPick(KriegsmesserAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            DOLCH_MISSILE_ID -> PluginPick(TwoStageMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            OMNA_MISSILE_ID -> PluginPick(OmnaAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            else -> null
        }
    }

    override fun pickShipAI(member: FleetMemberAPI?, ship: ShipAPI): PluginPick<ShipAIPlugin>? {
        if (ship.isFighter) return null
        if (ship.captain == null || ship.captain.isDefault) return null

        val fearless = ShipAIConfig().apply {
            alwaysStrafeOffensively = true
            backingOffWhileNotVentingAllowed = false
            turnToFaceWithUndamagedArmor = false
            burnDriveIgnoreEnemies = true
        }

        if (ship.captain.memoryWithoutUpdate.contains(TahlanPeople.FEARLESS)) {
            return PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, fearless),CampaignPlugin.PickPriority.MOD_SPECIFIC)
        }

        return null
    }


    companion object {
        val LOGGER = Global.getLogger(TahlanModPlugin::class.java)!!


        /**
         * Quietly makes the Legio's daemon arsenal available (and upgrades its market defenses) WITHOUT
         * touching the Legio's relationships or Nex diplomacy — they stay as friendly as the player left
         * them. Used for the "delayed betrayal" path when the player hands the Legio a planetkiller:
         * daemons appear immediately, but the Legio stays friendly until its planetkiller strike lands.
         * Persistence across loads is driven by the `$tahlan_gavePKtoLegio` flag (set by the handover rule),
         * which re-adds the daemons on game load. Idempotent.
         */
        fun enableDaemons() {
            addDaemons(Global.getSector())
            upgradeDefenses()
        }

        /**
         * Fully awakens the Legio Infernalis: enables daemons (if not already), sets the `$tahlan_triggered`
         * flag, turns the Legio hostile (unless the player is commissioned with them), and flips Nex
         * diplomacy to "monstrous". Idempotent — a no-op past the first call. This is the betrayal: driven
         * either by the natural daemonic-incursion thresholds (reportEconomyMonthEnd) or by the planetkiller
         * strike detonating (PlanetkillerStrikeFleetAI).
         */
        fun awakenLegioHostility() {
            val sector = Global.getSector()
            enableDaemons()
            if (sector.memoryWithoutUpdate.getBoolean("\$tahlan_triggered")) return
            sector.memoryWithoutUpdate["\$tahlan_triggered"] = true
            LOGGER.info("The Daemonic horde awakens")
            val legio = sector.getFaction(LEGIO)
            if (Misc.getCommissionFaction() !== legio) {
                legio.setRelationship(sector.playerFaction.id, RepLevel.HOSTILE)
                if (Misc.getCommissionFaction() != null) legio.setRelationship(Misc.getCommissionFactionId(), RepLevel.HOSTILE)
            }
            if (HAS_NEX) {
                val cfg = NexConfig.getFactionConfig(LEGIO)
                if (!cfg.diplomacyTraits.contains("monstrous")) cfg.diplomacyTraits.add("monstrous")
                cfg.diplomacyPositiveChance["default"] = 0.1f
                cfg.diplomacyNegativeChance["default"] = 2f
            }
        }

        /** The natural daemonic incursion: the full awakening (daemons + hostility). */
        fun triggerDaemonicIncursion() = awakenLegioHostility()

        private fun addDaemons(sector: SectorAPI) {
            DAEMON_SHIPS.forEach {
                sector.getFaction(LEGIO).addKnownShip(it, false)
                sector.getFaction(BLACKWATCH).addKnownShip(it, false)
            }
            DAEMON_WINGS.forEach {
                sector.getFaction(LEGIO).addKnownFighter(it, false)
                sector.getFaction(BLACKWATCH).addKnownFighter(it, false)
            }
            DAEMON_WEAPONS.forEach {
                sector.getFaction(LEGIO).addKnownWeapon(it, false)
                sector.getFaction(BLACKWATCH).addKnownWeapon(it, false)
            }
            BLACKWATCH_DAEMONS.forEach {
                sector.getFaction(BLACKWATCH).addKnownShip(it, false)
            }

        }

        private fun removeDaemons(sector: SectorAPI) {
            DAEMON_SHIPS.forEach {
                sector.getFaction(LEGIO).removeKnownShip(it)
                sector.getFaction(BLACKWATCH).removeKnownShip(it)
            }
            DAEMON_WINGS.forEach {
                sector.getFaction(LEGIO).removeKnownFighter(it)
                sector.getFaction(BLACKWATCH).removeKnownFighter(it)
            }
        }

    }
}