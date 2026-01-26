package com.servantin.api.controller;

import com.servantin.api.domain.model.BookingStatus;
import com.servantin.api.domain.model.ReportStatus;
import com.servantin.api.domain.model.VerificationStatus;
import com.servantin.api.dto.admin.ProviderDocumentDto;
import com.servantin.api.dto.booking.BookingDto;
import com.servantin.api.dto.common.PageResponse;
import com.servantin.api.dto.provider.ProviderProfileDto;
import com.servantin.api.dto.report.ReportDto;
import com.servantin.api.service.BookingService;
import com.servantin.api.service.DocumentVerificationService;
import com.servantin.api.service.ProviderService;
import com.servantin.api.service.ReportService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative endpoints for managing the platform")
public class AdminController {

    private final ProviderService providerService;
    private final BookingService bookingService;
    private final ReportService reportService;
    private final DocumentVerificationService documentVerificationService;

    // ==================== Provider Management ====================

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

    // ==================== Booking Management ====================

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

    // ==================== Report Management ====================

    @GetMapping("/reports")
    @Operation(summary = "List all reports", description = "Get all reports with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of reports")
    })
    public ResponseEntity<PageResponse<ReportDto>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(reportService.getAllReports(pageRequest));
    }

    @GetMapping("/reports/status/{status}")
    @Operation(summary = "List reports by status", description = "Get reports filtered by status")
    public ResponseEntity<PageResponse<ReportDto>> getReportsByStatus(
            @PathVariable ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(reportService.getReportsByStatus(status, pageRequest));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get report details", description = "Get single report by ID")
    public ResponseEntity<ReportDto> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @PutMapping("/reports/{id}/status")
    @Operation(summary = "Update report status", description = "Update report status and add admin notes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report updated", content = @Content(schema = @Schema(implementation = ReportDto.class)))
    })
    public ResponseEntity<ReportDto> updateReportStatus(
            @PathVariable UUID id,
            @RequestParam ReportStatus status,
            @RequestParam(required = false) String adminNotes,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID adminId = providerService.getUserIdByEmail(userDetails.getUsername());
        return ResponseEntity.ok(reportService.updateReportStatus(id, adminId, status, adminNotes));
    }

    @GetMapping("/reports/statistics")
    @Operation(summary = "Get report statistics", description = "Get counts of reports by status")
    public ResponseEntity<ReportService.ReportStatistics> getReportStatistics() {
        return ResponseEntity.ok(reportService.getReportStatistics());
    }

    // ==================== Document Verification ====================

    @GetMapping("/documents/pending")
    @Operation(summary = "List pending documents", description = "Get all documents awaiting verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of pending documents", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProviderDocumentDto.class))))
    })
    public ResponseEntity<List<ProviderDocumentDto>> getPendingDocuments() {
        return ResponseEntity.ok(documentVerificationService.getPendingDocuments());
    }

    @GetMapping("/documents/status/{status}")
    @Operation(summary = "List documents by status", description = "Get documents filtered by verification status")
    public ResponseEntity<List<ProviderDocumentDto>> getDocumentsByStatus(@PathVariable VerificationStatus status) {
        return ResponseEntity.ok(documentVerificationService.getDocumentsByStatus(status));
    }

    @GetMapping("/documents/provider/{providerProfileId}")
    @Operation(summary = "List provider documents", description = "Get all documents for a specific provider")
    public ResponseEntity<List<ProviderDocumentDto>> getProviderDocuments(@PathVariable UUID providerProfileId) {
        return ResponseEntity.ok(documentVerificationService.getProviderDocuments(providerProfileId));
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "Get document details", description = "Get single document by ID")
    public ResponseEntity<ProviderDocumentDto> getDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(documentVerificationService.getDocumentById(id));
    }

    @PutMapping("/documents/{id}/verify")
    @Operation(summary = "Verify document", description = "Approve or reject a provider document")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document verified", content = @Content(schema = @Schema(implementation = ProviderDocumentDto.class)))
    })
    public ResponseEntity<ProviderDocumentDto> verifyDocument(
            @PathVariable UUID id,
            @RequestParam VerificationStatus status,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID adminId = providerService.getUserIdByEmail(userDetails.getUsername());
        return ResponseEntity.ok(documentVerificationService.verifyDocument(id, adminId, status, notes));
    }

    @GetMapping("/documents/statistics")
    @Operation(summary = "Get document statistics", description = "Get counts of documents by status")
    public ResponseEntity<DocumentVerificationService.DocumentStatistics> getDocumentStatistics() {
        return ResponseEntity.ok(documentVerificationService.getStatistics());
    }

    // ==================== Dashboard Statistics ====================

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get dashboard statistics", description = "Get overall platform statistics for admin dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        ReportService.ReportStatistics reportStats = reportService.getReportStatistics();
        DocumentVerificationService.DocumentStatistics docStats = documentVerificationService.getStatistics();

        return ResponseEntity.ok(Map.of(
                "reports", reportStats,
                "documents", docStats,
                "pendingActions", reportStats.getPending() + docStats.getPending()
        ));
    }
}
