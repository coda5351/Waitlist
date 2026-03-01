package com.waitlist.controller;

import com.waitlist.dto.CreateUserRequest;
import com.waitlist.dto.UpdateUserRequest;
import com.waitlist.model.User;
import com.waitlist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(required = false) User.UserRole role,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email) {
        
        // If any filter is provided, use the filtering method
        if (role != null || fullName != null || email != null) {
            List<User> users = userService.getUsersByFilters(role, fullName, email);
            return ResponseEntity.ok(users);
        }
        
        // Otherwise return all users
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<User.UserRole>> getUserRoles() {
        List<User.UserRole> roles = Arrays.asList(User.UserRole.values());
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }
}
