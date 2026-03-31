# Products Service

API Restful reactiva para gestion de productos, desarrollada con Spring Boot WebFlux y Clean Architecture.

## Stack Tecnologico

- **Java 21**
- **Spring Boot 4.0.5** (WebFlux - Reactivo)
- **R2DBC** con PostgreSQL 16
- **Flyway** para migraciones (JDBC)
- **Gradle 9.4.1**
- **Scaffold** Clean Architecture Plugin v4.4.1
- **Jakarta Bean Validation** para validacion de inputs
- **JUnit 5 + Mockito + StepVerifier** para pruebas unitarias
- **Log4j2 JsonTemplateLayout** para logs estructurados en JSON
- **Micrometer + Prometheus** para metricas
- **Docker + Docker Compose** para contenerizacion

## Arquitectura Hexagonal (Puertos y Adaptadores)

```
                    +--------------------------------------+
                    |           APPLICATION                |
                    |  (Ensamblaje, configuracion, beans)  |
                    |  ApiKeyFilter, RateLimitFilter,      |
                    |  CorrelationIdFilter, Flyway         |
                    +-----------------+--------------------+
                                      |
          +---------------------------+---------------------------+
          |                           |                           |
 +--------v---------+      +---------v----------+      +---------v--------+
 |   ENTRY POINTS    |      |      DOMAIN        |      | DRIVEN ADAPTERS  |
 |  (reactive-web)   |      |                    |      | (r2dbc-postgres) |
 |                   |      |  +==============+  |      |                  |
 |  Handler          |----->|  |  Use Cases   |  |<-----|  ProductRepo     |
 |  RouterRest       |      |  |              |  |      |    Adapter       |
 |  ResponseBuilder  |      |  +------+-------+  |      |                  |
 |  RestValidator    |      |         |          |      |  ProductEntity   |
 |  Mapper           |      |  +------v-------+  |      |  DatabaseClient  |
 |                   |      |  |   Models     |  |      |  ConnectionPool  |
 |  Requests/        |      |  |   Gateways   |  |      |                  |
 |  Responses        |      |  |   Enums      |  |      |                  |
 +---------+---------+      +--------+--------+  |      +------------------+
                            +--------------------+
```

### Capas

**Domain (Model):** Modelo `Product`, enum `ProductStatus`, interfaz `ProductGateway` (port), excepciones personalizadas (`BusinessException`, `CustomException`) y mensajes (`MessagesEnum`).

**Domain (Use Cases):** Logica de negocio pura. `ProductUseCase` maneja CRUD completo con validaciones de existencia, asignacion de estado inicial y merge parcial en updates.

**Entry Points (reactive-web):** Capa de presentacion con routing funcional de WebFlux. `Handler` orquesta las peticiones, `RestValidator` valida los DTOs con Jakarta Validation, `Mapper` convierte entre request/response y modelos de dominio. `ResponseBuilder` envuelve todas las respuestas en formato `{code, message, data}`.

**Driven Adapters (r2dbc-postgresql):** Implementacion del `ProductGateway`. Usa `DatabaseClient` para queries dinamicas con filtros, ordenamiento y paginacion. Traduce `DataIntegrityViolationException` a `BusinessException` (SKU duplicado).

## Seguridad

| Mecanismo | Descripcion |
|-----------|-------------|
| **API Key** | Todas las rutas `/api/**` requieren header `X-API-Key`. Endpoints de actuator (`/actuator/**`) estan excluidos. |
| **Rate Limiting** | Limite de 50 requests por minuto por IP (configurable). Ventana fija con token bucket in-memory. |
| **CORS** | Configurado via `cors.allowed-origins` en `application.yaml`. |
| **Correlation ID** | `WebFilter` que propaga `X-Correlation-Id` en request/response y MDC para trazabilidad. |

## Variables de Entorno

| Variable | Default | Descripcion |
|----------|---------|-------------|
| `DB_HOST` | localhost | Host de PostgreSQL |
| `DB_PORT` | 5432 | Puerto de PostgreSQL |
| `DB_NAME` | store_db | Nombre de la base de datos |
| `DB_SCHEMA` | public | Schema de la base de datos |
| `DB_USER` | admin | Usuario de PostgreSQL |
| `DB_PASSWORD` | admin | Password de PostgreSQL |
| `API_KEY` | products-secret-key-2026 | API Key para autenticacion |
| `RATE_LIMIT_MAX` | 50 | Requests maximos por ventana |
| `RATE_LIMIT_WINDOW` | 60000 | Ventana de rate limit en ms |

