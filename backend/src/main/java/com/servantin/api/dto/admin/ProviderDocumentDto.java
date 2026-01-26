package com.servantin.api.dto.admin;

import com.servantin.api.domain.model.DocumentType;
import com.servantin.api.domain.model.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ProviderDocumentDto {
    private UUID id;
    private UUID providerProfileId;
    private String providerName;
    private String providerEmail;
    private DocumentType documentType;
    private String documentUrl;
    private String fileName;
    private Long fileSizeBytes;
    private String mimeType;
    private VerificationStatus verificationStatus;
    private String verificationNotes;
    private UUID verifiedById;
    private String verifiedByName;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
