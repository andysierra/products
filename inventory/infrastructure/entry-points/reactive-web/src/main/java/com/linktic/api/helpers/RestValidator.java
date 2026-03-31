package com.linktic.api.helpers;

import com.linktic.model.exception.BusinessException;
import com.linktic.model.messages.MessagesEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

public class RestValidator {

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private RestValidator() {
    }

    public static <T> T validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new BusinessException(
                    MessagesEnum.BAD_REQUEST.getMessage(),
                    MessagesEnum.BAD_REQUEST.getOperationCode(),
                    MessagesEnum.BAD_REQUEST.getCode(),
                    new String[]{details}
            );
        }
        return request;
    }
}
