package com.programmerdan.minecraft.cropcontrol.data;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 
 * 
 * @author ProgrammerDan
 *
 * @param <T> The type of object to cache
 */
public class LocationCache<T extends Locatable> {
	private Map<WorldChunk, Set<T>> cache = new ConcurrentHashMap<WorldChunk, Set<T>>();
	
	public void expire(WorldChunk chunk) {
		if (cache.containsKey(chunk)) {
			cache.remove(chunk);
		}
	}
	
	public Set<T> getChunk()
}
