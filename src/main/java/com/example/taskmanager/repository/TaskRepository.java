package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByProjectId(UUID projectId, Pageable pageable);

    @Query("""
        SELECT t FROM Task t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        """)
    Page<Task> searchTasks(
        @Param("status") TaskStatus status,
        @Param("priority") TaskPriority priority,
        @Param("assigneeId") UUID assigneeId,
        Pageable pageable
    );
}
