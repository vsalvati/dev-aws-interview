package com.example.taskmanager.messaging;

import java.time.Instant;
import java.util.UUID;

public record TaskEvent(
    String eventType,
    UUID taskId,
    UUID projectId,
    UUID assigneeId,
    String oldStatus,
    String newStatus,
    Instant timestamp
) {
    public static TaskEvent statusChanged(UUID taskId, UUID projectId, String oldStatus, String newStatus) {
        return new TaskEvent("TASK_STATUS_CHANGED", taskId, projectId, null, oldStatus, newStatus, Instant.now());
    }

    public static TaskEvent assigned(UUID taskId, UUID projectId, UUID assigneeId) {
        return new TaskEvent("TASK_ASSIGNED", taskId, projectId, assigneeId, null, null, Instant.now());
    }
}