## DDL - Scripts SQL

```sql
CREATE TABLE products (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sku         VARCHAR(50)     NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    price       NUMERIC(12,2)   NOT NULL CHECK (price >= 0),
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_sku UNIQUE (sku)
);

CREATE INDEX idx_products_status     ON products (status);
CREATE INDEX idx_products_sku        ON products (sku);
CREATE INDEX idx_products_name       ON products USING gin (name gin_trgm_ops);
CREATE INDEX idx_products_price      ON products (price);
CREATE INDEX idx_products_created_at ON products (created_at);
```

La migracion se ejecuta automaticamente con Flyway al iniciar la aplicacion (`V1__create_products_table.sql`).

## Ejecucion

### Con Docker Compose (desde la raiz de pruebatecnica3)

```bash
docker compose up -d
```

Esto levanta PostgreSQL 16 y el servicio de Products en el puerto 8080.

### Local (con PostgreSQL existente)

```bash
./gradlew bootRun
```

### Con variables de entorno inline

```bash
DB_HOST=localhost DB_PORT=5432 DB_NAME=store_db DB_USER=admin DB_PASSWORD=admin API_KEY=mi-clave ./gradlew bootRun
```

## API Endpoints y cURLs

Todas las peticiones a `/api/**` requieren el header `X-API-Key`.

### 1. Crear Producto
`POST /api/v1/products`

```bash
curl -s -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "X-API-Key: products-secret-key-2026" \
  -d '{
    "sku": "SKU-001",
    "name": "Laptop Gamer Pro",
    "price": 2999.99
  }' | jq
```

### 2. Listar Productos
`GET /api/v1/products`

```bash
curl -s "http://localhost:8080/api/v1/products" \
  -H "X-API-Key: products-secret-key-2026" | jq
```

Con filtros:
```bash
curl -s "http://localhost:8080/api/v1/products?status=ACTIVE&search=laptop&sortBy=price&sortDir=asc&page=0&size=5" \
  -H "X-API-Key: products-secret-key-2026" | jq
```

### 3. Consultar Producto
`GET /api/v1/products/{id}`

```bash
curl -s "http://localhost:8080/api/v1/products/{UUID}" \
  -H "X-API-Key: products-secret-key-2026" | jq
```

### 4. Actualizar Producto
`PUT /api/v1/products/{id}`

```bash
curl -s -X PUT "http://localhost:8080/api/v1/products/{UUID}" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: products-secret-key-2026" \
  -d '{
    "name": "Laptop Gamer Pro Max",
    "price": 3499.99
  }' | jq
```

### 5. Eliminar Producto
`DELETE /api/v1/products/{id}`

```bash
curl -s -X DELETE "http://localhost:8080/api/v1/products/{UUID}" \
  -H "X-API-Key: products-secret-key-2026" | jq
```

### 6. Health Check (sin API Key)
`GET /actuator/health`

```bash
curl -s http://localhost:8080/actuator/health | jq
```

## Codigos de Operacion

| Codigo | Significado |
|--------|-------------|
| 21 | Producto creado |
| 22 | Producto actualizado |
| 23 | Producto eliminado |
| 24 | Producto encontrado |
| 25 | Productos encontrados |
| 40 | Datos de entrada invalidos |
| 41 | Producto no encontrado |
| 42 | SKU duplicado |
| 43 | Error de validacion |
| 44 | API Key invalida o no proporcionada |
| 45 | Rate limit excedido |
| 50 | Error desconocido |

## Calidad

```bash
# Ejecutar tests
./gradlew test

# Generar reporte Jacoco
./gradlew jacocoMergedReport
```

| Modulo | Tests | Descripcion |
|--------|-------|-------------|
| usecase | 11 | CRUD, validaciones de negocio, merge parcial |
| reactive-web | 12 | Handler: creacion, consulta, actualizacion, eliminacion, errores 400/404/409 |
| r2dbc-postgresql | 7 | Adapter: save, findBySku, update, delete, manejo de errores de BD |
| app-service | 7 | Arquitectura (Clean Architecture) + configuracion |
| **Total** | **37** | Todos pasan con 0 failures |

## Observabilidad

- **Logs estructurados:** JSON via Log4j2 JsonTemplateLayout con campos timestamp, level, thread, logger, message, correlationId, exception
- **Correlation ID:** Propagado automaticamente via `X-Correlation-Id` header y MDC
- **Metricas:** Prometheus en `/actuator/prometheus`
- **Health checks:** `/actuator/health` con liveness y readiness probes

Made with by Andres Sierra
