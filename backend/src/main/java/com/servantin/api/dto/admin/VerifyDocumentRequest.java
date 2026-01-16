package com.servantin.api.dto.admin;

import com.servantin.api.domain.model.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin to verify/reject provider documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyDocumentRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus status;

    /**
     * Optional notes about verification decision
     */
    private String notes;
}
