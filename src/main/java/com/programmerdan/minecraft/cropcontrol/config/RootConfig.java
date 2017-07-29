package com.programmerdan.minecraft.cropcontrol.config;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.data.Crop;
import com.programmerdan.minecraft.cropcontrol.data.Sapling;
import com.programmerdan.minecraft.cropcontrol.data.TreeComponent;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlEventHandler.BreakType;

/**
 * Root configuration for a config relative to a type of crop / sapling / tree in terms of drop natures.
 * 
 * @author Programmerdan
 *
 */
public class RootConfig {
	
	private static ConcurrentHashMap<String, RootConfig> rootConfigs = new ConcurrentHashMap<String, RootConfig>();
	private RootConfig() {};
	
	private ConfigurationSection baseSection = null;
	
	private ConcurrentHashMap<String, DropModifiers> baseDrops = null;
	
	private String index;
	
	public static void reload() {
		rootConfigs.clear();
	}
	
	public static RootConfig from(Crop crop) {
		String index = "crops." + crop.getCropType();
		if (crop.getCropState() != null) {
			index += "." + crop.getCropState();
		}
		return RootConfig.from(index);
	}

	public static RootConfig from(Sapling sapling) {
		String index = "saplings." + sapling.getSaplingType();
		return RootConfig.from(index);
	}

	public static RootConfig from(TreeComponent component) {
		String index = "trees." + component.getTreeType();
		return RootConfig.from(index);
	}
	
	public static RootConfig from(String index) {
		RootConfig config = rootConfigs.get(index);
		if (config != null) {
			return config;
		} 
		config = new RootConfig();
		config.index = index;
		config.baseSection = CropControl.getPlugin().getConfig().getConfigurationSection(index);
		if (config.baseSection != null) {
			ConfigurationSection drops = config.baseSection.getConfigurationSection("drops");
			if (drops != null) {
				config.baseDrops = new ConcurrentHashMap<String, DropModifiers>();
				for (String key : drops.getKeys(false)) {
					DropConfig.byIdent(key); // preload it.
					config.baseDrops.put(key, config.new DropModifiers(drops.getConfigurationSection(key)));
				}
			}
		}
		rootConfigs.put(index, config);
		return config;
	}
	
	public String predictDrops(BreakType breakType, UUID placer, UUID breaker, boolean harvestable, Biome biome, ItemStack tool, World world) {
		if (baseDrops == null || baseDrops.size() == 0) {
			return "No drops configured for " + index;
		}
		StringBuffer message = new StringBuffer("Drops configured for " + index + ":\n");
		double cumChance = 0.0d;
		double localChance = 0.0d;
		int localMin = 0;
		int localMax = 0;
		int counted = 0;
		for (String dropIdent: baseDrops.keySet()) {
			// for drop, divine the overall chance of drop.
			DropConfig dropConfig = DropConfig.byIdent(dropIdent);
			DropModifiers dropMod = baseDrops.get(dropIdent);
			
			// if only drop for harvestible, skip this drop. and don't increase local chance
			if (!harvestable && dropMod.requireHarvestable) {
				message.append("Skip ").append(dropIdent).append(" mismatch harvest\n");
				continue;
			}
			
			localChance = dropConfig.getChance() * dropMod.base.chanceMod; // baseline.
			localMin = dropConfig.getMultiplierMin() + dropMod.base.stackAdjust;
			localMax = dropConfig.getMutliplierMax() + dropMod.base.stackExpand;
			
			if (dropMod.biomes.containsKey(biome)) { // biome
				ModifierConfig modifier = dropMod.biomes.get(biome);
				localChance *= modifier.chanceMod;
				localMin += modifier.stackAdjust;
				localMax += modifier.stackExpand;
			}
			
			//per world support
			if (dropMod.worlds.containsKey(world.getName())) { // world
				ModifierConfig modifier = dropMod.worlds.get(world.getName());
				localChance *= modifier.chanceMod;
				localMin += modifier.stackAdjust;
				localMax += modifier.stackExpand;
				
			}
			
			if (dropMod.breaks.containsKey(breakType)) { // break
				ModifierConfig modifier = dropMod.breaks.get(breakType);
				localChance *= modifier.chanceMod;
				localMin += modifier.stackAdjust;
				localMax += modifier.stackExpand;
			}
			
			// TODO: adjust chance based on tool.
			
			if (BreakType.PLAYER.equals(breakType)) { // player (if applies)
				if (placer != null && placer.equals(breaker)) {
					localChance *= dropMod.samePlayer.chanceMod;
					localMin += dropMod.samePlayer.stackAdjust;
					localMax += dropMod.samePlayer.stackExpand;
				} else {
					localChance *= dropMod.differentPlayer.chanceMod;
					localMin += dropMod.differentPlayer.stackAdjust;
					localMax += dropMod.differentPlayer.stackExpand;
				}
			}
			
			if (localMax < localMin) localMax = localMin;
			
			// If no chance of drop or drop size would be * 0, move on.
			if (localChance <= 0) {
				message.append("Skip ").append(dropIdent).append(" no chance\n");
				continue;
			}
			if (localMin <= localMax && localMax <= 0) {
				message.append("Skip ").append(dropIdent).append(" zero drops [").append(localChance).append("]\n");
				cumChance += localChance;
				counted  ++;
				continue;
			}
			
			message.append("Chance ").append(dropIdent).append(" (").append(localMin).append(",").append(localMax).append(") [").append(localChance).append("]\n");

			counted ++;
			cumChance += localChance;
		}
		message.append("Overall ").append(counted).append(" potential, [").append(cumChance).append("] max likelihood");
		return message.toString();
	}
	
