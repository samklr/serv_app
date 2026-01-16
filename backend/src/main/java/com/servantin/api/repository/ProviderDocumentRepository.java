package com.servantin.api.repository;

import com.servantin.api.domain.entity.ProviderDocument;
import com.servantin.api.domain.model.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProviderDocumentRepository extends JpaRepository<ProviderDocument, UUID> {

    /**
     * Find all documents for a specific provider profile
     */
    List<ProviderDocument> findByProviderProfile_Id(UUID providerProfileId);

    /**
     * Find all documents by verification status (for admin review)
     */
    @Query("SELECT pd FROM ProviderDocument pd WHERE pd.verificationStatus = :status ORDER BY pd.createdAt ASC")
    List<ProviderDocument> findByVerificationStatus(VerificationStatus status);

    /**
     * Count documents by provider and status
     */
    long countByProviderProfile_IdAndVerificationStatus(UUID providerProfileId, VerificationStatus status);

    /**
     * Find all pending documents with provider details (for admin dashboard)
     */
    @Query("SELECT pd FROM ProviderDocument pd " +
           "LEFT JOIN FETCH pd.providerProfile pp " +
           "LEFT JOIN FETCH pp.user " +
           "WHERE pd.verificationStatus = :status " +
           "ORDER BY pd.createdAt ASC")
    List<ProviderDocument> findPendingDocumentsWithProviderDetails(VerificationStatus status);
}
