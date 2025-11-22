# FlexLease 智能共享租赁平台

## 项目概览

FlexLease 面向 B2C 场景，为厂商与消费者提供从入驻、商品配置、下单履约、支付结算到通知运营的完整共享租赁能力。当前实现基于：

- **后端**：Java 21、Spring Boot 3.3、Spring Cloud Netflix（Eureka + Gateway）、Spring Security + JWT、JPA/Hibernate、Flyway。
- **中间件**：PostgreSQL 16、Redis 7（通知模板缓存）、RabbitMQ 3（订单事件总线）。
- **前端**：Vue 3 + Vite + TypeScript + Ant Design Vue + Pinia，多角色统一工作台。
- **交付**：Docker Compose 一键拉起 registry/gateway/全部微服务 + Nginx 前端，CI 通过 `./mvnw clean verify` 与 `npm run build`。

## 架构拓扑

| 组件 | 端口 | 说明 |
| --- | --- | --- |
| registry-service | 8761 | Eureka Server，所有微服务注册发现入口 |
| gateway-service | 8080 | Spring Cloud Gateway，统一 JWT 校验与路由转发（`/api/v1/**`） |
| auth-service | 9001 | 账号注册/登录、密码重置、刷新令牌、内部账号绑定 vendorId |
| user-service | 9002 | 厂商入驻与资料、用户档案、管理员用户冻结/解冻 |
| product-service | 9003 | 商品/Rental Plan/SKU/库存、媒体文件管理、对前台提供目录查询与内部库存接口 |
| order-service | 9004 | 订单预览/下单/履约操作/购物车/合同、运营指标、超时取消调度、RabbitMQ 事件 |
| payment-service | 9005 | 支付流水、自动模拟确认与回调、退款、结算统计 |
| notification-service | 9006 | 模板管理、通知发送日志、订阅订单事件并向厂商推送站内信 |
| redis / postgres / rabbitmq | 6379 / 5432 / 5672+15672 | 依赖中间件（Volume 持久化 + 管控台端口映射） |
| frontend | 8080 (Nginx) | Vite 构建后的管理端 SPA，通过 Gateway 访问后端 |

> 默认管理员账号由认证服务启动时自动创建：`admin@flexlease.test / Admin@123`。所有内部互信调用需在 Header 中附带 `X-Internal-Token:flexlease-internal-secret`，生产部署请通过 `.env` 或 CI/CD 密文覆盖。

## 核心能力一览

- **认证与账号**
  - 邮箱注册（消费者/厂商）、JWT 登录、刷新/注销、密码重置。
  - 内部接口可启停账号、绑定厂商 ID，所有微服务复用统一 JWT 解析器。
- **厂商与用户**
  - 厂商入驻申请、管理员审核（自动回写认证中心并创建 vendor 资料）。
  - 厂商资料分页与状态管控、消费者档案（含管理员冻结）。
- **商品域**
  - 商品 CRUD、租赁方案/租期/押金配置、SKU 库存流水（预占/释放/入库/出库）。
  - 商品提交→审核→上/下架，前台目录查询及媒体资源上传（本地文件系统托管）。
- **订单域**
  - 购物车增删改查，订单预览、下单（支持从购物车导入）、支付确认、发货、确认收货。
  - 订单试算自动根据用户信用档案调整押金（优享减免、预警上浮、受限拦截），并写入订单快照供履约审核。
  - 续租/退租/买断流程及审批、自动库存处理、订单合同生成与签署。
  - 平台/厂商运营指标、管理员强制关闭、待支付订单自动取消调度。
  - RabbitMQ 事件总线 + Notification-Service 异步告警。
- **支付与结算**
  - 支付流水初始化、模拟自动确认与回调、手工确认、退款、押金/租金/买断分账。
  - 厂商结算汇总 API（按支付/退款时间窗口过滤）。
- **通知与运营**
  - 模板化站内通知（支持变量渲染/自定义内容），Redis 缓存模板，最近 50 条日志查询。
  - 订阅订单创建事件，自动向对应厂商推送“新订单待处理”通知。
