package com.example.taskmanager.repository;

import com.example.taskmanager.entity.Project;
import com.example.taskmanager.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);
}
