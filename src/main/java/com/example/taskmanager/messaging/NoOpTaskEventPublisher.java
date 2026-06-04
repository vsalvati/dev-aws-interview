package com.example.taskmanager.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class NoOpTaskEventPublisher implements TaskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpTaskEventPublisher.class);

    @Override
    public void publish(TaskEvent event) {
        log.info("SQS disabled (local profile). Event: {}", event);
    }
}
