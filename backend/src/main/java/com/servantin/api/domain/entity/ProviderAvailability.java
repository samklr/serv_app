package com.servantin.api.domain.entity;

import com.servantin.api.domain.model.TimeSlot;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "provider_availabilities", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "provider_profile_id", "weekday", "time_slot" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_profile_id", nullable = false)
    private ProviderProfile providerProfile;

    @Column(nullable = false)
    private Integer weekday; // 0 = Sunday, 1 = Monday, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot", nullable = false)
    private TimeSlot timeSlot;
}
