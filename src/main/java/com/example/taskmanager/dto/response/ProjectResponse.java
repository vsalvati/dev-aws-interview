package com.example.taskmanager.dto.response;

import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String name,
    String description,
    ProjectStatus status,
    int memberCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getStatus(),
            project.getMembers().size(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
