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


/**
 * Basic data storage for a Sapling, which can grow into a tree. Soft deletion via flag w/ optional hard deletion later or keep 'em
 * for history.
 * 
 * @author xFier
 * @author ProgrammerDan
 *
 */
public class Sapling extends Locatable {

	private static ConcurrentLinkedQueue<WeakReference<Sapling>> dirties = new ConcurrentLinkedQueue<WeakReference<Sapling>>();

	private long saplingID;
	private String saplingType;
	private UUID placer;
	private Timestamp timeStamp;
	private boolean harvestable;
	private boolean dirty;

	private boolean removed;

	private Sapling() {
	}

	public static Sapling create(WorldChunk chunk, int x, int y, int z, String saplingType, UUID placer,
			long timeStamp, boolean harvestable) {
		return create(chunk, x, y, z, saplingType, placer, new Timestamp(timeStamp), harvestable);
	}
	public static Sapling create(WorldChunk chunk, int x, int y, int z, String saplingType, UUID placer,
			Timestamp timeStamp, boolean harvestable) {
		Sapling sapling = new Sapling();
		sapling.chunkID = chunk.getChunkID();
		sapling.x = x;
		sapling.y = y;
		sapling.z = z;
		sapling.saplingType = saplingType;
		sapling.placer = placer;
		sapling.timeStamp = (timeStamp == null) ? new Timestamp(Calendar.getInstance().getTimeInMillis()) : timeStamp;
		sapling.harvestable = harvestable;
		sapling.dirty = false;
		sapling.removed = false;

		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"INSERT INTO crops_sapling(chunk_id, x, y, z, type, placer, track_time, harvestable) VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
						Statement.RETURN_GENERATED_KEYS);) {
			statement.setLong(1, sapling.chunkID);
			statement.setInt(2, sapling.x);
			statement.setInt(3, sapling.y);
			statement.setInt(4, sapling.z);
			if (sapling.saplingType == null) {
				statement.setNull(5, Types.VARCHAR);
			} else {
				statement.setString(5, sapling.saplingType);
			}
			if (sapling.placer == null) {
				statement.setNull(6, Types.VARCHAR);
			} else {
				statement.setString(6, sapling.placer.toString());
			}
			statement.setTimestamp(7, sapling.timeStamp);
			statement.setBoolean(8, sapling.harvestable);

			statement.execute();
			try (ResultSet rs = statement.getGeneratedKeys()) {
				if (rs.next()) {
					sapling.saplingID = rs.getLong(1);
				} else {
					CropControl.getPlugin().severe("No ID returned on sapling insert?!");
					throw new CreationError(Sapling.class, "Database did not return an ID");
				}
			}

		} catch (SQLException se) {
			CropControl.getPlugin().severe("Failed to create a new sapling: ", se);
			throw new CreationError(Sapling.class, se);
		}
		chunk.register(sapling); // important -- replaces any crop that might
									// exist there now
		return sapling;
	}

	public long getSaplingID() {
		return saplingID;
	}

	public String getSaplingType() {
		return saplingType;
	}

	public UUID getPlacer() {
		return placer;
	}

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public boolean getHarvestable() {
		return harvestable;
	}

	public void setRemoved() {
		this.removed = true;
		this.dirty = true;
		Sapling.dirties.offer(new WeakReference<Sapling>(this));
		WorldChunk.byId(this.chunkID).unregister(this);
	}

	public static void flushDirty(Iterable<Sapling> saplings) {
		if (saplings != null) {
			int batchSize = 0;
			try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
					PreparedStatement saveSapling = connection.prepareStatement("UPDATE crops_sapling SET removed = ? WHERE sapling_id = ?");) {
				for (Sapling sapling : saplings) {
					if (sapling != null && sapling.dirty) {
						sapling.dirty = false;
						saveSapling.setBoolean(1, sapling.removed);
						saveSapling.setLong(2, sapling.getSaplingID());
						saveSapling.addBatch();
						batchSize ++;
					}
					if (batchSize > 0 && batchSize % 100 == 0) {
						int[] batchRun = saveSapling.executeBatch();
						if (batchRun.length != batchSize) {
							CropControl.getPlugin().severe("Some elements of the Sapling dirty flush didn't save? " + batchSize + " vs " + batchRun.length);
						} else {
							CropControl.getPlugin().debug("Sapling flush: {0} saves", batchRun.length);
						}
						batchSize = 0;
					}
				}
				if (batchSize > 0 && batchSize % 100 > 0) {
					int[] batchRun = saveSapling.executeBatch();
					if (batchRun.length != batchSize) {
						CropControl.getPlugin().severe("Some elements of the Sapling dirty flush didn't save? " + batchSize + " vs " + batchRun.length);
					} else {
						CropControl.getPlugin().debug("Sapling flush: {0} saves", batchRun.length);
					}
				}
			} catch (SQLException se) {
				CropControl.getPlugin().severe("Save of Sapling dirty flush failed!: ", se);
			}
		}
	}
	
	public static void saveDirty() {
		int batchSize = 0;
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement saveSapling = connection.prepareStatement("UPDATE crops_sapling SET removed = ? WHERE sapling_id = ?");) {
			while (!Sapling.dirties.isEmpty()) {
				WeakReference<Sapling> rsapling = Sapling.dirties.poll();
				Sapling sapling = rsapling.get();
				if (sapling != null && sapling.dirty) {
					sapling.dirty = false;
					saveSapling.setBoolean(1, sapling.removed);
					saveSapling.setLong(2, sapling.getSaplingID());
					saveSapling.addBatch();
					batchSize ++;
				}
				if (batchSize > 0 && batchSize % 100 == 0) {
					int[] batchRun = saveSapling.executeBatch();
					if (batchRun.length != batchSize) {
						CropControl.getPlugin().severe("Some elements of the Sapling dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
					} else {
						CropControl.getPlugin().debug("Sapling batch: {0} saves", batchRun.length);
					}
					batchSize = 0;
				}
			}
			if (batchSize > 0 && batchSize % 100 > 0) {
				int[] batchRun = saveSapling.executeBatch();
				if (batchRun.length != batchSize) {
					CropControl.getPlugin().severe("Some elements of the Sapling dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
				} else {
					CropControl.getPlugin().debug("Sapling batch: {0} saves", batchRun.length);
				}
			}
		} catch (SQLException se) {
			CropControl.getPlugin().severe("Save of Sapling dirty batch failed!: ", se);
		}
	}

	public static List<Sapling> preload(WorldChunk chunk) {
		List<Sapling> saplings = new ArrayList<Sapling>();
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT * FROM crops_sapling WHERE (sapling_id, x, y, z) IN (SELECT max(sapling_id), x, y, z FROM crops_sapling WHERE chunk_id = ? AND removed = FALSE GROUP BY x, y, z);");) {
			statement.setLong(1, chunk.getChunkID());
			try (ResultSet results = statement.executeQuery();) {
				while(results.next()) {
					Sapling sapling = new Sapling();
					sapling.saplingID = results.getLong(1);
					sapling.chunkID = results.getLong(2);
					sapling.x = results.getInt(3);
					sapling.y = results.getInt(4);
					sapling.z = results.getInt(5);
					sapling.saplingType = results.getString(6);
					try {
						sapling.placer = UUID.fromString(results.getString(7));
					} catch (IllegalArgumentException iae) {
						sapling.placer = null;
					}
					sapling.timeStamp = results.getTimestamp(8);
					sapling.harvestable = results.getBoolean(9);
					sapling.removed = results.getBoolean(10);
					sapling.dirty = false;
					saplings.add(sapling);
				}
			}
		} catch (SQLException e) {
			CropControl.getPlugin().severe("Failed to preload saplings", e);
		}
		// Note we don't register as we assume the sapling is being preloaded _by_ the chunk
		return saplings;
	}

}
