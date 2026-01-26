-- V4: Add missing availability_override_time_slots table
-- Required for JPA @ElementCollection on ProviderAvailabilityOverride.timeSlots
-- Also removes the TEXT[] column since we're using a normalized table instead

-- ============================================
-- Create the time slots collection table for JPA @ElementCollection
-- ============================================

CREATE TABLE IF NOT EXISTS availability_override_time_slots (
    override_id UUID NOT NULL REFERENCES provider_availability_overrides(id) ON DELETE CASCADE,
    time_slot VARCHAR(50) NOT NULL,
    PRIMARY KEY (override_id, time_slot)
);

-- Create index for lookups
CREATE INDEX IF NOT EXISTS idx_availability_override_time_slots_override 
    ON availability_override_time_slots(override_id);

-- Comments for documentation
COMMENT ON TABLE availability_override_time_slots IS 'Time slots for partial availability overrides (JPA @ElementCollection)';
COMMENT ON COLUMN availability_override_time_slots.time_slot IS 'Time slot in format HH:MM-HH:MM (e.g., 09:00-12:00)';

-- Drop the TEXT[] column from the main table as we now use a normalized table
-- This must be done carefully to preserve data if any exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'provider_availability_overrides' 
        AND column_name = 'time_slots'
    ) THEN
        ALTER TABLE provider_availability_overrides DROP COLUMN time_slots;
    END IF;
END $$;
