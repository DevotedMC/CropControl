package com.programmerdan.minecraft.cropcontrol.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;
import org.bukkit.util.Vector;

import com.google.common.collect.Sets;
import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.config.RootConfig;
import com.programmerdan.minecraft.cropcontrol.data.Crop;
import com.programmerdan.minecraft.cropcontrol.data.Locatable;
import com.programmerdan.minecraft.cropcontrol.data.Sapling;
import com.programmerdan.minecraft.cropcontrol.data.Tree;
import com.programmerdan.minecraft.cropcontrol.data.TreeComponent;
import com.programmerdan.minecraft.cropcontrol.data.WorldChunk;
import com.programmerdan.minecraft.cropcontrol.events.CropControlDropEvent;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

/**
 * Monitor for all growth, placement, spread, and break events and such.
 * Some edge cases might be missing, will grow to account for them.
 * 
 * @author xFier
 * @author <a href="mailto:programmerdan@gmail.com">ProgrammerDan</a>
 *
 */
public class CropControlEventHandler implements Listener {
	private FileConfiguration config;
	
	private String baseDropMessage;
	/**
	 * List of materials that are crops, and if we track specific states
	 * belonging to that material.
	 */
	private Map<Material, Boolean> harvestableCrops;
	
	private Set<Material> trackedMaterials;
	
	private Set<Location> pendingChecks;
	
