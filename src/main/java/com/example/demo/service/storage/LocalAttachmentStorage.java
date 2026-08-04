package com.example.demo.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalAttachmentStorage implements AttachmentStorage {
    private final Path root;

    public LocalAttachmentStorage(@Value("${app.ticketing.upload-dir:./data/ticket-uploads}") String uploadDir) {
        try {
            this.root = Path.of(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize ticket attachment storage", ex);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        extension = extension == null ? "" : extension.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        if (extension.length() > 10) extension = extension.substring(0, 10);
        String key = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        Path target = safePath(key);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store ticket attachment", ex);
        }
    }

    @Override
    public Resource load(String storageKey) {
        try {
            Resource resource = new UrlResource(safePath(storageKey).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Attachment content is unavailable");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalStateException("Attachment content is unavailable", ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(safePath(storageKey));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete ticket attachment", ex);
        }
    }

    private Path safePath(String key) {
        Path candidate = root.resolve(key).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Invalid attachment storage key");
        }
        return candidate;
    }
}
