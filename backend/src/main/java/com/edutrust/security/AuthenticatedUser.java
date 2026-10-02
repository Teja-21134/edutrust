package com.edutrust.security;

import com.edutrust.database.UserRole;

public record AuthenticatedUser(String name, String email, UserRole role) {
}
