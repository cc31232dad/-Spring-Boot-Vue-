package com.agromall.usercenter.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneUpdateRequest(@NotBlank String currentPassword,
                                 @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone) {}
