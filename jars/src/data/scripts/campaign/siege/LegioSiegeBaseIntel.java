package data.scripts.campaign.siege;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin.ReputationAdjustmentResult;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase;
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel.BountyResult;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel.BountyResultType;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.intel.raid.PirateRaidActionStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.PirateRaidAssembleStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.impl.campaign.intel.raid.ReturnStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.TravelStage;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidDelegate;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidStageStatus;
import com.fs.starfarer.api.impl.campaign.procgen.MarkovNames;
import com.fs.starfarer.api.impl.campaign.procgen.MarkovNames.MarkovNameResult;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import static data.scripts.campaign.siege.LegioSiegeManager.LEGIO_ID;

public class LegioSiegeBaseIntel extends BaseIntelPlugin implements EveryFrameScript, FleetEventListener, EconomyUpdateListener, RaidDelegate {

    public static final float BASE_MARKET_SIZE = 3f;
    public static final float MARKET_SIZE_PER_CYCLE = 0.2f;
    public static final float MAX_MARKET_SIZE = 5f;

    public static final float BASE_RAID_FP = 100f;
    public static final float RAID_FP_PER_CYCLE = 50f;
    public static final float MAX_RAID_FP = 500f;

    public static final float BASE_STATION_LEVEL = 1f; // 1 = orbital
    public static final float STATION_LEVEL_PER_CYCLE = 0.25f; // 2 = battlestation
    public static final float MAX_STATION_LEVEL = 3f; // 3 = star fortress

    public static final float BASE_DURATION = 0f; // if 0 or lower, base will not despawn "naturally" - it must be killed

    public static final float BASE_BOUNTY_CREDITS = 200000f; // will be multiplied by level, 1 to 3
    public static final float BOUNTY_DAYS = 0f; // if 0 or lower, bounty will not expire *unless the base itself does* (and won't show duration)

    // for patrols
    public static final float BASE_PATROL_SHIPQUAL_BONUS = 0.5f;
    public static final float PATROL_SHIPQUAL_PER_CYCLE = 0.1f; // hardcoded max at 1.0f

    public static final float BASE_PATROL_SIZE_MULT = 1f;
    public static final float PATROL_SIZE_PER_CYCLE = 0.1f;
    public static final float MAX_PATROL_SIZE_MULT = 2f;

    public static final float BASE_LIGHT_PATROLS = 2f;
    public static final float LIGHT_PATROLS_PER_CYCLE = 1f;
    public static final float MAX_LIGHT_PATROLS = 7f;

    public static final float BASE_MED_PATROLS = 2f;
    public static final float MED_PATROLS_PER_CYCLE = 0.334f;
    public static final float MAX_MED_PATROLS = 5f;

    public static final float BASE_HEAVY_PATROLS = 1f;
    public static final float HEAVY_PATROLS_PER_CYCLE = 0.2f;
    public static final float MAX_HEAVY_PATROLS = 3f;

    public static Object BOUNTY_EXPIRED_PARAM = new Object();
    public static Object DISCOVERED_PARAM = new Object();

    protected StarSystemAPI target = null;

    public static class BaseBountyData {

        public float bountyElapsedDays = 0f;
        public float bountyDuration = 0;
        public float baseBounty = 0;
        public float repChange = 0;
        public FactionAPI bountyFaction = null;
    }

    public static Logger log = Global.getLogger(LegioSiegeBaseIntel.class);

    protected StarSystemAPI system;
    protected MarketAPI market;
    protected SectorEntityToken entity;

    protected float elapsedDays = 0f;

    protected BaseBountyData bountyData = null;

    protected IntervalUtil monthlyInterval = new IntervalUtil(20f, 40f);
    protected int raidTimeoutMonths = 1; // initial value = initial timeout before raid after contstructed

    protected boolean sentBountyUpdate = false;
    protected int monthsWithSameTarget = 0;
    protected int monthsAtCurrentTier = 0;

    protected BountyResult result = null;

