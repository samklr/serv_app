package com.servantin.api.service;

import com.servantin.api.domain.entity.User;
import com.servantin.api.domain.model.UserRole;
import com.servantin.api.dto.auth.AuthResponse;
import com.servantin.api.dto.auth.LoginRequest;
import com.servantin.api.dto.auth.RegisterRequest;
import com.servantin.api.repository.UserRepository;
import com.servantin.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Determine role
        UserRole role = request.isRegisterAsProvider() ? UserRole.PROVIDER : UserRole.CLIENT;

        // Generate verification token (24-hour expiry)
        String verificationToken = java.util.UUID.randomUUID().toString();
        java.time.Instant verificationExpiry = java.time.Instant.now().plus(24, java.time.temporal.ChronoUnit.HOURS);

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .role(role)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(verificationExpiry)
                .acceptedTermsAt(java.time.Instant.now()) // User accepted terms during registration
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: {} with role {}", user.getEmail(), role);

        // Send verification and welcome emails (don't fail registration if email fails)
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);
            emailService.sendWelcomeEmail(user.getEmail(), user.getName(), role == UserRole.PROVIDER);
        } catch (Exception e) {
            log.error("Failed to send registration emails to {}: {}", user.getEmail(), e.getMessage());
        }

        // Generate token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole())
                        .hasProviderProfile(false)
                        .emailVerified(false)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // Get user
        User user = userRepository.findByEmailWithProfile(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User logged in: {}", user.getEmail());

        // Generate token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        boolean hasProviderProfile = user.getProviderProfile() != null;

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole())
                        .hasProviderProfile(hasProviderProfile)
                        .emailVerified(user.getEmailVerified())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmailWithProfile(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasProviderProfile = user.getProviderProfile() != null;

        return AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .hasProviderProfile(hasProviderProfile)
                .emailVerified(user.getEmailVerified())
                .build();
    }

    // ==================== Email Verification Methods ====================

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (user.getVerificationTokenExpiry().isBefore(java.time.Instant.now())) {
            throw new RuntimeException("Verification token has expired");
        }

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        // Generate new verification token (24-hour expiry)
        String verificationToken = java.util.UUID.randomUUID().toString();
        java.time.Instant verificationExpiry = java.time.Instant.now().plus(24, java.time.temporal.ChronoUnit.HOURS);

        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(verificationExpiry);
        userRepository.save(user);

        // Send email
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);
        log.info("Resent verification email to: {}", user.getEmail());
    }

    // ==================== Password Reset Methods ====================

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate reset token (1-hour expiry)
        String resetToken = java.util.UUID.randomUUID().toString();
        java.time.Instant resetExpiry = java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.HOURS);

        user.setResetToken(resetToken);
        user.setResetTokenExpiry(resetExpiry);
        userRepository.save(user);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetToken);
        log.info("Password reset requested for: {}", user.getEmail());
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (user.getResetTokenExpiry().isBefore(java.time.Instant.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset completed for: {}", user.getEmail());
    }
}
