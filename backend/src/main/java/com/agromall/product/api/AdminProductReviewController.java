package com.agromall.product.api;

import com.agromall.auth.security.JwtService;
import com.agromall.common.api.ApiResponse;
import com.agromall.product.application.ProductService;
import com.agromall.product.domain.ProductStatus;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductReviewController {

    private final ProductService productService;

    public AdminProductReviewController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/review")
    public ApiResponse<List<ProductReviewView>> list(
            @RequestParam(defaultValue = "PENDING_REVIEW") ProductStatus status) {
        return ApiResponse.ok(productService.listReviewProducts(status));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ProductReviewView> approve(
            @AuthenticationPrincipal JwtService.JwtPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.ok(productService.approveProduct(principal.userId(), id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ProductReviewView> reject(
            @AuthenticationPrincipal JwtService.JwtPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ProductRejectRequest request) {
        return ApiResponse.ok(productService.rejectProduct(principal.userId(), id, request.reason()));
    }
}
