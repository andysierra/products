package com.linktic.usecase.product;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.product.Product;
import com.linktic.model.product.ProductStatus;
import com.linktic.model.product.gateways.ProductGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductUseCaseTest {

    @Mock
    private ProductGateway productGateway;

    @InjectMocks
    private ProductUseCase productUseCase;

    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
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

    // ---- findById ----

    @Test
    void shouldFindProductById() {
        when(productGateway.findById(productId)).thenReturn(Mono.just(product));

        StepVerifier.create(productUseCase.findById(productId))
                .assertNext(found -> {
                    assertEquals("SKU-001", found.getSku());
                    assertEquals("Laptop Gamer Pro", found.getName());
                })
                .verifyComplete();

        verify(productGateway).findById(productId);
    }

    @Test
    void shouldReturn404WhenProductNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(productGateway.findById(unknownId)).thenReturn(Mono.empty());

        StepVerifier.create(productUseCase.findById(unknownId))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals("Producto no encontrado", err.getMessage());
                })
                .verify();
    }

    // ---- create ----

    @Test
    void shouldCreateProduct() {
        Product input = Product.builder().sku("SKU-NEW").name("New Product").price(BigDecimal.TEN).build();
        Product saved = input.toBuilder().id(UUID.randomUUID()).status(ProductStatus.ACTIVE).createdAt(Instant.now()).build();

        when(productGateway.save(any(Product.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(productUseCase.create(input))
                .assertNext(created -> {
                    assertNotNull(created.getId());
                    assertEquals(ProductStatus.ACTIVE, created.getStatus());
                })
                .verifyComplete();

        verify(productGateway).save(any(Product.class));
    }

    @Test
    void shouldReturn409WhenDuplicateSku() {
        Product input = Product.builder().sku("SKU-001").name("Duplicate").price(BigDecimal.TEN).build();

        when(productGateway.save(any(Product.class)))
                .thenReturn(Mono.error(new BusinessException("Ya existe un producto con ese SKU", "42", 409)));

        StepVerifier.create(productUseCase.create(input))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(409, ((BusinessException) err).getClientCode());
                })
                .verify();
    }

    // ---- update ----

    @Test
    void shouldUpdateProduct() {
        Product updateData = Product.builder().name("Updated Name").price(BigDecimal.valueOf(3500)).build();
        Product updated = product.toBuilder().name("Updated Name").price(BigDecimal.valueOf(3500)).updatedAt(Instant.now()).build();

        when(productGateway.findById(productId)).thenReturn(Mono.just(product));
        when(productGateway.update(any(Product.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(productUseCase.update(productId, updateData))
                .assertNext(result -> {
                    assertEquals("Updated Name", result.getName());
                    assertEquals(BigDecimal.valueOf(3500), result.getPrice());
                })
                .verifyComplete();

        verify(productGateway).update(any(Product.class));
    }

    @Test
    void shouldKeepExistingValuesWhenUpdateFieldsAreNull() {
        Product partialUpdate = Product.builder().name("Only Name").build();
        Product updated = product.toBuilder().name("Only Name").build();

        when(productGateway.findById(productId)).thenReturn(Mono.just(product));
        when(productGateway.update(any(Product.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(productUseCase.update(productId, partialUpdate))
                .assertNext(result -> {
                    assertEquals("Only Name", result.getName());
                    assertEquals(product.getPrice(), result.getPrice());
                    assertEquals(product.getStatus(), result.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentProduct() {
        UUID unknownId = UUID.randomUUID();
        when(productGateway.findById(unknownId)).thenReturn(Mono.empty());

        StepVerifier.create(productUseCase.update(unknownId, Product.builder().name("X").build()))
                .expectErrorSatisfies(err -> assertInstanceOf(BusinessException.class, err))
                .verify();

        verify(productGateway, never()).update(any());
    }

    // ---- delete ----

    @Test
    void shouldDeleteProduct() {
        when(productGateway.findById(productId)).thenReturn(Mono.just(product));
        when(productGateway.deleteById(productId)).thenReturn(Mono.empty());

        StepVerifier.create(productUseCase.delete(productId))
                .verifyComplete();

        verify(productGateway).deleteById(productId);
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentProduct() {
        UUID unknownId = UUID.randomUUID();
        when(productGateway.findById(unknownId)).thenReturn(Mono.empty());

        StepVerifier.create(productUseCase.delete(unknownId))
                .expectErrorSatisfies(err -> assertInstanceOf(BusinessException.class, err))
                .verify();

        verify(productGateway, never()).deleteById(any());
    }

    // ---- findAll ----

    @Test
    void shouldFindAllProducts() {
        Product product2 = product.toBuilder().id(UUID.randomUUID()).sku("SKU-002").name("Monitor").build();
        when(productGateway.findAll(null, null, "createdAt", "desc", 0, 10))
                .thenReturn(Flux.just(product, product2));

        StepVerifier.create(productUseCase.findAll(null, null, "createdAt", "desc", 0, 10))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldCountProducts() {
        when(productGateway.count(ProductStatus.ACTIVE, null)).thenReturn(Mono.just(5L));

        StepVerifier.create(productUseCase.count(ProductStatus.ACTIVE, null))
                .assertNext(count -> assertEquals(5L, count))
                .verifyComplete();
    }
}
