# FlexLease 智能共享租赁平台

## 简介
 FlexLease 是一个面向 B2C 模式的共享租赁平台，覆盖厂商入驻、商品租赁、订单履约与支付结算等核心业务域。项目采用 Spring Boot 多模块微服务 + Vue 3 管理端的形态迭代交付，当前已完成通知与运营能力迭代。

## 当前进展
- ✅ 项目规划、数据库/API 设计文档维护（`docs/`）
- ✅ 微服务骨架与基础设施（Iteration 0）：模块化 Maven 工程、公共组件、Flyway schema 基线
- ✅ 账号与厂商入驻（Iteration 1）：认证中心登录/注册、厂商申请审核、审核通过后自动激活账号
- ✅ 商品与租赁方案（Iteration 2）：商品 CRUD、租赁方案与 SKU 管理、库存调整流水、管理员审核、前台商品目录查询
- ✅ 管理端前端（Iteration 1-2）：登录与仪表盘、厂商商品管理、管理员商品审核流程
- ✅ 多角色工作台前端重构（Iteration 6）：统一侧边导航与角色菜单、厂商工作台（商品/订单/指标/结算）、消费者目录与订单中心、管理员订单监控与通知中心（运营工具由自动支付模拟取代）
- ✅ 支付模拟自动化：订单创建或用户发起支付时自动生成并确认支付流水，彻底移除运营工具箱手动调试依赖
- ✅ 产品域集成测试：`backend/product-service` 覆盖商品生命周期 e2e 场景
- ✅ 订单与履约流程（Iteration 3）：交付独立 `order-service`，修复仓储命名问题并完成下单预览、创建、支付确认、发货、收货、续租、退租、买断等状态流转及集成测试
- ✅ 支付与结算（Iteration 4）：交付 `payment-service` 支付流水/分账/退款能力与厂商结算统计，订单服务集成支付凭证校验并补充跨服务集成测试，支付回调现已自动通知订单服务并具备幂等保障
- ✅ 通知与运营看板（Iteration 5）：上线 `notification-service` 模板化通知发送与日志查询，订单/支付服务打通通知推送，新增订单运营指标 API、前端仪表盘与端到端测试
- ✅ 账号管理增强：支持注销、密码重置、令牌刷新，开放管理员启停账号接口
- ✅ 厂商/用户资料中心与管理员订单干预（Iteration 6+）：提供厂商资料分页、用户档案维护及后台订单强制关闭能力，前端管理端同步支持
- ✅ 服务注册与内部通讯收敛（Iteration 7）：引入 Eureka 注册中心，统一改造各服务为负载均衡的 RestTemplate/RestClient 调用
- ✅ 购物车与库存预占闭环：提供购物车增删改查 API，下单时自动预占/释放库存并新增产品服务内部库存接口
- ✅ 订单维护调度：新增超时未支付订单的定时自动取消任务并推送通知
- ✅ 订单事件消息总线：基于 RabbitMQ 推送订单状态事件并由通知服务异步订阅
- ✅ 商品媒体资源管理：支持厂商上传商品图片并通过本地文件系统托管与分发
- ✅ Redis 缓存支撑：通知模板读取支持 Redis 缓存，可通过 `flexlease.redis.enabled` 开关控制
- ✅ 厂商身份同步：认证服务引入 `vendor_id` 字段与内部绑定接口，订单/前端根据 JWT 中的厂商身份进行租赁单校验，支持同一厂商的多账号共享 `vendor_id` 访问厂商工作台

## 快速开始
### 一键启动生产化环境
```powershell
docker compose up --build
```
首次执行包含镜像构建，耗时取决于网络与 Maven 构建缓存。命令成功后即可访问：

- 管理端入口：http://localhost:8080
- API 网关入口：http://localhost:8080/api/v1
- Eureka 注册中心：http://localhost:8761
- RabbitMQ 控制台：http://localhost:15672（默认账号/密码 `guest`）

> **默认凭据与密钥**：容器内各微服务默认复用 Postgres 数据库 `flexlease/flexlease` 与内部访问令牌 `flexlease-internal-secret`，JWT 秘钥为 `flexlease-prod-secret`。生产环境应在 `.env` 或 CI/CD 中覆盖这些值。
> **内部服务回调**：支付服务默认通过 `FLEXLEASE_ORDER_SERVICE_BASE_URL`（默认为 `http://localhost:9004/api/v1`）和 `FLEXLEASE_NOTIFICATION_SERVICE_BASE_URL`（默认为 `http://localhost:9006/api/v1`）调用订单/通知服务，Docker Compose 已在环境变量中覆盖为容器内地址；若自行拆分部署，请确保这两个变量指向可访问的地址以保证自动支付成功回调。

所有后端服务在容器中默认连接 PostgreSQL、Redis 与 RabbitMQ，并交由 Spring Cloud Eureka 与 Gateway 协调；商品媒体文件持久化在命名卷 `product-media` 中。

### 开发模式（H2 + 本地启动）

若需保留原有快速验证体验，可为单个服务启用开发配置：

```powershell
set SPRING_PROFILES_ACTIVE=dev
mvn -pl backend/product-service spring-boot:run
```

`application-dev.yml` 维持了 H2 内存库与控制台配置，便于编写与调试测试用例。PostgreSQL 场景可继续使用 `SPRING_PROFILES_ACTIVE=postgres`。

前端本地调试依旧可用 Vite：

```powershell
cd frontend
npm install
npm run dev
```

当需要整合网关时，可设置 `VITE_API_PROXY=http://localhost:8080` 将全部请求代理至 Docker 中的 Gateway。

完成启动后，可参考 `docs/API设计.md` 流程体验“注册厂商 → 提交入驻 → 审批 → 创建商品 → 审核上架 → 用户下单/履约”链路，并可通过订单服务暴露的 `/api/v1/analytics/**` 接口验证平台及厂商运营指标，再结合 `notification-service` 的 `/api/v1/notifications/**` 验证通知发送与日志查询。

### 常用命令
```powershell
# 构建并启动容器化环境
docker compose up --build

# 停止并移除容器（保留数据卷）
docker compose down

# 单个服务测试
mvn -pl backend/product-service -am test

# 全量构建
mvn clean package
```

### 接口调试
- 提供 Postman 集合：`docs/postman/cart-api.postman_collection.json`，覆盖购物车增删改查与基于购物车的下单流程，导入后配置环境变量 `baseUrl`、`userId` 等即可调用。
- Docker Compose 默认会在单条命令中启动注册中心、依赖中间件与所有微服务，无需手动分步执行。

## CI 与质量保障

- GitHub Actions 工作流位于 `.github/workflows/ci.yml`，后端作业会在 PostgreSQL（容器）下执行 `./mvnw clean verify`，前端作业运行 `npm ci` 与 `npm run build`。
- Flyway 迁移脚本已覆盖所有 schema 的 `V000__create_schema.sql`，确保在 PostgreSQL 中先建命名空间再执行业务表结构迁移。
- 生产部署以 PostgreSQL 为默认数据源，单元/集成测试通过 `@ActiveProfiles("test")` 搭配 H2 隔离执行，若需在本地复现 CI 行为可直接运行 `./mvnw clean verify`。

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
详见 `docs/项目规划与任务分解.md`。根据迭代决议，FlexLease 不订制对接速卖通、eBay 等外部电商商品接口——平台使用模拟支付闭环即可完成主业务场景，后续重点放在通知渠道扩展与持续交付流水线优化。
