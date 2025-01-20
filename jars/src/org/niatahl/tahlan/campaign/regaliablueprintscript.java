package org.niatahl.tahlan.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class regaliablueprintscript implements EveryFrameScript {


    private static final boolean secondInCommandMode = Global.getSettings().getModManager().isModEnabled("second_in_command");
    //ID/Tech Level
    private static final Map<String, Integer> VALID_APTITUDES = new HashMap<>();
    static {
        VALID_APTITUDES.put("sc_technology", 2);
        VALID_APTITUDES.put("sc_automated", 1);
        VALID_APTITUDES.put("rat_exotech", 1);
        VALID_APTITUDES.put("rat_abyssal", 1);
        VALID_APTITUDES.put("sc_dustkeeper", 1);
        VALID_APTITUDES.put("xo_synthesis", 1);
    }



    //This is the hull ID string the ship has to contain to trigger the script
    private final static String HULL_ID = "tahlan_halbmond";

    //This denotes whether we have 0: not gotten the ship yet, 1: got the ship in our fleet or 2: got and subsequently lost the ship
    private int stateOfScript = 0;

    //This is set to true once all blueprints in the ship has been discovered
    private boolean isFinished = false;

    //Timer, determines how often the script is run. We check on average every four seconds
    private IntervalUtil timer = new IntervalUtil(3.8f, 4.20f);

    //The tech aptitude we currently have. This is so we can track when we level it up
    private int currentTechAptitude = 0;

    //The multipliers on how fast the blueprints are recovered based on aptitude (0.1 means taking ten times as long as the listed value)
    private static final Map<Integer, Float> TECH_UNLOCK_MULT = new HashMap<>();
    static {
        TECH_UNLOCK_MULT.put(0, 1f);
        TECH_UNLOCK_MULT.put(1, 2f);
        TECH_UNLOCK_MULT.put(2, 3f);
        TECH_UNLOCK_MULT.put(3, 4f);
    }

    //The strings to describe how long it will take to recover stuff for each aptitude level
    private static final Map<Integer, String> LENGTH_DESCRIPTIONS = new HashMap<>();
    static {
        LENGTH_DESCRIPTIONS.put(0, "will take a very long time");
        LENGTH_DESCRIPTIONS.put(1, "will take quite some time");
        LENGTH_DESCRIPTIONS.put(2, "will take some time");
        LENGTH_DESCRIPTIONS.put(3, "will take little time");
    }

    //All the weapons that can be unlocked, and how much time it takes to unlock them (in campaign-seconds; 5 seconds or so is a day)
    //Note that the *actual* time until unlock is affected by your Technology aptitude (see TECH_UNLOCK_MULT above)
    //The string represents the ID for the blueprint package to add
    private static final Map<Float, String> BLUEPRINT_UNLOCKS = new HashMap<>();
    static {
        BLUEPRINT_UNLOCKS.put(1825f, "tahlan_jagdregalia_package");
        BLUEPRINT_UNLOCKS.put(3650f, "tahlan_silberblut_package");
        BLUEPRINT_UNLOCKS.put(5475f, "tahlan_schneefall_package");
        BLUEPRINT_UNLOCKS.put(7300f, "tahlan_halbmond_package");
    }
    
    //The name to display for each blueprint package; should ideally match up with the actual weapon in the package
    private static final Map<String, String> UNLOCK_NAMES = new HashMap<>();
    static {
        UNLOCK_NAMES.put("tahlan_jagdregalia_package", "Jagdregalia");
        UNLOCK_NAMES.put("tahlan_silberblut_package", "Silberblut Regalia");
        UNLOCK_NAMES.put("tahlan_halbmond_package", "Halbmond-class Carrier");
        UNLOCK_NAMES.put("tahlan_schneefall_package", "Rosenritter combat ships");
    }

    //The counter for tracking how far we are along with our discoveries
    private float discoveryCounter = 0f;

    //What is the highest-complexity discovery we've had yet? -UNUSED RIGHT NOW-
    private float alreadyDiscoveredMax = 0f;

    //List of our already-found weapons
    private List<String> alreadyFoundWeapons = new ArrayList<>();

    //Keeps track of our Intel
    private regaliablueprintintel intel = null;

    @Override
    public void advance(float amount) {
        //Tick our timer
        timer.advance(amount);

        //If our timer isn't done, don't run anything
        if (timer.intervalElapsed()) {
            //First, get some useful data
            boolean hasShip = playerFleetHasShip();
            int newTechAptitude = getTechAptitude();

            //Then, find our current "state"
            switch (stateOfScript) {
                case 0:
                    //Secretly update our tech aptitude
                    currentTechAptitude = newTechAptitude;

                    //If we got the ship this execution, run the script for getting the ship the first time
                    if (hasShip) {
                        getShipFirstTime();
                    }
                    break;
                case 1:
                    //If we no longer have the ship, run the appropriate code
                    if (!hasShip) {
                        loseShip();
                    }

                    //If our tech aptitude is higher now than it was earlier, alert the player of this and increase the stored tech aptitude


                    if (newTechAptitude != currentTechAptitude) {

                        if (secondInCommandMode) {
                            if (newTechAptitude < currentTechAptitude) {
                                Global.getSector().getCampaignUI().addMessage(
                                        "Your new arrangement of executive officers has a lower technical aptitude than the previous lineup, " +
                                                "decrypting the Carrier's databanks will now take more time.",
                                        Global.getSettings().getColor("standardTextColor"),
                                        "technical aptitude","take more time",
                                        Global.getSettings().getColor("mountBlueColor"),
                                        Global.getSettings().getColor("yellowTextColor"));
                            }
                            if (newTechAptitude > currentTechAptitude) {
                                Global.getSector().getCampaignUI().addMessage(
                                        "Your new arrangement of executive officers has a higher technical aptitude than before, decrypting the Carrier's databanks should now take notably less time.",
                                        Global.getSettings().getColor("standardTextColor"),
                                        "technical aptitude","notably less time",
                                        Global.getSettings().getColor("mountBlueColor"),
                                        Global.getSettings().getColor("yellowTextColor"));
                            }
                        } else {
                            Global.getSector().getCampaignUI().addMessage(
                                    "Thanks to your heightened aptitude with Technology, decrypting the Carrier's databanks should now take notably less time.",
                                    Global.getSettings().getColor("standardTextColor"),
                                    "Technology","notably less time",
                                    Global.getSettings().getColor("mountBlueColor"),
                                    Global.getSettings().getColor("yellowTextColor"));
                        }
                        currentTechAptitude = newTechAptitude;
                    }

                    //Then, tick along our "discovery" counter depending on our tech aptitude
                    discoveryCounter += ((timer.getMinInterval() + timer.getMaxInterval())/2f) * TECH_UNLOCK_MULT.get(currentTechAptitude);

                    //Then, check if we get a new blueprint!
                    for (float wepTime : BLUEPRINT_UNLOCKS.keySet()) {
                        //If we have already found the weapon, we ignore it
                        if (alreadyFoundWeapons.contains(BLUEPRINT_UNLOCKS.get(wepTime))) {
                            continue;
                        }

                        //If we *haven't* already gotten this thing, we check if our progress is high enough to get it. If so, we run the scripts for weapon-getting
                        //Note that we run it slightly different if this is the last weapon we can discover
                        if (discoveryCounter > wepTime) {
                            addBlueprint (BLUEPRINT_UNLOCKS.get(wepTime), (alreadyFoundWeapons.size() >= (BLUEPRINT_UNLOCKS.keySet().size()-1)));
                            alreadyFoundWeapons.add(BLUEPRINT_UNLOCKS.get(wepTime));
                        }
                    }

                    //Lastly, update our Intel with eventual status changes
                    intel.advanceQuestStage(alreadyFoundWeapons.size());

                    //Even *more* lastly, we can end the script completely if we have unlocked all the weapons
                    if (alreadyFoundWeapons.size() >= BLUEPRINT_UNLOCKS.keySet().size()) {
                        isFinished = true;
                    }

                    break;
                case 2:
                    //Secretly update our tech aptitude
                    currentTechAptitude = newTechAptitude;

                    //If we have regained the ship, run the script for getting the ship back
                    if (hasShip) {
                        getShipAgain();
                    }
                    break;
                default:
                    //It should never reach this point; throw a crash errorso we find out why it got here
                    throw new RuntimeException("UNREACHABLE STATE EXCEPTION: An EveryFrameScript has reached a state it should never be able to reach." +
                            " Contact Nicke535 with a modlist and what you were doing at the time of the crash. Error code: TSW-001");
            }
        }
    }


    //Function for checking if the ship is in the player's fleet
    private boolean playerFleetHasShip () {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return false;
        }

        //Go through each fleetMemberAPI and check their hull IDs
        //If we find the correct one, we run our "found the ship" code and finish for this frame
        for (FleetMemberAPI member : playerFleet.getMembersWithFightersCopy()) {
            //Ignore fighters
            if (member.getHullSpec().getHullSize() == ShipAPI.HullSize.FIGHTER) {
                continue;
            }

            //Then, check the ID
            if (member.getHullId().contains(HULL_ID)) {
                return true;
            }
        }
        return false;
    }


    //Function for getting the player's aptitude in technology (as 0, 1, 2 or 3)
    private int getTechAptitude () {

        if (secondInCommandMode) {
            int level = 0;
            SCData data = SCUtils.getPlayerData();

            for (SCOfficer officer : data.getActiveOfficers()) {
                if (VALID_APTITUDES.containsKey(officer.getAptitudeId())) {
                    level += VALID_APTITUDES.get(officer.getAptitudeId());
                }
            }

            level = MathUtils.clamp(level, 0, 3);

            return level;
        }

        if (Global.getSector().getPlayerStats().getSkillLevel("special_modifications")>0 || Global.getSector().getPlayerStats().getSkillLevel("automated_ships")>0) {
            return 3;
        } else if (Global.getSector().getPlayerStats().getSkillLevel("electronic_warfare")>0 || Global.getSector().getPlayerStats().getSkillLevel("fighter_uplink")>0) {
            return 2;
        } else if (Global.getSector().getPlayerStats().getSkillLevel("navigation")>0 || Global.getSector().getPlayerStats().getSkillLevel("sensors")>0) {
            return 1;
        } else {
            return 0;
        }
    }


    //Function for doing anything that needs to be done after getting the ship the first time
    private void getShipFirstTime () {
        //Adds our intel
        regaliablueprintintel intel = new regaliablueprintintel(this);
        if (!intel.isDone()) {
            this.intel = intel;
            Global.getSector().getIntelManager().addIntel(intel, true);
        }

        //Spawns a message to tell the player all they need to know about the new acquiring
        if (secondInCommandMode) {
            Global.getSector().getCampaignUI().addMessage(
                    "Initial examinations of the Halbmond-class carrier you recently recovered have revealed a well-encrypted but intact Rosenritter blueprint database stored on the ship's mainframe. " +
                            "Recovery is likely to be a complicated process, but the rewards should prove worth the wait. An executive officer with technical proficiency may improve recovery, though officers with adjacent aptitudes will also be of help. With your decks current aptitude, decrypting it " +
                            LENGTH_DESCRIPTIONS.get(currentTechAptitude) + ".",
                    Global.getSettings().getColor("standardTextColor"),
                     "executive officer with technical proficiency",LENGTH_DESCRIPTIONS.get(currentTechAptitude),
                    Global.getSettings().getColor("mountBlueColor"),
                    Global.getSettings().getColor("yellowTextColor"));
        } else {
            Global.getSector().getCampaignUI().addMessage(
                    "Initial examinations of the Halbmond-class carrier you recently recovered have revealed a well-encrypted but intact Rosenritter blueprint database stored on the ship's mainframe. Recovery is likely to be a complicated process, but the rewards should prove worth the wait. With your current aptitude in Technology, decrypting it " +
                            LENGTH_DESCRIPTIONS.get(currentTechAptitude) + ".",
                    Global.getSettings().getColor("standardTextColor"),
                    "Technology",LENGTH_DESCRIPTIONS.get(currentTechAptitude),
                    Global.getSettings().getColor("mountBlueColor"),
                    Global.getSettings().getColor("yellowTextColor"));
        }


        stateOfScript = 1;
    }


    //Function for doing anything that needs to be done after getting the ship subsequent times
    private void getShipAgain () {
        Global.getSector().getCampaignUI().addMessage(
                "Having re-accquired the Halbmond, your teams resume their decryption efforts, though at your current aptitude in Technology cracking the encryption " +
                        LENGTH_DESCRIPTIONS.get(currentTechAptitude) + ".",
                Global.getSettings().getColor("standardTextColor"),
                "Technology",LENGTH_DESCRIPTIONS.get(currentTechAptitude),
                Global.getSettings().getColor("mountBlueColor"),
                Global.getSettings().getColor("yellowTextColor"));
        stateOfScript = 1;
    }


    //Function for applying all effects that happen when you lose the ship
    private void loseShip () {
        Global.getSector().getCampaignUI().addMessage(
        "As the Halbmond is no longer in your fleet, all data decryption efforts have halted.",
                Global.getSettings().getColor("standardTextColor"),
                "Halbmond","halted",
                Global.getSettings().getColor("textFriendColor"),
                Global.getSettings().getColor("flatRedTextColor"));
        stateOfScript = 2;
    }


    //Function for adding the recovered blueprint to the player's inventory, and alerting them that this has happened
    private void addBlueprint (String blueprintID, boolean wasFinalBlueprint) {
        //First, actually add the blueprint to the player's fleet
        Global.getSector().getPlayerFleet().getCargo().addSpecial(new SpecialItemData(blueprintID, null), 1f);

        //Then, tell the player this has happened, with the "actual" name of the weapon. Have a separate message if this was the last weapon uncovered
        String nameToPrint = UNLOCK_NAMES.get(blueprintID);
        if (wasFinalBlueprint) {
            Global.getSector().getCampaignUI().addMessage(
                    "Your decryption efforts have allowed you to retrieve the final segment of the Halbmond's database, a single blueprint for the " + nameToPrint + " itself.",
                    Global.getSettings().getColor("standardTextColor"),
                    "final",nameToPrint,
                    Global.getSettings().getColor("flatRedTextColor"),
                    Global.getSettings().getColor("yellowTextColor"));
        } else {
            Global.getSector().getCampaignUI().addMessage(
                    "Your decryption efforts have progressed well and uncovered a part of the database containing the blueprints for " + nameToPrint + ". Analysis indicates there is still more data to be recovered, however...",
                    Global.getSettings().getColor("standardTextColor"),
                    nameToPrint,"still more blueprints to be recovered",
                    Global.getSettings().getColor("yellowTextColor"),
                    Global.getSettings().getColor("yellowTextColor"));
        }
    }


    //No need to run the script when paused
    @Override
    public boolean runWhilePaused() {
        return false;
    }

    //We have no use for the script once we've discovered all the blueprints
    @Override
    public boolean isDone() {
        return isFinished;
    }
}
