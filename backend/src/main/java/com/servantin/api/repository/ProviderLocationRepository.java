package com.servantin.api.repository;

import com.servantin.api.domain.entity.ProviderLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProviderLocationRepository extends JpaRepository<ProviderLocation, UUID> {

    List<ProviderLocation> findByProviderProfileId(UUID providerProfileId);

    List<ProviderLocation> findByPostalCode(String postalCode);

    List<ProviderLocation> findByCityIgnoreCase(String city);

    void deleteByProviderProfileId(UUID providerProfileId);
}
