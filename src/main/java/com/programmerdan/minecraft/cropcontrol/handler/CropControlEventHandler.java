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
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
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
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;
import org.bukkit.util.Vector;

import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.data.Crop;
import com.programmerdan.minecraft.cropcontrol.data.Sapling;
import com.programmerdan.minecraft.cropcontrol.data.Tree;
import com.programmerdan.minecraft.cropcontrol.data.TreeComponent;
import com.programmerdan.minecraft.cropcontrol.data.WorldChunk;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

/**
 * Simple monitor for all growth and break events and such.
 * 
 * @author <a href="mailto:programmerdan@gmail.com">ProgrammerDan</a>
 *
 */
public class CropControlEventHandler implements Listener {
	private FileConfiguration config;
	/**
	 * List of materials that are crops, and if we track specific states
	 * belonging to that material.
	 */
	private Map<Material, Boolean> harvestableCrops;
	
	public static final BlockFace[] directions = new BlockFace[] { 
			BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };

	public static final BlockFace[] growDirections = new BlockFace[] { 
			BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP };
	
	public CropControlEventHandler(FileConfiguration config) {
		this.config = config;

		harvestableCrops = new HashMap<Material, Boolean>();

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
		switch (blockState.getBlock().getType()) {
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
			return null;
		}
	}

