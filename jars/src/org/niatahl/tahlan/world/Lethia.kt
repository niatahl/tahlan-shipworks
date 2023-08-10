package org.niatahl.tahlan.world

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.ShipRecoverySpecialCreator
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.ShipCondition
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import java.awt.Color
import java.util.*

class Lethia {
    fun generate(sector: SectorAPI) {
        val system = sector.createStarSystem("Lethia")
        system.addTag(Tags.THEME_CORE_POPULATED)
        when (MathUtils.getRandomNumberInRange(1, 4)) {
            1 -> system.location[45000f] = 30000f
            2 -> system.location[-46000f] = 28000f
            3 -> system.location[-42000f] = -31000f
            4 -> system.location[41000f] = -27000f
        }
        system.backgroundTextureFilename = "graphics/tahlan/backgrounds/tahlan_lethia.jpg"
        val lethia_star = system.initStar(
            "tahlan_lethia",
            "star_yellow",
            350f,
            600f
        )
        system.lightColor = Color(255, 255, 255)

        val atanor = system.addPlanet(
            "tahlan_lethia_p02",
            lethia_star,
            "Atanor",
            "lava",
            360f * Math.random().toFloat(),
            180f,
            2800f,
            170f
        )
        PlanetConditionGenerator.generateConditionsForPlanet(atanor, StarAge.AVERAGE)
        atanor.customDescriptionId = "tahlan_planet_atanor"
        atanor.market.addCondition("tahlan_kassadariclaim")

        //debris field near Atanor
        val params1 = DebrisFieldParams(
            130f,
            1.2f,
            10000000f,
            10000000f
        )
        params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE
        params1.baseSalvageXP = 550
        params1.glowColor = Color.white
        val debrisLethia2 = Misc.addDebrisField(system, params1, StarSystemGenerator.random)
        debrisLethia2.sensorProfile = 1500f
        debrisLethia2.isDiscoverable = true
        debrisLethia2.setCircularOrbit(atanor, 360 * Math.random().toFloat(), 300f, 250f)
        debrisLethia2.id = "tahlan_lethia_debrisLethia2"

        //asteroid ring
        system.addAsteroidBelt(lethia_star, 1000, 3800f, 1000f, 120f, 500f, Terrain.ASTEROID_BELT, "The Brink")
        system.addRingBand(lethia_star, "misc", "rings_asteroids0", 256f, 3, Color.gray, 256f, 3650f, 220f)
        system.addRingBand(lethia_star, "misc", "rings_asteroids0", 256f, 0, Color.gray, 256f, 3800f, 370f)
        system.addRingBand(lethia_star, "misc", "rings_asteroids0", 256f, 2, Color.gray, 256f, 4050f, 235f)
        addDerelict(system, lethia_star, "tahlan_onslaught_gh_knight", ShipCondition.BATTERED, 3900f, Math.random() < 0.05)
        val akrom = system.addPlanet(
            "tahlan_lethia_p03",
            lethia_star,
            "Akrom",
            "barren",
            360f * Math.random().toFloat(),
            140f,
            4500f,
            240f
        )
        PlanetConditionGenerator.generateConditionsForPlanet(akrom, StarAge.AVERAGE)
        val stableLoc1: SectorEntityToken = system.addCustomEntity("tahlan_lethia_stableloc_1", "Stable Location", "stable_location", Factions.NEUTRAL)
        stableLoc1.setCircularOrbit(lethia_star, MathUtils.getRandomNumberInRange(0f, 360f), 5100f, 460f)
        val heridal = system.addPlanet(
            "tahlan_lethia_p04",
            lethia_star,
            "Heridal",
            "jungle",
            360f * Math.random().toFloat(),
            170f,
            5400f,
            190f
        )
        PlanetConditionGenerator.generateConditionsForPlanet(heridal, StarAge.AVERAGE)
        heridal.customDescriptionId = "tahlan_planet_heridal"
        heridal.market.addCondition("tahlan_kassadariclaim")

        //debris field near Heridal
        val params2 = DebrisFieldParams(
            150f,
            1.3f,
            10000000f,
            10000000f
        )
        params2.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE
        params2.baseSalvageXP = 650
        params2.glowColor = Color.white
        val debrisLethia4 = Misc.addDebrisField(system, params2, StarSystemGenerator.random)
        debrisLethia4.sensorProfile = 1500f
        debrisLethia4.isDiscoverable = true
        debrisLethia4.setCircularOrbit(heridal, 360 * Math.random().toFloat(), 350f, 280f)
        debrisLethia4.id = "tahlan_lethia_debrisLethia4"
        val relay: SectorEntityToken = system.addCustomEntity(
            "tahlan_lethia_relay",  // unique id
            "Lethia Relay",  // name - if null, defaultName from custom_entities.json will be used
            "comm_relay",  // type of object, defined in custom_entities.json
            "independent"
        ) // faction
        relay.setCircularOrbitPointingDown(lethia_star, 360f * Math.random().toFloat(), 5900f, MathUtils.getRandomNumberInRange(250, 410).toFloat())
        val kassadar = system.addPlanet(
            "tahlan_lethia_p05",
            lethia_star,
            "Kassadar",
            "tundra",
            360f * Math.random().toFloat(),
            190f,
            6600f,
            260f
        )
        kassadar.customDescriptionId = "tahlan_planet_kassadar"
        kassadar.setInteractionImage("illustrations", "tahlan_kassadar_illus")
        system.addRingBand(kassadar, "misc", "rings_dust0", 256f, 1, Color.gray, 256f, 360f, 400f)
        val kassadarMarket = addMarketplace(
            "independent", kassadar, null,
            "Kassadar",
            7,
            ArrayList(
                Arrays.asList(
                    Conditions.POPULATION_7,
                    Conditions.HABITABLE,
                    Conditions.FARMLAND_POOR,
                    Conditions.ORE_MODERATE,
                    Conditions.RARE_ORE_SPARSE,
                    Conditions.ORGANICS_TRACE,
                    Conditions.COLD,
                    Conditions.LARGE_REFUGEE_POPULATION
                )
            ),
            ArrayList(
                Arrays.asList(
                    Submarkets.GENERIC_MILITARY,
                    Submarkets.SUBMARKET_OPEN,
                    Submarkets.SUBMARKET_STORAGE,
                    "tahlan_kassadarmarket"
                )
            ),
            ArrayList(
                Arrays.asList(
                    Industries.POPULATION,
                    Industries.MEGAPORT,
                    Industries.LIGHTINDUSTRY,
                    Industries.FARMING,
                    Industries.WAYSTATION,
                    Industries.STARFORTRESS,
                    Industries.HEAVYBATTERIES,
                    Industries.MILITARYBASE,
                    Industries.ORBITALWORKS
                )
            ),
            0.3f,
            false,
            true
        )
        kassadarMarket.addTag("magellan_indiemarket")
        val stableLoc2: SectorEntityToken = system.addCustomEntity("tahlan_lethia_stableloc_2", "Stable Location", "stable_location", Factions.NEUTRAL)
        stableLoc2.setCircularOrbit(lethia_star, MathUtils.getRandomNumberInRange(0f, 360f), 8200f, 520f)

        //Jump point for Kassadar
        val jumpPoint1 = Global.getFactory().createJumpPoint("tahlan_lethia_kassadar_jump", "Kassadar Jump Point")
        jumpPoint1.setCircularOrbit(system.getEntityById("tahlan_lethia_p05"), 290f, 1400f, 120f)
        jumpPoint1.relatedPlanet = kassadar
        system.addEntity(jumpPoint1)

        // Some procgen can go out here.
        val radiusAfter = StarSystemGenerator.addOrbitingEntities(
            system, lethia_star, StarAge.AVERAGE,
            1, 2,  // min/max entities to add
            8000f,  // radius to start adding at
            5,  // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
            true
        ) // whether to use custom or system-name based names


        // Inactive gate at the edge of the system
        val lethiaGate: SectorEntityToken = system.addCustomEntity(
            "tahlan_lethia_gate",
            "Lethia Gate",
            "inactive_gate",
            null
        )
        lethiaGate.setCircularOrbit(lethia_star, MathUtils.getRandomNumberInRange(0f, 360f), radiusAfter + 1000, 520f)
        lethiaGate.customDescriptionId = "tahlan_gate_lethia"

        //debris fields around gate
        val params3 = DebrisFieldParams(
            600f,
            0.8f,
            10000000f,
            10000000f
        )
        params3.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE
        params3.baseSalvageXP = 850
        params3.glowColor = Color.white
        val debrisLethiaGate1 = Misc.addDebrisField(system, params3, StarSystemGenerator.random)
        debrisLethiaGate1.sensorProfile = 1500f
        debrisLethiaGate1.isDiscoverable = true
        debrisLethiaGate1.setCircularOrbit(lethiaGate, 360 * Math.random().toFloat(), 100f, 250f)
        debrisLethiaGate1.id = "tahlan_lethia_debrisGate1"

        //derelicts near gate
        addDerelict(system, lethiaGate, "tahlan_Ristreza_knight", ShipCondition.BATTERED, 500f, Math.random() < 0.1)
        addDerelict(system, lethiaGate, "tahlan_Vale_crusader", ShipCondition.AVERAGE, 240f, Math.random() < 0.2)
        val debrisLethiaRim = Misc.addDebrisField(system, params3, StarSystemGenerator.random)
        debrisLethiaRim.sensorProfile = 1200f
        debrisLethiaRim.isDiscoverable = true
        debrisLethiaRim.setCircularOrbit(lethia_star, 360 * Math.random().toFloat(), radiusAfter + 2200, 640f)
        debrisLethiaRim.id = "tahlan_lethia_debrisRim"

        //remainders of the Sins of the Past mission
        addDerelict(system, debrisLethiaRim, "tahlan_legion_gh_knight", ShipCondition.WRECKED, 200f, Math.random() < 0.1)
        addDerelict(system, debrisLethiaRim, "tahlan_Timeless_standard", ShipCondition.WRECKED, 240f, false)
        addDerelict(system, debrisLethiaRim, "tahlan_Nameless_standard", ShipCondition.WRECKED, 300f, false)
        addDerelict(system, debrisLethiaRim, "tahlan_Nameless_standard", ShipCondition.WRECKED, 320f, false)

        // generates hyperspace destinations for in-system jump points
        system.autogenerateHyperspaceJumpPoints(true, true)

        Misc.setAllPlanetsSurveyed(system, true)

        //Finally cleans up hyperspace
        cleanup(system)
    }

