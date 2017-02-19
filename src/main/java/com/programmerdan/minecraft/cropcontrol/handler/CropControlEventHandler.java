package com.programmerdan.minecraft.cropcontrol.handler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;

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
public class CropControlEventHandler implements Listener
{
	private FileConfiguration config;

	private List<Crop> crops;

	private List<WorldChunk> chunks;

	private List<Sapling> saplings;

	private List<Tree> trees;

	private List<TreeComponent> treeComponents;

	/**
	 * List of materials that are crops, and if we track specific states
	 * belonging to that material.
	 */
	private Map<Material, Boolean> harvestableCrops;

	public CropControlEventHandler(FileConfiguration config)
	{
		this.config = config;

		crops = new ArrayList<Crop>();

		chunks = new ArrayList<WorldChunk>();

		saplings = new ArrayList<Sapling>();

		trees = new ArrayList<Tree>();

		treeComponents = new ArrayList<TreeComponent>();

		harvestableCrops = new HashMap<Material, Boolean>();

		fillHarvestableCropsList();

		loadExistingChunks();
	}

	public void loadExistingChunks()
	{
		for (World world : CropControl.getPlugin().getServer().getWorlds())
		{
			for (Chunk loadedChunk : world.getLoadedChunks())
			{
				if (getChunk(loadedChunk.getWorld().getUID(), loadedChunk.getX(), loadedChunk.getZ()) != null)
					continue;

				chunks.add(new WorldChunk(new BigInteger(chunks.size() + ""), loadedChunk.getWorld().getUID(), loadedChunk.getX(), loadedChunk.getZ()));
			}
		}
	}

	public void fillHarvestableCropsList()
	{
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
		harvestableCrops.put(Material.CHORUS_FLOWER, false);
	}

	public String getBaseCropState(Material material)
	{
		switch (material)
		{
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
			case CHORUS_FLOWER:
				return null;
			default:
				return "SEEDED";
		}
	}

	public String getCropState(BlockState blockState)
	{
		switch (blockState.getBlock().getType())
		{
			case COCOA:
				return ((CocoaPlant) blockState.getData()).getSize().toString();
			case NETHER_WARTS:
				return ((NetherWarts) blockState.getData()).getState().toString();
			case MELON_STEM:
				return (int) blockState.getBlock().getData() + "";
			case PUMPKIN_STEM:
				return (int) blockState.getBlock().getData() + "";
			case CHORUS_FRUIT:
				return null;
			case CHORUS_FLOWER:
				return null;
			default:
				return ((Crops) blockState.getData()).getState().toString();
		}
	}

	public String getSaplingType(Byte data)
	{
		switch (data)
		{
			case 0:
				return "OAK";
			case 1:
				return "SPRUCE";
			case 2:
				return "BIRCH";
			case 3:
				return "JUNGLE";
			case 4:
				return "ACACIA";
			case 5:
				return "DARK_OAK";
			default:
				return null;
		}
	}

	public Crop getCrop(int x, int y, int z, BigInteger chunkID)
	{
		for (Crop crop : crops)
		{
			if (crop.getX() == x && crop.getY() == y && crop.getZ() == z && crop.getChunkID() == chunkID)
				return crop;
		}

		return null;
	}

	public WorldChunk getChunk(UUID worldID, int chunkX, int chunkZ)
	{
		for (WorldChunk worldChunk : chunks)
		{
			if (worldChunk.getWorldID() == worldID && worldChunk.getChunkX() == chunkX && worldChunk.getChunkZ() == chunkZ)
				return worldChunk;
		}

		return null;
	}

	public WorldChunk getChunk(Chunk chunk)
	{
		for (WorldChunk worldChunk : chunks)
		{
			if (worldChunk.getWorldID() == chunk.getWorld().getUID() && worldChunk.getChunkX() == chunk.getX() && worldChunk.getChunkZ() == chunk.getZ())
				return worldChunk;
		}

		return null;
	}

	public Sapling getSapling(int x, int y, int z, BigInteger chunkID)
	{
		for (Sapling sapling : saplings)
		{
			if (sapling.getX() == x && sapling.getY() == y && sapling.getZ() == z && sapling.getChunkID() == chunkID)
				return sapling;
		}

		return null;
	}

	public Tree getTree(BigInteger treeID)
	{
		for (Tree tree : trees)
		{
			if (tree.getTreeID() == treeID)
				return tree;
		}

		return null;
	}

