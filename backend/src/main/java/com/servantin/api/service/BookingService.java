package com.servantin.api.service;

import com.servantin.api.domain.entity.*;
import com.servantin.api.domain.model.BookingStatus;
import com.servantin.api.dto.booking.BookingDto;
import com.servantin.api.dto.booking.CreateBookingRequest;
import com.servantin.api.dto.category.CategoryDto;
import com.servantin.api.dto.common.PageResponse;
import com.servantin.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MessageRepository messageRepository;
    private final RatingRepository ratingRepository;
    private final ProviderProfileRepository providerProfileRepository;

    @Transactional
    public BookingDto createBooking(UUID clientId, CreateBookingRequest request) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        User provider = null;
        if (request.getProviderId() != null) {
            provider = userRepository.findById(request.getProviderId())
                    .orElseThrow(() -> new RuntimeException("Provider not found"));
        }

        Booking booking = Booking.builder()
                .client(client)
                .provider(provider)
                .category(category)
                .status(BookingStatus.REQUESTED)
                .description(request.getDescription())
                .postalCode(request.getPostalCode())
                .city(request.getCity())
                .addressText(request.getAddressText())
                .scheduledAt(request.getScheduledAt())
                .urgency(request.getUrgency())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .build();

        booking = bookingRepository.save(booking);
        log.info("Created booking {} for client {} with provider {}",
                booking.getId(), clientId, request.getProviderId());

        return toDto(booking, clientId);
    }

    @Transactional(readOnly = true)
    public BookingDto getBooking(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Verify user has access
        if (!booking.getClient().getId().equals(userId) &&
                (booking.getProvider() == null || !booking.getProvider().getId().equals(userId))) {
            throw new RuntimeException("Access denied to this booking");
        }

        return toDto(booking, userId);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getClientBookings(UUID clientId) {
        return bookingRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(b -> toDto(b, clientId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getProviderBookings(UUID providerId) {
        return bookingRepository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(b -> toDto(b, providerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getProviderPendingRequests(UUID providerId) {
        return bookingRepository.findPendingRequestsForProvider(providerId).stream()
                .map(b -> toDto(b, providerId))
                .toList();
    }

    @Transactional
    public BookingDto acceptBooking(UUID bookingId, UUID providerId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getProvider().getId().equals(providerId)) {
            throw new RuntimeException("Only the selected provider can accept this booking");
        }

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new RuntimeException("Booking is not in REQUESTED status");
        }

        booking.setStatus(BookingStatus.ACCEPTED);
        booking = bookingRepository.save(booking);
        log.info("Provider {} accepted booking {}", providerId, bookingId);

        return toDto(booking, providerId);
    }

    @Transactional
    public BookingDto declineBooking(UUID bookingId, UUID providerId, String reason) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getProvider().getId().equals(providerId)) {
            throw new RuntimeException("Only the selected provider can decline this booking");
        }

        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new RuntimeException("Booking is not in REQUESTED status");
        }

        booking.setStatus(BookingStatus.DECLINED);
        booking.setProvider(null); // Allow client to select another provider
        booking = bookingRepository.save(booking);
        log.info("Provider {} declined booking {}: {}", providerId, bookingId, reason);

        return toDto(booking, providerId);
    }

    @Transactional
    public BookingDto completeBooking(UUID bookingId, UUID providerId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getProvider().getId().equals(providerId)) {
            throw new RuntimeException("Only the provider can complete this booking");
        }

        if (booking.getStatus() != BookingStatus.ACCEPTED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new RuntimeException("Booking cannot be completed in current status");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        booking = bookingRepository.save(booking);
        log.info("Provider {} completed booking {}", providerId, bookingId);

        return toDto(booking, providerId);
    }

    @Transactional
    public BookingDto cancelBooking(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Both client and provider can cancel
        if (!booking.getClient().getId().equals(userId) &&
                (booking.getProvider() == null || !booking.getProvider().getId().equals(userId))) {
            throw new RuntimeException("Access denied to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Completed bookings cannot be canceled");
        }

        booking.setStatus(BookingStatus.CANCELED);
        booking = bookingRepository.save(booking);
        log.info("User {} canceled booking {}", userId, bookingId);

        return toDto(booking, userId);
    }

    // Admin methods
    @Transactional(readOnly = true)
    public PageResponse<BookingDto> getAllBookings(Pageable pageable) {
        Page<Booking> page = bookingRepository.findAllWithDetails(pageable);

        List<BookingDto> content = page.getContent().stream()
                .map(b -> toDto(b, null))
                .toList();

        return PageResponse.<BookingDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public BookingDto updateBookingStatus(UUID bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(status);
        if (status == BookingStatus.COMPLETED) {
            booking.setCompletedAt(Instant.now());
        }
        booking = bookingRepository.save(booking);

        return toDto(booking, null);
    }

    private BookingDto toDto(Booking booking, UUID currentUserId) {
        CategoryDto categoryDto = CategoryDto.builder()
                .id(booking.getCategory().getId())
                .slug(booking.getCategory().getSlug())
                .name(booking.getCategory().getName())
                .description(booking.getCategory().getDescription())
                .icon(booking.getCategory().getIcon())
                .build();

        BookingDto.ClientDto clientDto = BookingDto.ClientDto.builder()
                .id(booking.getClient().getId())
                .name(booking.getClient().getName())
                .email(booking.getClient().getEmail())
                .phone(booking.getClient().getPhone())
                .build();

        BookingDto.ProviderDto providerDto = null;
        if (booking.getProvider() != null) {
            User provider = booking.getProvider();
            Double avgRating = ratingRepository.getAverageRatingForProvider(provider.getId());

            // Get provider profile for photo
            String photoUrl = providerProfileRepository.findByUser_Id(provider.getId())
                    .map(ProviderProfile::getPhotoUrl)
                    .orElse(null);

            Boolean isVerified = providerProfileRepository.findByUser_Id(provider.getId())
                    .map(ProviderProfile::getIsVerified)
                    .orElse(false);

            providerDto = BookingDto.ProviderDto.builder()
                    .id(provider.getId())
                    .name(provider.getName())
                    .email(provider.getEmail())
                    .phone(provider.getPhone())
                    .photoUrl(photoUrl)
                    .isVerified(isVerified)
                    .averageRating(avgRating)
                    .build();
        }

        BookingDto.RatingDto ratingDto = null;
        if (booking.getRating() != null) {
            ratingDto = BookingDto.RatingDto.builder()
                    .id(booking.getRating().getId())
                    .score(booking.getRating().getScore())
                    .comment(booking.getRating().getComment())
                    .createdAt(booking.getRating().getCreatedAt())
                    .build();
        }

        Long unreadCount = 0L;
        if (currentUserId != null) {
            unreadCount = messageRepository.countUnreadMessages(booking.getId(), currentUserId);
        }

        return BookingDto.builder()
                .id(booking.getId())
                .status(booking.getStatus())
                .description(booking.getDescription())
                .postalCode(booking.getPostalCode())
                .city(booking.getCity())
                .addressText(booking.getAddressText())
                .scheduledAt(booking.getScheduledAt())
                .urgency(booking.getUrgency())
                .budgetMin(booking.getBudgetMin())
                .budgetMax(booking.getBudgetMax())
                .paymentStatus(booking.getPaymentStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .completedAt(booking.getCompletedAt())
                .category(categoryDto)
                .client(clientDto)
                .provider(providerDto)
                .rating(ratingDto)
                .unreadMessageCount(unreadCount)
                .build();
    }
}
