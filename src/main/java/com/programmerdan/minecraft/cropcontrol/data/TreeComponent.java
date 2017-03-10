package com.programmerdan.minecraft.cropcontrol.data;

import java.util.UUID;

public class TreeComponent
{
	private long treeComponentID;
	private long treeID;
	private long chunkID;
	private int x;
	private int y;
	private int z;
	private String treeType;
	private UUID placer;
	private boolean harvestable;
	
	public TreeComponent(long treeComponentID, long treeID, long chunkID, int x, int y, int z, String treeType, UUID placer, boolean harvestable)
	{
		this.treeComponentID = treeComponentID;
		this.treeID = treeID;
		this.chunkID = chunkID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.treeType = treeType;
		this.placer = placer;
		this.harvestable = harvestable;
	}

	public long getTreeComponentID()
	{
		return treeComponentID;
	}

	public long getTreeID()
	{
		return treeID;
	}

	public long getChunkID()
	{
		return chunkID;
	}
	
	public void setChunkID(long chunkID)
	{
		this.chunkID = chunkID;
	}

	public int getX()
	{
		return x;
	}

	public void setX(int x)
	{
		this.x = x;
	}

	public void setY(int y)
	{
		this.y = y;
	}

	public void setZ(int z)
	{
		this.z = z;
	}

	public int getY()
	{
		return y;
	}

	public int getZ()
	{
		return z;
	}

	public String getTreeType()
	{
		return treeType;
	}
	
	public UUID getPlacer()
	{
		return placer;
	}

	public boolean isHarvestable()
	{
		return harvestable;
	}
	
	public void setHarvestable(boolean harvestable)
	{
		this.harvestable = harvestable;
	}
	
}
