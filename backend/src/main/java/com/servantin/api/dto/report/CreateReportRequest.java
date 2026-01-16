package com.servantin.api.dto.report;

import com.servantin.api.domain.model.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new report.
 * Must report either a user OR a booking (at least one required).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    /**
     * ID of user being reported (optional if booking is reported)
     */
    private UUID reportedUserId;

    /**
     * ID of booking being reported (optional if user is reported)
     */
    private UUID reportedBookingId;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;
}
