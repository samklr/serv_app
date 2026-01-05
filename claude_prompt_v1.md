Below is a prompt you can copy‑paste into a coding agent. Adjust tech stack names (e.g. Next.js vs. Vue) if you prefer others.

***

You are an experienced full‑stack engineer and product-minded architect.  
Your task: design and implement a **web MVP** for **Serv@nitin**, an Uber‑style marketplace for services in French‑speaking Switzerland (starting in Canton du Jura), based on the following product description:

**Concept recap**

- Serv@nitin = “Uber for services”: a marketplace where users can book verified providers nearby for 7 launch categories:
  1. Babysitting / Nanny services  
  2. Home support / Handyman  
  3. Disability & healthcare application assistance  
  4. Tax & administration support  
  5. Entrepreneur & startup support  
  6. Travel / visa / booking assistance  
  7. Elderly support / at-home assistance  

- Key attributes: fast, local, safe, inclusive, socially impactful (youth, women, job‑seekers, migrants, vulnerable people).  
- Long‑term: mobile apps and real‑time geolocation. MVP: **web app** to validate demand, onboarding and matching.

***

## 1. Overall goals of this MVP

- Let **clients**:
  - Discover the value proposition and service categories.
  - Search services by category and location (postal code + city in Romandie).
  - Submit a booking request with basic details, preferred time, and budget.
  - Create an account, see their requests, and communicate with providers via basic messaging.
  - Pay via a **mock or sandbox** payment flow (Stripe test mode).

- Let **providers**:
  - Sign up and create a profile with basic KYC fields.
  - Select which of the 7 categories they offer, locations covered, and availability slots.
  - Set pricing model (hourly + minimum, or fixed price per typical task).
  - Receive and respond to booking requests (accept / decline).
  - View a simple dashboard of their upcoming bookings.

- Let **admins** (internal tool or admin panel):
  - View users, providers, and bookings.
  - Manually verify providers (toggle “verified” flag).
  - Manually match or reassign bookings if auto-matching fails.

- Start with **postal-code + city based matching**, not live GPS.

***

## 2. UX scope and pages

Design the app as a responsive web app (desktop + mobile) with clear separation of client vs provider flows. Do NOT over‑engineer; focus on clarity and speed.

**Public pages**

1. Homepage
   - Hero section with:
     - Headline: “Find trusted help — anytime, nearby.”
     - Sub-headline: “Book a verified professional for anything you need — fast, safe and local.”
     - Buttons:
       - “Book a service”
       - “Become a provider”
       - “Download the app (coming soon)” (non-functional or email capture)
   - Value icons:
     - Nearby providers
     - Fast booking
     - Transparent prices
     - Ratings & reviews
     - Safety first
   - Short social mission block (Serv@nitin = “service anything”, inclusion, empowerment).

2. Service categories page
   - List the 7 service categories with short descriptions.
   - Each card → CTA “Book this service” → leads to booking flow with category pre-selected.

3. “How it works” page (or section)
   - Steps:
     1. Choose a service
     2. Describe your need + location
     3. Compare available providers (price, rating, delivery time)
     4. Book securely and chat in-app
     5. Payment released only when service is completed (for now, we can simulate this).

4. Trust & Safety page (or section)
   - Explain:
     - Identity verification
     - Background / certification checks for sensitive categories
     - Rating & review after each mission
     - Secure payments
     - Optional insurance (described textually; integration later)

5. Social mission page (or section)
   - Explain mission and target groups: youth, women, job‑seekers, seniors, skilled migrants, people with disabilities.

**Authenticated client area**

1. Client dashboard
   - View list of all requests (pending, accepted, completed).
   - Status indicators and basic filters.
   - Access to conversation per booking.

2. Booking flow (wizard or multi‑step form)
   - Step 1: Choose category (pre-filled if coming from category card).
   - Step 2: Enter location (postal code, city; optional address for now), preferred date/time, urgency.
   - Step 3: Brief description of need and optional budget range.
   - Step 4: See list of matching providers with:
     - Name (or first name + initial).
     - Category tags.
     - Indicative price (hourly or range).
     - Distance (rough, based on city/postal code).
     - Rating (placeholder for now).
   - Step 5: Select provider and confirm booking request; simulate payment or use Stripe test mode.

3. Messaging
   - Simple, booking‑bound message thread between client and provider.

4. Profile & settings
   - Basic user info, saved locations (optional later), preferences.

**Authenticated provider area**

