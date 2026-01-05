package com.servantin.api.service;

import com.servantin.api.domain.entity.*;
import com.servantin.api.domain.model.*;
import com.servantin.api.dto.provider.MatchRequest;
import com.servantin.api.dto.provider.ProviderMatchDto;
import com.servantin.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderProfileRepository providerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProviderLocationRepository providerLocationRepository;

    @Mock
    private ProviderAvailabilityRepository providerAvailabilityRepository;

    @Mock
    private ProviderPricingRepository providerPricingRepository;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private ProviderService providerService;

    private UUID categoryId;
    private ProviderProfile verifiedProvider;
    private ProviderProfile unverifiedProvider;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .slug("babysitting")
                .name("Babysitting & Nanny")
                .build();

        // Create verified provider
        User verifiedUser = User.builder()
                .id(UUID.randomUUID())
                .email("verified@test.ch")
                .name("Verified Provider")
                .role(UserRole.PROVIDER)
                .build();

        verifiedProvider = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(verifiedUser)
                .bio("Experienced babysitter")
                .languages(List.of("fr", "de"))
                .isVerified(true)
                .categories(new ArrayList<>())
                .locations(new ArrayList<>())
                .availabilities(new ArrayList<>())
                .pricings(new ArrayList<>())
                .createdAt(Instant.now().minusSeconds(86400)) // 1 day ago
                .build();

        ProviderCategory pc1 = ProviderCategory.builder()
                .providerProfile(verifiedProvider)
                .category(category)
                .build();
        verifiedProvider.getCategories().add(pc1);

        ProviderLocation loc1 = ProviderLocation.builder()
                .providerProfile(verifiedProvider)
                .postalCode("2800")
                .city("Delémont")
                .canton("JU")
                .build();
        verifiedProvider.getLocations().add(loc1);

        ProviderAvailability avail1 = ProviderAvailability.builder()
                .providerProfile(verifiedProvider)
                .weekday(1) // Monday
                .timeSlot(TimeSlot.MORNING)
                .build();
        verifiedProvider.getAvailabilities().add(avail1);

        ProviderPricing pricing1 = ProviderPricing.builder()
                .providerProfile(verifiedProvider)
                .category(category)
                .pricingType(PricingType.HOURLY)
                .hourlyRate(new BigDecimal("35.00"))
                .minHours(new BigDecimal("2.00"))
                .currency("CHF")
                .build();
        verifiedProvider.getPricings().add(pricing1);

        // Create unverified provider
        User unverifiedUser = User.builder()
                .id(UUID.randomUUID())
                .email("unverified@test.ch")
                .name("Unverified Provider")
                .role(UserRole.PROVIDER)
                .build();

        unverifiedProvider = ProviderProfile.builder()
                .id(UUID.randomUUID())
                .user(unverifiedUser)
                .bio("New babysitter")
                .languages(List.of("fr"))
                .isVerified(false)
                .categories(new ArrayList<>())
                .locations(new ArrayList<>())
                .availabilities(new ArrayList<>())
                .pricings(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        ProviderCategory pc2 = ProviderCategory.builder()
                .providerProfile(unverifiedProvider)
                .category(category)
                .build();
        unverifiedProvider.getCategories().add(pc2);

        ProviderLocation loc2 = ProviderLocation.builder()
                .providerProfile(unverifiedProvider)
                .postalCode("2800")
                .city("Delémont")
                .canton("JU")
                .build();
        unverifiedProvider.getLocations().add(loc2);

        ProviderPricing pricing2 = ProviderPricing.builder()
                .providerProfile(unverifiedProvider)
                .category(category)
                .pricingType(PricingType.HOURLY)
                .hourlyRate(new BigDecimal("30.00"))
                .minHours(new BigDecimal("1.00"))
                .currency("CHF")
                .build();
        unverifiedProvider.getPricings().add(pricing2);
    }

    @Test
    @DisplayName("Should match providers by category and location")
    void matchProviders_byCategoryAndLocation() {
        // Given
        MatchRequest request = new MatchRequest();
        request.setCategoryId(categoryId);
        request.setPostalCode("2800");
        request.setCity("Delémont");

        when(providerProfileRepository.findMatchingProviders(categoryId, "2800", "Delémont"))
                .thenReturn(List.of(verifiedProvider, unverifiedProvider));
        when(ratingRepository.getAverageRatingForProvider(any())).thenReturn(4.5);
        when(ratingRepository.countByProviderId(any())).thenReturn(10L);

        // When
        List<ProviderMatchDto> results = providerService.matchProviders(request);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Verified Provider");
        assertThat(results.get(0).getIsVerified()).isTrue();
        assertThat(results.get(1).getName()).isEqualTo("Unverified Provider");
        assertThat(results.get(1).getIsVerified()).isFalse();
    }

    @Test
    @DisplayName("Should prioritize verified providers in matching results")
    void matchProviders_verifiedFirst() {
        // Given
        MatchRequest request = new MatchRequest();
        request.setCategoryId(categoryId);
        request.setPostalCode("2800");
        request.setCity("Delémont");

        // Return in wrong order to verify sorting
        when(providerProfileRepository.findMatchingProviders(categoryId, "2800", "Delémont"))
                .thenReturn(List.of(unverifiedProvider, verifiedProvider));
        when(ratingRepository.getAverageRatingForProvider(any())).thenReturn(null);
        when(ratingRepository.countByProviderId(any())).thenReturn(0L);

        // When
        List<ProviderMatchDto> results = providerService.matchProviders(request);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getIsVerified()).isTrue();
        assertThat(results.get(1).getIsVerified()).isFalse();
    }

    @Test
    @DisplayName("Should filter by availability when time is specified")
    void matchProviders_filterByAvailability() {
        // Given
        // Monday at 10:00 AM (should match MORNING slot)
        ZonedDateTime monday10am = ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"));

        MatchRequest request = new MatchRequest();
        request.setCategoryId(categoryId);
        request.setPostalCode("2800");
        request.setCity("Delémont");
        request.setPreferredTime(monday10am.toInstant());

        when(providerProfileRepository.findMatchingProviders(categoryId, "2800", "Delémont"))
                .thenReturn(List.of(verifiedProvider, unverifiedProvider));
        when(ratingRepository.getAverageRatingForProvider(any())).thenReturn(null);
        when(ratingRepository.countByProviderId(any())).thenReturn(0L);

        // When
        List<ProviderMatchDto> results = providerService.matchProviders(request);

        // Then
        // Only verified provider has MORNING availability on Monday
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Verified Provider");
    }

    @Test
    @DisplayName("Should return empty list when no providers match")
    void matchProviders_noMatch() {
        // Given
        MatchRequest request = new MatchRequest();
        request.setCategoryId(categoryId);
        request.setPostalCode("1000");
        request.setCity("Lausanne");

        when(providerProfileRepository.findMatchingProviders(categoryId, "1000", "Lausanne"))
                .thenReturn(List.of());

        // When
        List<ProviderMatchDto> results = providerService.matchProviders(request);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should include pricing information in match results")
    void matchProviders_includesPricing() {
        // Given
        MatchRequest request = new MatchRequest();
        request.setCategoryId(categoryId);
        request.setPostalCode("2800");
        request.setCity("Delémont");

        when(providerProfileRepository.findMatchingProviders(categoryId, "2800", "Delémont"))
                .thenReturn(List.of(verifiedProvider));
        when(ratingRepository.getAverageRatingForProvider(any())).thenReturn(4.5);
        when(ratingRepository.countByProviderId(any())).thenReturn(10L);

        // When
        List<ProviderMatchDto> results = providerService.matchProviders(request);

        // Then
        assertThat(results).hasSize(1);
        ProviderMatchDto match = results.get(0);
        assertThat(match.getHourlyRate()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(match.getPricingType()).isEqualTo("HOURLY");
        assertThat(match.getAverageRating()).isEqualTo(4.5);
        assertThat(match.getRatingCount()).isEqualTo(10L);
    }
}
