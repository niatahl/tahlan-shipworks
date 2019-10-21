package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.*;

public class tahlan_Afterburners extends BaseHullMod {

    private static final Color OVERDRIVE_ENGINE_COLOR = new Color(255, 44, 0);
    private static final String id = "tahlan_afterburners_id";

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        boolean missilesLoaded = true;
        for (WeaponAPI currentWeapon : ship.getAllWeapons()) {
            if (currentWeapon.getSlot().getId().contains("MSL") && currentWeapon.getAmmo() <= 0f)
                missilesLoaded = false;
        }
        if (!missilesLoaded) {
            ship.getMutableStats().getMaxSpeed().modifyFlat(id,50f);
            ship.getMutableStats().getMaxTurnRate().modifyFlat(id,100f);
            ship.getMutableStats().getAcceleration().modifyFlat(id,150f);
            ship.getMutableStats().getTurnAcceleration().modifyFlat(id,150f);
            ship.getEngineController().fadeToOtherColor(this,OVERDRIVE_ENGINE_COLOR,null,1f,0.7f);
            ship.getEngineController().extendFlame(this,1.2f,1.2f,1f);
        }
    }
}
