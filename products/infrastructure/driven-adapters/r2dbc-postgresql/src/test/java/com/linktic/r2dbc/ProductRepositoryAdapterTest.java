package com.linktic.r2dbc;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.product.Product;
import com.linktic.model.product.ProductStatus;
import com.linktic.r2dbc.entity.ProductEntity;
import com.linktic.r2dbc.reactiveRepository.ProductReactiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryAdapterTest {

    @Mock
    private ProductReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private DatabaseClient databaseClient;

    private ProductRepositoryAdapter adapter;

    private Product product;
    private ProductEntity entity;
    private UUID productId;

    @BeforeEach
    void setUp() {
        adapter = new ProductRepositoryAdapter(repository, mapper, databaseClient);

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

        entity = ProductEntity.builder()
                .id(productId)
                .sku("SKU-001")
                .name("Laptop Gamer Pro")
                .price(BigDecimal.valueOf(2999.99))
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldSaveProduct() {
        when(mapper.map(product, ProductEntity.class)).thenReturn(entity);
        when(repository.save(any(ProductEntity.class))).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Product.class)).thenReturn(product);

        StepVerifier.create(adapter.save(product))
                .assertNext(saved -> {
                    assertEquals("SKU-001", saved.getSku());
                    assertEquals("Laptop Gamer Pro", saved.getName());
                    assertEquals(ProductStatus.ACTIVE, saved.getStatus());
                })
                .verifyComplete();

        verify(repository).save(any(ProductEntity.class));
    }

    @Test
    void shouldMapDataIntegrityViolationToDuplicateSkuException() {
        when(mapper.map(product, ProductEntity.class)).thenReturn(entity);
        when(repository.save(any(ProductEntity.class)))
                .thenReturn(Mono.error(new DataIntegrityViolationException("duplicate key")));

        StepVerifier.create(adapter.save(product))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals("Ya existe un producto con ese SKU", err.getMessage());
                })
                .verify();
    }

    @Test
    void shouldMapUnknownErrorToGenericException() {
        when(mapper.map(product, ProductEntity.class)).thenReturn(entity);
        when(repository.save(any(ProductEntity.class)))
                .thenReturn(Mono.error(new RuntimeException("connection lost")));

        StepVerifier.create(adapter.save(product))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals("Error desconocido, estaremos reparandolo muy pronto", err.getMessage());
                })
                .verify();
    }

    @Test
    void shouldFindBySku() {
        when(repository.findBySku("SKU-001")).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Product.class)).thenReturn(product);

        StepVerifier.create(adapter.findBySku("SKU-001"))
                .assertNext(found -> {
                    assertEquals("SKU-001", found.getSku());
                    assertEquals(productId, found.getId());
                })
                .verifyComplete();

        verify(repository).findBySku("SKU-001");
    }

    @Test
    void shouldReturnEmptyWhenSkuNotFound() {
        when(repository.findBySku("UNKNOWN")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findBySku("UNKNOWN"))
                .verifyComplete();
    }

    @Test
    void shouldUpdateProduct() {
        ProductEntity updated = entity.toBuilder().name("Updated Name").price(BigDecimal.valueOf(3500)).build();
        Product updatedProduct = product.toBuilder().name("Updated Name").price(BigDecimal.valueOf(3500)).build();

        when(repository.findById(productId)).thenReturn(Mono.just(entity));
        when(repository.save(any(ProductEntity.class))).thenReturn(Mono.just(updated));
        when(mapper.map(updated, Product.class)).thenReturn(updatedProduct);

        StepVerifier.create(adapter.update(updatedProduct))
                .assertNext(result -> {
                    assertEquals("Updated Name", result.getName());
                    assertEquals(BigDecimal.valueOf(3500), result.getPrice());
                })
                .verifyComplete();
    }

    @Test
    void shouldDeleteProduct() {
        when(repository.deleteById(productId)).thenReturn(Mono.empty());

        StepVerifier.create(adapter.deleteById(productId))
                .verifyComplete();

        verify(repository).deleteById(productId);
    }
}
