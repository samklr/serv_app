package com.servantin.api.service;

import com.servantin.api.domain.entity.*;
import com.servantin.api.domain.model.PricingType;
import com.servantin.api.domain.model.TimeSlot;
import com.servantin.api.domain.model.UserRole;
import com.servantin.api.dto.category.CategoryDto;
import com.servantin.api.dto.provider.*;
import com.servantin.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderService {

        private final ProviderProfileRepository providerProfileRepository;
        private final UserRepository userRepository;
        private final CategoryRepository categoryRepository;
        private final ProviderLocationRepository providerLocationRepository;
        private final ProviderAvailabilityRepository providerAvailabilityRepository;
        private final ProviderPricingRepository providerPricingRepository;
        private final RatingRepository ratingRepository;

        @Transactional(readOnly = true)
        public ProviderProfileDto getProviderProfile(UUID userId) {
                ProviderProfile profile = providerProfileRepository.findByUserIdWithCategories(userId)
                                .orElseThrow(() -> new RuntimeException("Provider profile not found"));
                return toDto(profile);
        }

        @Transactional(readOnly = true)
        public ProviderProfileDto getProviderProfileById(UUID profileId) {
                ProviderProfile profile = providerProfileRepository.findByIdWithDetails(profileId)
                                .orElseThrow(() -> new RuntimeException("Provider profile not found"));
                return toDto(profile);
        }

        @Transactional
        public ProviderProfileDto createOrUpdateProfile(UUID userId, ProviderProfileRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Ensure user has PROVIDER role
                if (user.getRole() != UserRole.PROVIDER) {
                        user.setRole(UserRole.PROVIDER);
                        userRepository.save(user);
                }

                ProviderProfile profile = providerProfileRepository.findByUser_Id(userId)
                                .orElseGet(() -> {
                                        ProviderProfile newProfile = ProviderProfile.builder()
                                                        .user(user)
                                                        .build();
                                        return providerProfileRepository.save(newProfile);
                                });

                // Update basic fields
                profile.setBio(request.getBio());
                profile.setPhotoUrl(request.getPhotoUrl());
                profile.setLanguages(request.getLanguages());

                // Update categories
                profile.getCategories().clear();
                for (UUID categoryId : request.getCategoryIds()) {
                        Category category = categoryRepository.findById(categoryId)
                                        .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
                        ProviderCategory pc = ProviderCategory.builder()
                                        .providerProfile(profile)
                                        .category(category)
                                        .build();
                        profile.getCategories().add(pc);
                }

                // Update locations
                providerLocationRepository.deleteByProviderProfileId(profile.getId());
                for (ProviderProfileRequest.LocationDto loc : request.getLocations()) {
                        ProviderLocation pl = ProviderLocation.builder()
                                        .providerProfile(profile)
                                        .postalCode(loc.getPostalCode())
                                        .city(loc.getCity())
                                        .canton(loc.getCanton() != null ? loc.getCanton() : "JU")
                                        .build();
                        profile.getLocations().add(pl);
                }

                // Update availabilities
                if (request.getAvailabilities() != null) {
                        providerAvailabilityRepository.deleteByProviderProfileId(profile.getId());
                        for (ProviderProfileRequest.AvailabilityDto avail : request.getAvailabilities()) {
                                ProviderAvailability pa = ProviderAvailability.builder()
                                                .providerProfile(profile)
                                                .weekday(avail.getWeekday())
                                                .timeSlot(avail.getTimeSlot())
                                                .build();
                                profile.getAvailabilities().add(pa);
                        }
                }

                // Update pricings
                if (request.getPricings() != null) {
                        providerPricingRepository.deleteByProviderProfileId(profile.getId());
                        for (ProviderProfileRequest.PricingDto pricing : request.getPricings()) {
                                Category category = categoryRepository.findById(pricing.getCategoryId())
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Category not found: " + pricing.getCategoryId()));
                                ProviderPricing pp = ProviderPricing.builder()
                                                .providerProfile(profile)
                                                .category(category)
                                                .pricingType(pricing.getPricingType())
                                                .hourlyRate(pricing.getHourlyRate())
                                                .fixedPrice(pricing.getFixedPrice())
                                                .minHours(pricing.getMinHours())
                                                .build();
                                profile.getPricings().add(pp);
                        }
                }

                profile = providerProfileRepository.save(profile);
                return toDto(profile);
        }

        /**
         * Match providers based on category, location, and optionally time
         */
        @Transactional(readOnly = true)
        public List<ProviderMatchDto> matchProviders(MatchRequest request) {
                log.info("Matching providers for category={}, postalCode={}, city={}",
                                request.getCategoryId(), request.getPostalCode(), request.getCity());

                // Step 1: Find providers matching category and location
                List<ProviderProfile> matchingProviders = new ArrayList<>(
                                providerProfileRepository.findMatchingProviders(
                                                request.getCategoryId(),
                                                request.getPostalCode(),
                                                request.getCity()));

                // Step 2: Filter by availability if time is specified
                if (request.getPreferredTime() != null) {
                        int weekday = request.getPreferredTime()
                                        .atZone(ZoneId.of("Europe/Zurich"))
                                        .getDayOfWeek()
                                        .getValue() % 7; // Convert to 0=Sunday format
                        TimeSlot timeSlot = getTimeSlot(request.getPreferredTime());

                        matchingProviders = matchingProviders.stream()
                                        .filter(p -> hasAvailability(p, weekday, timeSlot))
                                        .collect(Collectors.toList());
                }

                // Step 3: Sort by priority (verified first, then rating, then seniority)
                matchingProviders.sort((p1, p2) -> {
                        // Verified first
                        if (p1.getIsVerified() != p2.getIsVerified()) {
                                return p2.getIsVerified().compareTo(p1.getIsVerified());
                        }
                        // Then by rating
                        Double rating1 = ratingRepository.getAverageRatingForProvider(p1.getUser().getId());
                        Double rating2 = ratingRepository.getAverageRatingForProvider(p2.getUser().getId());
                        if (rating1 == null)
                                rating1 = 0.0;
                        if (rating2 == null)
                                rating2 = 0.0;
                        if (!rating1.equals(rating2)) {
                                return rating2.compareTo(rating1);
                        }
                        // Then by seniority (older accounts first)
                        return p1.getCreatedAt().compareTo(p2.getCreatedAt());
                });

                // Step 4: Convert to DTOs
                return matchingProviders.stream()
                                .map(p -> toMatchDto(p, request.getCategoryId()))
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<ProviderProfileDto> getAllProviders() {
                return providerProfileRepository.findAllWithDetails().stream()
                                .map(this::toDto)
                                .toList();
        }

        @Transactional
        public ProviderProfileDto verifyProvider(UUID profileId, boolean verified, String notes) {
                ProviderProfile profile = providerProfileRepository.findById(profileId)
                                .orElseThrow(() -> new RuntimeException("Provider not found"));
                profile.setIsVerified(verified);
                profile.setVerificationNotes(notes);
                profile = providerProfileRepository.save(profile);
                return toDto(profile);
        }

        private boolean hasAvailability(ProviderProfile profile, int weekday, TimeSlot timeSlot) {
                return profile.getAvailabilities().stream()
                                .anyMatch(a -> a.getWeekday().equals(weekday) && a.getTimeSlot() == timeSlot);
        }

        private TimeSlot getTimeSlot(Instant time) {
                int hour = time.atZone(ZoneId.of("Europe/Zurich")).getHour();
                if (hour >= 8 && hour < 12)
                        return TimeSlot.MORNING;
                if (hour >= 12 && hour < 17)
                        return TimeSlot.AFTERNOON;
                return TimeSlot.EVENING;
        }

        private ProviderProfileDto toDto(ProviderProfile profile) {
                User user = profile.getUser();
                Double avgRating = ratingRepository.getAverageRatingForProvider(user.getId());
                Long ratingCount = ratingRepository.countByProviderId(user.getId());

                List<CategoryDto> categories = profile.getCategories().stream()
                                .map(pc -> CategoryDto.builder()
                                                .id(pc.getCategory().getId())
                                                .slug(pc.getCategory().getSlug())
                                                .name(pc.getCategory().getName())
                                                .description(pc.getCategory().getDescription())
                                                .icon(pc.getCategory().getIcon())
                                                .build())
                                .toList();

                List<ProviderProfileDto.LocationDto> locations = profile.getLocations().stream()
                                .map(l -> ProviderProfileDto.LocationDto.builder()
                                                .id(l.getId())
                                                .postalCode(l.getPostalCode())
                                                .city(l.getCity())
                                                .canton(l.getCanton())
                                                .build())
                                .toList();

                List<ProviderProfileDto.AvailabilityDto> availabilities = profile.getAvailabilities().stream()
                                .map(a -> ProviderProfileDto.AvailabilityDto.builder()
                                                .id(a.getId())
                                                .weekday(a.getWeekday())
                                                .timeSlot(a.getTimeSlot())
                                                .build())
                                .toList();

                List<ProviderProfileDto.PricingDto> pricings = profile.getPricings().stream()
                                .map(p -> ProviderProfileDto.PricingDto.builder()
                                                .id(p.getId())
                                                .categoryId(p.getCategory().getId())
                                                .categoryName(p.getCategory().getName())
                                                .pricingType(p.getPricingType())
                                                .hourlyRate(p.getHourlyRate())
                                                .fixedPrice(p.getFixedPrice())
                                                .minHours(p.getMinHours())
                                                .currency(p.getCurrency())
                                                .build())
                                .toList();

                return ProviderProfileDto.builder()
                                .id(profile.getId())
                                .userId(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .phone(user.getPhone())
                                .bio(profile.getBio())
                                .photoUrl(profile.getPhotoUrl())
                                .languages(profile.getLanguages())
                                .isVerified(profile.getIsVerified())
                                .verificationNotes(profile.getVerificationNotes())
                                .responseTimeMinutes(profile.getResponseTimeMinutes())
                                .averageRating(avgRating)
                                .ratingCount(ratingCount)
                                .createdAt(profile.getCreatedAt())
                                .categories(categories)
                                .locations(locations)
                                .availabilities(availabilities)
                                .pricings(pricings)
                                .build();
        }

        private ProviderMatchDto toMatchDto(ProviderProfile profile, UUID categoryId) {
                User user = profile.getUser();
                Double avgRating = ratingRepository.getAverageRatingForProvider(user.getId());
                Long ratingCount = ratingRepository.countByProviderId(user.getId());

                // Get pricing for the specific category
                ProviderPricing pricing = profile.getPricings().stream()
                                .filter(p -> p.getCategory().getId().equals(categoryId))
                                .findFirst()
                                .orElse(null);

                // Get first location city
                String city = profile.getLocations().stream()
                                .findFirst()
                                .map(ProviderLocation::getCity)
                                .orElse(null);

                return ProviderMatchDto.builder()
                                .id(profile.getId())
                                .userId(user.getId())
                                .name(user.getName())
                                .photoUrl(profile.getPhotoUrl())
                                .bio(profile.getBio())
                                .languages(profile.getLanguages())
                                .isVerified(profile.getIsVerified())
                                .averageRating(avgRating)
                                .ratingCount(ratingCount)
                                .city(city)
                                .hourlyRate(pricing != null ? pricing.getHourlyRate() : null)
                                .fixedPrice(pricing != null ? pricing.getFixedPrice() : null)
                                .pricingType(pricing != null ? pricing.getPricingType().name() : null)
                                .responseTimeMinutes(profile.getResponseTimeMinutes())
                                .build();
        }
}
