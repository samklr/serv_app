package com.servantin.api.dto.message;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MessageDto {
    private UUID id;
    private UUID bookingId;
    private UUID senderId;
    private String senderName;
    private String content;
    private Boolean isRead;
    private Boolean isOwnMessage;
    private Instant createdAt;
}
