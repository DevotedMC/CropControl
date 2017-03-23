package com.programmerdan.minecraft.cropcontrol.data;

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
		int base = super.hashCode();
		base = 31*31*31*31*base + 31*31*31*x + 31*31*y + 31*z + (int)chunkID;
		return base;
	}
}