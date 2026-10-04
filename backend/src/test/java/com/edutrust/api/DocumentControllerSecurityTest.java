package com.edutrust.api;

import com.edutrust.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerSecurityTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.seed-users.admin-email}")
    private String adminEmail;

    @Value("${app.seed-users.admin-password}")
    private String adminPassword;

    @Value("${app.seed-users.student-email}")
    private String studentEmail;

    @Value("${app.seed-users.student-password}")
    private String studentPassword;

    @Test
    void studentGetsForbiddenForAllDocumentOperations() throws Exception {
        String studentToken = tokenFor(studentEmail, studentPassword);

        mockMvc.perform(get("/api/documents").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/documents/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListDocuments() throws Exception {
        String adminToken = tokenFor(adminEmail, adminPassword);

        mockMvc.perform(get("/api/documents").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String tokenFor(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.get("token").asText();
    }
}
