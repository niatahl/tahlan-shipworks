package data.missions.tahlan_sinsofthepast;

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
		api.initFleet(FleetSide.PLAYER, "KNV", FleetGoal.ATTACK, false, 2);
		api.initFleet(FleetSide.ENEMY, "NN", FleetGoal.ATTACK, true, 10);

		// Set a blurb for each fleet
		api.setFleetTagline(FleetSide.PLAYER, "Patrol Fleet Karda Secundus");
		api.setFleetTagline(FleetSide.ENEMY, "The Mind Without Name");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("The KNV Anra Emmeris must survive");
		
		// Set up the player's fleet
		api.addToFleet(FleetSide.PLAYER, "tahlan_legion_gh_knight", FleetMemberType.SHIP, "KNV Anra Emmeris", true);
		api.addToFleet(FleetSide.PLAYER, "tahlan_Castigator_knight_errant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_Castigator_knight_errant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_enforcer_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_enforcer_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_monitor_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_monitor_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_monitor_gh_knight", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "tahlan_monitor_gh_knight", FleetMemberType.SHIP, false);
		//api.addToFleet(FleetSide.PLAYER, "onslaught_Standard", FleetMemberType.SHIP, "TTS Invincible", true, CrewXPLevel.ELITE);
		
		// Mark player flagship as essential
		api.defeatOnShipLoss("KNV Anra Emmeris");
		
		// Set up the enemy fleet

		api.addToFleet(FleetSide.ENEMY, "tahlan_Timeless_standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tahlan_Nameless_standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tahlan_Nameless_standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tahlan_Nameless_standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tahlan_Nameless_standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tahlan_Nameless_standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "tahlan_Nameless_standard", FleetMemberType.SHIP, false);

		
		
		// Set up the map.
		float width = 24000f;
		float height = 18000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		for (int i = 0; i < 15; i++) {
			float x = (float) Math.random() * width - width/2;
			float y = (float) Math.random() * height - height/2;
			float radius = 100f + (float) Math.random() * 900f; 
			api.addNebula(x, y, radius);
		}
		api.setBackgroundSpriteName("graphics/backgrounds/background2.jpg");
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






