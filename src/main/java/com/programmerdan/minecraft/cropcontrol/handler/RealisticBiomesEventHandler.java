package com.programmerdan.minecraft.cropcontrol.handler;

import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.untamedears.realisticbiomes.events.RealisticBiomesBlockBreakEvent;
import com.untamedears.realisticbiomes.events.RealisticBiomesBlockGrowEvent;
import com.untamedears.realisticbiomes.events.RealisticBiomesStructureGrowEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Realistic Biomes cancels all the normal bukkit events; I've layered in shadow events called after RB's manipulations
 * that mirror the underlying Bukkit events. These are captured and processed here, calling their counterparts in the
 * ordinary EventHandler.
 * 
 * @author ProgrammerDan
 */
public class RealisticBiomesEventHandler implements Listener {
	
	/**
	 * RB handles the growth of crops, canceling the vanilla grow and replacing it with its own.
	 * @param e RB wrapper for BlockGrowEvent
	 */
	@EventHandler
	public void onCropGrow(RealisticBiomesBlockGrowEvent e) {
		//CropControl.getPlugin().debug("Captured RB Grow Event Wrapper");
		try {
			CropControl.getPlugin().getEventHandler().onCropGrow( e.getEvent() );
		} catch (Exception g) {
			CropControl.getPlugin().warning("Failed to handle RB Grow Event:", g);
		}
	}
	
	/* Doesn't override Spread */
	
	/**
	 * RB handles the growth of trees, canceling the vanilla grow and replacing it with its own grow.
	 * 
	 * @param e RB wrapper for StructureGrowEvent
	 */
	@EventHandler
	public void onTreeGrow(RealisticBiomesStructureGrowEvent e) {
		//CropControl.getPlugin().debug("Captured RB Structure Grow Event Wrapper");
		try {
			CropControl.getPlugin().getEventHandler().onTreeGrow( e.getEvent() );
		} catch (Exception g) {
			CropControl.getPlugin().warning("Failed to handle RB Tree Grow Event:", g);
		}

	}
	
	/* All other interaction events perform modifications to the backing world but should otherwise be fine
	 * as long as our handlers run after RB's, which on HIGHEST they do.
	 */
	
	/**
	 * In some situations RB forces a grow only to immediately break it (cactus, etc) so this will allow proper
	 * propagation of those events.
	 * 
	 * @param e RB wrapper for BlockBreakEvent (situational)
	 */
	@EventHandler
	public void onBlockBreak(RealisticBiomesBlockBreakEvent e) {
		//CropControl.getPlugin().debug("Captured RB Block Break Event Wrapper - defer");
		try {
			Bukkit.getScheduler().runTaskLater( CropControl.getPlugin(), new Runnable() {
				public void run() {
					CropControl.getPlugin().getEventHandler().onBlockBreak( e.getEvent() );
				}
			}, 1L);
		} catch (Exception g) {
			CropControl.getPlugin().warning("Failed to handle RB Break Event:", g);
		}
	}
}
