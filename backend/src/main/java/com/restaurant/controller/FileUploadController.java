package com.restaurant.controller;

import com.restaurant.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Admin-only endpoint for uploading menu item and category images.
 * Uploaded files are saved to the local filesystem under the configured
 * upload directory and served as static resources via /uploads/**.
 *
 * FileUploadService validates both the declared content-type and the
 * actual file magic bytes to prevent spoofed uploads.
 */
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    /**
     * Accepts a multipart image file and returns its public URL.
     * Responds with 400 if the file is empty or not an allowed image type.
     * Responds with 500 if the file cannot be written to disk.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }
        try {
            String url = fileUploadService.save(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to save file"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
