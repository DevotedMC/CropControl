package com.programmerdan.minecraft.cropcontrol.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;

import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.data.Crop;
import com.programmerdan.minecraft.cropcontrol.data.Sapling;
import com.programmerdan.minecraft.cropcontrol.data.WorldChunk;

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

		harvestableCrops = new HashMap<Material, Boolean>();

		fillHarvestableCropsList();
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

	public Crop getCrop(int x, int y, int z, Material cropType)
	{
		for (Crop crop : crops)
		{
			if (crop.getX() == x && crop.getY() == y && crop.getZ() == z && crop.getCropType().equals(cropType.toString()))
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

	public Sapling getSapling(int x, int y, int z, String saplingType)
	{
		for (Sapling sapling : saplings)
		{
			if (sapling.getX() == x && sapling.getY() == y && sapling.getZ() == z && sapling.getSaplingType() == saplingType)
				return sapling;
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
			if (getCrop(block.getX(), block.getY(), block.getZ(), blockMaterial) != null)
				return;

			crops.add(new Crop(null, null, block.getX(), block.getY(), block.getZ(), blockMaterial.toString(), getBaseCropState(blockMaterial), e.getPlayer().getUniqueId(), System.currentTimeMillis(), harvestableCrops.get(blockMaterial)));
		}
		else if (blockMaterial == Material.SAPLING)
		{
			if (getSapling(block.getX(), block.getY(), block.getZ(), getSaplingType(block.getData())) != null)
				return;
			
			saplings.add(new Sapling(null, null, block.getX(), block.getY(), block.getZ(), getSaplingType(block.getData()), e.getPlayer().getUniqueId(), System.currentTimeMillis()));
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e)
	{
		Bukkit.broadcastMessage("Debug: Chunks: ");
		for (int i = 0; i < chunks.size(); i++)
		{
			Bukkit.broadcastMessage(i + ") WorldID: " + chunks.get(i).getWorldID());
			Bukkit.broadcastMessage(i + ") X: " + chunks.get(i).getChunkX());
			Bukkit.broadcastMessage(i + ") Z: " + chunks.get(i).getChunkZ());
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
				if (getCrop(block.getX(), block.getY(), block.getZ(), block.getType()) != null)
				{
					getCrop(block.getX(), block.getY(), block.getZ(), block.getType()).setCropState(getCropState(e.getNewState()));
				}
				else
				{
					if (block.getType() == Material.MELON_BLOCK || block.getType() == Material.PUMPKIN)
					{
						BlockFace[] directions = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };

						for (BlockFace blockFace : directions)
						{
							Block otherBlock = block.getRelative(blockFace);

							Material stemMaterial = block.getType() == Material.MELON_BLOCK ? Material.MELON_STEM : Material.PUMPKIN_STEM;

							if (getCrop(otherBlock.getX(), otherBlock.getY(), otherBlock.getZ(), stemMaterial) != null)
							{
								UUID placerUUID = getCrop(otherBlock.getX(), otherBlock.getY(), otherBlock.getZ(), stemMaterial).getPlacer();

								crops.add(new Crop(null, null, block.getX(), block.getY(), block.getZ(), block.getType().toString(), null, placerUUID, System.currentTimeMillis(), true));
							}
						}
					}
					else if (block.getType() == Material.CACTUS || block.getType() == Material.SUGAR_CANE_BLOCK)
					{
						Block otherBlock = block.getRelative(BlockFace.DOWN);

						if (getCrop(otherBlock.getX(), otherBlock.getY(), otherBlock.getZ(), otherBlock.getType()) != null)
						{
							UUID placerUUID = getCrop(otherBlock.getX(), otherBlock.getY(), otherBlock.getZ(), otherBlock.getType()).getPlacer();

							crops.add(new Crop(null, null, block.getX(), block.getY(), block.getZ(), otherBlock.getType().toString(), null, placerUUID, System.currentTimeMillis(), true));
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

		if (getCrop(source.getX(), source.getY(), source.getZ(), source.getType()) != null)
		{
			UUID placerUUID = getCrop(source.getX(), source.getY(), source.getZ(), source.getType()).getPlacer();

			crops.add(new Crop(null, null, block.getX(), block.getY(), block.getZ(), source.getType().toString(), null, placerUUID, System.currentTimeMillis(), true));
		}
	}

	@EventHandler
	public void onTreeGrow(StructureGrowEvent e)
	{
		
	}

	/*
	 * This is where we should (in my humble opinion) be getting data from the
	 * DB, Such that when a chunk is loaded we load all of the crops, saplings,
	 * trees & tree componenets. And therefore when a chunk is unloaded we save
	 * it all to the DB,
	 * 
	 * Or something along those lines.
	 * 
	 * Secondly, I think these work. Need clarification.
	 */
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e)
	{
		Chunk chunk = e.getChunk();

		if (getChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()) != null)
			return;

		chunks.add(new WorldChunk(null, chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()));

	}

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
