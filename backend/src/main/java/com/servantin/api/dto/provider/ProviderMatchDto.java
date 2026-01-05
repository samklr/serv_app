package com.servantin.api.dto.provider;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for provider match results (simplified view for client selection)
 */
@Data
@Builder
public class ProviderMatchDto {
    private UUID id;
    private UUID userId;
    private String name;
    private String photoUrl;
    private String bio;
    private List<String> languages;
    private Boolean isVerified;
    private Double averageRating;
    private Long ratingCount;
    private String city;
    private BigDecimal hourlyRate;
    private BigDecimal fixedPrice;
    private String pricingType;
    private Integer responseTimeMinutes;
}
