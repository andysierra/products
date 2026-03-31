package com.linktic.r2dbc;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.inventory.Inventory;
import com.linktic.model.purchase.Purchase;
import com.linktic.model.purchase.PurchaseStatus;
import com.linktic.r2dbc.entity.InventoryEntity;
import com.linktic.r2dbc.entity.PurchaseEntity;
import com.linktic.r2dbc.reactiveRepository.InventoryReactiveRepository;
import com.linktic.r2dbc.reactiveRepository.PurchaseReactiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryAdapterTest {

    @Mock
    private InventoryReactiveRepository inventoryRepository;
    @Mock
    private PurchaseReactiveRepository purchaseRepository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private DatabaseClient databaseClient;

    private InventoryRepositoryAdapter inventoryAdapter;
    private PurchaseRepositoryAdapter purchaseAdapter;

    private UUID productId;
    private InventoryEntity inventoryEntity;
    private Inventory inventory;
    private PurchaseEntity purchaseEntity;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        inventoryAdapter = new InventoryRepositoryAdapter(inventoryRepository, mapper);
        purchaseAdapter = new PurchaseRepositoryAdapter(purchaseRepository, mapper, databaseClient);

        productId = UUID.randomUUID();

        inventoryEntity = InventoryEntity.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .available(100)
                .reserved(0)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        inventory = Inventory.builder()
                .id(inventoryEntity.getId())
                .productId(productId)
                .available(100)
                .reserved(0)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        purchaseEntity = PurchaseEntity.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .quantity(5)
                .status("COMPLETED")
                .idempotencyKey("key-001")
                .createdAt(Instant.now())
                .build();

        purchase = Purchase.builder()
                .id(purchaseEntity.getId())
                .productId(productId)
                .quantity(5)
                .status(PurchaseStatus.COMPLETED)
                .idempotencyKey("key-001")
                .createdAt(Instant.now())
                .build();
    }

    // ---- InventoryRepositoryAdapter ----

    @Test
    void shouldFindInventoryByProductId() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Mono.just(inventoryEntity));
        when(mapper.map(inventoryEntity, Inventory.class)).thenReturn(inventory);

        StepVerifier.create(inventoryAdapter.findByProductId(productId))
                .assertNext(inv -> {
                    assertEquals(100, inv.getAvailable());
                    assertEquals(productId, inv.getProductId());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenInventoryNotFound() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Mono.empty());

        StepVerifier.create(inventoryAdapter.findByProductId(productId))
                .verifyComplete();
    }

    @Test
    void shouldUpdateStockWithOptimisticLocking() {
        InventoryEntity updatedEntity = inventoryEntity.toBuilder().available(95).reserved(5).build();
        Inventory updatedInventory = inventory.toBuilder().available(95).reserved(5).build();

        when(inventoryRepository.findByProductId(productId)).thenReturn(Mono.just(inventoryEntity));
        when(inventoryRepository.save(any(InventoryEntity.class))).thenReturn(Mono.just(updatedEntity));
        when(mapper.map(updatedEntity, Inventory.class)).thenReturn(updatedInventory);

        StepVerifier.create(inventoryAdapter.updateStock(productId, -5, 0L))
                .assertNext(inv -> {
                    assertEquals(95, inv.getAvailable());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturn409OnOptimisticLockFailure() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Mono.just(inventoryEntity));
        when(inventoryRepository.save(any(InventoryEntity.class)))
                .thenReturn(Mono.error(new OptimisticLockingFailureException("version mismatch")));

        StepVerifier.create(inventoryAdapter.updateStock(productId, -5, 0L))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(409, ((BusinessException) err).getClientCode());
                })
                .verify();
    }

    @Test
    void shouldReturn409WhenInsufficientStockOnUpdate() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Mono.just(inventoryEntity));

        StepVerifier.create(inventoryAdapter.updateStock(productId, -200, 0L))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals("Stock insuficiente para realizar la compra", err.getMessage());
                })
                .verify();
    }

    // ---- PurchaseRepositoryAdapter ----

    @Test
    void shouldSavePurchase() {
        when(mapper.map(purchase, PurchaseEntity.class)).thenReturn(purchaseEntity);
        when(purchaseRepository.save(any(PurchaseEntity.class))).thenReturn(Mono.just(purchaseEntity));
        when(mapper.map(purchaseEntity, Purchase.class)).thenReturn(purchase);

        StepVerifier.create(purchaseAdapter.save(purchase))
                .assertNext(p -> {
                    assertEquals("key-001", p.getIdempotencyKey());
                    assertEquals(5, p.getQuantity());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturn409OnDuplicateIdempotencyKey() {
        when(mapper.map(purchase, PurchaseEntity.class)).thenReturn(purchaseEntity);
        when(purchaseRepository.save(any(PurchaseEntity.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("duplicate key")));

        StepVerifier.create(purchaseAdapter.save(purchase))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(409, ((BusinessException) err).getClientCode());
                })
                .verify();
    }

    @Test
    void shouldFindPurchaseByIdempotencyKey() {
        when(purchaseRepository.findByIdempotencyKey("key-001")).thenReturn(Mono.just(purchaseEntity));
        when(mapper.map(purchaseEntity, Purchase.class)).thenReturn(purchase);

        StepVerifier.create(purchaseAdapter.findByIdempotencyKey("key-001"))
                .assertNext(p -> assertEquals("key-001", p.getIdempotencyKey()))
                .verifyComplete();
    }
}
