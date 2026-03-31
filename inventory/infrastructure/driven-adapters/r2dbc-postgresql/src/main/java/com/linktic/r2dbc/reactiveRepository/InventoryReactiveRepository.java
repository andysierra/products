package com.linktic.r2dbc.reactiveRepository;

import com.linktic.r2dbc.entity.InventoryEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface InventoryReactiveRepository extends ReactiveCrudRepository<InventoryEntity, UUID>,
        ReactiveQueryByExampleExecutor<InventoryEntity> {

    Mono<InventoryEntity> findByProductId(UUID productId);
}
