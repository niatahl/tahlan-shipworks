package data.missions.tahlan_01_izanagissacrifice;

import java.util.List;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		
		// Set up the fleets
		api.initFleet(FleetSide.PLAYER, "KNV", FleetGoal.ATTACK, false, 5);
		api.initFleet(FleetSide.ENEMY, "HDS", FleetGoal.ATTACK, true, 10);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "Kassadari Pursuit Squadron");
		api.setFleetTagline(FleetSide.ENEMY, "The Helldogs");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("The KNV Izanagi must survive");
		api.addBriefingItem("Leave none alive");
		
		// Set up the player's fleet
		api.addToFleet(FleetSide.PLAYER, "tahlan_Izanami_prototype", FleetMemberType.SHIP, "KNV Izanagi", true);
		api.addToFleet(FleetSide.PLAYER, "tahlan_dominator_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_enforcer_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_enforcer_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_monitor_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_monitor_gh_knight", FleetMemberType.SHIP, false);
		
		// Mark player flagship as essential
		api.defeatOnShipLoss("KNV Anra Emmeris");
		
		// Set up the enemy fleet

		api.addToFleet(FleetSide.ENEMY, "onslaught_Outdated", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_Outdated", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_Outdated", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "dominator_Outdated", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_d_pirates_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "enforcer_d_pirates_Strike", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "cerberus_d_pirates_Shielded", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "cerberus_d_pirates_Shielded", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "cerberus_d_pirates_Shielded", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_d_pirates_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_d_pirates_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "hound_d_pirates_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_d_pirates_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_d_pirates_Attack", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "wolf_d_pirates_Attack", FleetMemberType.SHIP, false);

		
		
		// Set up the map.
		float width = 24000f;
		float height = 18000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);

		api.setBackgroundSpriteName("graphics/tahlan/backgrounds/tahlan_lethia.jpg");
		//api.setBackgroundSpriteName("graphics/backgrounds/background2.jpg");
		
		//system.setBackgroundTextureFilename("graphics/backgrounds/background2.jpg");
		//api.setBackgroundSpriteName();
		
		// Add an asteroid field going diagonally across the
		// battlefield, 2000 pixels wide, with a maximum of 
		// 100 asteroids in it.
		// 20-70 is the range of asteroid speeds.
		api.addAsteroidField(0f, 0f, (float) Math.random() * 360f, width,
									20f, 70f, 150);
		
		
		api.addPlugin(new BaseEveryFrameCombatPlugin() {
			public void advance(float amount, List events) {
			}
			public void init(CombatEngineAPI engine) {
				engine.getContext().setStandoffRange(10000f);
			}
		});
	}

}