	public static final BlockFace[] directions = new BlockFace[] { 
			BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	
	public static final BlockFace[] traverse = new BlockFace[] {
			BlockFace.DOWN, BlockFace.SELF, BlockFace.UP
	};

	public static final BlockFace[] growDirections = new BlockFace[] { 
			BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP };
	
	public CropControlEventHandler(FileConfiguration config) {
		this.config = config;
		
		baseDropMessage = (this.config.getBoolean("alert.enable", false) ? this.config.getString("alert.message") : null);

		harvestableCrops = new HashMap<Material, Boolean>();
		trackedMaterials = new HashSet<Material>();
		pendingChecks = Sets.newConcurrentHashSet();

		fillHarvestableCropsList();

		CropControlDatabaseHandler.getInstance().preloadExistingChunks();
	}

	public void fillHarvestableCropsList() {
		harvestableCrops.put(Material.CROPS, true);
		harvestableCrops.put(Material.CARROT, true);
		harvestableCrops.put(Material.POTATO, true);
		harvestableCrops.put(Material.NETHER_WARTS, true);
		harvestableCrops.put(Material.BEETROOT_BLOCK, true);
		harvestableCrops.put(Material.COCOA, true);
		harvestableCrops.put(Material.PUMPKIN_STEM, false);
		harvestableCrops.put(Material.MELON_STEM, false);
		harvestableCrops.put(Material.CACTUS, false);
		harvestableCrops.put(Material.BROWN_MUSHROOM, false);
		harvestableCrops.put(Material.RED_MUSHROOM, false);
		harvestableCrops.put(Material.SUGAR_CANE_BLOCK, false);
		
		trackedMaterials.add(Material.CROPS);
		trackedMaterials.add(Material.CARROT);
		trackedMaterials.add(Material.POTATO);
		trackedMaterials.add(Material.NETHER_WARTS);
		trackedMaterials.add(Material.BEETROOT_BLOCK);
		trackedMaterials.add(Material.COCOA);
		trackedMaterials.add(Material.PUMPKIN_STEM);
		trackedMaterials.add(Material.PUMPKIN);
		trackedMaterials.add(Material.MELON_STEM);
		trackedMaterials.add(Material.MELON_BLOCK);
		trackedMaterials.add(Material.CACTUS);
		trackedMaterials.add(Material.SUGAR_CANE_BLOCK);
		trackedMaterials.add(Material.BROWN_MUSHROOM);
		trackedMaterials.add(Material.RED_MUSHROOM);
		trackedMaterials.add(Material.SAPLING);
		trackedMaterials.add(Material.CHORUS_PLANT);
		trackedMaterials.add(Material.CHORUS_FLOWER);
		trackedMaterials.add(Material.HUGE_MUSHROOM_1);
		trackedMaterials.add(Material.HUGE_MUSHROOM_2);
		trackedMaterials.add(Material.LOG);
		trackedMaterials.add(Material.LOG_2);
		trackedMaterials.add(Material.LEAVES);
		trackedMaterials.add(Material.LEAVES_2);
	}

	/*
	 * 
	 * Start of getters
	 * 
	 */

	public String getBaseCropState(Material material) {
		switch (material) {
		case COCOA:
			return "SMALL";
		case MELON_STEM:
			return "0";
		case PUMPKIN_STEM:
			return "0";
		case CACTUS:
			return null;
		case BROWN_MUSHROOM:
			return null;
		case RED_MUSHROOM:
			return null;
		case SUGAR_CANE_BLOCK:
			return null;
		default:
			return "SEEDED";
		}
	}

	public String getCropState(BlockState blockState) {
		switch (blockState.getType()) { // .getBlock().getType()) {
		case COCOA:
			return ((CocoaPlant) blockState.getData()).getSize().toString();
		case NETHER_WARTS:
			return ((NetherWarts) blockState.getData()).getState().toString();
		case MELON_STEM:
			return (int) blockState.getBlock().getData() + "";
		case PUMPKIN_STEM:
			return (int) blockState.getBlock().getData() + "";
		case CACTUS:
			return null;
		case BROWN_MUSHROOM:
			return null;
		case RED_MUSHROOM:
			return null;
		case SUGAR_CANE_BLOCK:
			return null;
		default:
			//CropControl.getPlugin().debug("Unable to find CropState match for {0}", blockState);
			return ((Crops) blockState.getData()).getState().toString();
		}
	}

	public String getSaplingType(Byte data) {
		switch (data) {
		case 0:
			return "OAK_SAPLING";
		case 1:
			return "SPRUCE_SAPLING";
		case 2:
			return "BIRCH_SAPLING";
		case 3:
			return "JUNGLE_SAPLING";
		case 4:
			return "ACACIA_SAPLING";
		case 5:
			return "DARK_OAK_SAPLING";
		default:
			CropControl.getPlugin().debug("Unknown sapling type {0}", data);
			return null;
		}
	}

	public Material getTrackedTypeMaterial(String trackedType) {
		for (Material material : harvestableCrops.keySet()) {
			if (material.toString().equals(trackedType))
				return material;
		}

		if (Material.MELON_BLOCK.toString().equals(trackedType))
			return Material.MELON_BLOCK;
		else if (Material.PUMPKIN.toString().equals(trackedType))
			return Material.PUMPKIN;

		for (Byte i = 0; i < 6; i++) {
			if (getSaplingType(i).equals(trackedType)) // TODO: odd structure here
				return Material.SAPLING;
		}

		for (TreeType treeType : TreeType.values()) {
			if (treeType.toString().equals(trackedType)) {
				if (treeType == TreeType.ACACIA || treeType == TreeType.DARK_OAK)
					return Material.LOG_2;
				else if (treeType == TreeType.BROWN_MUSHROOM)
					return Material.HUGE_MUSHROOM_1;
				else if (treeType == TreeType.RED_MUSHROOM)
					return Material.HUGE_MUSHROOM_2;
				else
					return Material.LOG;
			}
		}

		if (Material.CHORUS_PLANT.toString().equals(trackedType))
			return Material.CHORUS_PLANT;

		CropControl.getPlugin().debug("Unable to match tracked type material {0}", trackedType);
		return null;
	}

	public Material getTrackedCropMaterial(String _trackedType) {
		Material trackedType = Material.getMaterial(_trackedType);
		if (Material.MELON_BLOCK.equals(trackedType))
			return Material.MELON_BLOCK;
		else if (Material.PUMPKIN.equals(trackedType))
			return Material.PUMPKIN;
		else {
			for (Material material : harvestableCrops.keySet()) {
				if (material.equals(trackedType))
					return material;
			}
		}
		CropControl.getPlugin().debug("Unable to match tracked crop type material {0}", trackedType);
		return null;
	}

	public Material getTrackedSaplingMaterial(String trackedType) {
		for (Byte i = 0; i < 6; i++) {
			if (getSaplingType(i).equals(trackedType))
				return Material.SAPLING;
		}
		CropControl.getPlugin().debug("Unable to match tracked sapling type material {0}", trackedType);
		return null;
	}

	public Material getTrackedTreeMaterial(String trackedType) {
		if (Material.CHORUS_PLANT.toString().equals(trackedType))
			return Material.CHORUS_PLANT;
		else {
			for (TreeType treeType : TreeType.values()) {
				if (treeType.toString().equals(trackedType)) {
					if (treeType == TreeType.ACACIA || treeType == TreeType.DARK_OAK)
						return Material.LOG_2;
					else if (treeType == TreeType.BROWN_MUSHROOM)
						return Material.HUGE_MUSHROOM_1;
					else if (treeType == TreeType.RED_MUSHROOM)
						return Material.HUGE_MUSHROOM_2;
					else
						return Material.LOG;
				}
			}
		}
		CropControl.getPlugin().debug("Unable to match tracked tree type material {0}", trackedType);

		return null;
	}
	
	public boolean maybeTracked(Material type) {
		return trackedMaterials.contains(type);
	}

	@EventHandler(ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent e) {
		if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		Player p = e.getPlayer();
		
		if (!p.hasPermission("cropcontrol.debug") ) return;

		Block block = e.getClickedBlock();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();

		WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
		
		if (!e.getPlayer().isSneaking()) {
			if (chunk == null) {
				ComponentBuilder hoverBuilder = new ComponentBuilder("Not being tracked by CropControl").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Chunk Missing!").color(ChatColor.DARK_AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
				return;
			}

			Crop crop = chunk.getCrop(x, y, z);
			Sapling sapling = chunk.getSapling(x, y, z);
			TreeComponent component = chunk.getTreeComponent(x, y, z);
			
			if (crop == null && sapling == null && component == null) return;
			
			p.sendMessage(ChatColor.GREEN + "Fier's fancy debug system:");

			ComponentBuilder hoverBuilder1 = new ComponentBuilder("ChunkID: " + chunk.getChunkID())
					.color(ChatColor.RED).append("\nChunkX: " + chunk.getChunkX()).color(ChatColor.RED)
					.append("\nChunkZ: " + chunk.getChunkZ()).color(ChatColor.RED);

			BaseComponent[] hoverMessage1 = hoverBuilder1.create();

			ComponentBuilder message1 = new ComponentBuilder("Chunks").color(ChatColor.AQUA)
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage1));

			p.spigot().sendMessage(message1.create());

			if (crop != null) {
				ComponentBuilder hoverBuilder = new ComponentBuilder("CropID: " + crop.getCropID()).color(ChatColor.RED)
						.append("\nChunkID: " + crop.getChunkID()).color(ChatColor.RED).append("\nX: " + crop.getX())
						.color(ChatColor.RED).append("\nY: " + crop.getY()).color(ChatColor.RED)
						.append("\nZ: " + crop.getZ()).color(ChatColor.RED).append("\nCropType: " + crop.getCropType())
						.color(ChatColor.RED).append("\nCropState: " + crop.getCropState()).color(ChatColor.RED)
						.append("\nPlacer: " + crop.getPlacer()).color(ChatColor.RED)
						.append("\nTimeStamp: " + crop.getTimeStamp()).color(ChatColor.RED)
						.append("\nHarvestable: " + crop.getHarvestable()).color(ChatColor.RED)
						.append("\nNatural Drops: " + 
								RootConfig.from(crop).predictDrops(BreakType.NATURAL, crop.getPlacer(), null, crop.getHarvestable(), block.getBiome(), null));

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Crops").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			} else {
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Crops").color(ChatColor.DARK_AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			if (sapling != null) {
				ComponentBuilder hoverBuilder = new ComponentBuilder("SaplingID: " + sapling.getSaplingID())
						.color(ChatColor.RED).append("\nChunkID: " + sapling.getChunkID()).color(ChatColor.RED)
						.append("\nX: " + sapling.getX()).color(ChatColor.RED).append("\nY: " + sapling.getY())
						.color(ChatColor.RED).append("\nZ: " + sapling.getZ()).color(ChatColor.RED)
						.append("\nSaplingType: " + sapling.getSaplingType()).color(ChatColor.RED)
						.append("\nPlacer: " + sapling.getPlacer()).color(ChatColor.RED)
						.append("\nTimeStamp: " + sapling.getTimeStamp()).color(ChatColor.RED)
						.append("\nHarvestable: " + sapling.getHarvestable()).color(ChatColor.RED)
						.append("\nNatural Drops: " + 
								RootConfig.from(sapling).predictDrops(BreakType.NATURAL, sapling.getPlacer(), null, sapling.getHarvestable(), block.getBiome(), null));

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Saplings").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			} else {
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Saplings").color(ChatColor.DARK_AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			if (component != null) {
				Tree tree = CropControl.getDAO().getTree(component);
				ComponentBuilder hoverBuilder = new ComponentBuilder("TreeID: " + tree.getTreeID()).color(ChatColor.RED)
						.append("\nChunkID: " + tree.getChunkID()).color(ChatColor.RED).append("\nX: " + tree.getX())
						.color(ChatColor.RED).append("\nY: " + tree.getY()).color(ChatColor.RED)
						.append("\nZ: " + tree.getZ()).color(ChatColor.RED).append("\nTreeType: " + tree.getTreeType())
						.color(ChatColor.RED).append("\nPlacer: " + tree.getPlacer()).color(ChatColor.RED)
						.append("\nTimeStamp: " + tree.getTimeStamp()).color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			} else {
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree").color(ChatColor.DARK_AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			if (component != null) {
				TreeComponent treeComponent = component;

				ComponentBuilder hoverBuilder = new ComponentBuilder(
						"TreeComponentID: " + treeComponent.getTreeComponentID()).color(ChatColor.RED)
								.append("\nChunkID: " + treeComponent.getChunkID())
								.append("\nX: " + treeComponent.getX()).append("\nY: " + treeComponent.getY())
								.append("\nZ: " + treeComponent.getZ())
								.append("\nTreeType: " + treeComponent.getTreeType())
								.append("\nPlacer: " + treeComponent.getPlacer())
								.append("\nHarvestable: " + treeComponent.isHarvestable())
								.append("\nNatural Drops: " + 
										RootConfig.from(treeComponent).predictDrops(BreakType.NATURAL, treeComponent.getPlacer(), null, treeComponent.isHarvestable(), block.getBiome(), null));


				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree Component").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			} else {
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree Component").color(ChatColor.DARK_AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}
		} else {
			TreeComponent component = chunk.getTreeComponent(x, y, z);
			if (component != null) {
				p.sendMessage(ChatColor.GREEN + "Fier's fancy debug system:");

				Tree tree = CropControl.getDAO().getTree(component.getTreeID());

				for (TreeComponent treeComponent : CropControl.getDAO().getTreeComponents(tree)) {
					p.sendMessage(ChatColor.GREEN + "TreeComponentID: " + treeComponent.getTreeComponentID()
							+ " ChunkID: " + treeComponent.getChunkID() + " X: " + treeComponent.getX() + " Y: "
							+ treeComponent.getY() + " Z: " + treeComponent.getZ() + " TreeType: "
							+ treeComponent.getTreeType() + " Placer: " + treeComponent.getPlacer() + " Harvestable: "
							+ treeComponent.isHarvestable());
				}
			}
		}

	}

