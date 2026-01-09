package com.flexlease.product.dto;

/**
 * 媒体文件上传响应。
 */
public record FileUploadResponse(
        String fileName,
        String fileUrl,
        String contentType,
        long fileSize
) {
}
