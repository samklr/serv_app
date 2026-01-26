package com.servantin.api.service;

import com.servantin.api.domain.entity.Booking;
import com.servantin.api.domain.entity.Message;
import com.servantin.api.domain.entity.User;
import com.servantin.api.dto.message.MessageDto;
import com.servantin.api.dto.message.SendMessageRequest;
import com.servantin.api.repository.BookingRepository;
import com.servantin.api.repository.MessageRepository;
import com.servantin.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<MessageDto> getBookingMessages(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Verify user has access
        if (!booking.getClient().getId().equals(userId) &&
                (booking.getProvider() == null || !booking.getProvider().getId().equals(userId))) {
            throw new RuntimeException("Access denied to this booking");
        }

        return messageRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(m -> toDto(m, userId))
                .toList();
    }

    @Transactional
    public MessageDto sendMessage(UUID bookingId, UUID senderId, SendMessageRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Verify user has access
        if (!booking.getClient().getId().equals(senderId) &&
                (booking.getProvider() == null || !booking.getProvider().getId().equals(senderId))) {
            throw new RuntimeException("Access denied to this booking");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message message = Message.builder()
                .booking(booking)
                .sender(sender)
                .content(request.getContent())
                .isRead(false)
                .build();

        message = messageRepository.save(message);
        log.info("User {} sent message in booking {}", senderId, bookingId);

        // Send email notification to the recipient (don't fail message sending if email fails)
        try {
            boolean senderIsClient = booking.getClient().getId().equals(senderId);
            User recipient = senderIsClient ? booking.getProvider() : booking.getClient();

            if (recipient != null) {
                String messagePreview = request.getContent().length() > 100
                        ? request.getContent().substring(0, 100) + "..."
                        : request.getContent();
                emailService.sendNewMessageNotification(
                        recipient.getEmail(),
                        recipient.getName(),
                        sender.getName(),
                        booking.getId().toString(),
                        messagePreview
                );
            }
        } catch (Exception e) {
            log.error("Failed to send new message email notification for booking {}: {}", bookingId, e.getMessage());
        }

        return toDto(message, senderId);
    }

    @Transactional
    public void markMessagesAsRead(UUID bookingId, UUID userId) {
        int updated = messageRepository.markAsRead(bookingId, userId);
        log.debug("Marked {} messages as read for user {} in booking {}", updated, userId, bookingId);
    }

    @Transactional(readOnly = true)
    public long countUnreadMessages(UUID bookingId, UUID userId) {
        return messageRepository.countUnreadMessages(bookingId, userId);
    }

    private MessageDto toDto(Message message, UUID currentUserId) {
        return MessageDto.builder()
                .id(message.getId())
                .bookingId(message.getBooking().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .isOwnMessage(message.getSender().getId().equals(currentUserId))
                .createdAt(message.getCreatedAt())
                .build();
    }
}
