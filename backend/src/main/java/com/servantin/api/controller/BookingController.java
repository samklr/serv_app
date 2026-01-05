package com.servantin.api.controller;

import com.servantin.api.dto.booking.BookingDto;
import com.servantin.api.dto.booking.CreateBookingRequest;
import com.servantin.api.dto.message.MessageDto;
import com.servantin.api.dto.message.SendMessageRequest;
import com.servantin.api.security.CurrentUserService;
import com.servantin.api.service.BookingService;
import com.servantin.api.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

    private final BookingService bookingService;
    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new booking request as a client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking created", content = @Content(schema = @Schema(implementation = BookingDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.createBooking(userId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking", description = "Get booking details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking details", content = @Content(schema = @Schema(implementation = BookingDto.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<BookingDto> getBooking(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.getBooking(id, userId));
    }

    @GetMapping("/client")
    @Operation(summary = "Get client bookings", description = "Get all bookings for the current client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of bookings", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookingDto.class))))
    })
    public ResponseEntity<List<BookingDto>> getClientBookings() {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.getClientBookings(userId));
    }

    @GetMapping("/provider")
    @Operation(summary = "Get provider bookings", description = "Get all bookings for the current provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of bookings", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookingDto.class))))
    })
    public ResponseEntity<List<BookingDto>> getProviderBookings() {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.getProviderBookings(userId));
    }

    @GetMapping("/provider/pending")
    @Operation(summary = "Get pending requests", description = "Get pending booking requests for the current provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of pending bookings", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookingDto.class))))
    })
    public ResponseEntity<List<BookingDto>> getProviderPendingRequests() {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.getProviderPendingRequests(userId));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept booking", description = "Provider accepts a booking request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking accepted", content = @Content(schema = @Schema(implementation = BookingDto.class))),
            @ApiResponse(responseCode = "400", description = "Cannot accept booking in current status"),
            @ApiResponse(responseCode = "403", description = "Only selected provider can accept")
    })
    public ResponseEntity<BookingDto> acceptBooking(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.acceptBooking(id, userId));
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Decline booking", description = "Provider declines a booking request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking declined", content = @Content(schema = @Schema(implementation = BookingDto.class)))
    })
    public ResponseEntity<BookingDto> declineBooking(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.declineBooking(id, userId, reason));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete booking", description = "Provider marks booking as completed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking completed", content = @Content(schema = @Schema(implementation = BookingDto.class)))
    })
    public ResponseEntity<BookingDto> completeBooking(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.completeBooking(id, userId));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking", description = "Client or provider cancels the booking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking canceled", content = @Content(schema = @Schema(implementation = BookingDto.class)))
    })
    public ResponseEntity<BookingDto> cancelBooking(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
    }

    // --- Messages ---

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages", description = "Get all messages for a booking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of messages", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MessageDto.class))))
    })
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable UUID id) {
        UUID userId = currentUserService.getCurrentUserId();
        // Mark messages as read when fetching
        messageService.markMessagesAsRead(id, userId);
        return ResponseEntity.ok(messageService.getBookingMessages(id, userId));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send message", description = "Send a message in a booking conversation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent", content = @Content(schema = @Schema(implementation = MessageDto.class)))
    })
    public ResponseEntity<MessageDto> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(messageService.sendMessage(id, userId, request));
    }
}
