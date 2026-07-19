package com.agromall.auth.api;

public record TokenView(String tokenType, String accessToken) {

    public static TokenView bearer(String accessToken) {
        return new TokenView("Bearer", accessToken);
    }
}
