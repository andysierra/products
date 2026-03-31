package com.linktic.model.inventory;

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
public class Inventory {
    private UUID id;
    private UUID productId;
    private Integer available;
    private Integer reserved;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
