package data.scripts.campaign.siege;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import java.util.List;
import org.apache.log4j.Logger;

public class LegioSiegeManager implements EveryFrameScript {
    
    public static Logger log = Global.getLogger(LegioSiegeManager.class);
    public static final String KEY = "$tahlan_LegioRaidBaseManager";
    public static final String LEGIO_ID = "tahlan_legioinfernalis";
    
    public FactionAPI legio = null;
    
    // ENTERING DEV MODE WILL CAUSE 1 (ONE) LEGIO SIEGE EXPEDITION TO SPAWN INSTANTLY
    public static final float BASE_SPAWN_DAYS = 360f; // starting minimum value for spawn interval
    public static final float MIN_SPAWN_DAYS = 75f; // minimum minimum value for spawn interval, will never spawn faster than this
    public static final float SPAWN_DAYS_PER_CYCLE = 30f; // minimum value for spawn interval will decrease by this much per cycle
    public static final float MAX_SPAWN_DAYS_MULT = 1.33f; // maximum value for spawn interval will be current minimum * this

    public static final float BASE_FP = 150f; // base FP for siege fleet
    public static final float FP_PER_CYCLE = 50f; // increase this much every cycle
    public static final float MAX_FP = 600f; // cap out at this much

    private IntervalUtil timer = null;
    private boolean devSpawn = false;
    
    public static LegioSiegeManager getInstance() {
        Object test = Global.getSector().getMemoryWithoutUpdate().get(KEY);
        return (LegioSiegeManager) test;
    }
    
    public LegioSiegeManager() {
        super();
        Global.getSector().getMemoryWithoutUpdate().set(KEY, this);
        legio = Global.getSector().getFaction(LEGIO_ID);
    }
    
    @Override
    public void advance(float amount) {
        
        if (legio == null) {
            legio = Global.getSector().getFaction(LEGIO_ID);
        }

        // if legio is dead, do nothing
        if (legio == null || !legio.isShowInIntelTab()) {
            return;
        }
        boolean nonHiddenMarkets = false;
        for (MarketAPI market : Misc.getFactionMarkets(legio)) {
            if (!market.isHidden()) {
                nonHiddenMarkets = true;
            }
        }
        if (!nonHiddenMarkets) {
            return;
        }
        
        float days = Global.getSector().getClock().convertToDays(amount);
        
        if (timer == null) {
            timer = newTimer();
            log.info("timer was null, creating new timer");
        }
        
        timer.advance(days);
        if (Global.getSettings().isDevMode() && !devSpawn) {
            timer.forceIntervalElapsed();
            devSpawn = true;
        } else if (!Global.getSettings().isDevMode()) {
            devSpawn = false;
        }
        if (timer.intervalElapsed()) {
            log.info("timer expired, spawning siege fleet");
            spawnSiegeFleet(legio);
        }
    }
    
    private IntervalUtil newTimer() {
        float cycle = Global.getSector().getClock().getCycle() - 206;
        float base = BASE_SPAWN_DAYS;
        float perCycle = cycle * SPAWN_DAYS_PER_CYCLE;
        float min = Math.max(MIN_SPAWN_DAYS, base - perCycle);
        float max = min * MAX_SPAWN_DAYS_MULT;
        log.info("new siege timer, min: " + min + ", max: " + max);
        return new IntervalUtil(min, max);
    }
    
    private void spawnSiegeFleet(FactionAPI faction) {
        timer = newTimer();
        MarketAPI source = pickSource();
        StarSystemAPI target = pickTarget();
        float fp = pickFleetPoints();
        
        if (target == null) {
            log.warn("couldn't pick target, not spawning a siege mission");
            return;
        }
        
        if (faction instanceof FactionAPI) {
            // all good
        } else {
            log.error("WE LOST THE LEGIO FACTION OBJECT WHAT HAPPENED");
            return;
        }
        
        log.info("creating new LegioSiegeMissionIntel, arguments: " + faction.getDisplayNameLong() + ", " + source.getName() + ", " + target.getNameWithTypeIfNebula() + ", " + fp);
        try {
            LegioSiegeMissionIntel intel = new LegioSiegeMissionIntel(faction, source, target, fp);
        } catch (NullPointerException npx) {
            log.error("creating LegioSiegeMissionIntel null crashed for some reason... nightmare bug");
        }
    }

    // siege fleets will spawn from the largest nonhidden market (first one, if there are multiple)
    private MarketAPI pickSource() {
        MarketAPI source = null;
        for (MarketAPI market : Misc.getFactionMarkets(legio)) {
            if (!market.isHidden() && (source == null || market.getSize() > source.getSize())) {
                source = market;
            }
        }
        if (source == null) {
            return null;
        }
        log.info("picked " + source.getName() + " to spawn siege fleet");
        return source;
    }

    // picks a random star system with at least one hostile nonhidden market in it and no legio markets in it or near it in hyperspace
    // weight is based on market size
    private StarSystemAPI pickTarget() {
        WeightedRandomPicker<StarSystemAPI> targets = new WeightedRandomPicker();
        LocationAPI hyperspace = Global.getSector().getHyperspace();
        
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        
        for (MarketAPI market : markets) {
            StarSystemAPI system = market.getStarSystem();

            // skip hidden, nonmarket, and allied markets as well as markets in hyperspace and markets in the same system as a legio market
            if (system == null
                    || market.isPlanetConditionMarketOnly()
                    || market.isHidden()
                    || !market.getFaction().isHostileTo(legio)
                    || !Misc.getMarketsInLocation(system, LEGIO_ID).isEmpty()) {
                continue;
            }

            // skip systems with a legio market in hyperspace near them (we're already besieging them or their neighbour)
            boolean tooClose = false;
            List<MarketAPI> legioInHyper = Misc.getMarketsInLocation(hyperspace, LEGIO_ID);
            for (MarketAPI legioBase : legioInHyper) {
                if (Misc.getDistance(legioBase.getPrimaryEntity(), system.getHyperspaceAnchor()) < 2000f) {
                    tooClose = true;
                }
            }
            if (tooClose) {
                continue;
            }
            
            targets.add(system, market.getSize());
        }
        
        StarSystemAPI target = targets.pick();
        log.info("picked " + target.getNameWithLowercaseType() + " as target for siege fleet");
        
        return target;
    }
    
    private float pickFleetPoints() {
        float cycle = Global.getSector().getClock().getCycle() - 206;
        float base = BASE_FP;
        float perCycle = cycle * FP_PER_CYCLE;
        float max = MAX_FP;
        return Math.min(base + perCycle, max);
    }
    
    @Override
    public boolean isDone() {
        return false;
    }
    
    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
