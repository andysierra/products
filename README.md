# 🧩 Inventory & Products Microservices

## Overview

Sistema basado en microservicios para gestión de productos e inventario.

## Servicios

### Products Service (Puerto 8080)

CRUD de productos con validaciones, paginación, filtros y ordenamiento.

### Inventory Service (Puerto 8081)

Gestión de inventario y compras con control de concurrencia e
idempotencia.

## Ejecución

``` bash
docker compose up -d
```

## Ejemplos de uso

### Crear producto

``` bash
curl -X POST http://localhost:8080/api/v1/products \
-H "Content-Type: application/json" \
-H "X-API-Key: products-secret-key-2026" \
-d '{"sku":"SKU-001","name":"Laptop Gamer Pro","price":2999.99}'
```

### Inicializar inventario

``` bash
curl -X PUT http://localhost:8081/api/v1/inventory/{PRODUCT_ID} \
-H "Content-Type: application/json" \
-H "X-API-Key: inventory-secret-key-2026" \
-d '{"available":100}'
```

### Crear compra

``` bash
curl -X POST http://localhost:8081/api/v1/purchases \
-H "Content-Type: application/json" \
-H "X-API-Key: inventory-secret-key-2026" \
-d '{"productId":"{PRODUCT_ID}","quantity":5,"idempotencyKey":"compra-001"}'
```

## Características técnicas

-   Java 21 + Spring Boot WebFlux
-   PostgreSQL + R2DBC
-   Flyway
-   Resilience4j
-   Arquitectura limpia
-   API Key Security
-   Rate limiting
-   Logs estructurados + correlation-id
-   Métricas con Prometheus

## Concurrencia

Se implementa **optimistic locking** para evitar stock negativo en
compras concurrentes.

## Resiliencia

-   Timeout
-   Retry
-   Circuit Breaker

## Autor

Andrés Sierra
