package com.programmerdan.minecraft.cropcontrol.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.CropState;
import org.bukkit.Material;
import org.bukkit.NetherWartsState;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

	public CropControlEventHandler(FileConfiguration config)
	{
		this.config = config;
		crops = new ArrayList<Crop>();
	}

	@EventHandler
	public void onPlaceBlock(BlockPlaceEvent e)
	{
		Material placedMaterial = e.getBlock().getType();

		if (placedMaterial == Material.CROPS || placedMaterial == Material.CARROT || placedMaterial == Material.POTATO || placedMaterial == Material.NETHER_WARTS || placedMaterial == Material.BEETROOT_BLOCK || placedMaterial == Material.COCOA || placedMaterial == Material.PUMPKIN_STEM || placedMaterial == Material.MELON_STEM)
		{
			if (cropsContains(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), placedMaterial.toString()))
				return;

			crops.add(new Crop(null, null, e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), placedMaterial.toString(), (placedMaterial == Material.NETHER_WARTS ? NetherWartsState.SEEDED.toString() : placedMaterial == Material.COCOA ? CocoaPlant.CocoaPlantSize.SMALL.toString() : placedMaterial == Material.MELON_STEM || placedMaterial == Material.PUMPKIN_STEM ? "0" : CropState.SEEDED.toString()), e.getPlayer().getUniqueId(), System.currentTimeMillis(), (placedMaterial == Material.PUMPKIN_STEM || placedMaterial == Material.MELON_STEM ? false : true)));
		}
		else if (placedMaterial == Material.CACTUS || placedMaterial == Material.BROWN_MUSHROOM || placedMaterial == Material.RED_MUSHROOM || placedMaterial == Material.SUGAR_CANE_BLOCK)
		{
			if (cropsContains(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), placedMaterial.toString()))
				return;

			crops.add(new Crop(null, null, e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), placedMaterial.toString(), null, e.getPlayer().getUniqueId(), System.currentTimeMillis(), false));
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e)
	{
		if (cropsContains(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ(), e.getClickedBlock().getType().toString()))
		{
			Crop crop = getCrop(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ(), e.getClickedBlock().getType().toString());

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
		if (cropsContains(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), e.getBlock().getType().toString()))
		{
			if (e.getBlock().getType() == Material.NETHER_WARTS)
				getCrop(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), e.getBlock().getType().toString()).setCropState(((NetherWarts) e.getNewState().getData()).getState().toString());
			else if (e.getBlock().getType() == Material.COCOA)
				getCrop(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), e.getBlock().getType().toString()).setCropState(((CocoaPlant) e.getNewState().getData()).getSize().toString());
			else if (e.getBlock().getType() == Material.MELON_STEM || e.getBlock().getType() == Material.PUMPKIN_STEM)
				getCrop(e.getNewState().getBlock().getX(), e.getNewState().getBlock().getY(), e.getNewState().getBlock().getZ(), e.getNewState().getBlock().getType().toString()).setCropState((int) e.getNewState().getBlock().getData() + "");

			else
				getCrop(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ(), e.getBlock().getType().toString()).setCropState(((Crops) e.getNewState().getData()).getState().toString());
		}
		else
		{

			if (e.getNewState().getBlock().getType() == Material.MELON_BLOCK)
			{

				for (BlockFace face : BlockFace.values())
				{
					if (face != BlockFace.NORTH && face != BlockFace.SOUTH && face != BlockFace.EAST && face != BlockFace.WEST)
						continue;

					Block block = e.getNewState().getBlock().getRelative(face);

					if (cropsContains(block.getX(), block.getY(), block.getZ(), Material.MELON_STEM.toString()))
					{
						UUID placer = getCrop(block.getX(), block.getY(), block.getZ(), Material.MELON_STEM.toString()).getPlacer();

						crops.add(new Crop(null, null, e.getNewState().getBlock().getX(), e.getNewState().getBlock().getY(), e.getNewState().getBlock().getZ(), Material.MELON_BLOCK.toString(), null, placer, System.currentTimeMillis(), true));
					}
				}
			}
			else if (e.getNewState().getBlock().getType() == Material.PUMPKIN)
			{

				for (BlockFace face : BlockFace.values())
				{
					if (face != BlockFace.NORTH && face != BlockFace.SOUTH && face != BlockFace.EAST && face != BlockFace.WEST)
						continue;

					Block block = e.getNewState().getBlock().getRelative(face);

					if (cropsContains(block.getX(), block.getY(), block.getZ(), Material.PUMPKIN_STEM.toString()))
					{
						UUID placer = getCrop(block.getX(), block.getY(), block.getZ(), Material.PUMPKIN_STEM.toString()).getPlacer();

						crops.add(new Crop(null, null, e.getNewState().getBlock().getX(), e.getNewState().getBlock().getY(), e.getNewState().getBlock().getZ(), Material.PUMPKIN.toString(), null, placer, System.currentTimeMillis(), true));
					}
				}

			}
			else if (e.getNewState().getBlock().getType() == Material.CACTUS)
			{
				Block block = e.getNewState().getBlock().getRelative(BlockFace.DOWN);

				if (cropsContains(block.getX(), block.getY(), block.getZ(), Material.CACTUS.toString()))
				{
					UUID placer = getCrop(block.getX(), block.getY(), block.getZ(), Material.CACTUS.toString()).getPlacer();

					crops.add(new Crop(null, null, e.getNewState().getBlock().getX(), e.getNewState().getBlock().getY(), e.getNewState().getBlock().getZ(), Material.CACTUS.toString(), null, placer, System.currentTimeMillis(), true));
				}
			}
			else if (e.getNewState().getBlock().getType() == Material.SUGAR_CANE_BLOCK)
			{
				Block block = e.getNewState().getBlock().getRelative(BlockFace.DOWN);

				if (cropsContains(block.getX(), block.getY(), block.getZ(), Material.SUGAR_CANE_BLOCK.toString()))
				{
					UUID placer = getCrop(block.getX(), block.getY(), block.getZ(), Material.SUGAR_CANE_BLOCK.toString()).getPlacer();

					crops.add(new Crop(null, null, e.getNewState().getBlock().getX(), e.getNewState().getBlock().getY(), e.getNewState().getBlock().getZ(), Material.SUGAR_CANE_BLOCK.toString(), null, placer, System.currentTimeMillis(), true));
				}
			}

		}
	}

	@EventHandler
	public void onBlockSpread(BlockSpreadEvent e)
	{
	}

	@EventHandler
	public void onTreeGrow(StructureGrowEvent e)
	{

	}

	public boolean cropsContains(int x, int y, int z, String cropType)
	{
		for (Crop crop : crops)
		{
			if (crop.getX() == x && crop.getY() == y && crop.getZ() == z && crop.getCropType().equals(cropType))
				return true;
		}

		return false;
	}

	public Crop getCrop(int x, int y, int z, String cropType)
	{
		for (Crop crop : crops)
		{
			if (crop.getX() == x && crop.getY() == y && crop.getZ() == z && crop.getCropType().equals(cropType))
				return crop;
		}

		return null;
	}

}
