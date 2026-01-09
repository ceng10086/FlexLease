package com.flexlease.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层扩展点（预留）。
 * <p>
 * 当前没有额外的 MVC 自定义配置，但保留该类便于后续统一扩展（如 CORS、格式化器等）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
