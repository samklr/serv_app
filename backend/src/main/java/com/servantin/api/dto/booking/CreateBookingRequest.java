package com.servantin.api.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class CreateBookingRequest {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    private UUID providerId; // Optional - client may not have selected yet

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "City is required")
    private String city;

    private String addressText;

    private Instant scheduledAt;

    private String urgency; // "flexible", "within_week", "urgent"

    private BigDecimal budgetMin;

    private BigDecimal budgetMax;
}
