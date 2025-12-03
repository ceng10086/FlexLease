package com.flexlease.product.dto;

public record FileUploadResponse(
        String fileName,
        String fileUrl,
        String contentType,
        long fileSize
) {
}
