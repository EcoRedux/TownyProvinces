package io.github.townyadvanced.townyprovinces.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.PreNewTownEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.event.TranslationLoadEvent;
import com.palmergames.bukkit.towny.event.town.TownPreMergeEvent;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.TranslationLoader;
import com.palmergames.bukkit.towny.object.WorldCoord;
import io.github.townyadvanced.townyprovinces.TownyProvinces;
import io.github.townyadvanced.townyprovinces.data.TownyProvincesDataHolder;
import io.github.townyadvanced.townyprovinces.objects.Province;
import io.github.townyadvanced.townyprovinces.settings.TownyProvincesSettings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

public class TownyListener implements Listener {

	/* Handle reloading MetaData when Towny's DB is loaded. */
//	@EventHandler
	public void onTownyLoadDB(TownyLoadedDatabaseEvent event) {
	}

	/* Handle re-adding the lang string into Towny when Towny reloads the Translation Registry. */
	@EventHandler
	public void onTownyLoadLang(TranslationLoadEvent event) {
		Plugin plugin = TownyProvinces.getPlugin();
		Path langFolderPath = Paths.get(plugin.getDataFolder().getPath()).resolve("lang");
		TranslationLoader loader = new TranslationLoader(langFolderPath, plugin, TownyProvinces.class);
		loader.load();
		Map<String, Map<String, String>> translations = loader.getTranslations();
		for (String language : translations.keySet())
			for (Map.Entry<String, String> map : translations.get(language).entrySet())
				event.addTranslation(language, map.getKey(), map.getValue());
	}

	@EventHandler(ignoreCancelled = true)
	public void onTownMergeAttempt(TownPreMergeEvent event) {
		if (!TownyProvincesSettings.isTownyProvincesEnabled()) {
			return;
		}
		event.setCancelled(true);
		event.setCancelMessage(TownyProvinces.getTranslatedPrefix() + Translatable.of("msg_err_cannot_merge_towns").translate(Locale.ROOT));
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onNewTownAttempt(PreNewTownEvent event) {
		if (!TownyProvincesSettings.isTownyProvincesEnabled()) {
			return;
		}
		//No restriction if it is not in the TP-enabled world
		if (!event.getTownWorldCoord().getWorldName().equalsIgnoreCase(TownyProvincesSettings.getWorldName())) {
			return;
		}
		/* 
		 * Can't place new town outside a province
		 * Note: unless some hacking has occurred,
		 * the only possible place, this can occur,
		 * is on a province border.
		 */
		Coord coord = Coord.parseCoord(event.getTownLocation());
		Province province = TownyProvincesDataHolder.getInstance().getProvinceAt(coord);
		if (province == null) {
			event.setCancelled(true);
			event.setCancelMessage(TownyProvinces.getTranslatedPrefix() + " " + Translatable.of("msg_err_cannot_create_town_on_province_border").translate(Locale.ROOT));
			return;
		}
		//Can't place town in Sea province
		if(province.isSea()) {
			event.setCancelled(true);
			event.setCancelMessage(TownyProvinces.getTranslatedPrefix() + " " + Translatable.of("msg_err_cannot_create_town_in_sea_provinces").translate(Locale.ROOT));
			return;
		}
		//Can't place new town is province-at-location already has one
		if (doesProvinceContainTown(province)) {
			event.setCancelled(true);
			event.setCancelMessage(TownyProvinces.getTranslatedPrefix() + " " + Translatable.of("msg_err_cannot_create_town_in_full_province").translate(Locale.ROOT));
			return;
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onTownClaimAttempt(TownPreClaimEvent event) {
		if (!TownyProvincesSettings.isTownyProvincesEnabled()) {
			return;
		}
		//No restriction if it is not in the TP-enabled world
		if (!event.getTownBlock().getWorld().getName().equalsIgnoreCase(TownyProvincesSettings.getWorldName())) {
			return;
		}
		/*
		 * Can't clam outside a province
		 * Note: unless some hacking has occurred,
		 * the only possible place, this can occur,
		 * is on a province border.
		 */
		Province provinceAtClaimLocation = TownyProvincesDataHolder.getInstance().getProvinceAt(event.getTownBlock().getCoord());
		if (provinceAtClaimLocation == null) {
			event.setCancelled(true);
			event.setCancelMessage(TownyProvinces.getTranslatedPrefix() + " " + Translatable.of("msg_err_cannot_claim_land_on_province_border").translate(Locale.ROOT));
			return;
		}
		//Can't claim without homeblock
		if(!event.getTown().hasHomeBlock()) {
			event.setCancelled(true);
			event.setCancelMessage(TownyProvinces.getTranslatedPrefix() + " " + Translatable.of("msg_err_cannot_claim_land_without_homeblock").translate(Locale.ROOT));
			return;
		}
		//Can't claim outside town's province
		Province provinceOfClaimingTown = TownyProvincesDataHolder.getInstance().getProvinceAt(event.getTown().getHomeBlockOrNull().getCoord());
		if(!provinceOfClaimingTown.equals(provinceAtClaimLocation)) {
			event.setCancelled(true);
			event.setCancelMessage(TownyProvinces.getTranslatedPrefix() + " " + Translatable.of("msg_err_cannot_claim_land_outside_own_province").translate(Locale.ROOT));
			return;
		}
	}
		
	private boolean doesProvinceContainTown(Province province) {
		String worldName = TownyProvincesSettings.getWorldName();
		WorldCoord worldCoord;
		for(Coord coord: province.getCoordsInProvince()) {
			worldCoord = new WorldCoord(worldName, coord);
			if(!TownyAPI.getInstance().isWilderness(worldCoord)) {
				return true;
			}
		}
		return false;
	}

}