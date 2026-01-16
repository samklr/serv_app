package com.servantin.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

/**
 * AWS configuration for Simple Email Service (SES).
 * Configures the SES client bean for sending transactional emails.
 */
@Slf4j
@Configuration
public class AwsConfig {

    @Value("${aws.ses.region}")
    private String region;

    @Value("${aws.ses.access-key-id}")
    private String accessKeyId;

    @Value("${aws.ses.secret-access-key}")
    private String secretAccessKey;

    /**
     * Creates and configures the AWS SES client bean.
     * The client is used by EmailService to send transactional emails.
     *
     * @return configured SesClient instance
     */
    @Bean
    public SesClient sesClient() {
        log.info("Initializing AWS SES client for region: {}", region);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