	public Material getTrackedTypeMaterial(String trackedType) {
		for (Material material : harvestableCrops.keySet()) {
			if (material.toString() == trackedType)
				return material;
		}

		if (Material.MELON_BLOCK.toString() == trackedType)
			return Material.MELON_BLOCK;
		else if (Material.PUMPKIN.toString() == trackedType)
			return Material.PUMPKIN;

		for (Byte i = 0; i < 6; i++) {
			if (getSaplingType(i) == trackedType)
				return Material.SAPLING;
		}

		for (TreeType treeType : TreeType.values()) {
			if (treeType.toString() == trackedType) {
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

		if (Material.CHORUS_PLANT.toString() == trackedType)
			return Material.CHORUS_PLANT;

		return null;
	}

	public Material getTrackedCropMaterial(String trackedType) {
		if (Material.MELON_BLOCK.toString() == trackedType)
			return Material.MELON_BLOCK;
		else if (Material.PUMPKIN.toString() == trackedType)
			return Material.PUMPKIN;
		else {
			for (Material material : harvestableCrops.keySet()) {
				if (material.toString() == trackedType)
					return material;
			}
		}

		return null;
	}

	public Material getTrackedSaplingMaterial(String trackedType) {
		for (Byte i = 0; i < 6; i++) {
			if (getSaplingType(i) == trackedType)
				return Material.SAPLING;
		}

		return null;
	}

	public Material getTrackedTreeMaterial(String trackedType) {
		if (Material.CHORUS_PLANT.toString() == trackedType)
			return Material.CHORUS_PLANT;
		else {
			for (TreeType treeType : TreeType.values()) {
				if (treeType.toString() == trackedType) {
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

		return null;
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		Player p = e.getPlayer();

		Block block = e.getClickedBlock();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();

		p.sendMessage(ChatColor.GREEN + "Fier's fancy debug system:");
		WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
		
		if (!e.getPlayer().isSneaking()) {
			if (chunk != null) {
				ComponentBuilder hoverBuilder = new ComponentBuilder("ChunkID: " + chunk.getChunkID())
						.color(ChatColor.RED).append("\nChunkX: " + chunk.getChunkX()).color(ChatColor.RED)
						.append("\nChunkZ: " + chunk.getChunkZ()).color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Chunks").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			} else {
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Chunks").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
				return;
			}

			Crop crop = chunk.getCrop(x, y, z);
			if (crop != null) {
				ComponentBuilder hoverBuilder = new ComponentBuilder("CropID: " + crop.getCropID()).color(ChatColor.RED)
						.append("\nChunkID: " + crop.getChunkID()).color(ChatColor.RED).append("\nX: " + crop.getX())
						.color(ChatColor.RED).append("\nY: " + crop.getY()).color(ChatColor.RED)
						.append("\nZ: " + crop.getZ()).color(ChatColor.RED).append("\nCropType: " + crop.getCropType())
						.color(ChatColor.RED).append("\nCropState: " + crop.getCropState()).color(ChatColor.RED)
						.append("\nPlacer: " + crop.getPlacer()).color(ChatColor.RED)
						.append("\nTimeStamp: " + crop.getTimeStamp()).color(ChatColor.RED)
						.append("\nHarvestable: " + crop.getHarvestable()).color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Crops").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			} else {
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Crops").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			Sapling sapling = chunk.getSapling(x, y, z);
			if (sapling != null) {
				ComponentBuilder hoverBuilder = new ComponentBuilder("SaplingID: " + sapling.getSaplingID())
						.color(ChatColor.RED).append("\nChunkID: " + sapling.getChunkID()).color(ChatColor.RED)
						.append("\nX: " + sapling.getX()).color(ChatColor.RED).append("\nY: " + sapling.getY())
						.color(ChatColor.RED).append("\nZ: " + sapling.getZ()).color(ChatColor.RED)
						.append("\nSaplingType: " + sapling.getSaplingType()).color(ChatColor.RED)
						.append("\nPlacer: " + sapling.getPlacer()).color(ChatColor.RED)
						.append("\nTimeStamp: " + sapling.getTimeStamp()).color(ChatColor.RED)
						.append("\nHarvestable: " + sapling.getHarvestable()).color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Saplings").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			} else {
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Saplings").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			TreeComponent component = chunk.getTreeComponent(x, y, z);
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

				ComponentBuilder message = new ComponentBuilder("Tree").color(ChatColor.AQUA)
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
								.append("\nHarvestable: " + treeComponent.isHarvestable());

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree Component").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			} else {
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree Component").color(ChatColor.AQUA)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}
		} else {
			TreeComponent component = chunk.getTreeComponent(x, y, z);
			if (component != null) {
				Tree tree = CropControl.getDAO().getTree(component.getTreeID());

				for (TreeComponent treeComponent : CropControl.getDAO().getTreeComponents(tree)) {
					p.sendMessage(ChatColor.RED + "TreeComponentID: " + treeComponent.getTreeComponentID()
							+ " ChunkID: " + treeComponent.getChunkID() + " X: " + treeComponent.getX() + " Y: "
							+ treeComponent.getY() + " Z: " + treeComponent.getZ() + " TreeType: "
							+ treeComponent.getTreeType() + " Placer: " + treeComponent.getPlacer() + " Harvestable: "
							+ treeComponent.isHarvestable());
				}
			} else {
				p.sendMessage(ChatColor.RED + "No Tree Component info to show.");
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

	@EventHandler
	public void onPlaceBlock(BlockPlaceEvent e) {
		Block block = e.getBlock();

		Material blockMaterial = block.getType();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());

		if (harvestableCrops.containsKey(blockMaterial)) {
			// we placed a block overtop an existing crop. Will be handled by a break event?
			if (chunk.getCrop(x, y, z) != null) {
				CropControl.getPlugin().debug("Ignoring placement overtop a Crop at {0}, {1}, {2}", x, y, z);
				return;
			}
			
			// We've placed a crop!
			Crop.create(chunk, x, y, z, blockMaterial.toString(), getBaseCropState(blockMaterial),
					e.getPlayer().getUniqueId(), System.currentTimeMillis(), harvestableCrops.get(blockMaterial));
		} else if (blockMaterial == Material.SAPLING) {
			// we placed a block overtop an existing sapling. TODO: Do I need to remove sapling here, or will there be a break event?
			if (chunk.getSapling(x, y, z) != null) {
				CropControl.getPlugin().debug("Ignoring placement overtop a Sapling at {0}, {1}, {2}", x, y, z);
				return;
			}
			// We've placed a sapling!
			Sapling.create(chunk, x, y, z, getSaplingType(block.getData()),
					e.getPlayer().getUniqueId(), System.currentTimeMillis(), false);
		} else if (blockMaterial == Material.CHORUS_FLOWER) {
			if (CropControl.getDAO().isTracked(block) == true) {
				CropControl.getPlugin().debug("Ignoring placement overtop a tracked object at {0}, {1}, {2}", x, y, z);
				return;
			}

			// First register the "tree"
			Tree chorusPlant = Tree.create(chunk, x, y, z, Material.CHORUS_PLANT.toString(),
					e.getPlayer().getUniqueId(), System.currentTimeMillis());

			// Then the component in the tree.
			TreeComponent.create(chorusPlant, chunk, x, y, z, Material.CHORUS_PLANT.toString(),
					e.getPlayer().getUniqueId(), false);
		}

	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onCropGrow(BlockGrowEvent e) {
		Block block = e.getNewState().getBlock();

		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(CropControl.getPlugin(), new Runnable() {
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
						}
					}
				}
			}
		}, 1L);

	}

	@EventHandler
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

	@EventHandler
	public void onTreeGrow(StructureGrowEvent e) {
		Location structureLocation = e.getLocation();
		int x = structureLocation.getBlockX();
		int y = structureLocation.getBlockY();
		int z = structureLocation.getBlockZ();
		WorldChunk sourceChunk = CropControl.getDAO().getChunk(structureLocation.getChunk());
		
		List<BlockState> blocks = new ArrayList<BlockState>();

		Sapling sapling =  sourceChunk.getSapling(x, y, z);
		if (sapling != null) {
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
				if (state.getBlock().getType() != Material.SAPLING)
					continue;
				WorldChunk testChunk = CropControl.getDAO().getChunk(state.getChunk());
				Sapling testSapling = testChunk.getSapling(state.getX(), state.getY(), state.getZ());
				if (testSapling == null) {
					CropControl.getPlugin().debug("Found a sapling part of a recognized structure that wasn't itself tracked at {0}", state.getLocation());
					continue;
				}
				
				testSapling.setRemoved();
			}

			for (BlockState state : blocks) {
				WorldChunk partChunk = CropControl.getDAO().getChunk(state.getChunk());
				TreeComponent.create(tree, partChunk, state.getX(), state.getY(), state.getZ(), e.getSpecies().toString(),
						tree.getPlacer(), true);
			}
			return;
		}
		
		Crop crop =  sourceChunk.getCrop(x, y, z);
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

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		if (e.getPlayer() != null ) {
			handleBreak(e.getBlock(), BreakType.PLAYER, e.getPlayer().getUniqueId(), null);
		} else {
			handleBreak(e.getBlock(), BreakType.NATURAL, null, null);
		}
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent e) {
		handleBreak(e.getBlock(), BreakType.NATURAL, null, null);
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent e) {
		doExplodeHandler(e.blockList());
	}
	
	private void doExplodeHandler(List<Block> blockList) {
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
					handleBreak(block, BreakType.EXPLOSION, null, null);
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
					handleBreak(block, BreakType.EXPLOSION, null, null);
				}
				continue;
			}
			
			handleBreak(block, BreakType.EXPLOSION, null, null);
		}
		// if this has anything, it has a set of sets of parts of connected to-drops. Not a single stem; so need the
		//   code to not require a single start as there isn't one.
		if (toBreakList.size() > 0) {
			// TODO: null first param.
			handleBreak(null, BreakType.EXPLOSION, null, toBreakList);
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e) {
		doExplodeHandler(e.blockList());
	}

	@EventHandler
	public void onLeafDecay(LeavesDecayEvent e) {
		handleBreak(e.getBlock(), BreakType.NATURAL, null, null);
	}

	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent e) {
		handleBreak(e.getBlock(), BreakType.NATURAL, null, null);
	}

	@EventHandler
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent e) {
		UUID uuid = null;
		if (e.getPlayer() != null) {
			uuid = e.getPlayer().getUniqueId();			
		}
		
		if (e.getBucket() == Material.LAVA_BUCKET)
			handleBreak(e.getBlockClicked().getRelative(e.getBlockFace()), BreakType.LAVA, uuid, null);
		else if (e.getBucket() == Material.WATER_BUCKET)
			handleBreak(e.getBlockClicked().getRelative(e.getBlockFace()), BreakType.WATER, uuid, null);
	}

	@EventHandler
	public void onBlockFromTo(BlockFromToEvent e) {
		if (e.getToBlock().getType() == Material.WATER || e.getToBlock().getType() == Material.STATIONARY_WATER) {
			handleBreak(e.getBlock(), BreakType.WATER, null, null);
		} else if (e.getToBlock().getType() == Material.LAVA || e.getToBlock().getType() == Material.STATIONARY_LAVA) {
			handleBreak(e.getBlock(), BreakType.LAVA, null, null);
		}
	}

	@EventHandler
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
					handleBreak(block, BreakType.PISTON, null, null);

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
								}
							}
					);
				} else {
					movements.put(e.getBlock().getLocation().distance(block.getLocation()), 
							new Runnable() {
								@Override
								public void run() {
									component.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
									tree.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
								}
							}
					);
				}
				// if tree base, move the whole tree

				continue;
			} 
			
			if (block.getType() == Material.SOIL) { // TODO: Mycellium, sand, etc.
				handleBreak(block.getRelative(BlockFace.UP), BreakType.PISTON, null, null);
			} else {
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
	
	@EventHandler
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
					handleBreak(block, BreakType.PISTON, null, null);

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
								}
							}
					);
				} else {
					movements.put(e.getBlock().getLocation().distance(block.getLocation()), 
							new Runnable() {
								@Override
								public void run() {
									component.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
									tree.updateLocation(newChunk.getChunkID(), newX, newY, newZ);
								}
							}
					);
				}
				// if tree base, move the whole tree

				continue;
			} else if (block.getType() == Material.SOIL) {
				handleBreak(block.getRelative(BlockFace.UP), BreakType.PISTON, null, null);
			} else {
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
		Bukkit.getScheduler().runTaskLater(CropControl.getPlugin(),
				new Runnable() {
					@Override
					public void run() {
						if (startBlock != null) { // no altBlocks.
							WorldChunk chunk = CropControl.getDAO().getChunk(startBlock.getChunk());
							int x = startBlock.getX();
							int y = startBlock.getY();
							int z = startBlock.getZ();
							
							Crop crop = chunk.getCrop(x, y, z);
							if (crop != null) {
								Material type = getTrackedCropMaterial(crop.getCropType());
								if (type != startBlock.getType()) {
									CropControl.getPlugin().debug("Ignoring mismatched Crop track vs. actual type {0}, {1}, {2}", x, y, z);
									return;
								}
	
								if (type == Material.SUGAR_CANE_BLOCK || type == Material.CACTUS) {
									for (Location location : returnUpwardsBlocks(startBlock, type)) {
										Bukkit.broadcastMessage(
												ChatColor.YELLOW + "Broke Crop (" + breakType.toString() + ")");
										
										Crop upCrop = CropControl.getDAO().getChunk(location.getChunk())
												.getCrop(location.getBlockX(), location.getBlockY(), location.getBlockZ());

										drop(location.getBlock(), upCrop, breaker, breakType);

										upCrop.setRemoved();
									}
								} else {
									Bukkit.broadcastMessage(ChatColor.YELLOW + "Broke Crop (" + breakType.toString() + ")");
	
									drop(startBlock, crop, breaker, breakType);
	
									crop.setRemoved();
								}
								
								return;
							}
							
							Sapling sapling = chunk.getSapling(x, y, z);
							if (sapling != null) {
								if (getTrackedSaplingMaterial(sapling.getSaplingType()) != startBlock.getType()) {
									CropControl.getPlugin().debug("Ignoring mismatched Sapling track vs. actual type {0}, {1}, {2}", x, y, z);
									return;
								}
	
								Bukkit.broadcastMessage(ChatColor.GREEN + "Broke Sapling (" + breakType.toString() + ")");
	
								drop(startBlock, sapling, breaker, breakType);
	
								sapling.setRemoved();
								return;
							}
							
							TreeComponent treeComponent = chunk.getTreeComponent(x, y, z);
							if (treeComponent != null) {
								Tree tree = CropControl.getDAO().getTree(treeComponent);
								Material type = getTrackedTreeMaterial(treeComponent.getTreeType());
								if (type != 
										(startBlock.getType() == Material.LEAVES ? Material.LOG : 
										startBlock.getType() == Material.LEAVES_2 ? Material.LOG_2 :
										startBlock.getType() == Material.CHORUS_FLOWER	? Material.CHORUS_PLANT : 
										startBlock.getType())) {
									CropControl.getPlugin().debug("Ignoring mismatched Tree Component track vs. actual type {0}, {1}, {2}", x, y, z);
									return;
								}
	
								if (type == Material.CHORUS_PLANT) {
									for (Location location : returnUpwardsChorusBlocks(startBlock)) {
										Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Broke Tree Component ("
												+ breakType.toString() + ")");

										TreeComponent upComponent = CropControl.getDAO().getChunk(location.getChunk())
												.getTreeComponent(location.getBlockX(), location.getBlockY(), location.getBlockZ());
										
										drop(location.getBlock(), upComponent, breaker, breakType);

										upComponent.setRemoved();

									}
								} else {
									Bukkit.broadcastMessage(
											ChatColor.DARK_GREEN + "Broke Tree Component (" + breakType.toString() + ")");
	
									drop(startBlock, treeComponent, breaker, breakType);
	
									treeComponent.setRemoved();
								}
								if (CropControl.getDAO().getTreeComponents(tree).isEmpty()) {
									Bukkit.broadcastMessage(
											ChatColor.AQUA + "Broke Tree (" + breakType.toString() + ")");

									tree.setRemoved();										
								}
								return;
							}
							
							CropControl.getPlugin().debug("Failed to find matching element at {0}, {1}, {2}", x, y, z);
						} else {
							for (Location location : altBlocks) {
								Block block = location.getBlock();
								int x = block.getX();
								int y = block.getY();
								int z = block.getZ();
								WorldChunk chunk = CropControl.getDAO().getChunk(block.getChunk());
								
								Crop crop = chunk.getCrop(x, y, z);
								if (crop != null) {
									Bukkit.broadcastMessage(
											ChatColor.YELLOW + "Broke Crop (" + breakType.toString() + ")");
	
									drop(location.getBlock(), crop, breaker, breakType);
									
									crop.setRemoved();
									continue;
								}
								
								TreeComponent treeComponent = chunk.getTreeComponent(x, y, z);
								
								if (treeComponent != null) {
									Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Broke Tree Component ("
											+ breakType.toString() + ")");
	
									drop(location.getBlock(), treeComponent, breaker, breakType);
	
									treeComponent.setRemoved();
									
									if (CropControl.getDAO().getTreeComponents(treeComponent.getTreeID()).isEmpty()) {
										Bukkit.broadcastMessage(
												ChatColor.AQUA + "Broke Tree (" + breakType.toString() + ")");
	
										CropControl.getDAO().getTree(treeComponent).setRemoved();
									}
									
									continue;
								}
								
								CropControl.getPlugin().debug("Encountered altBlock to drop that isn't a crop or component at {0}, {1}, {2}", x, y, z);
							}
						}
					}
				}, 1L);
	}

	public void drop(Block block, Crop crop, UUID player, BreakType breakType) {
		Biome biome = block.getBiome();
		boolean byPlayer = player != null;
		boolean placed = byPlayer && player.equals(crop.getPlacer());
		boolean harvestable = crop.getHarvestable();
		ItemStack toolUsed = null;
		if (byPlayer) {
			Player p = Bukkit.getPlayer(player);
			if (p != null) {
				toolUsed = p.getInventory().getItemInMainHand();
			}
		}
		
		 
	}
	
	public void drop(Block block, Sapling sapling, UUID player, BreakType breakType) {
		
	}
	
	public void drop(Block block, TreeComponent component, UUID player, BreakType breakType) {
		
	}
	
	public void drop(Block block, Tree tree, UUID player, BreakType breakType) {
		
	}
	
	public void realDrop(Location location, List<ItemStack> items) {
		for (ItemStack item : items) {
			location.getWorld().dropItem(location.add(0.5, 0.5, 0.5), item).setVelocity(new Vector(0, 0.05, 0));
		}
	}

	private enum BreakType {
		PLAYER, WATER, LAVA, PISTON, EXPLOSION, NATURAL;
	}

	/*
	 * 
	 * End of Block Break Tracking
	 * 
	 * Start of Chunk Loading/Unloading
	 * 
	 */

	/*
	 * This is where we should (in my humble opinion) be getting data from the
	 * DB, Such that when a chunk is loaded we load all of the crops, saplings,
	 * trees & tree componenets. And therefore when a chunk is unloaded we save
	 * it all to the DB,
	 * 
	 * Or something along those lines.
	 */
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		Chunk chunk = e.getChunk();
		
		CropControl.getDAO().getChunk(chunk);

	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		Chunk chunk = e.getChunk();
		
		CropControl.getDAO().unloadChunk(chunk);
	}

}
