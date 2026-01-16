package com.servantin.api.domain.model;

/**
 * Types of reports that users can submit for trust & safety.
 * Categorizes the nature of reported issues.
 */
public enum ReportType {
    /**
     * Inappropriate, offensive, or explicit content
     */
    INAPPROPRIATE_CONTENT,

    /**
     * Suspected fraudulent activity or scam
     */
    FRAUD,

    /**
     * Harassment, bullying, or threatening behavior
     */
    HARASSMENT,

    /**
     * Spam or unsolicited messages
     */
    SPAM,

    /**
     * Safety concerns (physical, mental, or financial)
     */
    SAFETY_CONCERN,

    /**
     * Other issues not listed above
     */
    OTHER
}
