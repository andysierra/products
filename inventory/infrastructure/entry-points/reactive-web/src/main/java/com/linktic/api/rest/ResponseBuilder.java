package com.linktic.api.rest;

import com.linktic.model.exception.CustomException;
import com.linktic.model.messages.MessagesEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Getter
@Builder
@Slf4j
public class ResponseBuilder {
    private String code;
    private String message;
    private Object data;

    public static Mono<ServerResponse> success(Object data, MessagesEnum messagesEnum) {
        ResponseBuilder body = ResponseBuilder.builder()
                .code(messagesEnum.getOperationCode())
                .message(messagesEnum.getMessage())
                .data(data)
                .build();
        return ServerResponse.status(HttpStatus.valueOf(messagesEnum.getCode()))
                .bodyValue(body);
    }

    public static Mono<ServerResponse> error(MessagesEnum messagesEnum) {
        ResponseBuilder body = ResponseBuilder.builder()
                .code(messagesEnum.getOperationCode())
                .message(messagesEnum.getMessage())
                .data("")
                .build();
        return ServerResponse.status(HttpStatus.valueOf(messagesEnum.getCode()))
                .bodyValue(body);
    }

    public static Mono<ServerResponse> handleError(Throwable err) {
        var knownError = "ERROR [%s]: %s";
        if (err instanceof CustomException cEx) {
            var knownMessage = MessagesEnum.findByOpCode(cEx.getOpcode());
            log.error(String.format(knownError, cEx.getClientCode(), knownMessage.getMessage()), err);
            return ResponseBuilder.error(knownMessage);
        }

        log.error("ERROR [UNKNOWN]", err);
        return ResponseBuilder.error(MessagesEnum.UNKNOWN_ERROR);
    }
}
