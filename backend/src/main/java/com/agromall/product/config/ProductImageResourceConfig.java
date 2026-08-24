package com.agromall.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class ProductImageResourceConfig implements WebMvcConfigurer {
    private final Path productImageRoot;

    public ProductImageResourceConfig(@Value("${agromall.upload.product-dir:uploads/products}") String directory) {
        this.productImageRoot = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(productImageRoot.toUri().toString());
    }
}
