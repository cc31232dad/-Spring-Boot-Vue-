package com.agromall.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Long farmerId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String originPlace;
    private String imageUrl;
    private String status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reviewedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime reviewedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String reviewReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Product create(Long categoryId, Long farmerId, String name, String description,
                                 BigDecimal price, Integer stock, String originPlace, String imageUrl) {
        Product product = new Product();
        product.categoryId = categoryId;
        product.farmerId = farmerId;
        product.name = name;
        product.description = description;
        product.price = price;
        product.stock = stock;
        product.originPlace = originPlace;
        product.imageUrl = imageUrl;
        product.submitForReview();
        return product;
    }

    public void update(Long categoryId, String name, String description, BigDecimal price,
                       Integer stock, String originPlace, String imageUrl) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.originPlace = originPlace;
        this.imageUrl = imageUrl;
        submitForReview();
    }

    public void submitForReview() {
        this.status = ProductStatus.PENDING_REVIEW.name();
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.reviewReason = null;
    }

    public void approve(Long reviewerId, LocalDateTime reviewTime) {
        this.status = ProductStatus.ON_SALE.name();
        this.reviewedBy = Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        this.reviewedAt = Objects.requireNonNull(reviewTime, "reviewTime must not be null");
        this.reviewReason = null;
    }

    public void reject(Long reviewerId, LocalDateTime reviewTime, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("review reason must not be blank");
        }
        this.status = ProductStatus.REJECTED.name();
        this.reviewedBy = Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        this.reviewedAt = Objects.requireNonNull(reviewTime, "reviewTime must not be null");
        this.reviewReason = reason.trim();
    }

    public void offSale() {
        this.status = ProductStatus.OFF_SALE.name();
    }
}
