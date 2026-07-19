package com.agromall.common.exception;

public enum ErrorCode {
    USER_ALREADY_EXISTS(1001, "User already exists"),
    INVALID_CREDENTIALS(1002, "Invalid credentials"),
    UNAUTHORIZED(1003, "Unauthorized"),
    FORBIDDEN(1004, "Forbidden"),
    VALIDATION_ERROR(1005, "Validation error"),
    INTERNAL_ERROR(9999, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
