package com.agromall.usercenter.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "^1\\d{10}$") String phone,
        @NotBlank @Size(max = 50) String province,
        @NotBlank @Size(max = 50) String city,
        @NotBlank @Size(max = 50) String district,
        @NotBlank @Size(max = 255) String detail,
        Boolean isDefault
) {}
