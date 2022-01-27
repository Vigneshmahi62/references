package com.precision.mdm.data.exception;

/**
 * Throw an exception using this class {@code DuplicateException} if found
 * duplicate
 * 
 * @author Vignesh
 *
 */
public class DuplicateException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5035624242156847768L;

	public DuplicateException(final String message) {
		super(message);
	}
}
