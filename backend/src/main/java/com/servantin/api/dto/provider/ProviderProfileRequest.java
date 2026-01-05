package com.servantin.api.dto.provider;

import com.servantin.api.domain.model.PricingType;
import com.servantin.api.domain.model.TimeSlot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProviderProfileRequest {

    private String bio;
    private String photoUrl;

    @NotEmpty(message = "At least one language is required")
    private List<String> languages;

    @NotEmpty(message = "At least one category is required")
    private List<UUID> categoryIds;

    @NotEmpty(message = "At least one location is required")
    private List<LocationDto> locations;

    private List<AvailabilityDto> availabilities;

    private List<PricingDto> pricings;

    @Data
    public static class LocationDto {
        @NotBlank
        private String postalCode;
        @NotBlank
        private String city;
        private String canton = "JU";
    }

    @Data
    public static class AvailabilityDto {
        private Integer weekday; // 0-6
        private TimeSlot timeSlot;
    }

    @Data
    public static class PricingDto {
        private UUID categoryId;
        private PricingType pricingType;
        private BigDecimal hourlyRate;
        private BigDecimal fixedPrice;
        private BigDecimal minHours;
    }
}
