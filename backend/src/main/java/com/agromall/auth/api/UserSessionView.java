package com.agromall.auth.api;

import java.util.Set;

public record UserSessionView(Long userId, String username, Set<String> roles) {
}
