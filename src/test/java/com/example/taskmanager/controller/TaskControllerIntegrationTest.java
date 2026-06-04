package com.example.taskmanager.controller;

import com.example.taskmanager.BaseIntegrationTest;
import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.CreateTaskRequest;
import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.dto.request.UpdateTaskStatusRequest;
import com.example.taskmanager.entity.TaskPriority;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.ProjectRepository;
import com.example.taskmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TaskControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;

    private String projectId;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Create a project to use in tests
        var projectRequest = new CreateProjectRequest("Test Project", "For testing");
        MvcResult result = mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projectRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        projectId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void createTask_shouldReturn201() throws Exception {
        var request = new CreateTaskRequest("Build feature", "Build the thing",
            TaskPriority.HIGH, LocalDate.of(2026, 7, 15));

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Build feature"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.projectId").value(projectId));
    }

    @Test
    void updateTaskStatus_shouldChangeStatusAndReturn200() throws Exception {
        var createRequest = new CreateTaskRequest("Task 1", "Desc", TaskPriority.MEDIUM, null);
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        var statusRequest = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void searchTasks_withFilters_shouldReturnFiltered() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateTaskRequest("High task", "Desc", TaskPriority.HIGH, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateTaskRequest("Low task", "Desc", TaskPriority.LOW, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks?priority=HIGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("High task"));
    }

    @Test
    void listProjectTasks_shouldReturnOnlyProjectTasks() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new CreateTaskRequest("My task", "Desc", TaskPriority.MEDIUM, null))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("My task"));
    }
}
