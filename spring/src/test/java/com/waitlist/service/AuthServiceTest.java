package com.waitlist.service;

import com.waitlist.dto.AuthResponse;
import com.waitlist.dto.ForgotPasswordRequest;
import com.waitlist.exception.UnauthorizedException;
import com.waitlist.model.User;
import com.waitlist.repository.AccountRepository;
import com.waitlist.repository.UserRepository;
import com.waitlist.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setupEncoder() {
        // the tests don't always call the encoder; make the stubbing lenient so
        // Mockito doesn't complain about "unnecessary stubbings" when a particular
        // test doesn't exercise encode/matches (see previous build failure).
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "ENC:" + invocation.getArgument(0));
        lenient().when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(invocation -> {
            String raw = invocation.getArgument(0);
            String encoded = invocation.getArgument(1);
            return encoded.equals("ENC:" + raw);
        });

        // also ensure frontendUrl is set so we don't hit NPE when building reset links
        try {
            Field f = AuthService.class.getDeclaredField("frontendUrl");
            f.setAccessible(true);
            f.set(authService, "http://localhost:5173");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void resetPassword_shouldUpdatePassword_whenTokenIsValid() throws Exception {
        User user = new User("bob", "bob@example.com", "oldpw", "Bob");
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        // request forgot password to generate token
        ForgotPasswordRequest forgot = new ForgotPasswordRequest();
        forgot.setEmail("bob@example.com");
        authService.forgotPassword(forgot);

        // grab token from internal map via reflection (test only)
        Field field = AuthService.class.getDeclaredField("resetTokens");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> tokens = (Map<String, String>) field.get(authService);
        String token = tokens.get("bob@example.com");
        assertNotNull(token, "token should have been generated");

        String response = authService.resetPassword("bob@example.com", token, "newpass");
        assertEquals("Password has been reset successfully.", response);

        verify(userRepository).save(argThat(new ArgumentMatcher<User>() {
            @Override
            public boolean matches(User u) {
                return passwordEncoder.matches("newpass", u.getPassword());
            }
        }));
        assertFalse(tokens.containsKey("bob@example.com"), "token should be cleared after reset");
    }

    @Test
    public void resetPassword_shouldThrow_whenTokenInvalid() {
        // don't bother stubbing the repository – the method throws before it looks up
        // the user when the token map contains no entry for the email.
        assertThrows(UnauthorizedException.class,
                () -> authService.resetPassword("foo@bar.com", "bad", "pw"));
    }

    // ------------------------------------------------------------------------
    // login behaviour tests
    // ------------------------------------------------------------------------

    @Test
    public void loginWithUsername_shouldReturnAuthResponse_whenCredentialsValid() {
        User user = new User("alice", "alice@exam.com", "pw123", "Alice");
        // password stored in the database will be encoded; the lenient stub in
        // setupEncoder expects the encoded value to start with "ENC:" so mimic that.
        user.setPassword("ENC:pw123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        // user id is null in these unit tests, so match with any()/any() rather
        // than anyLong() to avoid strict-stubbing complaints.
        when(jwtTokenProvider.generateToken(any(), any())).thenReturn("tok");

        AuthResponse resp = authService.login(new com.waitlist.dto.LoginRequest("alice", "pw123"));
        assertEquals("tok", resp.getToken());
        assertSame(user, resp.getUser());
    }

    @Test
    public void loginWithEmail_shouldReturnAuthResponse_whenCredentialsValid() {
        User user = new User("bob", "bob@exam.com", "pw123", "Bob");
        user.setPassword("ENC:pw123");

        when(userRepository.findByUsername("bob@exam.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("bob@exam.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(any(), any())).thenReturn("tok2");

        AuthResponse resp = authService.login(new com.waitlist.dto.LoginRequest("bob@exam.com", "pw123"));
        assertEquals("tok2", resp.getToken());
        assertSame(user, resp.getUser());
    }

    @Test
    public void login_shouldThrowUnauthorized_whenUserNotFoundOrPasswordWrong() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new com.waitlist.dto.LoginRequest("unknown", "pw")));

        User user = new User();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        // the password field in this user is null; the service will call
        // passwordEncoder.matches("wrong", null) – the anyString matcher doesn't
        // allow null, so use a more permissive matcher or lenient stub instead.
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        assertThrows(UnauthorizedException.class,
                () -> authService.login(new com.waitlist.dto.LoginRequest("user", "wrong")));
    }
}
