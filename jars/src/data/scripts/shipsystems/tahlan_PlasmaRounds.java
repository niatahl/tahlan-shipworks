package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.ArrayList;
import java.util.List;

public class tahlan_PlasmaRounds extends BaseShipSystemScript {
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

	}

	public void unapply(MutableShipStatsAPI stats, String id) {

	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Plasma rounds loaded", false);
		}
		return null;
	}
}