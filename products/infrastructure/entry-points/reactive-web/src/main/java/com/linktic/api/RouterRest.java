package com.linktic.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    private static final String PRODUCTS = "/api/v1/products";

    @Bean
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route()
                .GET(PRODUCTS, handler::getProducts)
                .GET(PRODUCTS + "/{id}", handler::getProduct)
                .POST(PRODUCTS, handler::createProduct)
                .PUT(PRODUCTS + "/{id}", handler::updateProduct)
                .DELETE(PRODUCTS + "/{id}", handler::deleteProduct)
                .build();
    }
}
