package com.linktic.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    private static final String INVENTORY = "/api/v1/inventory";
    private static final String PURCHASES = "/api/v1/purchases";

    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route()
                .GET(INVENTORY + "/{productId}", handler::getInventory)
                .PUT(INVENTORY + "/{productId}", handler::updateInventory)
                .POST(PURCHASES, handler::createPurchase)
                .GET(PURCHASES + "/{id}", handler::getPurchase)
                .GET(PURCHASES, handler::getPurchases)
                .build();
    }
}
