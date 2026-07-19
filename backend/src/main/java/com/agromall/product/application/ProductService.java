package com.agromall.product.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.product.api.CategoryView;
import com.agromall.product.api.ProductDetailView;
import com.agromall.product.api.ProductSummaryView;
import com.agromall.product.domain.Product;
import com.agromall.product.domain.ProductCategory;
import com.agromall.product.domain.ProductStatus;
import com.agromall.product.infrastructure.ProductCategoryMapper;
import com.agromall.product.infrastructure.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;

    public ProductService(ProductMapper productMapper, ProductCategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryView> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                        .orderByAsc(ProductCategory::getSortOrder))
                .stream()
                .map(category -> new CategoryView(category.getId(), category.getName()))
                .toList();
    }

    public List<ProductSummaryView> listPublicProducts(String keyword, Long categoryId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatus.ON_SALE.name());
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword.trim());
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreatedAt);

        Map<Long, String> categoryNames = categoryNames();
        return productMapper.selectList(wrapper).stream()
                .map(product -> new ProductSummaryView(product.getId(), product.getCategoryId(),
                        categoryNames.get(product.getCategoryId()), product.getName(), product.getPrice(),
                        product.getStock(), product.getOriginPlace(), product.getImageUrl()))
                .toList();
    }

    public ProductDetailView getPublicProduct(Long id) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                        .eq(Product::getId, id)
                        .eq(Product::getStatus, ProductStatus.ON_SALE.name()));
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        String categoryName = categoryNames().get(product.getCategoryId());
        return new ProductDetailView(product.getId(), product.getCategoryId(), categoryName, product.getFarmerId(),
                product.getName(), product.getDescription(), product.getPrice(), product.getStock(),
                product.getOriginPlace(), product.getImageUrl(), product.getStatus());
    }

    private Map<Long, String> categoryNames() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
    }
}
