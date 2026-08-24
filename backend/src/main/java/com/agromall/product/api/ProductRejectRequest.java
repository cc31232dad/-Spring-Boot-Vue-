package com.agromall.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRejectRequest(@NotBlank @Size(max = 500) String reason) {
}
