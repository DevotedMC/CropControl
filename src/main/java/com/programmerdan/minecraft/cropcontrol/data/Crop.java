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
 * Crop is the base holder for all crops, even if spreadable, that effectively captures who placed the crop, 
 * if it's ready for harvest, and other notable attributes.
 * 
 * Lightweight removal via boolean flag, with scheduled cleanup possible if desired (or keep the data for great
 * farm tracking!).
 *  
 * @author xFier
 * @author ProgrammerDan
 *
 */
public class Crop extends Locatable {
	
	private static ConcurrentLinkedQueue<WeakReference<Crop>> dirties = new ConcurrentLinkedQueue<WeakReference<Crop>>();
	
	private long cropID;
	private String cropType;
	private String cropState;
	private UUID placer;
	private Timestamp timeStamp;
	private boolean harvestable;
	
	private boolean dirty;
	
	private boolean removed;

	private Crop() {}
	
	public static Crop create(WorldChunk chunk, int x, int y, int z, String cropType, String cropState, UUID placer, long timeStamp, boolean harvestable) {
		return create(chunk, x, y, z, cropType, cropState, placer, new Timestamp(timeStamp), harvestable);
	}
	public static Crop create(WorldChunk chunk, int x, int y, int z, String cropType, String cropState, UUID placer, Timestamp timeStamp, boolean harvestable) {
		Crop crop = new Crop();
		crop.chunkID = chunk.getChunkID();
		crop.x = x;
		crop.y = y;
		crop.z = z;
		crop.cropType = cropType;
		crop.cropState = cropState;
		crop.placer = placer;
		crop.timeStamp = (timeStamp == null) ? new Timestamp(Calendar.getInstance().getTimeInMillis()) : timeStamp;
		crop.harvestable = harvestable;
		crop.dirty = false;
		crop.removed = false;
		
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"INSERT INTO crops_crop(chunk_id, x, y, z, type, state, placer, track_time, harvestable) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
						Statement.RETURN_GENERATED_KEYS);){
			statement.setLong(1, crop.chunkID);
			statement.setInt(2, crop.x);
			statement.setInt(3, crop.y);
			statement.setInt(4, crop.z);
			if (crop.cropType == null) {
				statement.setNull(5, Types.VARCHAR);
			} else {
				statement.setString(5, crop.cropType);
			}
			if (crop.cropState == null) {
				statement.setNull(6,  Types.VARCHAR);
			} else {
				statement.setString(6, crop.cropState);
			}
			if (crop.placer == null) {
				statement.setNull(7, Types.VARCHAR);
			} else {
				statement.setString(7, crop.placer.toString());
			}
			statement.setTimestamp(8, crop.timeStamp);
			statement.setBoolean(9, crop.harvestable);
			
			statement.execute();
			try (ResultSet rs = statement.getGeneratedKeys()) {
				if (rs.next()) { 
					crop.cropID = rs.getLong(1);
				} else {
					CropControl.getPlugin().severe("No ID returned on crop insert?!");
					throw new CreationError(Crop.class, "Database did not return an ID");
				}
			}

		} catch (SQLException se) {
			CropControl.getPlugin().severe("Failed to create a new crop: ", se);
			throw new CreationError(Crop.class, se);
		}
		chunk.register(crop); // important -- replaces any crop that might exist there now
		return crop;
	}

	public long getCropID() {
		return cropID;
	}

	public String getCropType() {
		return cropType;
	}

	public String getCropState() {
		return cropState;
	}

	public void setCropState(String cropState) {
		this.cropState = cropState;
		this.dirty = true;
		Crop.dirties.offer(new WeakReference<Crop>(this));
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
		Crop.dirties.offer(new WeakReference<Crop>(this));
		WorldChunk.byId(this.chunkID).unregister(this);
	}
	
	public static int flushDirty(Iterable<Crop> crops) {
		int totalSave = 0;
		if (crops != null) {
			int batchSize = 0;
			int totalSkip = 0;
			try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
					PreparedStatement saveCrop = connection.prepareStatement("UPDATE crops_crop SET state = ?, removed = ? WHERE crop_id = ?");) {
				for (Crop crop : crops) {
					if (crop != null && crop.dirty) {
						crop.dirty = false;
						saveCrop.setString(1, crop.cropState);
						saveCrop.setBoolean(2, crop.removed);
						saveCrop.setLong(3, crop.getCropID());
						saveCrop.addBatch();
						batchSize ++;
						totalSave ++;
					} else {
						totalSkip ++;
					}
					if (batchSize > 0 && batchSize % 100 == 0) {
						int[] batchRun = saveCrop.executeBatch();
						if (batchRun.length != batchSize) {
							CropControl.getPlugin().severe("Some elements of the Crop dirty flush didn't save? " + batchSize + " vs " + batchRun.length);
						//} else {
							//CropControl.getPlugin().debug("Crop flush: {0} saves", batchRun.length);
						}
						batchSize = 0;
					}
				}
				if (batchSize > 0 && batchSize % 100 > 0) {
					int[] batchRun = saveCrop.executeBatch();
					if (batchRun.length != batchSize) {
						CropControl.getPlugin().severe("Some elements of the Crop dirty flush didn't save? " + batchSize + " vs " + batchRun.length);
					//} else {
						//CropControl.getPlugin().debug("Crop flush: {0} saves", batchRun.length);
					}
				}
				//CropControl.getPlugin().debug("Crop flush: {0} saves {1} skips", totalSave, totalSkip);
			} catch (SQLException se) {
				CropControl.getPlugin().severe("Save of Crop dirty flush failed!: ", se);
			}	
		}
		return totalSave;
	}

	public static int saveDirty() {
		int batchSize = 0;
		int totalSave = 0;
		int totalSkip = 0;
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement saveCrop = connection.prepareStatement("UPDATE crops_crop SET state = ?, removed = ? WHERE crop_id = ?");) {
			while (!Crop.dirties.isEmpty()) {
				WeakReference<Crop> rcrop = Crop.dirties.poll();
				Crop crop = rcrop.get();
				if (crop != null && crop.dirty) {
					crop.dirty = false;
					saveCrop.setString(1, crop.cropState);
					saveCrop.setBoolean(2, crop.removed);
					saveCrop.setLong(3, crop.getCropID());
					saveCrop.addBatch();
					batchSize ++;
					totalSave ++;
				} else {
					totalSkip ++;
				}
				if (batchSize > 0 && batchSize % 100 == 0) {
					int[] batchRun = saveCrop.executeBatch();
					if (batchRun.length != batchSize) {
						CropControl.getPlugin().severe("Some elements of the Crop dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
					//} else {
						//CropControl.getPlugin().debug("Crop batch: {0} saves", batchRun.length);
					}
					batchSize = 0;
				}
			}
			if (batchSize > 0 && batchSize % 100 > 0) {
				int[] batchRun = saveCrop.executeBatch();
				if (batchRun.length != batchSize) {
					CropControl.getPlugin().severe("Some elements of the Crop dirty batch didn't save? " + batchSize + " vs " + batchRun.length);
				//} else {
					//CropControl.getPlugin().debug("Crop batch: {0} saves", batchRun.length);
				}
			}
			//CropControl.getPlugin().debug("Crop batch: {0} saves {1} skips", totalSave, totalSkip);
		} catch (SQLException se) {
			CropControl.getPlugin().severe("Save of Crop dirty batch failed!: ", se);
		}
		return totalSave;
	}

	public static List<Crop> preload(WorldChunk chunk) {
		List<Crop> crops = new ArrayList<>();
		try (Connection connection = CropControlDatabaseHandler.getInstanceData().getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT * FROM crops_crop WHERE (crop_id, x, y, z) IN (SELECT max(crop_id), x, y, z FROM crops_crop WHERE chunk_id = ? AND removed = FALSE GROUP BY x, y, z);");) {
			statement.setLong(1, chunk.getChunkID());
			try (ResultSet results = statement.executeQuery();) {
				while(results.next()) {
					Crop crop = new Crop();
					crop.cropID = results.getLong(1);
					crop.chunkID = results.getLong(2);
					crop.x = results.getInt(3);
					crop.y = results.getInt(4);
					crop.z = results.getInt(5);
					crop.cropType = results.getString(6);
					crop.cropState = results.getString(7);
					try {
						crop.placer = UUID.fromString(results.getString(8));
					} catch (IllegalArgumentException iae) {
						crop.placer = null;
					}
					crop.timeStamp = results.getTimestamp(9);
					crop.harvestable = results.getBoolean(10);
					crop.removed = results.getBoolean(11);
					crop.dirty = false;
					if (crop.removed) {
						CropControl.getPlugin().warning("A removed crop was loaded at {0}, {1}, {2}", crop.x, crop.y, crop.z);
						continue;
					}
					crops.add(crop);
				}
			}
		} catch (SQLException e) {
			CropControl.getPlugin().severe("Failed to preload crops", e);
		}
		// Note we don't register as we assume the crop is being preloaded _by_ the chunk
		return crops;
	}
}
