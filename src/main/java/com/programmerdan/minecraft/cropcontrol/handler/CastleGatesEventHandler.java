package com.programmerdan.minecraft.cropcontrol.handler;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.aleksey.castlegates.events.CastleGatesDrawGateEvent;
import com.aleksey.castlegates.events.CastleGatesUndrawGateEvent;
import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlEventHandler.BreakType;
import com.untamedears.realisticbiomes.events.RealisticBiomesBlockGrowEvent;

/**
 * CastleGates voids a bunch of blocks temporarily. This can cause secondary breaks.
 * 
 * These are captured and processed here, calling their counterparts in the
 * ordinary EventHandler.
 * 
 * @author ProgrammerDan
 */
public class CastleGatesEventHandler implements Listener {

	/**
	 * CastleGates has custom block voiding when you "draw" a gate.
	 * 
	 * @param e CastleGates post-action event.
	 */
	@EventHandler
	public void onDrawEvent(CastleGatesDrawGateEvent e) {
		try {
			List<Block> blocks = new ArrayList<Block>();
			for (Location location : e.getImpacted()) {
				blocks.add(location.getBlock());
			}
			CropControl.getPlugin().getEventHandler().doExplodeHandler(blocks, BreakType.PISTON, null);
		} catch (Exception g) {
			CropControl.getPlugin().warning("Failed to handle CastleGates Draw Gate Event:", g);
		}
	}

	/**
	 * CastleGates has custom block voiding when you "undraw" a gate.
	 * 
	 * @param e CastleGates post-action event.
	 */
	@EventHandler
	public void onDrawEvent(CastleGatesUndrawGateEvent e) {
		try {
			List<Block> blocks = new ArrayList<Block>();
			for (Location location : e.getImpacted()) {
				blocks.add(location.getBlock());
			}
			CropControl.getPlugin().getEventHandler().doExplodeHandler(blocks, BreakType.PISTON, null);
		} catch (Exception g) {
			CropControl.getPlugin().warning("Failed to handle CastleGates Undraw Gate Event:", g);
		}
	}
}
