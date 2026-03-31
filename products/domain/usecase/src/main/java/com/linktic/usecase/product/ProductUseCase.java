package com.linktic.usecase.product;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.product.Product;
import com.linktic.model.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ProductUseCase {

    // TODO: replace with ProductGateway once adapter is ready

    private static final List<Product> MOCK_PRODUCTS = List.of(
            Product.builder()
                    .id(UUID.fromString("a1b2c3d4-0001-0001-0001-000000000001"))
                    .sku("SKU-001")
                    .name("Laptop Gamer Pro")
                    .price(BigDecimal.valueOf(2999.99))
                    .status(ProductStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build(),
            Product.builder()
                    .id(UUID.fromString("a1b2c3d4-0001-0001-0001-000000000002"))
                    .sku("SKU-002")
                    .name("Monitor 4K UltraWide")
                    .price(BigDecimal.valueOf(899.50))
                    .status(ProductStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build(),
            Product.builder()
                    .id(UUID.fromString("a1b2c3d4-0001-0001-0001-000000000003"))
                    .sku("SKU-003")
                    .name("Teclado Mecánico RGB")
                    .price(BigDecimal.valueOf(150.00))
                    .status(ProductStatus.INACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build()
    );

    public Mono<Product> findById(UUID id) {
        return Flux.fromIterable(MOCK_PRODUCTS)
                .filter(p -> p.getId().equals(id))
                .next()
                .switchIfEmpty(Mono.error(BusinessException.fromMessage(MessagesEnum.PRODUCT_NOT_FOUND)));
    }

    public Flux<Product> findAll(ProductStatus status, String search, String sortBy, String sortDir, int page, int size) {
        return Flux.fromIterable(MOCK_PRODUCTS)
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> search == null || search.isBlank()
                        || p.getSku().toLowerCase().contains(search.toLowerCase())
                        || p.getName().toLowerCase().contains(search.toLowerCase()))
                .skip((long) page * size)
                .take(size);
    }

    public Mono<Long> count(ProductStatus status, String search) {
        return findAll(status, search, null, null, 0, Integer.MAX_VALUE).count();
    }

    public Mono<Product> create(Product product) {
        boolean skuExists = MOCK_PRODUCTS.stream()
                .anyMatch(p -> p.getSku().equalsIgnoreCase(product.getSku()));
        if (skuExists) {
            return Mono.error(BusinessException.fromMessage(MessagesEnum.DUPLICATE_SKU));
        }
        Product created = product.toBuilder()
                .id(UUID.randomUUID())
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return Mono.just(created);
    }

    public Mono<Product> update(UUID id, Product product) {
        return findById(id)
                .map(existing -> existing.toBuilder()
                        .name(product.getName() != null ? product.getName() : existing.getName())
                        .price(product.getPrice() != null ? product.getPrice() : existing.getPrice())
                        .status(product.getStatus() != null ? product.getStatus() : existing.getStatus())
                        .updatedAt(Instant.now())
                        .build());
    }

    public Mono<Void> delete(UUID id) {
        return findById(id).then();
    }
}
