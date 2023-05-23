package org.niatahl.tahlan

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignPlugin
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.fs.starfarer.api.util.Misc
import exerelin.campaign.SectorManager
import exerelin.utilities.NexConfig
import lunalib.lunaSettings.LunaSettings
import org.apache.log4j.Level
import org.dark.shaders.light.LightData
import org.dark.shaders.util.ShaderLib
import org.dark.shaders.util.TextureData
import org.json.JSONException
import org.magiclib.kotlin.getFactionMarkets
import org.niatahl.tahlan.campaign.*
import org.niatahl.tahlan.plugins.CampaignPluginImpl
import org.niatahl.tahlan.utils.ExiledSpaceIntegrations.ToggleDaemons
import org.niatahl.tahlan.utils.IndEvoIntegrations.addDefenses
import org.niatahl.tahlan.utils.IndEvoIntegrations.upgradeDefenses
import org.niatahl.tahlan.utils.TahlanIDs.BLACKWATCH
import org.niatahl.tahlan.utils.TahlanIDs.DAEMONIC_HEART
import org.niatahl.tahlan.utils.TahlanIDs.TAG_DAEMONIZE
import org.niatahl.tahlan.utils.TahlanIDs.HEL_CARAPACE
import org.niatahl.tahlan.utils.TahlanIDs.LEGIO
import org.niatahl.tahlan.utils.TahlanIDs.TAG_DAEMON
import org.niatahl.tahlan.utils.TahlanPeople
import org.niatahl.tahlan.weapons.ai.FountainAI
import org.niatahl.tahlan.weapons.ai.KriegsmesserAI
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
        val hasGraphicsLib = Global.getSettings().modManager.isModEnabled("shaderLib")
        if (hasGraphicsLib) {
            isGraphicsLibAvailable = true
            ShaderLib.init()
            LightData.readLightDataCSV("data/lights/tahlan_lights.csv")
            TextureData.readTextureDataCSV("data/lights/tahlan_texture.csv")
        } else {
            isGraphicsLibAvailable = false
        }
        HAS_INDEVO = Global.getSettings().modManager.isModEnabled("IndEvo")
        HAS_EXILED = Global.getSettings().modManager.isModEnabled("pt_exiledSpace")
        HAS_LUNA = Global.getSettings().modManager.isModEnabled("lunalib")

        try {
            loadTahlanSettings()
        } catch (e: IOException) {
            Global.getLogger(TahlanModPlugin::class.java).log(Level.ERROR, "tahlan_settings.json loading failed! ;....; " + e.message)
        } catch (e: JSONException) {
            Global.getLogger(TahlanModPlugin::class.java).log(Level.ERROR, "tahlan_settings.json loading failed! ;....; " + e.message)
        }


        //Adds shield hullmods
        for (hullModSpecAPI in Global.getSettings().allHullModSpecs) {
            if (hullModSpecAPI.hasTag("shields") && !SHIELD_HULLMODS.contains(hullModSpecAPI.id)) {
                SHIELD_HULLMODS.add(hullModSpecAPI.id)
            } else if (hullModSpecAPI.id.contains("swp_shieldbypass") && !SHIELD_HULLMODS.contains(hullModSpecAPI.id)) {
                SHIELD_HULLMODS.add("swp_shieldbypass") //Dirty fix for Shield Bypass, since that one is actually not tagged as a Shield mod, apparently
            }
        }

        Global.getSettings().allShipHullSpecs
            .filter { it.hasTag(TAG_DAEMONIZE) && it.hullSize != ShipAPI.HullSize.FIGHTER }
            .forEach { ship ->
                ship.addBuiltInMod(DAEMONIC_HEART)
                ship.addBuiltInMod(HEL_CARAPACE)
            }

        Global.getSettings().allShipHullSpecs
            .filter { it.hasTag(TAG_DAEMON) && it.hullSize != ShipAPI.HullSize.FIGHTER }
            .filter { !DAEMON_SHIPS.contains(it.baseHullId) }
            .forEach { DAEMON_SHIPS.add(it.baseHullId) }

        Global.getSettings().allFighterWingSpecs
            .filter { it.hasTag(TAG_DAEMON) }
            .filter { !DAEMON_WINGS.contains(it.id) }
            .forEach { DAEMON_WINGS.add(it.id) }
    }

    //New game stuff
    override fun onNewGame() {
        val sector = Global.getSector()

        if (HAS_LUNA) loadLunaSettings()

        //If we have Nexerelin and random worlds enabled, don't spawn our manual systems
        HAS_NEX = Global.getSettings().modManager.isModEnabled("nexerelin")
        if (!HAS_NEX || SectorManager.getManager().isCorvusMode) {
            if (ENABLE_LETHIA) Lethia().generate(sector)
            if (ENABLE_LEGIO) Rubicon().generate(sector)
        }

        //Legio things
        if (ENABLE_LEGIO) {

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

        if (HAS_LUNA) loadLunaSettings()

        sector.registerPlugin(CampaignPluginImpl())

        TahlanPeople.synchronise()

        CieveScript.register()
        DigitalSoulScript.register()

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
        if (sector.memoryWithoutUpdate.getBoolean("\$tahlan_haslegio")) {
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
            // If somehow the Daemons are missing, add them
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered")) {
                addDaemons(sector)
            }
            // Daemon subfaction for ES maps
            if (HAS_EXILED) {
                ToggleDaemons(Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered"))
            }

            val legio = Global.getSector().getFaction(LEGIO)
            DAEMON_SHIPS.run { if (ENABLE_HARDMODE) forEach { legio.addPriorityShip(it) } else forEach { legio.removePriorityShip(it) } }

            // Adding new fun(tm) to existing saves
            if (HAS_INDEVO) {
                addDefenses()
                // Daemon upgrade now also upgrades defenses
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered"))
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
    }

    private class TahlanTrigger : BaseCampaignEventListener(false) {
        override fun reportEconomyMonthEnd() {
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$tahlan_triggered")) {
                LOGGER.info("Daemons lurk")
                return
            }
            val sector = Global.getSector()
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
                Global.getSector().memoryWithoutUpdate["\$tahlan_triggered"] = true
                LOGGER.info("The Daemonic horde awakens")
                val legio = sector.getFaction(LEGIO)
                if (Misc.getCommissionFaction() !== legio) {
                    legio.setRelationship(sector.playerFaction.id, RepLevel.HOSTILE)
                    if (Misc.getCommissionFaction() != null) legio.setRelationship(Misc.getCommissionFactionId(), RepLevel.HOSTILE)
                }
                if (HAS_NEX) {
                    NexConfig.getFactionConfig(LEGIO).diplomacyTraits.add("monstrous")
                    NexConfig.getFactionConfig(LEGIO).diplomacyPositiveChance["default"] = 0.1f
                    NexConfig.getFactionConfig(LEGIO).diplomacyNegativeChance["default"] = 2f
                }
                addDaemons(sector)
                upgradeDefenses()
                if (HAS_EXILED) {
                    ToggleDaemons(true)
                }
            }
        }
    }

    override fun pickMissileAI(missile: MissileAPI, launchingShip: ShipAPI): PluginPick<MissileAIPlugin>? {
        return when (missile.projectileSpecId) {
            FOUNTAIN_MISSILE_ID -> PluginPick(FountainAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            KRIEGSMESSER_MISSILE_ID -> PluginPick(KriegsmesserAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            DOLCH_MISSILE_ID -> PluginPick(TwoStageMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
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

    fun loadLunaSettings() {
        ENABLE_LETHIA = LunaSettings.getBoolean("tahlan", "tahlan_enable_lethia") ?: true
        ENABLE_LEGIO = LunaSettings.getBoolean("tahlan", "tahlan_enable_legio") ?: true
        ENABLE_FASTMODE = LunaSettings.getBoolean("tahlan", "tahlan_enable_fastmode") ?: false
        ENABLE_DAEMONS = LunaSettings.getBoolean("tahlan", "tahlan_enable_daemons") ?: true
        ENABLE_HARDMODE = LunaSettings.getBoolean("tahlan", "tahlan_enable_hardmode") ?: false
        ENABLE_ADAPTIVEMODE = LunaSettings.getBoolean("tahlan", "tahlan_enable_adaptivemode") ?: true
        ENABLE_LEGIOBPS = LunaSettings.getBoolean("tahlan", "tahlan_enable_legiobps") ?: false
        ENABLE_LIFELESS = LunaSettings.getBoolean("tahlan", "tahlan_enable_lifeless") ?: false
    }

    companion object {
        @JvmField
        var isGraphicsLibAvailable = false

        @JvmField
        val SHIELD_HULLMODS: MutableList<String> = ArrayList()

        const val FOUNTAIN_MISSILE_ID = "tahlan_fountain_msl"
        const val KRIEGSMESSER_MISSILE_ID = "tahlan_kriegsmesser_msl"
        const val DOLCH_MISSILE_ID = "tahlan_dolch_msl"

        private const val SETTINGS_FILE = "tahlan_settings.json"
        var ENABLE_LETHIA = false
        var ENABLE_LEGIO = false

        var ENABLE_FASTMODE = false
        var ENABLE_LIFELESS = false
        var ENABLE_LEGIOBPS = false
        var ENABLE_DAEMONS = false

        @JvmField
        var ENABLE_HARDMODE = false
        @JvmField
        var ENABLE_ADAPTIVEMODE = false
        var WEEB_MODE = false

        @JvmField
        var HAS_NEX = false
        var HAS_INDEVO = false
        var HAS_EXILED = false
        var HAS_LUNA = false
        val LOGGER = Global.getLogger(TahlanModPlugin::class.java)!!

        val DAEMON_SHIPS = mutableListOf(
            "tahlan_dominator_dmn",
            "tahlan_champion_dmn",
            "tahlan_manticore_dmn",
            "tahlan_hammerhead_dmn",
            "tahlan_centurion_dmn",
            "tahlan_vanguard_dmn",
            "tahlan_DunScaith_dmn",
            "tahlan_hound_dmn",
            "tahlan_sunder_dmn",
            "tahlan_kodai_dmn",
            "tahlan_retribution_dmn"
        )

        val DAEMON_WINGS = mutableListOf(
            "tahlan_miasma_drone_wing",
            "tahlan_flash_dmn_wing",
            "tahlan_spark_dmn_wing",
            "tahlan_lux_dmn_wing",
            "tahlan_thunder_dmn_wing",
            "tahlan_gaze_dmn_wing"
        )

        val DAEMON_WEAPONS = mutableListOf(
            "kineticblaster",
            "gigacannon"
        )


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

        @Throws(IOException::class, JSONException::class)
        private fun loadTahlanSettings() {
            val setting = Global.getSettings().loadJSON(SETTINGS_FILE)
            ENABLE_LETHIA = setting.getBoolean("enableLethia")
            ENABLE_LEGIO = setting.getBoolean("enableLegio")
            ENABLE_LIFELESS = setting.getBoolean("enableLifelessShips")
            ENABLE_LEGIOBPS = setting.getBoolean("enableLegioBlueprintLearning")
            ENABLE_HARDMODE = setting.getBoolean("enableHardMode")
            ENABLE_ADAPTIVEMODE = setting.getBoolean("enableAdaptiveMode")
            ENABLE_DAEMONS = setting.getBoolean("enableDaemons")
            ENABLE_FASTMODE = setting.getBoolean("enableFastmode")
            WEEB_MODE = setting.getBoolean("enableWaifu")
        }
    }
}