package com.linktic.model.product.gateways;

import com.linktic.model.product.ProductInfo;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProductServiceGateway {
    Mono<ProductInfo> getProduct(UUID productId);
}
