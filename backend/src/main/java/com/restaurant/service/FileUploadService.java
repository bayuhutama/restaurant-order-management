package com.restaurant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public String save(MultipartFile file) throws IOException {
        // Validate declared content-type first
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Only image files are allowed (jpeg, png, gif, webp)");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File size must not exceed 10 MB");
        }

        // Validate actual file content via magic bytes — prevents spoofed content-type
        String detectedType = detectMimeType(file.getInputStream());
        if (!ALLOWED_TYPES.contains(detectedType)) {
            throw new RuntimeException("File content does not match an allowed image type");
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        // Extension is derived from validated content, never from user-supplied filename
        String extension = switch (detectedType) {
            case "image/png"  -> ".png";
            case "image/gif"  -> ".gif";
            case "image/webp" -> ".webp";
            default           -> ".jpg";
        };
        String filename = UUID.randomUUID() + extension;

        // Resolve and verify the target path stays inside uploadDir (path traversal guard)
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new RuntimeException("Invalid file path");
        }

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return baseUrl + "/uploads/" + filename;
    }

    /**
     * Reads the first 12 bytes of the stream and identifies the image type by magic bytes.
     * This cannot be spoofed via HTTP headers or filename manipulation.
     */
    private String detectMimeType(InputStream in) throws IOException {
        byte[] header = new byte[12];
        int read = in.read(header);
        if (read < 4) {
            throw new RuntimeException("File is too small to be a valid image");
        }

        // JPEG: FF D8 FF
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G') {
            return "image/png";
        }
        // GIF: 47 49 46 38 (GIF8)
        if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
            return "image/gif";
        }
        // WebP: RIFF????WEBP (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
        if (read >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "image/webp";
        }

        return "application/octet-stream";
    }
}
