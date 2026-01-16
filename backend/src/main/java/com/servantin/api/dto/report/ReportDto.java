package com.servantin.api.dto.report;

import com.servantin.api.domain.model.ReportStatus;
import com.servantin.api.domain.model.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for report details (read operations).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto {

    private UUID id;

    // Reporter info
    private UUID reporterId;
    private String reporterName;
    private String reporterEmail;

    // Reported entity info
    private UUID reportedUserId;
    private String reportedUserName;
    private UUID reportedBookingId;

    // Report details
    private ReportType reportType;
    private String description;
    private ReportStatus status;

    // Admin review info
    private String adminNotes;
    private UUID resolvedById;
    private String resolvedByName;
    private Instant resolvedAt;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
