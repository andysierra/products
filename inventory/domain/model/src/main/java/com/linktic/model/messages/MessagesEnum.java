package com.linktic.model.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MessagesEnum {

    INVENTORY_FOUND(200, "Inventario encontrado", "21"),
    INVENTORY_UPDATED(200, "Inventario actualizado exitosamente", "22"),
    PURCHASE_CREATED(201, "Compra registrada exitosamente", "23"),
    PURCHASE_FOUND(200, "Compra encontrada", "24"),
    PURCHASES_FOUND(200, "Compras encontradas", "25"),

    BAD_REQUEST(400, "Datos de entrada invalidos", "40"),
    UNAUTHORIZED(401, "API Key invalida o no proporcionada", "41"),
    PRODUCT_NOT_FOUND(404, "Producto no encontrado en el servicio de productos", "42"),
    INVENTORY_NOT_FOUND(404, "Inventario no encontrado para este producto", "43"),
    PURCHASE_NOT_FOUND(404, "Compra no encontrada", "44"),
    INSUFFICIENT_STOCK(409, "Stock insuficiente para realizar la compra", "45"),
    DUPLICATE_IDEMPOTENCY(409, "Ya existe una compra con esa clave de idempotencia", "46"),
    OPTIMISTIC_LOCK_ERROR(409, "Conflicto de concurrencia, intente nuevamente", "47"),
    RATE_LIMITED(429, "Demasiadas solicitudes, intente mas tarde", "48"),

    PRODUCT_SERVICE_UNAVAILABLE(503, "Servicio de productos no disponible", "50"),
    UNKNOWN_ERROR(500, "Error desconocido, estaremos reparandolo muy pronto", "51");

    private final int code;
    private final String message;
    private final String operationCode;

    public static MessagesEnum findByOpCode(String operationCode) {
        for (MessagesEnum value : values()) {
            if (operationCode.equals(value.operationCode)) {
                return value;
            }
        }
        return null;
    }
}
