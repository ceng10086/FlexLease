# FlexLease 智能共享租赁平台

## 简介
FlexLease 是一个面向 B2C 模式的共享租赁平台，支持厂商入驻、商品租赁、订单履约与支付结算等完整业务流程。本仓库当前处于 Iteration 1 阶段，已完成基础骨架、认证与厂商入驻等首批能力。

## 当前进展
- ✅ 项目规划文档与数据库/API 设计初稿（见 `docs/`）
- ✅ 多模块 Spring Boot 微服务骨架（Iteration 0，见 `backend/`）
- ✅ 认证中心支持用户注册、登录与 JWT 发放（Iteration 1）
- ✅ 用户服务支持厂商入驻申请与审批（Iteration 1）
- ✅ 前端管理端完成登录与基础仪表盘骨架（Iteration 1）
- ✅ 厂商审核通过后自动激活账号（User Service → Auth Service 内部 API）
- ✅ 前端管理端完成登录与基础仪表盘骨架（Iteration 1）
- ✅ PostgreSQL schema 迁移基线与迭代脚本（见 `db/`）
- ✅ GitHub Actions CI 草稿（待补充）

## 快速开始
```powershell
# 在仓库根目录打开两个 PowerShell 终端
mvn -pl backend/auth-service spring-boot:run   # 终端 A，端口 9001
mvn -pl backend/user-service spring-boot:run   # 终端 B，端口 9002
```
> 各服务 `application.yml` 默认端口：Auth 9001、User 9002、Product 9003、Order 9004、Payment 9005、Notification 9006、Gateway 8080。

> **内部访问令牌**：`auth-service` 的 `/api/v1/internal/**` 接口需携带 `X-Internal-Token` 请求头。默认值为 `flexlease-internal-secret`，可分别通过
> `security.jwt.internal-access-token`（认证服务）与 `flexlease.auth-service.internal-token`（调用方，如用户服务）自定义。用户服务在审核厂商时会通过该接口将账号状态从 `PENDING_REVIEW` 激活为 `ENABLED`。

完成启动后，可参考 `docs/API设计.md` 的示例调用“注册厂商 → 提交申请 → 审批 → 登录”链路验证系统行为。

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
