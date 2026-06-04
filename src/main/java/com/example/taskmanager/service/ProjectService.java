package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateProjectRequest;
import com.example.taskmanager.dto.request.UpdateProjectRequest;
import com.example.taskmanager.dto.response.ProjectResponse;
import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.ProjectStatus;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        var project = new Project(request.name(), request.description(), ProjectStatus.ACTIVE);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID id) {
        return ProjectResponse.from(findProjectOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(ProjectStatus status, Pageable pageable) {
        Page<Project> page = (status != null)
            ? projectRepository.findByStatus(status, pageable)
            : projectRepository.findAll(pageable);
        return page.map(ProjectResponse::from);
    }

    public ProjectResponse updateProject(UUID id, UpdateProjectRequest request) {
        var project = findProjectOrThrow(id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(request.status());
        return ProjectResponse.from(projectRepository.save(project));
    }

    public void deleteProject(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        }
        projectRepository.deleteById(id);
    }

    public void addMember(UUID projectId, UUID userId) {
        var project = findProjectOrThrow(projectId);
        var user = userService.findUserOrThrow(userId);
        project.addMember(user);
        projectRepository.save(project);
    }

    public void removeMember(UUID projectId, UUID userId) {
        var project = findProjectOrThrow(projectId);
        var user = userService.findUserOrThrow(userId);
        project.removeMember(user);
        projectRepository.save(project);
    }

    Project findProjectOrThrow(UUID id) {
        return projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
