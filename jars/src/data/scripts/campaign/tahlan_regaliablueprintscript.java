package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class tahlan_regaliablueprintscript implements EveryFrameScript {
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
        LENGTH_DESCRIPTIONS.put(3, "will take a little time");
    }

    //All the weapons that can be unlocked, and how much time it takes to unlock them (in campaign-seconds; 5 seconds or so is a day)
    //Note that the *actual* time until unlock is affected by your Technology aptitude (see TECH_UNLOCK_MULT above)
    //The string represents the ID for the blueprint package to add
    private static final Map<Float, String> BLUEPRINT_UNLOCKS = new HashMap<>();
    static {
        BLUEPRINT_UNLOCKS.put(1825f, "tahlan_jagdregalia_package");
        BLUEPRINT_UNLOCKS.put(3650f, "tahlan_silberblut_package");
        BLUEPRINT_UNLOCKS.put(5475f, "tahlan_halbmond_package");
        BLUEPRINT_UNLOCKS.put(7300f, "tahlan_schneefall_package");
    }
    
    //The name to display for each blueprint package; should ideally match up with the actual weapon in the package
    private static final Map<String, String> UNLOCK_NAMES = new HashMap<>();
    static {
        UNLOCK_NAMES.put("tahlan_jagdregalia_package", "Jagdregalia");
        UNLOCK_NAMES.put("tahlan_silberblut_package", "Silberblut Regalia");
        UNLOCK_NAMES.put("tahlan_halbmond_package", "Halbmond-class Carrier");
        UNLOCK_NAMES.put("tahlan_schneefall_package", "Schneefall-class Battlecruiser");
    }

    //The counter for tracking how far we are along with our discoveries
    private float discoveryCounter = 0f;

    //What is the highest-complexity discovery we've had yet? -UNUSED RIGHT NOW-
    private float alreadyDiscoveredMax = 0f;

    //List of our already-found weapons
    private List<String> alreadyFoundWeapons = new ArrayList<>();

    //Keeps track of our Intel
    private tahlan_regaliablueprintintel intel = null;

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
                        Global.getSector().getCampaignUI().addMessage(
                                "Thanks to your heightened aptitude with Technology, decrypting the Carrier's databanks should now take notably less time.",
                                Global.getSettings().getColor("standardTextColor"),
                                "Technology","notably less time",
                                Global.getSettings().getColor("mountBlueColor"),
                                Global.getSettings().getColor("yellowTextColor"));
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
        float level = Global.getSector().getPlayerStats().getAptitudeLevel(Skills.APT_TECHNOLOGY);
        if (level >= 3f) {
            return 3;
        } else if (level >= 2f) {
            return 2;
        } else if (level >= 1f) {
            return 1;
        } else {
            return 0;
        }
    }


    //Function for doing anything that needs to be done after getting the ship the first time
    private void getShipFirstTime () {
        //Adds our intel
        tahlan_regaliablueprintintel intel = new tahlan_regaliablueprintintel(this);
        if (!intel.isDone()) {
            this.intel = intel;
            Global.getSector().getIntelManager().addIntel(intel, true);
        }

        //Spawns a message to tell the player all they need to know about the new acquiring
        Global.getSector().getCampaignUI().addMessage(
                "The databanks of the recently recovered Halbmond class carrier are completely foreign to you and seem to be in bad shape, a preliminary analysis suggests you might still be able to piece together some useful data from what is left." +
                        "The software running the Halbmond's computer cores is every bit as enigmatic as its hardware. With your current aptitude in Technology, decrypting it " +
                        LENGTH_DESCRIPTIONS.get(currentTechAptitude) + ".",
                Global.getSettings().getColor("standardTextColor"),
                "Technology",LENGTH_DESCRIPTIONS.get(currentTechAptitude),
                Global.getSettings().getColor("mountBlueColor"),
                Global.getSettings().getColor("yellowTextColor"));
        stateOfScript = 1;
    }


    //Function for doing anything that needs to be done after getting the ship subsequent times
    private void getShipAgain () {
        Global.getSector().getCampaignUI().addMessage(
                "Having re-accquired the Halbmond, you begin anew with working through its databanks, though at your current aptitude in Technology cracking the encryption " +
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
                    "You have succesfully recovered one last blueprint from the Halbmond's databanks and spliced it onto a hacked production chip. The data identifies it as the " + nameToPrint + ".",
                    Global.getSettings().getColor("standardTextColor"),
                    "one last",nameToPrint,
                    Global.getSettings().getColor("flatRedTextColor"),
                    Global.getSettings().getColor("yellowTextColor"));
        } else {
            Global.getSector().getCampaignUI().addMessage(
                    "You have succesfully recovered what seems to be a blueprint from the Halbmond's databanks and downloaded it onto a hacked production chip. The data in question identifies the item as a " + nameToPrint + ". Analysis indicates there is still more data to be recovered, however...",
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
