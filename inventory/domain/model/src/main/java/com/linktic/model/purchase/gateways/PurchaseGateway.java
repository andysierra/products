package com.linktic.model.purchase.gateways;

import com.linktic.model.purchase.Purchase;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PurchaseGateway {
    Mono<Purchase> save(Purchase purchase);
    Mono<Purchase> findById(UUID id);
    Mono<Purchase> findByIdempotencyKey(String idempotencyKey);
    Flux<Purchase> findByProductId(UUID productId);
    Flux<Purchase> findAll(String sortBy, String sortDir, int page, int size);
    Mono<Long> count();
}
