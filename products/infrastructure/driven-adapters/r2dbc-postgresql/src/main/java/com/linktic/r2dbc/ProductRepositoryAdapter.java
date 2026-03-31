package com.linktic.r2dbc;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.messages.MessagesEnum;
import com.linktic.model.product.Product;
import com.linktic.model.product.ProductStatus;
import com.linktic.model.product.gateways.ProductGateway;
import com.linktic.r2dbc.entity.ProductEntity;
import com.linktic.r2dbc.helper.ReactiveAdapterOperations;
import com.linktic.r2dbc.reactiveRepository.ProductReactiveRepository;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Repository
public class ProductRepositoryAdapter extends ReactiveAdapterOperations<Product, ProductEntity, UUID, ProductReactiveRepository>
        implements ProductGateway {

    private final DatabaseClient databaseClient;

    public ProductRepositoryAdapter(ProductReactiveRepository repository, ObjectMapper mapper, DatabaseClient databaseClient) {
        super(repository, mapper, d -> mapper.map(d, Product.class));
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Product> save(Product product) {
        return super.save(product)
                .onErrorMap(DataIntegrityViolationException.class, e -> BusinessException.fromMessage(MessagesEnum.DUPLICATE_SKU))
                .onErrorMap(e -> !(e instanceof BusinessException), e -> BusinessException.fromMessage(MessagesEnum.UNKNOWN_ERROR));
    }

    @Override
    public Mono<Product> findById(UUID id) {
        return super.findById(id);
    }

    @Override
    public Mono<Product> findBySku(String sku) {
        return repository.findBySku(sku).map(this::toEntity);
    }

    @Override
    public Flux<Product> findAll(ProductStatus status, String search, String sortBy, String sortDir, int page, int size) {
        String statusStr = status != null ? status.name() : null;
        String searchPattern = (search != null && !search.isBlank()) ? "%" + search.toLowerCase() + "%" : null;
        int offset = page * size;

        var spec = databaseClient.sql(buildFilterQuery(statusStr, searchPattern, sortBy, sortDir))
                .bind("size", size)
                .bind("offset", offset);

        if (statusStr != null) {
            spec = spec.bind("status", statusStr);
        }
        if (searchPattern != null) {
            spec = spec.bind("search", searchPattern);
        }

        return spec.map((row, metadata) -> mapRow(row))
                .all()
                .map(this::toEntity);
    }

    @Override
    public Mono<Long> count(ProductStatus status, String search) {
        String statusStr = status != null ? status.name() : null;
        String searchPattern = (search != null && !search.isBlank()) ? "%" + search.toLowerCase() + "%" : null;

        var spec = databaseClient.sql(buildCountQuery(statusStr, searchPattern));

        if (statusStr != null) {
            spec = spec.bind("status", statusStr);
        }
        if (searchPattern != null) {
            spec = spec.bind("search", searchPattern);
        }

        return spec.map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Product> update(Product product) {
        return repository.findById(product.getId())
                .flatMap(existing -> {
                    ProductEntity updated = existing.toBuilder()
                            .name(product.getName() != null ? product.getName() : existing.getName())
                            .price(product.getPrice() != null ? product.getPrice() : existing.getPrice())
                            .status(product.getStatus() != null ? product.getStatus().name() : existing.getStatus())
                            .build();
                    return repository.save(updated);
                })
                .map(this::toEntity);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return repository.deleteById(id);
    }

    private String buildFilterQuery(String status, String search, String sortBy, String sortDir) {
        String orderColumn = switch (sortBy != null ? sortBy : "createdAt") {
            case "price" -> "price";
            case "name" -> "name";
            default -> "created_at";
        };
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        var sb = new StringBuilder("SELECT * FROM products WHERE 1=1");
        if (status != null) {
            sb.append(" AND status = :status");
        }
        if (search != null) {
            sb.append(" AND (LOWER(sku) LIKE :search OR LOWER(name) LIKE :search)");
        }
        sb.append(" ORDER BY ").append(orderColumn).append(" ").append(direction);
        sb.append(" LIMIT :size OFFSET :offset");
        return sb.toString();
    }

    private String buildCountQuery(String status, String search) {
        var sb = new StringBuilder("SELECT COUNT(*) FROM products WHERE 1=1");
        if (status != null) {
            sb.append(" AND status = :status");
        }
        if (search != null) {
            sb.append(" AND (LOWER(sku) LIKE :search OR LOWER(name) LIKE :search)");
        }
        return sb.toString();
    }

    private ProductEntity mapRow(io.r2dbc.spi.Row row) {
        return ProductEntity.builder()
                .id(row.get("id", UUID.class))
                .sku(row.get("sku", String.class))
                .name(row.get("name", String.class))
                .price(row.get("price", BigDecimal.class))
                .status(row.get("status", String.class))
                .createdAt(row.get("created_at", Instant.class))
                .updatedAt(row.get("updated_at", Instant.class))
                .build();
    }
}
