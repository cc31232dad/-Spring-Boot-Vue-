package com.agromall.product.api;

import com.agromall.common.api.ApiResponse;
import com.agromall.product.application.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ProductService productService;

    public CategoryController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<CategoryView>> list() {
        return ApiResponse.ok(productService.listCategories());
    }
}
