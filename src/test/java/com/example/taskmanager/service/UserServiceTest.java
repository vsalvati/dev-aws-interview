package com.example.taskmanager.service;

import com.example.taskmanager.dto.request.CreateUserRequest;
import com.example.taskmanager.dto.request.UpdateUserRequest;
import com.example.taskmanager.dto.response.UserResponse;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldSaveAndReturnResponse() {
        var request = new CreateUserRequest("john", "john@example.com", UserRole.MEMBER);
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.createUser(request);

        assertThat(response.username()).isEqualTo("john");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.role()).isEqualTo(UserRole.MEMBER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUser_whenExists_shouldReturnResponse() {
        var id = UUID.randomUUID();
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUser(id);

        assertThat(response.username()).isEqualTo("john");
    }

    @Test
    void getUser_whenNotFound_shouldThrow() {
        var id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    void listUsers_shouldReturnPage() {
        var pageable = PageRequest.of(0, 20);
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> page = userService.listUsers(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().username()).isEqualTo("john");
    }

    @Test
    void updateUser_whenExists_shouldUpdateAndReturn() {
        var id = UUID.randomUUID();
        var user = new User("john", "john@example.com", UserRole.MEMBER);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var request = new UpdateUserRequest("jane", "jane@example.com", UserRole.ADMIN);
        UserResponse response = userService.updateUser(id, request);

        assertThat(response.username()).isEqualTo("jane");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void deleteUser_whenExists_shouldDelete() {
        var id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        userService.deleteUser(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void deleteUser_whenNotFound_shouldThrow() {
        var id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
