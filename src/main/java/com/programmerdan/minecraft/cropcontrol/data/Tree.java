package com.programmerdan.minecraft.cropcontrol.data;

import java.math.BigInteger;
import java.util.UUID;

public class Tree
{
	private BigInteger treeID;
	private BigInteger chunkID;
	private int x;
	private int y;
	private int z;
	private String treeType;
	private UUID placer;
	private long timeStamp;
	
	public Tree(BigInteger treeID, BigInteger chunkID, int x, int y, int z, String treeType, UUID placer, long timeStamp)
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

	public int getY()
	{
		return y;
	}

	public int getZ()
	{
		return z;
	}

	public BigInteger getTreeID()
	{
		return treeID;
	}

	public BigInteger getChunkID()
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
