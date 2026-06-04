package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.*;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.messaging.TaskEvent;
import com.example.taskmanager.messaging.TaskEventPublisher;
import com.example.taskmanager.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final TaskEventPublisher eventPublisher;

    public TaskService(TaskRepository taskRepository, ProjectService projectService,
                       UserService userService, TaskEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public TaskResponse createTask(UUID projectId, CreateTaskRequest request) {
        var project = projectService.findProjectOrThrow(projectId);
        var task = new Task(
            request.title(), request.description(),
            TaskStatus.TODO, request.priority(),
            request.dueDate(), project
        );
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID id) {
        return TaskResponse.from(findTaskOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> listTasksByProject(UUID projectId, Pageable pageable) {
        return taskRepository.findByProjectId(projectId, pageable).map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasks(TaskStatus status, TaskPriority priority,
                                           UUID assigneeId, Pageable pageable) {
        return taskRepository.searchTasks(status, priority, assigneeId, pageable)
            .map(TaskResponse::from);
    }

    public TaskResponse updateTask(UUID id, UpdateTaskRequest request) {
        var task = findTaskOrThrow(id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse updateTaskStatus(UUID id, UpdateTaskStatusRequest request) {
        var task = findTaskOrThrow(id);
        String oldStatus = task.getStatus().name();
        task.setStatus(request.status());
        var saved = taskRepository.save(task);

        eventPublisher.publish(TaskEvent.statusChanged(
            task.getId(), task.getProject().getId(), oldStatus, request.status().name()
        ));

        return TaskResponse.from(saved);
    }

    public TaskResponse assignTask(UUID id, AssignTaskRequest request) {
        var task = findTaskOrThrow(id);

        if (request.assigneeId() != null) {
            var user = userService.findUserOrThrow(request.assigneeId());
            task.setAssignee(user);
            taskRepository.save(task);

            eventPublisher.publish(TaskEvent.assigned(
                task.getId(), task.getProject().getId(), user.getId()
            ));
        } else {
            task.setAssignee(null);
            taskRepository.save(task);
        }

        return TaskResponse.from(task);
    }

    public void deleteTask(UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id);
        }
        taskRepository.deleteById(id);
    }

    private Task findTaskOrThrow(UUID id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }
}
