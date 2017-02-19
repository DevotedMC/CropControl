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
import org.bukkit.TreeType;
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
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
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

	public String getSaplingType(Byte data)
	{
		switch (data)
		{
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

	public WorldChunk getChunk(BigInteger chunkID)
	{
		for (WorldChunk worldChunk : chunks)
		{
			if (worldChunk.getChunkID() == chunkID)
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
		else if (blockMaterial == Material.CHORUS_FLOWER)
		{
			if (isTracked(block) == true)
				return;

			// TODO Fix ID
			trees.add(new Tree(new BigInteger(trees.size() + ""), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), Material.CHORUS_PLANT.toString(), e.getPlayer().getUniqueId(), System.currentTimeMillis()));

			// TODO Fix ID
			treeComponents.add(new TreeComponent(new BigInteger(treeComponents.size() + ""), getTree(block.getX(), block.getY(), block.getZ(), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID()).getTreeID(), getChunk(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), Material.CHORUS_PLANT.toString(), e.getPlayer().getUniqueId(), false));
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

		if (!harvestableCrops.containsKey(source.getType()) && source.getType() != Material.CHORUS_FLOWER && source.getType() != Material.CHORUS_PLANT && block.getType() != Material.CHORUS_FLOWER && block.getType() != Material.CHORUS_PLANT)
			return;

		if (getCrop(source.getX(), source.getY(), source.getZ(), getChunk(source.getWorld().getUID(), source.getChunk().getX(), source.getChunk().getZ()).getChunkID()) != null)
		{
			UUID placerUUID = getCrop(source.getX(), source.getY(), source.getZ(), getChunk(source.getWorld().getUID(), source.getChunk().getX(), source.getChunk().getZ()).getChunkID()).getPlacer();
			// TODO Manage correct CropID with DB support
			crops.add(new Crop(new BigInteger(crops.size() + ""), getChunk(block.getChunk().getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), source.getType().toString(), null, placerUUID, System.currentTimeMillis(), true));
		}
		else if (getTreeComponent(source.getX(), source.getY(), source.getZ(), getChunk(source.getWorld().getUID(), source.getChunk().getX(), source.getChunk().getZ()).getChunkID()) != null)
		{
			TreeComponent treeComponent = getTreeComponent(source.getX(), source.getY(), source.getZ(), getChunk(source.getWorld().getUID(), source.getChunk().getX(), source.getChunk().getZ()).getChunkID());

			treeComponent.setHarvestable(true);

			// TODO Manage correct ID with DB support
			treeComponents.add(new TreeComponent(new BigInteger(treeComponents.size() + ""), treeComponent.getTreeID(), getChunk(block.getChunk().getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ()).getChunkID(), block.getX(), block.getY(), block.getZ(), Material.CHORUS_PLANT.toString(), treeComponent.getPlacer(), true));

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

			// Done in the case of Multiple saplings (Big Jungle trees etc)
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

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e)
	{
		floodTracker(e.getBlock(), BreakType.PLAYER);
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent e)
	{
		floodTracker(e.getBlock(), BreakType.NATURAL);
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent e)
	{
		for (Block block : e.blockList())
		{
			floodTracker(block, BreakType.EXPLOSION);
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent e)
	{
		for (Block block : e.blockList())
		{
			floodTracker(block, BreakType.EXPLOSION);
		}
	}

	@EventHandler
	public void onLeafDecay(LeavesDecayEvent e)
	{
		floodTracker(e.getBlock(), BreakType.NATURAL);
	}

	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent e)
	{
		floodTracker(e.getBlock(), BreakType.NATURAL);
	}

	@EventHandler
	public void onBlockFromTo(BlockFromToEvent e)
	{
		if (e.getBlock().getType() == Material.WATER || e.getBlock().getType() == Material.STATIONARY_WATER)
		{
			floodTracker(e.getBlock(), BreakType.WATER);
			
		}
		else if (e.getBlock().getType() == Material.LAVA || e.getBlock().getType() == Material.STATIONARY_LAVA)
		{
			floodTracker(e.getBlock(), BreakType.LAVA);
		}
	}

	// TODO Handle these. If a piston moves a tree block, that would cause
	// issues unless handled.
	// @EventHandler
	// public void onPistionExtend(BlockPistonExtendEvent e)
	// {
	//
	// Bukkit.broadcastMessage("Tripped: PistonExtend");
	// }
	//
	// @EventHandler
	// public void onPistonRetract(BlockPistonRetractEvent e)
	// {
	// for (Block block : e.getBlocks())
	// {
	// breakTracked(e.getBlock(), e.getEventName());
	// }
	// }

	public Material getTrackedTypeMaterial(String trackedType)
	{
		for (Material material : harvestableCrops.keySet())
		{
			if (material.toString() == trackedType)
				return material;
		}

		if (Material.MELON_BLOCK.toString() == trackedType)
			return Material.MELON_BLOCK;
		else if (Material.PUMPKIN.toString() == trackedType)
			return Material.PUMPKIN;

		for (Byte i = 0; i < 6; i++)
		{
			if (getSaplingType(i) == trackedType)
				return Material.SAPLING;
		}

		for (TreeType treeType : TreeType.values())
		{
			if (treeType.toString() == trackedType)
			{
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

	public Material getTrackedCropMaterial(String trackedType)
	{
		if (Material.MELON_BLOCK.toString() == trackedType)
			return Material.MELON_BLOCK;
		else if (Material.PUMPKIN.toString() == trackedType)
			return Material.PUMPKIN;
		else
		{
			for (Material material : harvestableCrops.keySet())
			{
				if (material.toString() == trackedType)
					return material;
			}
		}

		return null;
	}

	public Material getTrackedSaplingMaterial(String trackedType)
	{
		for (Byte i = 0; i < 6; i++)
		{
			if (getSaplingType(i) == trackedType)
				return Material.SAPLING;
		}

		return null;
	}

	public Material getTrackedTreeMaterial(String trackedType)
	{
		if (Material.CHORUS_PLANT.toString() == trackedType)
			return Material.CHORUS_PLANT;
		else
		{
			for (TreeType treeType : TreeType.values())
			{
				if (treeType.toString() == trackedType)
				{
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

	public boolean isTracked(Block block)
	{
		if (getCrop(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
			return true;
		else if (getSapling(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
			return true;
		else if (getTreeComponent(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
			return true;

		return false;

	}

	@SuppressWarnings("deprecation")
	public void floodTracker(Block startBlock, BreakType breakType)
	{
		BlockFace[] blockFaces = new BlockFace[] { BlockFace.UP, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };

		ArrayList<Location> checkedLocations = new ArrayList<Location>();

		ArrayList<Location> uncheckedLocations = new ArrayList<Location>();

		uncheckedLocations.add(startBlock.getLocation());

		do
		{
			for (int i = 0; i < uncheckedLocations.size(); i++)
			{
				if (isTracked(uncheckedLocations.get(i).getBlock()) && !checkedLocations.contains(uncheckedLocations.get(i)))
				{
					checkedLocations.add(uncheckedLocations.get(i));
				}
			}

			ArrayList<Location> toAddLocations = new ArrayList<Location>();

			for (BlockFace blockFace : blockFaces)
			{
				for (Location location : uncheckedLocations)
				{
					if (isTracked(location.getBlock().getRelative(blockFace)) && !toAddLocations.contains(location.getBlock().getRelative(blockFace).getLocation()) && !checkedLocations.contains(location.getBlock().getRelative(blockFace).getLocation()))
						toAddLocations.add(location.getBlock().getRelative(blockFace).getLocation());
				}
			}
			
			uncheckedLocations.clear();

			uncheckedLocations.addAll(toAddLocations);
			
			toAddLocations.clear();

		}
		while (uncheckedLocations.size() > 0);

		for (Location location : checkedLocations)
		{
			if ((location.getBlock().getType() == Material.CHORUS_FLOWER ? Material.CHORUS_PLANT : location.getBlock().getType()) == Material.CHORUS_PLANT)
			{
				verifyAndDrop(location.getBlock(), breakType, 20L);
			}
			else
				verifyAndDrop(location.getBlock(), breakType, 1L);
		}
	}

	public void drop(Block block, BreakType breakType)
	{

	}

	@SuppressWarnings("deprecation")
	public void verifyAndDrop(Block block, BreakType breakType, long delay)
	{
		CropControl.getPlugin().getServer().getScheduler().scheduleAsyncDelayedTask(CropControl.getPlugin(), new Runnable()
		{
			@Override
			public void run()
			{
				if (getCrop(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
				{
					Crop crop = getCrop(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID());

					if (getTrackedCropMaterial(crop.getCropType()) == block.getType())
						return;

					Bukkit.broadcastMessage(ChatColor.YELLOW + "Broke Crop (" + breakType.toString() + ")");

					drop(block, breakType);

					crops.remove(crop);
				}
				else if (getSapling(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
				{
					Sapling sapling = getSapling(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID());

					if (getTrackedSaplingMaterial(sapling.getSaplingType()) == block.getType())
						return;

					Bukkit.broadcastMessage(ChatColor.GREEN + "Broke Sapling (" + breakType.toString() + ")");

					drop(block, breakType);

					saplings.remove(sapling);
				}
				else if (getTreeComponent(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()) != null)
				{
					TreeComponent treeComponent = getTreeComponent(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID());

					Tree tree = getTree(getTreeComponent(block.getX(), block.getY(), block.getZ(), getChunk(block.getChunk()).getChunkID()).getTreeID());

					if (getTrackedTreeMaterial(treeComponent.getTreeType()) == (block.getType() == Material.LEAVES ? Material.LOG : block.getType() == Material.LEAVES_2 ? Material.LOG_2 : block.getType() == Material.CHORUS_FLOWER ? Material.CHORUS_PLANT : block.getType()))
						return;

					Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Broke Tree Component (" + breakType.toString() + ")");

					drop(block, breakType);

					treeComponents.remove(treeComponent);

					if (getTreeComponenets(tree.getTreeID()).size() == 0)
					{
						Bukkit.broadcastMessage(ChatColor.AQUA + "Broke Tree (" + breakType.toString() + ")");

						trees.remove(tree);
					}
				}
			}
		}, delay);
	}

	private enum BreakType
	{
		PLAYER, WATER, LAVA, PISTON, EXPLOSION, NATURAL;
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
