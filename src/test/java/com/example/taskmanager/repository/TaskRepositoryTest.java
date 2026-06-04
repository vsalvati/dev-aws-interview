package com.example.taskmanager.repository;

import com.example.taskmanager.BaseIntegrationTest;
import com.example.taskmanager.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRepositoryTest extends BaseIntegrationTest {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;

    private Project project;
    private User user;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("john", "john@example.com", UserRole.MEMBER));
        project = projectRepository.save(new Project("Test", "Desc", ProjectStatus.ACTIVE));
    }

    @Test
    void searchTasks_byStatus_shouldReturnMatching() {
        var todo = new Task("Todo task", "Desc", TaskStatus.TODO, TaskPriority.LOW, null, project);
        var done = new Task("Done task", "Desc", TaskStatus.DONE, TaskPriority.LOW, null, project);
        taskRepository.save(todo);
        taskRepository.save(done);

        var page = taskRepository.searchTasks(TaskStatus.TODO, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Todo task");
    }

    @Test
    void searchTasks_byAssignee_shouldReturnMatching() {
        var assigned = new Task("Assigned", "Desc", TaskStatus.TODO, TaskPriority.HIGH, null, project);
        assigned.setAssignee(user);
        var unassigned = new Task("Unassigned", "Desc", TaskStatus.TODO, TaskPriority.LOW, null, project);
        taskRepository.save(assigned);
        taskRepository.save(unassigned);

        var page = taskRepository.searchTasks(null, null, user.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Assigned");
    }

    @Test
    void searchTasks_withNoFilters_shouldReturnAll() {
        taskRepository.save(new Task("T1", "Desc", TaskStatus.TODO, TaskPriority.LOW, null, project));
        taskRepository.save(new Task("T2", "Desc", TaskStatus.DONE, TaskPriority.HIGH, null, project));

        var page = taskRepository.searchTasks(null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
    }
}
