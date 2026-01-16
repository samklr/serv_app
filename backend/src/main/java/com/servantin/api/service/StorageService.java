package com.servantin.api.service;

import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing file uploads to Google Cloud Storage.
 * Handles provider profile photos and verification documents with validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final Storage storage;
    private final Tika tika = new Tika();

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Value("${upload.max-file-size}")
    private long maxFileSize;

    @Value("${upload.allowed-image-types}")
    private String allowedImageTypes;

    @Value("${upload.allowed-document-types}")
    private String allowedDocumentTypes;

    /**
     * Upload file to GCS with validation.
     *
     * @param file file to upload
     * @param folder folder path within bucket (e.g., "profiles/userId")
     * @param allowedTypes list of allowed MIME types
     * @return GCS URI (gs://bucket/path)
     * @throws IOException if file read fails
     * @throws RuntimeException if validation fails
     */
    public String uploadFile(MultipartFile file, String folder, List<String> allowedTypes) throws IOException {
        // Validate file
        validateFile(file, allowedTypes);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = folder + "/" + UUID.randomUUID() + extension;

        // Detect MIME type using Tika (more reliable than file.getContentType())
        String mimeType = tika.detect(file.getBytes());

        // Upload to GCS
        BlobId blobId = BlobId.of(bucketName, filename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(mimeType)
                .build();

        Blob blob = storage.create(blobInfo, file.getBytes());

        log.info("File uploaded successfully: {} (size: {} bytes, type: {})",
                filename, file.getSize(), mimeType);

        // Return GCS URI
        return String.format("gs://%s/%s", bucketName, filename);
    }

    /**
     * Upload profile photo for a user.
     *
     * @param file photo file (JPEG/PNG)
     * @param userId user ID
     * @return GCS URI
     * @throws IOException if upload fails
     */
    public String uploadProfilePhoto(MultipartFile file, UUID userId) throws IOException {
        List<String> allowed = Arrays.asList(allowedImageTypes.split(","));
        String folder = "profiles/" + userId;
        return uploadFile(file, folder, allowed);
    }

    /**
     * Upload provider verification document.
     *
     * @param file document file (PDF/JPEG/PNG)
     * @param providerId provider user ID
     * @return GCS URI
     * @throws IOException if upload fails
     */
    public String uploadProviderDocument(MultipartFile file, UUID providerId) throws IOException {
        List<String> allowed = Arrays.asList(allowedDocumentTypes.split(","));
        String folder = "documents/" + providerId;
        return uploadFile(file, folder, allowed);
    }

    /**
     * Generate signed URL for private file access.
     * URL is valid for 15 minutes.
     *
     * @param gcsUrl GCS URI (gs://bucket/path)
     * @return temporary signed URL
     */
    public String generateSignedUrl(String gcsUrl) {
        // Extract blob name from gs://bucket/path format
        if (!gcsUrl.startsWith("gs://")) {
            throw new RuntimeException("Invalid GCS URL format: " + gcsUrl);
        }

        String blobName = gcsUrl.replace("gs://" + bucketName + "/", "");

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, blobName).build();

        try {
            URL signedUrl = storage.signUrl(
                    blobInfo,
                    15, // 15 minutes
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.withV4Signature()
            );

            log.debug("Generated signed URL for: {} (valid for 15 minutes)", blobName);
            return signedUrl.toString();

        } catch (Exception e) {
            log.error("Failed to generate signed URL for {}: {}", gcsUrl, e.getMessage());
            throw new RuntimeException("Failed to generate signed URL", e);
        }
    }

    /**
     * Delete file from GCS.
     *
     * @param gcsUrl GCS URI (gs://bucket/path)
     */
    public void deleteFile(String gcsUrl) {
        try {
            if (!gcsUrl.startsWith("gs://")) {
                log.warn("Invalid GCS URL format, cannot delete: {}", gcsUrl);
                return;
            }

            String blobName = gcsUrl.replace("gs://" + bucketName + "/", "");
            BlobId blobId = BlobId.of(bucketName, blobName);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.info("File deleted successfully: {}", blobName);
            } else {
                log.warn("File not found for deletion: {}", blobName);
            }
        } catch (Exception e) {
            log.error("Failed to delete file {}: {}", gcsUrl, e.getMessage());
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Validate file size and MIME type.
     *
     * @param file file to validate
     * @param allowedTypes list of allowed MIME types
     * @throws IOException if file read fails
     * @throws RuntimeException if validation fails
     */
    private void validateFile(MultipartFile file, List<String> allowedTypes) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            long maxSizeMB = maxFileSize / 1024 / 1024;
            throw new RuntimeException(
                    String.format("File size exceeds maximum allowed size of %d MB", maxSizeMB));
        }

        // Detect and validate MIME type using Apache Tika
        String mimeType = tika.detect(file.getBytes());
        if (!allowedTypes.contains(mimeType)) {
            throw new RuntimeException(
                    String.format("File type not allowed. Detected: %s. Allowed: %s",
                            mimeType, String.join(", ", allowedTypes)));
        }

        log.debug("File validation passed: {} ({}, {} bytes)",
                file.getOriginalFilename(), mimeType, file.getSize());
    }

    /**
     * Check if file exists in GCS.
     *
     * @param gcsUrl GCS URI
     * @return true if file exists
     */
    public boolean fileExists(String gcsUrl) {
        try {
            if (!gcsUrl.startsWith("gs://")) {
                return false;
            }

            String blobName = gcsUrl.replace("gs://" + bucketName + "/", "");
            BlobId blobId = BlobId.of(bucketName, blobName);
            Blob blob = storage.get(blobId);

            return blob != null && blob.exists();
        } catch (Exception e) {
            log.error("Error checking file existence for {}: {}", gcsUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Get file metadata from GCS.
     *
     * @param gcsUrl GCS URI
     * @return Blob object with metadata, or null if not found
     */
    public Blob getFileMetadata(String gcsUrl) {
        try {
            if (!gcsUrl.startsWith("gs://")) {
                return null;
            }

            String blobName = gcsUrl.replace("gs://" + bucketName + "/", "");
            BlobId blobId = BlobId.of(bucketName, blobName);

            return storage.get(blobId);
        } catch (Exception e) {
            log.error("Error getting file metadata for {}: {}", gcsUrl, e.getMessage());
            return null;
        }
    }
}
