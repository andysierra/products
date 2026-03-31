package com.linktic.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("purchases")
public class PurchaseEntity {
    @Id
    private UUID id;
    private UUID productId;
    private Integer quantity;
    private String status;
    private String idempotencyKey;
    private Instant createdAt;
}