	public List<ItemStack> realizeDrops(BreakType breakType, UUID placer, UUID breaker, boolean harvestable, Biome biome, ItemStack tool, World world) {
		LinkedList<ItemStack> outcome = new LinkedList<ItemStack>();
		if (baseDrops == null || baseDrops.size() == 0) {
			return outcome;
		}
		double cumChance = 0.0d;
		double localChance = 0.0d;
		int localMin = 0;
		int localMax = 0;
		int counted = 0;
		double dice = Math.random();
		for (String dropIdent: baseDrops.keySet()) {
			// for drop, divine the overall chance of drop.
			DropConfig dropConfig = DropConfig.byIdent(dropIdent);
			DropModifiers dropMod = baseDrops.get(dropIdent);
			
			// if only drop for harvestible, skip this drop. and don't increase local chance
			if (!harvestable && dropMod.requireHarvestable) {
				continue;
			}
			
			localChance = dropConfig.getChance() * dropMod.base.chanceMod; // baseline.
			localMin = dropConfig.getMultiplierMin() + dropMod.base.stackAdjust;
			localMax = dropConfig.getMutliplierMax() + dropMod.base.stackExpand;
			
			if (dropMod.biomes.containsKey(biome)) { // biome
				ModifierConfig modifier = dropMod.biomes.get(biome);
				localChance *= modifier.chanceMod;
				localMin += modifier.stackAdjust;
				localMax += modifier.stackExpand;
			}
			
			//per world support
			if (dropMod.worlds.containsKey(world.getName())) { // world
				ModifierConfig modifier = dropMod.worlds.get(world.getName());
				localChance *= modifier.chanceMod;
				localMin += modifier.stackAdjust;
				localMax += modifier.stackExpand;

			}
			
			if (dropMod.breaks.containsKey(breakType)) { // break
				ModifierConfig modifier = dropMod.breaks.get(breakType);
				localChance *= modifier.chanceMod;
				localMin += modifier.stackAdjust;
				localMax += modifier.stackExpand;
			}
			
			// Adjust chance based on tool.
			boolean useCatchall = dropMod.toolCatchall != null;;
			if ((tool != null && dropMod.toolsByType.containsKey(tool.getType())) || dropMod.toolCatchall != null) {
				List<String> orderedTools = dropMod.toolsByType.get(tool.getType());
				if (orderedTools != null) {
					for (String nextTool : orderedTools) {
						if (ToolConfig.getConfig(nextTool).matches(tool)) {
							useCatchall = false; // found a match!
							ModifierConfig modifier = dropMod.tools.get(nextTool);
							localChance *= modifier.chanceMod;
							localMin += modifier.stackAdjust;
							localMax += modifier.stackExpand;
							break;
						}
					}
				}
			}
			if (useCatchall) {
				ModifierConfig modifier = dropMod.tools.get(dropMod.toolCatchall);
				localChance *= modifier.chanceMod;
				localMin += modifier.stackAdjust;
				localMax += modifier.stackExpand;
			}
			
			if (BreakType.PLAYER.equals(breakType)) { // player (if applies)
				if (placer != null && placer.equals(breaker)) {
					localChance *= dropMod.samePlayer.chanceMod;
					localMin += dropMod.samePlayer.stackAdjust;
					localMax += dropMod.samePlayer.stackExpand;
				} else {
					localChance *= dropMod.differentPlayer.chanceMod;
					localMin += dropMod.differentPlayer.stackAdjust;
					localMax += dropMod.differentPlayer.stackExpand;
				}
			}
			
			if (localMax < localMin) localMax = localMin;
			
			// If no chance of drop or drop size would be * 0, move on.
			if (localChance <= 0) continue;
			if (localMin <= localMax && localMax <= 0) {
				cumChance += localChance;
				counted  ++;
				continue;
			}
			
			if (dice >= cumChance && dice < cumChance + localChance) {
				int multiplier = (int) (Math.round(Math.random() * ((double) (localMax - localMin))) + localMin);
				//CropControl.getPlugin().debug("Generated a drop for {0} at chance {1} with multiplier {2}", dropIdent, localChance, multiplier);
				outcome.addAll(dropConfig.getDrops(multiplier));
				break;
			}
			counted ++;
			cumChance += localChance;
		}
		CropControl.getPlugin().debug("Evaluated {0} drops with {1} cum total vs {2} rng, generated {3} items to drop", counted, cumChance, dice, outcome.size());
		return outcome;
	}

