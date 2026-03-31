package com.linktic.model.exception;

import com.linktic.model.messages.MessagesEnum;

public class BusinessException extends CustomException {

    public BusinessException(String message, String code, int clientCode) {
        super(message, code, clientCode);
    }

    public BusinessException(String message, String code, int clientCode, String[] info) {
        super(message, code, clientCode, null, info);
    }

    public static BusinessException fromMessage(MessagesEnum messagesEnum) {
        return new BusinessException(
                messagesEnum.getMessage(),
                messagesEnum.getOperationCode(),
                messagesEnum.getCode()
        );
    }
}
