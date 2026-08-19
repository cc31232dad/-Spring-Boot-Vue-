package com.agromall.product;

import com.agromall.product.domain.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductReviewDomainTest {

    @Test
    void approveRecordsReviewerAndTimeAndClearsReason() {
        Product product = product();
        LocalDateTime rejectedAt = LocalDateTime.of(2026, 8, 19, 10, 0);
        product.reject(8L, rejectedAt, "  信息不完整  ");
        LocalDateTime approvedAt = rejectedAt.plusHours(1);

        product.approve(9L, approvedAt);

        assertThat(product.getStatus()).isEqualTo("ON_SALE");
        assertThat(product.getReviewedBy()).isEqualTo(9L);
        assertThat(product.getReviewedAt()).isEqualTo(approvedAt);
        assertThat(product.getReviewReason()).isNull();
    }

    @Test
    void rejectRequiresReasonAndStoresTrimmedReason() {
        Product product = product();
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 8, 19, 11, 0);

        product.reject(9L, reviewedAt, "  图片不清晰  ");

        assertThat(product.getStatus()).isEqualTo("REJECTED");
        assertThat(product.getReviewedBy()).isEqualTo(9L);
        assertThat(product.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(product.getReviewReason()).isEqualTo("图片不清晰");
        assertThatThrownBy(() -> product.reject(9L, reviewedAt, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Product product() {
        return Product.create(1L, 2L, "苹果", "新鲜苹果", new BigDecimal("12.50"),
                20, "陕西", "https://example.com/apple.jpg");
    }
}
