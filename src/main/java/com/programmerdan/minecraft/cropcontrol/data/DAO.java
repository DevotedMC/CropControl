package com.programmerdan.minecraft.cropcontrol.data;

import com.programmerdan.minecraft.cropcontrol.handler.CropControlDatabaseHandler;

/**
 * Accessor wrapper
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
	
}
