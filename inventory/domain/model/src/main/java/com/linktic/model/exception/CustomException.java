package com.linktic.model.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class CustomException extends RuntimeException {

    private final List<Error> errors = new ArrayList<>();
    private final int clientCode;
    private final String opcode;
    private final Throwable cause;

    protected CustomException(String message, String code, int clientCode, Throwable throwable, String... details) {
        super(message, throwable);
        errors.add(new Error(code, message, LocalDateTime.now(), List.of(details)));
        this.clientCode = clientCode;
        this.opcode = code;
        this.cause = throwable;
    }

    protected CustomException(String message, String code, int clientCode) {
        this(message, code, clientCode, null);
    }
}
