package com.servantin.api.repository;

import com.servantin.api.domain.entity.Booking;
import com.servantin.api.domain.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.client " +
            "LEFT JOIN FETCH b.provider " +
            "LEFT JOIN FETCH b.category " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(UUID id);

    /**
     * Find bookings for a client
     */
    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.category " +
            "LEFT JOIN FETCH b.provider " +
            "WHERE b.client.id = :clientId " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    /**
     * Find bookings for a provider
     */
    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.category " +
            "LEFT JOIN FETCH b.client " +
            "WHERE b.provider.id = :providerId " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    /**
     * Find new requests for a provider (where they're selected but haven't accepted
     * yet)
     */
    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.category " +
            "LEFT JOIN FETCH b.client " +
            "WHERE b.provider.id = :providerId AND b.status = 'REQUESTED' " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findPendingRequestsForProvider(UUID providerId);

    /**
     * Find bookings by status
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Find all bookings with pagination (for admin)
     */
    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.client " +
            "LEFT JOIN FETCH b.provider " +
            "LEFT JOIN FETCH b.category")
    Page<Booking> findAllWithDetails(Pageable pageable);

    /**
     * Count bookings by status
     */
    long countByStatus(BookingStatus status);

    /**
     * Count bookings for a client
     */
    long countByClientId(UUID clientId);

    /**
     * Count bookings for a provider
     */
    long countByProviderId(UUID providerId);
}
