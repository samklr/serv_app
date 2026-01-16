package com.servantin.api.domain.model;

/**
 * Status of document verification by admins.
 * Tracks the review state of provider-submitted documents.
 */
public enum VerificationStatus {
    /**
     * Document uploaded but not yet reviewed by admin
     */
    PENDING,

    /**
     * Document reviewed and approved by admin
     */
    APPROVED,

    /**
     * Document reviewed and rejected by admin
     */
    REJECTED
}
