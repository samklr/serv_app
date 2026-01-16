package com.servantin.api.service;

import com.servantin.api.domain.entity.Booking;
import com.servantin.api.domain.entity.Report;
import com.servantin.api.domain.entity.User;
import com.servantin.api.domain.model.ReportStatus;
import com.servantin.api.dto.report.CreateReportRequest;
import com.servantin.api.dto.report.ReportDto;
import com.servantin.api.repository.BookingRepository;
import com.servantin.api.repository.ReportRepository;
import com.servantin.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        List<Report> reports = reportRepository.findByReporterIdOrReportedUserId(userId, userId);
        return reports.stream()
                .map(this::toDto)
                .toList();
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
