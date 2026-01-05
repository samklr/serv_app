-- V2__Seed_Categories.sql
-- Seed the 7 service categories

INSERT INTO categories (id, slug, name, description, icon, sort_order) VALUES
(gen_random_uuid(), 'babysitting', 'Babysitting & Nanny', 'Garde d''enfants à domicile, baby-sitting ponctuel ou régulier, accompagnement scolaire', 'baby', 1),
(gen_random_uuid(), 'home-support', 'Home Support & Handyman', 'Petits travaux, bricolage, montage de meubles, réparations diverses à domicile', 'wrench', 2),
(gen_random_uuid(), 'disability-healthcare', 'Disability & Healthcare Assistance', 'Aide aux personnes en situation de handicap, accompagnement médical, assistance administrative santé', 'heart-pulse', 3),
(gen_random_uuid(), 'tax-admin', 'Tax & Administration Support', 'Aide à la déclaration d''impôts, démarches administratives, rédaction de courriers officiels', 'file-text', 4),
(gen_random_uuid(), 'entrepreneur-startup', 'Entrepreneur & Startup Support', 'Conseil en création d''entreprise, business plan, comptabilité de base, accompagnement entrepreneurial', 'briefcase', 5),
(gen_random_uuid(), 'travel-visa', 'Travel, Visa & Booking Assistance', 'Aide aux démarches de visa, réservations de voyage, traduction de documents', 'plane', 6),
(gen_random_uuid(), 'elderly-support', 'Elderly Support & At-Home Assistance', 'Aide à domicile pour personnes âgées, compagnie, courses, préparation de repas', 'hand-helping', 7);
