package com.programmerdan.minecraft.cropcontrol.data;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.programmerdan.minecraft.cropcontrol.CreationError;
import com.programmerdan.minecraft.cropcontrol.CropControl;
import com.programmerdan.minecraft.cropcontrol.handler.CropControlDatabaseHandler;

/**
 * Every tree is composed of a number of Components fixed in actual space. This captures those elements for tracking purposes,
 * and is soft deleted via flag when they are removed.
 * 
 *  
 * @author xFier
 * @author ProgrammerDan
 *
 */
public class TreeComponent extends Locatable {
	
	private static ConcurrentLinkedQueue<WeakReference<TreeComponent>> dirties = new ConcurrentLinkedQueue<WeakReference<TreeComponent>>();

	private long treeComponentID;
	private long treeID;
	private String treeType;
	private UUID placer;
	private boolean harvestable;

	private boolean dirty;

	private boolean removed;

	private TreeComponent() {
	}

	public static TreeComponent create(Tree tree, WorldChunk chunk, int x, int y, int z, String treeType, UUID placer,
			boolean harvestable) {
		return create(tree.getTreeID(), chunk, x, y, z, treeType, placer, harvestable);
	}
	
	public static TreeComponent create(long treeId, WorldChunk chunk, int x, int y, int z, String treeType, UUID placer, boolean harvestable) {
		TreeComponent component = new TreeComponent();
		component.treeID = treeId;
		component.chunkID = chunk.getChunkID();
		component.x = x;
		component.y = y;
		component.z = z;
		component.treeType = treeType;
		component.placer = placer;
		component.harvestable = harvestable;
		component.dirty = false;
		component.removed = false;

		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"INSERT INTO crops_tree_component(tree_id, chunk_id, x, y, z, type, placer, harvestable) VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
						Statement.RETURN_GENERATED_KEYS);) {
			statement.setLong(1, component.treeID);
			statement.setLong(2, component.chunkID);
			statement.setInt(3, component.x);
			statement.setInt(4, component.y);
			statement.setInt(5, component.z);
			if (component.treeType == null) {
				statement.setNull(6, Types.VARCHAR);
			} else {
				statement.setString(6, component.treeType);
			}
			if (component.placer == null) {
				statement.setNull(7, Types.VARCHAR);
			} else {
				statement.setString(7, component.placer.toString());
			}
			statement.setBoolean(8, component.harvestable);

			statement.execute();
			try (ResultSet rs = statement.getGeneratedKeys()) {
				if (rs.next()) {
					component.treeComponentID = rs.getLong(1);
				} else {
					CropControl.getPlugin().severe("No ID returned on tree component insert?!");
					throw new CreationError(TreeComponent.class, "Database did not return an ID");
				}
			}

		} catch (SQLException se) {
			CropControl.getPlugin().severe("Failed to create a new tree component: ", se);
			throw new CreationError(TreeComponent.class, se);
		}
		chunk.register(component); // important -- replaces any tree component that might
								// exist there now
		return component;
	}

	
	public long getTreeComponentID() {
		return treeComponentID;
	}

	public long getTreeID() {
		return treeID;
	}

	public void updateLocation(long chunkID, int x, int y, int z) {
		this.chunkID = chunkID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dirty = true;
		TreeComponent.dirties.offer(new WeakReference<TreeComponent>(this));
	}

	public void setChunkID(long chunkID) {
		this.chunkID = chunkID;
		this.dirty = true;
		TreeComponent.dirties.offer(new WeakReference<TreeComponent>(this));
	}

	public void setX(int x) {
		this.x = x;
		this.dirty = true;
		TreeComponent.dirties.offer(new WeakReference<TreeComponent>(this));
	}

	public void setY(int y) {
		this.y = y;
		this.dirty = true;
		TreeComponent.dirties.offer(new WeakReference<TreeComponent>(this));
	}

	public void setZ(int z) {
		this.z = z;
		this.dirty = true;
		TreeComponent.dirties.offer(new WeakReference<TreeComponent>(this));
	}

	public String getTreeType() {
		return treeType;
	}

	public UUID getPlacer() {
		return placer;
	}

	public boolean isHarvestable() {
		return harvestable;
	}

	public void setHarvestable(boolean harvestable) {
		this.harvestable = harvestable;
		this.dirty = true;
		TreeComponent.dirties.offer(new WeakReference<TreeComponent>(this));
	}

	public void setRemoved() {
		this.removed = true;
		this.dirty = true;
		TreeComponent.dirties.offer(new WeakReference<TreeComponent>(this));
		WorldChunk.remove(this);
		WorldChunk.byId(this.chunkID).unregister(this);
	}

	public static void flushDirty(Iterable<TreeComponent> components) {
		if (components != null) {
			int batchSize = 0;
			try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
					PreparedStatement saveComponent = connection.prepareStatement("UPDATE crops_tree_component SET chunk_id = ?, x = ?, y = ?, z = ?, harvestable = ?, removed = ? WHERE tree_component_id = ?");) {
				for (TreeComponent treeComponent : components) {
					if (treeComponent != null && treeComponent.dirty) {
						treeComponent.dirty = false;
						saveComponent.setLong(1, treeComponent.getChunkID());
						saveComponent.setInt(2, treeComponent.getX());
						saveComponent.setInt(3, treeComponent.getY());
						saveComponent.setInt(4, treeComponent.getZ());
						saveComponent.setBoolean(5, treeComponent.harvestable);
						saveComponent.setBoolean(6, treeComponent.removed);
						saveComponent.setLong(7, treeComponent.getTreeComponentID());
						saveComponent.addBatch();
						batchSize ++;
					}
					if (batchSize > 0 && batchSize % 100 == 0) {
						int[] batchRun = saveComponent.executeBatch();
						if (batchRun.length != batchSize) {
							CropControl.getPlugin().severe("Some elements of the Tree Component dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
						} else {
							CropControl.getPlugin().debug("Tree Component batch: {0} saves", batchRun.length);
						}
						batchSize = 0;
					}
				}
				if (batchSize > 0 && batchSize % 100 > 0) {
					int[] batchRun = saveComponent.executeBatch();
					if (batchRun.length != batchSize) {
						CropControl.getPlugin().severe("Some elements of the Tree Component dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
					} else {
						CropControl.getPlugin().debug("Tree Component batch: {0} saves", batchRun.length);
					}
				}
			} catch (SQLException se) {
				CropControl.getPlugin().severe("Save of Tree Component dirty batch failed!: ", se);
			}
		}
	}
	
	public static void saveDirty() {
		int batchSize = 0;
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement saveComponent = connection.prepareStatement("UPDATE crops_tree_component SET chunk_id = ?, x = ?, y = ?, z = ?, harvestable = ?, removed = ? WHERE tree_component_id = ?");) {
			while (!TreeComponent.dirties.isEmpty()) {
				WeakReference<TreeComponent> rcomponent = TreeComponent.dirties.poll();
				TreeComponent treeComponent = rcomponent.get();
				if (treeComponent != null && treeComponent.dirty) {
					treeComponent.dirty = false;
					saveComponent.setLong(1, treeComponent.getChunkID());
					saveComponent.setInt(2, treeComponent.getX());
					saveComponent.setInt(3, treeComponent.getY());
					saveComponent.setInt(4, treeComponent.getZ());
					saveComponent.setBoolean(5, treeComponent.harvestable);
					saveComponent.setBoolean(6, treeComponent.removed);
					saveComponent.setLong(7, treeComponent.getTreeComponentID());
					saveComponent.addBatch();
					batchSize ++;
				}
				if (batchSize > 0 && batchSize % 100 == 0) {
					int[] batchRun = saveComponent.executeBatch();
					if (batchRun.length != batchSize) {
						CropControl.getPlugin().severe("Some elements of the Tree Component dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
					} else {
						CropControl.getPlugin().debug("Tree Component batch: {0} saves", batchRun.length);
					}
					batchSize = 0;
				}
			}
			if (batchSize > 0 && batchSize % 100 > 0) {
				int[] batchRun = saveComponent.executeBatch();
				if (batchRun.length != batchSize) {
					CropControl.getPlugin().severe("Some elements of the Tree Component dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
				} else {
					CropControl.getPlugin().debug("Tree Component batch: {0} saves", batchRun.length);
				}
			}
		} catch (SQLException se) {
			CropControl.getPlugin().severe("Save of Tree Component dirty batch failed!: ", se);
		}
	}

	public static List<TreeComponent> preload(WorldChunk chunk) {
		List<TreeComponent> components = new ArrayList<TreeComponent>();
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT * FROM crops_tree_component WHERE (tree_component_id, x, y, z) IN (SELECT max(tree_component_id), x, y, z FROM crops_tree_component WHERE chunk_id = ? AND removed = FALSE GROUP BY x, y, z);");) {
			statement.setLong(1, chunk.getChunkID());
			try (ResultSet results = statement.executeQuery();) {
				while(results.next()) {
					TreeComponent component = new TreeComponent();
					component.treeComponentID = results.getLong(1);
					component.treeID = results.getLong(2);
					component.chunkID = results.getLong(3);
					component.x = results.getInt(4);
					component.y = results.getInt(5);
					component.z = results.getInt(6);
					component.treeType = results.getString(7);
					try {
						component.placer = UUID.fromString(results.getString(8));
					} catch (IllegalArgumentException iae) {
						component.placer = null;
					}
					component.harvestable = results.getBoolean(9);
					component.removed = results.getBoolean(10);
					component.dirty = false;
					components.add(component);
				}
			}
		} catch (SQLException e) {
			CropControl.getPlugin().severe("Failed to preload tree components", e);
		}
		// Note we don't register as we assume the tree components is being preloaded _by_ the chunk
		return components;
	}
}
