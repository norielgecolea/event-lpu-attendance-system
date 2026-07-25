package org.nors.dev.codes.lpu.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nors.dev.codes.lpu.config.UploadProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PhotoStorageService {

    private static final Logger log = LogManager.getLogger(PhotoStorageService.class);
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path picturesDir;

    public PhotoStorageService(UploadProperties uploadProperties) {
        this.picturesDir = Paths.get(uploadProperties.getPicturesDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.picturesDir);
            log.info("Event pictures directory: {}", this.picturesDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create pictures directory: " + this.picturesDir, ex);
        }
    }

    /** Saves an image and returns a public path like {@code /pictures/uuid.jpg}. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo file is required");
        }

        String contentType = resolveContentType(file);
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPEG, PNG, WebP, or GIF images are allowed"
            );
        }

        String filename = UUID.randomUUID() + extensionFor(contentType, file.getOriginalFilename());
        Path destination = picturesDir.resolve(filename).normalize();
        if (!destination.startsWith(picturesDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }

        try {
            file.transferTo(destination);
        } catch (IOException ex) {
            log.error("Failed to store photo {}", filename, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store photo");
        }

        log.info("Stored event photo {}", filename);
        return "/pictures/" + filename;
    }

    public void deleteIfManaged(String photoPath) {
        if (photoPath == null || photoPath.isBlank() || !photoPath.startsWith("/pictures/")) {
            return;
        }
        String filename = photoPath.substring("/pictures/".length());
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return;
        }
        Path file = picturesDir.resolve(filename).normalize();
        if (!file.startsWith(picturesDir)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("Unable to delete old photo {}: {}", filename, ex.toString());
        }
    }

    private static String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return null;
    }

    private static String extensionFor(String contentType, String originalFilename) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> {
                if (originalFilename != null && originalFilename.toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
                    yield ".jpeg";
                }
                yield ".jpg";
            }
        };
    }
}
