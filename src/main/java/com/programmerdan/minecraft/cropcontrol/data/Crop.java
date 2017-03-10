package com.programmerdan.minecraft.cropcontrol.data;

import java.util.UUID;

import org.bukkit.CropState;

public class Crop
{

	private long cropID;
	private long chunkID;
	private int x;
	private int y;
	private int z;
	private String cropType;
	private String cropState;
	private UUID placer;
	private long timeStamp;
	private boolean harvestable;

	public Crop(long cropID, long chunkID, int x, int y, int z, String cropType, String cropState, UUID placer, long timeStamp, boolean harvestable)
	{
		this.cropID = cropID;
		this.chunkID = chunkID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.cropType = cropType;
		this.cropState = cropState;
		this.placer = placer;
		this.timeStamp = timeStamp;
		this.harvestable = harvestable;
	}

	public long getCropID()
	{
		return cropID;
	}

	public long getChunkID()
	{
		return chunkID;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getZ()
	{
		return z;
	}

	public String getCropType()
	{
		return cropType;
	}

	public String getCropState()
	{
		return cropState;
	}

	public void setCropState(String cropState)
	{
		this.cropState = cropState;
	}

	public UUID getPlacer()
	{
		return placer;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}
	
	public boolean getHarvestable()
	{
		return harvestable;
	}
}
