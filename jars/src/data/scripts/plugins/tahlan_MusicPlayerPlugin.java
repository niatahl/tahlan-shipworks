package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class tahlan_MusicPlayerPlugin extends BaseEveryFrameCombatPlugin {

    private static final Map<String, String> SHIP_THEMES = new HashMap<String, String>();
    private static final Map<String, String> THEME_TRACKS = new HashMap<String, String>();

    //We map all supported ships and their themes here
    static {
        SHIP_THEMES.put("tahlan_Izanami", "tahlan_kassadari_theme");
    }

    //Here we map all the names of the sound files for each theme
    static {
        THEME_TRACKS.put("tahlan_kassadari_theme", "tahlan_kassadari_theme");
    }

    //private boolean MUSIC_PLAYING = false;
    private String CURRENT_TRACK = null;
    private List<ShipAPI> trackedShips = new ArrayList<ShipAPI>();
    private IntervalUtil tracker = new IntervalUtil(1, 1);

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        //run only oncer per sec
        tracker.advance(amount);
        if (tracker.intervalElapsed()) {

            List<ShipAPI> untrack = new ArrayList<ShipAPI>();
            CombatEngineAPI engine = Global.getCombatEngine();

            //Search for supported ships and track them
            for (ShipAPI ship : engine.getShips()) {
                String HullId = ship.getHullSpec().getHullId();
                if (SHIP_THEMES.containsKey(HullId) && !trackedShips.contains(ship)) {
                    trackedShips.add(ship);
                }
            }


            for (ShipAPI ship : trackedShips) {
                String HullId = ship.getHullSpec().getHullId();

                //If we aren't playing a track, we start one for the first supported ship in the list
                if (CURRENT_TRACK == null && ship.isAlive() && !engine.isSimulation()) {
                    Global.getSoundPlayer().playCustomMusic(1, 1, SHIP_THEMES.get(HullId), true);
                    CURRENT_TRACK = SHIP_THEMES.get(HullId);
                    //MUSIC_PLAYING = true;

                //If we are, we check if the ship belongs to the current track and has died, in which case we stop the track and untrack the ship
                } else if (!ship.isAlive() && SHIP_THEMES.get(HullId) == CURRENT_TRACK) {
                    if (Global.getSoundPlayer().getCurrentMusicId().contains(THEME_TRACKS.get(SHIP_THEMES.get(HullId)))) {
                        Global.getSoundPlayer().pauseCustomMusic();
                    }
                    //MUSIC_PLAYING = false;
                    CURRENT_TRACK = null;
                    untrack.add(ship);
                }

            }

            for (ShipAPI ship : untrack) {
                trackedShips.remove(ship);
            }
            untrack.clear();
        }
    }
}
