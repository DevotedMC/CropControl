package com.programmerdan.minecraft.cropcontrol.handler;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.programmerdan.minecraft.cropcontrol.config.DropConfig;
import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.config.RootConfig;
import com.programmerdan.minecraft.cropcontrol.config.ToolConfig;

import vg.civcraft.mc.civmodcore.command.Command;
import vg.civcraft.mc.civmodcore.command.CommandHandler;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;

public class CropControlCommandHandler extends CommandHandler {

	@Override
	public void registerCommands() {
		this.addCommands( new Generate("ccgen") );
		this.addCommands( new CropControlCommand("cropcontrol") );
	}

	private class Generate extends PlayerCommand {

		public Generate(String name) {
			super(name);
			setIdentifier("ccgen");
			setDescription("Generates all CC drops around calling player");
			setUsage("/ccgen");
			setArguments(0,1);
			setSenderMustBePlayer(true);
		}

		@Override
		public boolean execute(CommandSender sender, String[] args) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Cannot be run as console");
			}
			Player player = (Player) sender;
			double mult = 1;
			try {
				mult = Integer.parseInt(args[0]);
			} catch (Exception e) {mult = 1;}
			final double vmult = mult;
			
			sender.sendMessage("Force loading all configs and generating all drops, this could cause lag");
			
			Bukkit.getScheduler().runTaskAsynchronously(CropControl.getPlugin(), new Runnable() {
				@Override
				public void run() {
					long delay = 0l;
					FileConfiguration config = CropControl.getPlugin().getConfig();
					ConfigurationSection crops = config.getConfigurationSection("crops");
					ConfigurationSection saplings = config.getConfigurationSection("saplings");
					ConfigurationSection trees = config.getConfigurationSection("trees");
					
					List<String> indexMap = new LinkedList<String>();
					
					for (String crop : crops.getKeys(false)) {
						ConfigurationSection cropConfig = crops.getConfigurationSection(crop);
						if (!cropConfig.isConfigurationSection("drops")) {
							for (String subtype : cropConfig.getKeys(false)) {
								indexMap.add("crops." + crop + "." + subtype);
							}
						} else {
							indexMap.add("crops." + crop);
						}
					}
					
					for (String sapling : saplings.getKeys(false)) {
						indexMap.add("saplings." + sapling);
					}
					
					for (String tree : trees.getKeys(false)) {
						indexMap.add("trees." + tree);
					}
					
					for (String index : indexMap) {
						RootConfig rootConfig = RootConfig.from(index);
						if (rootConfig != null) {
							for (String drop : rootConfig.getDropList()) {
								DropConfig dropConfig = DropConfig.byIdent(drop);
								List<ItemStack> drops = dropConfig.getDrops((int) vmult);
								if (drops != null && !drops.isEmpty()) {
									for (ItemStack item : drops) {
										Bukkit.getScheduler().runTaskLater(CropControl.getPlugin(),  new Runnable() {
											@Override
											public void run() {
												sender.sendMessage(String.format("Root: %s, drop: %s, item: %s", index, drop, item.getType()));
												Item dropped = player.getWorld().dropItem(player.getLocation().add(0, 1.0, 0), item);
												dropped.setPickupDelay(20);
											}
										}, delay++);
									}
								}
							}
						}
					}
				}
			});
			
			
			return false;
		}

		@Override
		public List<String> tabComplete(CommandSender arg0, String[] arg1) {
			return null;
		}
	}
	
	private class CropControlCommand extends PlayerCommand {

		public CropControlCommand(String name) {
			super(name);
			setIdentifier("cropcontrol");
			setDescription("Reloads the configuration");
			setUsage("/cropcontrol");
			setArguments(0,0);
			setSenderMustBePlayer(false);
		}

		@Override
		public boolean execute(CommandSender sender, String[] arg1) {
			RootConfig.reload();
			DropConfig.reload();
			ToolConfig.clear();
			sender.sendMessage("Configuration cleared, will progressively reload.");
			
			return true;
		}

		@Override
		public List<String> tabComplete(CommandSender arg0, String[] arg1) {
			return null;
		}
		
	}
}
