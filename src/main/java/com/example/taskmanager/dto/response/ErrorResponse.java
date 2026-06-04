package com.example.taskmanager.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    int status,
    String message,
    Instant timestamp,
    List<String> errors
) {
    public ErrorResponse(int status, String message) {
        this(status, message, Instant.now(), List.of());
    }

    public ErrorResponse(int status, String message, List<String> errors) {
        this(status, message, Instant.now(), errors);
    }
}
