package com.linktic.api.helpers;

import com.linktic.api.rest.request.CreateProductRequest;
import com.linktic.api.rest.request.UpdateProductRequest;
import com.linktic.api.rest.response.ProductResponse;
import com.linktic.model.product.Product;
import com.linktic.model.product.ProductStatus;

public class Mapper {

    private Mapper() {}

    public static Product toProduct(CreateProductRequest request) {
        return Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .price(request.getPrice())
                .build();
    }

    public static Product toProduct(UpdateProductRequest request) {
        return Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .status(request.getStatus() != null
                        ? ProductStatus.valueOf(request.getStatus().toUpperCase())
                        : null)
                .build();
    }

    public static ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId().toString())
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
