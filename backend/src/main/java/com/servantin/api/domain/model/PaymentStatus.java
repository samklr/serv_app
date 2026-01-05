package com.servantin.api.domain.model;

/**
 * Payment status for bookings
 */
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    FAILED
}
