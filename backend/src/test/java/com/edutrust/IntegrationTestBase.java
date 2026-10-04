package com.edutrust;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Shared isolation and profile configuration for tests that load the Spring context. */
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTestDocuments() {
        jdbcTemplate.update("DELETE FROM chunks");
        jdbcTemplate.update("DELETE FROM documents");
    }
}
