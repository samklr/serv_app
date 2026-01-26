-- V3: Add authentication, storage, and trust & safety fields
-- Adds email verification, password reset, provider documents, reports, and availability overrides

-- ============================================
-- Add email verification and password reset fields to users table
-- ============================================

ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN verification_token_expiry TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN reset_token VARCHAR(255);
ALTER TABLE users ADD COLUMN reset_token_expiry TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN accepted_terms_at TIMESTAMP WITH TIME ZONE;

-- Create indexes for token lookups (performance optimization with partial indexes)
CREATE INDEX idx_users_verification_token ON users(verification_token) WHERE verification_token IS NOT NULL;
CREATE INDEX idx_users_reset_token ON users(reset_token) WHERE reset_token IS NOT NULL;

-- Add comments for documentation
COMMENT ON COLUMN users.email_verified IS 'Whether user has verified their email address';
COMMENT ON COLUMN users.verification_token IS 'Token for email verification (24-hour expiry)';
COMMENT ON COLUMN users.reset_token IS 'Token for password reset (1-hour expiry)';
COMMENT ON COLUMN users.accepted_terms_at IS 'Timestamp when user accepted Terms of Service';

-- ============================================
-- Create provider_documents table for verification documents
-- ============================================

CREATE TABLE provider_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_profile_id UUID NOT NULL REFERENCES provider_profiles(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verification_notes TEXT,
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_document_type CHECK (document_type IN ('ID_CARD', 'PASSPORT', 'BUSINESS_LICENSE', 'PROFESSIONAL_CERTIFICATION', 'INSURANCE_CERTIFICATE', 'OTHER')),
    CONSTRAINT chk_verification_status CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_file_size CHECK (file_size_bytes > 0),
    CONSTRAINT chk_document_url_format CHECK (document_url LIKE 'gs://%')
);

-- Indexes for provider documents
CREATE INDEX idx_provider_documents_profile ON provider_documents(provider_profile_id);
CREATE INDEX idx_provider_documents_status ON provider_documents(verification_status);
CREATE INDEX idx_provider_documents_created ON provider_documents(created_at DESC);

-- Comments for documentation
COMMENT ON TABLE provider_documents IS 'Provider verification documents (ID, certificates, insurance)';
COMMENT ON COLUMN provider_documents.document_type IS 'Type of document: ID_CARD, PASSPORT, BUSINESS_LICENSE, etc.';
COMMENT ON COLUMN provider_documents.verification_status IS 'PENDING, APPROVED, or REJECTED by admin';
COMMENT ON COLUMN provider_documents.document_url IS 'GCS URL format: gs://bucket-name/path/to/file';

-- ============================================
-- Create reports table for trust & safety
-- ============================================

CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reported_booking_id UUID REFERENCES bookings(id) ON DELETE SET NULL,
    report_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_notes TEXT,
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_report_type CHECK (report_type IN ('INAPPROPRIATE_CONTENT', 'FRAUD', 'HARASSMENT', 'SPAM', 'SAFETY_CONCERN', 'OTHER')),
    CONSTRAINT chk_report_status CHECK (status IN ('PENDING', 'INVESTIGATING', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT chk_report_target CHECK (reported_user_id IS NOT NULL OR reported_booking_id IS NOT NULL),
    CONSTRAINT chk_description_length CHECK (char_length(description) >= 10)
);

-- Indexes for reports
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_reported_user ON reports(reported_user_id) WHERE reported_user_id IS NOT NULL;
CREATE INDEX idx_reports_reported_booking ON reports(reported_booking_id) WHERE reported_booking_id IS NOT NULL;
CREATE INDEX idx_reports_created ON reports(created_at DESC);

-- Comments for documentation
COMMENT ON TABLE reports IS 'User reports for inappropriate content, fraud, harassment, etc.';
COMMENT ON COLUMN reports.report_type IS 'Type of report: INAPPROPRIATE_CONTENT, FRAUD, HARASSMENT, SPAM, SAFETY_CONCERN, OTHER';
COMMENT ON COLUMN reports.status IS 'PENDING, INVESTIGATING, RESOLVED, or DISMISSED';
COMMENT ON CONSTRAINT chk_report_target ON reports IS 'Must report either a user or a booking';

-- ============================================
-- Create provider_availability_overrides table for date-specific availability
-- ============================================

CREATE TABLE provider_availability_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_profile_id UUID NOT NULL REFERENCES provider_profiles(id) ON DELETE CASCADE,
    specific_date DATE NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT FALSE,
    time_slots TEXT[],
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Ensure one override per provider per date
    CONSTRAINT uq_provider_availability_override UNIQUE(provider_profile_id, specific_date),

    -- Prevent setting past dates
    CONSTRAINT chk_future_date CHECK (specific_date >= CURRENT_DATE)
);

