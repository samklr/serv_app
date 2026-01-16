package com.servantin.api.dto.provider;

import com.servantin.api.domain.model.DocumentType;
import com.servantin.api.domain.model.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for provider verification documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDocumentDto {

    private UUID id;
    private DocumentType documentType;
    private String fileName;
    private Long fileSizeBytes;
    private VerificationStatus verificationStatus;
    private String verificationNotes;
    private Instant createdAt;
    private Instant verifiedAt;

    /**
     * Signed URL for temporary access (not stored, generated on demand)
     */
    private String signedUrl;
}