    public LegioSiegeBaseIntel(StarSystemAPI system) {
        this.system = system;

        createStation();

        Global.getSector().getIntelManager().addIntel(this, true);
        timestamp = null;

        Global.getSector().getListenerManager().addListener(this);
        Global.getSector().getEconomy().addUpdateListener(this);
    }

    private void createStation() {
        int size = pickSize();
        market = Global.getFactory().createMarket(Misc.genUID(), "Legio Infernalis Siege Base", size);
        market.setSize(size);
        market.setHidden(true);

        market.setFactionId(LEGIO_ID);
        market.setSurveyLevel(SurveyLevel.FULL);
        market.addCondition("population_" + size);

        market.addCondition(Conditions.NO_ATMOSPHERE);
        market.addCondition(Conditions.OUTPOST);
        market.addCondition(Conditions.ORGANIZED_CRIME);
        market.addCondition(Conditions.STEALTH_MINEFIELDS);
        market.addCondition("tahlan_legiotyranny");

        market.addIndustry(Industries.POPULATION);
        market.addIndustry(Industries.SPACEPORT);
        market.addIndustry(Industries.MILITARYBASE);
        market.addIndustry(Industries.HEAVYBATTERIES);

        market.addIndustry(pickStationType());

        market.addSubmarket(Submarkets.SUBMARKET_OPEN);
        market.addSubmarket(Submarkets.GENERIC_MILITARY);

        market.getTariff().modifyFlat("default_tariff", market.getFaction().getTariffFraction());

        String name = generateName();
        if (name == null) {
            endImmediately();
            return;
        }

        LocationAPI hyperspace = Global.getSector().getHyperspace();
        entity = hyperspace.addCustomEntity(null, name, Entities.MAKESHIFT_STATION, LEGIO_ID);

        if (entity == null) {
            endImmediately();
            return;
        }

        market.setName(name);
        entity.setName(name);

        market.setPrimaryEntity(entity);
        entity.setMarket(market);

        entity.setSensorProfile(1f);
        entity.setDiscoverable(true);
        entity.getDetectedRangeMod().modifyFlat("gen", 5000f);

        market.setEconGroup(market.getId());
        market.getMemoryWithoutUpdate().set(DecivTracker.NO_DECIV_KEY, true);

        market.reapplyIndustries();

        Global.getSector().getEconomy().addMarket(market, true);
        entity.setCircularOrbitPointingDown(system.getHyperspaceAnchor(), (float) (Math.random() * 360f), 600f, 180f);

        createAdmin(market);

        // market.advance(666f); // maybe this will spawn patrols?? maybe??
        // no it won't, not really
        // fuck
        MilitaryBase base = (MilitaryBase) market.getIndustry(Industries.MILITARYBASE);
        // try to force spawn patrols here?

        // force a check for bounty/raid
        monthlyInterval.forceIntervalElapsed();
        advanceImpl(0f);

        log.info(String.format("Added legio siege base at [%s]", system.getName()));
    }

    // this is where you would put the custom icon, if you had one
    @Override
    public String getIcon() {
        return market.getFaction().getCrest();
    }

    private float getLevel() {
        float cycle = Global.getSector().getClock().getCycle() - 206;
        float base = BASE_STATION_LEVEL;
        float perCycle = cycle * STATION_LEVEL_PER_CYCLE;
        float max = MAX_STATION_LEVEL;
        return Math.min(base + perCycle, max);
    }

    private int pickSize() {
        float cycle = Global.getSector().getClock().getCycle() - 206;
        float base = BASE_MARKET_SIZE;
        float perCycle = cycle * MARKET_SIZE_PER_CYCLE;
        float max = MAX_MARKET_SIZE;
        return (int) Math.min(base + perCycle, max);
    }

    @Override
    public boolean isHidden() {
        if (super.isHidden()) {
            return true;
        }
        return timestamp == null;
    }

    public float getRaidFP() {
        float base = getBaseRaidFP();
        return base * (0.75f + (float) Math.random() * 0.5f);
    }

