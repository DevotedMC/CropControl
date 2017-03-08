package com.programmerdan.minecraft.cropcontrol.data;

import java.math.BigInteger;
import java.util.UUID;

public class TreeComponent
{
	private BigInteger treeComponentID;
	private BigInteger treeID;
	private BigInteger chunkID;
	private int x;
	private int y;
	private int z;
	private String treeType;
	private UUID placer;
	private boolean harvestable;
	
	public TreeComponent(BigInteger treeComponentID, BigInteger treeID, BigInteger chunkID, int x, int y, int z, String treeType, UUID placer, boolean harvestable)
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

	public BigInteger getTreeComponentID()
	{
		return treeComponentID;
	}

	public BigInteger getTreeID()
	{
		return treeID;
	}

	public BigInteger getChunkID()
	{
		return chunkID;
	}
	
	public void setChunkID(BigInteger chunkID)
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
