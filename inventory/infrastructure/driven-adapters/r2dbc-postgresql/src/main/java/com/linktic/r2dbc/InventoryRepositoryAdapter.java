package com.linktic.r2dbc;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.inventory.Inventory;
import com.linktic.model.inventory.gateways.InventoryGateway;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.r2dbc.entity.InventoryEntity;
import com.linktic.r2dbc.helper.ReactiveAdapterOperations;
import com.linktic.r2dbc.reactiveRepository.InventoryReactiveRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class InventoryRepositoryAdapter extends ReactiveAdapterOperations<Inventory, InventoryEntity, UUID, InventoryReactiveRepository>
        implements InventoryGateway {

    public InventoryRepositoryAdapter(InventoryReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Inventory.class));
    }

    @Override
    public Mono<Inventory> findByProductId(UUID productId) {
        return repository.findByProductId(productId).map(this::toEntity);
    }

    @Override
    public Mono<Inventory> save(Inventory inventory) {
        return super.save(inventory);
    }

    @Override
    public Mono<Inventory> updateStock(UUID productId, int quantityDelta, long expectedVersion) {
        return repository.findByProductId(productId)
                .switchIfEmpty(Mono.error(BusinessException.fromMessage(MessagesEnum.INVENTORY_NOT_FOUND)))
                .flatMap(entity -> {
                    int newAvailable = entity.getAvailable() + quantityDelta;
                    int newReserved = entity.getReserved() - quantityDelta;
                    if (newAvailable < 0) {
                        return Mono.error(BusinessException.fromMessage(MessagesEnum.INSUFFICIENT_STOCK));
                    }
                    InventoryEntity updated = entity.toBuilder()
                            .available(newAvailable)
                            .reserved(Math.max(0, newReserved))
                            .updatedAt(Instant.now())
                            .build();
                    return repository.save(updated);
                })
                .map(this::toEntity)
                .onErrorMap(OptimisticLockingFailureException.class,
                        e -> BusinessException.fromMessage(MessagesEnum.OPTIMISTIC_LOCK_ERROR));
    }
}
