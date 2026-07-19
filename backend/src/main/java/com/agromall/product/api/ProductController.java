package com.agromall.product.api;

import com.agromall.common.api.ApiResponse;
import com.agromall.product.application.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductSummaryView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId
    ) {
        return ApiResponse.ok(productService.listPublicProducts(keyword, categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailView> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.getPublicProduct(id));
    }
}
