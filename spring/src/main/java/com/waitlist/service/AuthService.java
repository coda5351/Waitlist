package com.waitlist.service;

import com.waitlist.dto.AuthResponse;
import com.waitlist.dto.ForgotPasswordRequest;
import com.waitlist.dto.LoginRequest;
import com.waitlist.dto.RegisterRequest;
import com.waitlist.exception.UnauthorizedException;
import com.waitlist.model.Account;
import com.waitlist.model.CodeGenerator;
import com.waitlist.model.User;
import com.waitlist.repository.AccountRepository;
import com.waitlist.repository.UserRepository;
import com.waitlist.security.JwtTokenProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String[] CHINESE_ZODIAC = {
        "Rat", "Ox", "Tiger", "Rabbit", "Dragon", "Snake",
        "Horse", "Goat", "Monkey", "Rooster", "Dog", "Pig"
    };
    private static final Random random = new Random();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailService emailService;

    // simple in-memory store of reset tokens ; in production this would be persisted and expire
    private final Map<String,String> resetTokens = new ConcurrentHashMap<>();

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName()
        );

        user = userRepository.save(user);

        // No registration code - create a new account for this user and make them an admin
        String zodiacAnimal = CHINESE_ZODIAC[random.nextInt(CHINESE_ZODIAC.length)];
        Account account = new Account(zodiacAnimal, "green");
        account.ensureCode();
        account = accountRepository.save(account);
        user.setRole(User.UserRole.ADMIN);
    

        // Set the account on the user
        user.setAccount(account);
        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        // the UI currently sends a single identifier field called "username" but we
        // accept either a username or an email address so that clients can log in with
        // whichever they happen to remember.  This mirrors the behaviour of many
        // modern applications and avoids a common source of "401 Unauthorized" errors.
        String identifier = request.getUsername();

        User user = userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier)
                        .orElseThrow(() -> new UnauthorizedException("The login attempt failed, try again.")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // don't reveal whether the username/email was wrong or the password – keep it generic
            throw new UnauthorizedException("The login attempt failed, try again.");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, user);
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        if (request.getUsername() == null && request.getEmail() == null) {
            throw new IllegalArgumentException("Either username or email must be provided");
        }
        
        User user = null;
        
        if (request.getUsername() != null) {
            user = userRepository.findByUsername(request.getUsername())
                    .orElse(null);
        }
        
        if (user == null && request.getEmail() != null) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElse(null);
        }
        
        if (user == null) {
            // Log the failed attempt but don't leak that the user doesn't exist
            logger.warn("Password reset attempt for non-existent user. Username: {}, Email: {}", 
                       request.getUsername(), request.getEmail());
            // Return generic success message to prevent user enumeration
            return "If an account exists with the provided information, password reset instructions have been sent.";
        }
        
        // 1. generate a (very basic) reset token and build a link
        String token = Integer.toHexString(random.nextInt());
        // store token so we can validate it later (demo only)
        resetTokens.put(user.getEmail(), token);
        String resetLink = "http://localhost:5173/reset-password?token=" + token + "&email=" + user.getEmail();

        // 2. send via email (smtp4dev will capture it when running dev profile)
        String subject = "[Waitlist] Password reset request";
        String body = "Hello " + user.getFullName() + ",\n\n" +
                "We received a request to reset your password. " +
                "Visit the following link to choose a new password:\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this message.\n\n" +
                "Regards,\n" +
                "Waitlist Team";
        try {
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception ex) {
            logger.warn("Failed to send password reset email to {}: {}", user.getEmail(), ex.getMessage());
        }

        logger.info("Password reset requested for user: {}", user.getUsername());
        return "If an account exists with the provided information, password reset instructions have been sent.";
    }

    public String resetPassword(String email, String token, String newPassword) {
        // validate input
        if (email == null || token == null || newPassword == null) {
            throw new IllegalArgumentException("Email, token and new password must be provided");
        }

        String expectedToken = resetTokens.get(email);
        if (expectedToken == null || !expectedToken.equals(token)) {
            logger.warn("Invalid or missing reset token for {} (provided token={})", email, token);
            throw new UnauthorizedException("Invalid password reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found for email"));

        // update password and clear token
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokens.remove(email);

        logger.info("Password successfully reset for user: {}", user.getUsername());
        return "Password has been reset successfully.";
    }
}
