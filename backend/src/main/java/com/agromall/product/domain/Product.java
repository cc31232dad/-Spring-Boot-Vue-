package com.agromall.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        product.status = ProductStatus.ON_SALE.name();
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
    }

    public void onSale() {
        this.status = ProductStatus.ON_SALE.name();
    }

    public void offSale() {
        this.status = ProductStatus.OFF_SALE.name();
    }
}
