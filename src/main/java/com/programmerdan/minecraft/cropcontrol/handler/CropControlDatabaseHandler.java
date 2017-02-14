package com.programmerdan.minecraft.cropcontrol.handler;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.programmerdan.minecraft.cropcontrol.CropControl;

import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;

/**
 * Ties into the managed datasource processes of the CivMod core plugin.
 * 
 * @author <a href="mailto:programmerdan@gmail.com">ProgrammerDan</a>
 *
 */
public class CropControlDatabaseHandler {

	private ManagedDatasource data;
	
	public ManagedDatasource getData() {
		return this.data;
	}
	
	private static CropControlDatabaseHandler instance;
	
	public static CropControlDatabaseHandler getInstance() {
		return CropControlDatabaseHandler.instance;
	}
	
	public static ManagedDatasource getinstanceData() {
		return CropControlDatabaseHandler.instance.data;
	}
	
	public CropControlDatabaseHandler(FileConfiguration config) {
		if (!configureData(config.getConfigurationSection("database"))) {
			throw new RuntimeException("Failed to configure Database for CropControl!");
		}
		CropControlDatabaseHandler.instance = this;
	}

	private boolean configureData(ConfigurationSection config) {
		String host = config.getString("host", "localhost");
		int port = config.getInt("port", 3306);
		String dbname = config.getString("database", "cropcontrol");
		String username = config.getString("user");
		String password = config.getString("password");
		int poolsize = config.getInt("poolsize", 5);
		long connectionTimeout = config.getLong("connection_timeout", 10000l);
		long idleTimeout = config.getLong("idle_timeout", 600000l);
		long maxLifetime = config.getLong("max_lifetime", 7200000l);
		try {
			data = new ManagedDatasource(CropControl.getPlugin(), username, password, host, port, dbname,
					poolsize, connectionTimeout, idleTimeout, maxLifetime);
			data.getConnection().close();
		} catch (Exception se) {
			CropControl.getPlugin().info("Failed to initialize Database connection");
			return false;
		}

		initializeTables();		
		stageUpdates();
		
		long begin_time = System.currentTimeMillis();

		try {
			CropControl.getPlugin().info("Update prepared, starting database update.");
			if (!data.updateDatabase()) {
				CropControl.getPlugin().info( "Update failed, disabling plugin.");
				return false;
			}
		} catch (Exception e) {
			CropControl.getPlugin().severe("Update failed, disabling plugin. Cause:", e);
			return false;
		}

		CropControl.getPlugin().info(String.format("Database update took %d seconds", (System.currentTimeMillis() - begin_time) / 1000));
		
		activatePreload(config.getConfigurationSection("preload"));
		activateDirtySave(config.getConfigurationSection("dirtysave"));
		return true;
	}

	/*
	 * Rough data model:
	 * 
	 * [worldchunks]
	 * cid BIGINT
	 * world uuid
	 * chunk_x INT 
	 * chunk_z INT
	 * 
	 * [crop]
	 * crid BIGINT
	 * cid BIGINT
	 * x SMALL
	 * y INT
	 * z SMALL
	 * type STRING
	 * stage SMALL
	 * placed UUID
	 * time LONG
	 * 
	 * [sapling]
	 * sid BIGINT
	 * cid BIGINT
	 * x SMALL
	 * y INT
	 * z SMALL
	 * type STRING
	 * placed UUID
	 * time LONG
	 * 
	 * [tree]
	 * tid BIGINT
	 * cid BIGINT
	 * type STRING
	 * placed UUID
	 * time LONG
	 * 
	 * [tree_component]
	 * tcid BIGINT
	 * tid BIGINT
	 * cid BIGINT
	 * x SMALL
	 * y INT
	 * z SMALL
	 * type SMALL
	 * 
	 *   
	 */

	private void activateDirtySave(ConfigurationSection config) {
		long period = 5*60*1000l;
		long delay = 5*60*1000l;
		if (config != null) {
			period = config.getLong("period", period);
			delay = config.getLong("delay", delay);
		}
		
		Bukkit.getScheduler().runTaskTimerAsynchronously(CropControl.getPlugin(), new Runnable() {
			@Override
			public void run() {
				
			}
		}, delay, period);

		Bukkit.getScheduler().runTaskTimerAsynchronously(CropControl.getPlugin(), new Runnable() {
			@Override
			public void run() {
				
			}
		}, delay + (period / 5), period);
		
		Bukkit.getScheduler().runTaskTimerAsynchronously(CropControl.getPlugin(), new Runnable() {
			@Override
			public void run() {
				
			}
		}, delay + ((period * 2) / 5), period);

		Bukkit.getScheduler().runTaskTimerAsynchronously(CropControl.getPlugin(), new Runnable() {
			@Override
			public void run() {
				
			}
		}, delay + ((period * 3) / 5), period);

		Bukkit.getScheduler().runTaskTimerAsynchronously(CropControl.getPlugin(), new Runnable() {
			@Override
			public void run() {
				
			}
		}, delay + ((period * 4) / 5), period);

		CropControl.getPlugin().info("Dirty save tasks started.");
	}

	private void activatePreload(ConfigurationSection config) {
		if (config != null && config.getBoolean("enabled")) {
			long period = 5*60*1000l;
			long delay = 5*60*1000l;
			if (config != null) {
				period = config.getLong("period", period);
				delay = config.getLong("delay", delay);
			}
			final int batchsize = config.getInt("batch", 100);
			
			new BukkitRunnable() {
				private long lastId = 0l;
				@Override
				public void run() {
					lastId = .preload(lastId, batchsize);
					if (lastId < 0) this.cancel();
				}
			}.runTaskTimerAsynchronously(CropControl.getPlugin(), delay, period);
			
			new BukkitRunnable() {
				private long lastId = 0l;
				@Override
				public void run() {
					lastId = .preload(lastId, batchsize);
					if (lastId < 0) this.cancel();
				}
			}.runTaskTimerAsynchronously(CropControl.getPlugin(), delay + (period / 6), period);
			
			new BukkitRunnable() {
				private long lastId = 0l;
				@Override
				public void run() {
					lastId = .preload(lastId, batchsize, false);
					if (lastId < 0) this.cancel();
				}
			}.runTaskTimerAsynchronously(CropControl.getPlugin(), delay + ((period * 2) / 6), period);
			
			new BukkitRunnable() {
				private long lastId = 0l;
				@Override
				public void run() {
					lastId = .preload(lastId, batchsize);
					if (lastId < 0) this.cancel();
				}
			}.runTaskTimerAsynchronously(CropControl.getPlugin(), delay + ((period * 3) / 6), period);

			new BukkitRunnable() {
				private long lastId = 0l;
				@Override
				public void run() {
					lastId = .preload(lastId, batchsize);
					if (lastId < 0) this.cancel();
				}
			}.runTaskTimerAsynchronously(CropControl.getPlugin(), delay + ((period * 4) / 6), period);
			
			new BukkitRunnable() {
				private long lastId = 0l;
				@Override
				public void run() {
					lastId = .preload(lastId, batchsize);
					if (lastId < 0) this.cancel();
				}
			}.runTaskTimerAsynchronously(CropControl.getPlugin(), delay + ((period * 5) / 6), period);
		} else {
			CropControl.getPlugin().info("Preloading is disabled. Expect more lag.");
		}
		
	}

	/**
	 * Basic method to set up data model v1.
	 */
	private void initializeTables() {
		data.registerMigration(0,  false, "" 
				);
		
	}
	
	/**
	 * Add all new migrations here.
	 */
	private void stageUpdates() {
		
	}
}
