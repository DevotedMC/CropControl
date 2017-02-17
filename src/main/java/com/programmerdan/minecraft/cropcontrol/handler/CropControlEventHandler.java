package com.programmerdan.minecraft.cropcontrol.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.Crops;
import org.bukkit.material.NetherWarts;
import org.bukkit.material.SimpleAttachableMaterialData;

import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.data.Crop;

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

	/**
	 * List of materials that are crops, and if we track specific states
	 * belonging to that material.
	 */
	private Map<Material, Boolean> harvestableCrops;
	
	private Map<BlockFace, BlockFace> adjacent;

	public CropControlEventHandler(FileConfiguration config)
	{
		this.config = config;

		crops = new ArrayList<Crop>();

		harvestableCrops = new HashMap<Material, Boolean>();
		
		adjacent = new HashMap<BlockFace, BlockFace>();

		fillHarvestableCropsList();
		
		loadAdjacents();
	}
	
	public void loadAdjacents()
	{
		adjacent.put(BlockFace.NORTH, BlockFace.SOUTH);
		adjacent.put(BlockFace.SOUTH, BlockFace.NORTH);
		adjacent.put(BlockFace.EAST, BlockFace.WEST);
		adjacent.put(BlockFace.WEST, BlockFace.EAST);
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

	public Crop getCrop(int x, int y, int z, Material cropType)
	{
		for (Crop crop : crops)
		{
			if (crop.getX() == x && crop.getY() == y && crop.getZ() == z && crop.getCropType().equals(cropType.toString()))
				return crop;
		}

		return null;
	}

	@EventHandler
	public void onPlaceBlock(BlockPlaceEvent e)
	{
		Block block = e.getBlock();

		Material blockMaterial = block.getType();

		if (!harvestableCrops.containsKey(blockMaterial))
			return;

		if (getCrop(block.getX(), block.getY(), block.getZ(), blockMaterial) != null)
			return;

		crops.add(new Crop(null, null, block.getX(), block.getY(), block.getZ(), blockMaterial.toString(), getBaseCropState(blockMaterial), e.getPlayer().getUniqueId(), System.currentTimeMillis(), harvestableCrops.get(blockMaterial)));
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e)
	{
		Block block = e.getClickedBlock();

		if (getCrop(block.getX(), block.getY(), block.getZ(), block.getType()) != null)
		{
			Crop crop = getCrop(block.getX(), block.getY(), block.getZ(), block.getType());

			Bukkit.broadcastMessage("");
			Bukkit.broadcastMessage("Debug: There is a crop here.");
			Bukkit.broadcastMessage("X: " + crop.getX());
			Bukkit.broadcastMessage("Y: " + crop.getY());
			Bukkit.broadcastMessage("Z: " + crop.getZ());
			Bukkit.broadcastMessage("Crop Type: " + crop.getCropType());
			Bukkit.broadcastMessage("Crop State: " + crop.getCropState());
			Bukkit.broadcastMessage("Placer UUID: " + crop.getPlacer());
			Bukkit.broadcastMessage("TimeStamp: " + crop.getTimeStamp());
			Bukkit.broadcastMessage("Directly Harvestable: " + crop.getHarvestable());
		}
		else
		{
			Bukkit.broadcastMessage("Debug: There is not a crop here.");
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
						for (BlockFace blockFace : adjacent.keySet())
						{
							Block otherBlock = block.getRelative(blockFace);
							
							if (!(otherBlock.getState().getData() instanceof SimpleAttachableMaterialData))
								continue;
							
							BlockFace otherBlockFacing = ((SimpleAttachableMaterialData) otherBlock.getState().getData()).getFacing();
							
							Material stemMaterial = block.getType() == Material.MELON_BLOCK ? Material.MELON_STEM : Material.PUMPKIN_STEM;
							
							if (blockFace != adjacent.get(otherBlockFacing))
								continue;
							
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

	}

	@EventHandler
	public void onTreeGrow(StructureGrowEvent e)
	{

	}

}
