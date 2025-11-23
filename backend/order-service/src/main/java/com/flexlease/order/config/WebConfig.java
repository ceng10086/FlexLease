package com.flexlease.order.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ProofStorageProperties proofStorageProperties;

    public WebConfig(ProofStorageProperties proofStorageProperties) {
        this.proofStorageProperties = proofStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path location = Path.of(proofStorageProperties.getRoot()).toAbsolutePath().normalize();
        registry.addResourceHandler("/proofs/**")
                .addResourceLocations("file:" + location + "/")
                .setCachePeriod(3600);
    }
}
