package com.servantin.api.repository;

import com.servantin.api.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m " +
            "LEFT JOIN FETCH m.sender " +
            "WHERE m.booking.id = :bookingId " +
            "ORDER BY m.createdAt ASC")
    List<Message> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);

    /**
     * Count unread messages for a user in a booking
     */
    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.booking.id = :bookingId " +
            "AND m.sender.id != :userId " +
            "AND m.isRead = false")
    long countUnreadMessages(UUID bookingId, UUID userId);

    /**
     * Mark messages as read
     */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true " +
            "WHERE m.booking.id = :bookingId " +
            "AND m.sender.id != :userId " +
            "AND m.isRead = false")
    int markAsRead(UUID bookingId, UUID userId);
}
