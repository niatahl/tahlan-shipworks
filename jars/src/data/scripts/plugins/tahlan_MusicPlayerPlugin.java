package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class tahlan_MusicPlayerPlugin extends BaseEveryFrameCombatPlugin {

    private static boolean MUSIC_PLAYING = false;
    private static List<ShipAPI> trackedShips = new ArrayList<ShipAPI>();


    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        List<ShipAPI> untrack = new ArrayList<ShipAPI>();
        CombatEngineAPI engine = Global.getCombatEngine();

        for (ShipAPI ship : engine.getShips()) {
            if (ship.getHullSpec().getHullId().contains("tahlan_Izanami") && !trackedShips.contains(ship)) {
                trackedShips.add(ship);
            }
        }

        for (ShipAPI ship : trackedShips) {
            if (ship.getHullSpec().getHullId().contains("tahlan_Izanami")) {
                if (!MUSIC_PLAYING && ship.isAlive()) {
                    Global.getSoundPlayer().playCustomMusic(1, 1, "tahlan_kassadari_theme", true);
                    MUSIC_PLAYING = true;
                } else if (!ship.isAlive() || engine.isCombatOver()) {
                    if (Global.getSoundPlayer().getCurrentMusicId().equals("tahlan_kassadari_theme.ogg")) {
                        Global.getSoundPlayer().restartCurrentMusic();
                        engine.addFloatingText(ship.getLocation(),"HERE",15,Color.white,ship,5,5);
                    }
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
