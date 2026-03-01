package com.waitlist.controller;

import com.waitlist.dto.CreateUserRequest;
import com.waitlist.dto.UpdateUserRequest;
import com.waitlist.model.User;
import com.waitlist.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    @Test
    void getAllUsers_noFilters_delegates() {
        List<User> users = List.of(new User());
        when(userService.getAllUsers()).thenReturn(users);
        ResponseEntity<List<User>> res = controller.getAllUsers(null, null, null);
        assertSame(users, res.getBody());
    }

    @Test
    void getAllUsers_withFilters_usesFilterMethod() {
        List<User> users = List.of(new User());
        when(userService.getUsersByFilters(User.UserRole.USER, "f", "e")).thenReturn(users);
        ResponseEntity<List<User>> res = controller.getAllUsers(User.UserRole.USER, "f", "e");
        assertSame(users, res.getBody());
    }

    @Test
    void getUserById_returnsUser() {
        User u = new User();
        when(userService.getUserById(5L)).thenReturn(u);
        ResponseEntity<User> res = controller.getUserById(5L);
        assertSame(u, res.getBody());
    }

    @Test
    void createUser_returnsCreated() {
        CreateUserRequest req = new CreateUserRequest();
        User u = new User();
        when(userService.createUser(req)).thenReturn(u);
        ResponseEntity<User> res = controller.createUser(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertSame(u, res.getBody());
    }

    @Test
    void updateUser_returnsOk() {
        UpdateUserRequest req = new UpdateUserRequest();
        User u = new User();
        when(userService.updateUser(7L, req)).thenReturn(u);
        ResponseEntity<User> res = controller.updateUser(7L, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertSame(u, res.getBody());
    }

    @Test
    void getUserRoles_returnsAllRoles() {
        ResponseEntity<List<User.UserRole>> res = controller.getUserRoles();
        assertTrue(res.getBody().containsAll(List.of(User.UserRole.values())));
    }
}
