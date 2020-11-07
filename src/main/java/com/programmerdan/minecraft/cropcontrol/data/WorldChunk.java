package com.programmerdan.minecraft.cropcontrol.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Chunk;

import com.google.common.collect.Sets;
import com.programmerdan.minecraft.cropcontrol.CreationError;
import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlDatabaseHandler;

/**
 * Lots of baked in logic and tracking here. Chunks effectively root our tracking and data into manageable portions, so 
 * caching and other related activities occur here.
 * 
 * Note that there are known _potential_ race conditions with unloading and deferred dirty saving due to a non-canonical approach
 * to flyweights. I'll probably refactor this all soon, especially if I get even a hint of issue, so don't get too comfortable.
 * 
 * @author xFier
 * @author ProgrammerDan
 *
 */
public class WorldChunk {
	
	// consider locating all related to the chunk into the chunk, load as chunk, etc.
	private Map<Long, Crop> cropCacheID = new ConcurrentHashMap<>();
	private Map<Locatable, Crop> cropCacheLoc = new ConcurrentHashMap<>();

	private Map<Long, Sapling> saplingCacheID = new ConcurrentHashMap<>();
	private Map<Locatable, Sapling> saplingCacheLoc = new ConcurrentHashMap<>();
	
	private static Map<Long, Tree> universalTreeCache = new ConcurrentHashMap<>();
	private static Map<Long, Set<TreeComponent>> universalTreeComponentCache = new ConcurrentHashMap<>();
	
	private Map<Long, Tree> treeCacheID = new ConcurrentHashMap<>();
	private Map<Locatable, Tree> treeCacheLoc = new ConcurrentHashMap<>();
	
	private Map<Long, TreeComponent> componentCacheID = new ConcurrentHashMap<>();
	private Map<Locatable, TreeComponent> componentCacheLoc = new ConcurrentHashMap<>();
	
	private static Map<Long, WorldChunk> chunkCacheID = new ConcurrentHashMap<>();
	private static Map<UUID, Map<Long, WorldChunk>> chunkCacheLoc = new ConcurrentHashMap<>(); // using transposed x,z
	
	private static ConcurrentLinkedQueue<WorldChunk> unloadQueue = new ConcurrentLinkedQueue<>();
	
	private ConcurrentLinkedQueue<Crop> removedCropQueue = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Sapling> removedSaplingQueue = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<TreeComponent> removedTreeComponentQueue = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Tree> removedTreeQueue = new ConcurrentLinkedQueue<>();

	private long chunkID;
	private UUID worldID;
	private int chunkX;
	private int chunkZ;
	
