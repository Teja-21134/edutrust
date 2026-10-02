package com.edutrust.config;

import com.edutrust.database.AppUser;
import com.edutrust.database.UserRepository;
import com.edutrust.database.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;
    private final String studentName;
    private final String studentEmail;
    private final String studentPassword;

    public UserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed-users.admin-name}") String adminName,
            @Value("${app.seed-users.admin-email}") String adminEmail,
            @Value("${app.seed-users.admin-password}") String adminPassword,
            @Value("${app.seed-users.student-name}") String studentName,
            @Value("${app.seed-users.student-email}") String studentEmail,
            @Value("${app.seed-users.student-password}") String studentPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.studentPassword = studentPassword;
    }

    @Override
    public void run(String... args) {
        seedUser(adminName, adminEmail, adminPassword, UserRole.ADMIN);
        seedUser(studentName, studentEmail, studentPassword, UserRole.STUDENT);
    }

    private void seedUser(String name, String email, String password, UserRole role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(new AppUser(
                    UUID.randomUUID(), name, email, passwordEncoder.encode(password), role));
        }
    }
}
