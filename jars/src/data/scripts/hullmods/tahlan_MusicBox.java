package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.apache.log4j.Level;

public class tahlan_MusicBox extends BaseHullMod {

    private boolean MUSIC_PLAYING = false;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        CombatEngineAPI engine = Global.getCombatEngine();

        MUSIC_PLAYING = Global.getSoundPlayer().getCurrentMusicId().equals("tahlan_kassadari_theme.ogg");
        if (ship.getHullSpec().getHullId().contains("tahlan_Izanami")) {
            if (!MUSIC_PLAYING && ship.isAlive() && !engine.isSimulation()) {
                Global.getSoundPlayer().playCustomMusic(1, 1, "tahlan_kassadari_theme", true);
                //Global.getSoundPlayer().pauseCustomMusic();
                MUSIC_PLAYING = true;
            } else if (!ship.isAlive()) {
                if (Global.getSoundPlayer().getCurrentMusicId().equals("tahlan_kassadari_theme.ogg")) {
                    Global.getSoundPlayer().pauseCustomMusic();
                    MUSIC_PLAYING = false;
                }
            }
        }
    }


}
