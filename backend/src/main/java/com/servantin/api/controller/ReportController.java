package com.servantin.api.controller;

import com.servantin.api.dto.report.CreateReportRequest;
import com.servantin.api.dto.report.ReportDto;
import com.servantin.api.security.CurrentUserService;
import com.servantin.api.service.ReportService;
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
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Trust & Safety - Report users or bookings")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Create a report", description = "Report a user or booking for inappropriate behavior")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report created", content = @Content(schema = @Schema(implementation = ReportDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ReportDto> createReport(@Valid @RequestBody CreateReportRequest request) {
        UUID reporterId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(reportService.createReport(reporterId, request));
    }

    @GetMapping("/my-reports")
    @Operation(summary = "Get my reports", description = "Get all reports created by or about the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reports list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReportDto.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<ReportDto>> getMyReports() {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(reportService.getUserReports(userId));
    }
}
