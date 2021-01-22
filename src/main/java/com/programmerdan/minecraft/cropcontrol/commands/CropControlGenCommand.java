package com.programmerdan.minecraft.cropcontrol.commands;

import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.config.DropConfig;
import com.programmerdan.minecraft.cropcontrol.config.RootConfig;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.command.CivCommand;
import vg.civcraft.mc.civmodcore.command.StandaloneCommand;

@CivCommand(id = "ccgen")
public class CropControlGenCommand extends StandaloneCommand {

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		double mult = 1;
		try {
			mult = Integer.parseInt(args[0]);
		} catch (Exception e) {
			mult = 1;
		}
		final double vmult = mult;

		sender.sendMessage("Force loading all configs and generating all drops, this could cause lag");

		Bukkit.getScheduler().runTaskAsynchronously(CropControl.getPlugin(), () -> {
			long delay = 0L;
			FileConfiguration config = CropControl.getPlugin().getConfig();
			ConfigurationSection crops = config.getConfigurationSection("crops");
			ConfigurationSection saplings = config.getConfigurationSection("saplings");
			ConfigurationSection trees = config.getConfigurationSection("trees");

			List<String> indexMap = new LinkedList<>();

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
								Bukkit.getScheduler().runTaskLater(CropControl.getPlugin(), () -> {
									sender.sendMessage(
											String.format("Root: %s, drop: %s, item: %s", index, drop, item.getType()));
									Item dropped = player.getWorld().dropItem(player.getLocation().add(0, 1.0, 0),
											item);
									dropped.setPickupDelay(20);
								}, delay++);
							}
						}
					}
				}
			}
		});

		return false;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}

}
