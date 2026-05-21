package com.mstcc.friendshipms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised HTTP exception handling for friendship-ms.
 *
 * <p>Applying SRP: controllers are responsible only for routing and delegation —
 * all error-response formatting is handled here.
 *
 * <p>Applying OCP: adding a new exception type requires only a new
 * {@code @ExceptionHandler} method here; no controller needs to change.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation failures raised by {@code @Valid} on request bodies.
     *
     * @param ex validation exception containing field errors
     * @return 400 Bad Request with field-level error detail
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
     * Handles explicit business rule violations (e.g. self-friendship, entity not found).
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
     * Handles upstream validation failures — e.g. user-ms is unavailable or timed out.
     *
     * @param ex validation exception from async upstream calls
     * @return 503 Service Unavailable
     */
    @ExceptionHandler(FriendshipValidationException.class)
    public ResponseEntity<ErrorResponse> handleFriendshipValidation(FriendshipValidationException ex) {
        logger.error("Friendship validation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(503, "Service Unavailable", "Upstream service error: " + ex.getMessage()));
    }

    /**
     * Catch-all for unexpected runtime errors.
     *
     * @param ex unexpected exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        logger.error("Unexpected runtime error in friendship-ms", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
