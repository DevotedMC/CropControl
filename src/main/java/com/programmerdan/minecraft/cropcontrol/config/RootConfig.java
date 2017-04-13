package com.programmerdan.minecraft.cropcontrol.config;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.data.Crop;
import com.programmerdan.minecraft.cropcontrol.data.Sapling;
import com.programmerdan.minecraft.cropcontrol.data.TreeComponent;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlEventHandler.BreakType;

public class RootConfig {
	
	private static ConcurrentHashMap<String, RootConfig> rootConfigs = new ConcurrentHashMap<String, RootConfig>();
	private RootConfig() {};
	
	private ConfigurationSection baseSection = null;
	
	private ConcurrentHashMap<String, DropModifiers> baseDrops = null;
	
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
	
	public List<ItemStack> realizeDrops(BreakType breakType, UUID placer, UUID breaker, boolean harvestable, Biome biome, ItemStack tool) {
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
			if (localChance <= 0) continue;
			if (localMin <= localMax && localMax <= 0) {
				cumChance += localChance;
				counted  ++;
				continue;
			}
			
			if (dice >= cumChance && dice < cumChance + localChance) {
				int multiplier = (int) (Math.round(Math.random() * ((double) (localMin + localMax))) - localMin);
				CropControl.getPlugin().debug("Generated a drop for {0} at chance {1} with multiplier {2}", dropIdent, localChance, multiplier);
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
		// tool todo -- organize internally by base tool type? if that'll work, otherwise w/e
		// private Map<String, List<ToolConfig>> tool configs
		
		Map<Biome, ModifierConfig> biomes = new ConcurrentHashMap<Biome, ModifierConfig>();
		
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
			
			// TODO tool 'strap here
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
