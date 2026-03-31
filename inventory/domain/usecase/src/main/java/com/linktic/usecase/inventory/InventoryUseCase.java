package com.linktic.usecase.inventory;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.inventory.Inventory;
import com.linktic.model.inventory.gateways.InventoryGateway;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.product.gateways.ProductServiceGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
public class InventoryUseCase {

    private final InventoryGateway inventoryGateway;
    private final ProductServiceGateway productServiceGateway;

    public Mono<Inventory> findByProductId(UUID productId) {
        return inventoryGateway.findByProductId(productId)
                .switchIfEmpty(Mono.error(BusinessException.fromMessage(MessagesEnum.INVENTORY_NOT_FOUND)));
    }

    public Mono<Inventory> initializeStock(UUID productId, int available) {
        return productServiceGateway.getProduct(productId)
                .then(inventoryGateway.findByProductId(productId))
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        return inventoryGateway.findByProductId(productId)
                                .flatMap(inv -> {
                                    inv.setAvailable(available);
                                    inv.setUpdatedAt(Instant.now());
                                    return inventoryGateway.save(inv);
                                });
                    }
                    Inventory inventory = Inventory.builder()
                            .productId(productId)
                            .available(available)
                            .reserved(0)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return inventoryGateway.save(inventory);
                });
    }
}