- **前端体验**
  - 单点登录 + 多角色工作台：消费者覆盖商品目录/详情、购物车、结算、订单详情（含支付、续租/退租/买断、电子合同）与通知中心；厂商拥有商品/媒体工作台、库存流水、订单履约抽屉、运营指标与结算看板；管理员负责入驻审核、商品审核与订单监控（含合同预览、强制关闭）。
  - 仪表盘提供平台/厂商双视角 GMV、订单状态与 7 日趋势，同步呈现租赁模式构成与常用入口。
  - 自动支付模拟、`useVendorContext` 厂商身份刷新、Ant Design Vue + Pinia + Vue Router 组合支撑桌面级交互，Playwright 覆盖仪表盘渲染。
- **系统保障**
  - `platform-common` 内置轻量级 `IdempotencyService`，`/orders`、`/payments` 等写操作支持 `Idempotency-Key` 防重复提交。
  - `OrderMaintenanceScheduler` 周期性取消超时订单、释放库存并推送通知，RabbitMQ `order.events` → `notification-service` 统一消费。
  - `flexlease.payment.auto-confirm` 默认开启以模拟端到端自动支付，置为 `false` 后可通过 `/payments/{id}/confirm`/`/callback` 体验人工确认与失败回调。
  - 商品库存预占/释放使用基于 `@Version` 的乐观锁 + 可配置自动重试（`flexlease.inventory.concurrency.*`），可在百级并发下保持库存正确性且避免数据库长时间行锁。

## 微服务实现要点

- **auth-service**：`backend/auth-service/src/main/java/com/flexlease/auth/config/DataInitializer.java` 会根据 `flexlease.bootstrap.admin.*` 环境变量初始化管理员账号并写入 `ADMIN/VENDOR/USER` 三种角色，账号状态使用 `ENABLED/DISABLED/PENDING_REVIEW` 枚举，`/api/v1/internal/users/**` 以内置 `X-Internal-Token` 保护供其它服务启停账号或绑定 `vendorId`。
- **user-service**：`VendorApplicationController` 支持厂商反复提交/查看申请，`VendorService` 统一维护 `users.vendor` 表，管理员在 `/vendors/applications/{id}/approve` 触发 `AuthServiceClient` 激活账号；`CustomerProfileController` 会在首次访问时自动建档，管理员可通过 `/admin/users/**` 远程调用认证服务冻结账号。
- **product-service**：`VendorProductController` 暴露商品/方案/SKU 以及库存调整 API，`InventoryChangeType` 支持 `INBOUND/OUTBOUND/RESERVE/RELEASE`，并通过 `InventoryReservationService` 处理 `/api/v1/internal/inventory/reservations` 批量预占；`ProductMediaController` 以上传到 `FLEXLEASE_STORAGE_ROOT` 目录为中心，返回媒体的 `fileName/fileUrl/contentType/fileSize/sortOrder`。
- **order-service**：`RentalOrderService` 会读取 `product-service` Catalog 验证计划与 SKU，支持 `cartItemIds` 合并下单、生成 `OrderContract` 并同步 `order_event`、续租/退租/买断审批、押金自动退款以及 `CartService` 的访问控制；`OrderMaintenanceScheduler` 根据 `flexlease.order.maintenance.*` 周期取消 `PENDING_PAYMENT` 订单；`OrderAnalyticsService` 聚合平台与厂商指标供 `/analytics/**` 使用。
- **payment-service**：`PaymentTransactionService` 结合 `IdempotencyService` 限制同一订单/场景只存在一条待支付流水，`flexlease.payment.auto-confirm` 为真时自动将状态切换为 `SUCCEEDED` 并调用 `order-service` `/internal/orders/{id}/payment-success`；退款通过 `PaymentClient.createRefund` 回流，结算接口会统计押金/租金/买断/违约金及退款窗口。
- **notification-service**：开启 `flexlease.redis.enabled=true` 时通过 `NotificationTemplateProvider` + Spring Cache 缓存模板，`NotificationService` 根据角色自动收敛 `/notifications/logs` 查询范围，`OrderEventListener` 监听 `order.events.notification` 队列对厂商推送“新订单”站内信。
- **gateway-service / registry-service**：Gateway 依据 `backend/gateway-service/src/main/resources/application.yml` 中的路由表把 `/api/v1/**` 映射到各微服务，Eureka 负责注册发现，所有服务默认以 `prefer-ip-address=true` 注册节点。
- **frontend**：Vite + Vue 3 + Ant Design Vue。`pages/overview/OverviewPage.vue` 同时拉取 `/analytics/dashboard`、`/analytics/vendor/{id}`、`/notifications/logs` 与最新订单，消费者端通过 `CartPage.vue` + `autoCompleteInitialPayment` 与 `/payments/{orderId}/init` 形成“下单即付”的体验，厂商端通过 `useVendorContext` 读取登录会话中的 `vendorId`（若缺失请退出并重新登录）。