    public float getBaseRaidFP() {
        float cycle = Global.getSector().getClock().getCycle() - 206;
        float base = BASE_RAID_FP;
        float perCycle = cycle * RAID_FP_PER_CYCLE;
        float max = MAX_RAID_FP;
        float fp = Math.min(base + perCycle, max);
        return fp;
    }

    @Override
    public void notifyRaidEnded(RaidIntel raid, RaidStageStatus status) {
        if (status == RaidStageStatus.SUCCESS) {
            raidTimeoutMonths = 0;
        } else {
            float base = getBaseRaidFP();
            float raidFP = raid.getAssembleStage().getOrigSpawnFP();
            raidTimeoutMonths += Math.round(raidFP / base) * 2;
        }
    }

    public void startRaid(StarSystemAPI target, float raidFP) {
        boolean hasTargets = false;
        for (MarketAPI curr : Misc.getMarketsInLocation(target)) {
            if (curr.getFaction().isHostileTo(getFactionForUIColors())) {
                hasTargets = true;
                break;
            }
        }

        if (!hasTargets) {
            return;
        }

        RaidIntel raid = new RaidIntel(target, getFactionForUIColors(), this);

        float successMult = 0.5f;

        JumpPointAPI gather = null;
        List<JumpPointAPI> points = system.getEntities(JumpPointAPI.class);
        float min = Float.MAX_VALUE;
        for (JumpPointAPI curr : points) {
            float dist = Misc.getDistance(entity.getLocation(), curr.getLocation());
            if (dist < min) {
                min = dist;
                gather = curr;
            }
        }

        PirateRaidAssembleStage assemble = new PirateRaidAssembleStage(raid, gather, this);
        assemble.addSource(market);
        assemble.setSpawnFP(raidFP);
        assemble.setAbortFP(raidFP * successMult);
        raid.addStage(assemble);

        SectorEntityToken raidJump = RouteLocationCalculator.findJumpPointToUse(getFactionForUIColors(), target.getCenter());

        TravelStage travel = new TravelStage(raid, gather, raidJump, false);
        travel.setAbortFP(raidFP * successMult);
        raid.addStage(travel);

        PirateRaidActionStage action = new PirateRaidActionStage(raid, target);
        action.setAbortFP(raidFP * successMult);
        raid.addStage(action);

        raid.addStage(new ReturnStage(raid));

        boolean shouldNotify = raid.shouldSendUpdate();
        Global.getSector().getIntelManager().addIntel(raid, !shouldNotify);
    }

    public StarSystemAPI getSystem() {
        return system;
    }

    // here's where you would add custom legio station industry IDs, if you had one
    protected String pickStationType() {
        WeightedRandomPicker<String> stations = new WeightedRandomPicker<>();

        int level = (int) getLevel();

        if (level >= 3f) {
            stations.add(Industries.STARFORTRESS, 5f);
            stations.add(Industries.STARFORTRESS_MID, 3f);
            stations.add(Industries.STARFORTRESS_HIGH, 1f);
        } else if (level >= 2f) {
            stations.add(Industries.BATTLESTATION, 5f);
            stations.add(Industries.BATTLESTATION_MID, 3f);
            stations.add(Industries.BATTLESTATION_HIGH, 1f);

        } else {
            stations.add(Industries.ORBITALSTATION, 5f);
            stations.add(Industries.ORBITALSTATION_MID, 3f);
            stations.add(Industries.ORBITALSTATION_HIGH, 1f);
        }

        return stations.pick();
    }

    protected Industry getStationIndustry() {
        for (Industry curr : market.getIndustries()) {
            if (curr.getSpec().hasTag(Industries.TAG_STATION)) {
                return curr;
            }
        }
        return null;
    }

    public CampaignFleetAPI getAddedListenerTo() {
        return addedListenerTo;
    }

    protected CampaignFleetAPI addedListenerTo = null;

