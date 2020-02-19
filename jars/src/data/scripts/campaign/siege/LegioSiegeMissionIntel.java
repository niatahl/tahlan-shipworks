package data.scripts.campaign.siege;

import java.awt.Color;
import java.util.Random;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin.ReputationAdjustmentResult;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidDelegate;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import static data.scripts.campaign.siege.LegioSiegeManager.LEGIO_ID;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

public class LegioSiegeMissionIntel extends RaidIntel implements RaidDelegate {

    public static Logger log = Global.getLogger(LegioSiegeMissionIntel.class);

    public static enum LegioRaidSetupOutcome {
        EXPEDITION_DESTROYED,
        BASE_ESTABLISHED, // yeah boiiiiiiiiii
    }

    public static final Object REACHED_SYSTEM_UPDATE = new Object();
    public static final Object OUTCOME_UPDATE = new Object();

    protected LegioSiegeMissionStage4Construct action;

    protected MarketAPI from;

    protected boolean reachedTarget = false;
    protected LegioRaidSetupOutcome outcome;

    protected Random random = new Random();

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public LegioSiegeMissionIntel(FactionAPI faction, MarketAPI from, StarSystemAPI target, float fleetPoints) {
        super(target, faction, null);
        this.delegate = this;
        this.from = from;

        float orgDur = 15f + 15f * (float) Math.random();
        if (Global.getSettings().isDevMode()) {
            orgDur = 1f;
        }
        addStage(new LegioSiegeMissionStage1Organize(this, from, orgDur));

        SectorEntityToken gather = from.getPrimaryEntity();

        float successMult = 0.5f;

        LegioSiegeMissionStage2Assemble assemble = new LegioSiegeMissionStage2Assemble(this, gather);
        assemble.addSource(from);
        assemble.setSpawnFP(fleetPoints);
        assemble.setAbortFP(fleetPoints * successMult);
        addStage(assemble);

        SectorEntityToken travelTo = target.getHyperspaceAnchor();

        LegioSiegeMissionStage3Travel travel = new LegioSiegeMissionStage3Travel(this, gather, travelTo, true);
        travel.setAbortFP(fleetPoints * successMult);
        addStage(travel);

        action = new LegioSiegeMissionStage4Construct(this, target);
        action.setAbortFP(fleetPoints * successMult);
        addStage(action);

        addStage(new LegioSiegeMissionStage5Defend(this, target));

        Global.getSector().getIntelManager().addIntel(this);
    }

    @Override
    public String getCommMessageSound() {
        if (isSendingUpdate()) {
            return getSoundStandardUpdate();
        }
        return "ui_shutdown_industry";
    }

    @Override
    public FactionAPI getFaction() {
        return faction;
    }

    @Override
    public CampaignFleetAPI spawnFleet(RouteData route) {

        Random thisRandom = route.getRandom();

        MarketAPI market = route.getMarket();
        CampaignFleetAPI fleet = createFleet(LEGIO_ID, route, market, null, thisRandom);

        if (fleet == null || fleet.isEmpty()) {
            return null;
        }

        market.getContainingLocation().addEntity(fleet);
        fleet.setFacing((float) Math.random() * 360f);
        // this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
        fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);

        fleet.addScript(createAssignmentAI(fleet, route));

