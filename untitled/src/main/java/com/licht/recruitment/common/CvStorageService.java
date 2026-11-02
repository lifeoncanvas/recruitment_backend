package com.licht.recruitment.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Stores CV documents outside any public web root and serves them only through
 * authorised controller endpoints. Files are validated by their content signature,
 * not just their extension.
 *
 * Files go to the server disk by default, or to a private S3-compatible bucket when
 * STORAGE_TYPE=s3 (needed on hosts whose disk is wiped on each redeploy).
 */
@Service
public class CvStorageService {

    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};
    private static final byte[] DOC_MAGIC = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};
    private static final byte[] DOCX_MAGIC = {'P', 'K', 0x03, 0x04};
    private static final byte[] JPG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

    private final FileStore fileStore;

    public CvStorageService(@Value("${app.storage.type:local}") String storageType,
                            @Value("${app.storage.upload-dir:uploads}") String uploadDir,
                            @Value("${app.storage.s3.bucket:}") String bucket,
                            @Value("${app.storage.s3.region:auto}") String region,
                            @Value("${app.storage.s3.endpoint:}") String endpoint,
                            @Value("${app.storage.s3.access-key:}") String accessKey,
                            @Value("${app.storage.s3.secret-key:}") String secretKey) {
        this.fileStore = "s3".equalsIgnoreCase(storageType)
                ? new S3FileStore(bucket, region, endpoint, accessKey, secretKey)
                : new LocalFileStore(uploadDir);
    }

    /**
     * Validates and stores the file. Returns the storage key (relative path) to persist on the entity.
     *
     * @throws IllegalArgumentException with a user-facing message when the file is not acceptable
     */
    public String store(MultipartFile file, String folder) throws IOException {
        return store(file, folder, false);
    }

    /**
     * Same as {@link #store(MultipartFile, String)}, optionally also accepting JPG/PNG scans
     * (used for employer compliance documents).
     */
    public String store(MultipartFile file, String folder, boolean allowImages) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please attach your CV document (PDF, DOC, or DOCX).");
        }
        String rawName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cv.pdf";
        String lowerName = rawName.toLowerCase();

        byte[] header = new byte[4];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, 4);
        }
        boolean valid = read == 4 && (
                (lowerName.endsWith(".pdf") && startsWith(header, PDF_MAGIC)) ||
                (lowerName.endsWith(".doc") && startsWith(header, DOC_MAGIC)) ||
                (lowerName.endsWith(".docx") && startsWith(header, DOCX_MAGIC)) ||
                (allowImages && (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) && startsWith(header, JPG_MAGIC)) ||
                (allowImages && lowerName.endsWith(".png") && startsWith(header, PNG_MAGIC)));
        if (!valid) {
            throw new IllegalArgumentException(allowImages
                    ? "Invalid file. Only genuine PDF, DOC, DOCX, JPG, or PNG documents are accepted."
                    : "Invalid file. Only genuine PDF, DOC, and DOCX documents are accepted.");
        }

        String sanitizedName = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = folder + "/" + UUID.randomUUID() + "_" + sanitizedName;
        try (InputStream in = file.getInputStream()) {
            fileStore.save(key, in, file.getSize(), mediaTypeFor(lowerName).toString());
        }
        return key;
    }

    /**
     * Builds a download response for a stored CV, or 404 if it is missing.
     */
    public ResponseEntity<?> download(String storageKey, String downloadName) {
        if (storageKey == null || storageKey.isBlank()) {
            return ResponseEntity.status(404).body(ApiResponse.error("No CV document on file."));
        }
        // Older rows stored a public-looking URL ("/uploads/..."); strip that prefix
        String key = storageKey.startsWith("/uploads/") ? storageKey.substring("/uploads/".length()) : storageKey;
        FileStore.StoredFile stored;
        try {
            stored = fileStore.open(key);
        } catch (IOException e) {
            stored = null;
        }
        if (stored == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("CV document could not be found."));
        }
        String name = downloadName != null ? downloadName.replaceAll("[^a-zA-Z0-9._-]", "_") : "cv";
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(mediaTypeFor(name.toLowerCase()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"");
        if (stored.size() >= 0) {
            response.contentLength(stored.size());
        }
        return response.body(new InputStreamResource(stored.content()));
    }

    private static MediaType mediaTypeFor(String lowerName) {
        return lowerName.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : lowerName.endsWith(".png") ? MediaType.IMAGE_PNG
                : (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) ? MediaType.IMAGE_JPEG
                : MediaType.APPLICATION_OCTET_STREAM;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
