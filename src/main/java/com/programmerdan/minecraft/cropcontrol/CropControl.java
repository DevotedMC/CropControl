package com.programmerdan.minecraft.cropcontrol;

import com.programmerdan.minecraft.cropcontrol.data.DAO;
import com.programmerdan.minecraft.cropcontrol.handler.CastleGatesEventHandler;
import com.programmerdan.minecraft.cropcontrol.handler.CitadelEventHandler;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlDatabaseHandler;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlEventHandler;
import com.programmerdan.minecraft.cropcontrol.handler.RealisticBiomesEventHandler;

import vg.civcraft.mc.civmodcore.ACivMod;

/**
 * Welcome to Crop Control! This is a companion mod to our other plugin, HiddenOre, and does for crops what HiddenOre does for ores.
 * 
 * It gives you augmentation control over the drops that crops generate, allowing you to give specific bonuses and penalties based
 * on who planted vs. who harvested, how it was harvested, where; and this control extends to all growables, including trees, 
 * crops, chorus fruit, and more.
 * 
 * Read the example configs for details. Requires a database.
 * 
 * @author ProgrammerDan, xFier
 *
 */
public class CropControl extends ACivMod {
	private static CropControl instance;
	private CropControlEventHandler eventHandler;
	private CropControlDatabaseHandler databaseHandler;
	
	@Override
	public void onEnable() {
		super.onEnable();

		saveDefaultConfig();
		reloadConfig();
		
		CropControl.instance = this;
		connectDatabase();
		if (!this.isEnabled()) {
			return;
		}

		registerEventHandler();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		
		databaseHandler.doShutdown();
	}
	
	private void connectDatabase() {
		try {
			this.databaseHandler = new CropControlDatabaseHandler(getConfig());
		} catch (Exception e) {
			this.severe("Failed to establish database", e);
			this.setEnabled(false);
		}
	}

	private void registerEventHandler() {
		if (!this.isEnabled()) {
			return;
		}
		try {
			this.eventHandler = new CropControlEventHandler(getConfig());
			this.getServer().getPluginManager().registerEvents(eventHandler, this);
			
			if (this.getServer().getPluginManager().isPluginEnabled("RealisticBiomes")) {
				this.getServer().getPluginManager().registerEvents(new RealisticBiomesEventHandler(), this);
			}

			if (this.getServer().getPluginManager().isPluginEnabled("Citadel")) {
				this.getServer().getPluginManager().registerEvents(new CitadelEventHandler(), this);
			}

			if (this.getServer().getPluginManager().isPluginEnabled("CastleGates")) {
				this.getServer().getPluginManager().registerEvents(new CastleGatesEventHandler(), this);
			}
		} catch (Exception e) {
			this.severe("Failed to set up event capture / handling", e);
			this.setEnabled(false);
		}	
	}

	/**
	 * 
	 * @return the static global instance. Not my fav pattern, but whatever.
	 */
	public static CropControl getPlugin() {
		return CropControl.instance;
	}
	
	/**
	 * 
	 * @return the singleton DAO for this CropControl.
	 */
	public static DAO getDAO() {
		return CropControlDatabaseHandler.getDAO();
	}

	public CropControlEventHandler getEventHandler() {
		return this.eventHandler;
	}

}
