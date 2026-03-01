package com.waitlist.controller;

import com.waitlist.dto.AuthResponse;
import com.waitlist.dto.ForgotPasswordRequest;
import com.waitlist.dto.LoginRequest;
import com.waitlist.dto.ResetPasswordRequest;
import com.waitlist.dto.RegisterRequest;
import com.waitlist.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @Test
    public void register_shouldReturnResponseFromService() {
        RegisterRequest req = new RegisterRequest();
        AuthResponse resp = new AuthResponse();
        when(authService.register(req)).thenReturn(resp);

        ResponseEntity<AuthResponse> result = controller.register(req);
        assertSame(resp, result.getBody());
    }

    @Test
    public void login_shouldReturnResponseFromService() {
        LoginRequest req = new LoginRequest();
        AuthResponse resp = new AuthResponse();
        when(authService.login(req)).thenReturn(resp);

        ResponseEntity<AuthResponse> result = controller.login(req);
        assertSame(resp, result.getBody());
    }

    @Test
    public void forgotPassword_shouldReturnMap() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        when(authService.forgotPassword(req)).thenReturn("ok");

        ResponseEntity<Map<String, String>> res = controller.forgotPassword(req);
        assertEquals("ok", res.getBody().get("message"));
    }

    @Test
    public void resetPassword_shouldReturnMap() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("e");
        req.setToken("t");
        req.setNewPassword("p");
        when(authService.resetPassword("e", "t", "p")).thenReturn("done");

        ResponseEntity<Map<String, String>> res = controller.resetPassword(req);
        assertEquals("done", res.getBody().get("message"));
    }
}
