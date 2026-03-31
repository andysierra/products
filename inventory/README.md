# Inventory Service

API Restful reactiva para gestion de inventario y compras, desarrollada con Spring Boot WebFlux y Clean Architecture.

## Stack Tecnologico

- **Java 21**
- **Spring Boot 4.0.5** (WebFlux - Reactivo)
- **R2DBC** con PostgreSQL 16
- **Flyway** para migraciones (JDBC)
- **Resilience4j** (Circuit Breaker + Retry + Timeout)
- **Gradle 9.4.1**
- **Scaffold** Clean Architecture Plugin v4.4.1
- **Jakarta Bean Validation** para validacion de inputs
- **JUnit 5 + Mockito + StepVerifier + MockWebServer** para pruebas unitarias

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
 +--------v---------+      +---------v----------+      +---------v---------+
 |   ENTRY POINTS    |      |      DOMAIN        |      |  DRIVEN ADAPTERS  |
 |  (reactive-web)   |      |                    |      |                   |
 |                   |      |  +==============+  |      | r2dbc-postgresql  |
 |  Handler          |----->|  | InventoryUC  |  |<-----| InventoryRepo     |
 |  RouterRest       |      |  | PurchaseUC   |  |      | PurchaseRepo      |
 |  ResponseBuilder  |      |  +------+-------+  |      |                   |
 |  RestValidator    |      |         |          |      | rest-consumer     |
 |  Mapper           |      |  +------v-------+  |      | ProductService    |
 |                   |      |  |   Models     |  |      |   Adapter         |
 |  Requests/        |      |  |   Gateways   |  |      | (WebClient +      |
 |  Responses        |      |  |   Enums      |  |      |  Resilience4j)    |
 +---------+---------+      +--------+--------+  |      +-------------------+
                            +--------------------+
