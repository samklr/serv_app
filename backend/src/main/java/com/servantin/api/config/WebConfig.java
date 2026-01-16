package com.servantin.api.config;

import com.servantin.api.security.EmailVerificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for interceptors and CORS.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final EmailVerificationInterceptor emailVerificationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(emailVerificationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",           // Don't block authentication endpoints
                        "/api/categories/**",      // Public category browsing
                        "/api/providers/match",    // Public provider search
                        "/api/providers/{id}",     // Public provider details
                        "/api/health",             // Health check
                        "/api/legal/**"            // Legal pages
                );
    }
}
