package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.AssignTaskRequest;
import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.UpdateTaskStatusRequest;
import com.example.taskmanager.dto.response.TaskResponse;
import com.example.taskmanager.entity.*;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.messaging.TaskEvent;
import com.example.taskmanager.messaging.TaskEventPublisher;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectService projectService;
    @Mock private UserService userService;
    @Mock private TaskEventPublisher eventPublisher;

    @InjectMocks
    private TaskService taskService;

    private Project sampleProject() {
        return new Project("Test Project", "Desc", ProjectStatus.ACTIVE);
    }

    private User sampleUser() {
        return new User("john", "john@example.com", UserRole.MEMBER);
    }

    @Test
    void createTask_shouldCreateWithTodoStatus() {
        var projectId = UUID.randomUUID();
        var project = sampleProject();
        when(projectService.findProjectOrThrow(projectId)).thenReturn(project);

        var request = new CreateTaskRequest("My Task", "Description", TaskPriority.HIGH, LocalDate.of(2026, 7, 1));
        var task = new Task("My Task", "Description", TaskStatus.TODO, TaskPriority.HIGH,
            LocalDate.of(2026, 7, 1), project);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService.createTask(projectId, request);

        assertThat(response.title()).isEqualTo("My Task");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void updateTaskStatus_shouldPublishEvent() {
        var taskId = UUID.randomUUID();
        var project = sampleProject();
        var task = new Task("Task", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, null, project);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        var request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);
        taskService.updateTaskStatus(taskId, request);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

        var captor = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TASK_STATUS_CHANGED");
        assertThat(captor.getValue().oldStatus()).isEqualTo("TODO");
        assertThat(captor.getValue().newStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void assignTask_shouldSetAssigneeAndPublishEvent() {
        var taskId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var project = sampleProject();
        var user = sampleUser();
        var task = new Task("Task", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, null, project);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(taskRepository.save(task)).thenReturn(task);

        taskService.assignTask(taskId, new AssignTaskRequest(userId));

        assertThat(task.getAssignee()).isEqualTo(user);

        var captor = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("TASK_ASSIGNED");
    }

    @Test
    void assignTask_withNullAssigneeId_shouldUnassign() {
        var taskId = UUID.randomUUID();
        var project = sampleProject();
        var task = new Task("Task", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, null, project);
        task.setAssignee(sampleUser());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.assignTask(taskId, new AssignTaskRequest(null));

        assertThat(task.getAssignee()).isNull();
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void getTask_whenNotFound_shouldThrow() {
        var id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
