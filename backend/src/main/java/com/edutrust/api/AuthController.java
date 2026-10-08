package com.edutrust.api;

import com.edutrust.database.AppUser;
import com.edutrust.database.UserRepository;
import com.edutrust.security.AuthenticatedUser;
import com.edutrust.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Pattern NORMALIZED_EMAIL = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            JwtService jwtService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .filter(candidate -> candidate.getPasswordHash() != null
                        && passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        return tokenResponse(user);
    }

    @PostMapping("/auth/email")
    public ResponseEntity<?> emailLogin(@RequestBody EmailLoginRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        if (!NORMALIZED_EMAIL.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required");
        }

        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                userRepository.save(new AppUser(
                        UUID.randomUUID(), "Student User", email, null,
                        com.edutrust.database.UserRole.STUDENT)));
        if (user.getRole() == com.edutrust.database.UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Passwordless login is only available for normal users");
        }
        return tokenResponse(user);
    }

    @GetMapping("/me")
    public AuthenticatedUser me(@AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }

    @GetMapping("/admin/ping")
    public Map<String, String> adminPing() {
        return Map.of("message", "admin pong");
    }

    private ResponseEntity<Map<String, String>> tokenResponse(AppUser user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getName(), user.getEmail(), user.getRole());
        return ResponseEntity.ok(Map.of(
                "token", jwtService.createToken(authenticatedUser),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record EmailLoginRequest(String email) {
    }
}
