package com.servantin.api.config;

import com.servantin.api.domain.entity.*;
import com.servantin.api.domain.model.*;
import com.servantin.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdminUser();
        seedTestUsers();
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail("admin@servantin.ch").isEmpty()) {
            User admin = User.builder()
                    .email("admin@servantin.ch")
                    .passwordHash(passwordEncoder.encode("Admin123!"))
                    .name("Platform Admin")
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Created admin user: admin@servantin.ch / Admin123!");
        }
    }

    private void seedTestUsers() {
        // Create test client
        if (userRepository.findByEmail("client@test.ch").isEmpty()) {
            User client = User.builder()
                    .email("client@test.ch")
                    .passwordHash(passwordEncoder.encode("Test123!"))
                    .name("Jean Dupont")
                    .phone("+41 79 123 45 67")
                    .role(UserRole.CLIENT)
                    .build();
            userRepository.save(client);
            log.info("Created test client: client@test.ch / Test123!");
        }

        // Create test providers
        createTestProvider(
                "marie.bernard@test.ch",
                "Marie Bernard",
                "+41 79 234 56 78",
                "Professionnelle expérimentée avec plus de 10 ans d'expérience dans la garde d'enfants. Diplômée en petite enfance, je propose des services de babysitting personnalisés et bienveillants.",
                List.of("fr", "de"),
                "babysitting",
                List.of(new LocationData("2800", "Delémont", "JU"), new LocationData("2830", "Courrendlin", "JU")),
                new BigDecimal("35.00"),
                new BigDecimal("2.00"),
                true);

        createTestProvider(
                "pierre.muller@test.ch",
                "Pierre Müller",
                "+41 79 345 67 89",
                "Bricoleur polyvalent avec 15 ans d'expérience. Montage de meubles, petites réparations, peinture, et travaux divers. Interventions rapides et soignées.",
                List.of("fr", "de", "en"),
                "home-support",
                List.of(new LocationData("2800", "Delémont", "JU"), new LocationData("2900", "Porrentruy", "JU")),
                new BigDecimal("50.00"),
                new BigDecimal("1.00"),
                true);

        createTestProvider(
                "sophie.martin@test.ch",
                "Sophie Martin",
                "+41 79 456 78 90",
                "Aide-soignante diplômée, spécialisée dans l'accompagnement des personnes âgées. Patience, écoute et professionnalisme au service de votre bien-être.",
                List.of("fr"),
                "elderly-support",
                List.of(new LocationData("2800", "Delémont", "JU")),
                new BigDecimal("40.00"),
                new BigDecimal("3.00"),
                true);

        createTestProvider(
                "lucas.weber@test.ch",
                "Lucas Weber",
                "+41 79 567 89 01",
                "Expert-comptable indépendant. Aide aux déclarations d'impôts, conseil fiscal, et accompagnement administratif pour particuliers et petites entreprises.",
                List.of("fr", "de", "en"),
                "tax-admin",
                List.of(new LocationData("2800", "Delémont", "JU"), new LocationData("2900", "Porrentruy", "JU"),
                        new LocationData("2720", "Tramelan", "BE")),
                new BigDecimal("80.00"),
                new BigDecimal("1.00"),
                false);

        createTestProvider(
                "claire.favre@test.ch",
                "Claire Favre",
                "+41 79 678 90 12",
                "Coach en entrepreneuriat. Accompagnement à la création d'entreprise, business plan, stratégie de développement. Plus de 50 startups accompagnées.",
                List.of("fr", "en"),
                "entrepreneur-startup",
                List.of(new LocationData("2800", "Delémont", "JU")),
                null, // Fixed price
                null,
                false);

        log.info("Test data seeding completed");
    }

    private void createTestProvider(String email, String name, String phone, String bio,
            List<String> languages, String categorySlug,
            List<LocationData> locations, BigDecimal hourlyRate,
            BigDecimal minHours, boolean isVerified) {

        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Test123!"))
                .name(name)
                .phone(phone)
                .role(UserRole.PROVIDER)
                .build();
        user = userRepository.save(user);

        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categorySlug));

        ProviderProfile profile = ProviderProfile.builder()
                .user(user)
                .bio(bio)
                .languages(languages)
                .isVerified(isVerified)
                .verificationNotes(isVerified ? "Identity verified" : null)
                .build();

        // Add category
        ProviderCategory pc = ProviderCategory.builder()
                .providerProfile(profile)
                .category(category)
                .build();
        profile.getCategories().add(pc);

        // Add locations
        for (LocationData loc : locations) {
            ProviderLocation pl = ProviderLocation.builder()
                    .providerProfile(profile)
                    .postalCode(loc.postalCode)
                    .city(loc.city)
                    .canton(loc.canton)
                    .build();
            profile.getLocations().add(pl);
        }

        // Add availability (some default slots)
        for (int day = 1; day <= 5; day++) { // Monday to Friday
            for (TimeSlot slot : TimeSlot.values()) {
                ProviderAvailability pa = ProviderAvailability.builder()
                        .providerProfile(profile)
                        .weekday(day)
                        .timeSlot(slot)
                        .build();
                profile.getAvailabilities().add(pa);
            }
        }

        // Add pricing
        if (hourlyRate != null) {
            ProviderPricing pp = ProviderPricing.builder()
                    .providerProfile(profile)
                    .category(category)
                    .pricingType(PricingType.HOURLY)
                    .hourlyRate(hourlyRate)
                    .minHours(minHours)
                    .currency("CHF")
                    .build();
            profile.getPricings().add(pp);
        } else {
            ProviderPricing pp = ProviderPricing.builder()
                    .providerProfile(profile)
                    .category(category)
                    .pricingType(PricingType.FIXED)
                    .fixedPrice(new BigDecimal("150.00"))
                    .currency("CHF")
                    .build();
            profile.getPricings().add(pp);
        }

        providerProfileRepository.save(profile);
        log.info("Created test provider: {} ({})", name, email);
    }

    private record LocationData(String postalCode, String city, String canton) {
    }
}
