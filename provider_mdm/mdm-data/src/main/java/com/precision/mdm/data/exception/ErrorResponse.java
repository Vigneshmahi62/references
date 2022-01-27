package com.precision.mdm.data.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * {@code ErrorResponse} is the custom error response that helps client to know
 * more about the errors.
 * 
 * @author Vignesh
 *
 */
@RequiredArgsConstructor
@Getter
@Setter
public class ErrorResponse {
	private final int status;
	private final String message;
	private String stackTrace;
	private List<ValidationError> errors;

	@RequiredArgsConstructor
	@Getter
	private class ValidationError {
		private final String field;
		private final String message;
	}

	public void addValidationError(final String field, final String message) {
		if (Objects.isNull(errors)) {
			errors = new ArrayList<>();
		}
		errors.add(new ValidationError(field, message));
	}
}
