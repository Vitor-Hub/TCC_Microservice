package com.mstcc.monolith.exception;

/**
 * Thrown when cross-domain validation fails in the monolith —
 * for example, when a comment references a non-existent post, or a like
 * references a non-existent user.
 *
 * <p>In the microservices implementation this would surface as a Feign
 * client exception propagating from a 4xx response of an upstream service.
 * In the monolith the equivalent check is a direct service call, and this
 * exception carries the same semantic meaning.
 */
public class ValidationException extends RuntimeException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message human-readable description of the failure
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given detail message and root cause.
     *
     * @param message human-readable description of the failure
     * @param cause   underlying exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
