package com.mstcc.userms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised HTTP exception handling for user-ms.
 * <p>
 * Applying SRP: controllers are responsible only for routing and delegation —
 * all error-response formatting is handled here.
 * <p>
 * Applying OCP: adding a new exception type requires only a new {@code @ExceptionHandler}
 * method here; no controller needs to change.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation failures raised by {@code @Valid} on request bodies.
     * Returns 400 with a field-level error summary.
     *
     * @param ex validation exception containing field errors
     * @return 400 Bad Request with detailed message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        logger.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Validation Error", message));
    }

    /**
     * Handles explicit business rule violations (e.g. duplicate username, entity not found).
     *
     * @param ex the violated business rule exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    /**
     * Handles database unique-constraint violations caused by concurrent inserts
     * that race past the application-level duplicate check in {@code saveUser}.
     * Returns 409 Conflict so the caller can distinguish this from a generic 500.
     *
     * @param ex constraint violation raised by the JPA provider
     * @return 409 Conflict with a stable error message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.warn("Data integrity violation in user-ms: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", "Username already exists"));
    }

    /**
     * Catch-all for unexpected runtime errors. Returns 500 without leaking
     * internal details to the caller.
     *
     * @param ex unexpected exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        logger.error("Unexpected runtime error in user-ms", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
