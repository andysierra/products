package com.linktic.r2dbc;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.purchase.Purchase;
import com.linktic.model.purchase.gateways.PurchaseGateway;
import com.linktic.r2dbc.entity.PurchaseEntity;
import com.linktic.r2dbc.helper.ReactiveAdapterOperations;
import com.linktic.r2dbc.reactiveRepository.PurchaseReactiveRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class PurchaseRepositoryAdapter extends ReactiveAdapterOperations<Purchase, PurchaseEntity, UUID, PurchaseReactiveRepository>
        implements PurchaseGateway {

    private final DatabaseClient databaseClient;

    public PurchaseRepositoryAdapter(PurchaseReactiveRepository repository, ObjectMapper mapper, DatabaseClient databaseClient) {
        super(repository, mapper, d -> mapper.map(d, Purchase.class));
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Purchase> save(Purchase purchase) {
        return super.save(purchase)
                .onErrorMap(DataIntegrityViolationException.class,
                        e -> BusinessException.fromMessage(MessagesEnum.DUPLICATE_IDEMPOTENCY));
    }

    @Override
    public Mono<Purchase> findById(UUID id) {
        return super.findById(id);
    }

    @Override
    public Mono<Purchase> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(this::toEntity);
    }

    @Override
    public Flux<Purchase> findByProductId(UUID productId) {
        return repository.findByProductId(productId).map(this::toEntity);
    }

    @Override
    public Flux<Purchase> findAll(String sortBy, String sortDir, int page, int size) {
        int offset = page * size;
        String orderColumn = "created_at";
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        String sql = "SELECT * FROM purchases ORDER BY " + orderColumn + " " + direction +
                " LIMIT :size OFFSET :offset";

        return databaseClient.sql(sql)
                .bind("size", size)
                .bind("offset", offset)
                .map((row, metadata) -> PurchaseEntity.builder()
                        .id(row.get("id", UUID.class))
                        .productId(row.get("product_id", UUID.class))
                        .quantity(row.get("quantity", Integer.class))
                        .status(row.get("status", String.class))
                        .idempotencyKey(row.get("idempotency_key", String.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .build())
                .all()
                .map(this::toEntity);
    }

    @Override
    public Mono<Long> count() {
        return databaseClient.sql("SELECT COUNT(*) FROM purchases")
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }
}