	class DropModifiers {
		// so for tools we keep a list based on declaration order, and a set to connect to configs.
		String toolCatchall = null;
		Map<String, ModifierConfig> tools = new ConcurrentHashMap<String, ModifierConfig>();
		Map<Material, List<String>> toolsByType = new ConcurrentHashMap<Material, List<String>>();
		
		Map<Biome, ModifierConfig> biomes = new ConcurrentHashMap<Biome, ModifierConfig>();
		
		//per-world support
		Map<String, ModifierConfig> worlds = new ConcurrentHashMap<String, ModifierConfig>();
		
		ModifierConfig base = null;
		
		boolean requireHarvestable = true;
		
		Map<BreakType, ModifierConfig> breaks = new ConcurrentHashMap<BreakType, ModifierConfig>();
		
		ModifierConfig differentPlayer = null;
		ModifierConfig samePlayer = null;
		
		DropModifiers(ConfigurationSection config) {
			base = new ModifierConfig(config);
			// TODO: inheritance so you can set it on the root and it applies here.
			differentPlayer = new ModifierConfig(config.getConfigurationSection("player.different"));
			samePlayer = new ModifierConfig(config.getConfigurationSection("player.same"));
			
			requireHarvestable = config.getBoolean("harvestableOnly", requireHarvestable);
			
			ConfigurationSection biomeConfigs = config.getConfigurationSection("biomes");
			// unwraps biomes and divines modifier configs for each one listed.
			if (biomeConfigs != null) {
				for (String biome : biomeConfigs.getKeys(false)) {
					try {
						Biome match = Biome.valueOf(biome);
						if (match != null) {
							biomes.put(match, new ModifierConfig(biomeConfigs.getConfigurationSection(biome)));
						} else {
							CropControl.getPlugin().warning("Unrecognized biome {0} in config at {1}", biome, config.getCurrentPath());
						}
					} catch (Exception e) {
						CropControl.getPlugin().warning("Invalid biome {0} in config at {1}", biome, config.getCurrentPath());
					}
				}
			} // else no unique settings per biome.
			
			ConfigurationSection worldConfigs = config.getConfigurationSection("worlds");
			// unwraps worlds and divines modifier configs for each one listed.
			if (worldConfigs != null) {
				for (String worldName : worldConfigs.getKeys(false)) {
					try {
						//See if the world is included in the server
						World match = Bukkit.getServer().getWorld(worldName);
						if(match != null){
							worlds.put(match.getName(), new ModifierConfig(worldConfigs.getConfigurationSection(worldName)));
						}
					} catch (Exception e) {
						CropControl.getPlugin().warning("Invalid world {0} in config at {1}", worldName, config.getCurrentPath());
					}
				}
			} // else no unique settings per world.
			
			
			ConfigurationSection breakConfigs = config.getConfigurationSection("breaktypes");
			// unwraps break types and divines modifier configs for each one listed.
			if (breakConfigs != null) {
				for (String breakKey : breakConfigs.getKeys(false)) {
					try {
						BreakType match = BreakType.valueOf(breakKey);
						if (match != null) {
							breaks.put(match, new ModifierConfig(breakConfigs.getConfigurationSection(breakKey)));
						} else {
							CropControl.getPlugin().warning("Unrecognized break type {0} in config at {1}", breakKey, config.getCurrentPath());
						}
					} catch (Exception e) {
						CropControl.getPlugin().warning("Invalid break type {0} in config at {1}", breakKey, config.getCurrentPath());
					}
				}
			} // else no unique settings per break type.
			
			// optionally set a tool ordering to define resolution precedence. Any tools not listed in toolOrder
			// will be added to the end of the ordering list.
			List<String> toolOrder = config.getStringList("toolOrder");
			if (toolOrder == null) {
				toolOrder = new ArrayList<String>();
			}
 
			ConfigurationSection toolConfigs = config.getConfigurationSection("tools");
		
			// wraps tools by name and divines modifier configs for each one list. We handle matching elsewhere.
			if (toolConfigs != null) {
				Set<String> toolsLeft = toolConfigs.getKeys(false);
				toolsLeft.removeAll(toolOrder);
				
				toolOrder.addAll(toolsLeft);

				for (String toolKey : toolOrder) {
					try {
						ToolConfig tool = ToolConfig.getConfig(toolKey);
						ConfigurationSection toolConfig = toolConfigs.getConfigurationSection(toolKey);
						if (tool == null) {
							CropControl.getPlugin().warning("Unrecognized tool {0} in config at {1}, be sure to add in global tools declaration before use!", 
									toolKey, config.getCurrentPath());
						} else if (toolConfig == null) {
							CropControl.getPlugin().warning("Valid tool {0} specified in tools or toolOrder at {1} but no modifiers specified in tools!",
									toolKey, config.getCurrentPath());
						} else {
							// add to master list for this drop.
							tools.put(toolKey, new ModifierConfig(toolConfig));
							ItemStack toolItem = tool.getTemplate();
							if (toolItem == null) { // "catchall" type
								toolCatchall = toolKey; // can be only one, use last
							} else {
								Material toolMat = toolItem.getType();
								List<String> typeList = toolsByType.get(toolMat);
								if (typeList == null) {
									typeList = new CopyOnWriteArrayList<>();
								}
								typeList.add(toolKey);
								toolsByType.put(toolMat, typeList);
							}
						}
					} catch (Exception  e) {
						CropControl.getPlugin().warning("Invalid tool type {0} in config at {1}", toolKey, config.getCurrentPath());
					}
				}
			}
		}
	}
	
	class ModifierConfig {
		double chanceMod = 1.0d;
		int stackExpand = 0;
		int stackAdjust = 0;
		
		public ModifierConfig(ConfigurationSection config) {
			if (config == null) return; // leave as no-op default.
			chanceMod = config.getDouble("chance", chanceMod);
			stackExpand = config.getInt("expand", stackExpand);
			stackAdjust = config.getInt("adjust", stackAdjust);
		}
	}
}
