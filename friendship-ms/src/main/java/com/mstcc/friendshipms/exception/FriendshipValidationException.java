package com.mstcc.friendshipms.exception;

/**
 * Exception thrown when async validation of users in friendship-ms fails —
 * e.g. user-ms is unavailable, returns an error, or the validation times out.
 *
 * <p>Extends {@link RuntimeException} so it propagates through Spring's transaction
 * boundary and is caught by {@link GlobalExceptionHandler} without requiring checked
 * exception declarations on service methods.
 */
public class FriendshipValidationException extends RuntimeException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message human-readable description of the failure
     */
    public FriendshipValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given detail message and root cause.
     *
     * @param message human-readable description of the failure
     * @param cause   the underlying exception (e.g. {@link java.util.concurrent.TimeoutException})
     */
    public FriendshipValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
