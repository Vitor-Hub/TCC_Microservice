package com.mstcc.likesms.exceptions;

public class LikeValidationException extends RuntimeException {

    public LikeValidationException(String message) {
        super(message);
    }

    public LikeValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