    @Override
    protected void advanceImpl(float amount) {
        float days = Global.getSector().getClock().convertToDays(amount);
        if (getPlayerVisibleTimestamp() == null && entity.isInCurrentLocation() && isHidden()) {
            makeKnown();
            sendUpdateIfPlayerHasIntel(DISCOVERED_PARAM, false);
        }

        if (!sentBountyUpdate && bountyData != null
                && (Global.getSector().getIntelManager().isPlayerInRangeOfCommRelay()
                || (!isHidden() && DebugFlags.SEND_UPDATES_WHEN_NO_COMM))) {
            makeKnown();
            sendUpdateIfPlayerHasIntel(bountyData, false);
            sentBountyUpdate = true;
        }

        CampaignFleetAPI fleet = Misc.getStationFleet(market);
        if (fleet != null && addedListenerTo != fleet) {
            if (addedListenerTo != null) {
                addedListenerTo.removeEventListener(this);
            }
            fleet.addEventListener(this);
            addedListenerTo = fleet;
        }

        if (target != null) {
            if (getAffectedMarkets(target).isEmpty() || !Misc.getMarketsInLocation(target, LEGIO_ID).isEmpty()) {
                endAfterDelay();
            }
        }

        if (DebugFlags.RAID_DEBUG) {
            days *= 100f;
        }
        
        elapsedDays += days;
        monthlyInterval.advance(days);

        if (bountyData != null) {
            boolean canEndBounty = !entity.isInCurrentLocation() && BOUNTY_DAYS > 0;
            bountyData.bountyElapsedDays += days;
            if (bountyData.bountyElapsedDays > bountyData.bountyDuration && canEndBounty) {
                endBounty();
            }
        }
        
        if (BASE_DURATION > 0 && elapsedDays >= BASE_DURATION && !isEnding()) {
            endAfterDelay();
            return; // no switching targets while expired it's annoying
        }
        
        if (monthlyInterval.intervalElapsed()) {
            monthsWithSameTarget++;
            raidTimeoutMonths--;
            if (raidTimeoutMonths < 0) {
                raidTimeoutMonths = 0;
            }

            if (target != null && bountyData == null) {
                setBounty();
            }

            if (target != null && raidTimeoutMonths <= 0) {
                startRaid(target, getRaidFP());
                raidTimeoutMonths = 2 + (int) Math.round((float) Math.random() * 3f);
            }
        }
    }

    public void makeKnown() {
        makeKnown(null);
    }

    public void makeKnown(TextPanelAPI text) {

        if (getPlayerVisibleTimestamp() == null) {
            Global.getSector().getIntelManager().removeIntel(this);
            Global.getSector().getIntelManager().addIntel(this, text == null, text);
        }
    }

    @Override
    public float getTimeRemainingFraction() {
        float f = 1f;
        if (BASE_DURATION > 0) {
            f = 1f - elapsedDays / BASE_DURATION;
        }
        return f;
    }

    @Override
    protected void notifyEnding() {
        super.notifyEnding();
        endBounty();
        log.info(String.format("Removing legio siege base at [%s]", system.getName()));
        Global.getSector().getListenerManager().removeListener(this);
        Global.getSector().getEconomy().removeMarket(market);
        Global.getSector().getEconomy().removeUpdateListener(this);
        Misc.removeRadioChatter(market);
        market.advance(0f);
    }

