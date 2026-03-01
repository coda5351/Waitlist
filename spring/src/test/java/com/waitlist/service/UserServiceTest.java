package com.waitlist.service;

import com.waitlist.dto.CreateUserRequest;
import com.waitlist.dto.UpdateUserRequest;
import com.waitlist.exception.ResourceNotFoundException;
import com.waitlist.model.User;
import com.waitlist.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User existing;

    @BeforeEach
    void setUp() {
        existing = new User("jane", "jane@ex.com", "pw", "Jane");
        existing.setId(10L);
    }

    @Test
    void getAllUsers_shouldReturnList() {
        when(userRepository.findAll()).thenReturn(List.of(existing));
        List<User> list = userService.getAllUsers();
        assertEquals(1, list.size());
        assertSame(existing, list.get(0));
    }

    @Test
    void getUserById_shouldThrow_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void createUser_shouldEncodePassword_andSave() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("alice");
        req.setEmail("a@b.com");
        req.setPassword("secret");
        req.setFullName("Alice");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("ENC");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User saved = userService.createUser(req);
        assertEquals("alice", saved.getUsername());
        assertEquals("ENC", saved.getPassword());
    }

    @Test
    void createUser_shouldThrow_whenUsernameTaken() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("jane");
        when(userRepository.existsByUsername("jane")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> userService.createUser(req));
    }

    @Test
    void updateUser_shouldModifyFields() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("new@ex.com");
        req.setFullName("New Name");
        req.setPassword("newpw");
        req.setRole(User.UserRole.ADMIN);

        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@ex.com")).thenReturn(false);
        when(passwordEncoder.encode("newpw")).thenReturn("E2");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User updated = userService.updateUser(10L, req);
        assertEquals("new@ex.com", updated.getEmail());
        assertEquals("New Name", updated.getFullName());
        assertEquals("E2", updated.getPassword());
        assertEquals(User.UserRole.ADMIN, updated.getRole());
    }

    @Test
    void updateUser_shouldThrow_whenEmailConflict() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("taken@ex.com");
        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@ex.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(10L, req));
    }
}
