package com.servantin.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servantin.api.BaseIntegrationTest;
import com.servantin.api.domain.entity.Category;
import com.servantin.api.dto.auth.RegisterRequest;
import com.servantin.api.dto.booking.CreateBookingRequest;
import com.servantin.api.repository.CategoryRepository;
import com.servantin.api.repository.ProviderProfileRepository;
import com.servantin.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BookingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProviderProfileRepository providerProfileRepository;

    @BeforeEach
    void setUp() {
        // Clean up data
        providerProfileRepository.deleteAll(); // Because of FK constraints, delete profile first (if not cascaded
                                               // properly)
        // Actually easiest is usually to let @Transactional handle rollback if using
        // that,
        // but Integration Tests with actual HTTP calls commit the transaction unless we
        // use @Transactional on the test method.
        // SpringBootTest + MockMvc usually allows @Transactional.
        // But for safety with cleaning up between tests:
        userRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    private String getAuthToken(String email, String password, boolean provider) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setName("User " + email);
        request.setRegisterAsProvider(provider);

        String result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(result);
        return root.get("token").asText();
    }

    @Test
    @DisplayName("Should successfully create a booking")
    void shouldCreateBooking() throws Exception {
        // 1. Setup Data
        String clientToken = getAuthToken("client@test.com", "password123", false);

        // Create a category
        Category category = Category.builder()
                .slug("cleaning")
                .name("Cleaning")
                .description("House cleaning")
                .icon("broom")
                .sortOrder(1)
                .build();
        category = categoryRepository.save(category);

        // 2. Prepare Request
        CreateBookingRequest bookingRequest = new CreateBookingRequest();
        bookingRequest.setCategoryId(category.getId());
        bookingRequest.setDescription("Clean my house please");
        bookingRequest.setPostalCode("1000");
        bookingRequest.setCity("Lausanne");
        bookingRequest.setScheduledAt(Instant.now().plus(2, ChronoUnit.DAYS));
        bookingRequest.setUrgency("within_week");
        bookingRequest.setBudgetMin(new BigDecimal("50"));
        bookingRequest.setBudgetMax(new BigDecimal("100"));

        // 3. Execute
        mockMvc.perform(post("/api/bookings")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk()) // Actually returns 200 OK
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("REQUESTED")) // Initial status
                .andExpect(jsonPath("$.description").value("Clean my house please"));
    }

    @Test
    @DisplayName("Should fail to create booking without auth")
    void shouldFailCreateBookingNoAuth() throws Exception {
        CreateBookingRequest bookingRequest = new CreateBookingRequest();
        bookingRequest.setCategoryId(UUID.randomUUID());
        bookingRequest.setDescription("Test");

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isForbidden()); // or isUnauthorized
    }
}
