package com.servantin.api.domain.model;

/**
 * Status of a report submitted by users.
 * Tracks the lifecycle of report review and resolution by admins.
 */
public enum ReportStatus {
    /**
     * Report submitted but not yet reviewed
     */
    PENDING,

    /**
     * Report is actively being investigated by admin
     */
    INVESTIGATING,

    /**
     * Report has been reviewed and action taken
     */
    RESOLVED,

    /**
     * Report reviewed but no action required
     */
    DISMISSED
}
