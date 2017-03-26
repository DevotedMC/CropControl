package com.programmerdan.minecraft.cropcontrol.data;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
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

public class WorldChunk {
	
	// consider locating all related to the chunk into the chunk, load as chunk, etc.
	private Map<Long, Crop> cropCacheID = new ConcurrentHashMap<Long, Crop>();
	private Map<Locatable, Crop> cropCacheLoc = new ConcurrentHashMap<Locatable, Crop>();

	private Map<Long, Sapling> saplingCacheID = new ConcurrentHashMap<Long, Sapling>();
	private Map<Locatable, Sapling> saplingCacheLoc = new ConcurrentHashMap<Locatable, Sapling>();
	
	private static Map<Long, Tree> universalTreeCache = new ConcurrentHashMap<Long, Tree>();
	private static Map<Long, Set<TreeComponent>> universalTreeComponentCache = new ConcurrentHashMap<Long, Set<TreeComponent>>();
	
	private Map<Long, Tree> treeCacheID = new ConcurrentHashMap<Long, Tree>();
	private Map<Locatable, Tree> treeCacheLoc = new ConcurrentHashMap<Locatable, Tree>();
	
	private Map<Long, TreeComponent> componentCacheID = new ConcurrentHashMap<Long, TreeComponent>();
	private Map<Locatable, TreeComponent> componentCacheLoc = new ConcurrentHashMap<Locatable, TreeComponent>();
	
	private static Map<Long, WorldChunk> chunkCacheID = new ConcurrentHashMap<Long, WorldChunk>();
	private static Map<UUID, Map<Long, WorldChunk>> chunkCacheLoc = new ConcurrentHashMap<UUID, Map<Long, WorldChunk>>(); // using transposed x,z
	
	private static ConcurrentLinkedQueue<WorldChunk> unloadQueue = new ConcurrentLinkedQueue<WorldChunk>();

	private long chunkID;
	private UUID worldID;
	private int chunkX;
	private int chunkZ;
	
	private transient long retrieveTime;
	
