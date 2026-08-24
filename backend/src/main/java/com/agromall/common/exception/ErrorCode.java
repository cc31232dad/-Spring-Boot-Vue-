package com.agromall.common.exception;

public enum ErrorCode {
    USER_ALREADY_EXISTS(1001, "User already exists"),
    INVALID_CREDENTIALS(1002, "Invalid credentials"),
    UNAUTHORIZED(1003, "Unauthorized"),
    FORBIDDEN(1004, "Forbidden"),
    VALIDATION_ERROR(1005, "Validation error"),
    PRODUCT_NOT_FOUND(1006, "Product not found"),
    CATEGORY_NOT_FOUND(1007, "Category not found"),
    CART_ITEM_NOT_FOUND(1008, "Cart item not found"),
    PRODUCT_UNAVAILABLE(1009, "Product unavailable"),
    INSUFFICIENT_STOCK(1010, "Insufficient stock"),
    ORDER_NOT_FOUND(1011, "Order not found"),
    INVALID_ORDER_STATUS(1012, "Invalid order status"),
    SECKILL_ACTIVITY_NOT_FOUND(1013, "Seckill activity not found"),
    SECKILL_NOT_STARTED(1014, "Seckill activity has not started"),
    SECKILL_ENDED(1015, "Seckill activity has ended"),
    SECKILL_SOLD_OUT(1016, "Seckill activity is sold out"),
    SECKILL_ALREADY_BOUGHT(1017, "User already bought this seckill item"),
    SECKILL_NOT_PUBLISHABLE(1018, "Seckill activity cannot be published"),
    SECKILL_ORDER_FAILED(1019, "Unable to create seckill order"),
    ADDRESS_NOT_FOUND(1020, "Address not found"),
    FAVORITE_NOT_FOUND(1021, "Favorite not found"),
    USER_NOT_FOUND(1022, "User not found"),
    PRODUCT_REVIEW_INVALID(1023, "Product review state is invalid"),
    CURRENT_PASSWORD_INVALID(1024, "Current password is incorrect"),
    FARMER_APPLICATION_PENDING(1025, "Farmer application is pending review"),
    FARMER_APPLICATION_REJECTED(1026, "Farmer application was rejected"),
    FARMER_APPLICATION_INVALID(1027, "Farmer application state is invalid"),
    FARMER_PROFILE_NOT_FOUND(1028, "Farmer profile not found"),
    PAYMENT_NOT_FOUND(1029, "Payment record not found"),
    PAYMENT_INVALID_STATUS(1030, "Payment status is invalid"),
    PAYMENT_SIGNATURE_INVALID(1031, "Payment signature is invalid"),
    PAYMENT_AMOUNT_MISMATCH(1032, "Payment amount does not match order"),
    PAYMENT_EXPIRED(1033, "Payment has expired"),
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
