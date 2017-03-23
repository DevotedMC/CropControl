package com.programmerdan.minecraft.cropcontrol;

public class CreationError extends RuntimeException {

	private static final long serialVersionUID = 725722602896129633L;

	public CreationError(Class<?> clazz, String string) {
		super(string + " with " + clazz.toString());
	}
	
	public CreationError(Class<?> clazz, Throwable cause) {
		super("Creation failure for " + clazz.toString(), cause);
	}
}
