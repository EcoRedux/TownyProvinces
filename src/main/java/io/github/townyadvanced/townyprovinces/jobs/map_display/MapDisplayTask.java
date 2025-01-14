package io.github.townyadvanced.townyprovinces.jobs.map_display;

import io.github.townyadvanced.townyprovinces.TownyProvinces;
import org.bukkit.scheduler.BukkitRunnable;


public class MapDisplayTask extends BukkitRunnable {
	private boolean jobRunning;

    MapDisplayTask() {
		this.jobRunning = false;
    }

    public void run() {
		try {
			if(jobRunning) {
				return;
			} else {
				jobRunning = true;
			}
			synchronized (TownyProvinces.MAP_DISPLAY_LOCK) {
				for (DisplayProvincesOnMapAction mapDisplayAction : MapDisplayTaskController.getMapDisplayActions()) {
					mapDisplayAction.executeAction(MapDisplayTaskController.getFullProvinceColoursRefreshRequested(), MapDisplayTaskController.getFullHomeBlockIconsRefreshRequest());
				}
			}
		} finally {
			//Reset refresh flags
			MapDisplayTaskController.setFullProvinceColoursRefreshRequested(false);
			MapDisplayTaskController.setFullHomeBlockIconsRefreshRequest(false);
			jobRunning = false;
		}
	}
}