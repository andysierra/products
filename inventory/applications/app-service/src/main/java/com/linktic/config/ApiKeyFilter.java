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

@Component
@Order(0)
@Slf4j
public class ApiKeyFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_PREFIX = "/api/";

    @Value("${security.api-key}")
    private String apiKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (!path.startsWith(API_PREFIX)) {
            return chain.filter(exchange);
        }

        String requestApiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

        if (apiKey.equals(requestApiKey)) {
            return chain.filter(exchange);
        }

        log.warn("Unauthorized request to {} - invalid or missing API Key", path);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        MessagesEnum msg = MessagesEnum.UNAUTHORIZED;
        String body = String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"data\":\"\"}",
                msg.getOperationCode(), msg.getMessage()
        );

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory().wrap(body.getBytes())));
    }
}
