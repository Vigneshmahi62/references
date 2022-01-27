package com.precision.mdm.data.exception;

import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * {@code GlobalExceptionHandler} to catch exceptions and as to send error
 * response against each requests
 * 
 * @author Vignesh
 *
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * This field helps to include stack trace in the response as per client
	 * request.
	 */
	public static final String TRACE = "trace";

//	@Value("${reflectoring.trace}")
	private boolean printStackTrace;

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			final MethodArgumentNotValidException ex, final HttpHeaders headers,
			final HttpStatus status, final WebRequest request) {
		final ErrorResponse errorResponse = new ErrorResponse(
				HttpStatus.UNPROCESSABLE_ENTITY.value(),
				"Validation error. Check 'errors' field for details.");
		for (final FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			errorResponse.addValidationError(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return ResponseEntity.unprocessableEntity().body(errorResponse);
	}

	/**
	 * @see NoContentException. Method to catch NoContentException
	 * 
	 * @param noContentException - NoContentReturnException
	 * @param request            - WebRequest
	 * @return ResponseEntity
	 */
	@ExceptionHandler(NoContentException.class)
	public ResponseEntity<Object> handleNoContentReturnException(
			final NoContentException noContentException, final WebRequest request) {
		return buildErrorResponse(noContentException, noContentException.getMessage(),
				HttpStatus.NO_CONTENT, request);
	}

	/**
	 * @see DuplicateException. Method to catch DuplicateException
	 * 
	 * @param duplicateException - DuplicateException
	 * @param request            - WebRequest
	 * @return ResponseEntity
	 */
	@ExceptionHandler(DuplicateException.class)
	public ResponseEntity<Object> handleDuplicateException(
			final DuplicateException duplicateException, final WebRequest request) {
		return buildErrorResponse(duplicateException, duplicateException.getMessage(),
				HttpStatus.CONFLICT, request);
	}

	/**
	 * @see NoSuchElementFoundException. Method to catch NoSuchElementFoundException
	 * 
	 * @param itemNotFoundException - NoSuchElementFoundException
	 * @param request               - WebRequest
	 * @return ResponseEntity
	 */
	@ExceptionHandler(NoSuchElementFoundException.class)
	public ResponseEntity<Object> handleNoSuchElementFoundException(
			final NoSuchElementFoundException itemNotFoundException, final WebRequest request) {
		return buildErrorResponse(itemNotFoundException, itemNotFoundException.getMessage(),
				HttpStatus.NOT_FOUND, request);
	}

	/**
	 * @see InvalidDataException. Method to catch InvaliDataException
	 * 
	 * @param invalidDataFoundException
	 * @param request                   - WebRequest
	 * @return ResponseEntity
	 */
	@ExceptionHandler(InvalidDataException.class)
	public ResponseEntity<Object> handleInvalidDataException(
			final InvalidDataException invalidDataFoundException, final WebRequest request) {
		return buildErrorResponse(invalidDataFoundException, invalidDataFoundException.getMessage(),
				HttpStatus.BAD_REQUEST, request);
	}

	/**
	 * Method to catch Exception. (Internal Server Error)
	 * 
	 * @param exception - Exception
	 * @param request   - WebRequest
	 * @return ResponseEntity
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleAllUncaughtException(final Exception exception,
			final WebRequest request) {
		return buildErrorResponse(exception, exception.getMessage(),
				HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	/**
	 * This will construct & return ResponseEntity based on the requested parameters
	 * 
	 * @param exception  - Exception
	 * @param message    - Error Message
	 * @param httpStatus - HttpStatus
	 * @param request    - WebRequest
	 * @return ResponseEntity
	 */
	private ResponseEntity<Object> buildErrorResponse(final Exception exception,
			final String message, final HttpStatus httpStatus, final WebRequest request) {
		final ErrorResponse errorResponse = new ErrorResponse(httpStatus.value(), message);
		if (printStackTrace && isTraceOn(request)) {
			errorResponse.setStackTrace(exception.getMessage());
		}
		return ResponseEntity.status(httpStatus).body(errorResponse);
	}

	/**
	 * Checks if the received request enabled trace
	 * 
	 * @param request - WebRequest
	 * @return True if trace enabled in request else false
	 */
	private boolean isTraceOn(final WebRequest request) {
		final String[] value = request.getParameterValues(TRACE);
		return Objects.nonNull(value) && value.length > 0 && value[0].contentEquals("true");
	}

	@Override
	public ResponseEntity<Object> handleExceptionInternal(final Exception ex, final Object body,
			final HttpHeaders headers, final HttpStatus status, final WebRequest request) {
		return buildErrorResponse(ex, ex.getMessage(), status, request);
	}
}
