package com.linktic.model.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MessagesEnum {

    PRODUCT_CREATED(201, "Producto creado exitosamente", "21"),
    PRODUCT_UPDATED(200, "Producto actualizado exitosamente", "22"),
    PRODUCT_DELETED(200, "Producto eliminado exitosamente", "23"),
    PRODUCT_FOUND(200, "Producto encontrado", "24"),
    PRODUCTS_FOUND(200, "Productos encontrados", "25"),

    BAD_REQUEST(400, "Datos de entrada invalidos", "40"),
    PRODUCT_NOT_FOUND(404, "Producto no encontrado", "41"),
    DUPLICATE_SKU(409, "Ya existe un producto con ese SKU", "42"),
    VALIDATION_ERROR(422, "Error de validacion", "43"),

    UNAUTHORIZED(401, "API Key invalida o no proporcionada", "44"),
    RATE_LIMITED(429, "Demasiadas solicitudes, intente mas tarde", "45"),

    UNKNOWN_ERROR(500, "Error desconocido, estaremos reparandolo muy pronto", "50");

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
