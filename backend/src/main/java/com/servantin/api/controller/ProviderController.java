package com.servantin.api.controller;

import com.servantin.api.dto.provider.*;
import com.servantin.api.security.CurrentUserService;
import com.servantin.api.service.ProviderService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "Provider profile and matching endpoints")
public class ProviderController {

    private final ProviderService providerService;
    private final CurrentUserService currentUserService;

    @PostMapping("/match")
    @Operation(summary = "Match providers", description = "Find providers matching category, location, and optionally time. Returns sorted list with verified providers first.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Matching providers", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProviderMatchDto.class))))
    })
    public ResponseEntity<List<ProviderMatchDto>> matchProviders(@Valid @RequestBody MatchRequest request) {
        return ResponseEntity.ok(providerService.matchProviders(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get provider by ID", description = "Get full provider profile details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provider details", content = @Content(schema = @Schema(implementation = ProviderProfileDto.class))),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    public ResponseEntity<ProviderProfileDto> getProvider(@PathVariable UUID id) {
        return ResponseEntity.ok(providerService.getProviderProfileById(id));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
    @Operation(summary = "Get own profile", description = "Get the current provider's own profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Provider profile", content = @Content(schema = @Schema(implementation = ProviderProfileDto.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ProviderProfileDto> getOwnProfile() {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(providerService.getProviderProfile(userId));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN', 'CLIENT')")
    @Operation(summary = "Create or update profile", description = "Create or update the provider profile. If user is CLIENT, they will be upgraded to PROVIDER role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated provider profile", content = @Content(schema = @Schema(implementation = ProviderProfileDto.class)))
    })
    public ResponseEntity<ProviderProfileDto> createOrUpdateProfile(
            @Valid @RequestBody ProviderProfileRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(providerService.createOrUpdateProfile(userId, request));
    }
}
