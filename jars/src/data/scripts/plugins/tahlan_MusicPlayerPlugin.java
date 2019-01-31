package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.combat.entities.Ship;

import java.util.ArrayList;
import java.util.List;

public class tahlan_MusicPlayerPlugin extends BaseEveryFrameCombatPlugin {

    private static boolean MUSIC_PLAYING = false;
    private List<ShipAPI> trackedShips = new ArrayList<ShipAPI>();


    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        List<ShipAPI> untrack = new ArrayList<ShipAPI>();
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getHullSpec().getHullId().contains("tahlan_Izanami")) {
                trackedShips.add(ship);
            }
        }

        for (ShipAPI ship : trackedShips) {
            if (ship.getHullSpec().getHullId().contains("tahlan_Izanami")) {
                if (!MUSIC_PLAYING && ship.isAlive()) {
                    Global.getSoundPlayer().playCustomMusic(1, 1, "tahlan_kassadari_theme", true);
                    MUSIC_PLAYING = true;
                } else if (!ship.isAlive()) {
                    Global.getSoundPlayer().pauseCustomMusic();
                    MUSIC_PLAYING = false;
                    untrack.add(ship);
                }
            }
        }

        for (ShipAPI ship : untrack) {
            trackedShips.remove(ship);
        }

    }
}
