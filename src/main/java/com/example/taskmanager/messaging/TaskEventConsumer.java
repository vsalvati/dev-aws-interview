package com.example.taskmanager.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);
    private final ObjectMapper objectMapper;

    public TaskEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SqsListener("${app.sqs.task-events-queue}")
    public void handleTaskEvent(String message) throws JsonProcessingException {
        TaskEvent event = objectMapper.readValue(message, TaskEvent.class);
        log.info("Received task event: type={}, taskId={}, projectId={}",
            event.eventType(), event.taskId(), event.projectId());

        switch (event.eventType()) {
            case "TASK_STATUS_CHANGED" -> log.info("Task {} status changed: {} -> {}",
                event.taskId(), event.oldStatus(), event.newStatus());
            case "TASK_ASSIGNED" -> log.info("Task {} assigned to user {}",
                event.taskId(), event.assigneeId());
            default -> log.warn("Unknown event type: {}", event.eventType());
        }
    }
}
