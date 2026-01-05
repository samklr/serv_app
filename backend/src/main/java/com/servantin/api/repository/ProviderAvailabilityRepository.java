package com.servantin.api.repository;

import com.servantin.api.domain.entity.ProviderAvailability;
import com.servantin.api.domain.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability, UUID> {

    List<ProviderAvailability> findByProviderProfileId(UUID providerProfileId);

    List<ProviderAvailability> findByProviderProfileIdAndWeekday(UUID providerProfileId, Integer weekday);

    List<ProviderAvailability> findByProviderProfileIdAndWeekdayAndTimeSlot(
            UUID providerProfileId, Integer weekday, TimeSlot timeSlot);

    void deleteByProviderProfileId(UUID providerProfileId);
}
