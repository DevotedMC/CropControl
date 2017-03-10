package com.programmerdan.minecraft.cropcontrol.data;

import java.util.UUID;

public class Tree
{
	private long treeID;
	private long chunkID;
	private int x;
	private int y;
	private int z;
	private String treeType;
	private UUID placer;
	private long timeStamp;
	
	public Tree(long treeID, long chunkID, int x, int y, int z, String treeType, UUID placer, long timeStamp)
	{
		this.treeID = treeID;
		this.chunkID = chunkID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.treeType = treeType;
		this.placer = placer;
		this.timeStamp = timeStamp;
	}

	public int getX()
	{
		return x;
	}

	public void setChunkID(long chunkID)
	{
		this.chunkID = chunkID;
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

	public long getTreeID()
	{
		return treeID;
	}

	public long getChunkID()
	{
		return chunkID;
	}

	public String getTreeType()
	{
		return treeType;
	}

	public UUID getPlacer()
	{
		return placer;
	}

	public long getTimeStamp()
	{
		return timeStamp;
	}
	
}
