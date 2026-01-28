package com.servantin.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * AWS configuration for Simple Email Service (SES).
 * Configures the SES client bean for sending transactional emails.
 * 
 * When AWS credentials are not configured, a null client is created
 * and EmailService will log emails instead of sending them.
 */
@Slf4j
@Configuration
public class AwsConfig {

    @Value("${aws.ses.region:us-east-1}")
    private String region;

    @Value("${aws.ses.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.ses.secret-access-key:}")
    private String secretAccessKey;

    /**
     * Creates and configures the AWS SES client bean.
     * The client is used by EmailService to send transactional emails.
     * 
     * If credentials are not provided or are empty/dummy values,
     * returns null and EmailService should handle this gracefully.
     *
     * @return configured SesClient instance or null if not configured
     */
    @Bean
    @Primary
    public SesClient sesClient() {
        // Check if credentials are properly configured
        if (!isCredentialsConfigured()) {
            log.warn("AWS SES credentials not configured - email sending will be disabled");
            log.warn("Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY to enable email");
            return null;
        }

        log.info("Initializing AWS SES client for region: {}", region);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    /**
     * Check if AWS credentials are properly configured.
     * Returns false for empty, null, or dummy placeholder values.
     */
    private boolean isCredentialsConfigured() {
        if (accessKeyId == null || accessKeyId.isBlank()) {
            return false;
        }
        if (secretAccessKey == null || secretAccessKey.isBlank()) {
            return false;
        }
        // Check for known dummy/placeholder values
        if (accessKeyId.equals("dummy-access-key") ||
                accessKeyId.startsWith("YOUR_") ||
                accessKeyId.equals("AKIA_PLACEHOLDER")) {
            return false;
        }
        return true;
    }
}
