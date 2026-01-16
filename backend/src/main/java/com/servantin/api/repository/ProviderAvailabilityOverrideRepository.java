package com.servantin.api.repository;

import com.servantin.api.domain.entity.ProviderAvailabilityOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderAvailabilityOverrideRepository extends JpaRepository<ProviderAvailabilityOverride, UUID> {

    /**
     * Find availability override for a specific provider on a specific date
     */
    Optional<ProviderAvailabilityOverride> findByProviderProfile_IdAndSpecificDate(UUID providerProfileId, LocalDate date);

    /**
     * Find all overrides for a provider within a date range
     */
    @Query("SELECT pao FROM ProviderAvailabilityOverride pao " +
           "WHERE pao.providerProfile.id = :providerProfileId " +
           "AND pao.specificDate BETWEEN :startDate AND :endDate " +
           "ORDER BY pao.specificDate ASC")
    List<ProviderAvailabilityOverride> findByProviderProfileIdAndDateRange(
            UUID providerProfileId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all future overrides for a provider
     */
    @Query("SELECT pao FROM ProviderAvailabilityOverride pao " +
           "WHERE pao.providerProfile.id = :providerProfileId " +
           "AND pao.specificDate >= :fromDate " +
           "ORDER BY pao.specificDate ASC")
    List<ProviderAvailabilityOverride> findFutureOverrides(UUID providerProfileId, LocalDate fromDate);

    /**
     * Delete past overrides (cleanup job)
     */
    void deleteBySpecificDateBefore(LocalDate date);
}
