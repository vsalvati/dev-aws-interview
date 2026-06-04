package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
    @NotNull TaskStatus status
) {}
