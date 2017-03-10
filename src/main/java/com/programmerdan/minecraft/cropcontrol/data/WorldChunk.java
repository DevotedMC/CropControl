package com.programmerdan.minecraft.cropcontrol.data;

import java.util.UUID;

public class WorldChunk {
	
	private long chunkID;
	private UUID worldID;
	private int chunkX;
	private int chunkZ;
	
	public WorldChunk(long chunkID, UUID worldID, int chunkX, int chunkZ)
	{
		this.chunkID = chunkID;
		this.worldID = worldID;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	public long getChunkID() {
		return chunkID;
	}

	public UUID getWorldID() {
		return worldID;
	}

	public int getChunkX() {
		return chunkX;
	}

	public int getChunkZ() {
		return chunkZ;
	}
	
}
