package com.crunchycode.bookstore.controller;

import com.crunchycode.bookstore.dto.AuthorCreateRequest;
import com.crunchycode.bookstore.dto.BookCreateRequest;
import com.crunchycode.bookstore.dto.LoginRequest;
import com.crunchycode.bookstore.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityAndIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setupTokens() throws Exception {
        // Register & login admin
        RegisterRequest adminReg = new RegisterRequest("admin_test", "admintest@crunchy.com", "adminpass", "ROLE_ADMIN");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminReg)));

        String loginAdminRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin_test", "adminpass"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(loginAdminRes).get("token").asText();

        // Register & login regular user
        RegisterRequest userReg = new RegisterRequest("user_test", "usertest@crunchy.com", "userpass", "ROLE_USER");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userReg)));

        String loginUserRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user_test", "userpass"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        userToken = objectMapper.readTree(loginUserRes).get("token").asText();
    }

    // --- Happy Paths ---

    @Test
    void testPublicGetEndpointsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/books?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/authors?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testAdminCanCreateAuthorAndBook() throws Exception {
        AuthorCreateRequest authorReq = new AuthorCreateRequest("Arthur Conan Doyle", "doyle@detective.uk", "Sherlock Holmes Creator");
        String authorRes = mockMvc.perform(post("/api/v1/authors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorReq)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/authors/")))
                .andExpect(jsonPath("$.name").value("Arthur Conan Doyle"))
                .andReturn().getResponse().getContentAsString();

        Long authorId = objectMapper.readTree(authorRes).get("id").asLong();

        BookCreateRequest bookReq = new BookCreateRequest("A Study in Scarlet", "9781503252516", new BigDecimal("11.50"), 1887, authorId, Set.of());
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/books/")))
                .andExpect(jsonPath("$.title").value("A Study in Scarlet"));
    }

    @Test
    void testUserCanCreateBook() throws Exception {
        AuthorCreateRequest authorReq = new AuthorCreateRequest("Leo Tolstoy", "tolstoy@russia.org", "Russian Writer");
        String authorRes = mockMvc.perform(post("/api/v1/authors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorReq)))
                .andReturn().getResponse().getContentAsString();
        Long authorId = objectMapper.readTree(authorRes).get("id").asLong();

        BookCreateRequest bookReq = new BookCreateRequest("War and Peace", "9781400079988", new BigDecimal("24.00"), 1869, authorId, Set.of());
        // ROLE_USER is allowed to create books
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isCreated());
    }

    // --- 401 Unauthorized Failure Cases ---

    @Test
    void test401UnauthorizedWhenCreatingWithoutToken() throws Exception {
        AuthorCreateRequest authorReq = new AuthorCreateRequest("Ghost Author", "ghost@example.com", "Bio");
        mockMvc.perform(post("/api/v1/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void test401UnauthorizedWhenDeletingWithoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/books/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // --- 403 Forbidden Failure Cases (Different Role Permissions) ---

    @Test
    void test403ForbiddenWhenRegularUserTriesToCreateAuthor() throws Exception {
        // Only ROLE_ADMIN can create authors; ROLE_USER must get 403
        AuthorCreateRequest authorReq = new AuthorCreateRequest("H.G. Wells", "wells@time.org", "Sci-Fi Pioneer");
        mockMvc.perform(post("/api/v1/authors")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void test403ForbiddenWhenRegularUserTriesToDeleteBook() throws Exception {
        // Only ROLE_ADMIN can delete books; ROLE_USER must get 403
        mockMvc.perform(delete("/api/v1/books/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void test403ForbiddenWhenRegularUserTriesToDeleteAuthor() throws Exception {
        mockMvc.perform(delete("/api/v1/authors/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // --- Validation & Global Exception Handling ---

    @Test
    void test400BadRequestValidationPayload() throws Exception {
        AuthorCreateRequest invalidReq = new AuthorCreateRequest("", "bad-email", "Bio");
        mockMvc.perform(post("/api/v1/authors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void test404NotFoundReturnsStandardErrorShape() throws Exception {
        mockMvc.perform(get("/api/v1/books/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // --- Task 5: OpenAPI / Swagger & Actuator Public Access ---

    @Test
    void testRootWebLandingPageIsPubliclyReachable() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testOpenApiDocsAndSwaggerUiArePubliclyReachable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Crunchy Code Bookstore REST API"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testActuatorHealthEndpointIsPubliclyReachable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testBenchmarkEndpointReturnsTelemetry() throws Exception {
        mockMvc.perform(get("/api/v1/benchmark/run?iterations=10&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetsRequirement").value(true))
                .andExpect(jsonPath("$.meanLatencyMs").isNumber())
                .andExpect(jsonPath("$.targetThresholdMs").value(500.0));
    }
}