```

### Capas

**Domain (Model):** Modelos `Inventory`, `Purchase`, `ProductInfo`. Enum `PurchaseStatus`. Interfaces gateway: `InventoryGateway`, `PurchaseGateway`, `ProductServiceGateway`. Excepciones personalizadas y `MessagesEnum`.

**Domain (Use Cases):** `InventoryUseCase` gestiona consulta e inicializacion de stock. `PurchaseUseCase` orquesta la creacion de compras: valida producto (via ProductServiceGateway), verifica stock disponible, descuenta con optimistic locking e idempotencia.

**Entry Points (reactive-web):** Routing funcional WebFlux con `Handler`, `RestValidator`, `Mapper`, `ResponseBuilder` (patron `{code, message, data}`).

**Driven Adapters:**
- `r2dbc-postgresql`: Persistencia reactiva con R2DBC. `@Version` para optimistic locking en inventory. `DatabaseClient` para queries dinamicas.
- `rest-consumer`: WebClient hacia Products Service con Resilience4j (circuit breaker, retry, timeout).

## Optimistic Locking

La tabla `inventory` tiene un campo `version BIGINT` que se usa para **control de concurrencia optimista**. Cuando dos compras concurrentes intentan descontar stock del mismo producto:

```
Thread A: lee inventory (version=0, available=100)
Thread B: lee inventory (version=0, available=100)
Thread A: UPDATE ... SET available=95, version=1 WHERE version=0  --> OK
Thread B: UPDATE ... SET available=95, version=1 WHERE version=0  --> FALLA (version ya es 1)
```

Spring Data R2DBC maneja esto automaticamente con `@Version`. Si la version no coincide, lanza `OptimisticLockingFailureException` que se traduce a un error 409 (`OPTIMISTIC_LOCK_ERROR`), indicando al cliente que reintente.

Esto evita race conditions sin usar locks pesimistas (SELECT FOR UPDATE), lo cual es mas eficiente para escenarios de alta concurrencia.

## Resiliencia (Inventory -> Products)

La comunicacion con el Products Service usa **Resilience4j** con tres mecanismos:

| Mecanismo | Configuracion | Comportamiento |
|-----------|--------------|----------------|
| **Timeout** | 2 segundos | Corta la llamada si tarda mas |
| **Retry** | 3 intentos, 500ms backoff | Reintenta en caso de fallo transitorio |
| **Circuit Breaker** | 50% failure rate, ventana de 10 calls | Abre el circuito si >50% de fallos, espera 10s para half-open |

Si el circuito esta abierto o todos los reintentos fallan, se retorna 503 (`PRODUCT_SERVICE_UNAVAILABLE`).

## Seguridad

| Mecanismo | Descripcion |
|-----------|-------------|
| **API Key** | Rutas `/api/**` requieren header `X-API-Key`. Actuator excluido. |
| **Rate Limiting** | 50 requests/minuto por IP (configurable). |
| **CORS** | Configurable via `cors.allowed-origins`. |
| **Correlation ID** | Propagado via `X-Correlation-Id` en headers y MDC. |

## Variables de Entorno

| Variable | Default | Descripcion |
|----------|---------|-------------|
| `DB_HOST` | localhost | Host de PostgreSQL |
| `DB_PORT` | 5432 | Puerto de PostgreSQL |
| `DB_NAME` | store_db | Nombre de la base de datos |
| `DB_SCHEMA` | public | Schema |
| `DB_USER` | admin | Usuario |
| `DB_PASSWORD` | admin | Password |
| `API_KEY` | inventory-secret-key-2026 | API Key del Inventory Service |
| `PRODUCTS_SERVICE_URL` | http://localhost:8080 | URL base del Products Service |
| `PRODUCTS_API_KEY` | products-secret-key-2026 | API Key para llamar al Products Service |
| `PRODUCTS_TIMEOUT` | 2000 | Timeout en ms para llamadas al Products Service |
| `RATE_LIMIT_MAX` | 50 | Requests maximos por ventana |
| `RATE_LIMIT_WINDOW` | 60000 | Ventana de rate limit en ms |

## DDL - Scripts SQL

```sql
CREATE TABLE inventory (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID            NOT NULL,
    available   INTEGER         NOT NULL DEFAULT 0 CHECK (available >= 0),
    reserved    INTEGER         NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    version     BIGINT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_product UNIQUE (product_id)
);

CREATE TABLE purchases (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID            NOT NULL,
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    status          VARCHAR(20)     NOT NULL DEFAULT 'COMPLETED',
    idempotency_key VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_purchases_idempotency UNIQUE (idempotency_key)
);
```

Migracion automatica con Flyway (`V1__create_inventory_and_purchases_tables.sql`).

## Ejecucion

### Con Docker Compose (desde la raiz de pruebatecnica3)

```bash
docker compose up -d
```

### Local (requiere Products Service corriendo en :8080)

```bash
./gradlew bootRun
```

## API Endpoints y cURLs

Todas las peticiones a `/api/**` requieren el header `X-API-Key`.

### 1. Consultar Inventario
`GET /api/v1/inventory/{productId}`

```bash
curl -s "http://localhost:8081/api/v1/inventory/{PRODUCT_UUID}" \
  -H "X-API-Key: inventory-secret-key-2026" | jq
```

### 2. Inicializar/Actualizar Stock
`PUT /api/v1/inventory/{productId}`

```bash
curl -s -X PUT "http://localhost:8081/api/v1/inventory/{PRODUCT_UUID}" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: inventory-secret-key-2026" \
  -d '{"available": 100}' | jq
```

### 3. Crear Compra
`POST /api/v1/purchases`

```bash
curl -s -X POST http://localhost:8081/api/v1/purchases \
  -H "Content-Type: application/json" \
  -H "X-API-Key: inventory-secret-key-2026" \
  -d '{
    "productId": "{PRODUCT_UUID}",
    "quantity": 5,
    "idempotencyKey": "purchase-001"
  }' | jq
```

### 4. Consultar Compra
`GET /api/v1/purchases/{id}`

```bash
curl -s "http://localhost:8081/api/v1/purchases/{PURCHASE_UUID}" \
  -H "X-API-Key: inventory-secret-key-2026" | jq
```

### 5. Listar Compras
`GET /api/v1/purchases`

```bash
curl -s "http://localhost:8081/api/v1/purchases?page=0&size=10" \
  -H "X-API-Key: inventory-secret-key-2026" | jq
```

Con filtro por producto:
```bash
curl -s "http://localhost:8081/api/v1/purchases?productId={PRODUCT_UUID}" \
  -H "X-API-Key: inventory-secret-key-2026" | jq
```

### 6. Health Check (sin API Key)
```bash
curl -s http://localhost:8081/actuator/health | jq
```

## Codigos de Operacion

| Codigo | Significado |
|--------|-------------|
| 21 | Inventario encontrado |
| 22 | Inventario actualizado |
| 23 | Compra registrada |
| 24 | Compra encontrada |
| 25 | Compras encontradas |
| 40 | Datos de entrada invalidos |
| 41 | API Key invalida |
| 42 | Producto no encontrado (Products Service) |
| 43 | Inventario no encontrado |
| 44 | Compra no encontrada |
| 45 | Stock insuficiente |
| 46 | Idempotency key duplicada |
| 47 | Conflicto de concurrencia (optimistic lock) |
| 48 | Rate limit excedido |
| 50 | Products Service no disponible |
| 51 | Error desconocido |

## Observabilidad

- **Logs estructurados:** JSON via Log4j2 JsonTemplateLayout
- **Correlation ID:** Propagado via `X-Correlation-Id` y MDC
- **Metricas:** Prometheus en `/actuator/prometheus`
- **Health checks:** `/actuator/health` con liveness y readiness probes

Made with by Andres Sierra
