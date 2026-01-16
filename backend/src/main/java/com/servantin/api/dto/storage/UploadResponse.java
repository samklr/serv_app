package com.servantin.api.dto.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for file upload operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    /**
     * GCS URL (gs://bucket/path) - internal storage reference
     */
    private String url;

    /**
     * Original filename
     */
    private String fileName;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * MIME type detected
     */
    private String mimeType;
}
