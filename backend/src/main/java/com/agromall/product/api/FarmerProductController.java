package com.agromall.product.api;

import com.agromall.auth.security.JwtService;
import com.agromall.common.api.ApiResponse;
import com.agromall.product.application.ProductService;
import com.agromall.product.domain.ProductStatus;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/farmer/products")
public class FarmerProductController {

    private final ProductService productService;

    public FarmerProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductReviewView>> list(@AuthenticationPrincipal JwtService.JwtPrincipal principal) {
        return ApiResponse.ok(productService.listFarmerProducts(principal.userId()));
    }

    @PostMapping
    public ApiResponse<ProductDetailView> create(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                                 @Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.ok(productService.createProduct(principal.userId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetailView> update(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                                 @PathVariable Long id,
                                                 @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.ok(productService.updateProduct(principal.userId(), principal.roles().contains("ADMIN"), id, request));
    }

    @PatchMapping("/{id}/off-sale")
    public ApiResponse<ProductDetailView> offSale(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                                  @PathVariable Long id) {
        return ApiResponse.ok(productService.changeStatus(principal.userId(), principal.roles().contains("ADMIN"),
                id, ProductStatus.OFF_SALE));
    }

    @PatchMapping("/{id}/on-sale")
    public ApiResponse<ProductDetailView> onSale(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                                 @PathVariable Long id) {
        return ApiResponse.ok(productService.changeStatus(principal.userId(), principal.roles().contains("ADMIN"),
                id, ProductStatus.PENDING_REVIEW));
    }
}
