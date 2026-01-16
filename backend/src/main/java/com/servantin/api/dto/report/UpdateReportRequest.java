package com.servantin.api.dto.report;

import com.servantin.api.domain.model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin to update report status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportRequest {

    @NotNull(message = "Status is required")
    private ReportStatus status;

    /**
     * Admin notes/comments about the resolution
     */
    private String adminNotes;
}