## 配置要点

- 所有服务共用 `security.jwt.secret` 与 `security.jwt.internal-access-token`，请在部署时统一覆盖；内部调用统一在 Header 中写入 `X-Internal-Token`（默认 `flexlease-internal-secret`）。
- `flexlease.bootstrap.admin.username/password` 控制认证服务默认管理员账号；`FLEXLEASE_STORAGE_ROOT` 指定商品媒体文件目录；`FLEXLEASE_*_BASE_URL` 用于跨服务调用（order→product/payment/notification 等）。
- `FLEXLEASE_PAYMENT_AUTO_CONFIRM`（或 `flexlease.payment.auto-confirm`）控制支付是否自动成功；`FLEXLEASE_ORDER_MAINTENANCE_PENDING_PAYMENT_EXPIRE_MINUTES` 与 `FLEXLEASE_ORDER_MAINTENANCE_SCAN_INTERVAL_MS` 调整待支付超时策略；`FLEXLEASE_MESSAGING_ENABLED` 与 `FLEXLEASE_REDIS_ENABLED` 可在开发环境禁用 RabbitMQ 或 Redis 依赖。

## 多角色能力速览

- **平台管理员**
  - 入驻审核：`/app/admin/vendor-review` 通过 `user-service` 审核接口激活厂商账号并回写认证中心。
  - 商品审核：`/app/admin/product-review` 直连 `product-service` 的 `/api/v1/admin/products/**`。
  - 订单监控：`/app/admin/orders` 支持按用户/厂商/状态过滤，抽屉内嵌电子合同查看、操作时间线与 `/admin/orders/{id}/force-close`。
- **厂商**
  - 商品与库存：`VendorProductWorkspace` 绑定 `vendorId`，可配置方案、SKU、媒体并调用 `/inventory/adjust`。
  - 履约操作：`VendorOrderWorkspace` 针对 `/orders/{id}/ship`、续租/退租/买断审批等动作提供抽屉，内置库存出入库补偿。
  - 指标与结算：`VendorAnalyticsPage`、`VendorSettlementPage` 调用 `/analytics/vendor/{vendorId}`、`/payments/settlements`，依赖登录会话携带的 `vendorId`（缺少时需重新登录）。
- **消费者**
  - 自助下单：商品目录 → 详情 → 购物车/结算页 → `/orders` & `/payments` 的试算、下单与自动支付流程。
  - 订单售后：详情页直接操作续租/退租/买断/确认收货并触发合同生成、通知推送。
  - 通知中心：基于 `/notifications/logs` 的时间轴展示最新站内信，可交叉验证订单事件。

## 运行指南

### Docker Compose（推荐）

```bash
docker compose up --build
```

命令会依次构建并启动数据库、Redis、RabbitMQ、Eureka、所有微服务以及 Nginx 容器，首次执行耗时取决于网络与 Maven 缓存。完成后可访问：

- 管理端 & API 网关：http://localhost:8080
- Eureka 面板：http://localhost:8761
- RabbitMQ 控制台：http://localhost:15672（`guest/guest`）

> Compose 会挂载 `product-media` 卷存放商品图片，其余数据存放在 `postgres-data`、`redis-data`、`rabbitmq-*` 卷中。停止环境使用 `docker compose down`，如需清理持久化可加 `-v`。

### 本地开发

1. **后端单服务**：`SPRING_PROFILES_ACTIVE=dev` 使用 H2 + 自动建 schema，或 `postgres` profile 连接本地数据库。
   ```bash
   mvn -pl backend/order-service -am spring-boot:run
   ```
2. **前端**：
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   开发期默认使用 Vite 代理分别联调 9001～9006 服务，也可设置 `VITE_API_PROXY=http://localhost:8080` 直接走网关；构建阶段通过 `VITE_API_BASE_URL`（默认 `/api/v1`）切换 API 前缀。
3. **常用命令**
   ```bash
   ./mvnw clean verify      # 全量构建+测试
   mvn -pl backend/product-service -am test
   npm run build && npm run test:e2e
   ```

