package com.agromall.product.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.product.api.CategoryView;
import com.agromall.product.api.ProductCreateRequest;
import com.agromall.product.api.ProductDetailView;
import com.agromall.product.api.ProductSummaryView;
import com.agromall.product.api.ProductUpdateRequest;
import com.agromall.product.api.ProductReviewView;
import com.agromall.product.domain.Product;
import com.agromall.product.domain.ProductCategory;
import com.agromall.product.domain.ProductStatus;
import com.agromall.product.infrastructure.ProductCategoryMapper;
import com.agromall.product.infrastructure.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.time.LocalDateTime;
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
        return toDetailView(product);
    }

    public ProductDetailView createProduct(Long actorId, ProductCreateRequest request) {
        assertCategoryExists(request.categoryId());
        Product product = Product.create(request.categoryId(), actorId, request.name(), request.description(),
                request.price(), request.stock(), request.originPlace(), request.imageUrl());
        productMapper.insert(product);
        return toDetailView(product);
    }

    public ProductDetailView updateProduct(Long actorId, boolean admin, Long productId, ProductUpdateRequest request) {
        Product product = getProduct(productId);
        assertCanManage(actorId, admin, product);
        assertCategoryExists(request.categoryId());
        product.update(request.categoryId(), request.name(), request.description(), request.price(), request.stock(),
                request.originPlace(), request.imageUrl());
        productMapper.updateById(product);
        return toDetailView(product);
    }

    public ProductDetailView changeStatus(Long actorId, boolean admin, Long productId, ProductStatus status) {
        Product product = getProduct(productId);
        assertCanManage(actorId, admin, product);
        switch (status) {
            case PENDING_REVIEW -> product.submitForReview();
            case OFF_SALE -> product.offSale();
            default -> throw new IllegalArgumentException("Unsupported farmer product status: " + status);
        }
        productMapper.updateById(product);
        return toDetailView(product);
    }

    public List<ProductReviewView> listReviewProducts(ProductStatus status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, status.name())
                .orderByDesc(Product::getCreatedAt);
        return productMapper.selectList(wrapper).stream().map(this::toReviewView).toList();
    }

    public ProductReviewView approveProduct(Long reviewerId, Long productId) {
        Product product = getProduct(productId);
        assertPendingReview(product);
        product.approve(reviewerId, LocalDateTime.now());
        productMapper.updateById(product);
        return toReviewView(product);
    }

    public ProductReviewView rejectProduct(Long reviewerId, Long productId, String reason) {
        Product product = getProduct(productId);
        assertPendingReview(product);
        product.reject(reviewerId, LocalDateTime.now(), reason);
        productMapper.updateById(product);
        return toReviewView(product);
    }

    private Product getProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void assertCanManage(Long actorId, boolean admin, Product product) {
        if (!admin && !product.getFarmerId().equals(actorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void assertPendingReview(Product product) {
        if (!ProductStatus.PENDING_REVIEW.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_REVIEW_INVALID);
        }
    }

    private void assertCategoryExists(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    private ProductDetailView toDetailView(Product product) {
        String categoryName = categoryNames().get(product.getCategoryId());
        return new ProductDetailView(product.getId(), product.getCategoryId(), categoryName, product.getFarmerId(),
                product.getName(), product.getDescription(), product.getPrice(), product.getStock(),
                product.getOriginPlace(), product.getImageUrl(), product.getStatus());
    }

    private ProductReviewView toReviewView(Product product) {
        String categoryName = categoryNames().get(product.getCategoryId());
        return new ProductReviewView(product.getId(), product.getCategoryId(), categoryName, product.getFarmerId(),
                product.getName(), product.getDescription(), product.getPrice(), product.getStock(),
                product.getOriginPlace(), product.getImageUrl(), product.getStatus(), product.getReviewedBy(),
                product.getReviewedAt(), product.getReviewReason());
    }

    private Map<Long, String> categoryNames() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
    }
}
