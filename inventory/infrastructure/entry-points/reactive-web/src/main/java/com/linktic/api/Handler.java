package com.linktic.api;

import com.linktic.api.helpers.Mapper;
import com.linktic.api.helpers.RestValidator;
import com.linktic.api.rest.ResponseBuilder;
import com.linktic.api.rest.request.CreatePurchaseRequest;
import com.linktic.api.rest.request.UpdateInventoryRequest;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.usecase.inventory.InventoryUseCase;
import com.linktic.usecase.purchase.PurchaseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Handler {

    private final InventoryUseCase inventoryUseCase;
    private final PurchaseUseCase purchaseUseCase;

    public Mono<ServerResponse> getInventory(ServerRequest serverRequest) {
        UUID productId = UUID.fromString(serverRequest.pathVariable("productId"));
        return inventoryUseCase.findByProductId(productId)
                .flatMap(inv -> ResponseBuilder.success(Mapper.toInventoryResponse(inv), MessagesEnum.INVENTORY_FOUND))
                .onErrorResume(ResponseBuilder::handleError);
    }

    public Mono<ServerResponse> updateInventory(ServerRequest serverRequest) {
        UUID productId = UUID.fromString(serverRequest.pathVariable("productId"));
        return serverRequest.bodyToMono(UpdateInventoryRequest.class)
                .map(RestValidator::validate)
                .flatMap(request -> inventoryUseCase.initializeStock(productId, request.getAvailable()))
                .flatMap(inv -> ResponseBuilder.success(Mapper.toInventoryResponse(inv), MessagesEnum.INVENTORY_UPDATED))
                .onErrorResume(ResponseBuilder::handleError);
    }

    public Mono<ServerResponse> createPurchase(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreatePurchaseRequest.class)
                .map(RestValidator::validate)
                .flatMap(request -> purchaseUseCase.create(Mapper.toPurchase(request)))
                .flatMap(purchase -> ResponseBuilder.success(Mapper.toPurchaseResponse(purchase), MessagesEnum.PURCHASE_CREATED))
                .onErrorResume(ResponseBuilder::handleError);
    }

    public Mono<ServerResponse> getPurchase(ServerRequest serverRequest) {
        UUID id = UUID.fromString(serverRequest.pathVariable("id"));
        return purchaseUseCase.findById(id)
                .flatMap(purchase -> ResponseBuilder.success(Mapper.toPurchaseResponse(purchase), MessagesEnum.PURCHASE_FOUND))
                .onErrorResume(ResponseBuilder::handleError);
    }

    public Mono<ServerResponse> getPurchases(ServerRequest serverRequest) {
        UUID productId = serverRequest.queryParam("productId")
                .map(UUID::fromString)
                .orElse(null);
        String sortBy = serverRequest.queryParam("sortBy").orElse("createdAt");
        String sortDir = serverRequest.queryParam("sortDir").orElse("desc");
        int page = serverRequest.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = serverRequest.queryParam("size").map(Integer::parseInt).orElse(10);

        return purchaseUseCase.findAll(productId, sortBy, sortDir, page, size)
                .map(Mapper::toPurchaseResponse)
                .collectList()
                .flatMap(list -> ResponseBuilder.success(list, MessagesEnum.PURCHASES_FOUND))
                .onErrorResume(ResponseBuilder::handleError);
    }
}
