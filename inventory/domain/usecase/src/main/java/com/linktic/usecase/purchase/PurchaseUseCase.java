package com.linktic.usecase.purchase;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.inventory.gateways.InventoryGateway;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.product.gateways.ProductServiceGateway;
import com.linktic.model.purchase.Purchase;
import com.linktic.model.purchase.PurchaseStatus;
import com.linktic.model.purchase.gateways.PurchaseGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
public class PurchaseUseCase {

    private final PurchaseGateway purchaseGateway;
    private final InventoryGateway inventoryGateway;
    private final ProductServiceGateway productServiceGateway;

    public Mono<Purchase> create(Purchase purchase) {
        return purchaseGateway.findByIdempotencyKey(purchase.getIdempotencyKey())
                .flatMap(existing -> Mono.<Purchase>error(
                        BusinessException.fromMessage(MessagesEnum.DUPLICATE_IDEMPOTENCY)))
                .switchIfEmpty(Mono.defer(() -> doCreate(purchase)));
    }

    private Mono<Purchase> doCreate(Purchase purchase) {
        return productServiceGateway.getProduct(purchase.getProductId())
                .then(inventoryGateway.findByProductId(purchase.getProductId())
                        .switchIfEmpty(Mono.error(
                                BusinessException.fromMessage(MessagesEnum.INVENTORY_NOT_FOUND))))
                .flatMap(inventory -> {
                    if (inventory.getAvailable() < purchase.getQuantity()) {
                        return Mono.error(BusinessException.fromMessage(MessagesEnum.INSUFFICIENT_STOCK));
                    }
                    return inventoryGateway.updateStock(
                            purchase.getProductId(),
                            -purchase.getQuantity(),
                            inventory.getVersion()
                    );
                })
                .flatMap(updatedInventory -> {
                    purchase.setStatus(PurchaseStatus.COMPLETED);
                    purchase.setCreatedAt(Instant.now());
                    return purchaseGateway.save(purchase);
                });
    }

    public Mono<Purchase> findById(UUID id) {
        return purchaseGateway.findById(id)
                .switchIfEmpty(Mono.error(BusinessException.fromMessage(MessagesEnum.PURCHASE_NOT_FOUND)));
    }

    public Flux<Purchase> findAll(UUID productId, String sortBy, String sortDir, int page, int size) {
        if (productId != null) {
            return purchaseGateway.findByProductId(productId);
        }
        return purchaseGateway.findAll(sortBy, sortDir, page, size);
    }

    public Mono<Long> count() {
        return purchaseGateway.count();
    }
}
