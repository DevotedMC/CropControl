package com.programmerdan.minecraft.cropcontrol;

/**
 * Used to indicate something went wrong when creating a new Data Object. Preferred over simply returning null in some cases.
 * 
 * @author ProgrammerDan
 *
 */
public class CreationError extends RuntimeException {

	private static final long serialVersionUID = 725722602896129633L;

	public CreationError(Class<?> clazz, String string) {
		super(string + " with " + clazz.toString());
	}
	
	public CreationError(Class<?> clazz, Throwable cause) {
		super("Creation failure for " + clazz.toString(), cause);
	}
}
