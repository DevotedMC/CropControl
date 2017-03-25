package com.programmerdan.minecraft.cropcontrol.data;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.programmerdan.minecraft.cropcontrol.CreationError;
import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlDatabaseHandler;

public class Tree extends Locatable {
	
	private static ConcurrentLinkedQueue<WeakReference<Tree>> dirties = new ConcurrentLinkedQueue<WeakReference<Tree>>();

	private long treeID;
	private String treeType;
	private UUID placer;
	private Timestamp timeStamp;

	private boolean dirty;

	private boolean removed;

	private Tree() {
	}

	public static Tree create(WorldChunk chunk, int x, int y, int z, String treeType, UUID placer,
			long timeStamp) {
		return create(chunk, x, y, z, treeType, placer, new Timestamp(timeStamp));
	}
	public static Tree create(WorldChunk chunk, int x, int y, int z, String treeType, UUID placer,
			Timestamp timeStamp) {
		Tree tree = new Tree();
		tree.chunkID = chunk.getChunkID();
		tree.x = x;
		tree.y = y;
		tree.z = z;
		tree.treeType = treeType;
		tree.placer = placer;
		tree.timeStamp = (timeStamp == null) ? new Timestamp(Calendar.getInstance().getTimeInMillis()) : timeStamp;
		tree.dirty = false;
		tree.removed = false;

		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"INSERT INTO crops_tree(chunk_id, x, y, z, type, placer, track_time) VALUES (?, ?, ?, ?, ?, ?, ?);",
						Statement.RETURN_GENERATED_KEYS);) {
			statement.setLong(1, tree.chunkID);
			statement.setInt(2, tree.x);
			statement.setInt(3, tree.y);
			statement.setInt(4, tree.z);
			if (tree.treeType == null) {
				statement.setNull(5, Types.VARCHAR);
			} else {
				statement.setString(5, tree.treeType);
			}
			if (tree.placer == null) {
				statement.setNull(6, Types.VARCHAR);
			} else {
				statement.setString(6, tree.placer.toString());
			}
			statement.setTimestamp(7, tree.timeStamp);

			statement.execute();
			try (ResultSet rs = statement.getGeneratedKeys()) {
				if (rs.next()) {
					tree.treeID = rs.getLong(1);
				} else {
					CropControl.getPlugin().severe("No ID returned on tree insert?!");
					throw new CreationError(Tree.class, "Database did not return an ID");
				}
			}

		} catch (SQLException se) {
			CropControl.getPlugin().severe("Failed to create a new tree: ", se);
			throw new CreationError(Tree.class, se);
		}
		chunk.register(tree); // important -- replaces any tree that might
								// exist there now
		return tree;
	}

	public void updateLocation(long chunkID, int x, int y, int z) {
		this.chunkID = chunkID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dirty = true;
		Tree.dirties.offer(new WeakReference<Tree>(this));
	}

	public void setChunkID(long chunkID) {
		this.chunkID = chunkID;
		this.dirty = true;
		Tree.dirties.offer(new WeakReference<Tree>(this));
	}

	public void setX(int x) {
		this.x = x;
		this.dirty = true;
		Tree.dirties.offer(new WeakReference<Tree>(this));
	}

	public void setY(int y) {
		this.y = y;
		this.dirty = true;
		Tree.dirties.offer(new WeakReference<Tree>(this));
	}

	public void setZ(int z) {
		this.z = z;
		this.dirty = true;
		Tree.dirties.offer(new WeakReference<Tree>(this));
	}

	public long getTreeID() {
		return treeID;
	}

	public String getTreeType() {
		return treeType;
	}

	public UUID getPlacer() {
		return placer;
	}

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public void setRemoved() {
		this.removed = true;
		this.dirty = true;
		Tree.dirties.offer(new WeakReference<Tree>(this));
	}

	public static void flushDirty(Iterable<Tree> trees) {
		if (trees != null) {
			int batchSize = 0;
			try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
					PreparedStatement saveTree = connection.prepareStatement("UPDATE crops_tree SET chunk_id = ?, x = ?, y = ?, z = ?, removed = ? WHERE tree_id = ?");) {
				for (Tree tree : trees) {
					if (tree != null && tree.dirty) {
						tree.dirty = false;
						saveTree.setLong(1, tree.getChunkID());
						saveTree.setInt(2, tree.getX());
						saveTree.setInt(3, tree.getY());
						saveTree.setInt(4, tree.getZ());
						saveTree.setBoolean(5, tree.removed);
						saveTree.setLong(6, tree.getTreeID());
						saveTree.addBatch();
						batchSize ++;
					}
					if (batchSize > 0 && batchSize % 100 == 0) {
						int[] batchRun = saveTree.executeBatch();
						if (batchRun.length != batchSize) {
							CropControl.getPlugin().severe("Some elements of the Tree dirty flush didn't save? " + batchSize + " vs " + batchRun.length);
						} else {
							CropControl.getPlugin().debug("Tree flush: {0} saves", batchRun.length);
						}
						batchSize = 0;
					}
				}
				if (batchSize > 0 && batchSize % 100 > 0) {
					int[] batchRun = saveTree.executeBatch();
					if (batchRun.length != batchSize) {
						CropControl.getPlugin().severe("Some elements of the Tree dirty flush didn't save? " + batchSize + " vs " + batchRun.length);
					} else {
						CropControl.getPlugin().debug("Tree flush: {0} saves", batchRun.length);
					}
				}
			} catch (SQLException se) {
				CropControl.getPlugin().severe("Save of Tree dirty flush failed!: ", se);
			}
		}
	}
	
	public static void saveDirty() {
		int batchSize = 0;
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement saveTree = connection.prepareStatement("UPDATE crops_tree SET chunk_id = ?, x = ?, y = ?, z = ?, removed = ? WHERE tree_id = ?");) {
			while (!Tree.dirties.isEmpty()) {
				WeakReference<Tree> rtree = Tree.dirties.poll();
				Tree tree = rtree.get();
				if (tree != null && tree.dirty) {
					tree.dirty = false;
					saveTree.setLong(1, tree.getChunkID());
					saveTree.setInt(2, tree.getX());
					saveTree.setInt(3, tree.getY());
					saveTree.setInt(4, tree.getZ());
					saveTree.setBoolean(5, tree.removed);
					saveTree.setLong(6, tree.getTreeID());
					saveTree.addBatch();
					batchSize ++;
				}
				if (batchSize > 0 && batchSize % 100 == 0) {
					int[] batchRun = saveTree.executeBatch();
					if (batchRun.length != batchSize) {
						CropControl.getPlugin().severe("Some elements of the Tree dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
					} else {
						CropControl.getPlugin().debug("Tree batch: {0} saves", batchRun.length);
					}
					batchSize = 0;
				}
			}
			if (batchSize > 0 && batchSize % 100 > 0) {
				int[] batchRun = saveTree.executeBatch();
				if (batchRun.length != batchSize) {
					CropControl.getPlugin().severe("Some elements of the Tree dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
				} else {
					CropControl.getPlugin().debug("Tree batch: {0} saves", batchRun.length);
				}
			}
		} catch (SQLException se) {
			CropControl.getPlugin().severe("Save of Tree dirty batch failed!: ", se);
		}
	}

	public static List<Tree> preload(WorldChunk chunk) {
		List<Tree> trees = new ArrayList<Tree>();
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT * FROM crops_tree WHERE (tree_id, x, y, z) IN (SELECT max(tree_id), x, y, z FROM crops_tree WHERE chunk_id = ? AND removed = FALSE GROUP BY x, y, z);");) {
			statement.setLong(1, chunk.getChunkID());
			try (ResultSet results = statement.executeQuery();) {
				while(results.next()) {
					Tree tree = new Tree();
					tree.treeID = results.getLong(1);
					tree.chunkID = results.getLong(2);
					tree.x = results.getInt(3);
					tree.y = results.getInt(4);
					tree.z = results.getInt(5);
					tree.treeType = results.getString(6);
					try {
						tree.placer = UUID.fromString(results.getString(7));
					} catch (IllegalArgumentException iae) {
						tree.placer = null;
					}
					tree.timeStamp = results.getTimestamp(8);
					tree.removed = results.getBoolean(9);
					tree.dirty = false;
					trees.add(tree);
				}
			}
		} catch (SQLException e) {
			CropControl.getPlugin().severe("Failed to preload trees", e);
		}
		// Note we don't register as we assume the trees is being preloaded _by_ the chunk
		return trees;
	}
	
	public static Tree byId(long treeID) {
		Tree tree = null;
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT * FROM crops_tree WHERE tree_id = ?;");) {
			statement.setLong(1, treeID);
			try (ResultSet results = statement.executeQuery();) {
				if (results.next()) {
					tree = new Tree();
					tree.treeID = results.getLong(1);
					tree.chunkID = results.getLong(2);
					tree.x = results.getInt(3);
					tree.y = results.getInt(4);
					tree.z = results.getInt(5);
					tree.treeType = results.getString(6);
					try {
						tree.placer = UUID.fromString(results.getString(7));
					} catch (IllegalArgumentException iae) {
						tree.placer = null;
					}
					tree.timeStamp = results.getTimestamp(8);
					tree.removed = results.getBoolean(9);
					tree.dirty = false;
				} else {
					throw new CreationError(Tree.class, "Unable to retrieve tree by ID");
				}
			}
		} catch (SQLException e) {
			CropControl.getPlugin().severe("Failed to load trees", e);
		}
		return tree;
	}
}