### 功能体验建议

1. 通过 `/api/v1/auth/register/vendor` 注册厂商账号 → 登录管理端 → 填写入驻资料 → 管理员在 `/vendors/applications/{id}/approve` 审核通过（认证中心自动激活账号）。
2. 厂商工作台创建商品/方案/SKU，上传媒体、调整库存并提交审核，管理员在 `/api/v1/admin/products/**` 审核后即可在 Catalog 中看到。
3. 消费者登录 → 浏览 `/app/catalog` → 进入商品详情 → 加入购物车/直接试算 `/orders/preview` → 创建订单（可附带 `cartItemIds`）→ 支付由 `payment-service` 自动确认。
4. 在订单详情体验续租/退租/买断、支付回执与电子合同签署，同时打开通知中心验证 `/notifications/logs` 的最新日志。
5. 切换到厂商角色，在订单履约抽屉中完成发货/审批，接着打开运营指标与结算页面（若缺少 `vendorId`，请退出账号后重新登录）。
6. 切换管理员查看订单监控列表、过滤条件与合同抽屉，并使用强制关闭演练补偿流程；如需纯 API 调试可导入 `docs/postman/cart-api.postman_collection.json`。

## 测试与质量

- `backend/auth-service`：`AuthServiceApplicationTests` 覆盖注册/登录/刷新/密码重置，确保默认管理员初始化与 JWT 解析配置无误。
- `backend/user-service`：`UserServiceApplicationTests` 演练厂商入驻到审批流程，校验对认证中心内部接口的启用与厂商 ID 绑定。
- `backend/product-service`：`ProductServiceIntegrationTest` 走通商品创建→方案/SKU→库存调整→审核→ Catalog 暴露的完整链路。
- `backend/order-service`：`RentalOrderServiceIntegrationTest` 验证购物车合并、库存预占/释放、支付回执、发货、售后、电子合同与 `OrderMaintenanceScheduler`；`OrderAnalyticsServiceIntegrationTest` 校验 `/analytics/**` 聚合。
- `backend/payment-service`：`PaymentTransactionServiceIntegrationTest`（自动确认 + 结算）、`PaymentTransactionServiceManualFlowTest`（手动确认与失败回调）覆盖退款/分账/事件回调。
- `backend/notification-service`：`NotificationServiceIntegrationTest` 检查模板渲染与 Redis 缓存，`OrderEventListener` 监听 RabbitMQ 并推送厂商提醒。
- 前端 `tests/dashboard.spec.ts` 使用 Playwright 模拟 `/auth/me` 与 `/analytics/dashboard`，校验 Overview 指标渲染。
- `./mvnw -pl backend/product-service -am -Dtest=InventoryReservationConcurrencyTest -Dsurefire.failIfNoSpecifiedTests=false test` 可直接触发库存高并发回归，验证乐观锁 + 自动重试链路。
- `./mvnw clean verify` 在 H2 + Flyway 下执行，CI 挂载 PostgreSQL 验证脚本一致性；`npm run build && npm run test:e2e` 覆盖前端构建与端到端回归。
- `platform-common` 提供异常枚举、JWT 解析、幂等工具、消息常量等基础能力，保障跨服务契约。

## 文档索引

- 《项目说明》：`docs/项目说明.md`
- 《项目规划与任务分解》：`docs/项目规划与任务分解.md`
- 《API 设计》：`docs/API设计.md`
- 《数据库设计》：`docs/数据库设计.md`
- 《用例设计》：`docs/用例设计.md`
- 《测试与质量策略》：`docs/测试与质量策略.md`

更多细节及迭代路线可参考 `docs/` 目录。

## 目录结构

```
backend/            后端微服务源码
  platform-common/  公共依赖与 DTO
  auth-service/
  user-service/
  product-service/
  order-service/
  payment-service/
  notification-service/
  gateway-service/
  registry-service/

db/                 Flyway 迁移脚本（按 schema 分类）
docs/               需求、设计、测试等文档
frontend/           Vite + Vue 3 前端
```

## 后续计划

详见 `docs/项目规划与任务分解.md`。根据当前迭代结论，平台聚焦“自有商品 + 模拟支付”闭环，后续重点将落在通知渠道扩展、指标可视化与流水线自动化优化，暂不对接速卖通/eBay 等第三方商品接口。