	private WorldChunk() {
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
	
	public long getChunkLocID() {
		return ((long) chunkX << 32L) + chunkZ;
	}
	
	
	private void stuff() {
		List<Crop> crops = Crop.preload(this);
		for (Crop crop : crops) {
			//CropControl.getPlugin().debug("Loading crop at {0}: {1}", (Locatable) crop, crop);
			cropCacheLoc.put(new Locatable(crop.getChunkID(), crop.getX(), crop.getY(), crop.getZ()), crop);
			cropCacheID.put(crop.getCropID(), crop);
		}
		
		List<Sapling> saplings = Sapling.preload(this);
		for (Sapling sapling: saplings) {
			//CropControl.getPlugin().debug("Loading sapling at {0}: {1}", (Locatable) sapling, sapling);
			saplingCacheLoc.put(new Locatable(sapling.getChunkID(), sapling.getX(), sapling.getY(), sapling.getZ()), sapling);
			saplingCacheID.put(sapling.getSaplingID(), sapling);			
		}
		
		List<Tree> trees = Tree.preload(this);
		for (Tree tree: trees) {
			//CropControl.getPlugin().debug("Loading tree at {0}: {1}", (Locatable) tree, tree);
			treeCacheLoc.put(new Locatable(tree.getChunkID(), tree.getX(), tree.getY(), tree.getZ()), tree);
			treeCacheID.put(tree.getTreeID(), tree);
			WorldChunk.universalTreeCache.put(tree.getTreeID(), tree);
		}
		
		List<TreeComponent> components = TreeComponent.preload(this);
		for (TreeComponent component : components) {
			//CropControl.getPlugin().debug("Loading tree component at {0}: {1}", (Locatable) component, component);
			componentCacheLoc.put(new Locatable(component.getChunkID(), component.getX(), component.getY(), component.getZ()), component);
			componentCacheID.put(component.getTreeComponentID(), component);
			Set<TreeComponent> treeComponents = WorldChunk.universalTreeComponentCache.get(component.getTreeID());
			if (treeComponents == null) {
				treeComponents = Sets.newConcurrentHashSet();
				WorldChunk.universalTreeComponentCache.put(component.getTreeID(), treeComponents);
			}
			treeComponents.add(component);
		}
	}
	
	
	public void register(Crop crop) {
		Crop preExist = cropCacheLoc.remove(crop); // did we have one here already?
		if (preExist != null) {
			CropControl.getPlugin().debug("Replacing crop at {0}: {1}", crop, crop);
			cropCacheID.remove(preExist.getCropID());
		} else {
			CropControl.getPlugin().debug("Putting crop at {0}: {1}", crop, crop);
		}
		cropCacheLoc.put(new Locatable(crop.getChunkID(), crop.getX(), crop.getY(), crop.getZ()), crop);
		cropCacheID.put(crop.getCropID(), crop);
	}
	
	public void unregister(Crop crop) {
		Crop preExist = cropCacheLoc.remove(crop); // did we have one here already?
		if (preExist != null) {
			cropCacheID.remove(preExist.getCropID());
			removedCropQueue.offer(preExist);
			CropControl.getPlugin().debug("Removed crop {0}", crop);
		}
	}
	
	public Crop getCrop(int x, int y, int z) {
		Crop result = cropCacheLoc.get(new Locatable(chunkID, x, y, z));
		//CropControl.getPlugin().debug("Get crop at {0},{1},{2},{3}: {4}", chunkID, x, y, z, result);
		return result;
	}

	public void register(Sapling sapling) {
		Sapling preExist = saplingCacheLoc.remove(sapling); // did we have one here already?
		if (preExist != null) {
			CropControl.getPlugin().debug("Replacing sapling at {0}: {1}", sapling, sapling);
			saplingCacheID.remove(preExist.getSaplingID());
		} else {
			CropControl.getPlugin().debug("Putting sapling at {0}: {1}", sapling, sapling);
		}
		saplingCacheLoc.put(new Locatable(sapling.getChunkID(), sapling.getX(), sapling.getY(), sapling.getZ()), sapling);
		saplingCacheID.put(sapling.getSaplingID(), sapling);
	}

	public void unregister(Sapling sapling) {
		Sapling preExist = saplingCacheLoc.remove(sapling); // did we have one here already?
		if (preExist != null) {
			saplingCacheID.remove(preExist.getSaplingID());
			removedSaplingQueue.offer(preExist);
			CropControl.getPlugin().debug("Removed sapling {0}", sapling);
			//saplingCacheLoc.remove(new Locatable(sapling.getChunkID(), sapling.getX(), sapling.getY(), sapling.getZ()));
		}
	}
	
	public Sapling getSapling(int x, int y, int z) {
		Sapling result = saplingCacheLoc.get(new Locatable(chunkID, x, y, z));
		//CropControl.getPlugin().debug("Get Sapling at {0},{1},{2},{3}: {4}", chunkID, x, y, z, result);
		return result;
	}

	public void register(Tree tree) {
		Tree preExist = treeCacheLoc.remove(tree); // did we have one here already?
		if (preExist != null) {
			CropControl.getPlugin().debug("Replacing tree at {0}: {1}", tree, tree);
			treeCacheID.remove(preExist.getTreeID());
		} else {
			CropControl.getPlugin().debug("Putting tree at {0}: {1}", tree, tree);
		}
		treeCacheLoc.put(new Locatable(tree.getChunkID(), tree.getX(), tree.getY(), tree.getZ()), tree);
		treeCacheID.put(tree.getTreeID(), tree);
		WorldChunk.universalTreeCache.put(tree.getTreeID(), tree);
	}
	
	public void unregister(Tree tree) {
		Tree preExist = treeCacheLoc.remove(tree); // did we have one here already?
		if (preExist != null) {
			treeCacheID.remove(preExist.getTreeID());
			removedTreeQueue.offer(preExist);
			CropControl.getPlugin().debug("Removed tree {0}", tree);
			//treeCacheLoc.remove(new Locatable(tree.getChunkID(), tree.getX(), tree.getY(), tree.getZ()));
		}
	}
	
	public Tree getTree(int x, int y, int z) {
		Tree result = treeCacheLoc.get(new Locatable(chunkID, x, y, z));
		//CropControl.getPlugin().debug("Get Tree at {0},{1},{2},{3}: {4}", chunkID, x, y, z, result);
		return result;
	}
	
	public static Tree getTree(long treeID) {
		Tree tree = WorldChunk.universalTreeCache.get(treeID);
		if (tree == null) {
			tree = Tree.byId(treeID);
		}
		//CropControl.getPlugin().debug("Get tree {0}: {1}", treeID, tree);
		return tree;
	}
	
	public static void remove(Tree tree) {
		WorldChunk.universalTreeCache.remove(tree.getTreeID());
	}

	public void register(TreeComponent component) {
		TreeComponent preExist = componentCacheLoc.remove(component); // did we have one here already?
		if (preExist != null) {
			CropControl.getPlugin().debug("Replacing treecomponent at {0}: {1}", component, component);
			componentCacheID.remove(preExist.getTreeComponentID());
		} else {
			CropControl.getPlugin().debug("Putting treecomponent at {0}: {1}", component, component);
		}
		componentCacheLoc.put(new Locatable(component.getChunkID(), component.getX(), component.getY(), component.getZ()), component);
		componentCacheID.put(component.getTreeComponentID(), component);
		
		Set<TreeComponent> treeComponents = WorldChunk.universalTreeComponentCache.get(component.getTreeID());
		if (treeComponents == null) {
			treeComponents = Sets.newConcurrentHashSet();
			WorldChunk.universalTreeComponentCache.put(component.getTreeID(), treeComponents);
		}
		treeComponents.add(component);
	}
	
	public void unregister(TreeComponent component) {
		TreeComponent preExist = componentCacheLoc.remove(component); // did we have one here already?
		if (preExist != null) {
			componentCacheID.remove(preExist.getTreeID());
			removedTreeComponentQueue.offer(preExist);
			CropControl.getPlugin().debug("Removed treecomponent {0}", component);
			//componentCacheLoc.remove(new Locatable(component.getChunkID(), component.getX(), component.getY(), component.getZ()));
		}
	}
	
	public static void remove(TreeComponent component) {
		Set<TreeComponent> treeComponents = WorldChunk.universalTreeComponentCache.get(component.getTreeID());
		if (treeComponents == null) {
			WorldChunk.universalTreeComponentCache.put(component.getTreeID(), Sets.newConcurrentHashSet());
		} else {
			treeComponents.remove(component);
		}
	}
	
	public TreeComponent getTreeComponent(int x, int y, int z) {
		TreeComponent result = componentCacheLoc.get(new Locatable(chunkID, x, y, z));
		//CropControl.getPlugin().debug("Get TreeComponent at {0},{1},{2},{3}: {4}", chunkID, x, y, z, result);
		return result;
	}
	
	public static List<TreeComponent> getTreeComponents(Tree tree) {
		//CropControl.getPlugin().debug("Getting treecomponents for tree {0}", tree);
		return getTreeComponents(tree.getTreeID());
	}
	
	public static List<TreeComponent> getTreeComponents(long treeId) {
		List<TreeComponent> list = new CopyOnWriteArrayList<TreeComponent>();
		Set<TreeComponent> set = WorldChunk.universalTreeComponentCache.get(treeId);
		if (set != null) {
			list.addAll(set);
		}
		//CropControl.getPlugin().debug("Get Tree components from {0}: {1}", treeId, list);
		return list;
	}
	
	public static boolean isTreeComponent(Tree tree, TreeComponent component) {
		Set<TreeComponent> set = WorldChunk.universalTreeComponentCache.get(tree.getTreeID());
		if (set != null) {
			return set.contains(component);
		}
		return false;
	}
	
	public static void unloadChunk(Chunk chunk) {
		long chunk_id = ((long) chunk.getX() << 32L) + chunk.getZ();
		UUID world_uuid = chunk.getWorld().getUID();
		//CropControl.getPlugin().debug("Registering unload for chunk {0}:{1}", world_uuid, chunk_id);
		Map<Long, WorldChunk> chunks = chunkCacheLoc.get(world_uuid);
		if (chunks == null) {
			return;
		}
		WorldChunk cacheChunk = chunks.get(chunk_id);
		if (cacheChunk != null) {
			unloadQueue.offer(cacheChunk);
		}
		// note we do not actually remove it here.
	}
	
	// Should be in an async thread.
	public static void doUnloads() {
		doUnloads(500L);// don't spend more then half a second unloading.
	}
	
	public static void doAllUnloads() {
		doUnloads(null);
	}
	
	public static void doUnloads(Long maxTime) {
		int unloads = 0;
		long start = System.currentTimeMillis();
		long cropz = 0L;
		long saplingz = 0L;
		long treez = 0L;
		long componentz = 0L;
		
		while (!unloadQueue.isEmpty()) {
			WorldChunk unload = unloadQueue.poll();
			
			if (unload == null) {
				continue;
			}
			
			// extract crops and such
			Iterable<Crop> crops = unload.cropCacheID.values();
			Iterable<Sapling> saplings = unload.saplingCacheID.values();
			Iterable<Tree> trees = unload.treeCacheID.values();
			Iterable<TreeComponent> components = unload.componentCacheID.values();
			
			// remove from cache
			long chunkId = ((long) unload.getChunkX() << 32L) + unload.getChunkZ();
			UUID worldUuid = unload.getWorldID();
			Map<Long, WorldChunk> chunks = chunkCacheLoc.get(worldUuid);
			if (chunks == null) {
				continue;
			}
			
			WorldChunk cacheChunk = chunks.remove(chunkId);
			if (cacheChunk != null) {
				chunkCacheID.remove(unload.getChunkID());
			}
			/* CropControl.getPlugin().debug("Actually unloading for chunk {0}:{1},{2}:{3}:{4}", world_uuid, cacheChunk.chunkX,
					cacheChunk.chunkZ, chunk_id, cacheChunk.chunkID);*/
			// empty the collections
			unload.componentCacheID.clear();
			unload.componentCacheLoc.clear();
			unload.treeCacheID.clear();
			unload.treeCacheLoc.clear();
			unload.saplingCacheID.clear();
			unload.saplingCacheLoc.clear();
			unload.cropCacheID.clear();
			unload.cropCacheLoc.clear();
			
			// save collections to disk.
			cropz += Crop.flushDirty(crops);
			saplingz += Sapling.flushDirty(saplings);
			treez += Tree.flushDirty(trees);
			componentz += TreeComponent.flushDirty(components);
			
			cropz += Crop.flushDirty(unload.removedCropQueue);
			saplingz += Sapling.flushDirty(unload.removedSaplingQueue);
			treez += Tree.flushDirty(unload.removedTreeQueue);
			componentz += TreeComponent.flushDirty(unload.removedTreeComponentQueue);
			
			// excessive local cleanup
			crops = null;
			saplings = null;
			trees = null;
			components = null;
			
			unload.removedCropQueue.clear();
			unload.removedSaplingQueue.clear();
			unload.removedTreeComponentQueue.clear();
			unload.removedTreeQueue.clear();
			
			unloads ++;
			
			if (maxTime != null && System.currentTimeMillis() - start > maxTime) {
				break;
			}
		}
		CropControl.getPlugin().debug("Unloaded {0} chunks in {1} ms, saved {2} crops, {3} saplings, {4} components, {5} trees",
				unloads, (System.currentTimeMillis() - start), cropz, saplingz, componentz, treez);
	}
	
	public static WorldChunk getChunk(Chunk chunk) {
		WorldChunk cacheChunk = null;
		long chunkId = ((long) chunk.getX() << 32L) + chunk.getZ();
		UUID worldUuid = chunk.getWorld().getUID();
		Map<Long, WorldChunk> chunks = chunkCacheLoc.get(worldUuid);
		if (chunks == null) {
			chunks = new ConcurrentHashMap<>();
			chunkCacheLoc.put(worldUuid, chunks);
		}
		cacheChunk = chunks.get(chunkId);
		if (cacheChunk != null) {
			// remove from removal list -- tiny possibility of race condition here. Not sure ATM how to address.
			unloadQueue.remove(cacheChunk);
			return cacheChunk;
		}

		if (cacheChunk == null) {
			try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
					PreparedStatement statement = connection.prepareStatement(
							"SELECT * FROM crops_chunk WHERE world = ? and x = ? and z = ?;")) {
				statement.setString(1, worldUuid.toString());
				statement.setInt(2, chunk.getX());
				statement.setInt(3, chunk.getZ());
				try (ResultSet rs = statement.executeQuery();) {
					if (rs.next()) {
						//CropControl.getPlugin().debug("Retrieving existing chunk {0}:{1}:{2}", world_uuid, chunk_id, rs.getLong(1));
						cacheChunk = new WorldChunk();
						cacheChunk.chunkX = chunk.getX();
						cacheChunk.chunkZ = chunk.getZ();
						cacheChunk.chunkID = rs.getLong(1);
						cacheChunk.worldID = worldUuid;
						/*CropControl.getPlugin().debug("Loaded existing chunk {0}:{1},{2}:{3}:{4}", world_uuid, cacheChunk.chunkX, 
								cacheChunk.chunkZ, chunk_id, cacheChunk.chunkID);*/
					}
				}
			} catch (SQLException se) {
				CropControl.getPlugin().severe("Failed to load chunk due to error", se);
				throw new CreationError(WorldChunk.class, se);
			}
		}
		// now SELECT; if nothing found, INSERT.
		if (cacheChunk == null) {
			try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
					PreparedStatement statement = connection.prepareStatement(
							"INSERT INTO crops_chunk(world, x, z) VALUES (?, ?, ?);", Statement.RETURN_GENERATED_KEYS)) {
				statement.setString(1, worldUuid.toString());
				statement.setInt(2, chunk.getX());
				statement.setInt(3, chunk.getZ());
				statement.execute();
				try (ResultSet rs = statement.getGeneratedKeys()) {
					if (rs.next()) {
						CropControl.getPlugin().debug("Registered new chunk {0}:{1},{2}:{3}:{4}", worldUuid, chunk.getX(), 
								chunk.getZ(), chunkId, rs.getLong(1));
						cacheChunk = new WorldChunk();
						cacheChunk.chunkX = chunk.getX();
						cacheChunk.chunkZ = chunk.getZ();
						cacheChunk.chunkID = rs.getLong(1);
						cacheChunk.worldID = worldUuid;
					} else {
						throw new CreationError(WorldChunk.class, "ID assignment of chunk failed!");
					}
				}
			} catch (SQLException se) {
				CropControl.getPlugin().severe("Failed to load chunk due to error", se);
				throw new CreationError(WorldChunk.class, se);
			}
		}
		cacheChunk.stuff();
		chunks.put(chunkId, cacheChunk);
		WorldChunk.chunkCacheID.put(cacheChunk.getChunkID(), cacheChunk);
		return cacheChunk;
	}
	
	public static WorldChunk byId(long chunkID) {
		WorldChunk cacheChunk = null;
		cacheChunk = WorldChunk.chunkCacheID.get(chunkID);
		if (cacheChunk != null) {
			// remove from removal list -- tiny possibility of race condition here. Not sure ATM how to address.
			unloadQueue.remove(cacheChunk);
			return cacheChunk;
		}

		if (cacheChunk == null) {
			try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
					PreparedStatement statement = connection.prepareStatement(
							"SELECT * FROM crops_chunk WHERE chunk_id = ?;")) {
				statement.setLong(1, chunkID);
				try (ResultSet rs = statement.executeQuery();) {
					if (rs.next()) {
						cacheChunk = new WorldChunk();
						cacheChunk.chunkX = rs.getInt(3);
						cacheChunk.chunkZ = rs.getInt(4);
						cacheChunk.chunkID = rs.getLong(1);
						cacheChunk.worldID = UUID.fromString(rs.getString(2));
						/*CropControl.getPlugin().debug("Loaded existing chunk by ID {0}:{1},{2}:{3}:{4}", cacheChunk.worldID, 
								cacheChunk.chunkX, cacheChunk.chunkZ, cacheChunk.getChunkLocID(), cacheChunk.chunkID);*/
					} else {
						throw new CreationError(WorldChunk.class, "ID of Chunk referenced but nothing found");
					}
				}
			} catch (SQLException se) {
				CropControl.getPlugin().severe("Failed to load chunk due to error", se);
				throw new CreationError(WorldChunk.class, se);
			} catch (IllegalArgumentException iae) {
				CropControl.getPlugin().severe("Failed to load chunk due to data quality error", iae);
				throw new CreationError(WorldChunk.class, iae);
			}
		}
		if (cacheChunk != null) {
			cacheChunk.stuff();
			WorldChunk.chunkCacheID.put(chunkID, cacheChunk);
			Map<Long, WorldChunk> chunks = chunkCacheLoc.get(cacheChunk.getWorldID());
			if (chunks == null) {
				chunks = new ConcurrentHashMap<>();
				chunkCacheLoc.put(cacheChunk.getWorldID(), chunks);
			}
			chunks.put(cacheChunk.getChunkLocID(), cacheChunk);
		}
		return cacheChunk;
	}
}