	private WorldChunk() {
		retrieveTime = System.currentTimeMillis();
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
	
	
	private void stuff() {
		List<Crop> crops = Crop.preload(this);
		for (Crop crop : crops) {
			cropCacheLoc.put(new Locatable(crop.getChunkID(), crop.getX(), crop.getY(), crop.getZ()), crop);
			cropCacheID.put(crop.getCropID(), crop);
		}
		
		List<Sapling> saplings = Sapling.preload(this);
		for (Sapling sapling: saplings) {
			saplingCacheLoc.put(new Locatable(sapling.getChunkID(), sapling.getX(), sapling.getY(), sapling.getZ()), sapling);
			saplingCacheID.put(sapling.getSaplingID(), sapling);			
		}
		
		List<Tree> trees = Tree.preload(this);
		for (Tree tree: trees) {
			treeCacheLoc.put(new Locatable(tree.getChunkID(), tree.getX(), tree.getY(), tree.getZ()), tree);
			treeCacheID.put(tree.getTreeID(), tree);
			WorldChunk.universalTreeCache.put(tree.getTreeID(), tree);
		}
		
		List<TreeComponent> components = TreeComponent.preload(this);
		for (TreeComponent component : components) {
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
		Crop preExist = cropCacheLoc.remove((Locatable) crop); // did we have one here already?
		if (preExist != null) {
			cropCacheID.remove(preExist.getCropID());
		}
		cropCacheLoc.put(new Locatable(crop.getChunkID(), crop.getX(), crop.getY(), crop.getZ()), crop);
		cropCacheID.put(crop.getCropID(), crop);
	}
	
	public Crop getCrop(int x, int y, int z) {
		Crop result = cropCacheLoc.get(new Locatable(chunkID, x, y, z));
		return result;
	}

	public void register(Sapling sapling) {
		Sapling preExist = saplingCacheLoc.remove((Locatable) sapling); // did we have one here already?
		if (preExist != null) {
			saplingCacheID.remove(preExist.getSaplingID());
		}
		saplingCacheLoc.put(new Locatable(sapling.getChunkID(), sapling.getX(), sapling.getY(), sapling.getZ()), sapling);
		saplingCacheID.put(sapling.getSaplingID(), sapling);
	}
	
	public Sapling getSapling(int x, int y, int z) {
		Sapling result = saplingCacheLoc.get(new Locatable(chunkID, x, y, z));
		return result;
	}

	public void register(Tree tree) {
		Tree preExist = treeCacheLoc.remove((Locatable) tree); // did we have one here already?
		if (preExist != null) {
			treeCacheID.remove(preExist.getTreeID());
		}
		treeCacheLoc.put(new Locatable(tree.getChunkID(), tree.getX(), tree.getY(), tree.getZ()), tree);
		treeCacheID.put(tree.getTreeID(), tree);
		WorldChunk.universalTreeCache.put(tree.getTreeID(), tree);
	}
	
	public void unregister(Tree tree) {
		Tree preExist = treeCacheLoc.remove((Locatable) tree); // did we have one here already?
		if (preExist != null) {
			treeCacheID.remove(preExist.getTreeID());
			treeCacheLoc.remove(new Locatable(tree.getChunkID(), tree.getX(), tree.getY(), tree.getZ()));
		}
	}
	
	public Tree getTree(int x, int y, int z) {
		Tree result = treeCacheLoc.get(new Locatable(chunkID, x, y, z));
		return result;
	}
	
	public static Tree getTree(long treeID) {
		Tree tree = WorldChunk.universalTreeCache.get(treeID);
		if (tree == null) {
			tree = Tree.byId(treeID);
		}
		return tree;
	}
	
	public static void remove(Tree tree) {
		WorldChunk.universalTreeCache.remove(tree.getTreeID());
	}

	public void register(TreeComponent component) {
		TreeComponent preExist = componentCacheLoc.remove((Locatable) component); // did we have one here already?
		if (preExist != null) {
			componentCacheID.remove(preExist.getTreeComponentID());
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
		TreeComponent preExist = componentCacheLoc.remove((Locatable) component); // did we have one here already?
		if (preExist != null) {
			componentCacheID.remove(preExist.getTreeID());
			componentCacheLoc.remove(new Locatable(component.getChunkID(), component.getX(), component.getY(), component.getZ()));
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
		return result;
	}
	
	public static List<TreeComponent> getTreeComponents(Tree tree) {
		return getTreeComponents(tree.getTreeID());
	}
	
	public static List<TreeComponent> getTreeComponents(long treeId) {
		List<TreeComponent> list = new CopyOnWriteArrayList<TreeComponent>();
		Set<TreeComponent> set = WorldChunk.universalTreeComponentCache.get(treeId);
		if (set != null) {
			list.addAll(set);
		}
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
		long chunk_id = ((long) chunk.getX() << 32L) + (long) chunk.getZ();
		UUID world_uuid = chunk.getWorld().getUID();
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
		int unloads = 0;
		long start = System.currentTimeMillis();
		long maxTime = 500; // don't spend more then half a second unloading.
		
		while (!unloadQueue.isEmpty()) {
			WorldChunk unload = unloadQueue.poll();
			
			if (unload == null) continue;
			
			// extract crops and such
			Iterable<Crop> crops = unload.cropCacheID.values();
			Iterable<Sapling> saplings = unload.saplingCacheID.values();
			Iterable<Tree> trees = unload.treeCacheID.values();
			Iterable<TreeComponent> components = unload.componentCacheID.values();
			
			// remove from cache
			long chunk_id = ((long) unload.getChunkX() << 32L) + (long) unload.getChunkZ();
			UUID world_uuid = unload.getWorldID();
			Map<Long, WorldChunk> chunks = chunkCacheLoc.get(world_uuid);
			if (chunks == null) {
				continue;
			}
			WorldChunk cacheChunk = chunks.remove(chunk_id);
			if (cacheChunk != null) {
				chunkCacheID.remove(unload.getChunkID());
			}
			
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
			Crop.flushDirty(crops);
			Sapling.flushDirty(saplings);
			Tree.flushDirty(trees);
			TreeComponent.flushDirty(components);
			
			// excessive local cleanup
			crops = null;
			saplings = null;
			trees = null;
			components = null;
			
			unloads ++;
			
			if (System.currentTimeMillis() - start > maxTime) {
				break;
			}
		}
		CropControl.getPlugin().debug("Unloaded {0} chunks in {1} ms", unloads, (System.currentTimeMillis() - start));
	}
	
	public static WorldChunk getChunk(Chunk chunk) {
		WorldChunk cacheChunk = null;
		long chunk_id = ((long) chunk.getX() << 32L) + (long) chunk.getZ();
		UUID world_uuid = chunk.getWorld().getUID();
		Map<Long, WorldChunk> chunks = chunkCacheLoc.get(world_uuid);
		if (chunks == null) {
			chunks = new ConcurrentHashMap<Long, WorldChunk>();
			chunkCacheLoc.put(world_uuid, chunks);
		}
		cacheChunk = chunks.get(chunk_id);
		if (cacheChunk != null) {
			// remove from removal list -- tiny possibility of race condition here. Not sure ATM how to address.
			unloadQueue.remove(cacheChunk);
			return cacheChunk;
		}

		if (cacheChunk == null) {
			try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
					PreparedStatement statement = connection.prepareStatement(
							"SELECT * FROM crops_chunk WHERE world = ? and x = ? and z = ?;")) {
				statement.setString(1, world_uuid.toString());
				statement.setInt(2, chunk.getX());
				statement.setInt(3, chunk.getZ());
				try (ResultSet rs = statement.executeQuery();) {
					if (rs.next()) {
						cacheChunk = new WorldChunk();
						cacheChunk.chunkX = chunk.getX();
						cacheChunk.chunkZ = chunk.getZ();
						cacheChunk.chunkID = rs.getLong(1);
						cacheChunk.worldID = world_uuid;
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
							"INSERT INTO crops_chunk(world, x, y, z) VALUES (?, ?, ?);", Statement.RETURN_GENERATED_KEYS)) {
				statement.setString(1, world_uuid.toString());
				statement.setInt(2, chunk.getX());
				statement.setInt(3, chunk.getZ());
				statement.execute();
				try (ResultSet rs = statement.getGeneratedKeys()) {
					if (rs.next()) {
						cacheChunk = new WorldChunk();
						cacheChunk.chunkX = chunk.getX();
						cacheChunk.chunkZ = chunk.getZ();
						cacheChunk.chunkID = rs.getLong(1);
						cacheChunk.worldID = world_uuid;
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
		
		cacheChunk.stuff();
		return cacheChunk;
	}
}
