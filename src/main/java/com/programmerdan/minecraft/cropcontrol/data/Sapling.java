package com.programmerdan.minecraft.cropcontrol.data;

import java.math.BigInteger;
import java.util.UUID;

public class Sapling
{
	private BigInteger saplingID;
	private BigInteger chunkID;
	private int x;
	private int y;
	private int z;
	private String saplingType;
	private UUID placer;
	private long timeStamp;
	
	public Sapling(BigInteger saplingID, BigInteger chunkID, int x, int y, int z, String saplingType, UUID placer, long timeStap)
	{
		this.saplingID = saplingID;
		this.chunkID = chunkID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.saplingType = saplingType;
		this.placer = placer;
		this.timeStamp = timeStamp;
	}

	public BigInteger getSaplingID()
	{
		return saplingID;
	}

	public BigInteger getChunkID()
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
}
