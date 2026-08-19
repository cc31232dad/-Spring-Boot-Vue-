package com.agromall.farmer.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record FarmerApplicationRequest(
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
        @NotBlank @Size(min = 8, max = 64) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") String password,
        @NotBlank String realName, @NotBlank String idCard, @NotBlank String province,
        @NotBlank String city, @NotBlank String district, @NotBlank String detailAddress,
        @NotBlank String category, String licenseNo) { }
