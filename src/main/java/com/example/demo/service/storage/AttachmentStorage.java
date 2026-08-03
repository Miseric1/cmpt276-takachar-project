package com.example.demo.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentStorage {
    String store(MultipartFile file);
    Resource load(String storageKey);
    void delete(String storageKey);
}
