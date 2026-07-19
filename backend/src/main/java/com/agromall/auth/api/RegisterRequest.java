package com.agromall.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$")
        String username,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$")
        String password,

        @NotBlank
        @Pattern(regexp = "^\\d{11}$")
        String phone
) {
}
