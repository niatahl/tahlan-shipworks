package org.niatahl.tahlan.world

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.ShipRecoverySpecialCreator
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.niatahl.tahlan.plugins.TahlanModPlugin
import org.niatahl.tahlan.utils.IndEvoIntegrations.addDefenses
import org.niatahl.tahlan.utils.random
import org.niatahl.tahlan.world.Lethia.Companion.addMarketplace
import java.awt.Color
import java.util.*

class Rubicon {
    fun generate(sector: SectorAPI) {
        val system = sector.createStarSystem("Rubicon")
        system.location[-28000f] = -4500f
        system.backgroundTextureFilename = "graphics/tahlan/backgrounds/tahlan_rubicon.jpg"
        system.addTag(Tags.THEME_CORE_POPULATED)
        val rubicon_star = system.initStar(
            "tahlan_rubicon_maw",
            "black_hole",
            140f,
            300f
        )
        rubicon_star.name = "The Abyssal Maw"
        system.lightColor = Color(255, 205, 205)
        system.addRingBand(rubicon_star, "misc", "rings_asteroids0", 256f, 3, Color(68, 57, 56), 300f, 1000f, 200f)
        system.addRingBand(rubicon_star, "misc", "rings_dust0", 256f, 2, Color(201, 77, 49), 600f, 700f, 200f)
        system.addRingBand(rubicon_star, "misc", "rings_dust0", 256f, 3, Color(201, 49, 49), 300f, 300f, 200f)
        system.addRingBand(rubicon_star, "misc", "rings_dust0", 256f, 3, Color.gray, 600f, 900f, 200f)
        system.addRingBand(rubicon_star, "misc", "rings_dust0", 256f, 4, Color(119, 48, 48), 600f, 500f, 200f)
        system.addRingBand(rubicon_star, "misc", "rings_dust0", 256f, 1, Color.gray, 600f, 700f, 200f)
        //        system.addAsteroidBelt(rubicon_star, 300, 1200f, 500, 120, 300, Terrain.ASTEROID_BELT,"");
        system.addTerrain(
            Terrain.MAGNETIC_FIELD, MagneticFieldParams(
                1800f,  // terrain effect band width
                1300f,  // terrain effect middle radius
                rubicon_star,  // entity that it's around
                190f,  // visual band start
                600f,  // visual band end
                Color(157, 28, 9, 150),  // base color
                0.5f,  // probability to spawn aurora sequence, checked once/day when no aurora in progress
                Color(180, 118, 73),
                Color(190, 128, 105),
                Color(225, 150, 123),
                Color(240, 152, 132),
                Color(250, 33, 25),
                Color(240, 28, 0),
                Color(150, 0, 0)
            )
        )

        // Debris fields
        val params1 = DebrisFieldParams(
            130f,
            1.2f,
            10000000f,
            10000000f
        )
        params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE
        params1.baseSalvageXP = 550
        params1.glowColor = Color.white
        val debrisRubicon1 = Misc.addDebrisField(system, params1, StarSystemGenerator.random)
        debrisRubicon1.sensorProfile = 1500f
        debrisRubicon1.isDiscoverable = true
        debrisRubicon1.setCircularOrbit(rubicon_star, 360 * Math.random().toFloat(), 700f, 250f)
        debrisRubicon1.id = "tahlan_lethia_debrisRubicon1"
        val debrisRubicon2 = Misc.addDebrisField(system, params1, StarSystemGenerator.random)
        debrisRubicon2.sensorProfile = 1500f
        debrisRubicon2.isDiscoverable = true
        debrisRubicon2.setCircularOrbit(rubicon_star, 360 * Math.random().toFloat(), 1300f, 300f)
        debrisRubicon2.id = "tahlan_lethia_debrisRubicon2"
        val rubicon_star2 = system.addPlanet("tahlan_rubicon_heart", rubicon_star, "The Infernal Heart", "star_red_dwarf", 90f, 200f, 2100f, 300f)
        system.addCorona(rubicon_star2, 200f, 5f, 0.2f, 2f)

//        addDerelict(system,rubicon_star2,"tahlan_DunScaith_barrage", ShipRecoverySpecial.ShipCondition.WRECKED, 500f, Math.random()<0.1);
        system.addAsteroidBelt(rubicon_star, 1000, 3000f, 1000f, 120f, 500f, Terrain.ASTEROID_BELT, "")
        system.addRingBand(rubicon_star, "misc", "rings_asteroids0", 256f, 1, Color.gray, 500f, 3000f, 250f)
        system.addRingBand(rubicon_star, "misc", "rings_dust0", 256f, 1, Color.gray, 500f, 3200f, 250f)
        system.addRingBand(rubicon_star, "misc", "rings_dust0", 256f, 4, Color.gray, 500f, 2800f, 250f)

        // First Planet
        val rubicon_1 = system.addPlanet(
            "tahlan_rubicon_p01",
            rubicon_star,
            "Melchiresa",
            "cryovolcanic",
            360f * Math.random().toFloat(),
            150f,
            4600f,
            320f
        )
        rubicon_1.customDescriptionId = "tahlan_rubicon_p01"
        rubicon_1.setInteractionImage("illustrations", "tahlan_melchiresa_illus")
        val rubicon_1_market = addMarketplace(
            "tahlan_legioinfernalis", rubicon_1, null,
            "Melchiresa",
            5,
            ArrayList(
                Arrays.asList(
                    Conditions.POPULATION_5,
                    Conditions.ORE_RICH,
                    Conditions.RARE_ORE_ABUNDANT,
                    Conditions.COLD,
                    Conditions.LOW_GRAVITY,
                    Conditions.ORGANIZED_CRIME,
                    "tahlan_legiotyranny"
                )
            ),
            ArrayList(
                Arrays.asList(
                    Submarkets.GENERIC_MILITARY,
                    Submarkets.SUBMARKET_OPEN,
                    Submarkets.SUBMARKET_STORAGE,
                    Submarkets.SUBMARKET_BLACK
                )
            ),
            ArrayList(
                Arrays.asList(
                    Industries.POPULATION,
                    Industries.MEGAPORT,
                    Industries.MINING,
                    Industries.STARFORTRESS,
                    Industries.HEAVYBATTERIES,
                    Industries.MILITARYBASE,
                    Industries.REFINING
                )
            ),
            0.3f,
            true,
            true
        )
        rubicon_1_market.getIndustry(Industries.MILITARYBASE).aiCoreId = Commodities.ALPHA_CORE
        rubicon_1_market.getIndustry(Industries.STARFORTRESS).aiCoreId = Commodities.ALPHA_CORE
        rubicon_1_market.getIndustry(Industries.MEGAPORT).aiCoreId = Commodities.ALPHA_CORE
        rubicon_1_market.getIndustry(Industries.HEAVYBATTERIES).aiCoreId = Commodities.ALPHA_CORE
        val stableLoc1: SectorEntityToken = system.addCustomEntity("tahlan_rubicon_stableloc_1", "Stable Location", "stable_location", Factions.NEUTRAL)
        stableLoc1.setCircularOrbit(rubicon_star, MathUtils.getRandomNumberInRange(0f, 360f), 5400f, 520f)

        // Second Planet
        val rubicon_2 = system.addPlanet(
            "tahlan_rubicon_p02",
            rubicon_star,
            "Ornias",
            "barren3",
            360f * Math.random().toFloat(),
            210f,
            6000f,
            410f
        )
        PlanetConditionGenerator.generateConditionsForPlanet(rubicon_2, StarAge.OLD)
        val relay: SectorEntityToken = system.addCustomEntity(
            "tahlan_rubicon_relay",  // unique id
            "Rubicon Relay",  // name - if null, defaultName from custom_entities.json will be used
            "comm_relay",  // type of object, defined in custom_entities.json
            "tahlan_legioinfernalis"
        ) // faction
        relay.setCircularOrbitPointingDown(rubicon_star, 360f * Math.random().toFloat(), 6700f, MathUtils.getRandomNumberInRange(250, 410).toFloat())

        // Third Planet - Primary Legio base
        val angle = 360f * Math.random().toFloat()
        val rubicon_3 = system.addPlanet(
            "tahlan_rubicon_p03",
            rubicon_star,
            "Lucifron",
            "toxic_cold",
            angle,
            320f,
            7400f,
            380f
        )
        rubicon_3.customDescriptionId = "tahlan_rubicon_p03"
        rubicon_3.setInteractionImage("illustrations", "tahlan_lucifron_illus")
        val rubicon_3_market = Lethia.addMarketplace(
            "tahlan_legioinfernalis", rubicon_3, null,
            "Lucifron",
            7,
            ArrayList(
                Arrays.asList(
                    Conditions.POPULATION_7,
                    Conditions.ORE_SPARSE,
                    Conditions.RARE_ORE_SPARSE,
                    Conditions.ORGANICS_COMMON,
                    Conditions.COLD,
                    Conditions.HIGH_GRAVITY,
                    Conditions.ORGANIZED_CRIME,
                    Conditions.REGIONAL_CAPITAL,
                    "tahlan_legiotyranny"
                )
            ),
            ArrayList(
                Arrays.asList(
                    Submarkets.GENERIC_MILITARY,
                    Submarkets.SUBMARKET_OPEN,
                    Submarkets.SUBMARKET_STORAGE,
                    Submarkets.SUBMARKET_BLACK
                )
            ),
            ArrayList(
                Arrays.asList(
                    Industries.POPULATION,
                    Industries.MEGAPORT,
                    Industries.MINING,
                    Industries.STARFORTRESS,
                    Industries.HEAVYBATTERIES,
                    Industries.HIGHCOMMAND,
                    Industries.LIGHTINDUSTRY,
                    "tahlan_CloningFacility",
                    "tahlan_legiohq"
                )
            ),
            0.3f,
            false,
            true
        )
        rubicon_3_market.addIndustry(Industries.ORBITALWORKS, ArrayList(listOf(Items.PRISTINE_NANOFORGE)))
        rubicon_3_market.getIndustry(Industries.HIGHCOMMAND).aiCoreId = Commodities.ALPHA_CORE
        rubicon_3_market.getIndustry(Industries.STARFORTRESS).aiCoreId = Commodities.ALPHA_CORE
        rubicon_3_market.getIndustry(Industries.HEAVYBATTERIES).aiCoreId = Commodities.ALPHA_CORE


        //debris of failed attacks
        val debrisRubicon3 = Misc.addDebrisField(system, params1, StarSystemGenerator.random)
        debrisRubicon3.sensorProfile = 1500f
        debrisRubicon3.isDiscoverable = true
        debrisRubicon3.setCircularOrbit(rubicon_3, 360 * Math.random().toFloat(), 600f, 200f)
        debrisRubicon3.id = "tahlan_lethia_debrisRubicon3"
        val debrisRubicon4 = Misc.addDebrisField(system, params1, StarSystemGenerator.random)
        debrisRubicon4.sensorProfile = 1500f
        debrisRubicon4.isDiscoverable = true
        debrisRubicon4.setCircularOrbit(rubicon_3, 360 * Math.random().toFloat(), 800f, 250f)
        debrisRubicon4.id = "tahlan_lethia_debrisRubicon4"

        //Jump point for Lucifron
        val jumpPoint1 = Global.getFactory().createJumpPoint("tahlan_rubicon_lucifron_jump", "Lucifron Jump Point")
        jumpPoint1.setCircularOrbit(rubicon_star, angle + 25f, 7400f, 380f)
        jumpPoint1.relatedPlanet = rubicon_3
        system.addEntity(jumpPoint1)

        // Let's procgen some stuff here cause fuck doing that manually
        val radiusAfter = StarSystemGenerator.addOrbitingEntities(
            system, rubicon_star, StarAge.OLD,
            4, 6,  // min/max entities to add
            8000f,  // radius to start adding at
            3,  // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
            true
        ) // whether to use custom or system-name based names


        // Small outpost at system edge
        val rubicon_outpost: SectorEntityToken = system.addCustomEntity("tahlan_rubicon_outpost", "Adramelech Fortress", "station_side06", "tahlan_legioinfernalis")
        rubicon_outpost.setCircularOrbitPointingDown(rubicon_star, 360 * Math.random().toFloat(), radiusAfter + 700f, 600f)
        rubicon_outpost.customDescriptionId = "tahlan_rubicon_outpost"
        rubicon_outpost.setInteractionImage("illustrations", "tahlan_adramelech_illus")
        val rubicon_outpost_market = Lethia.addMarketplace(
            "tahlan_legioinfernalis", rubicon_outpost, null,
            "Adramelech Fortress",
            5,
            ArrayList(
                Arrays.asList(
                    Conditions.POPULATION_3,
                    Conditions.NO_ATMOSPHERE,
                    Conditions.OUTPOST,
                    Conditions.ORGANIZED_CRIME,
                    "tahlan_legiotyranny"
                )
            ),
            ArrayList(
                Arrays.asList(
                    Submarkets.GENERIC_MILITARY,
                    Submarkets.SUBMARKET_OPEN,
                    Submarkets.SUBMARKET_STORAGE,
                    Submarkets.SUBMARKET_BLACK
                )
            ),
            ArrayList(
                Arrays.asList(
                    Industries.POPULATION,
                    Industries.SPACEPORT,
                    Industries.STARFORTRESS,
                    Industries.HEAVYBATTERIES,
                    Industries.MILITARYBASE
                )
            ),
            0.3f,
            false,
            true
        )
        rubicon_outpost_market.getIndustry(Industries.MILITARYBASE).aiCoreId = Commodities.ALPHA_CORE
        rubicon_outpost_market.getIndustry(Industries.STARFORTRESS).aiCoreId = Commodities.ALPHA_CORE
        rubicon_outpost_market.getIndustry(Industries.HEAVYBATTERIES).aiCoreId = Commodities.ALPHA_CORE


        // Bit more procgen
        val radiusAfter2 = StarSystemGenerator.addOrbitingEntities(
            system, rubicon_star, StarAge.OLD,
            2, 3,  // min/max entities to add
            radiusAfter + 1500f,  // radius to start adding at
            3,  // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
            true
        ) // whether to use custom or system-name based names

        // add nightwatch derelicts in random spots
        addDerelict(system,rubicon_star2,"tahlan_eagle_nw_enforcer", ShipRecoverySpecial.ShipCondition.WRECKED, (800f..radiusAfter2).random(), Math.random()<0.25)
        addDerelict(system,rubicon_star2,"tahlan_falcon_nw_enforcer", ShipRecoverySpecial.ShipCondition.BATTERED, (800f..radiusAfter2).random(), Math.random()<0.25)
        addDerelict(system,rubicon_star2,"tahlan_falcon_nw_enforcer", ShipRecoverySpecial.ShipCondition.WRECKED, (800f..radiusAfter2).random(), Math.random()<0.25)

        // generates hyperspace destinations for in-system jump points
        system.autogenerateHyperspaceJumpPoints(true, true)
        if (TahlanModPlugin.HAS_INDEVO) {
            addDefenses()
        } else {
            rubicon_1_market.addIndustry(Industries.PLANETARYSHIELD)
            rubicon_3_market.addIndustry(Industries.PLANETARYSHIELD)
        }

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
        condition: ShipRecoverySpecial.ShipCondition?, orbitRadius: Float, recoverable: Boolean
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
}