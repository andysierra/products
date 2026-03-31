package com.linktic.r2dbc.reactiveRepository;

import com.linktic.r2dbc.entity.ProductEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductReactiveRepository extends ReactiveCrudRepository<ProductEntity, UUID>, ReactiveQueryByExampleExecutor<ProductEntity> {
    Mono<ProductEntity> findBySku(String sku);
}
