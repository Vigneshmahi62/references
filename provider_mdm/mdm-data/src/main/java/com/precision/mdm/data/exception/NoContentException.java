package com.precision.mdm.data.exception;

/**
 * Throw an exception using this class {@code NoContentException} if no
 * content/records found in the requested tables
 * 
 * @author Vignesh
 *
 */
public class NoContentException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5780568227047369657L;

	public NoContentException(final String message) {
		super(message);
	}

}
