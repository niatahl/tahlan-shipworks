package data.missions.tahlan_test;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.combat.ShipAPI;

public class MissionDefinition implements MissionDefinitionPlugin {

	public void defineMission(MissionDefinitionAPI api) {

		// Set up the fleets so we can add ships and fighter wings to them.
		// In this scenario, the fleets are attacking each other, but
		// in other scenarios, a fleet may be defending or trying to escape
		api.initFleet(FleetSide.PLAYER, "TSW", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 3);
//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 3);
		
		// Set a small blurb for each fleet that shows up on the mission detail and
		// mission results screens to identify each side.
		api.setFleetTagline(FleetSide.PLAYER, "Tahlan Shipworks fleet portfolio");
		api.setFleetTagline(FleetSide.ENEMY, "Space Junk");
		
		// These show up as items in the bulleted list under 
		// "Tactical Objectives" on the mission detail screen
		api.addBriefingItem("Initiate fleet performance tests");
		
		// Set up the player's fleet.  Variant names come from the
		// files in data/variants and data/variants/fighters
		//api.addToFleet(FleetSide.PLAYER, "SRD_Equilibrium_standard", FleetMemberType.SHIP, true);

		boolean isFlagship = true;
		for (String testID : Global.getSettings().getAllVariantIds()) {
			if (testID.contains("tahlan_") && Global.getSettings().getVariant(testID).getSource() == VariantSource.HULL && !Global.getSettings().getVariant(testID).getHullSize().equals(ShipAPI.HullSize.FIGHTER)) {
				api.addToFleet(FleetSide.PLAYER, testID, FleetMemberType.SHIP, isFlagship);
				isFlagship = false;
			}
		}
		
		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "colossus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus_Standard", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.ENEMY, "colossus_Standard", FleetMemberType.SHIP, false);
		
		// Set up the map.
		float width = 10000f;
		float height = 10000f;
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		// Add an asteroid field
		api.addAsteroidField(minX, minY + height / 2, 0, 8000f,
							 20f, 70f, 100);
		
		api.addPlanet(100f, -700f, 50f, StarTypes.RED_GIANT, 250f, true);
		
		api.addPlanet(1700f, 400, 550f, StarTypes.BLUE_SUPERGIANT, 450f, true);
	}
}
