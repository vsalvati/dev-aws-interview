package com.example.taskmanager.messaging;

public interface TaskEventPublisher {

    void publish(TaskEvent event);
}