-- Indexes for availability overrides
CREATE INDEX idx_availability_overrides_profile_date ON provider_availability_overrides(provider_profile_id, specific_date);
CREATE INDEX idx_availability_overrides_date ON provider_availability_overrides(specific_date);

-- Comments for documentation
COMMENT ON TABLE provider_availability_overrides IS 'Date-specific availability overrides (vacations, special hours, etc.)';
COMMENT ON COLUMN provider_availability_overrides.specific_date IS 'Specific calendar date for this override';
COMMENT ON COLUMN provider_availability_overrides.is_available IS 'Whether provider is available on this date';
COMMENT ON COLUMN provider_availability_overrides.time_slots IS 'Array of available time slots if partially available';
COMMENT ON COLUMN provider_availability_overrides.reason IS 'Optional reason for unavailability (e.g., "On vacation")';

-- ============================================
-- Update trigger for updated_at columns
-- ============================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers to new tables
CREATE TRIGGER update_provider_documents_updated_at
    BEFORE UPDATE ON provider_documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reports_updated_at
    BEFORE UPDATE ON reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Data validation and statistics views
-- ============================================

-- View for provider verification readiness
CREATE OR REPLACE VIEW provider_verification_readiness AS
SELECT
    pp.id AS provider_profile_id,
    pp.user_id,
    u.email,
    u.name,
    u.email_verified,
    pp.is_verified AS profile_verified,
    COUNT(pd.id) FILTER (WHERE pd.verification_status = 'APPROVED') AS approved_documents,
    COUNT(pd.id) FILTER (WHERE pd.verification_status = 'PENDING') AS pending_documents,
    COUNT(pd.id) FILTER (WHERE pd.verification_status = 'REJECTED') AS rejected_documents,
    MAX(pd.created_at) AS last_document_uploaded
FROM provider_profiles pp
INNER JOIN users u ON pp.user_id = u.id
LEFT JOIN provider_documents pd ON pp.id = pd.provider_profile_id
GROUP BY pp.id, pp.user_id, u.email, u.name, u.email_verified, pp.is_verified;

COMMENT ON VIEW provider_verification_readiness IS 'Shows provider verification status and document counts';

-- View for report statistics
CREATE OR REPLACE VIEW report_statistics AS
SELECT
    r.reported_user_id,
    u.name AS reported_user_name,
    u.email AS reported_user_email,
    COUNT(*) AS total_reports,
    COUNT(*) FILTER (WHERE r.status = 'PENDING') AS pending_reports,
    COUNT(*) FILTER (WHERE r.status = 'INVESTIGATING') AS investigating_reports,
    COUNT(*) FILTER (WHERE r.status = 'RESOLVED') AS resolved_reports,
    COUNT(*) FILTER (WHERE r.status = 'DISMISSED') AS dismissed_reports,
    MAX(r.created_at) AS last_reported_at
FROM reports r
LEFT JOIN users u ON r.reported_user_id = u.id
WHERE r.reported_user_id IS NOT NULL
GROUP BY r.reported_user_id, u.name, u.email;

COMMENT ON VIEW report_statistics IS 'Aggregated statistics of reports per user';
