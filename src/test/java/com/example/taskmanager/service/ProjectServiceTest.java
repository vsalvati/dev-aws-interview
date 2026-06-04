package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.UpdateProjectRequest;
import com.example.taskmanager.dto.response.ProjectResponse;
import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.ProjectStatus;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_shouldSaveWithActiveStatus() {
        var request = new CreateProjectRequest("My Project", "Description");
        var project = new Project("My Project", "Description", ProjectStatus.ACTIVE);
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectResponse response = projectService.createProject(request);

        assertThat(response.name()).isEqualTo("My Project");
        assertThat(response.status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void getProject_whenNotFound_shouldThrow() {
        var id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listProjects_withStatusFilter_shouldDelegateToRepo() {
        var pageable = PageRequest.of(0, 20);
        var project = new Project("P1", "Desc", ProjectStatus.ACTIVE);
        when(projectRepository.findByStatus(ProjectStatus.ACTIVE, pageable))
            .thenReturn(new PageImpl<>(List.of(project)));

        var page = projectService.listProjects(ProjectStatus.ACTIVE, pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(projectRepository).findByStatus(ProjectStatus.ACTIVE, pageable);
    }

    @Test
    void listProjects_withoutFilter_shouldReturnAll() {
        var pageable = PageRequest.of(0, 20);
        when(projectRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        projectService.listProjects(null, pageable);

        verify(projectRepository).findAll(pageable);
    }

    @Test
    void updateProject_shouldUpdateFields() {
        var id = UUID.randomUUID();
        var project = new Project("Old", "Old desc", ProjectStatus.ACTIVE);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var request = new UpdateProjectRequest("New", "New desc", ProjectStatus.ARCHIVED);
        ProjectResponse response = projectService.updateProject(id, request);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void addMember_shouldAddUserToProject() {
        var projectId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var project = new Project("P1", "Desc", ProjectStatus.ACTIVE);
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userService.findUserOrThrow(userId)).thenReturn(user);
        when(projectRepository.save(project)).thenReturn(project);

        projectService.addMember(projectId, userId);

        assertThat(project.getMembers()).contains(user);
    }
}
