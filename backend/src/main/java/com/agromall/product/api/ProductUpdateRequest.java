package com.agromall.product.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotBlank @Size(max = 100) String originPlace,
        @NotBlank @Size(max = 500) @Pattern(regexp = "^(?:https?://[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?(?::\\d{1,5})?(?:[/?#][^\\s]*)?|/[^\\s]+)$") String imageUrl
) {
}
