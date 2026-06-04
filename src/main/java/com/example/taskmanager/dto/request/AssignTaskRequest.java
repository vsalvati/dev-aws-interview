package com.example.taskmanager.dto.request;

import java.util.UUID;

public record AssignTaskRequest(
    UUID assigneeId
) {}
