package com.linktic.api;

import com.linktic.api.rest.request.CreateProductRequest;
import com.linktic.api.rest.request.UpdateProductRequest;
import com.linktic.model.exception.BusinessException;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.product.Product;
import com.linktic.model.product.ProductStatus;
import com.linktic.usecase.product.ProductUseCase;
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

import java.math.BigDecimal;
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
    private ProductUseCase productUseCase;

    @InjectMocks
    private Handler handler;

    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.fromString("a1b2c3d4-0001-0001-0001-000000000001");
        product = Product.builder()
                .id(productId)
                .sku("SKU-001")
                .name("Laptop Gamer Pro")
                .price(BigDecimal.valueOf(2999.99))
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ---- createProduct ----

    @Test
    void shouldCreateProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-001").name("Laptop Gamer Pro").price(BigDecimal.valueOf(2999.99)).build();

        when(productUseCase.create(any())).thenReturn(Mono.just(product));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        StepVerifier.create(handler.createProduct(serverRequest))
                .assertNext(res -> assertEquals(201, res.statusCode().value()))
                .verifyComplete();

        verify(productUseCase).create(any());
    }

    @Test
    void shouldReturn409WhenDuplicateSku() {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-001").name("Duplicate").price(BigDecimal.TEN).build();

        when(productUseCase.create(any()))
                .thenReturn(Mono.error(BusinessException.fromMessage(MessagesEnum.DUPLICATE_SKU)));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        StepVerifier.create(handler.createProduct(serverRequest))
                .assertNext(res -> assertEquals(409, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn400WhenSkuIsBlank() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("No SKU").price(BigDecimal.TEN).build();

        MockServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        StepVerifier.create(handler.createProduct(serverRequest))
                .assertNext(res -> assertEquals(400, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn400WhenPriceIsNegative() {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-BAD").name("Bad Price").price(BigDecimal.valueOf(-10)).build();

        MockServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        StepVerifier.create(handler.createProduct(serverRequest))
                .assertNext(res -> assertEquals(400, res.statusCode().value()))
                .verifyComplete();
    }

    @Test
    void shouldReturn400WhenPriceIsNull() {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-X").name("No Price").build();

        MockServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        StepVerifier.create(handler.createProduct(serverRequest))
                .assertNext(res -> assertEquals(400, res.statusCode().value()))
                .verifyComplete();
    }

    // ---- getProduct ----

    @Test
    void shouldGetProduct() {
        when(productUseCase.findById(productId)).thenReturn(Mono.just(product));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .pathVariable("id", productId.toString())
                .build();

        StepVerifier.create(handler.getProduct(serverRequest))
                .assertNext(res -> assertEquals(200, res.statusCode().value()))
                .verifyComplete();

        verify(productUseCase).findById(productId);
    }

    @Test
    void shouldReturn404WhenProductNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(productUseCase.findById(unknownId))
                .thenReturn(Mono.error(BusinessException.fromMessage(MessagesEnum.PRODUCT_NOT_FOUND)));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .pathVariable("id", unknownId.toString())
                .build();

        StepVerifier.create(handler.getProduct(serverRequest))
                .assertNext(res -> assertEquals(404, res.statusCode().value()))
                .verifyComplete();
    }

    // ---- getProducts ----

    @Test
    void shouldGetProducts() {
        when(productUseCase.findAll(null, null, "createdAt", "desc", 0, 10))
                .thenReturn(Flux.just(product));

        MockServerRequest serverRequest = MockServerRequest.builder().build();

        StepVerifier.create(handler.getProducts(serverRequest))
                .assertNext(res -> assertEquals(200, res.statusCode().value()))
                .verifyComplete();
    }

    // ---- updateProduct ----

    @Test
    void shouldUpdateProduct() {
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Updated Name").price(BigDecimal.valueOf(3500)).build();

        Product updated = product.toBuilder().name("Updated Name").price(BigDecimal.valueOf(3500)).build();
        when(productUseCase.update(eq(productId), any())).thenReturn(Mono.just(updated));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .pathVariable("id", productId.toString())
                .body(Mono.just(request));

        StepVerifier.create(handler.updateProduct(serverRequest))
                .assertNext(res -> assertEquals(200, res.statusCode().value()))
                .verifyComplete();

        verify(productUseCase).update(eq(productId), any());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentProduct() {
        UUID unknownId = UUID.randomUUID();
        UpdateProductRequest request = UpdateProductRequest.builder().name("X").build();

        when(productUseCase.update(eq(unknownId), any()))
                .thenReturn(Mono.error(BusinessException.fromMessage(MessagesEnum.PRODUCT_NOT_FOUND)));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .pathVariable("id", unknownId.toString())
                .body(Mono.just(request));

        StepVerifier.create(handler.updateProduct(serverRequest))
                .assertNext(res -> assertEquals(404, res.statusCode().value()))
                .verifyComplete();
    }

    // ---- deleteProduct ----

    @Test
    void shouldDeleteProduct() {
        when(productUseCase.delete(productId)).thenReturn(Mono.empty());

        MockServerRequest serverRequest = MockServerRequest.builder()
                .pathVariable("id", productId.toString())
                .build();

        StepVerifier.create(handler.deleteProduct(serverRequest))
                .assertNext(res -> assertEquals(200, res.statusCode().value()))
                .verifyComplete();

        verify(productUseCase).delete(productId);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentProduct() {
        UUID unknownId = UUID.randomUUID();
        when(productUseCase.delete(unknownId))
                .thenReturn(Mono.error(BusinessException.fromMessage(MessagesEnum.PRODUCT_NOT_FOUND)));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .pathVariable("id", unknownId.toString())
                .build();

        StepVerifier.create(handler.deleteProduct(serverRequest))
                .assertNext(res -> assertEquals(404, res.statusCode().value()))
                .verifyComplete();
    }
}
