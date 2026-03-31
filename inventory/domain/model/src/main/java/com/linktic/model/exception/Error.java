package com.linktic.model.exception;

import java.time.LocalDateTime;
import java.util.List;

public record Error(String code, String message, LocalDateTime timestamp, List<String> details) {
}
