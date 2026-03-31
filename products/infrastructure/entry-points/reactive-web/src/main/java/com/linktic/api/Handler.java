package com.linktic.api;

import com.linktic.api.helpers.Mapper;
import com.linktic.api.helpers.RestValidator;
import com.linktic.api.rest.ResponseBuilder;
import com.linktic.api.rest.request.CreateProductRequest;
import com.linktic.api.rest.request.UpdateProductRequest;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.product.ProductStatus;
import com.linktic.usecase.product.ProductUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Handler {

    private final ProductUseCase productUseCase;

    public Mono<ServerResponse> createProduct(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreateProductRequest.class)
                .map(RestValidator::validate)
                .flatMap(request -> productUseCase.create(Mapper.toProduct(request)))
                .flatMap(product -> ResponseBuilder.success(Mapper.toProductResponse(product), MessagesEnum.PRODUCT_CREATED))
                .onErrorResume(ResponseBuilder::handleError);
    }

    public Mono<ServerResponse> getProduct(ServerRequest serverRequest) {
        UUID id = UUID.fromString(serverRequest.pathVariable("id"));
        return productUseCase.findById(id)
                .flatMap(product -> ResponseBuilder.success(Mapper.toProductResponse(product), MessagesEnum.PRODUCT_FOUND))
                .onErrorResume(ResponseBuilder::handleError);
    }

    public Mono<ServerResponse> getProducts(ServerRequest serverRequest) {
        ProductStatus status = serverRequest.queryParam("status")
                .map(s -> ProductStatus.valueOf(s.toUpperCase()))
                .orElse(null);
        String search = serverRequest.queryParam("search").orElse(null);
        String sortBy = serverRequest.queryParam("sortBy").orElse("createdAt");
        String sortDir = serverRequest.queryParam("sortDir").orElse("desc");
        int page = serverRequest.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = serverRequest.queryParam("size").map(Integer::parseInt).orElse(10);

        return productUseCase.findAll(status, search, sortBy, sortDir, page, size)
                .map(Mapper::toProductResponse)
                .collectList()
                .flatMap(list -> ResponseBuilder.success(list, MessagesEnum.PRODUCTS_FOUND))
                .onErrorResume(ResponseBuilder::handleError);
    }

    public Mono<ServerResponse> updateProduct(ServerRequest serverRequest) {
        UUID id = UUID.fromString(serverRequest.pathVariable("id"));
        return serverRequest.bodyToMono(UpdateProductRequest.class)
                .map(RestValidator::validate)
                .flatMap(request -> productUseCase.update(id, Mapper.toProduct(request)))
                .flatMap(product -> ResponseBuilder.success(Mapper.toProductResponse(product), MessagesEnum.PRODUCT_UPDATED))
                .onErrorResume(ResponseBuilder::handleError);
    }

    public Mono<ServerResponse> deleteProduct(ServerRequest serverRequest) {
        UUID id = UUID.fromString(serverRequest.pathVariable("id"));
        return productUseCase.delete(id)
                .then(ResponseBuilder.success("", MessagesEnum.PRODUCT_DELETED))
                .onErrorResume(ResponseBuilder::handleError);
    }
}
