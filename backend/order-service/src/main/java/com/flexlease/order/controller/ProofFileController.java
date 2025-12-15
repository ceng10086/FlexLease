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
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
            .contentLength(file.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(file.resource());
    }
}
