package com.linktic.usecase.product;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.product.Product;
import com.linktic.model.product.ProductStatus;
import com.linktic.model.product.gateways.ProductGateway;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ProductUseCase {

    private final ProductGateway productGateway;

    public Mono<Product> findById(UUID id) {
        return productGateway.findById(id)
                .switchIfEmpty(Mono.error(BusinessException.fromMessage(MessagesEnum.PRODUCT_NOT_FOUND)));
    }

    public Flux<Product> findAll(ProductStatus status, String search, String sortBy, String sortDir, int page, int size) {
        return productGateway.findAll(status, search, sortBy, sortDir, page, size);
    }

    public Mono<Long> count(ProductStatus status, String search) {
        return productGateway.count(status, search);
    }

    public Mono<Product> create(Product product) {
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(Instant.now());
        return productGateway.save(product);
    }

    public Mono<Product> update(UUID id, Product product) {
        return findById(id)
                .flatMap(existing -> {
                    existing.setName(product.getName() != null ? product.getName() : existing.getName());
                    existing.setPrice(product.getPrice() != null ? product.getPrice() : existing.getPrice());
                    existing.setStatus(product.getStatus() != null ? product.getStatus() : existing.getStatus());
                    return productGateway.update(existing);
                });
    }

    public Mono<Void> delete(UUID id) {
        return findById(id)
                .flatMap(existing -> productGateway.deleteById(existing.getId()));
    }
}
