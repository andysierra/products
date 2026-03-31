package com.linktic.config;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@Component
@Order(-1)
public class CorrelationIdFilter implements WebFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        String finalCorrelationId = correlationId;
        return chain.filter(exchange)
                .contextWrite(Context.of(CORRELATION_ID_KEY, finalCorrelationId))
                .doOnEach(signal -> {
                    if (!signal.isOnComplete()) {
                        signal.getContextView().getOrEmpty(CORRELATION_ID_KEY)
                                .ifPresent(id -> MDC.put(CORRELATION_ID_KEY, id.toString()));
                    }
                })
                .doFinally(signalType -> MDC.remove(CORRELATION_ID_KEY));
    }
}
