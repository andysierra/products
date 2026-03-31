package com.linktic.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("inventory")
public class InventoryEntity {
    @Id
    private UUID id;
    private UUID productId;
    private Integer available;
    private Integer reserved;
    @Version
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
