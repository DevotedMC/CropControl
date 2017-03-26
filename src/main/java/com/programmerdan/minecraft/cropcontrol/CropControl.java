package com.programmerdan.minecraft.cropcontrol;

import com.programmerdan.minecraft.cropcontrol.data.DAO;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlDatabaseHandler;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlEventHandler;

import vg.civcraft.mc.civmodcore.ACivMod;

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
		if (!this.isEnabled()) return;

		registerEventHandler();

		
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
		if (!this.isEnabled()) return;
		try {
			this.eventHandler = new CropControlEventHandler(getConfig());
			this.getServer().getPluginManager().registerEvents(eventHandler, this);
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
	
	public static DAO getDAO() {
		return CropControlDatabaseHandler.getDAO();
	}
	
	/**
	 * 
	 * @return the name of this plugin.
	 */
	@Override
	protected String getPluginName() {
		return "CropControl";
	}

}
