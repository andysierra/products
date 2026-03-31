package com.linktic.usecase;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.inventory.Inventory;
import com.linktic.model.inventory.gateways.InventoryGateway;
import com.linktic.model.product.ProductInfo;
import com.linktic.model.product.gateways.ProductServiceGateway;
import com.linktic.model.purchase.Purchase;
import com.linktic.model.purchase.PurchaseStatus;
import com.linktic.model.purchase.gateways.PurchaseGateway;
import com.linktic.usecase.inventory.InventoryUseCase;
import com.linktic.usecase.purchase.PurchaseUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UseCaseTest {

    @Mock
    private InventoryGateway inventoryGateway;
    @Mock
    private PurchaseGateway purchaseGateway;
    @Mock
    private ProductServiceGateway productServiceGateway;

    private InventoryUseCase inventoryUseCase;
    private PurchaseUseCase purchaseUseCase;

    private UUID productId;
    private Inventory inventory;
    private ProductInfo productInfo;

    @BeforeEach
    void setUp() {
        inventoryUseCase = new InventoryUseCase(inventoryGateway, productServiceGateway);
        purchaseUseCase = new PurchaseUseCase(purchaseGateway, inventoryGateway, productServiceGateway);

        productId = UUID.randomUUID();
        inventory = Inventory.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .available(100)
                .reserved(0)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        productInfo = ProductInfo.builder()
                .id(productId)
                .sku("SKU-001")
                .name("Laptop")
                .status("ACTIVE")
                .build();
    }

    // ---- InventoryUseCase ----

    @Test
    void shouldFindInventoryByProductId() {
        when(inventoryGateway.findByProductId(productId)).thenReturn(Mono.just(inventory));

        StepVerifier.create(inventoryUseCase.findByProductId(productId))
                .assertNext(inv -> {
                    assertEquals(100, inv.getAvailable());
                    assertEquals(productId, inv.getProductId());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturn404WhenInventoryNotFound() {
        when(inventoryGateway.findByProductId(productId)).thenReturn(Mono.empty());

        StepVerifier.create(inventoryUseCase.findByProductId(productId))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(404, ((BusinessException) err).getClientCode());
                })
                .verify();
    }

    @Test
    void shouldInitializeStockForNewProduct() {
        when(productServiceGateway.getProduct(productId)).thenReturn(Mono.just(productInfo));
        when(inventoryGateway.findByProductId(productId)).thenReturn(Mono.empty());
        when(inventoryGateway.save(any(Inventory.class))).thenReturn(Mono.just(inventory));

        StepVerifier.create(inventoryUseCase.initializeStock(productId, 100))
                .assertNext(inv -> assertEquals(100, inv.getAvailable()))
                .verifyComplete();

        verify(inventoryGateway).save(any(Inventory.class));
    }

    // ---- PurchaseUseCase ----

    @Test
    void shouldCreatePurchase() {
        Purchase input = Purchase.builder()
                .productId(productId)
                .quantity(5)
                .idempotencyKey("key-001")
                .build();
        Purchase saved = input.toBuilder()
                .id(UUID.randomUUID())
                .status(PurchaseStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
        Inventory updatedInv = inventory.toBuilder().available(95).build();

        when(purchaseGateway.findByIdempotencyKey("key-001")).thenReturn(Mono.empty());
        when(productServiceGateway.getProduct(productId)).thenReturn(Mono.just(productInfo));
        when(inventoryGateway.findByProductId(productId)).thenReturn(Mono.just(inventory));
        when(inventoryGateway.updateStock(eq(productId), eq(-5), eq(0L))).thenReturn(Mono.just(updatedInv));
        when(purchaseGateway.save(any(Purchase.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(purchaseUseCase.create(input))
                .assertNext(p -> {
                    assertEquals(PurchaseStatus.COMPLETED, p.getStatus());
                    assertNotNull(p.getId());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturn409WhenDuplicateIdempotencyKey() {
        Purchase existing = Purchase.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("key-dup")
                .build();
        Purchase input = Purchase.builder()
                .productId(productId)
                .quantity(1)
                .idempotencyKey("key-dup")
                .build();

        when(purchaseGateway.findByIdempotencyKey("key-dup")).thenReturn(Mono.just(existing));

        StepVerifier.create(purchaseUseCase.create(input))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(409, ((BusinessException) err).getClientCode());
                })
                .verify();
    }

    @Test
    void shouldReturn409WhenInsufficientStock() {
        Inventory lowStock = inventory.toBuilder().available(2).build();
        Purchase input = Purchase.builder()
                .productId(productId)
                .quantity(10)
                .idempotencyKey("key-low")
                .build();

        when(purchaseGateway.findByIdempotencyKey("key-low")).thenReturn(Mono.empty());
        when(productServiceGateway.getProduct(productId)).thenReturn(Mono.just(productInfo));
        when(inventoryGateway.findByProductId(productId)).thenReturn(Mono.just(lowStock));

        StepVerifier.create(purchaseUseCase.create(input))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals("Stock insuficiente para realizar la compra", err.getMessage());
                })
                .verify();
    }

    @Test
    void shouldFindPurchaseById() {
        UUID purchaseId = UUID.randomUUID();
        Purchase purchase = Purchase.builder().id(purchaseId).productId(productId).quantity(3).build();

        when(purchaseGateway.findById(purchaseId)).thenReturn(Mono.just(purchase));

        StepVerifier.create(purchaseUseCase.findById(purchaseId))
                .assertNext(p -> assertEquals(purchaseId, p.getId()))
                .verifyComplete();
    }

    @Test
    void shouldReturn404WhenPurchaseNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(purchaseGateway.findById(unknownId)).thenReturn(Mono.empty());

        StepVerifier.create(purchaseUseCase.findById(unknownId))
                .expectErrorSatisfies(err -> assertInstanceOf(BusinessException.class, err))
                .verify();
    }

    @Test
    void shouldFindAllPurchases() {
        Purchase p1 = Purchase.builder().id(UUID.randomUUID()).productId(productId).build();
        Purchase p2 = Purchase.builder().id(UUID.randomUUID()).productId(productId).build();

        when(purchaseGateway.findAll("createdAt", "desc", 0, 10)).thenReturn(Flux.just(p1, p2));

        StepVerifier.create(purchaseUseCase.findAll(null, "createdAt", "desc", 0, 10))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldFilterPurchasesByProductId() {
        Purchase p1 = Purchase.builder().id(UUID.randomUUID()).productId(productId).build();

        when(purchaseGateway.findByProductId(productId)).thenReturn(Flux.just(p1));

        StepVerifier.create(purchaseUseCase.findAll(productId, "createdAt", "desc", 0, 10))
                .expectNextCount(1)
                .verifyComplete();

        verify(purchaseGateway, never()).findAll(any(), any(), anyInt(), anyInt());
    }
}
