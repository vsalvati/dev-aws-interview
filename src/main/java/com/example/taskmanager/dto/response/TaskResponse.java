package com.example.taskmanager.dto.response;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueDate,
    UUID projectId,
    UUID assigneeId,
    String assigneeUsername,
    Instant createdAt,
    Instant updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate(),
            task.getProject().getId(),
            task.getAssignee() != null ? task.getAssignee().getId() : null,
            task.getAssignee() != null ? task.getAssignee().getUsername() : null,
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
