package com.agromall.product.application;

import org.springframework.web.multipart.MultipartFile;

public interface ProductImageStorage {
    String store(MultipartFile file);
}