	public Tree getTree(int x, int y, int z, BigInteger chunkID)
	{
		for (Tree tree : trees)
		{
			if (tree.getX() == x && tree.getY() == y && tree.getZ() == z && tree.getChunkID() == chunkID)
				return tree;
		}

		return null;
	}

	public List<TreeComponent> getTreeComponenets(BigInteger treeID)
	{
		List<TreeComponent> trees = new ArrayList<TreeComponent>();

		for (TreeComponent treeComponent : treeComponents)
		{
			if (treeComponent.getTreeID() == treeID)
				trees.add(treeComponent);
		}

		return trees;
	}

	public TreeComponent getTreeComponent(int x, int y, int z, BigInteger chunkID)
	{
		for (TreeComponent treeComponent : treeComponents)
		{
			if (treeComponent.getX() == x && treeComponent.getY() == y && treeComponent.getZ() == z && treeComponent.getChunkID() == chunkID)
				return treeComponent;
		}

		return null;
	}

	@EventHandler
	public void onPlaceBlock(BlockPlaceEvent e)
	{
		Block block = e.getBlock();

		Material blockMaterial = block.getType();

		if (harvestableCrops.containsKey(blockMaterial))
		{
			if (getCrop(block.getX(), block.getY(), block.getZ(), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
				return;
			// TODO Manage correct CropID with DB support
			crops.add(new Crop(new BigInteger(crops.size() + ""), getChunk(block.getChunk().getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), blockMaterial.toString(), getBaseCropState(blockMaterial), e.getPlayer().getUniqueId(), System.currentTimeMillis(), harvestableCrops.get(blockMaterial)));
		}
		else if (blockMaterial == Material.SAPLING)
		{
			if (getSapling(block.getX(), block.getY(), block.getZ(), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
				return;
			// TODO Manage correct SaplingID with DB support
			saplings.add(new Sapling(new BigInteger(saplings.size() + ""), getChunk(block.getChunk().getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), getSaplingType(block.getData()), e.getPlayer().getUniqueId(), System.currentTimeMillis(), false));
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e)
	{
		if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		Player p = e.getPlayer();

		Block block = e.getClickedBlock();

		p.sendMessage(ChatColor.GREEN + "Fier's fancy debug system:");

		if (!e.getPlayer().isSneaking())
		{
			if (getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()) != null)
			{
				WorldChunk chunk = getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ());
				ComponentBuilder hoverBuilder = new ComponentBuilder("ChunkID: " + chunk.getChunkID()).color(ChatColor.RED).append("\nChunkX: " + chunk.getChunkX()).color(ChatColor.RED).append("\nChunkZ: " + chunk.getChunkZ()).color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Chunks").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}
			else
			{
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Chunks").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			if (getCrop(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
			{
				Crop crop = getCrop(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID());
				ComponentBuilder hoverBuilder = new ComponentBuilder("CropID: " + crop.getCropID()).color(ChatColor.RED).append("\nChunkID: " + crop.getChunkID()).color(ChatColor.RED).append("\nX: " + crop.getX()).color(ChatColor.RED).append("\nY: " + crop.getY()).color(ChatColor.RED).append("\nZ: " + crop.getZ()).color(ChatColor.RED).append("\nCropType: " + crop.getCropType()).color(ChatColor.RED).append("\nCropState: " + crop.getCropState()).color(ChatColor.RED).append("\nPlacer: " + crop.getPlacer()).color(ChatColor.RED).append("\nTimeStamp: " + crop.getTimeStamp()).color(ChatColor.RED).append("\nHarvestable: " + crop.getHarvestable()).color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Crops").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}
			else
			{
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Crops").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			if (getSapling(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
			{
				Sapling sapling = getSapling(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID());
				ComponentBuilder hoverBuilder = new ComponentBuilder("SaplingID: " + sapling.getSaplingID()).color(ChatColor.RED).append("\nChunkID: " + sapling.getChunkID()).color(ChatColor.RED).append("\nX: " + sapling.getX()).color(ChatColor.RED).append("\nY: " + sapling.getY()).color(ChatColor.RED).append("\nZ: " + sapling.getZ()).color(ChatColor.RED).append("\nSaplingType: " + sapling.getSaplingType()).color(ChatColor.RED).append("\nPlacer: " + sapling.getPlacer()).color(ChatColor.RED).append("\nTimeStamp: " + sapling.getTimeStamp()).color(ChatColor.RED).append("\nHarvestable: " + sapling.getHarvestable()).color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Saplings").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}
			else
			{
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Saplings").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			if (getTreeComponent(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
			{
				Tree tree = getTree(getTreeComponent(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()).getTreeID());
				ComponentBuilder hoverBuilder = new ComponentBuilder("TreeID: " + tree.getTreeID()).color(ChatColor.RED).append("\nChunkID: " + tree.getChunkID()).color(ChatColor.RED).append("\nX: " + tree.getX()).color(ChatColor.RED).append("\nY: " + tree.getY()).color(ChatColor.RED).append("\nZ: " + tree.getZ()).color(ChatColor.RED).append("\nTreeType: " + tree.getTreeType()).color(ChatColor.RED).append("\nPlacer: " + tree.getPlacer()).color(ChatColor.RED).append("\nTimeStamp: " + tree.getTimeStamp()).color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}
			else
			{
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}

			if (getTreeComponent(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
			{
				TreeComponent treeComponent = getTreeComponent(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID());

				ComponentBuilder hoverBuilder = new ComponentBuilder("TreeComponentID: " + treeComponent.getTreeComponentID()).color(ChatColor.RED).append("\nChunkID: " + treeComponent.getChunkID()).append("\nX: " + treeComponent.getX()).append("\nY: " + treeComponent.getY()).append("\nZ: " + treeComponent.getZ()).append("\nTreeType: " + treeComponent.getTreeType()).append("\nPlacer: " + treeComponent.getPlacer()).append("\nHarvestable: " + treeComponent.isHarvestable());

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree Component").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}
			else
			{
				ComponentBuilder hoverBuilder = new ComponentBuilder("No info to show.").color(ChatColor.RED);

				BaseComponent[] hoverMessage = hoverBuilder.create();

				ComponentBuilder message = new ComponentBuilder("Tree Component").color(ChatColor.AQUA).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage));

				p.spigot().sendMessage(message.create());
			}
		}
		else
		{
			if (getTreeComponent(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
			{

				TreeComponent orgignalTreeComponent = getTreeComponent(block.getX(), block.getY(), (block.getZ()), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID());
				Tree tree = getTree(orgignalTreeComponent.getTreeID());

				for (TreeComponent treeComponent : getTreeComponenets(tree.getTreeID()))
				{
					p.sendMessage(ChatColor.RED + "TreeComponentID: " + treeComponent.getTreeComponentID() + " ChunkID: " + treeComponent.getChunkID() + " X: " + treeComponent.getX() + " Y: " + treeComponent.getY() + " Z: " + treeComponent.getZ() + " TreeType: " + treeComponent.getTreeType() + " Placer: " + treeComponent.getPlacer() + " Harvestable: " + treeComponent.isHarvestable());
				}
			}
			else
			{
				p.sendMessage(ChatColor.RED + "No Tree Component info to show.");
			}
		}

	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onCropGrow(BlockGrowEvent e)
	{
		Block block = e.getNewState().getBlock();

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(CropControl.getPlugin(), new Runnable()
		{
			@Override
			public void run()
			{
				if (getCrop(block.getX(), block.getY(), block.getZ(), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
				{
					getCrop(block.getX(), block.getY(), block.getZ(), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()).setCropState(getCropState(e.getNewState()));
				}
				else
				{
					if (block.getType() == Material.MELON_BLOCK || block.getType() == Material.PUMPKIN)
					{
						BlockFace[] directions = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };

						for (BlockFace blockFace : directions)
						{
							Block otherBlock = block.getRelative(blockFace);

							if (getCrop(otherBlock.getX(), otherBlock.getY(), otherBlock.getZ(), getChunk(otherBlock.getWorld().getUID(), otherBlock.getChunk().getX(), otherBlock.getChunk().getZ()).getChunkID()) != null)
							{
								UUID placerUUID = getCrop(otherBlock.getX(), otherBlock.getY(), otherBlock.getZ(), getChunk(otherBlock.getWorld().getUID(), otherBlock.getChunk().getX(), otherBlock.getChunk().getZ()).getChunkID()).getPlacer();
								// TODO Manage correct CropID with DB support
								crops.add(new Crop(new BigInteger(crops.size() + ""), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), block.getType().toString(), null, placerUUID, System.currentTimeMillis(), true));
							}
						}
					}
					else if (block.getType() == Material.CACTUS || block.getType() == Material.SUGAR_CANE_BLOCK)
					{
						Block otherBlock = block.getRelative(BlockFace.DOWN);

						if (getCrop(otherBlock.getX(), otherBlock.getY(), otherBlock.getZ(), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()) != null)
						{
							UUID placerUUID = getCrop(otherBlock.getX(), otherBlock.getY(), otherBlock.getZ(), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()).getPlacer();
							// TODO Manage correct CropID with DB support
							crops.add(new Crop(new BigInteger(crops.size() + ""), getChunk(block.getChunk().getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), otherBlock.getType().toString(), null, placerUUID, System.currentTimeMillis(), true));
						}
					}
				}
			}
		}, 1L);

	}

	@EventHandler
	public void onBlockSpread(BlockSpreadEvent e)
	{
		Block source = e.getSource();

		Block block = e.getBlock();

		if (!harvestableCrops.containsKey(source.getType()))
			return;

		if (getCrop(source.getX(), source.getY(), source.getZ(), getChunk(source.getWorld().getUID(), source.getChunk().getX(), source.getChunk().getZ()).getChunkID()) != null)
		{
			UUID placerUUID = getCrop(source.getX(), source.getY(), source.getZ(), getChunk(source.getWorld().getUID(), source.getChunk().getX(), source.getChunk().getZ()).getChunkID()).getPlacer();
			// TODO Manage correct CropID with DB support
			crops.add(new Crop(new BigInteger(crops.size() + ""), getChunk(block.getChunk().getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), source.getType().toString(), null, placerUUID, System.currentTimeMillis(), true));
		}
	}

	@EventHandler
	public void onTreeGrow(StructureGrowEvent e)
	{
		Location structureLocation = e.getLocation();

		List<BlockState> blocks = new ArrayList<BlockState>();

		if (getSapling(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()) != null)
		{
			// Because dirt & saplings are part of the structure
			for (BlockState state : e.getBlocks())
			{
				if (state.getType() == Material.LOG || state.getType() == Material.LOG_2 || state.getType() == Material.LEAVES || state.getType() == Material.LEAVES_2)
					blocks.add(state);
			}

			if (blocks.size() == 0)
				return;

			// TODO Fix ID here.
			trees.add(new Tree(new BigInteger(trees.size() + ""), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID(), structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), e.getSpecies().toString(), getSapling(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()).getPlacer(), System.currentTimeMillis()));

			//Done in the case of Multiple saplings (Big Jungle trees etc)
			for (BlockState state : e.getBlocks())
			{
				if (state.getBlock().getType() != Material.SAPLING)
					continue;
				
				if (getSapling(state.getX(), state.getY(), state.getZ(), getChunk(state.getWorld().getUID(), state.getChunk().getX(), state.getChunk().getZ()).getChunkID()) == null)
					return;
				
				saplings.remove(getSapling(state.getX(), state.getY(), state.getZ(), getChunk(state.getWorld().getUID(), state.getChunk().getX(), state.getChunk().getZ()).getChunkID()));
			}
			
			for (BlockState state : blocks)
			{
				// TODO Fix ID here.
				treeComponents.add(new TreeComponent(new BigInteger(treeComponents.size() + ""), getTree(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()).getTreeID(), getChunk(state.getWorld().getUID(), state.getChunk().getX(), state.getChunk().getZ()).getChunkID(), state.getX(), state.getY(), state.getZ(), e.getSpecies().toString(), getTree(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()).getPlacer(), true));
			}
		}
		else if (getCrop(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()) != null)
		{
			// Because dirt & saplings are part of the structure
			for (BlockState state : e.getBlocks())
			{
				if (state.getType() == Material.HUGE_MUSHROOM_1 || state.getType() == Material.HUGE_MUSHROOM_2)
					blocks.add(state);
			}

			if (blocks.size() == 0)
				return;

			// TODO Fix ID here.
			trees.add(new Tree(new BigInteger(trees.size() + ""), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID(), structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), e.getSpecies().toString(), getCrop(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()).getPlacer(), System.currentTimeMillis()));

			crops.remove(getCrop(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()));

			for (BlockState state : blocks)
			{
				// TODO Fix ID here.
				treeComponents.add(new TreeComponent(new BigInteger(treeComponents.size() + ""), getTree(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()).getTreeID(), getChunk(state.getWorld().getUID(), state.getChunk().getX(), state.getChunk().getZ()).getChunkID(), state.getX(), state.getY(), state.getZ(), e.getSpecies().toString(), getTree(structureLocation.getBlockX(), structureLocation.getBlockY(), structureLocation.getBlockZ(), getChunk(structureLocation.getWorld().getUID(), structureLocation.getChunk().getX(), structureLocation.getChunk().getZ()).getChunkID()).getPlacer(), true));
			}
		}
	}

	public void breakTracked(Block block, String howBroken)
	{
		if (getCrop(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
		{
			Crop crop = getCrop(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID());

			Bukkit.broadcastMessage("Broke a Crop " + howBroken);

			crops.remove(crop);
		}
		else if (getSapling(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
		{
			Sapling sapling = getSapling(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID());

			Bukkit.broadcastMessage("Broke a Sapling " + howBroken);

			saplings.remove(sapling);
		}
		else if (getTreeComponent(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
		{
			TreeComponent treeComponent = getTreeComponent(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID());

			Tree tree = getTree(getTreeComponent(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()).getTreeID());

			treeComponents.remove(treeComponent);

			Bukkit.broadcastMessage("Broke a Tree Component " + howBroken + (getTreeComponenets(tree.getTreeID()).size() == 0 ? " (Completely)" : ""));

			if (getTreeComponenets(tree.getTreeID()).size() == 0)
			{
				trees.remove(tree);
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e)
	{
		breakTracked(e.getBlock(), "by breaking it with a hand/tool.");
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent e)
	{
		breakTracked(e.getBlock(), "by burning it.");
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent e)
	{
		for (Block block : e.blockList())
		{
			breakTracked(block, "by blowing it up.");
		}
	}
	
	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e)
	{
		for (Block block : e.blockList())
		{
			breakTracked(block, "by blowing it up.");
		}
	}

	//TODO Handle these. If a piston moves a tree block, that would cause issues unless handled.
//	@EventHandler
//	public void onPistionExtend(BlockPistonExtendEvent e)
//	{
//		e.g
//		
//		Bukkit.broadcastMessage("Tripped: PistonExtend");
//	}
//
//	@EventHandler
//	public void onPistonRetract(BlockPistonRetractEvent e)
//	{
//		for (Block block : e.getBlocks())
//		{
//			breakTracked(e.getBlock(), e.getEventName());
//		}
//	}
	
	@EventHandler
	public void onBlockPhysics(BlockPhysicsEvent e)
	{		
		if (e.getChangedType() == Material.SOIL)
			breakTracked(e.getBlock(), "by breaking/killing the soil.");
		else if (e.getChangedType() == Material.PISTON_BASE || e.getChangedType() == Material.PISTON_STICKY_BASE || e.getChangedType() == Material.PISTON_EXTENSION || e.getChangedType() == Material.PISTON_MOVING_PIECE)
			breakTracked(e.getBlock(), "by using a piston on it.");
		else if (e.getChangedType() == Material.WATER || e.getChangedType() == Material.STATIONARY_WATER || e.getChangedType() == Material.LAVA || e.getChangedType() == Material.STATIONARY_LAVA)
			breakTracked(e.getBlock(), "by using water/lava on it.");
		else if (e.getChangedType() == Material.CHORUS_PLANT)
			breakTracked(e.getBlock(), "by breaking the supporting Chorus Plant.");
		else if (e.getChangedType() == Material.SUGAR_CANE)
			breakTracked(e.getBlock(), "by breaking the supporting Sugar Cane.");
		else if (e.getChangedType() == Material.CACTUS)
			breakTracked(e.getBlock(), "by breaking the supporting Cactus.");
	}
	
	@EventHandler
	public void onLeafDecay(LeavesDecayEvent e)
	{
		breakTracked(e.getBlock(), "by letting the leaf decay.");
	}

	/*
	 * This is where we should (in my humble opinion) be getting data from the
	 * DB, Such that when a chunk is loaded we load all of the crops, saplings,
	 * trees & tree componenets. And therefore when a chunk is unloaded we save
	 * it all to the DB,
	 * 
	 * Or something along those lines.
	 */
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e)
	{
		Chunk chunk = e.getChunk();

		if (getChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()) != null)
			return;
		// TODO Add in actual chunkID support via DB
		chunks.add(new WorldChunk(new BigInteger(chunks.size() + ""), chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()));

	}

	// TODO Uncomment
	// @EventHandler
	// public void onChunkUnload(ChunkUnloadEvent e)
	// {
	// Chunk chunk = e.getChunk();
	//
	// if (getChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()) ==
	// null)
	// return;
	//
	// chunks.remove(getChunk(chunk.getWorld().getUID(), chunk.getX(),
	// chunk.getZ()));
	// }

}
