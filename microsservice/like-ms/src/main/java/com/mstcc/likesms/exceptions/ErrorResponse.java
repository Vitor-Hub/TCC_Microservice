package com.mstcc.likesms.exceptions;

import java.time.LocalDateTime;

/**
 * Standard error response body returned by {@link GlobalExceptionHandler}.
 *
 * @param status    HTTP status code
 * @param error     short error type label (e.g. "Validation Error")
 * @param message   human-readable description of the failure
 * @param timestamp time the error occurred (server time)
 */
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {

    /**
     * Convenience factory that sets timestamp to now.
     *
     * @param status  HTTP status code
     * @param error   short label
     * @param message human-readable message
     * @return populated ErrorResponse
     */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}
