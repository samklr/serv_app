package com.servantin.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servantin.api.BaseIntegrationTest;
import com.servantin.api.dto.auth.LoginRequest;
import com.servantin.api.dto.auth.RegisterRequest;
import com.servantin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldRegisterUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@test.ch");
        request.setPassword("password123");
        request.setName("New User");
        request.setPhone("0791234567");
        request.setRegisterAsProvider(false);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("newuser@test.ch"));

        assertTrue(userRepository.findByEmail("newuser@test.ch").isPresent());
    }

    @Test
    @DisplayName("Should fail to register with existing email")
    void shouldFailRegisterExistingEmail() throws Exception {
        // Register first user
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@test.ch");
        request.setPassword("password123");
        request.setName("Existing User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Try to register again with same email
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        // Register user directly or via service
        RegisterRequest register = new RegisterRequest();
        register.setEmail("login@test.ch");
        register.setPassword("password123");
        register.setName("Login User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk());

        // Login
        LoginRequest login = new LoginRequest();
        login.setEmail("login@test.ch");
        login.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("login@test.ch"));
    }

    @Test
    @DisplayName("Should fail login with wrong password")
    void shouldFailLoginWrongPassword() throws Exception {
        // Register user
        RegisterRequest register = new RegisterRequest();
        register.setEmail("wrongpass@test.ch");
        register.setPassword("password123");
        register.setName("Wrong Pass User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk());

        // Login with wrong password
        LoginRequest login = new LoginRequest();
        login.setEmail("wrongpass@test.ch");
        login.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized()); // Assuming 401 for bad credentials
    }
}
