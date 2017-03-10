package com.programmerdan.minecraft.cropcontrol.data;

import java.util.UUID;

public class Sapling
{
	private long saplingID;
	private long chunkID;
	private int x;
	private int y;
	private int z;
	private String saplingType;
	private UUID placer;
	private long timeStamp;
	private boolean harvestable;
	
	public Sapling(long saplingID, long chunkID, int x, int y, int z, String saplingType, UUID placer, long timeStamp, boolean harvestable)
	{
		this.saplingID = saplingID;
		this.chunkID = chunkID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.saplingType = saplingType;
		this.placer = placer;
		this.timeStamp = timeStamp;
		this.harvestable = harvestable;
	}

	public long getSaplingID()
	{
		return saplingID;
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

	public String getSaplingType()
	{
		return saplingType;
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
