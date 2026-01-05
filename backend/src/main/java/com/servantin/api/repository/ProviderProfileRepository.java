package com.servantin.api.repository;

import com.servantin.api.domain.entity.ProviderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, UUID> {

        Optional<ProviderProfile> findByUser_Id(UUID userId);

        @Query("SELECT DISTINCT pp FROM ProviderProfile pp " +
                        "LEFT JOIN FETCH pp.user u " +
                        "LEFT JOIN FETCH pp.categories pc " +
                        "LEFT JOIN FETCH pc.category " +
                        "LEFT JOIN FETCH pp.locations " +
                        "LEFT JOIN FETCH pp.pricings pr " +
                        "LEFT JOIN FETCH pr.category " +
                        "WHERE pp.id = :id")
        Optional<ProviderProfile> findByIdWithDetails(UUID id);

        @Query("SELECT DISTINCT pp FROM ProviderProfile pp " +
                        "LEFT JOIN FETCH pp.user u " +
                        "LEFT JOIN FETCH pp.categories pc " +
                        "LEFT JOIN FETCH pc.category " +
                        "WHERE pp.user.id = :userId")
        Optional<ProviderProfile> findByUserIdWithCategories(UUID userId);

        /**
         * Find providers matching search criteria for the matching algorithm
         */
        @Query("SELECT DISTINCT pp FROM ProviderProfile pp " +
                        "JOIN pp.categories pc " +
                        "JOIN pp.locations pl " +
                        "JOIN pp.user u " +
                        "WHERE pc.category.id = :categoryId " +
                        "AND (pl.postalCode = :postalCode OR LOWER(pl.city) = LOWER(:city))")
        List<ProviderProfile> findMatchingProviders(
                        @Param("categoryId") UUID categoryId,
                        @Param("postalCode") String postalCode,
                        @Param("city") String city);

        /**
         * Find all providers with their full details for admin panel
         */
        @Query("SELECT DISTINCT pp FROM ProviderProfile pp " +
                        "LEFT JOIN FETCH pp.user " +
                        "LEFT JOIN FETCH pp.categories pc " +
                        "LEFT JOIN FETCH pc.category " +
                        "ORDER BY pp.createdAt DESC")
        List<ProviderProfile> findAllWithDetails();

        /**
         * Find verified providers
         */
        List<ProviderProfile> findByIsVerifiedTrue();

        /**
         * Count verified providers
         */
        long countByIsVerifiedTrue();
}