    @Override
    protected void notifyEnded() {
        super.notifyEnded();
    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        if (isEnding()) {
            return;
        }

        //CampaignFleetAPI station = Misc.getStationFleet(market); // null here since it's the skeleton station at this point
        if (addedListenerTo != null && fleet == addedListenerTo) {
            Misc.fadeAndExpire(entity);
            endAfterDelay();

            result = new BountyResult(BountyResultType.END_OTHER, 0, null);

            if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE
                    && param instanceof BattleAPI) {
                BattleAPI battle = (BattleAPI) param;
                if (battle.isPlayerInvolved()) {
                    int payment = 0;
                    if (bountyData != null) {
                        payment = (int) (bountyData.baseBounty * battle.getPlayerInvolvementFraction());
                    }
                    if (payment > 0) {
                        Global.getSector().getPlayerFleet().getCargo().getCredits().add(payment);

                        CustomRepImpact impact = new CustomRepImpact();
                        impact.delta = bountyData.repChange * battle.getPlayerInvolvementFraction();
                        if (impact.delta < 0.01f) {
                            impact.delta = 0.01f;
                        }
                        ReputationAdjustmentResult rep = Global.getSector().adjustPlayerReputation(
                                new RepActionEnvelope(RepActions.CUSTOM,
                                        impact, null, null, false, true),
                                bountyData.bountyFaction.getId());

                        result = new BountyResult(BountyResultType.END_PLAYER_BOUNTY, payment, rep);
                    } else {
                        result = new BountyResult(BountyResultType.END_PLAYER_NO_REWARD, 0, null);
                    }
                }
            }

            sendUpdateIfPlayerHasIntel(result, false);
        }
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

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

        if (bountyData != null && result == null) {
            if (getListInfoParam() != BOUNTY_EXPIRED_PARAM) {
                if (isUpdate || mode != ListInfoMode.IN_DESC) {
                    FactionAPI faction = bountyData.bountyFaction;
                    info.addPara("Bounty faction: " + faction.getDisplayName(), initPad, tc,
                            faction.getBaseUIColor(), faction.getDisplayName());
                    initPad = 0f;
                }
                info.addPara("%s reward", initPad, tc, h, Misc.getDGSCredits(bountyData.baseBounty));
                if (BOUNTY_DAYS > 0) {
                    addDays(info, "remaining", bountyData.bountyDuration - bountyData.bountyElapsedDays, tc);
                }
            }
        }

        if (result != null && bountyData != null) {
            switch (result.type) {
                case END_PLAYER_BOUNTY:
                    info.addPara("%s received", initPad, tc, h, Misc.getDGSCredits(result.payment));
                    CoreReputationPlugin.addAdjustmentMessage(result.rep.delta, bountyData.bountyFaction, null,
                            null, null, info, tc, isUpdate, 0f);
                    break;
                case END_TIME:
                    break;
                case END_OTHER:
                    break;

            }
        }

