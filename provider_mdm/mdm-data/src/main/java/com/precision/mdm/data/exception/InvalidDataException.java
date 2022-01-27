package com.precision.mdm.data.exception;

/**
 * Throw an exception using this class {@code InvalidDataException} if
 * invalid data found 
 * 
 *
 */
public class InvalidDataException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8725419880516038069L;

	public InvalidDataException(final String message) {
		super(message);
	}

}