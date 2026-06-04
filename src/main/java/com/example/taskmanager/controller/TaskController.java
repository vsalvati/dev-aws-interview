package com.example.taskmanager.controller;

import com.example.taskmanager.dto.request.*;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable UUID projectId,
                                    @Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(projectId, request);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public Page<TaskResponse> listProjectTasks(
            @PathVariable UUID projectId,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.listTasksByProject(projectId, pageable);
    }

    @GetMapping("/tasks")
    public Page<TaskResponse> searchTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @PageableDefault(size = 20) Pageable pageable) {
        return taskService.searchTasks(status, priority, assigneeId, pageable);
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse getTask(@PathVariable UUID id) {
        return taskService.getTask(id);
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse updateTask(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskResponse updateTaskStatus(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateTaskStatus(id, request);
    }

    @PatchMapping("/tasks/{id}/assign")
    public TaskResponse assignTask(@PathVariable UUID id,
                                    @Valid @RequestBody AssignTaskRequest request) {
        return taskService.assignTask(id, request);
    }
}
