package com.precision.mdm.data.exception;

/**
 * Throw an exception using this class{@code NoSuchElementFoundException} if no
 * content/records found against the requested id
 * 
 * @author Vignesh
 *
 */
public class NoSuchElementFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5451140846586706943L;

	public NoSuchElementFoundException(final String message) {
		super(message);
	}

}
