package com.servantin.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Map;

/**
 * Email service for sending transactional emails via AWS SES.
 * Handles all 13 notification touchpoints identified in the application.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesClient sesClient;
    private final TemplateEngine templateEngine;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.from-name}")
    private String fromName;

    @Value("${app.url.frontend}")
    private String frontendUrl;

    /**
     * Send HTML email using Thymeleaf template.
     *
     * @param to recipient email address
     * @param subject email subject
     * @param templateName Thymeleaf template name (without .html extension)
     * @param variables template variables
     */
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // Add common variables available to all templates
            variables.put("frontendUrl", frontendUrl);

            // Process template
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process("email/" + templateName, context);

            // Build email request
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(String.format("%s <%s>", fromName, fromEmail))
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();

            // Send email
            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("Email sent successfully to {} - MessageId: {}", to, response.messageId());

        } catch (SesException e) {
            log.error("Failed to send email to {}: {}", to, e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    // ==================== Notification Methods ====================

    /**
     * 1. Send email verification link to new user
     */
    public void sendVerificationEmail(String to, String name, String verificationToken) {
        String verificationLink = frontendUrl + "/auth/verify-email?token=" + verificationToken;
        Map<String, Object> variables = Map.of(
                "name", name,
                "verificationLink", verificationLink
        );
        sendEmail(to, "Verify Your Email - Servantin", "verification", variables);
        log.info("Verification email sent to {}", to);
    }

    /**
     * 2. Send password reset link
     */
    public void sendPasswordResetEmail(String to, String name, String resetToken) {
        String resetLink = frontendUrl + "/auth/reset-password?token=" + resetToken;
        Map<String, Object> variables = Map.of(
                "name", name,
                "resetLink", resetLink
        );
        sendEmail(to, "Password Reset Request - Servantin", "password-reset", variables);
        log.info("Password reset email sent to {}", to);
    }

    /**
     * 3. Send welcome email after registration
     */
    public void sendWelcomeEmail(String to, String name, boolean isProvider) {
        Map<String, Object> variables = Map.of(
                "name", name,
                "isProvider", isProvider,
                "dashboardLink", isProvider ? frontendUrl + "/provider/dashboard" : frontendUrl + "/dashboard"
        );
        sendEmail(to, "Welcome to Servantin!", "welcome", variables);
        log.info("Welcome email sent to {} (provider: {})", to, isProvider);
    }

    /**
     * 4. Notify client that booking request was created
     */
    public void sendBookingRequestedToClient(String to, String clientName, String bookingId, String categoryName) {
        Map<String, Object> variables = Map.of(
                "clientName", clientName,
                "bookingId", bookingId,
                "categoryName", categoryName,
                "bookingLink", frontendUrl + "/dashboard/bookings/" + bookingId
        );
        sendEmail(to, "Your Service Request Has Been Submitted - Servantin", "booking-requested-client", variables);
        log.info("Booking request confirmation sent to client {}", to);
    }

    /**
     * 5. Notify provider of new booking request
     */
    public void sendBookingRequestedToProvider(String to, String providerName, String bookingId,
                                               String clientName, String categoryName, String description) {
        Map<String, Object> variables = Map.of(
                "providerName", providerName,
                "bookingId", bookingId,
                "clientName", clientName,
                "categoryName", categoryName,
                "description", description.length() > 200 ? description.substring(0, 200) + "..." : description,
                "bookingLink", frontendUrl + "/provider/dashboard"
        );
        sendEmail(to, "New Service Request Received - Servantin", "booking-requested-provider", variables);
        log.info("New booking request notification sent to provider {}", to);
    }

    /**
     * 6. Notify client that provider accepted their booking
     */
    public void sendBookingAccepted(String to, String clientName, String providerName, String bookingId) {
        Map<String, Object> variables = Map.of(
                "clientName", clientName,
                "providerName", providerName,
                "bookingId", bookingId,
                "bookingLink", frontendUrl + "/dashboard/bookings/" + bookingId
        );
        sendEmail(to, "Service Request Accepted! - Servantin", "booking-accepted", variables);
        log.info("Booking accepted notification sent to client {}", to);
    }

    /**
     * 7. Notify client that provider declined their booking
     */
    public void sendBookingDeclined(String to, String clientName, String providerName, String bookingId) {
        Map<String, Object> variables = Map.of(
                "clientName", clientName,
                "providerName", providerName,
                "bookingId", bookingId,
                "bookingLink", frontendUrl + "/dashboard/bookings/" + bookingId,
                "searchLink", frontendUrl + "/book"
        );
        sendEmail(to, "Service Request Update - Servantin", "booking-declined", variables);
        log.info("Booking declined notification sent to client {}", to);
    }

    /**
     * 8. Notify both parties that booking is completed
     */
    public void sendBookingCompleted(String to, String recipientName, String bookingId, boolean isProvider) {
        Map<String, Object> variables = Map.of(
                "recipientName", recipientName,
                "bookingId", bookingId,
                "isProvider", isProvider,
                "bookingLink", frontendUrl + (isProvider ? "/provider/dashboard" : "/dashboard/bookings/" + bookingId)
        );
        String subject = isProvider ? "Service Completed - Servantin" : "Please Rate Your Experience - Servantin";
        sendEmail(to, subject, "booking-completed", variables);
        log.info("Booking completed notification sent to {} (provider: {})", to, isProvider);
    }

    /**
     * 9. Notify parties that booking was canceled
     */
    public void sendBookingCanceled(String to, String recipientName, String bookingId) {
        Map<String, Object> variables = Map.of(
                "recipientName", recipientName,
                "bookingId", bookingId,
                "bookingLink", frontendUrl + "/dashboard"
        );
        sendEmail(to, "Service Request Canceled - Servantin", "booking-canceled", variables);
        log.info("Booking canceled notification sent to {}", to);
    }

    /**
     * 10. Notify recipient of new message in booking conversation
     */
    public void sendNewMessageNotification(String to, String recipientName, String senderName,
                                          String bookingId, String messagePreview) {
        Map<String, Object> variables = Map.of(
                "recipientName", recipientName,
                "senderName", senderName,
                "messagePreview", messagePreview,
                "bookingId", bookingId,
                "bookingLink", frontendUrl + "/dashboard/bookings/" + bookingId
        );
        sendEmail(to, "New Message from " + senderName + " - Servantin", "new-message", variables);
        log.info("New message notification sent to {} from {}", to, senderName);
    }

    /**
     * 11. Notify provider they received a new rating
     */
    public void sendRatingReceived(String to, String providerName, int rating, String bookingId) {
        Map<String, Object> variables = Map.of(
                "providerName", providerName,
                "rating", rating,
                "bookingId", bookingId,
                "bookingLink", frontendUrl + "/provider/dashboard",
                "ratingStars", "★".repeat(rating) + "☆".repeat(5 - rating)
        );
        sendEmail(to, "You Received a New Rating - Servantin", "rating-received", variables);
        log.info("Rating received notification sent to provider {} (rating: {})", to, rating);
    }

    /**
     * 12. Notify provider their profile was verified
     */
    public void sendProviderVerified(String to, String providerName) {
        Map<String, Object> variables = Map.of(
                "providerName", providerName,
                "dashboardLink", frontendUrl + "/provider/dashboard",
                "profileLink", frontendUrl + "/provider/profile"
        );
        sendEmail(to, "Your Provider Profile Has Been Verified! - Servantin", "provider-verified", variables);
        log.info("Provider verification success email sent to {}", to);
    }

    /**
     * 13. Notify provider their verification was rejected
     */
    public void sendProviderRejected(String to, String providerName, String reason) {
        Map<String, Object> variables = Map.of(
                "providerName", providerName,
                "reason", reason != null ? reason : "Please review your documents and try again.",
                "supportLink", frontendUrl + "/support",
                "profileLink", frontendUrl + "/provider/profile"
        );
        sendEmail(to, "Provider Verification Update - Servantin", "provider-rejected", variables);
        log.info("Provider verification rejection email sent to {}", to);
    }
}
