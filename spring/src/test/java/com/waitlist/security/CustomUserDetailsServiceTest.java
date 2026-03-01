package com.waitlist.security;

import com.waitlist.model.User;
import com.waitlist.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_shouldReturnSpringUser_whenFound() {
        User user = new User("bob", "bob@example.com", "pw", "Bob");
        user.setRole(User.UserRole.ADMIN);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetails ud = service.loadUserByUsername("bob");
        assertEquals("bob", ud.getUsername());
        assertEquals("pw", ud.getPassword());
        assertTrue(ud.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_shouldThrow_whenNotFound() {
        when(userRepository.findByUsername("x")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("x"));
    }
}
