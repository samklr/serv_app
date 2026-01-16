package com.servantin.api.security;

import com.servantin.api.domain.entity.User;
import com.servantin.api.domain.model.UserRole;
import com.servantin.api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to enforce email verification for provider profile operations.
 *
 * Soft verification strategy:
 * - All users can browse and use the platform after registration
 * - Providers CANNOT create or edit their profile until email is verified
 * - Adds X-Email-Verified header to all responses for frontend warning banners
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Skip if not authenticated
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        // Get user email from authentication
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return true;
        }

        // Add email verification status to response header for frontend
        response.setHeader("X-Email-Verified", user.getEmailVerified() ? "true" : "false");

        // Block provider profile creation/updates if email not verified
        if (user.getRole() == UserRole.PROVIDER && !user.getEmailVerified()) {
            String requestURI = request.getRequestURI();
            String method = request.getMethod();

            // Block PUT/POST to provider profile endpoints
            if ((method.equals("PUT") || method.equals("POST")) &&
                (requestURI.contains("/api/providers/profile") && !requestURI.contains("/photo") && !requestURI.contains("/documents"))) {

                log.warn("Blocked unverified provider {} from creating/updating profile", email);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Email verification required\", \"message\": \"You must verify your email before creating or editing your provider profile. Please check your inbox for the verification link.\"}");
                return false;
            }
        }

        return true;
    }
}
