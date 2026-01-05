package com.servantin.api.dto.provider;

import com.servantin.api.domain.model.PricingType;
import com.servantin.api.domain.model.TimeSlot;
import com.servantin.api.dto.category.CategoryDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProviderProfileDto {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private String bio;
    private String photoUrl;
    private List<String> languages;
    private Boolean isVerified;
    private String verificationNotes;
    private Integer responseTimeMinutes;
    private Double averageRating;
    private Long ratingCount;
    private Instant createdAt;

    private List<CategoryDto> categories;
    private List<LocationDto> locations;
    private List<AvailabilityDto> availabilities;
    private List<PricingDto> pricings;

    @Data
    @Builder
    public static class LocationDto {
        private UUID id;
        private String postalCode;
        private String city;
        private String canton;
    }

    @Data
    @Builder
    public static class AvailabilityDto {
        private UUID id;
        private Integer weekday;
        private TimeSlot timeSlot;
    }

    @Data
    @Builder
    public static class PricingDto {
        private UUID id;
        private UUID categoryId;
        private String categoryName;
        private PricingType pricingType;
        private BigDecimal hourlyRate;
        private BigDecimal fixedPrice;
        private BigDecimal minHours;
        private String currency;
    }
}
