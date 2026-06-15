package com.mstcc.commentms.exception;

import java.time.LocalDateTime;

/**
 * Standard error response body returned by {@link GlobalExceptionHandler}.
 *
 * @param status    HTTP status code
 * @param error     short error type label
 * @param message   human-readable description of the failure
 * @param timestamp time the error occurred (server time)
 */
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {

    /**
     * Convenience factory that sets timestamp to now.
     */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}
