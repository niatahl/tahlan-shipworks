package org.niatahl.tahlan.world

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin.DerelictShipData
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.*
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.ShipRecoverySpecialCreator
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial.PerShipData
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin.CoronaParams
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
        val rubiconStar = system.initStar(
            "tahlan_rubicon_maw",
            "black_hole",
            140f,
            300f
        )
        rubiconStar.name = "The Abyssal Maw"
        setBlackHole(rubiconStar, system)
        system.lightColor = Color(200, 100, 90)
        system.addRingBand(rubiconStar, "misc", "rings_asteroids0", 256f, 3, Color(68, 57, 56), 300f, 1000f, 200f)
        system.addRingBand(rubiconStar, "misc", "rings_dust0", 256f, 2, Color(201, 77, 49), 600f, 700f, 200f)
        system.addRingBand(rubiconStar, "misc", "rings_dust0", 256f, 3, Color(201, 49, 49), 300f, 300f, 200f)
        system.addRingBand(rubiconStar, "misc", "rings_dust0", 256f, 3, Color.gray, 600f, 900f, 200f)
        system.addRingBand(rubiconStar, "misc", "rings_dust0", 256f, 4, Color(119, 48, 48), 600f, 500f, 200f)
        system.addRingBand(rubiconStar, "misc", "rings_dust0", 256f, 1, Color.gray, 600f, 700f, 200f)
        //        system.addAsteroidBelt(rubicon_star, 300, 1200f, 500, 120, 300, Terrain.ASTEROID_BELT,"");
        system.addTerrain(
            Terrain.MAGNETIC_FIELD, MagneticFieldParams(
                1800f,  // terrain effect band width
                1300f,  // terrain effect middle radius
                rubiconStar,  // entity that it's around
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
        debrisRubicon1.setCircularOrbit(rubiconStar, 360 * Math.random().toFloat(), 700f, 250f)
        debrisRubicon1.id = "tahlan_lethia_debrisRubicon1"
        val debrisRubicon2 = Misc.addDebrisField(system, params1, StarSystemGenerator.random)
        debrisRubicon2.sensorProfile = 1500f
        debrisRubicon2.isDiscoverable = true
        debrisRubicon2.setCircularOrbit(rubiconStar, 360 * Math.random().toFloat(), 1300f, 300f)
        debrisRubicon2.id = "tahlan_lethia_debrisRubicon2"
        val rubicon_star2 = system.addPlanet("tahlan_rubicon_heart", rubiconStar, "The Infernal Heart", "star_red_dwarf", 90f, 200f, 2100f, 300f)
        system.addCorona(rubicon_star2, 200f, 5f, 0.2f, 2f)

        system.addAsteroidBelt(rubiconStar, 1000, 3000f, 1000f, 120f, 500f, Terrain.ASTEROID_BELT, "")
        system.addRingBand(rubiconStar, "misc", "rings_asteroids0", 256f, 1, Color.gray, 500f, 3000f, 250f)
        system.addRingBand(rubiconStar, "misc", "rings_dust0", 256f, 1, Color.gray, 500f, 3200f, 250f)
        system.addRingBand(rubiconStar, "misc", "rings_dust0", 256f, 4, Color.gray, 500f, 2800f, 250f)

        // First Planet
        val melchiresa = system.addPlanet(
            "tahlan_rubicon_p01",
            rubiconStar,
            "Melchiresa",
            "cryovolcanic",
            360f * Math.random().toFloat(),
            150f,
            4600f,
            320f
        )
        melchiresa.customDescriptionId = "tahlan_rubicon_p01"
        melchiresa.setInteractionImage("illustrations", "tahlan_melchiresa_illus")
        val melchiresaMarket = addMarketplace(
            "tahlan_legioinfernalis", melchiresa, null,
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
            freePort = true,
            withJunkAndChatter = true
        )
        melchiresaMarket.getIndustry(Industries.MILITARYBASE).aiCoreId = Commodities.ALPHA_CORE
        melchiresaMarket.getIndustry(Industries.STARFORTRESS).aiCoreId = Commodities.ALPHA_CORE
        melchiresaMarket.getIndustry(Industries.MEGAPORT).aiCoreId = Commodities.ALPHA_CORE
        melchiresaMarket.getIndustry(Industries.HEAVYBATTERIES).aiCoreId = Commodities.ALPHA_CORE
        val stableLoc1: SectorEntityToken = system.addCustomEntity("tahlan_rubicon_stableloc_1", "Stable Location", "stable_location", Factions.NEUTRAL)
        stableLoc1.setCircularOrbit(rubiconStar, MathUtils.getRandomNumberInRange(0f, 360f), 5400f, 520f)

        // Second Planet
        val ornias = system.addPlanet(
            "tahlan_rubicon_p02",
            rubiconStar,
            "Ornias",
            "barren3",
            360f * Math.random().toFloat(),
            210f,
            6000f,
            410f
        )
        PlanetConditionGenerator.generateConditionsForPlanet(ornias, StarAge.OLD)
        val relay: SectorEntityToken = system.addCustomEntity(
            "tahlan_rubicon_relay",  // unique id
            "Rubicon Relay",  // name - if null, defaultName from custom_entities.json will be used
            "comm_relay",  // type of object, defined in custom_entities.json
            "tahlan_legioinfernalis"
        ) // faction
        relay.setCircularOrbitPointingDown(rubiconStar, 360f * Math.random().toFloat(), 6700f, MathUtils.getRandomNumberInRange(250, 410).toFloat())

        // Third Planet - Primary Legio base
        val angle = 360f * Math.random().toFloat()
        val lucifron = system.addPlanet(
            "tahlan_rubicon_p03",
            rubiconStar,
            "Lucifron",
            "toxic_cold",
            angle,
            320f,
            7400f,
            380f
        )
        lucifron.customDescriptionId = "tahlan_rubicon_p03"
        lucifron.setInteractionImage("illustrations", "tahlan_lucifron_illus")
        val lucifronMarket = addMarketplace(
            "tahlan_legioinfernalis", lucifron, null,
            "Lucifron",
            7,
            ArrayList(
                listOf(
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
                listOf(
                    Submarkets.GENERIC_MILITARY,
                    Submarkets.SUBMARKET_OPEN,
                    Submarkets.SUBMARKET_STORAGE,
                    Submarkets.SUBMARKET_BLACK
                )
            ),
            ArrayList(
                listOf(
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
            freePort = false,
            withJunkAndChatter = true
        )
        lucifronMarket.addIndustry(Industries.ORBITALWORKS, ArrayList(listOf(Items.PRISTINE_NANOFORGE)))
        lucifronMarket.getIndustry(Industries.HIGHCOMMAND).aiCoreId = Commodities.ALPHA_CORE
        lucifronMarket.getIndustry(Industries.STARFORTRESS).aiCoreId = Commodities.ALPHA_CORE
        lucifronMarket.getIndustry(Industries.HEAVYBATTERIES).aiCoreId = Commodities.ALPHA_CORE


        //debris of failed attacks
        val debrisRubicon3 = Misc.addDebrisField(system, params1, StarSystemGenerator.random)
        debrisRubicon3.sensorProfile = 1500f
        debrisRubicon3.isDiscoverable = true
        debrisRubicon3.setCircularOrbit(lucifron, 360 * Math.random().toFloat(), 600f, 200f)
        debrisRubicon3.id = "tahlan_lethia_debrisRubicon3"
        val debrisRubicon4 = Misc.addDebrisField(system, params1, StarSystemGenerator.random)
        debrisRubicon4.sensorProfile = 1500f
        debrisRubicon4.isDiscoverable = true
        debrisRubicon4.setCircularOrbit(lucifron, 360 * Math.random().toFloat(), 800f, 250f)
        debrisRubicon4.id = "tahlan_lethia_debrisRubicon4"

        //Jump point for Lucifron
        val jumpPoint1 = Global.getFactory().createJumpPoint("tahlan_rubicon_lucifron_jump", "Lucifron Jump Point")
        jumpPoint1.setCircularOrbit(rubiconStar, angle + 25f, 7400f, 380f)
        jumpPoint1.relatedPlanet = lucifron
        system.addEntity(jumpPoint1)

        // Let's procgen some stuff here cause fuck doing that manually
        val radiusAfter = StarSystemGenerator.addOrbitingEntities(
            system, rubiconStar, StarAge.OLD,
            4, 6,  // min/max entities to add
            8000f,  // radius to start adding at
            3,  // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
            true
        ) // whether to use custom or system-name based names


        // Small outpost at system edge
        val adramelech: SectorEntityToken = system.addCustomEntity("tahlan_rubicon_outpost", "Adramelech Fortress", "station_side06", "tahlan_legioinfernalis")
        adramelech.setCircularOrbitPointingDown(rubiconStar, 360 * Math.random().toFloat(), radiusAfter + 700f, 600f)
        adramelech.customDescriptionId = "tahlan_rubicon_outpost"
        adramelech.setInteractionImage("illustrations", "tahlan_adramelech_illus")
        val adramelechMarket = addMarketplace(
            "tahlan_legioinfernalis", adramelech, null,
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
            freePort = false,
            withJunkAndChatter = true
        )
        adramelechMarket.getIndustry(Industries.MILITARYBASE).aiCoreId = Commodities.ALPHA_CORE
        adramelechMarket.getIndustry(Industries.STARFORTRESS).aiCoreId = Commodities.ALPHA_CORE
        adramelechMarket.getIndustry(Industries.HEAVYBATTERIES).aiCoreId = Commodities.ALPHA_CORE


        // Bit more procgen
        val radiusAfter2 = StarSystemGenerator.addOrbitingEntities(
            system, rubiconStar, StarAge.OLD,
            2, 3,  // min/max entities to add
            radiusAfter + 1500f,  // radius to start adding at
            3,  // name offset - next planet will be <system name> <roman numeral of this parameter + 1>
            true
        ) // whether to use custom or system-name based names

        // add nightwatch derelicts in random spots
        addDerelict(system, rubicon_star2, "tahlan_eagle_nw_enforcer", ShipRecoverySpecial.ShipCondition.WRECKED, (800f..radiusAfter2).random(), Math.random() < 0.25)
        addDerelict(system, rubicon_star2, "tahlan_falcon_nw_enforcer", ShipRecoverySpecial.ShipCondition.BATTERED, (800f..radiusAfter2).random(), Math.random() < 0.25)
        addDerelict(system, rubicon_star2, "tahlan_falcon_nw_enforcer", ShipRecoverySpecial.ShipCondition.WRECKED, (800f..radiusAfter2).random(), Math.random() < 0.25)

        // generates hyperspace destinations for in-system jump points
        system.autogenerateHyperspaceJumpPoints(true, true)
        if (TahlanModPlugin.HAS_INDEVO) {
            addDefenses()
        } else {
            melchiresaMarket.addIndustry(Industries.PLANETARYSHIELD)
            lucifronMarket.addIndustry(Industries.PLANETARYSHIELD)
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

    protected fun setBlackHole(star: PlanetAPI, system: StarSystemAPI) {
        val coronaPlugin = Misc.getCoronaFor(star)
        if (coronaPlugin != null) {
            system.removeEntity(coronaPlugin.entity)
        }
        val starData = Global.getSettings().getSpec(StarGenDataSpec::class.java, star.spec.planetType, false) as StarGenDataSpec
        var corona = star.radius * (starData.coronaMult + starData.coronaVar * (StarSystemGenerator.random.nextFloat() - 0.5f))
        if (corona < starData.coronaMin) corona = starData.coronaMin
        val eventHorizon: SectorEntityToken = system.addTerrain(
            Terrain.EVENT_HORIZON,
            CoronaParams(
                star.radius + corona, (star.radius + corona) / 2f,
                star, starData.solarWind,
                (starData.minFlare + (starData.maxFlare - starData.minFlare) * StarSystemGenerator.random.nextFloat()),
                starData.crLossMult
            )
        )
        eventHorizon.setCircularOrbit(star, 0f, 0f, 100f)
    }
}