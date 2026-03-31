package com.linktic.api;

import com.linktic.api.rest.request.CreatePurchaseRequest;
import com.linktic.api.rest.request.UpdateInventoryRequest;
import com.linktic.model.exception.BusinessException;
import com.linktic.model.inventory.Inventory;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.purchase.Purchase;
import com.linktic.model.purchase.PurchaseStatus;
import com.linktic.usecase.inventory.InventoryUseCase;
import com.linktic.usecase.purchase.PurchaseUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlerTest {

    @Mock
    private InventoryUseCase inventoryUseCase;

    @Mock
    private PurchaseUseCase purchaseUseCase;

    @InjectMocks
    private Handler handler;

    private UUID productId;
    private Inventory inventory;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        productId = UUID.fromString("a1b2c3d4-0001-0001-0001-000000000001");
        inventory = Inventory.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .available(100)
                .reserved(0)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        purchase = Purchase.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .quantity(5)
                .status(PurchaseStatus.COMPLETED)
                .idempotencyKey("key-001")
                .createdAt(Instant.now())
                .build();
    }

    // ---- getInventory ----

    @Test
    void shouldGetInventory() {
        when(inventoryUseCase.findByProductId(productId)).thenReturn(Mono.just(inventory));

        MockServerRequest request = MockServerRequest.builder()
                .pathVariable("productId", productId.toString())
                .build();

        StepVerifier.create(handler.getInventory(request))
                .assertNext(res -> assertEquals(200, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn404WhenInventoryNotFound() {
        when(inventoryUseCase.findByProductId(productId))
                .thenReturn(Mono.error(BusinessException.fromMessage(MessagesEnum.INVENTORY_NOT_FOUND)));

        MockServerRequest request = MockServerRequest.builder()
                .pathVariable("productId", productId.toString())
                .build();

        StepVerifier.create(handler.getInventory(request))
                .assertNext(res -> assertEquals(404, res.statusCode().value()))
                .verifyComplete();
    }

    // ---- updateInventory ----

    @Test
    void shouldUpdateInventory() {
        UpdateInventoryRequest body = UpdateInventoryRequest.builder().available(200).build();
        Inventory updated = inventory.toBuilder().available(200).build();

        when(inventoryUseCase.initializeStock(productId, 200)).thenReturn(Mono.just(updated));

        MockServerRequest request = MockServerRequest.builder()
                .pathVariable("productId", productId.toString())
                .body(Mono.just(body));

        StepVerifier.create(handler.updateInventory(request))
                .assertNext(res -> assertEquals(200, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn400WhenAvailableIsNull() {
        UpdateInventoryRequest body = UpdateInventoryRequest.builder().build();

        MockServerRequest request = MockServerRequest.builder()
                .pathVariable("productId", productId.toString())
                .body(Mono.just(body));

        StepVerifier.create(handler.updateInventory(request))
                .assertNext(res -> assertEquals(400, res.statusCode().value()))
                .verifyComplete();
    }

    // ---- createPurchase ----

    @Test
    void shouldCreatePurchase() {
        CreatePurchaseRequest body = CreatePurchaseRequest.builder()
                .productId(productId).quantity(5).idempotencyKey("key-001").build();

        when(purchaseUseCase.create(any())).thenReturn(Mono.just(purchase));

        MockServerRequest request = MockServerRequest.builder()
                .body(Mono.just(body));

        StepVerifier.create(handler.createPurchase(request))
                .assertNext(res -> assertEquals(201, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn409WhenDuplicateIdempotency() {
        CreatePurchaseRequest body = CreatePurchaseRequest.builder()
                .productId(productId).quantity(1).idempotencyKey("key-dup").build();

        when(purchaseUseCase.create(any()))
                .thenReturn(Mono.error(BusinessException.fromMessage(MessagesEnum.DUPLICATE_IDEMPOTENCY)));

        MockServerRequest request = MockServerRequest.builder()
                .body(Mono.just(body));

        StepVerifier.create(handler.createPurchase(request))
                .assertNext(res -> assertEquals(409, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn400WhenQuantityIsNull() {
        CreatePurchaseRequest body = CreatePurchaseRequest.builder()
                .productId(productId).idempotencyKey("key-x").build();

        MockServerRequest request = MockServerRequest.builder()
                .body(Mono.just(body));

        StepVerifier.create(handler.createPurchase(request))
                .assertNext(res -> assertEquals(400, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn409WhenInsufficientStock() {
        CreatePurchaseRequest body = CreatePurchaseRequest.builder()
                .productId(productId).quantity(999).idempotencyKey("key-big").build();

        when(purchaseUseCase.create(any()))
                .thenReturn(Mono.error(BusinessException.fromMessage(MessagesEnum.INSUFFICIENT_STOCK)));

        MockServerRequest request = MockServerRequest.builder()
                .body(Mono.just(body));

        StepVerifier.create(handler.createPurchase(request))
                .assertNext(res -> assertEquals(409, res.statusCode().value()))
                .verifyComplete();
    }

    // ---- getPurchase ----

    @Test
    void shouldGetPurchase() {
        UUID purchaseId = purchase.getId();
        when(purchaseUseCase.findById(purchaseId)).thenReturn(Mono.just(purchase));

        MockServerRequest request = MockServerRequest.builder()
                .pathVariable("id", purchaseId.toString())
                .build();

        StepVerifier.create(handler.getPurchase(request))
                .assertNext(res -> assertEquals(200, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn404WhenPurchaseNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(purchaseUseCase.findById(unknownId))
                .thenReturn(Mono.error(BusinessException.fromMessage(MessagesEnum.PURCHASE_NOT_FOUND)));

        MockServerRequest request = MockServerRequest.builder()
                .pathVariable("id", unknownId.toString())
                .build();

        StepVerifier.create(handler.getPurchase(request))
                .assertNext(res -> assertEquals(404, res.statusCode().value()))
                .verifyComplete();
    }

    // ---- getPurchases ----

    @Test
    void shouldGetPurchases() {
        when(purchaseUseCase.findAll(null, "createdAt", "desc", 0, 10))
                .thenReturn(Flux.just(purchase));

        MockServerRequest request = MockServerRequest.builder().build();

        StepVerifier.create(handler.getPurchases(request))
                .assertNext(res -> assertEquals(200, res.statusCode().value()))
                .verifyComplete();
    }
}
