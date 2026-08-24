package com.agromall.product.api;

import com.agromall.common.api.ApiResponse;
import com.agromall.product.application.ProductImageStorage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/farmer/product-images")
public class ProductImageController {
    private final ProductImageStorage storage;

    public ProductImageController(ProductImageStorage storage) {
        this.storage = storage;
    }

    @PostMapping
    public ApiResponse<ProductImageView> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(new ProductImageView(storage.store(file)));
    }
}
