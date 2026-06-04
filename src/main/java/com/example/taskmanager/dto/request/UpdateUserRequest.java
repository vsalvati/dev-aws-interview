package com.example.taskmanager.dto.request;

import com.example.taskmanager.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Email @Size(max = 100) String email,
    @NotNull UserRole role
) {}
