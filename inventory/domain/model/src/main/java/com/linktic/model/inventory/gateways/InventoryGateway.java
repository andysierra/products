package com.linktic.model.inventory.gateways;

import com.linktic.model.inventory.Inventory;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface InventoryGateway {
    Mono<Inventory> findByProductId(UUID productId);
    Mono<Inventory> save(Inventory inventory);
    Mono<Inventory> updateStock(UUID productId, int quantityDelta, long expectedVersion);
}
