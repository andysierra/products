package com.linktic.consumer;

import com.linktic.model.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceAdapterTest {

    private MockWebServer mockWebServer;
    private ProductServiceAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        CircuitBreaker cb = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        Retry retry = Retry.of("test", RetryConfig.custom().maxAttempts(1).build());

        adapter = new ProductServiceAdapter(webClient, cb, retry, 5000);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldGetProductSuccessfully() {
        UUID productId = UUID.randomUUID();
        String json = """
                {
                  "code": "24",
                  "message": "Producto encontrado",
                  "data": {
                    "id": "%s",
                    "sku": "SKU-001",
                    "name": "Laptop",
                    "status": "ACTIVE"
                  }
                }
                """.formatted(productId);

        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(adapter.getProduct(productId))
                .assertNext(product -> {
                    assertEquals("SKU-001", product.getSku());
                    assertEquals("Laptop", product.getName());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturn404WhenProductNotFound() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(adapter.getProduct(UUID.randomUUID()))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(404, ((BusinessException) err).getClientCode());
                })
                .verify();
    }

    @Test
    void shouldReturn503WhenServiceUnavailable() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(adapter.getProduct(UUID.randomUUID()))
                .expectErrorSatisfies(err -> {
                    assertInstanceOf(BusinessException.class, err);
                    assertEquals(503, ((BusinessException) err).getClientCode());
                })
                .verify();
    }
}
