package com.linktic.api.rest.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {
    private UUID id;
    private UUID productId;
    private Integer quantity;
    private String status;
    private String idempotencyKey;
    private Instant createdAt;
}
