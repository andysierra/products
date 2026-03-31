package com.linktic.consumer;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.product.ProductInfo;
import com.linktic.model.product.gateways.ProductServiceGateway;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class ProductServiceAdapter implements ProductServiceGateway {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final int timeoutMs;

    public ProductServiceAdapter(
            WebClient productsWebClient,
            CircuitBreaker productsCircuitBreaker,
            Retry productsRetry,
            @Qualifier("productsTimeout") int productsTimeout) {
        this.webClient = productsWebClient;
        this.circuitBreaker = productsCircuitBreaker;
        this.retry = productsRetry;
        this.timeoutMs = productsTimeout;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<ProductInfo> getProduct(UUID productId) {
        return webClient.get()
                .uri("/api/v1/products/{id}", productId)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .flatMap(response -> {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    if (data == null) {
                        return Mono.error(BusinessException.fromMessage(MessagesEnum.PRODUCT_NOT_FOUND));
                    }
                    return Mono.just(ProductInfo.builder()
                            .id(UUID.fromString((String) data.get("id")))
                            .sku((String) data.get("sku"))
                            .name((String) data.get("name"))
                            .status((String) data.get("status"))
                            .build());
                })
                .onErrorMap(WebClientResponseException.NotFound.class,
                        e -> BusinessException.fromMessage(MessagesEnum.PRODUCT_NOT_FOUND))
                .onErrorMap(e -> !(e instanceof BusinessException),
                        e -> {
                            log.error("Error calling Products Service for productId={}: {}", productId, e.getMessage());
                            return BusinessException.fromMessage(MessagesEnum.PRODUCT_SERVICE_UNAVAILABLE);
                        });
    }
}
