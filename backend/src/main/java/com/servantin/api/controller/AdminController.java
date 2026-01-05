package com.servantin.api.controller;

import com.servantin.api.domain.model.BookingStatus;
import com.servantin.api.dto.booking.BookingDto;
import com.servantin.api.dto.common.PageResponse;
import com.servantin.api.dto.provider.ProviderProfileDto;
import com.servantin.api.service.BookingService;
import com.servantin.api.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative endpoints for managing the platform")
public class AdminController {

    private final ProviderService providerService;
    private final BookingService bookingService;

    // --- Provider Management ---

    @GetMapping("/providers")
    @Operation(summary = "List all providers", description = "Get all provider profiles with details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of providers", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProviderProfileDto.class))))
    })
    public ResponseEntity<List<ProviderProfileDto>> getAllProviders() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    @GetMapping("/providers/{id}")
    @Operation(summary = "Get provider", description = "Get detailed provider profile")
    public ResponseEntity<ProviderProfileDto> getProvider(@PathVariable UUID id) {
        return ResponseEntity.ok(providerService.getProviderProfileById(id));
    }

    @PutMapping("/providers/{id}/verify")
    @Operation(summary = "Verify provider", description = "Toggle provider verification status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provider updated", content = @Content(schema = @Schema(implementation = ProviderProfileDto.class)))
    })
    public ResponseEntity<ProviderProfileDto> verifyProvider(
            @PathVariable UUID id,
            @RequestParam boolean verified,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(providerService.verifyProvider(id, verified, notes));
    }

    // --- Booking Management ---

    @GetMapping("/bookings")
    @Operation(summary = "List all bookings", description = "Get all bookings with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of bookings")
    })
    public ResponseEntity<PageResponse<BookingDto>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(bookingService.getAllBookings(pageRequest));
    }

    @PutMapping("/bookings/{id}/status")
    @Operation(summary = "Update booking status", description = "Manually update booking status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking updated", content = @Content(schema = @Schema(implementation = BookingDto.class)))
    })
    public ResponseEntity<BookingDto> updateBookingStatus(
            @PathVariable UUID id,
            @RequestParam BookingStatus status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status));
    }
}
