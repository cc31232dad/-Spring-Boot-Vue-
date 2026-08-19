package com.agromall.farmer.api;
import jakarta.validation.constraints.NotBlank;
public record FarmerRejectRequest(@NotBlank String reason) { }
