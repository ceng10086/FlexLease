package com.flexlease.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关启动入口（Spring Cloud Gateway）。
 *
 * <p>本项目中网关只负责路由转发与基础跨域；JWT 校验由各业务微服务各自完成。</p>
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
