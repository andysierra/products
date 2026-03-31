package com.linktic.api.helpers;

import com.linktic.api.rest.request.CreatePurchaseRequest;
import com.linktic.api.rest.response.InventoryResponse;
import com.linktic.api.rest.response.PurchaseResponse;
import com.linktic.model.inventory.Inventory;
import com.linktic.model.purchase.Purchase;

public class Mapper {

    private Mapper() {
    }

    public static Purchase toPurchase(CreatePurchaseRequest request) {
        return Purchase.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .idempotencyKey(request.getIdempotencyKey())
                .build();
    }

    public static InventoryResponse toInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .available(inventory.getAvailable())
                .reserved(inventory.getReserved())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public static PurchaseResponse toPurchaseResponse(Purchase purchase) {
        return PurchaseResponse.builder()
                .id(purchase.getId())
                .productId(purchase.getProductId())
                .quantity(purchase.getQuantity())
                .status(purchase.getStatus() != null ? purchase.getStatus().name() : null)
                .idempotencyKey(purchase.getIdempotencyKey())
                .createdAt(purchase.getCreatedAt())
                .build();
    }
}
