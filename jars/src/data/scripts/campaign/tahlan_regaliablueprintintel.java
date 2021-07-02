package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class tahlan_regaliablueprintintel extends BaseIntelPlugin {

    //List of all the event stages we can be at
    public static enum QUEST_STAGE {
        FIRST_BLUEPRINT,
        SECOND_BLUEPRINT,
        THIRD_BLUEPRINT,
        FOURTH_BLUEPRINT,
        END_OF_BLUEPRINTS,
        ;
    }

    //Whether the Intel is done or not. By default, it's linked to "endAfterDelay()" in some way
    @Override
    public boolean isDone() {
        return super.isDone();
    }

    //Keep the script that spawned us in memory; we might need it later
    private tahlan_regaliablueprintscript script;

    //The stage of the event we are currently at
    protected QUEST_STAGE stage;

    //Initializer function
    public tahlan_regaliablueprintintel(tahlan_regaliablueprintscript script) {
        this.script = script;

        stage = QUEST_STAGE.FIRST_BLUEPRINT;
    }

    //The function for when we "actually" end. Clears up memory, removes us from the sector, and clears some unnecessary variables
    @Override
    protected void notifyEnded() {
        super.notifyEnded();
        script = null;
        Global.getSector().removeScript(this);
    }

    //My own function, for advancing the intel when new blueprints are found
    public void advanceQuestStage (int newestFoundBlueprint) {
        if (newestFoundBlueprint == 4) {
            stage = QUEST_STAGE.END_OF_BLUEPRINTS;
            endAfterDelay();
        } else if (newestFoundBlueprint == 3) {
            stage = QUEST_STAGE.FOURTH_BLUEPRINT;
        } else if (newestFoundBlueprint == 2) {
            stage = QUEST_STAGE.THIRD_BLUEPRINT;
        } else if (newestFoundBlueprint == 1) {
            stage = QUEST_STAGE.SECOND_BLUEPRINT;
        } else {
            stage = QUEST_STAGE.FIRST_BLUEPRINT;
        }
    }


    //Not *entirely* sure what this does. Probably related to rules.csv
    @Override
    public boolean callEvent(String ruleId, InteractionDialogAPI dialog,
                             List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        return true;
    }

    //Running this function ends the event, after a delay (around a couple of days, as far as I can tell)
    @Override
    public void endAfterDelay() {
        stage = QUEST_STAGE.END_OF_BLUEPRINTS;
        super.endAfterDelay();
    }

    //Related to telling quests that the quest has finished. Used to re-generate quests that are not one-offs
    @Override
    protected void notifyEnding() {
        super.notifyEnding();
    }


    //The function for adding all bullet-points in the Intel tooltip.
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {

        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        float pad = 3f;
        float opad = 10f;

        float initPad = pad;
        if (mode == ListInfoMode.IN_DESC) initPad = opad;

        Color tc = getBulletColorForMode(mode);

        bullet(info);
        boolean isUpdate = getListInfoParam() != null;

        //Use a slightly different bullet list if we're finished with the blueprints
        if (stage != QUEST_STAGE.END_OF_BLUEPRINTS) {
            info.addPara("Keep the %s in your fleet to continue decrypting its databanks", initPad, h, "Halbmond");
        } else {
            info.addPara("You've succesfully recovered all intact data from the %s databanks. Furthermore you have found information that an object of interest might be hidden in the " + Global.getSector().getMemoryWithoutUpdate().getString("$tahlan_traum_location") + " constellation", initPad, h, "Halbmond's", Global.getSector().getMemory().getString("$tahlan_traum_location"));
        }

        unindent(info);
    }

    //The function for writing the detailed info in the Intel screen.
    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = getTitleColor(mode);
        info.setParaSmallInsignia();
        info.addPara(getName(), c, 0f);
        info.setParaFontDefault();
        addBulletPoints(info, mode);
    }

    //The small description for the intel screen.
    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color tc = Misc.getTextColor();
        float pad = 3f;
        float opad = 10f;

        //We use slightly different texts depending
        if (stage == QUEST_STAGE.END_OF_BLUEPRINTS) {
            info.addPara("You succesfully recovered all intact data from the old ship's databanks.", opad);
        } else if (stage == QUEST_STAGE.FOURTH_BLUEPRINT) {
            info.addPara("You're almost done piecing together what is left of the old vessel's datacores; a little bit more and its secrets will be yours.", opad);
        } else {
            info.addPara("You've found an unusual carrier drifting in a debris field, its secrets appear well encrypted. Recovering them will take some time.", opad);
        }

        addBulletPoints(info, ListInfoMode.IN_DESC);

    }

    //Sets which icon the Intel screen should display. Can vary based on circumstances, but a single one often works just fine
    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("tahlan_intel", "tahlan_halbmond");
    }

    //This sets which "tags" the even has in the Intel screen. For example, giving it the Tags.INTEL_STORY tag makes it appear in the "Story" sub-category
    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_STORY);
        return tags;
    }

    //Sorting-related; see it as a form of "how important is the even" thingy. Lower number = more important
    @Override
    public IntelSortTier getSortTier() {
        return IntelSortTier.TIER_2;
    }

    //What string to sort with, when sorting alphabetically
    public String getSortString() {
        return "Abandoned Carrier";
    }

    //The name of the event; can vary based on circumstances. I decided to just make it say "completed" when completed
    public String getName() {
        if (isEnded() || isEnding()) {
            return "The Abandoned Carrier - Completed";
        }
        return "The Abandoned Carrier";
    }

    //Here, you can set which faction's UI colors to use. The default is to use the player's faction.
    @Override
    public FactionAPI getFactionForUIColors() {
        return super.getFactionForUIColors();
    }

    //This just seems to call back to the name again
    public String getSmallDescriptionTitle() {
        return getName();
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return Global.getSector().getPlayerFleet();
    }

    //Whether the intel should be removed from the intel screen. By default only called once the event is over
    @Override
    public boolean shouldRemoveIntel() {
        return super.shouldRemoveIntel();
    }

    //Which sound the Comms should make from getting the intel. Some default values include:
    //  getSoundMajorPosting();
    //  getSoundStandardUpdate();
    //  getSoundLogUpdate();
    //  getSoundColonyThreat();
    //  getSoundStandardPosting();
    //  getSoundStandardUpdate();
    //Other values can be inputted, from sounds.json
    @Override
    public String getCommMessageSound() {
        return getSoundMajorPosting();
    }

}



