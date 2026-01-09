package com.flexlease.order.controller;

import com.flexlease.order.service.OrderProofService;
import com.flexlease.order.service.OrderProofService.ProofFileResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 取证文件下载接口（鉴权）。
 * <p>
 * 下载前会通过订单归属校验访问权限，返回 {@code inline} 以便前端预览。
 */
@RestController
@RequestMapping("/api/v1/proofs")
public class ProofFileController {

    private final OrderProofService orderProofService;

    public ProofFileController(OrderProofService orderProofService) {
        this.orderProofService = orderProofService;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) {
        ProofFileResource file = orderProofService.loadProofFile(fileName);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"");
        if (file.fileSize() > 0) {
            builder.contentLength(file.fileSize());
        }
        return builder.body(file.resource());
    }
}