        return fleet;
    }

    @Override
    public CampaignFleetAPI createFleet(String factionId, RouteData route, MarketAPI market, Vector2f locInHyper, Random random) {
        if (random == null) {
            random = new Random();
        }

        RouteManager.OptionalFleetData extra = route.getExtra();

        float combat = extra.fp;
        float tanker = extra.fp * (0.1f + random.nextFloat() * 0.05f);
        float transport = extra.fp * (0.1f + random.nextFloat() * 0.05f);
        float freighter = extra.fp * (0.1f + random.nextFloat() * 0.05f);
        float liner = extra.fp * (0.1f + random.nextFloat() * 0.05f);
        float utility = extra.fp * (random.nextFloat() * 0.05f);

        FleetParamsV3 params = new FleetParamsV3(
                market,
                locInHyper,
                factionId,
                route.getQualityOverride(),
                extra.fleetType,
                combat, // combatPts
                freighter, // freighterPts 
                tanker, // tankerPts
                transport, // transportPts
                liner, // linerPts
                utility, // utilityPts
                0f // qualityMod, won't get used since routes mostly have quality override set
        );
        params.timestamp = route.getTimestamp();
        params.random = random;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        if (fleet == null || fleet.isEmpty()) {
            return null;
        }

        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_WAR_FLEET, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_RAIDER, true);

        if (fleet.getFaction().getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)) {
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PIRATE, true);
        }

        String postId = Ranks.POST_PATROL_COMMANDER;
        String rankId = Ranks.SPACE_COMMANDER;

        fleet.getCommander().setPostId(postId);
        fleet.getCommander().setRankId(rankId);

        return fleet;
    }

    public Random getRandom() {
        return random;
    }

    public StarSystemAPI getTarget() {
        return system;
    }

    public MarketAPI getFrom() {
        return from;
    }

    @Override
    public RouteFleetAssignmentAI createAssignmentAI(CampaignFleetAPI fleet, RouteData route) {
        LegioSiegeMissionAssignmentAI expeditionAI = new LegioSiegeMissionAssignmentAI(fleet, route, action);
        return expeditionAI;
    }

    public boolean reachedTarget() {
        return reachedTarget;
    }

    public void setReachedTarget(boolean reachedTarget) {
        this.reachedTarget = reachedTarget;
    }

    public LegioRaidSetupOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(LegioRaidSetupOutcome outcome) {
        this.outcome = outcome;
    }

    @Override
    protected void advanceImpl(float amount) {
        super.advanceImpl(amount);
    }

    protected transient ReputationAdjustmentResult repResult = null;

    public void makeHostile() {
        boolean hostile = getFaction().isHostileTo(Factions.PLAYER);
        if (!hostile) {
            repResult = Global.getSector().adjustPlayerReputation(new RepActionEnvelope(RepActions.MAKE_HOSTILE_AT_BEST,
                    null, null, null, false, true),
                    LEGIO_ID);
        }
    }

    public void sendInSystemUpdate() {
        sendUpdateIfPlayerHasIntel(REACHED_SYSTEM_UPDATE, false, true);
    }

    public void applyRepPenalty(float delta) {
        CustomRepImpact impact = new CustomRepImpact();
        impact.delta = delta;
        repResult = Global.getSector().adjustPlayerReputation(
                new RepActionEnvelope(RepActions.CUSTOM,
                        impact, null, null, false, false),
                getFaction().getId());
    }

    public void sendOutcomeUpdate() {
        outcome = LegioRaidSetupOutcome.BASE_ESTABLISHED;
        sendUpdateIfPlayerHasIntel(OUTCOME_UPDATE, false, true);
        log.info(String.format("Sending outcome update %s for raid setup at %s", outcome.name(), system.getNameWithLowercaseType()));
    }

    @Override
    public String getName() {
        String base = Misc.ucFirst(faction.getEntityNamePrefix()) + " Siege";
        if (outcome == LegioRaidSetupOutcome.EXPEDITION_DESTROYED) {
            return base + " - Failed";

        } else if (outcome == LegioRaidSetupOutcome.BASE_ESTABLISHED) {
            return base + " - Completed";
        }
        return base;
    }

    @Override
    @SuppressWarnings("UnusedAssignment")
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {

        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        float pad = 3f;
        float opad = 10f;

        float initPad = pad;
        if (mode == ListInfoMode.IN_DESC) {
            initPad = opad;
        }

        Color tc = getBulletColorForMode(mode);

        bullet(info);
        boolean isUpdate = getListInfoParam() != null;

        if (getListInfoParam() == REACHED_SYSTEM_UPDATE) {
            info.addPara("Target: %s", initPad, tc,
                    g, system.getName());
            initPad = 0f;
            info.addPara("Reached system", tc, initPad);
            log.info(String.format("Siege fleet has arrived at %s", system.getNameWithLowercaseType()));
            return;
        }

        if (getListInfoParam() == OUTCOME_UPDATE) {
            if (outcome == LegioRaidSetupOutcome.BASE_ESTABLISHED) {
            }
            initPad = 0f;
            info.addPara("Target: %s", initPad, tc,
                    g, system.getName());
            initPad = 0f;
            info.addPara("Siege base established", tc, initPad);
            return;
        }

        float eta = getETA();

        info.addPara("Target: %s", initPad, tc,
                g, system.getName());
        initPad = 0f;

        if (eta > 1 && outcome == null) {
            String days = getDaysString(eta);
            info.addPara("Estimated %s " + days + " until arrival",
                    initPad, tc, h, "" + Math.round(eta));
            initPad = 0f;

        } else if (outcome == null && action.getElapsed() > 0) {
            info.addPara("Construction of siege base underway", tc, initPad);
            initPad = 0f;
        }

        unindent(info);
    }

    @Override
    public LegioSiegeMissionStage4Construct getActionStage() {
        for (RaidStage stage : stages) {
            if (stage instanceof LegioSiegeMissionStage4Construct) {
                return (LegioSiegeMissionStage4Construct) stage;
            }
        }
        return null;
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        super.createIntelInfo(info, mode);
    }

    public static String aOrAn(String input) {

        ArrayList<String> vowels = new ArrayList<>(Arrays.asList(
                "a",
                "e",
                "i",
                "o",
                "u"));

        String firstLetter = input.substring(0, 1).toLowerCase();

        if (vowels.contains(firstLetter)) {
            return "an";
        } else {
            return "a";
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {

        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color tc = Misc.getTextColor();
        float pad = 3f;
        float opad = 10f;

        info.addImage(getFactionForUIColors().getLogo(), width, 128, opad);

        String has = faction.getDisplayNameHasOrHave();
        String is = faction.getDisplayNameIsOrAre();

        String strDesc = getRaidStrDesc();

        LabelAPI label = info.addPara(Misc.ucFirst(faction.getDisplayNameWithArticle()) + " " + is
                + " sending a fleet to " + system.getName() + ". The siege fleet is projected to be " + strDesc + ".",
                opad, faction.getBaseUIColor());
        label.setHighlight(faction.getDisplayNameWithArticleWithoutArticle(), system.getName(), strDesc);
        label.setHighlightColors(faction.getBaseUIColor(), h, h);

        info.addSectionHeading("Status",
                faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, opad);

        for (RaidStage stage : stages) {
            stage.showStageInfo(info);
            if (getStageIndex(stage) == failStage) {
                break;
            }
        }

    }

    @Override
    public void sendUpdateIfPlayerHasIntel(Object listInfoParam, boolean onlyIfImportant, boolean sendIfHidden) {

        if (listInfoParam == UPDATE_RETURNING) {
            // we're using sendOutcomeUpdate() to send an end-of-event update instead
            return;
        }

        super.sendUpdateIfPlayerHasIntel(listInfoParam, onlyIfImportant, sendIfHidden);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {

        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_FLEET_DEPARTURES);
        tags.add(getFaction().getId());
        return tags;
    }

    @Override
    public void notifyRaidEnded(RaidIntel raid, RaidStageStatus status) {
        if (outcome == null && failStage >= 0) {
            outcome = LegioRaidSetupOutcome.EXPEDITION_DESTROYED;
        }
    }

    @Override
    public String getIcon() {
        return faction.getCrest();
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (system != null) {
            return system.getHyperspaceAnchor();
        }
        return super.getMapLocation(map);
    }
}
