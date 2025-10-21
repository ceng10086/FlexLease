# FlexLease 智能共享租赁平台

## 简介
FlexLease 是一个面向 B2C 模式的共享租赁平台，支持厂商入驻、商品租赁、订单履约与支付结算等完整业务流程。本仓库当前处于 Iteration 0 阶段，完成了基础骨架与环境搭建。

## 当前进展
- ✅ 项目规划文档与数据库/API 设计初稿（见 `docs/`）
- ✅ 多模块 Spring Boot 微服务骨架（见 `backend/`）
- ✅ 认证中心支持用户注册、登录与 JWT 发放（Iteration 1）
- ✅ 用户服务支持厂商入驻申请与审批（Iteration 1）
- ✅ PostgreSQL schema 迁移基线与迭代脚本（见 `db/`）
- ✅ GitHub Actions CI 草稿（待补充）

## 快速开始
```powershell
# 在仓库根目录
mvn -pl backend/auth-service spring-boot:run
```
> 各服务 `application.yml` 默认端口：Auth 9001、User 9002、Product 9003、Order 9004、Payment 9005、Notification 9006、Gateway 8080。

## 目录结构
```
backend/            后端微服务源码
  platform-common/  公共依赖与 DTO
  auth-service/     认证授权服务
  user-service/     用户与厂商服务
  product-service/  商品与租赁方案服务
  order-service/    订单履约服务
  payment-service/  支付结算服务
  notification-service/ 通知与消息服务
  gateway-service/  Spring Cloud Gateway 网关

db/                 数据库迁移脚本
.docs/              需求与设计文档
```

## 后续计划
详见 `docs/项目规划与任务分解.md`，下一迭代将聚焦订单与支付业务流程、跨服务调用与持续交付优化。
