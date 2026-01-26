package com.servantin.api.service;

import com.servantin.api.domain.entity.ProviderDocument;
import com.servantin.api.domain.entity.User;
import com.servantin.api.domain.model.VerificationStatus;
import com.servantin.api.dto.admin.ProviderDocumentDto;
import com.servantin.api.exception.BadRequestException;
import com.servantin.api.exception.ResourceNotFoundException;
import com.servantin.api.repository.ProviderDocumentRepository;
import com.servantin.api.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVerificationService {

    private final ProviderDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Get all documents pending review
     */
    @Transactional(readOnly = true)
    public List<ProviderDocumentDto> getPendingDocuments() {
        List<ProviderDocument> documents = documentRepository.findPendingDocumentsWithProviderDetails(VerificationStatus.PENDING);
        return documents.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get all documents by status
     */
    @Transactional(readOnly = true)
    public List<ProviderDocumentDto> getDocumentsByStatus(VerificationStatus status) {
        List<ProviderDocument> documents = documentRepository.findByVerificationStatus(status);
        return documents.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get all documents for a specific provider
     */
    @Transactional(readOnly = true)
    public List<ProviderDocumentDto> getProviderDocuments(UUID providerProfileId) {
        List<ProviderDocument> documents = documentRepository.findByProviderProfile_Id(providerProfileId);
        return documents.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get single document by ID
     */
    @Transactional(readOnly = true)
    public ProviderDocumentDto getDocumentById(UUID documentId) {
        ProviderDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
        return toDto(document);
    }

    /**
     * Verify (approve or reject) a document
     */
    @Transactional
    public ProviderDocumentDto verifyDocument(UUID documentId, UUID adminId, VerificationStatus status, String notes) {
        if (status == VerificationStatus.PENDING) {
            throw BadRequestException.invalidOperation("Cannot set status back to PENDING");
        }

        ProviderDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user", adminId));

        document.setVerificationStatus(status);
        document.setVerificationNotes(notes);
        document.setVerifiedBy(admin);
        document.setVerifiedAt(Instant.now());

        document = documentRepository.save(document);
        log.info("Admin {} verified document {} as {}", adminId, documentId, status);

        // Send notification email to provider
        try {
            User providerUser = document.getProviderProfile().getUser();
            if (status == VerificationStatus.APPROVED) {
                // Check if all required documents are now approved
                long pendingCount = documentRepository.countByProviderProfile_IdAndVerificationStatus(
                        document.getProviderProfile().getId(), VerificationStatus.PENDING);
                if (pendingCount == 0) {
                    emailService.sendProviderVerified(providerUser.getEmail(), providerUser.getName());
                }
            } else if (status == VerificationStatus.REJECTED) {
                emailService.sendProviderRejected(providerUser.getEmail(), providerUser.getName(), notes);
            }
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }

        return toDto(document);
    }

    /**
     * Get verification statistics
     */
    @Transactional(readOnly = true)
    public DocumentStatistics getStatistics() {
        long pending = documentRepository.findByVerificationStatus(VerificationStatus.PENDING).size();
        long approved = documentRepository.findByVerificationStatus(VerificationStatus.APPROVED).size();
        long rejected = documentRepository.findByVerificationStatus(VerificationStatus.REJECTED).size();

        return DocumentStatistics.builder()
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .total(pending + approved + rejected)
                .build();
    }

    private ProviderDocumentDto toDto(ProviderDocument document) {
        return ProviderDocumentDto.builder()
                .id(document.getId())
                .providerProfileId(document.getProviderProfile().getId())
                .providerName(document.getProviderProfile().getUser().getName())
                .providerEmail(document.getProviderProfile().getUser().getEmail())
                .documentType(document.getDocumentType())
                .documentUrl(document.getDocumentUrl())
                .fileName(document.getFileName())
                .fileSizeBytes(document.getFileSizeBytes())
                .mimeType(document.getMimeType())
                .verificationStatus(document.getVerificationStatus())
                .verificationNotes(document.getVerificationNotes())
                .verifiedById(document.getVerifiedBy() != null ? document.getVerifiedBy().getId() : null)
                .verifiedByName(document.getVerifiedBy() != null ? document.getVerifiedBy().getName() : null)
                .verifiedAt(document.getVerifiedAt())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    @Data
    @Builder
    public static class DocumentStatistics {
        private long pending;
        private long approved;
        private long rejected;
        private long total;
    }
}
