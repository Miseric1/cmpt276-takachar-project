package com.example.demo.service;

import org.springframework.core.io.Resource;

public record AttachmentDownload(Resource resource, String filename, String contentType, long sizeBytes) {
}
