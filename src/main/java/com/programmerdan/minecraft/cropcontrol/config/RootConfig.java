package com.programmerdan.minecraft.cropcontrol.config;

import java.util.ArrayList;
import java.util.List;
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
	
	private ConcurrentSkipListSet<DropConfig> baseDrops = null;
	
	public static void reload() {
		rootConfigs.clear();
	}
	
	public static RootConfig from(Crop crop) {
		String index = "crops." + crop.getCropType() + "." + crop.getCropState();
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
			baseDrops = new ConcurrentSkipListSet<DropConfig>();
			for (String key : config.baseSection.getKeys(false)) {
				baseDrops.add(DropConfig.byIdent)
			}
		}
		rootConfigs.put(index, config);
		return config;
	}
	
	public List<ItemStack> realizeDrops(BreakType breakType, UUID placer, UUID breaker, boolean harvestable, Biome biome, ItemStack tool) {
		ArrayList<DropConfig> potentialDrops = new ArrayList<DropConfig>(baseDrops.size());
		
	}

}
