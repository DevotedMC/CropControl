package com.programmerdan.minecraft.cropcontrol.data;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.block.Block;

import com.programmerdan.minecraft.cropcontrol.handler.CropControlDatabaseHandler;

/**
 * Accessor wrapper, some of these methods are legacy from a prior approach to data access based on lists.
 * 
 * I'll remove them as I confirm they are unused.
 * 
 * @author ProgrammerDan
 *
 */
public class DAO {

	// reverse binding
	private CropControlDatabaseHandler database;

	public DAO(CropControlDatabaseHandler cropControlDatabaseHandler) {
		database = cropControlDatabaseHandler;
	}

	public Crop getCrop(int x, int y, int z, long chunkID) {
		return WorldChunk.byId(chunkID).getCrop(x, y, z);
	}

	public WorldChunk getChunk(Chunk chunk) {
		return WorldChunk.getChunk(chunk);
	}

	public WorldChunk getChunk(long chunkID) {
		return WorldChunk.byId(chunkID);
	}
	
	public void unloadChunk(Chunk chunk) {
		WorldChunk.unloadChunk(chunk);
	}

	public Sapling getSapling(int x, int y, int z, long chunkID) {
		return WorldChunk.byId(chunkID).getSapling(x, y, z);
	}

	public Tree getTree(long treeID) {
		return WorldChunk.getTree(treeID);
	}

	public Tree getTree(int x, int y, int z, long chunkID) {
		return WorldChunk.byId(chunkID).getTree(x, y, z);
	}
	
	public Tree getTree(TreeComponent component) {
		return getTree(component.getTreeID());
	}

	public List<TreeComponent> getTreeComponents(long treeID) {
		return WorldChunk.getTreeComponents(treeID);
	}
	
	public List<TreeComponent> getTreeComponents(Tree tree) {
		return WorldChunk.getTreeComponents(tree);
	}
	
	public boolean isTreeComponent(Tree tree, TreeComponent component) {
		return WorldChunk.isTreeComponent(tree, component);
	}

	public TreeComponent getTreeComponent(int x, int y, int z, long chunkID) {
		return WorldChunk.byId(chunkID).getTreeComponent(x, y, z);
	}

	public boolean isTracked(Block block) {
		WorldChunk chunk = WorldChunk.getChunk(block.getChunk());
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		return (chunk.getCrop(x, y, z) != null) || (chunk.getSapling(x, y, z) != null) || (chunk.getTreeComponent(x, y, z) != null);
	}

	public boolean isBreakableTracked(Block block) {
		WorldChunk chunk = WorldChunk.getChunk(block.getChunk());
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		return (chunk.getCrop(x, y, z) != null) || (chunk.getSapling(x, y, z) != null);
	}

}
