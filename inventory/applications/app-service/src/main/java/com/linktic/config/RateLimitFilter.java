package com.linktic.config;

import com.linktic.model.messages.MessagesEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(1)
@Slf4j
public class RateLimitFilter implements WebFilter {

    private static final String API_PREFIX = "/api/";

    @Value("${security.rate-limit.max-requests:50}")
    private int maxRequests;

    @Value("${security.rate-limit.window-ms:60000}")
    private long windowMs;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (!path.startsWith(API_PREFIX)) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(maxRequests, windowMs));

        if (bucket.tryConsume()) {
            return chain.filter(exchange);
        }

        log.warn("Rate limit exceeded for IP: {}", clientIp);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        MessagesEnum msg = MessagesEnum.RATE_LIMITED;
        String body = String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"data\":\"\"}",
                msg.getOperationCode(), msg.getMessage()
        );

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory().wrap(body.getBytes())));
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final long windowMs;
        private final AtomicLong tokens;
        private volatile long windowStart;

        TokenBucket(int maxTokens, long windowMs) {
            this.maxTokens = maxTokens;
            this.windowMs = windowMs;
            this.tokens = new AtomicLong(maxTokens);
            this.windowStart = System.currentTimeMillis();
        }

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) {
                tokens.set(maxTokens);
                windowStart = now;
            }
            return tokens.decrementAndGet() >= 0;
        }
    }
}
