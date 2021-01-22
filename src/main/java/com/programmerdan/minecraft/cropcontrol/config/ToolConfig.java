package com.programmerdan.minecraft.cropcontrol.config;

import com.programmerdan.minecraft.cropcontrol.CropControl;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 wood_pickaxe:
  template:
   ==: org.bukkit.inventory.ItemStack
   type: WOOD_PICKAXE
   amount: 1
  ignore:
   amount: true
   durability: true
   enchants: true
   otherEnchants: true
   enchantsLvl: true
   lore: true
   name: true
 *
 * special case for anything: (catchall)
 *  "ignore" section is ignored but "modifiers" are observed
 *  value after "all" is ignored, just something needs to be there.
 *
 name:
  ignore:
   all: true
 *
 * @author ProgrammerDan
 *
 */
public class ToolConfig {
	private ItemStack template;
	private boolean ignoreAmount;
	private boolean ignoreEnchants;
	private boolean ignoreOtherEnchants;
	private boolean ignoreEnchantsLvl;
	private boolean ignoreLore;
	private boolean ignoreName;
	private boolean ignoreMeta;

	private static ConcurrentHashMap<String, ToolConfig> tools = new ConcurrentHashMap<>();
	
	protected ToolConfig(ItemStack template, boolean ignoreAmount,
			boolean ignoreEnchants, boolean ignoreOtherEnchants, boolean ignoreEnchantsLvl, 
			boolean ignoreLore, boolean ignoreName) {
		this.template = template;
		this.ignoreAmount = ignoreAmount;
		this.ignoreEnchants = ignoreEnchants;
		this.ignoreOtherEnchants = ignoreOtherEnchants;
		this.ignoreEnchantsLvl = ignoreEnchantsLvl;
		this.ignoreLore = ignoreLore;
		this.ignoreName = ignoreName;
		this.ignoreMeta = ignoreEnchants && ignoreLore && ignoreName;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(template);
		sb.append(",ignore:");
		if (this.ignoreAmount) {
			sb.append("amount");
		}
		if (this.ignoreMeta) {
			sb.append("meta");
		} else {
			if (this.ignoreEnchants) {
				sb.append("enchants");
			}
			if (this.ignoreOtherEnchants) {
				sb.append("otherench");
			}
			if (this.ignoreEnchantsLvl) {
				sb.append("enchantslvl");
			}
			if (this.ignoreLore) {
				sb.append("lore");
			}
			if (this.ignoreName) {
				sb.append("name");
			}
		}
		
		return sb.toString();
	}
	
	public ItemStack getTemplate() {
		return template;
	}
	
	public boolean ignoreAmount() {
		return ignoreAmount;
	}
	
	public boolean ignoreEnchants() {
		return ignoreEnchants;
	}

	public boolean ignoreOtherEnchants() {
		return ignoreOtherEnchants;
	}
	
	public boolean ignoreEnchantsLvl() {
		return ignoreEnchantsLvl;
	}
	
	public boolean ignoreLore() {
		return ignoreLore;
	}
	
	public boolean ignoreName() {
		return ignoreName;
	}
	
	public boolean ignoreMeta() {
		return ignoreMeta;
	}
	
	public static void clear() {
		tools = new ConcurrentHashMap<String, ToolConfig>();
	}
	
	public static void initTool(ConfigurationSection tool) {
		if (tools == null) {
			clear();
		}
		if (!tool.contains("template")) {
			if (!tool.contains("ignore.all")) {
				return;
			} else {
				CropControl.getPlugin().info("Catchall tool found: {0}", tool.getName());
			}
		}
		
		if (tools.containsKey(tool.getName())){
			CropControl.getPlugin().info("Duplicate definition for tool {0}, old will be replaced", tool.getName());
		}
		
		ItemStack temp = (tool.contains("ignore.all") ? null : (ItemStack) tool.get("template"));
		
		tools.put(tool.getName(),
				new ToolConfig(temp,
						tool.getBoolean("ignore.amount", true),
						tool.getBoolean("ignore.enchants", true),
						tool.getBoolean("ignore.otherEnchants", true),
						tool.getBoolean("ignore.enchantsLvl", true),
						tool.getBoolean("ignore.lore", true),
						tool.getBoolean("ignore.name", true)
					)
				);
		CropControl.getPlugin().debug("Tool {0} defined as: {1}", tool.getName(), tools.get(tool.getName()));
	}

	public static ToolConfig getConfig(String t) {
		return tools.get(t);
	}

	public boolean matches(ItemStack tool) {
		ItemStack compare = this.getTemplate();
		if (compare == null) {
			return true; // this is catchall! matches everything.
		}
		if (compare.getType() != tool.getType()) {
			return false;
		}
		if (!ignoreAmount() && compare.getAmount() != tool.getAmount()) {
			return false;
		}

		// Short circuit of metachecks.
		if (ignoreMeta()) return true;

		// Metachecks.
		ItemMeta compmeta = compare.getItemMeta();
		ItemMeta toolmeta = tool.getItemMeta();
		if (toolmeta == null && toolmeta == compmeta) {
			return true; // equal but no further compare
		}
		
		if (compmeta == null) {
			return false; // toolmeta != null but compmeta == null
		}
		
		// both non-null.
		if (!ignoreName() && !(toolmeta.hasDisplayName() ? 
				toolmeta.getDisplayName().equals(compmeta.getDisplayName()) : !compmeta.hasDisplayName() ) ) {
			return false;
		}
		if (!ignoreLore() &&
				!(toolmeta.hasLore() ? toolmeta.getLore().equals(compmeta.getLore()) : !compmeta.hasLore())) {
			return false;
		}
		
		// Expensive enchantment checks.
		if (!ignoreEnchants()) {
			Map<Enchantment, Integer> compench = compmeta.getEnchants();
			Map<Enchantment, Integer> toolench = toolmeta.getEnchants();

			// check that set of enchants is same (both null or both not null and same) else bail
			if (!ignoreOtherEnchants() && !((compench == null && toolench == null) || 
					(compench != null && toolench != null && compench.keySet().equals(toolench.keySet()) ) ) ) {
				return false; 
			}

			// check that tool has at least the enchantments specified; ignore the rest.
			if (ignoreOtherEnchants() && !(compench == null || 
					(toolench != null && toolench.keySet().containsAll(compench.keySet()) ) ) ) {
				return false; 
			}

			// also check _level_ of enchants
			if (!ignoreEnchantsLvl() && compench != null) { 
				boolean fail = false;
				for(Enchantment ech : compench.keySet()) {
					if (!compench.get(ech).equals(toolench.get(ech))) {
						fail = true;
						break;
					}
				}
				if (fail) {
					return false;
				}
			}
		}
		return true;
	}
}
