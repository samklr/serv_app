package com.servantin.api.dto.booking;

import com.servantin.api.domain.model.BookingStatus;
import com.servantin.api.domain.model.PaymentStatus;
import com.servantin.api.dto.category.CategoryDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BookingDto {
    private UUID id;
    private BookingStatus status;
    private String description;
    private String postalCode;
    private String city;
    private String addressText;
    private Instant scheduledAt;
    private String urgency;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private PaymentStatus paymentStatus;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    private CategoryDto category;
    private ClientDto client;
    private ProviderDto provider;
    private RatingDto rating;

    private Long unreadMessageCount;

    @Data
    @Builder
    public static class ClientDto {
        private UUID id;
        private String name;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    public static class ProviderDto {
        private UUID id;
        private String name;
        private String email;
        private String phone;
        private String photoUrl;
        private Boolean isVerified;
        private Double averageRating;
    }

    @Data
    @Builder
    public static class RatingDto {
        private UUID id;
        private Integer score;
        private String comment;
        private Instant createdAt;
    }
}
