package com.programmerdan.minecraft.cropcontrol.data;

import java.util.Objects;

/**
 * At the core of fast hashing is Locatable, which gives us O(1) lookups of specific location objects without resorting to 
 * other cuboid sorting methods. 
 * That's the theory at least.
 * 
 * @author ProgrammerDan
 *
 */
public class Locatable {

	protected long chunkID;
	protected int x;
	protected int y;
	protected int z;

	public Locatable(){
		super();
	}
	
	public Locatable(long chunkID, int x, int y, int z) {
		super();
		this.chunkID = chunkID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public long getChunkID() {
		return chunkID;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Locatable) {
			Locatable l = (Locatable) o;
			return this.chunkID == l.chunkID && this.x == l.x && this.y == l.y && this.z == l.z;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(x, y, z, chunkID);
	}
	
	@Override
	public String toString() {
		return "" + hashCode() + "[" + chunkID + "," + x + "," + y + "," + z + "]";
	}
}