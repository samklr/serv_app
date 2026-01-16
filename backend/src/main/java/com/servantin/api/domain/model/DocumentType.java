package com.servantin.api.domain.model;

/**
 * Types of verification documents that providers can upload.
 * Used to categorize documents submitted for provider verification.
 */
public enum DocumentType {
    /**
     * Government-issued ID card
     */
    ID_CARD,

    /**
     * Passport
     */
    PASSPORT,

    /**
     * Business registration or license
     */
    BUSINESS_LICENSE,

    /**
     * Professional certification or qualification
     */
    PROFESSIONAL_CERTIFICATION,

    /**
     * Insurance certificate (liability, professional indemnity, etc.)
     */
    INSURANCE_CERTIFICATE,

    /**
     * Other document types not listed above
     */
    OTHER
}
