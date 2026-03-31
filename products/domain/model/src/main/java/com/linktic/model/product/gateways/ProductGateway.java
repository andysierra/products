package com.linktic.model.product.gateways;

import com.linktic.model.product.Product;
import com.linktic.model.product.ProductStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductGateway {
    Mono<Product> findById(UUID id);
    Mono<Product> findBySku(String sku);
    Flux<Product> findAll(ProductStatus status, String search, String sortBy, String sortDir, int page, int size);
    Mono<Long> count(ProductStatus status, String search);
    Mono<Product> save(Product product);
    Mono<Product> update(Product product);
    Mono<Void> deleteById(UUID id);
}
