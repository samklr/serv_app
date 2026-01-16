package com.servantin.api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Date-specific availability overrides for providers.
 * Allows providers to block out vacations, set special hours, or override their regular weekly schedule.
 */
@Entity
@Table(name = "provider_availability_overrides",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_provider_availability_override",
                           columnNames = {"provider_profile_id", "specific_date"})
       },
       indexes = {
           @Index(name = "idx_availability_overrides_profile_date",
                  columnList = "provider_profile_id,specific_date"),
           @Index(name = "idx_availability_overrides_date", columnList = "specific_date")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderAvailabilityOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_profile_id", nullable = false)
    private ProviderProfile providerProfile;

    @Column(name = "specific_date", nullable = false)
    private LocalDate specificDate;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = false;

    /**
     * Array of time slots if the provider is partially available on this date.
     * Null or empty if isAvailable is false (completely unavailable).
     * Example: ["09:00-12:00", "14:00-18:00"]
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "availability_override_time_slots",
                    joinColumns = @JoinColumn(name = "override_id"))
    @Column(name = "time_slot")
    @Builder.Default
    private List<String> timeSlots = new ArrayList<>();

    /**
     * Optional reason for unavailability (e.g., "On vacation", "Public holiday")
     */
    @Column(length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
