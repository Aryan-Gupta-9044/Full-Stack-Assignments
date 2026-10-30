package com.example.spb.web;

import com.example.spb.task.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Turns exceptions into consistent JSON errors: {status, error, message, fieldErrors}. */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(TaskNotFoundException.class)
	public ResponseEntity<ApiError> notFound(TaskNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(fe -> fields.putIfAbsent(fe.getField(), String.valueOf(fe.getDefaultMessage())));
		return build(HttpStatus.BAD_REQUEST, "Validation failed", fields);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
		return build(HttpStatus.BAD_REQUEST,
				"Malformed request body (check JSON syntax, priority = LOW/MEDIUM/HIGH, dueDate = yyyy-MM-dd)", Map.of());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> badParam(MethodArgumentTypeMismatchException ex) {
		return build(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'", Map.of());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> badArgument(IllegalArgumentException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
	}

	private static ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> fields) {
		return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), message, fields));
	}
}
