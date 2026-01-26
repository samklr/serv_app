package com.servantin.api.service;

import com.servantin.api.domain.entity.Booking;
import com.servantin.api.domain.entity.Report;
import com.servantin.api.domain.entity.User;
import com.servantin.api.domain.model.ReportStatus;
import com.servantin.api.dto.common.PageResponse;
import com.servantin.api.dto.report.CreateReportRequest;
import com.servantin.api.dto.report.ReportDto;
import com.servantin.api.exception.BadRequestException;
import com.servantin.api.exception.ResourceNotFoundException;
import com.servantin.api.repository.BookingRepository;
import com.servantin.api.repository.ReportRepository;
import com.servantin.api.repository.UserRepository;
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
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    /**
     * Create a new report for a user or booking
     */
    @Transactional
    public ReportDto createReport(UUID reporterId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        User reportedUser = null;
        Booking reportedBooking = null;

        if (request.getReportedUserId() != null) {
            reportedUser = userRepository.findById(request.getReportedUserId())
                    .orElseThrow(() -> new RuntimeException("Reported user not found"));
        }

        if (request.getReportedBookingId() != null) {
            reportedBooking = bookingRepository.findById(request.getReportedBookingId())
                    .orElseThrow(() -> new RuntimeException("Reported booking not found"));
        }

        // At least one of user or booking must be specified
        if (reportedUser == null && reportedBooking == null) {
            throw new RuntimeException("Must specify either reported user or booking");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportedBooking(reportedBooking)
                .reportType(request.getReportType())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        report = reportRepository.save(report);
        log.info("User {} created report {} for type {}", reporterId, report.getId(), request.getReportType());

        return toDto(report);
    }

    /**
     * Get all reports for a user (either as reporter or reported)
     */
    @Transactional(readOnly = true)
    public List<ReportDto> getUserReports(UUID userId) {
        List<Report> reports = reportRepository.findByReporterIdOrReportedUserId(userId);
        return reports.stream()
                .map(this::toDto)
                .toList();
    }

    // ==================== Admin Methods ====================

    /**
     * Get all reports with pagination (admin only)
     */
    @Transactional(readOnly = true)
    public PageResponse<ReportDto> getAllReports(Pageable pageable) {
        Page<Report> reports = reportRepository.findAll(pageable);
        return PageResponse.<ReportDto>builder()
                .content(reports.getContent().stream().map(this::toDto).toList())
                .totalElements(reports.getTotalElements())
                .totalPages(reports.getTotalPages())
                .page(reports.getNumber())
                .size(reports.getSize())
                .build();
    }

    /**
     * Get reports by status with pagination (admin only)
     */
    @Transactional(readOnly = true)
    public PageResponse<ReportDto> getReportsByStatus(ReportStatus status, Pageable pageable) {
        Page<Report> reports = reportRepository.findByStatus(status, pageable);
        return PageResponse.<ReportDto>builder()
                .content(reports.getContent().stream().map(this::toDto).toList())
                .totalElements(reports.getTotalElements())
                .totalPages(reports.getTotalPages())
                .page(reports.getNumber())
                .size(reports.getSize())
                .build();
    }

    /**
     * Get single report by ID (admin only)
     */
    @Transactional(readOnly = true)
    public ReportDto getReportById(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
        return toDto(report);
    }

    /**
     * Update report status (admin only)
     */
    @Transactional
    public ReportDto updateReportStatus(UUID reportId, UUID adminId, ReportStatus status, String adminNotes) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user", adminId));

        // Validate status transition
        validateStatusTransition(report.getStatus(), status);

        report.setStatus(status);
        if (adminNotes != null && !adminNotes.isBlank()) {
            report.setAdminNotes(adminNotes);
        }

        // If resolved or dismissed, record who resolved it
        if (status == ReportStatus.RESOLVED || status == ReportStatus.DISMISSED) {
            report.setResolvedBy(admin);
            report.setResolvedAt(Instant.now());
        }

        report = reportRepository.save(report);
        log.info("Admin {} updated report {} status to {}", adminId, reportId, status);

        return toDto(report);
    }

    /**
     * Get statistics for reports (admin dashboard)
     */
    @Transactional(readOnly = true)
    public ReportStatistics getReportStatistics() {
        long pending = reportRepository.findByStatus(ReportStatus.PENDING, Pageable.unpaged()).getTotalElements();
        long investigating = reportRepository.findByStatus(ReportStatus.INVESTIGATING, Pageable.unpaged())
                .getTotalElements();
        long resolved = reportRepository.findByStatus(ReportStatus.RESOLVED, Pageable.unpaged()).getTotalElements();
        long dismissed = reportRepository.findByStatus(ReportStatus.DISMISSED, Pageable.unpaged()).getTotalElements();

        return ReportStatistics.builder()
                .pending(pending)
                .investigating(investigating)
                .resolved(resolved)
                .dismissed(dismissed)
                .total(pending + investigating + resolved + dismissed)
                .build();
    }

    private void validateStatusTransition(ReportStatus current, ReportStatus target) {
        // Allow any transition for now, but log warnings for unusual ones
        if (current == ReportStatus.RESOLVED || current == ReportStatus.DISMISSED) {
            if (target == ReportStatus.PENDING) {
                throw BadRequestException.invalidOperation("Cannot reopen a resolved or dismissed report");
            }
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class ReportStatistics {
        private long pending;
        private long investigating;
        private long resolved;
        private long dismissed;
        private long total;
    }

    private ReportDto toDto(Report report) {
        return ReportDto.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getName())
                .reporterEmail(report.getReporter().getEmail())
                .reportedUserId(report.getReportedUser() != null ? report.getReportedUser().getId() : null)
                .reportedUserName(report.getReportedUser() != null ? report.getReportedUser().getName() : null)
                .reportedBookingId(report.getReportedBooking() != null ? report.getReportedBooking().getId() : null)
                .reportType(report.getReportType())
                .description(report.getDescription())
                .status(report.getStatus())
                .adminNotes(report.getAdminNotes())
                .resolvedById(report.getResolvedBy() != null ? report.getResolvedBy().getId() : null)
                .resolvedByName(report.getResolvedBy() != null ? report.getResolvedBy().getName() : null)
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