	/*
	 * 
	 * End of Getters
	 * 
	 * Start of Block Placement Tracking
	 * 
	 */

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlaceBlock(BlockPlaceEvent e) {
		Block block = e.getBlock();

		Material blockMaterial = block.getType();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());

		if (CropControl.getDAO().isTracked(block) == true) {
			// We've got a replacement
			CropControl.getPlugin().debug("Ghost object? Placement is overtop a tracked object at {0}, {1}, {2}", x, y, z);
			handleRemoval(block, chunk);
		}
		if (harvestableCrops.containsKey(blockMaterial)) {
			// we placed a block overtop an existing crop. Will be handled by a break event?
			/*Crop crop = chunk.getCrop(x, y, z);
			if (crop != null) {
				crop.setRemoved();
				CropControl.getPlugin().debug("Missed an event? Replacing a Crop at {0}, {1}, {2}", x, y, z);
				//return;
			}*/
			
			// We've placed a crop!
			Crop.create(chunk, x, y, z, blockMaterial.toString(), getBaseCropState(blockMaterial),
					e.getPlayer().getUniqueId(), System.currentTimeMillis(), harvestableCrops.get(blockMaterial));
		} else if (blockMaterial == Material.SAPLING) {
			// we placed a block overtop an existing sapling. TODO: Do I need to remove sapling here, or will there be a break event?
			/*Sapling sapling = chunk.getSapling(x, y, z);
			if (sapling != null) {
				sapling.setRemoved();
				CropControl.getPlugin().debug("Missed an event? Replacing a Sapling at {0}, {1}, {2}", x, y, z);
				//return;
			}*/
			// We've placed a sapling!
			Sapling.create(chunk, x, y, z, getSaplingType(block.getData()),
					e.getPlayer().getUniqueId(), System.currentTimeMillis(), false);
		} else if (blockMaterial == Material.CHORUS_FLOWER) {
			/*if (CropControl.getDAO().isTracked(block) == true) {
				CropControl.getPlugin().debug("Ghost object? Placement is overtop a tracked object at {0}, {1}, {2}", x, y, z);
				//return;
			}*/
			
			// TODO: Check if connected to an existing chorus tree.

			// First register the "tree"
			Tree chorusPlant = Tree.create(chunk, x, y, z, Material.CHORUS_PLANT.toString(),
					e.getPlayer().getUniqueId(), System.currentTimeMillis());

			// Then the component in the tree.
			TreeComponent.create(chorusPlant, chunk, x, y, z, Material.CHORUS_PLANT.toString(),
					e.getPlayer().getUniqueId(), false);
		} else if (blockMaterial.isSolid()){ // check for cactus.
			for (BlockFace face : CropControlEventHandler.directions) {
				Block adj = block.getRelative(face);
				if (Material.CACTUS.equals(adj.getType())) {
					Location loc = adj.getLocation();
					if (!pendingChecks.contains(loc)) {
						pendingChecks.add(loc);
						handleBreak(adj, BreakType.PLAYER, e.getPlayer().getUniqueId(), null);
					}		
				}
			}
		}
	}
	
	/**
	 * Generically handle a blanket, no-drop removal of tracking data. This does not attempt to cover edge cases, just removes things and prays.
	 * 
	 * @param block The block of interest
	 * @param chunk The chunk the block lives in.
	 */
	private void handleRemoval(final Block block, WorldChunk chunk) {
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		Crop crop = chunk.getCrop(x, y, z);
		Sapling sapling = chunk.getSapling(x, y, z);
		TreeComponent treeComponent = chunk.getTreeComponent(x, y, z);
		if (crop != null) {
			crop.setRemoved();
		}
		if (sapling != null) {
			sapling.setRemoved();
		}
		if (treeComponent != null) {
			Tree tree = CropControl.getDAO().getTree(treeComponent);
			
			treeComponent.setRemoved();
			
			if (CropControl.getDAO().getTreeComponents(treeComponent.getTreeID()).isEmpty()) {
				tree.setRemoved();
			}
		}
	}

	/**
	 * Implementation note: For columnar plants, I do a second check one-below if the block
	 * immediately under the "grown" block isn't a known crop. This is due to our RB integration
	 * and as defense against any future implementations; as the <i>handling</i> of the event
	 * is async, there is some risk that grow events are processed out of order when received
	 * en masse, like RB does.
	 * 
	 * @param e The grow event.
	 */
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCropGrow(BlockGrowEvent e) {
		Block block = e.getNewState().getBlock();

		Bukkit.getServer().getScheduler().runTaskAsynchronously(CropControl.getPlugin(), new Runnable() {
			@Override
			public void run() {
				WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
				int x = block.getX();
				int y = block.getY();
				int z = block.getZ();
				Crop crop = chunk.getCrop(x, y, z);
				if (crop != null) {
					crop.setCropState(getCropState(e.getNewState()));
				} else {
					if (block.getType() == Material.MELON_BLOCK || block.getType() == Material.PUMPKIN) {
						for (BlockFace blockFace : CropControlEventHandler.directions) {
							Block otherBlock = block.getRelative(blockFace);
							WorldChunk otherChunk = CropControl.getDAO().getChunk(otherBlock.getChunk());
							int otherX = otherBlock.getX();
							int otherY = otherBlock.getY();
							int otherZ = otherBlock.getZ();
							Crop otherCrop = otherChunk.getCrop(otherX, otherY, otherZ);
							if (otherCrop != null) {
								UUID placerUUID = otherCrop.getPlacer();
								
								Crop.create(chunk, x,y,z,  block.getType().toString(), null,
										placerUUID, System.currentTimeMillis(), true);
								break;
							}
						}
					} else if (block.getType() == Material.CACTUS || block.getType() == Material.SUGAR_CANE_BLOCK) {
						Block otherBlock = block.getRelative(BlockFace.DOWN);
						int otherX = otherBlock.getX();
						int otherY = otherBlock.getY();
						int otherZ = otherBlock.getZ();
						Crop otherCrop = chunk.getCrop(otherX, otherY, otherZ);
						if (otherCrop != null) {
							UUID placerUUID = otherCrop.getPlacer();
							
							Crop.create(chunk, x,y,z,  block.getType().toString(), null,
									placerUUID, System.currentTimeMillis(), true);
						} else { // go one lower, might have been async out of order ..
							Block finalCheck = otherBlock.getRelative(BlockFace.DOWN);
							if (block.getType().equals(finalCheck.getType())) { // same ballpark
								Crop finalCrop = chunk.getCrop(finalCheck.getX(), finalCheck.getY(), finalCheck.getZ());
								if (finalCrop != null) {
									UUID placerUUID = finalCrop.getPlacer();
									
									Crop.create(chunk, x,y,z,  block.getType().toString(), null,
											placerUUID, System.currentTimeMillis(), true);									
								}
							}
						}
					}
				}
			}
		});

	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent e) {
		Block source = e.getSource();
		WorldChunk sourceChunk = CropControl.getDAO().getChunk(source.getChunk());
		int sourceX = source.getX();
		int sourceY = source.getY();
		int sourceZ = source.getZ();

		Block block = e.getBlock();
		WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();

		if (!harvestableCrops.containsKey(source.getType()) && source.getType() != Material.CHORUS_FLOWER
				&& source.getType() != Material.CHORUS_PLANT && block.getType() != Material.CHORUS_FLOWER
				&& block.getType() != Material.CHORUS_PLANT)
			return;

		Crop sourceCrop = sourceChunk.getCrop(sourceX, sourceY, sourceZ);
		if (sourceCrop != null) {
			UUID placerUUID = sourceCrop.getPlacer();
			Crop.create(chunk, x, y, z, source.getType().toString(), null, placerUUID,
					System.currentTimeMillis(), true);
			return;
		} 
		
		TreeComponent treeComponent = sourceChunk.getTreeComponent(sourceX, sourceY, sourceZ);
		if (treeComponent != null) {
			treeComponent.setHarvestable(true);

			// TODO: should we differentiate between flower and plant here?
			TreeComponent.create(treeComponent.getTreeID(), chunk, x, y, z, Material.CHORUS_PLANT.toString(),
					treeComponent.getPlacer(), true);
		}

	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true) // RB is at HIGH and NORMAL
	public void onTreeGrow(StructureGrowEvent e) {
		Location structureLocation = e.getLocation();
		int x = structureLocation.getBlockX();
		int y = structureLocation.getBlockY();
		int z = structureLocation.getBlockZ();
		WorldChunk sourceChunk = CropControl.getDAO().getChunk(structureLocation.getChunk());
		Sapling sapling =  sourceChunk.getSapling(x, y, z);
		Crop crop =  sourceChunk.getCrop(x, y, z);

		if (sapling == null && crop == null) {
			// untracked growth
			return;
		}
		
		List<BlockState> blocks = new ArrayList<BlockState>();
	
		if (sapling != null) {
			sapling.setRemoved();
			// Because dirt & saplings are part of the structure
			for (BlockState state : e.getBlocks()) {
				if (state.getType() == Material.LOG || state.getType() == Material.LOG_2
						|| state.getType() == Material.LEAVES || state.getType() == Material.LEAVES_2) {
					blocks.add(state);
				}
			}

			if (blocks.size() == 0) {
				CropControl.getPlugin().debug("Ignoring tree grow that has no logs or leaves at {0}, {1}, {2}", x, y, z);
				// TODO: do we remove the sapling?
				return;
			}

			Tree tree = Tree.create(sourceChunk, x, y, z, e.getSpecies().toString(), sapling.getPlacer(), System.currentTimeMillis());

			// Done in the case of Multiple saplings (Big Jungle trees etc)
			for (BlockState state : e.getBlocks()) {
				if (state.getY() == sapling.getY()) {
					WorldChunk testChunk = CropControl.getDAO().getChunk(state.getChunk());
					Sapling testSapling = testChunk.getSapling(state.getX(), state.getY(), state.getZ());
					if (testSapling == null) {
						//CropControl.getPlugin().debug("Found a sapling part of a recognized structure that wasn't itself tracked at {0}", state.getLocation());
						continue;
					}
					
					testSapling.setRemoved();
				}
			}
			Set<Long> treesStolen = new HashSet<Long>();
			for (BlockState state : blocks) {
				WorldChunk partChunk = CropControl.getDAO().getChunk(state.getChunk());
				// TODO: differentiate between leaves and trunks
				// Check for "theft":
				TreeComponent exists = partChunk.getTreeComponent(state.getX(), state.getY(), state.getZ());
				if (exists != null) {
					treesStolen.add(exists.getTreeID());
					exists.setRemoved();
				}
				TreeComponent.create(tree, partChunk, state.getX(), state.getY(), state.getZ(), e.getSpecies().toString(),
						tree.getPlacer(), true);
			}
			if (treesStolen.size() > 0) {
				// Check we didn't just override the last leaves of an old tree or something
				for (Long treeID : treesStolen) {
					List<TreeComponent> remainder = CropControl.getDAO().getTreeComponents(treeID);
					if (remainder.isEmpty()) {
						Tree treeGone = CropControl.getDAO().getTree(treeID);
						treeGone.setRemoved();
					}
				}
			}
			return;
		}
		
		if (crop != null) {
			// Because dirt & saplings are part of the structure
			for (BlockState state : e.getBlocks()) {
				if (state.getType() == Material.HUGE_MUSHROOM_1 || state.getType() == Material.HUGE_MUSHROOM_2)
					blocks.add(state);
			}

			if (blocks.size() == 0) {
				CropControl.getPlugin().debug("Ignoring mushroom? grow that has no mushroom parts at {0}, {1}, {2}", x, y, z);
				return;
			}

			Tree tree = Tree.create(sourceChunk, x, y, z, e.getSpecies().toString(), crop.getPlacer(), System.currentTimeMillis());

			crop.setRemoved();

			for (BlockState state : blocks) {
				WorldChunk partChunk = CropControl.getDAO().getChunk(state.getChunk());
				TreeComponent.create(tree, partChunk, state.getX(), state.getY(), state.getZ(), e.getSpecies().toString(),
						tree.getPlacer(), true);
			}
		}
	}

	/*
	 * 
	 * End of Block Placement
	 * 
	 * Start of Block Break tracking
	 * 
	 */
	
	/**
	 * Issues:
	 * <ul>
	 *  <li>If soil is broken, any wheat, carrots, potatoes, beetroot, melon stalk, pumpkin stalk are broken (no new break event, just physics events)</li>
	 *  <li>If netherrack is broken, any netherwart above is broken</li>
	 *  <li>If sand is broken, any cactus or sugarcane above is broken</li>
	 *  <li>If endstone is broken, any chorus fruit above is broken</li>
	 *  <li>If a sapling or mushroom is above any block when the block is broken, the sapling and mushroom will break</li>
	 * </ul> 
	 * 
	 * Resolution:
	 *   This method checks for satisfaction of any of those conditions, and returns true, indicating the block <i>above</i> might need to be handled during break.
	 *   
	 * @param block The block to check
	 * @return True if any of the conditions are met.
	 */
	private boolean maybeBelowTracked(Block block) {
		if (Material.SOIL.equals(block.getType()) ||  // wheat, carrots, potatoes, beetroot, melon stalk, pumpkin stalk
				Material.NETHERRACK.equals(block.getType()) || // netherwart
				Material.SAND.equals(block.getType()) || // cactus, sugarcane
				Material.ENDER_STONE.equals(block.getType())) {  // chorus fruit 
			return true;
		}
		Block up = block.getRelative(BlockFace.UP);
		if (Material.BROWN_MUSHROOM.equals(up.getType()) || Material.RED_MUSHROOM.equals(up.getType()) || // mushrooms
				Material.SAPLING.equals(up.getType())) { // saplings
			return true;
		}
		return false;
	}
	
	/**
	 * Addresses cocoa on sides of trees; can be on any side, but not top, not bottom
	 * 
	 * @param block The block to test type on
	 * @return True if maybe could possible contain cocoa
	 */
	@SuppressWarnings("deprecation")
	private boolean maybeSideTracked(Block block) {
		if (Material.LOG.equals(block.getType()) && block.getData() == 3) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Propagates cocoa breaks
	 * 
	 * @param block
	 * @param type
	 * @param player
	 */
	private void trySideBreak(Block block, BreakType type, UUID player) {
		for (BlockFace face : directions) {
			Block faceBlock = block.getRelative(face);
			Location loc = faceBlock.getLocation();
			if (Material.COCOA.equals(faceBlock.getType()) && !pendingChecks.contains(loc)) {
				pendingChecks.add(loc);
				handleBreak(faceBlock, type, player, null);
			}
		}
	}
	
	/**
	 * Physics checks; launches a break task if the material under review might be breaking and not covered by some other test.
	 * 
	 * Note that basically all physics related checks involve breaks. Some are 
	 * immediate and can be cancelled; others you only know what's going on if you check the material
	 * and then check all the conditions that can break it (missing soil, light, etc) as the
	 * code paths in new.minecraft.server don't involve any return values.
	 * 
	 * So for our purposes as we're just augmenting drops at present, we'll look for things we
	 * care about, and register a break-check if it looks interesting.
	 * 
	 * Currently tracked: 
	 * <ul><li>Too little light on crops</li><li>Too much light on mushrooms (with wrong soil)</li>
	 * <li>No water adjacent to sugarcane</li><li>Solid blocks adjacent to cactus</li></ul> 
	 *  
	 * @param e The physics event.
	 */
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true) 
	public void onPhysics(BlockPhysicsEvent e) {
		Block block = e.getBlock();
		Material chMat = block.getType(); //getChangedType();
		Location loc = block.getLocation();
		boolean checkBreak = false;
		if (maybeTracked(chMat) && !pendingChecks.contains(loc)) { // do we even slightly care?
			// Check light levels.
			if (Material.CROPS.equals(chMat) || Material.POTATO.equals(chMat) || Material.CARROT.equals(chMat) || Material.BEETROOT.equals(chMat)) {
				if (block.getLightLevel() < 8) {
					checkBreak = true;
				}
			} else if (Material.RED_MUSHROOM.equals(chMat) || Material.BROWN_MUSHROOM.equals(chMat)) {
				Block below = block.getRelative(BlockFace.DOWN);
				Material belowM = below.getType();
				if (!Material.MYCEL.equals(belowM) && !Material.DIRT.equals(belowM)) {
					checkBreak = true;
				} else if (Material.DIRT.equals(belowM) && below.getData() != 2) {
					checkBreak = true;
				}
			}
		}
		if (checkBreak) {
			pendingChecks.add(loc);
			handleBreak(block, BreakType.PHYSICS, null, null);
		} else { // we haven't found a break condition yet. However, what follows aren't light level checks but rather
			// block checks, so these are controlled by a variety of rules. Some involve physics firing _adjacent_ to the block.
			// Basically it's a crapshoot.
			if (Material.SUGAR_CANE_BLOCK.equals(e.getChangedType())) {
				// Sugarcane winds up being weird. I'm still not sure what event fires and removes the bottom block but for
				// unattended (non-player) breaks physics events remove middle and top blocks. So, we just register
				// breaks for lower and upper blocks and if they are gone, we know it then.
				//
				// Note this will leave singular base blocks undetected. TODO
				if (chMat.equals(e.getChangedType())) {
					for (BlockFace a : CropControlEventHandler.traverse) {
						Location adjL = block.getRelative(a).getLocation();
						if (!pendingChecks.contains(adjL)) {
							pendingChecks.add(adjL);
							// So, the physics check can take a tick to resolve. We mark our interest but defer resolution.
							Bukkit.getScheduler().runTaskLater(CropControl.getPlugin(), new Runnable() {
								public void run() {
									handleBreak(adjL.getBlock(), BreakType.PHYSICS, null, null);
								}
							}, 1L);
						}
					}
				}
			} else if (Material.CACTUS.equals(e.getChangedType())) {
				if (chMat.equals(e.getChangedType())) return; // handled elsewhere
				
				// Cactus is a little simpler. It breaks on adjacent placements; that's what would trigger this event. 
				for (BlockFace face : CropControlEventHandler.directions) {
					// We look around face-adjacent places and trigger a break-check for any cactus found.
					Block adj = block.getRelative(face);
					Material adjM = adj.getType();
					Location adjL = adj.getLocation();
					if (Material.CACTUS.equals(adjM) && !pendingChecks.contains(adjL)) {
						pendingChecks.add(adjL);
						// So, the physics check can take a tick to resolve. We mark our interest but defer resolution.
						Bukkit.getScheduler().runTaskLater(CropControl.getPlugin(), new Runnable() {
							public void run() {
								handleBreak(adj, BreakType.PHYSICS, null, null);
							}
						}, 1L);
					}
				}
			} else if (Material.CHORUS_FLOWER.equals(e.getChangedType()) || Material.CHORUS_PLANT.equals(e.getChangedType())) {
				// TODO: this one is complicated; it's more like sugarcane I guess? Still need rules.
			}
		}
	}
	
	/**
	 * So far specifically handles these cases:
	 * 
	 * 1) Block broken is tracked
	 * 2) Block breaks by not-players
	 * 3) Block breaks by players
	 * 4) Indirect block breaks -- destroying block supporting a crop or collapsible tree, or under mushrooms
	 * 5) Indirect block break of cocoa bearing logs
	 * 6) Block broken had mushroom on top and cocoa on the sides
	 * 
	 * @param e The event
	 */
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		Block block = e.getBlock();
		Player player = e.getPlayer();
		BreakType type = player != null ? BreakType.PLAYER : BreakType.NATURAL;
		UUID uuid = player != null ? player.getUniqueId() : null;
		if (maybeSideTracked(block)) {
			trySideBreak(block, type, uuid);
		}
		if (maybeBelowTracked(block)) {
			block = block.getRelative(BlockFace.UP);
		}
		Location loc = block.getLocation();
		if (!pendingChecks.contains(loc)) {
			pendingChecks.add(loc);
			handleBreak(block, type, uuid, null);
		}
	}

	/**
	 * So far specifically handles these cases:
	 * 
	 * 1) Block burnt is tracked
	 * 2) Block burnt is under a tracked block (probably only mushrooms eligible)
	 * 3) Block burnt was a jungle tree, checks for cocoa.
	 * 4) Burnt block had mushroom on top and cocoa on the sides
	 * 
	 * @param e The event
	 */
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent e) {
		Block block = e.getBlock();
		if (maybeSideTracked(block)) {
			trySideBreak(block, BreakType.FIRE, null);
		}
		if (maybeBelowTracked(block)) {
			block = block.getRelative(BlockFace.UP);
		}
		Location loc = block.getLocation();
		if (!pendingChecks.contains(loc)) {
			pendingChecks.add(loc);
			handleBreak(block, BreakType.FIRE, null, null);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent e) {
		doMultiblockHandler(e.blockList(), BreakType.EXPLOSION, null);
	}

	/**
	 * Handles multiblock breaks, registering all things that are tracked that either might break or have broken
	 * based on explode/other break similar to explode
	 * 
	 * @param blockList The list of blocks to be broken
	 * @param breakType What kind of break it was
	 * @param player Who triggered it (if applicable)
	 */
	public void doMultiblockHandler(List<Block> blockList, BreakType breakType, UUID player) {
		Set<Location> toBreakList = new HashSet<Location>();

		for (Block block : blockList) {
			WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
			int x = block.getX();
			int y = block.getY();
			int z = block.getZ();
			TreeComponent component = chunk.getTreeComponent(x, y, z);
			if (component != null) {
				if (getTrackedTreeMaterial(component.getTreeType()) == Material.CHORUS_PLANT) {
					for (Location location : returnUpwardsChorusBlocks(block)) {
						if (!toBreakList.contains(location)) {
							toBreakList.add(location);
						} else {
							break; // once we hit a thing already contained, move on.
						}
					}
				} else {
					Location loc = block.getLocation();
					if (!pendingChecks.contains(loc)) {
						pendingChecks.add(loc);
						handleBreak(block, breakType, player, null);
					}
				}
				continue;
			}
			Crop crop = chunk.getCrop(x, y, z);
			if (crop != null) {
				Material type = getTrackedCropMaterial(crop.getCropType());
				if (type == Material.SUGAR_CANE_BLOCK || type == Material.CACTUS) {
					for (Location location : returnUpwardsBlocks(block, type)) {
						if (!toBreakList.contains(location)) {
							toBreakList.add(location);
						} else {
							break;
						}
					}
				} else {
					Location loc = block.getLocation();
					if (!pendingChecks.contains(loc)) {
						pendingChecks.add(loc);
						handleBreak(block, breakType, player, null);
					}
				}
				continue;
			}
			
			if (maybeSideTracked(block)) {
				for (BlockFace face : directions) {
					Block faceBlock = block.getRelative(face);
					if (Material.COCOA.equals(faceBlock.getType())) {
						toBreakList.add(faceBlock.getLocation());
					}
				}
			} 
			
			if (maybeBelowTracked(block)) {
				toBreakList.add(block.getRelative(BlockFace.UP).getLocation());
			} else {
				Location loc = block.getLocation();
				if (!pendingChecks.contains(loc)) {
					pendingChecks.add(loc);
					handleBreak(block, breakType, player, null);
				}
			}
		}
		if (toBreakList.size() > 0) {
			handleBreak(null, breakType, player, toBreakList);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent e) {
		doMultiblockHandler(e.blockList(), BreakType.EXPLOSION, null);
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onLeafDecay(LeavesDecayEvent e) {
		Block block = e.getBlock();
		Location loc = block.getLocation();
		if (!pendingChecks.contains(loc)) {
			pendingChecks.add(loc);
			handleBreak(block, BreakType.NATURAL, null, null);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent e) {
		Block block = e.getBlock();
		if (maybeSideTracked(block)) {
			trySideBreak(block, BreakType.NATURAL, null);
		}
		if (maybeBelowTracked(block)) {
			block = block.getRelative(BlockFace.UP);
		}
		Location loc = block.getLocation();
		if (!pendingChecks.contains(loc)) {
			pendingChecks.add(loc);
			handleBreak(block, BreakType.NATURAL, null, null);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent e) {
		UUID uuid = null;
		if (e.getPlayer() != null) {
			uuid = e.getPlayer().getUniqueId();			
		}
		
		if (e.getBucket() == Material.LAVA_BUCKET) {
			Block block = e.getBlockClicked().getRelative(e.getBlockFace());
			Location loc = block.getLocation();
			if (!pendingChecks.contains(loc)) {
				pendingChecks.add(loc);
				handleBreak(block, BreakType.LAVA, uuid, null);
			}
		} else if (e.getBucket() == Material.WATER_BUCKET) {
			Block block = e.getBlockClicked().getRelative(e.getBlockFace());
			Location loc = block.getLocation();
			if (!pendingChecks.contains(loc)) {
				pendingChecks.add(loc);
				handleBreak(block, BreakType.WATER, uuid, null);
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent e) {
		Material toBlock = e.getToBlock().getType();
		if (toBlock == Material.WATER || toBlock == Material.STATIONARY_WATER) {
			Block block = e.getBlock();
			Location loc = block.getLocation();
			if (!pendingChecks.contains(loc)) {
				pendingChecks.add(loc);
				handleBreak(block, BreakType.WATER, null, null);
			}
		} else if (toBlock == Material.LAVA || toBlock == Material.STATIONARY_LAVA) {
			Block block = e.getBlock();
			Location loc = block.getLocation();
			if (!pendingChecks.contains(loc)) {
				pendingChecks.add(loc);
				handleBreak(block, BreakType.LAVA, null, null);
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistionExtend(BlockPistonExtendEvent e) {
		// We need to order movements of moved components from furthest to nearest
		TreeMap<Double, Runnable> movements = new TreeMap<Double, Runnable>();
		for (Block block : e.getBlocks()) {
			// handle tree breaks
			WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
			int x = block.getX();
			int y = block.getY();
			int z = block.getZ();
			TreeComponent component = chunk.getTreeComponent(x, y, z);
			
			Block nextBlock = block.getRelative(e.getDirection());
			
			WorldChunk newChunk = CropControl.getDAO().getChunk(nextBlock.getChunk());
			int newX = nextBlock.getX();
			int newY = nextBlock.getY();
			int newZ = nextBlock.getZ();

			if (component != null) {
				// chorus fruit tree break
				if (getTrackedTreeMaterial(component.getTreeType()) == Material.CHORUS_PLANT) {
					Location loc = block.getLocation();
					if (!pendingChecks.contains(loc)) {
						pendingChecks.add(loc);
						handleBreak(block, BreakType.PISTON, null, null);
					}

					continue;
				}

				// move component and optionally tree
				Tree tree = chunk.getTree(x, y, z);
				if (tree == null) {
					movements.put(e.getBlock().getLocation().distance(block.getLocation()), 
							new Runnable() {
								@Override
								public void run() {
									component.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
									CropControl.getPlugin().debug("Moved tree component from {0} {1} {2} to {3}", x, y, z, component);
								}
							}
					);
				} else {
					movements.put(e.getBlock().getLocation().distance(block.getLocation()), 
							new Runnable() {
								@Override
								public void run() {
									component.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
									CropControl.getPlugin().debug("Moved tree component from {0} {1} {2} to {3}", x, y, z, component);
									tree.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
									CropControl.getPlugin().debug("Moved tree from {0} {1} {2} to {3}", x, y, z, tree);
								}
							}
					);
				}
				// if tree base, move the whole tree

				continue;
			} 
			
			if (maybeSideTracked(block)) {
				trySideBreak(block, BreakType.PISTON, null);
			}
			if (maybeBelowTracked(block)) {
				block = block.getRelative(BlockFace.UP);
			}
			Location loc = block.getLocation();
			if (!pendingChecks.contains(loc)) {
				pendingChecks.add(loc);
				handleBreak(block, BreakType.PISTON, null, null);
			}
		}
		if (!movements.isEmpty()) {
			for (Double update : movements.descendingKeySet()) {
				Runnable movement = movements.get(update);
				movement.run(); // just as a method, not a thread or anything  :)
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent e) {
		// We need to order movements of moved components from furthest to nearest
		TreeMap<Double, Runnable> movements = new TreeMap<Double, Runnable>();

		for (Block block : e.getBlocks()) {
			// handle tree breaks
			WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
			int x = block.getX();
			int y = block.getY();
			int z = block.getZ();
			TreeComponent component = chunk.getTreeComponent(x, y, z);
			
			Block nextBlock = block.getRelative(e.getDirection());
			
			WorldChunk newChunk = CropControl.getDAO().getChunk(nextBlock.getChunk());
			int newX = nextBlock.getX();
			int newY = nextBlock.getY();
			int newZ = nextBlock.getZ();

			if (component != null) {
				if (getTrackedTreeMaterial(component.getTreeType()) == Material.CHORUS_PLANT) {
					Location loc = block.getLocation();
					if (!pendingChecks.contains(loc)) {
						pendingChecks.add(loc);
						handleBreak(block, BreakType.PISTON, null, null);
					}

					continue;
				}

				// move component and optionally tree
				Tree tree = chunk.getTree(x, y, z);
				if (tree == null) {
					movements.put(e.getBlock().getLocation().distance(block.getLocation()), 
							new Runnable() {
								@Override
								public void run() {
									component.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
									CropControl.getPlugin().debug("Moved tree component from {0} {1} {2} to {3}", x, y, z, component);
								}
							}
					);
				} else {
					movements.put(e.getBlock().getLocation().distance(block.getLocation()), 
							new Runnable() {
								@Override
								public void run() {
									component.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
									CropControl.getPlugin().debug("Moved tree component from {0} {1} {2} to {3}", x, y, z, component);
									tree.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
									CropControl.getPlugin().debug("Moved tree from {0} {1} {2} to {3}", x, y, z, tree);
								}
							}
					);
				}
				// if tree base, move the whole tree

				continue;
			} else {
				if (maybeSideTracked(block)) {
					trySideBreak(block, BreakType.PISTON, null);
				}
				if (maybeBelowTracked(block)) {
					block = block.getRelative(BlockFace.UP);
				}
				Location loc = block.getLocation();
				if (!pendingChecks.contains(loc)) {
					pendingChecks.add(loc);
					handleBreak(block, BreakType.PISTON, null, null);
				}
			}
		}
		if (!movements.isEmpty()) {
			for (Double update : movements.descendingKeySet()) {
				Runnable movement = movements.get(update);
				movement.run(); // just as a method, not a thread or anything  :)
			}
		}
	}

	public Set<Location> returnUpwardsBlocks(Block startBlock, Material upwardBlockMaterial) {
		Set<Location> checkedLocations = new HashSet<Location>();

		Set<Location> uncheckedLocations = new HashSet<Location>();

		Crop crop = CropControl.getDAO().getChunk(startBlock.getChunk()).getCrop(startBlock.getX(), startBlock.getY(), startBlock.getZ());
		if (crop != null) {
			if (getTrackedCropMaterial(crop.getCropType()) == upwardBlockMaterial) {
				uncheckedLocations.add(startBlock.getLocation());
			}
		} else {
			return checkedLocations; // failfast
		}

		Set<Location> toAddLocations = new HashSet<Location>();
		
		do {
			for (Location unchecked : uncheckedLocations) {
				if (CropControl.getDAO().isTracked(unchecked.getBlock())
						&& !checkedLocations.contains(unchecked)) {
					checkedLocations.add(unchecked);
				}
			}

			for (Location location : uncheckedLocations) {
				Block upBlock = location.getBlock().getRelative(BlockFace.UP);
				Location up = upBlock.getLocation();
				if (CropControl.getDAO().isTracked(upBlock)
						&& !toAddLocations.contains(up)
						&& !checkedLocations.contains(up)) {
					
					if (getTrackedCropMaterial(
							CropControl.getDAO().getChunk(upBlock.getChunk()).getCrop(upBlock.getX(), upBlock.getY(), upBlock.getZ())
									.getCropType()) == upwardBlockMaterial)
						toAddLocations.add(up);
				}
			}

			uncheckedLocations.clear();

			uncheckedLocations.addAll(toAddLocations);

			toAddLocations.clear();

		} while (!uncheckedLocations.isEmpty());

		return checkedLocations;
	}

	public Set<Location> returnUpwardsChorusBlocks(Block startBlock) {
		Set<Location> checkedLocations = new HashSet<Location>();

		Set<Location> uncheckedLocations = new HashSet<Location>();

		TreeComponent component = CropControl.getDAO().getChunk(startBlock.getChunk())
				.getTreeComponent(startBlock.getX(), startBlock.getY(), startBlock.getZ());
		if (component != null) {
			if (getTrackedTreeMaterial(component.getTreeType()) == Material.CHORUS_PLANT) {
				uncheckedLocations.add(startBlock.getLocation());
			}
		} else {
			return checkedLocations; // failfast
		}
		
		Tree tree = CropControl.getDAO().getTree(component);

		Set<Location> toAddLocations = new HashSet<Location>();
		do {
			for (Location unchecked : uncheckedLocations) {
				if (CropControl.getDAO().isTracked(unchecked.getBlock())
						&& !checkedLocations.contains(unchecked)) {
					checkedLocations.add(unchecked);
				}
			}

			for (Location location : uncheckedLocations) {
				for (BlockFace blockFace : CropControlEventHandler.growDirections) {
					Block relBlock = location.getBlock().getRelative(blockFace);
					Location rel = relBlock.getLocation();
					if (CropControl.getDAO().isTracked(relBlock)
							&& !toAddLocations.contains(rel)
							&& !checkedLocations.contains(rel)) {
						TreeComponent relComponent = CropControl.getDAO().getChunk(relBlock.getChunk())
								.getTreeComponent(relBlock.getX(), relBlock.getY(), relBlock.getZ());
						if (getTrackedTreeMaterial(relComponent.getTreeType()) == Material.CHORUS_PLANT
								&& CropControl.getDAO().isTreeComponent(tree, relComponent)) {
							toAddLocations.add(rel);
						}
					}
				}
			}

			uncheckedLocations.clear();

			uncheckedLocations.addAll(toAddLocations);

			toAddLocations.clear();

		} while (!uncheckedLocations.isEmpty());

		return checkedLocations;
	}

	public void handleBreak(final Block startBlock, final BreakType breakType, final UUID breaker, final Set<Location> altBlocks) {
		if (startBlock != null && !CropControl.getDAO().isTracked(startBlock)) {
			//CropControl.getPlugin().debug("Rejecting break at {0}", startBlock.getLocation());
			pendingChecks.remove(startBlock.getLocation());
			
			// Check if we care pre-break.
			return;
		}
		// TODO does it need to be 1 tick? more? less?
		Bukkit.getScheduler().runTaskLater(CropControl.getPlugin(),
				new Runnable() {
					@Override
					public void run() {
						if (startBlock != null) { // no altBlocks.
							try {
								WorldChunk chunk = CropControl.getDAO().getChunk(startBlock.getChunk());
								int x = startBlock.getX();
								int y = startBlock.getY();
								int z = startBlock.getZ();
								
								Crop crop = chunk.getCrop(x, y, z);
								if (crop != null) {
									Material type = getTrackedCropMaterial(crop.getCropType());
									if (type == startBlock.getType()) { // Still there.
										//CropControl.getPlugin().debug("Ignoring cancelled Crop {3} track vs. actual {4} type {0}, {1}, {2}", x, y, z, type, startBlock.getType());
										return;
									}
		
									if (type == Material.SUGAR_CANE_BLOCK || type == Material.CACTUS) {
										for (Location location : returnUpwardsBlocks(startBlock, type)) {
											WorldChunk upChunk = CropControl.getDAO().getChunk(location.getChunk());
											Crop upCrop = upChunk.getCrop(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	
											drop(location.getBlock(), upCrop, breaker, breakType);
	
											upCrop.setRemoved();
										}
									} else {
										drop(startBlock, crop, breaker, breakType);
		
										crop.setRemoved();
									}
									
									return;
								}
								
								Sapling sapling = chunk.getSapling(x, y, z);
								if (sapling != null) {
									if (getTrackedSaplingMaterial(sapling.getSaplingType()) == startBlock.getType()) {
										//CropControl.getPlugin().debug("Ignoring cancelled Sapling {3} track vs. actual {4} type {0}, {1}, {2}", x, y, z, sapling.getSaplingType(), startBlock.getType());
										return;
									}
									drop(startBlock, sapling, breaker, breakType);
		
									sapling.setRemoved();
									return;
								}
								
								TreeComponent treeComponent = chunk.getTreeComponent(x, y, z);
								if (treeComponent != null) {
									Tree tree = CropControl.getDAO().getTree(treeComponent);
									Material type = getTrackedTreeMaterial(treeComponent.getTreeType());
									if (type == 
											(startBlock.getType() == Material.LEAVES ? Material.LOG : 
											startBlock.getType() == Material.LEAVES_2 ? Material.LOG_2 :
											startBlock.getType() == Material.CHORUS_FLOWER	? Material.CHORUS_PLANT : 
											startBlock.getType())) {
										//CropControl.getPlugin().debug("Ignoring cancelled Tree Component {3} track vs. actual {4} type {0}, {1}, {2}", x, y, z, type, startBlock.getType());
										return;
									}
		
									if (type == Material.CHORUS_PLANT) {
										for (Location location : returnUpwardsChorusBlocks(startBlock)) {
											TreeComponent upComponent = CropControl.getDAO().getChunk(location.getChunk())
													.getTreeComponent(location.getBlockX(), location.getBlockY(), location.getBlockZ());
											drop(location.getBlock(), upComponent, breaker, breakType);
	
											upComponent.setRemoved();
	
										}
									} else {
										drop(startBlock, treeComponent, breaker, breakType);
		
										treeComponent.setRemoved();
									}
									if (CropControl.getDAO().getTreeComponents(tree).isEmpty()) {
										CropControl.getPlugin().debug("Tree at {0} broken as {1} by {2}", 
												startBlock.getLocation(), breakType, breaker);
	
										tree.setRemoved();										
									}
									return;
								}
								CropControl.getPlugin().debug("Failed to find matching element at {0}, {1}, {2}", x, y, z);
							} finally {
								pendingChecks.remove(startBlock.getLocation());
							}
						} else {
							for (Location location : altBlocks) {
								try {
									Block block = location.getBlock();
									int x = block.getX();
									int y = block.getY();
									int z = block.getZ();
									WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
									
									Crop crop = chunk.getCrop(x, y, z);
									if (crop != null) {
										drop(location.getBlock(), crop, breaker, breakType);
										
										crop.setRemoved();
										continue;
									}
	
									Sapling sapling = chunk.getSapling(x, y, z);
									if (sapling != null) {
										drop(location.getBlock(), sapling, breaker, breakType);
										
										sapling.setRemoved();
										continue;
									}
									
									TreeComponent treeComponent = chunk.getTreeComponent(x, y, z);
									
									if (treeComponent != null) {
										drop(location.getBlock(), treeComponent, breaker, breakType);
		
										treeComponent.setRemoved();
										
										if (CropControl.getDAO().getTreeComponents(treeComponent.getTreeID()).isEmpty()) {
											CropControl.getPlugin().debug("Tree at {0} broken as {1} by {2}", 
													location, breakType, breaker);
		
											CropControl.getDAO().getTree(treeComponent).setRemoved();
										}
										
										continue;
									}
									CropControl.getPlugin().debug("Encountered altBlock to drop that isn't a crop or component at {0}, {1}, {2}", x, y, z);
								} finally {
									pendingChecks.remove(location);
								}
							}
						}
					}
				}, 1L);
	}

	private void drop(Block block, Locatable dropable, UUID player, BreakType breakType) {
		Location bLoc = block.getLocation();
		Biome biome = block.getBiome();
		UUID placePlayer = null; 
		boolean byPlayer = player != null;
		boolean harvestable = false;
		RootConfig config = null;
		if (dropable instanceof Crop) {
			Crop crop = (Crop) dropable;
			placePlayer = crop.getPlacer();
			harvestable = crop.getHarvestable();
			config = RootConfig.from(crop);
		}
		if (dropable instanceof Sapling) {
			Sapling sapling = (Sapling) dropable;
			placePlayer = sapling.getPlacer();
			harvestable = sapling.getHarvestable();
			config = RootConfig.from(sapling);
		}
		if (dropable instanceof TreeComponent) {
			TreeComponent component = (TreeComponent) dropable;
			placePlayer = component.getPlacer();
			harvestable = component.isHarvestable();
			config = RootConfig.from(component);
		}
		ItemStack toolUsed = null;
		if (byPlayer) {
			Player p = Bukkit.getPlayer(player);
			if (p != null) {
				toolUsed = p.getInventory().getItemInMainHand();
			}
		}
		
		List<ItemStack> items = config.realizeDrops(breakType, placePlayer, player, harvestable, biome, toolUsed);
		if (items != null) {
			CropControlDropEvent event = new CropControlDropEvent(bLoc, breakType, dropable, player, items);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled() && event.getItems() != null && event.getItems().size() > 0) {
				if (player != null) {
					CropControl.getPlugin().info("Dropping {0} items at {1} due to break {2} caused by player {3}: {4}", 
							event.getItems().size(), bLoc, breakType, player, summarizeDrops(event.getItems()));
					if (this.baseDropMessage != null) {
						String finalMsg = baseDropMessage.replaceAll("%crop%", friendlyCropName(dropable));
						finalMsg = finalMsg.replaceAll("%items%", friendlySummarizeDrops(event.getItems()));
						try {
							Bukkit.getPlayer(player).sendMessage(finalMsg);
						} catch (Exception e) {} // NO-OP, sending is just best effort
					}
				} else {
					CropControl.getPlugin().info("Dropping {0} items at {1} due to break {2}: {3}",
							event.getItems().size(), bLoc, breakType, summarizeDrops(event.getItems()));
				}
				realDrop(bLoc, event.getItems());
			}
		} else {
			CropControl.getPlugin().debug("Locatable {0} at {1} broken as {2} by {3}", 
					dropable.getClass().getName(), bLoc, breakType, player);
		}
	}
	
	public String summarizeDrops(List<ItemStack> items) {
		StringBuffer toString = new StringBuffer();
		for (ItemStack itemStack: items) {
			toString.append("[");
			Material material = itemStack.getType();
			int amount = itemStack.getAmount();
			short durability = itemStack.getDurability();
					
			if (amount != 1) {
				toString.append(amount).append("x");
			}
			
			toString.append(itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() ? 
						itemStack.getItemMeta().getDisplayName() : material.toString());
			toString.append(":").append(durability);
			toString.append("]");
		}
		return toString.toString();
	}
	
	public String friendlyCropName(Locatable dropable) {
		if (dropable instanceof Crop) {
			Crop crop = (Crop) dropable;
			return crop.getCropType();
		}
		if (dropable instanceof Sapling) {
			Sapling sapling = (Sapling) dropable;
			return sapling.getSaplingType();
		}
		if (dropable instanceof TreeComponent) {
			TreeComponent component = (TreeComponent) dropable;
			return component.getTreeType();
		}
		
		return "Unknown";
	}
	
	public String friendlySummarizeDrops(List<ItemStack> items) {
		StringBuffer toString = new StringBuffer();
		for (ItemStack itemStack: items) {
			toString.append(" ");
			Material material = itemStack.getType();
			int amount = itemStack.getAmount();
			short durability = itemStack.getDurability();
					
			if (amount != 1) {
				toString.append(amount).append(" x ");
			}
			
			toString.append(itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() ? 
						itemStack.getItemMeta().getDisplayName() : material.toString());
			if (durability > 0) {
				toString.append(":").append(durability);
			}
		}
		return toString.toString();
	}	
	
	public void realDrop(Location location, List<ItemStack> items) {
		for (ItemStack item : items) {
			location.getWorld().dropItem(location.add(0.5, 0.5, 0.5), item).setVelocity(new Vector(0, 0.05, 0));
		}
	}

	public static enum BreakType {
		PLAYER, WATER, LAVA, PISTON, EXPLOSION, NATURAL, FIRE, PHYSICS;
	}

	/*
	 * 
	 * End of Block Break Tracking
	 * 
	 * Start of Chunk Loading/Unloading
	 * 
	 */

	/**
	 * This is where we pre-load data for each chunk. If it's already in the loaded cache, nothing happens. If it was previously
	 * staged for unload, this cancels that.
	 * 
	 * @param e The chunk to load.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onChunkLoad(ChunkLoadEvent e) {
		Chunk chunk = e.getChunk();
		
		CropControl.getDAO().getChunk(chunk);

	}

	/**
	 * Marks a loaded chunk as pending unload. It'll be unloaded later en-masse.
	 * 
	 * @param e The chunk to unload.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onChunkUnload(ChunkUnloadEvent e) {
		Chunk chunk = e.getChunk();
		
		CropControl.getDAO().unloadChunk(chunk);
	}

}
