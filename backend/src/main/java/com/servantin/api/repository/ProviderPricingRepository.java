package com.servantin.api.repository;

import com.servantin.api.domain.entity.ProviderPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderPricingRepository extends JpaRepository<ProviderPricing, UUID> {

    List<ProviderPricing> findByProviderProfileId(UUID providerProfileId);

    Optional<ProviderPricing> findByProviderProfileIdAndCategoryId(UUID providerProfileId, UUID categoryId);

    void deleteByProviderProfileId(UUID providerProfileId);
}