1. Provider dashboard
   - Overview of:
     - New booking requests (with accept / decline).
     - Upcoming bookings.
     - Past jobs.

2. Provider profile setup
   - Personal info: name, photo, short bio, languages spoken.
   - Service categories they offer (multi‑select among 7).
   - Locations: postal codes / cities they serve.
   - Availability: simple weekly schedule grid (e.g., morning/afternoon/evening).
   - Pricing:
     - For each category: hourly rate + minimum hours OR indicative fixed price.
   - Verification status (boolean + note field, controlled by admin).

3. Messaging
   - Same booking‑bound message threads.

4. Simple stats (later): number of completed jobs, rating.

**Admin area (can be behind a simple auth flag)**

- List and edit clients and providers.
- Approve/verify providers.
- View all bookings and change status.
- Optional: reassign booking to another provider.

***

## 3. Matching logic (MVP)

Implement a simple **matching service**:

- Inputs:
  - Category
  - Client postal code / city
  - Preferred time window
- Logic:
  - Filter providers who:
    - Offer that category
    - Serve that postal code / city
    - Have availability overlapping requested time
  - Order by:
    - Verification status (verified first)
    - Rating (or placeholder average)
    - Response speed proxy (for MVP, just created_at or random)
- Output:
  - List of candidate providers for the client to choose.
- For v1, no automatic assignment is required; client explicitly selects.

***

## 4. Architecture & tech choices

Choose a modern, pragmatic stack. Example suggestion (feel free to propose better but keep it simple):

- Frontend: React + Next.js (App Router) with TypeScript.
- UI: TailwindCSS + simple component kit.
- Backend: Next.js API routes OR lightweight Node/Express service.
- Database: Postgres with an ORM (Prisma).
- Auth: email/password + optional magic links (e.g., NextAuth or custom).
- Payments: Stripe test mode with minimal integration (create “payment intent” on booking; mark booking as “paid pending completion”).
- Deployment: any simple managed environment (e.g., Vercel + managed Postgres).

Constraints:

- Prefer clear domain models and maintainable code over microservices.
- Keep infrastructure minimal: one web app, one database, one Stripe account.
- Prepare for future mobile app by:
  - Designing a REST or GraphQL API layer with clear endpoints for core entities (users, providers, bookings, messages).

***

## 5. Data model (first iteration)

Define models (pseudo‑Prisma style):

- User
  - id, role (CLIENT / PROVIDER / ADMIN), email, password_hash, name, phone, created_at, updated_at
- ProviderProfile
  - id, user_id (FK), bio, photo_url, languages, is_verified, verification_notes
- Category
  - id, name, description
- ProviderCategory
  - provider_profile_id, category_id
- ProviderLocation
  - provider_profile_id, postal_code, city
- ProviderAvailability
  - provider_profile_id, weekday, time_slot (e.g., MORNING/AFTERNOON/EVENING)
- ProviderPricing
  - provider_profile_id, category_id, pricing_type (HOURLY/FIXED), hourly_rate, fixed_price, min_hours
- Booking
  - id, client_id, provider_id, category_id, status (REQUESTED / ACCEPTED / DECLINED / COMPLETED / CANCELED), description, postal_code, city, address_text, scheduled_at, created_at, updated_at, payment_status (PENDING / AUTHORIZED / CAPTURED / REFUNDED)
- Message
  - id, booking_id, sender_id, content, created_at
- Rating (later)
  - id, booking_id, client_id, provider_id, score, comment

Ask the database/ORM to generate migrations.

***

## 6. What to deliver

1. A short **architecture overview** (files, components, main flows).  
2. Database schema / migrations.  
3. API endpoints (document as you go: method, path, request & response).  
4. Frontend pages and components implementing:
   - Public marketing pages.
   - Client dashboards and flows.
   - Provider dashboards and flows.
   - Basic admin panel.
5. Seed script to:
   - Create the 7 categories.
   - Create a few fake providers in different categories and postal codes in Romandie.
6. Basic tests (at least for matching logic and one or two API endpoints).

***

## 7. Non‑functional requirements

- Keep UI minimal but clean and mobile‑friendly.
- Code must be readable and modular (separate domain logic from controllers and UI).
- Clearly mark TODOs for:
  - Full identity verification.
  - Insurance integration.
  - Real‑time geolocation.
  - Native mobile apps.

***

Start by summarizing your plan briefly, then proceed to propose the folder structure, data models, and core flows. After that, start implementing step by step.

Sources
