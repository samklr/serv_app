package com.servantin.api.repository;

import com.servantin.api.domain.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByBookingId(UUID bookingId);

    List<Rating> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    /**
     * Calculate average rating for a provider
     */
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.provider.id = :providerId")
    Double getAverageRatingForProvider(UUID providerId);

    /**
     * Count ratings for a provider
     */
    long countByProviderId(UUID providerId);
}
