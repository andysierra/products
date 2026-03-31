package com.linktic.r2dbc.reactiveRepository;

import com.linktic.r2dbc.entity.PurchaseEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PurchaseReactiveRepository extends ReactiveCrudRepository<PurchaseEntity, UUID>,
        ReactiveQueryByExampleExecutor<PurchaseEntity> {

    Mono<PurchaseEntity> findByIdempotencyKey(String idempotencyKey);
    Flux<PurchaseEntity> findByProductId(UUID productId);
}