        unindent(info);
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        Color c = getTitleColor(mode);
        info.addPara(getName(), c, 0f);
        addBulletPoints(info, mode);
    }

    @Override
    public String getSortString() {
        String base = Misc.ucFirst(getFactionForUIColors().getPersonNamePrefix());
        return base + " Base";
    }

    public String getName() {
        String base = Misc.ucFirst(getFactionForUIColors().getPersonNamePrefix());

        if (getListInfoParam() == bountyData && bountyData != null) {
            return base + " Base - Bounty Posted";
        } else if (getListInfoParam() == BOUNTY_EXPIRED_PARAM) {
            return base + " Base - Bounty Expired";
        }

        if (result != null) {
            if (result.type == BountyResultType.END_PLAYER_BOUNTY) {
                return base + " Base - Bounty Completed";
            } else if (result.type == BountyResultType.END_PLAYER_NO_REWARD) {
                return base + " Base - Destroyed";
            }
        }

        String name = market.getName();
        if (isEnding()) {
            //return "Base Abandoned - " + name;
            return base + " Base - Abandoned";
        }
        if (getListInfoParam() == DISCOVERED_PARAM) {
            return base + " Base - Discovered";
        }
        if (entity.isDiscoverable()) {
            return base + " Base - Exact Location Unknown";
        }
        return base + " Base - " + name;
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        return market.getFaction();
    }

    @Override
    public String getSmallDescriptionTitle() {
        return getName();
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {

        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        Color tc = Misc.getTextColor();
        float pad = 3f;
        float opad = 10f;

        //info.addPara(getName(), c, 0f);
        //info.addSectionHeading(getName(), Alignment.MID, 0f);
        FactionAPI faction = market.getFaction();

        info.addImage(faction.getLogo(), width, 128, opad);

        String has = faction.getDisplayNameHasOrHave();

        info.addPara(Misc.ucFirst(faction.getDisplayNameWithArticle()) + " " + has
                + " established a base to besiege the "
                + system.getNameWithLowercaseType() + ". "
                + "The base serves as a staging ground for raids against nearby colonies.",
                opad, faction.getBaseUIColor(), faction.getDisplayNameWithArticleWithoutArticle());

        if (!entity.isDiscoverable()) {
            info.addPara("It has very well-developed defensive capabilities "
                    + "and is protected by a large number of fleets. Both the "
                    + "base and the fleets have elite-level equipment.", opad);

        } else {
            info.addPara("You have not yet discovered the exact location or capabilities of this base.", opad);
        }

        info.addSectionHeading("Recent events",
                faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, opad);

        if (target != null && !getAffectedMarkets(target).isEmpty() && !isEnding()) {
            info.addPara("Legio fleets operating from this base have been targeting the "
                    + target.getNameWithLowercaseType() + ".", opad);
        }

        if (bountyData != null) {
            info.addPara(Misc.ucFirst(bountyData.bountyFaction.getDisplayNameWithArticle()) + " "
                    + bountyData.bountyFaction.getDisplayNameHasOrHave()
                    + " posted a bounty for the destruction of this base.",
                    opad, bountyData.bountyFaction.getBaseUIColor(),
                    bountyData.bountyFaction.getDisplayNameWithArticleWithoutArticle());

            if (result != null && result.type == BountyResultType.END_PLAYER_BOUNTY) {
                info.addPara("You have successfully completed this bounty.", opad);
            }

            addBulletPoints(info, ListInfoMode.IN_DESC);
        }

        if (result != null) {
            if (result.type == BountyResultType.END_PLAYER_NO_REWARD) {
                info.addPara("You have destroyed this base.", opad);
            } else if (result.type == BountyResultType.END_OTHER) {
                info.addPara("It is rumored that this base is no longer operational.", opad);
            }
        }

    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        if (bountyData != null) {
            tags.add(Tags.INTEL_BOUNTY);
        }
        tags.add(Tags.INTEL_EXPLORATION);

        if (target != null && !Misc.getMarketsInLocation(target, Factions.PLAYER).isEmpty()) {
            tags.add(Tags.INTEL_COLONIES);
        }

        tags.add(market.getFactionId());
        if (bountyData != null) {
            tags.add(bountyData.bountyFaction.getId());
        }
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        if (market.getPrimaryEntity().isDiscoverable()) {
            return system.getHyperspaceAnchor();
        }
        return market.getPrimaryEntity();
    }

    protected String generateName() {
        MarkovNames.loadIfNeeded();

        MarkovNameResult gen;
        for (int i = 0; i < 10; i++) {
            gen = MarkovNames.generate(null);
            if (gen != null) {
                String test = gen.name;
                if (test.toLowerCase().startsWith("the ")) {
                    continue;
                }
                String p = pickPostfix();
                if (p != null && !p.isEmpty()) {
                    test += " " + p;
                }
                if (test.length() > 22) {
                    continue;
                }

                return test;
            }
        }
        return null;
    }

    protected String pickPostfix() {
        WeightedRandomPicker<String> post = new WeightedRandomPicker<>();
        post.add("Asylum");
        post.add("Astrome");
        post.add("Barrage");
        post.add("Base");
        post.add("Briganderie");
        post.add("Camp");
        post.add("Cover");
        post.add("Citadel");
        post.add("Den");
        post.add("Donjon");
        post.add("Depot");
        post.add("Fort");
        post.add("Galastat");
        post.add("Garrison");
        post.add("Headquarters");
        post.add("Hold");
        post.add("Lair");
        post.add("Locus");
        post.add("Main");
        post.add("Nexus");
        post.add("Orbit");
        post.add("Post");
        post.add("Presidio");
        post.add("Prison");
        post.add("Platform");
        post.add("Corsairie");
        post.add("Shadow");
        post.add("Starhold");
        post.add("Sanctuary");
        post.add("Station");
        post.add("Spacedock");
        post.add("Tertiary");
        post.add("Ward");
        post.add("Warsat");
        return post.pick();
    }

    @Override
    public void commodityUpdated(String commodityId) {
        CommodityOnMarketAPI com = market.getCommodityData(commodityId);
        int curr = 0;
        String modId = market.getId();
        StatMod mod = com.getAvailableStat().getFlatStatMod(modId);
        if (mod != null) {
            curr = Math.round(mod.value);
        }

        int a = com.getAvailable() - curr;
        int d = com.getMaxDemand();
        if (d > a) {
            int supply = d - a;
            com.getAvailableStat().modifyFlat(modId, supply, "Brought in by raiders");
        }
    }

    @Override
    public void economyUpdated() {

        float cycle = Global.getSector().getClock().getCycle() - 206;

        float fleetSizeBonus = Math.min(BASE_PATROL_SIZE_MULT + (cycle * PATROL_SIZE_PER_CYCLE), MAX_PATROL_SIZE_MULT);
        float qualityBonus = Math.min(BASE_PATROL_SHIPQUAL_BONUS + (cycle * PATROL_SHIPQUAL_PER_CYCLE), 1f);
        int light = (int) Math.min(BASE_LIGHT_PATROLS + (cycle * LIGHT_PATROLS_PER_CYCLE), MAX_LIGHT_PATROLS);
        int medium = (int) Math.min(BASE_MED_PATROLS + (cycle * MED_PATROLS_PER_CYCLE), MAX_MED_PATROLS);
        int heavy = (int) Math.min(BASE_HEAVY_PATROLS + (cycle * HEAVY_PATROLS_PER_CYCLE), MAX_HEAVY_PATROLS);

        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).
                modifyFlatAlways(market.getId(), qualityBonus,
                        "Development level");

        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlatAlways(market.getId(),
                fleetSizeBonus,
                "Development level");

        String modId = market.getId();
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_LIGHT_MOD).modifyFlat(modId, light);
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_MEDIUM_MOD).modifyFlat(modId, medium);
        market.getStats().getDynamic().getMod(Stats.PATROL_NUM_HEAVY_MOD).modifyFlat(modId, heavy);
    }

    @Override
    public boolean isEconomyListenerExpired() {
        return isEnded();
    }

    public MarketAPI getMarket() {
        return market;
    }

    protected void setBounty() {
        bountyData = new BaseBountyData();

        float level = getLevel();
        float baseBounty = BASE_BOUNTY_CREDITS * level;

        bountyData.repChange = 0.15f * level;

        bountyData.baseBounty = baseBounty * (0.9f + (float) Math.random() * 0.2f);

        bountyData.baseBounty = (int) (bountyData.baseBounty / 10000) * 10000;

        WeightedRandomPicker<FactionAPI> picker = new WeightedRandomPicker<>();
        for (MarketAPI curr : Global.getSector().getEconomy().getMarkets(target)) {
            if (curr.getFaction().isPlayerFaction()) {
                continue;
            }
            if (affectsMarket(curr)) {
                picker.add(curr.getFaction(), (float) Math.pow(2f, curr.getSize()));
            }
        }

        FactionAPI faction = picker.pick();
        if (faction == null) {
            bountyData = null;
            return;
        }

        bountyData.bountyFaction = faction;
        bountyData.bountyDuration = BOUNTY_DAYS;
        bountyData.bountyElapsedDays = 0f;

        Misc.makeImportant(entity, "baseBounty");

        sentBountyUpdate = false;
    }

    protected void endBounty() {
        sendUpdateIfPlayerHasIntel(BOUNTY_EXPIRED_PARAM, false);
        bountyData = null;
        sentBountyUpdate = false;
        Misc.makeUnimportant(entity, "baseBounty");
    }

    public StarSystemAPI getTarget() {
        return target;
    }

    public List<MarketAPI> getAffectedMarkets(StarSystemAPI system) {
        List<MarketAPI> affected = new ArrayList<>();
        for (MarketAPI curr : Global.getSector().getEconomy().getMarkets(system)) {
            if (!affectsMarket(curr)) {
                continue;
            }
            affected.add(curr);
        }
        return affected;
    }

    public boolean affectsMarket(MarketAPI market) {
        if (market.isHidden()) {
            return false;
        }
        if (!market.getFaction().isHostileTo(LEGIO_ID)) {
            return false;
        }
        return market.getFaction() != this.market.getFaction();
    }

    @Override
    public List<ArrowData> getArrowData(SectorMapAPI map) {
        if (target == null || target == entity.getContainingLocation()) {
            return null;
        }

        List<ArrowData> arrows = new ArrayList<>();

        SectorEntityToken entityFrom = entity;
        if (map != null) {
            SectorEntityToken iconEntity = map.getIntelIconEntity(this);
            if (iconEntity != null) {
                entityFrom = iconEntity;
            }
        }

        ArrowData arrow = new ArrowData(entityFrom, target.getCenter());
        arrow.color = getFactionForUIColors().getBaseUIColor();
        arrows.add(arrow);

        return arrows;
    }

    public float getAccessibilityPenalty() {
        return getLevel() * 0.3f;
    }

    public float getStabilityPenalty() {
        return getLevel() * 2f;
    }

    public SectorEntityToken getEntity() {
        return entity;
    }

    public static PersonAPI createAdmin(MarketAPI market) {
        FactionAPI faction = market.getFaction();
        PersonAPI admin = faction.createRandomPerson();
        int size = market.getSize();

        admin.setPostId(Ranks.POST_ADMINISTRATOR);

        switch (size) {
            case 3:
            case 4:
                admin.setRankId(Ranks.GROUND_CAPTAIN);
                break;
            case 5:
                admin.setRankId(Ranks.GROUND_MAJOR);
                break;
            case 6:
                admin.setRankId(Ranks.GROUND_COLONEL);
                break;
            case 7:
            case 8:
            case 9:
            case 10:
                admin.setRankId(Ranks.GROUND_GENERAL);
                break;
            default:
                admin.setRankId(Ranks.GROUND_LIEUTENANT);
                break;
        }

        List<String> skills = Global.getSettings().getSortedSkillIds();

        int industries = 0;
        int defenses = 0;
        boolean military = market.getMemoryWithoutUpdate().getBoolean(MemFlags.MARKET_MILITARY);

        for (Industry curr : market.getIndustries()) {
            if (curr.isIndustry()) {
                industries++;
            }
            if (curr.getSpec().hasTag(Industries.TAG_GROUNDDEFENSES)) {
                defenses++;
            }
        }

        admin.getStats().setSkipRefresh(true);

        int num = 0;
        if (industries >= 2 || (industries == 1 && defenses == 1)) {
            if (skills.contains(Skills.INDUSTRIAL_PLANNING)) {
                admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3);
            }
            num++;
        }

        if (num == 0 || size >= 7) {
            if (military) {
                if (skills.contains(Skills.FLEET_LOGISTICS)) {
                    admin.getStats().setSkillLevel(Skills.FLEET_LOGISTICS, 3);
                }
            } else if (defenses > 0) {
                if (skills.contains(Skills.PLANETARY_OPERATIONS)) {
                    admin.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, 3);
                }
            } else {
                // nothing else suitable, so just make sure there's at least one skill, if this wasn't already set
                if (skills.contains(Skills.INDUSTRIAL_PLANNING)) {
                    admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 3);
                }
            }
        }

        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        admin.getStats().setSkipRefresh(false);
        admin.getStats().refreshCharacterStatsEffects();
        market.addPerson(admin);
        market.setAdmin(admin);
        market.getCommDirectory().addPerson(admin);
        ip.addPerson(admin);
        ip.getData(admin).getLocation().setMarket(market);
        ip.checkOutPerson(admin, "permanent_staff");

        log.info(String.format("Applying admin %s %s to market %s", market.getFaction().getRank(admin.getRankId()), admin.getNameString(), market.getName()));

        return admin;
    }
}