    //Shorthand function for cleaning up hyperspace
    private fun cleanup(system: StarSystemAPI) {
        val plugin = Misc.getHyperspaceTerrain().plugin as HyperspaceTerrainPlugin
        val editor = NebulaEditor(plugin)
        val minRadius = plugin.tileSize * 2f
        val radius = system.maxRadiusInHyperspace
        editor.clearArc(system.location.x, system.location.y, 0f, radius + minRadius * 0.5f, 0f, 360f)
        editor.clearArc(system.location.x, system.location.y, 0f, radius + minRadius, 0f, 360f, 0.25f)
    }

    //Shorthand for adding derelicts, thanks Tart
    protected fun addDerelict(
        system: StarSystemAPI?, focus: SectorEntityToken?, variantId: String?,
        condition: ShipCondition?, orbitRadius: Float, recoverable: Boolean
    ) {
        val params = DerelictShipData(PerShipData(variantId, condition), false)
        val ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params)
        ship.isDiscoverable = true
        val orbitDays = orbitRadius / (10f + Math.random().toFloat() * 5f)
        ship.setCircularOrbit(focus, Math.random().toFloat() * 360f, orbitRadius, orbitDays)
        if (recoverable) {
            val creator = ShipRecoverySpecialCreator(null, 0, 0, false, null, null)
            Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null))
        }
    }

    companion object {
        //Shorthand function for adding a market
        fun addMarketplace(
            factionID: String?, primaryEntity: SectorEntityToken, connectedEntities: ArrayList<SectorEntityToken>?, name: String?,
            size: Int, marketConditions: ArrayList<String>, submarkets: ArrayList<String>?, industries: ArrayList<String>, tarrif: Float,
            freePort: Boolean, withJunkAndChatter: Boolean
        ): MarketAPI {
            val globalEconomy = Global.getSector().economy
            val planetID = primaryEntity.id
            val marketID = planetID + "_market"
            val newMarket = Global.getFactory().createMarket(marketID, name, size)
            newMarket.factionId = factionID
            newMarket.primaryEntity = primaryEntity
            newMarket.tariff.modifyFlat("generator", tarrif)

            //Adds submarkets
            if (null != submarkets) {
                for (market in submarkets) {
                    newMarket.addSubmarket(market)
                }
            }

            //Adds market conditions
            for (condition in marketConditions) {
                newMarket.addCondition(condition)
            }

            //Add market industries
            for (industry in industries) {
                newMarket.addIndustry(industry)
            }

            //Sets us to a free port, if we should
            newMarket.isFreePort = freePort

            //Adds our connected entities, if any
            if (null != connectedEntities) {
                for (entity in connectedEntities) {
                    newMarket.connectedEntities.add(entity)
                }
            }
            globalEconomy.addMarket(newMarket, withJunkAndChatter)
            primaryEntity.market = newMarket
            primaryEntity.setFaction(factionID)
            if (null != connectedEntities) {
                for (entity in connectedEntities) {
                    entity.market = newMarket
                    entity.setFaction(factionID)
                }
            }

            //Finally, return the newly-generated market
            return newMarket
        }
    }
}