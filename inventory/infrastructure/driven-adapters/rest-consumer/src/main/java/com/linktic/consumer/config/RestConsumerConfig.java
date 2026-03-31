package com.linktic.consumer.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class RestConsumerConfig {

    @Value("${adapters.rest-consumer.products-url}")
    private String productsBaseUrl;

    @Value("${adapters.rest-consumer.api-key}")
    private String apiKey;

    @Value("${adapters.rest-consumer.timeout:2000}")
    private int timeoutMs;

    @Bean
    public WebClient productsWebClient() {
        return WebClient.builder()
                .baseUrl(productsBaseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public CircuitBreaker productsCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        return CircuitBreaker.of("productsService", config);
    }

    @Bean
    public Retry productsRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build();
        return Retry.of("productsService", config);
    }

    @Bean
    public int productsTimeout() {
        return timeoutMs;
    }
}
