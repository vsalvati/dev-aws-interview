package com.example.taskmanager.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class SqsTaskEventPublisher implements TaskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SqsTaskEventPublisher.class);

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public SqsTaskEventPublisher(SqsTemplate sqsTemplate, ObjectMapper objectMapper,
                                  @Value("${app.sqs.task-events-queue}") String queueName) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    @Override
    public void publish(TaskEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            sqsTemplate.send(queueName, message);
            log.info("Published task event to SQS: {}", event.eventType());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task event", e);
            throw new RuntimeException("Failed to serialize task event", e);
        }
    }
}
