package com.programmerdan.minecraft.cropcontrol.events;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.programmerdan.minecraft.cropcontrol.data.Locatable;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlEventHandler.BreakType;

/**
 * This event is called whenever CropControl is about to drop an item related to a
 * configured plant. 
 * 
 * @author ProgrammerDan
 */
public class CropControlDropEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	private final Location location;
	private final BreakType breakType;
	private final Locatable dropable;
	private final UUID player;
	private List<ItemStack> items;
	private List<String> commands;
	
	/**
	 * Constructor for a drop event.
	 * 
	 * @param location Immutable location where the drop will occur unless cancelled
	 * @param breakType The type of break, also immutable
	 * @param dropable What is breaking, also immutable
	 * @param player The UUID of the player responsible, if any
	 * @param items A replaceable list of items. The list after all handlers will be dropped.
	 */
	public CropControlDropEvent(final Location location, final BreakType breakType, 
			final Locatable dropable, final UUID player, List<ItemStack> items, List<String> commands) {
		this.location = location;
		this.breakType = breakType;
		this.dropable = dropable;
		this.player = player;
		this.items = items;
		this.commands = commands;
	}

	@Override
	public HandlerList getHandlers() {
		return CropControlDropEvent.handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	private boolean cancel = false;

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancel = cancelled;
	}

	public Location getLocation() {
		return this.location;
	}
	
	public BreakType getBreakType() {
		return this.breakType;
	}
	
	public Locatable getBroken() {
		return this.dropable;
	}
	
	public UUID getPlayer() {
		return this.player;
	}
	
	public List<ItemStack> getItems() {
		return this.items;
	}
	
	public void setItems(List<ItemStack> items) {
		this.items = items;
	}
	
	public List<String> getCommands() {
		return this.commands;
	}
	
	public void setCommands(List<String> commands) {
		this.commands = commands;
	}
}
