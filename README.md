# FlexLease 智能共享租赁平台

## 简介
 FlexLease 是一个面向 B2C 模式的共享租赁平台，覆盖厂商入驻、商品租赁、订单履约与支付结算等核心业务域。项目采用 Spring Boot 多模块微服务 + Vue 3 管理端的形态迭代交付，当前已推进至迭代 5 并完成通知/运营能力。

## 当前进展
- ✅ 项目规划、数据库/API 设计文档维护（`docs/`）
- ✅ 微服务骨架与基础设施（Iteration 0）：模块化 Maven 工程、公共组件、Flyway schema 基线
- ✅ 账号与厂商入驻（Iteration 1）：认证中心登录/注册、厂商申请审核、审核通过后自动激活账号
- ✅ 商品与租赁方案（Iteration 2）：商品 CRUD、租赁方案与 SKU 管理、库存调整流水、管理员审核、前台商品目录查询
- ✅ 管理端前端（Iteration 1-2）：登录与仪表盘、厂商商品管理、管理员商品审核流程
- ✅ 产品域集成测试：`backend/product-service` 覆盖商品生命周期 e2e 场景
- ✅ 订单与履约流程（Iteration 3）：交付独立 `order-service`，支持下单预览、创建、支付确认、发货、收货、续租、退租、买断等状态流转，并提供集成测试覆盖核心闭环
- ✅ 支付与结算（Iteration 4）：交付 `payment-service` 支付流水/分账/退款能力与厂商结算统计，订单服务集成支付凭证校验并补充跨服务集成测试
- ✅ 通知与运营看板（Iteration 5）：上线 `notification-service` 模板化通知发送与日志查询，订单/支付服务打通通知推送，新增订单运营指标 API、前端仪表盘与端到端测试

## 快速开始
### 后端服务
```powershell
# 在仓库根目录依次启动需要的服务
mvn -pl backend/auth-service spring-boot:run      # 认证服务，端口 9001
mvn -pl backend/user-service spring-boot:run      # 用户/厂商服务，端口 9002
mvn -pl backend/product-service spring-boot:run   # 商品服务，端口 9003
mvn -pl backend/order-service spring-boot:run     # 订单服务，端口 9004
mvn -pl backend/payment-service spring-boot:run   # 支付服务，端口 9005
mvn -pl backend/notification-service spring-boot:run  # 通知与运营服务，端口 9006
```
> 常用端口：Auth 9001、User 9002、Product 9003、Order 9004、Payment 9005、Notification 9006、Gateway 8080。

> **内部访问令牌**：调用 `auth-service` 的 `/api/v1/internal/**` 接口时需携带 `X-Internal-Token`，默认值 `flexlease-internal-secret` 可在认证服务 `security.jwt.internal-access-token` 和调用方（如用户服务）`flexlease.auth-service.internal-token` 中调整。

完成启动后，可参考 `docs/API设计.md` 流程体验“注册厂商 → 提交入驻 → 审批 → 创建商品 → 审核上架 → 用户下单/履约”链路，并可通过订单服务暴露的 `/api/v1/analytics/**` 接口验证平台及厂商运营指标，再结合 `notification-service` 的 `/api/v1/notifications/**` 验证通知发送与日志查询。

### 前端管理端
```powershell
cd frontend
npm install
npm run dev
# 端到端测试
npm run test:e2e
```
开发环境默认通过 Vite 代理联调后台，可在 `vite.config.ts` 中调整；现已内置 `/api/v1/analytics`（订单服务）与 `/api/v1/notifications`（通知服务）等最新迭代的代理配置。如需统一走网关，可设置 `VITE_API_PROXY=http://localhost:8080`。

### 常用命令
```powershell
# 单个服务测试
mvn -pl backend/product-service test

# 全量构建
mvn clean package
```

## 目录结构
```
backend/            后端微服务源码
  platform-common/  公共依赖与 DTO
  auth-service/     认证授权服务
  user-service/     用户与厂商服务
  product-service/  商品与租赁方案服务
  order-service/    订单履约服务与运营指标
  payment-service/  支付结算服务
  notification-service/ 通知与消息服务
  gateway-service/  Spring Cloud Gateway 网关占位

db/                 数据库迁移脚本（按 schema 分类）
docs/               需求、设计与迭代文档
frontend/           管理端前端（Vite + Vue 3）
```

## 后续计划
详见 `docs/项目规划与任务分解.md`，后续将聚焦管理端仪表盘联调、通知渠道扩展与持续交付流水线优化。
