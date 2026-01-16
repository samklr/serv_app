package com.servantin.api.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud Platform Storage configuration.
 * Configures the GCP Storage client bean for file uploads (provider photos, documents).
 */
@Slf4j
@Configuration
public class GcpStorageConfig {

    @Value("${gcp.storage.project-id}")
    private String projectId;

    /**
     * Creates and configures the GCP Storage client bean.
     * The client is used by StorageService to upload and manage files in Cloud Storage.
     *
     * Authentication is handled via Application Default Credentials (ADC):
     * - Locally: Uses gcloud CLI credentials or GOOGLE_APPLICATION_CREDENTIALS env var
     * - Cloud Run: Automatically uses service account attached to the Cloud Run service
     *
     * @return configured Storage instance
     */
    @Bean
    public Storage gcpStorage() {
        log.info("Initializing GCP Storage client for project: {}", projectId);

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
}
