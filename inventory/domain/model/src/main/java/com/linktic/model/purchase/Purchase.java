package com.linktic.model.purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {
    private UUID id;
    private UUID productId;
    private Integer quantity;
    private PurchaseStatus status;
    private String idempotencyKey;
    private Instant createdAt;
}
