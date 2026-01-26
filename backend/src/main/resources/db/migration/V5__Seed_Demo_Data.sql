-- V5: Seed Demo Data
-- Adds synthetic users, profiles, bookings, and ratings using plain SQL with hardcoded IDs for idempotency

-- ============================================
-- 1. Create Users
-- ============================================

-- Admin User
INSERT INTO users (id, email, password_hash, name, role, email_verified, created_at, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'admin@servantin.ch', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Admin User', 'ADMIN', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Provider 1: Sarah (Babysitting)
INSERT INTO users (id, email, password_hash, name, role, email_verified, created_at, updated_at)
VALUES ('22222222-2222-2222-2222-222222222222', 'sarah@servantin.ch', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Sarah Nanny', 'PROVIDER', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Provider 2: Mike (Home Support)
INSERT INTO users (id, email, password_hash, name, role, email_verified, created_at, updated_at)
VALUES ('33333333-3333-3333-3333-333333333333', 'mike@servantin.ch', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Mike Handyman', 'PROVIDER', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Client 1: John (Regular Client)
INSERT INTO users (id, email, password_hash, name, role, email_verified, created_at, updated_at)
VALUES ('44444444-4444-4444-4444-444444444444', 'john@servantin.ch', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'John Doe', 'CLIENT', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Client 2: Jane (Regular Client)
INSERT INTO users (id, email, password_hash, name, role, email_verified, created_at, updated_at)
VALUES ('55555555-5555-5555-5555-555555555555', 'jane@servantin.ch', '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KsnmG', 'Jane Smith', 'CLIENT', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;


-- ============================================
-- 2. Create Provider Profiles
-- ============================================

-- Sarah's Profile
INSERT INTO provider_profiles (id, user_id, bio, is_verified, response_time_minutes, created_at, updated_at)
VALUES ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '22222222-2222-2222-2222-222222222222', 'Experienced nanny with 5 years of experience. Certified in First Aid and CPR. I love working with kids of all ages!', true, 30, NOW(), NOW())
ON CONFLICT (user_id) DO NOTHING;

-- Mike's Profile
INSERT INTO provider_profiles (id, user_id, bio, is_verified, response_time_minutes, created_at, updated_at)
VALUES ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', '33333333-3333-3333-3333-333333333333', 'Professional handyman for all your home repair needs. Plumbing, electrical, assembly, and general maintenance.', true, 60, NOW(), NOW())
ON CONFLICT (user_id) DO NOTHING;


-- ============================================
-- 3. Link Providers to Categories & Details
-- ============================================

-- Sarah -> Babysitting
INSERT INTO provider_categories (provider_profile_id, category_id)
VALUES ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', (SELECT id FROM categories WHERE slug = 'babysitting'))
ON CONFLICT (provider_profile_id, category_id) DO NOTHING;

-- Mike -> Home Support
INSERT INTO provider_categories (provider_profile_id, category_id)
VALUES ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', (SELECT id FROM categories WHERE slug = 'home-support'))
ON CONFLICT (provider_profile_id, category_id) DO NOTHING;

-- Sarah's Location (Delémont)
INSERT INTO provider_locations (id, provider_profile_id, postal_code, city, canton)
VALUES ('f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0', 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', '2800', 'Delémont', 'JU')
ON CONFLICT (id) DO NOTHING;

-- Mike's Location (Porrentruy)
INSERT INTO provider_locations (id, provider_profile_id, postal_code, city, canton)
VALUES ('10101010-1010-1010-1010-101010101010', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', '2900', 'Porrentruy', 'JU')
ON CONFLICT (id) DO NOTHING;

-- Sarah's Pricing (CHF 25/hr)
INSERT INTO provider_pricings (id, provider_profile_id, category_id, pricing_type, hourly_rate, currency)
VALUES ('20202020-2020-2020-2020-202020202020', 'a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', (SELECT id FROM categories WHERE slug = 'babysitting'), 'HOURLY', 25.00, 'CHF')
ON CONFLICT (provider_profile_id, category_id) DO NOTHING;

-- Mike's Pricing (CHF 80/hr)
INSERT INTO provider_pricings (id, provider_profile_id, category_id, pricing_type, hourly_rate, currency)
VALUES ('30303030-3030-3030-3030-303030303030', 'b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', (SELECT id FROM categories WHERE slug = 'home-support'), 'HOURLY', 80.00, 'CHF')
ON CONFLICT (provider_profile_id, category_id) DO NOTHING;

-- Languages
INSERT INTO provider_languages (provider_profile_id, language) VALUES ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'French') ON CONFLICT DO NOTHING;
INSERT INTO provider_languages (provider_profile_id, language) VALUES ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'English') ON CONFLICT DO NOTHING;
INSERT INTO provider_languages (provider_profile_id, language) VALUES ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'French') ON CONFLICT DO NOTHING;
INSERT INTO provider_languages (provider_profile_id, language) VALUES ('b0b0b0b0-b0b0-b0b0-b0b0-b0b0b0b0b0b0', 'German') ON CONFLICT DO NOTHING;


-- ============================================
-- 4. Create Bookings
-- ============================================

-- Booking 1: Completed & Rated (John -> Sarah)
INSERT INTO bookings (
    id, client_id, provider_id, category_id, status, description,
    postal_code, city, scheduled_at, payment_status, created_at, completed_at
) VALUES (
    'c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0', '44444444-4444-4444-4444-444444444444', 
    '22222222-2222-2222-2222-222222222222', (SELECT id FROM categories WHERE slug = 'babysitting'), 
    'COMPLETED', 'Need partial day care for my 2 kids.', '2800', 'Delémont',
    NOW() - INTERVAL '5 days', 'PAID', NOW() - INTERVAL '7 days', NOW() - INTERVAL '5 days'
) ON CONFLICT (id) DO NOTHING;

-- Rating for Booking 1
INSERT INTO ratings (id, booking_id, client_id, provider_id, score, comment, created_at)
VALUES ('40404040-4040-4040-4040-404040404040', 'c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0', '44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 5, 'Sarah was amazing! The kids loved her.', NOW() - INTERVAL '4 days')
ON CONFLICT (booking_id) DO NOTHING;

-- Booking 2: Upcoming (Jane -> Sarah)
INSERT INTO bookings (
    id, client_id, provider_id, category_id, status, description,
    postal_code, city, scheduled_at, payment_status, created_at
) VALUES (
    'd0d0d0d0-d0d0-d0d0-d0d0-d0d0d0d0d0d0', '55555555-5555-5555-5555-555555555555', 
    '22222222-2222-2222-2222-222222222222', (SELECT id FROM categories WHERE slug = 'babysitting'), 
    'CONFIRMED', 'Evening babysitting for date night.', '2800', 'Delémont',
    NOW() + INTERVAL '2 days', 'AUTHORIZED', NOW() - INTERVAL '1 day'
) ON CONFLICT (id) DO NOTHING;

-- Booking 3: Pending Request (John -> Mike)
INSERT INTO bookings (
    id, client_id, provider_id, category_id, status, description,
    postal_code, city, scheduled_at, payment_status, created_at, budget_min, budget_max
) VALUES (
    'e0e0e0e0-e0e0-e0e0-e0e0-e0e0e0e0e0e0', '44444444-4444-4444-4444-444444444444', 
    '33333333-3333-3333-3333-333333333333', (SELECT id FROM categories WHERE slug = 'home-support'), 
    'REQUESTED', 'Fix leaky faucet in the kitchen.', '2800', 'Delémont',
    NOW() + INTERVAL '5 days', 'PENDING', NOW(), 100.00, 150.00
) ON CONFLICT (id) DO NOTHING;


-- ============================================
-- 5. Create Messages
-- ============================================

-- Messages for Booking 1
INSERT INTO messages (id, booking_id, sender_id, content, created_at)
VALUES ('50505050-5050-5050-5050-505050505050', 'c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0', '44444444-4444-4444-4444-444444444444', 'Hi Sarah, are you available next Tuesday?', NOW() - INTERVAL '7 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO messages (id, booking_id, sender_id, content, created_at)
VALUES ('60606060-6060-6060-6060-606060606060', 'c0c0c0c0-c0c0-c0c0-c0c0-c0c0c0c0c0c0', '22222222-2222-2222-2222-222222222222', 'Yes, I am! What time?', NOW() - INTERVAL '7 days' + INTERVAL '1 hour')
ON CONFLICT (id) DO NOTHING;
