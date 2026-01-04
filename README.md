# FlexLease 智能共享租赁平台

## 项目概览

FlexLease 面向 B2C 场景，为厂商与消费者提供从入驻、商品配置、下单履约、支付结算到通知运营的完整共享租赁能力。当前实现基于：

- **后端**：Java 21、Spring Boot 3.3、Spring Cloud Netflix（Eureka + Gateway）、Spring Security + JWT、JPA/Hibernate、Flyway。
- **中间件**：PostgreSQL 16、Redis 7（通知模板缓存）、RabbitMQ 3（订单事件总线）。
- **前端**：Vue 3 + Vite + TypeScript + Ant Design Vue + Pinia，多角色统一工作台。
- **交付**：Docker Compose 一键拉起 registry/gateway/全部微服务 + Nginx 前端，CI 通过 `./mvnw clean verify` 与 `npm run build`。

## 架构拓扑

| 组件 | 端口（默认） | 说明 |
| --- | --- | --- |
| frontend | 8080（对外暴露） | 管理端 SPA；Nginx 反代 `/api/**` → `gateway-service:8080`，`/media/**` → `product-service:9003` |
| registry-service | 8761（对外暴露） | Eureka Server，所有微服务注册发现入口 |
| gateway-service | 8080（仅容器/本地） | Spring Cloud Gateway，统一路由转发（`/api/v1/**`）；JWT 校验由各微服务完成，Compose 中通过前端 Nginx 访问 |
| auth-service | 9001（仅容器/本地） | 账号注册/登录、密码重置、刷新令牌、内部账号绑定 vendorId |
| user-service | 9002（仅容器/本地） | 厂商入驻与资料、用户档案、管理员用户冻结/解冻 |
| product-service | 9003（仅容器/本地） | 商品/Rental Plan/SKU/库存、媒体文件管理、对前台提供目录查询与内部库存接口 |
| order-service | 9004（仅容器/本地） | 订单预览/下单/履约操作/购物车/合同、运营指标、超时取消调度、RabbitMQ 事件 |
| payment-service | 9005（仅容器/本地） | 支付流水、自动模拟确认与回调、退款、结算统计 |
| notification-service | 9006（仅容器/本地） | 模板管理、通知发送日志、订阅订单事件并向厂商推送站内信 |
| redis / postgres / rabbitmq | 6379 / 5432 / 5672+15672（对外暴露） | 依赖中间件（Volume 持久化 + 管控台端口映射） |

> 默认平台管理员账号由认证服务启动时自动创建：`admin@flexlease.test / Admin@123`。默认仲裁管理人员账号：`arbitrator@flexlease.test / Arbitrator@123`（用于纠纷仲裁与裁决，平台管理员无此权限；登录后可在 `/app/arbitration/orders` 进入仲裁中心）。所有内部互信调用需在 Header 中附带 `X-Internal-Token:flexlease-internal-secret`，生产部署请通过 `.env` 或 CI/CD 密文覆盖。

## 核心能力一览

- **认证与账号**
  - 邮箱或 11 位手机号注册（消费者/厂商）、JWT 登录、刷新/注销、密码重置。
  - 内部接口可启停账号、绑定厂商 ID，所有微服务复用统一 JWT 解析器。
- **厂商与用户**
  - 厂商入驻申请、管理员审核（自动回写认证中心并创建 vendor 资料）。
  - 厂商资料分页与状态管控、消费者档案（含管理员冻结）。
  - `CreditEventService` 暴露 `/api/v1/internal/users/{id}/credit-events`（内部事件），当前内置规则为：实名认证（预留/内部事件模拟）+10、按时支付 +5（每连续 3 单额外 +5）、按约归还（租期到期前或 24 小时宽限内）+8、巡检配合 +2、待支付超时取消 -8、友好协商 +3、恶意行为 -30 且冻结 30 天，并同步站内信提示；管理员亦可经 `/api/v1/admin/users/{id}/credit-adjustments` 人工干预信用分、记录原因并下发通知（支持查询历史调整记录）。
  - `AccountUnfreezeScheduler` 每小时自动检查并解冻到期账号（恶意行为冻结 30 天后自动恢复），确保账号生命周期完整闭环。
  - `VendorService` 为支付/订单服务提供 `/api/v1/internal/vendors/{id}/commission-profile` 内部接口与 `/vendors/{id}/commission-profile` 管理入口，管理员可直接维护行业基准费率、信用档位及最近一次 SLA 评分。
- **商品域**
  - 商品 CRUD、租赁方案/租期/押金配置、SKU 库存流水（预占/释放/入库/出库）。
  - 商品提交→审核→上/下架，前台目录查询及媒体资源上传（本地文件系统托管）。
  - 新增“下单前咨询”通道：消费者可在详情页提交 72 小时有效的咨询，并在详情页查看咨询历史/回复与过期状态；`ProductInquiryService` 会通知厂商并在工作台集中展示/回复，并在回复前再次校验过期状态，超过窗口会自动标记为 `EXPIRED` 并拒绝响应。
- **订单域**
  - 购物车增删改查，订单预览、下单（支持从购物车导入）、支付确认、发货、确认收货。
  - 订单试算自动根据用户信用档案调整押金（优享减免、预警上浮、受限拦截），并写入订单快照供履约审核。
  - 下单支持附带客户备注，厂商工作台与管理员订单抽屉可直接查看，方便处理特殊配送/履约指引。
  - 续租/退租/买断流程及审批、自动库存处理、订单合同生成与签署。
  - `/orders/{id}/messages` 支持用户与厂商在订单抽屉直接沟通（自动写入时间线），`OrderProofService`（`backend/order-service/src/main/java/com/flexlease/order/service/OrderProofService.java`）提供发货/收货/退租/巡检/其他凭证上传、`/api/v1/proofs/{fileName}` 鉴权下载（前端预览通过拉取 blob 生成本地 URL）以及和 Notification Service 的互通提醒；厂商也可通过 `/orders/{id}/inspection/request` 发起巡检请求，用户上传巡检凭证后自动触发信用加分。
  - `ProofPolicyService` 对外暴露 `/api/v1/proof-policy`，统一告知各阶段最小凭证数量与拍摄角度，并由 `ProofStorageService` 对图片自动打水印；其中 `watermarkExample` 为水印说明文本（前端按文本展示，若为 URL 则打开示例资源）。为保证中文水印正常显示，`order-service` 镜像包含 `fonts-noto-cjk` 字体包。
  - `OrderDisputeService`（`backend/order-service/src/main/java/com/flexlease/order/service/OrderDisputeService.java`）封装纠纷创建→协商→升级仲裁→平台裁决→信用扣分→满意度调查的全流程，仲裁管理人员可在 `/api/v1/admin/orders/{id}/disputes/{disputeId}/resolve` 提交裁决，并支持 `maliciousBehavior` 标志自动触发恶意行为惩罚；双方在发起/回应纠纷时可同步上传多媒体附件与电话纪要，信息将写入时间线与抽屉附件列表。
  - 纠纷进入平台仲裁后，仲裁管理人员可一键生成“LLM 仲裁建议”（结构化 JSON，包含事实摘要/缺失证据/建议裁决/话术草稿），接口为 `/api/v1/admin/orders/{id}/disputes/{disputeId}/ai-suggestion`；配置方式见 `docs/纠纷仲裁智能助手.md`。
    - 说明：仲裁建议的自然语言内容默认按 **中文** 输出（枚举值如 `recommendedDecision.option` 仍为固定英文枚举）。
  - 纠纷调度新增多阶段倒计时提醒（24小时、6小时、1小时前各提醒一次）与超时自动升级逻辑；用户二次申诉进入 `PENDING_REVIEW_PANEL` 状态由 `REVIEW_PANEL` 角色裁决，避免“申诉后无人结案”卡死。
  - 满意度调研由 `OrderSurveyService` 定时激活 `/orders/{id}/surveys` 调查，支持双方打分与评价，并追加时间线+站内信提醒。
  - 平台/厂商运营指标、管理员强制关闭、待支付订单自动取消调度。
  - RabbitMQ 事件总线 + Notification-Service 异步告警。
  - `VendorPerformanceService` 聚合发货准时率、纠纷友好解决率与取消率，并通过 `/api/v1/internal/vendors/{id}/performance-metrics` 暴露给用户服务，驱动 SLA 评分与抽成档位的自动调整。
- **支付与结算**
  - 支付流水初始化、模拟自动确认与回调、手工确认、退款、押金/租金/买断分账。
  - 厂商结算汇总 API（按支付/退款时间窗口过滤），结算明细会携带 `commissionRate/platformCommissionAmount`，与 `users.vendor` 上的行业/信用/SLA 抽成配置联动。
  - `CommissionReviewScheduler` 每季度自动评估厂商 SLA 评分（基于订单履约数据自动计算：准时发货率×40% + 纠纷解决率×30% + 低取消率×30%）并调整信用档位（`EXCELLENT/STANDARD/WARNING/RESTRICTED`），费率变更后自动通知厂商。
  - `payment-service` 将 `PaymentSplit` 记录拆分为 `DEPOSIT_RESERVE/VENDOR_INCOME/PLATFORM_COMMISSION` 三类，并在 `PaymentTransactionResponse` 中反映实际抽成。
- **通知与运营**
  - 模板化站内通知（支持变量渲染/自定义内容），Redis 缓存模板，最近 50 条日志查询。
  - 订阅订单创建事件，自动向对应厂商推送“新订单待处理”通知。
  - 纠纷升级、凭证上传、满意度调查邀请/感谢、信用分调整等都会通过 Notification Service 自动推送，便于双方回溯。
  - `V003__dispute_templates.sql` 内置纠纷倒计时/结案模板，结合计时调度器可自动提醒双方补充材料或确认处理结果。
- **前端体验**
  - 单点登录 + 多角色工作台：消费者覆盖商品目录/详情、购物车、结算、订单详情（含支付、续租/退租/买断、电子合同）与通知中心；厂商拥有商品/媒体工作台、库存流水、订单履约抽屉、运营指标与结算看板；管理员负责入驻审核、商品审核与订单监控（含合同预览、强制关闭）。
  - 仪表盘提供平台/厂商双视角 GMV、订单状态与 7 日趋势，同步呈现租赁模式构成、信用分布、纠纷态势与满意度调研待办，并暴露订单沟通/凭证/纠纷抽屉。
  - 商品详情页新增“下单前咨询”表单，并展示本人咨询历史、倒计时与厂商回复；订单详情/履约抽屉根据 `/api/v1/proof-policy` 动态展示拍摄指引与水印示例，厂商指标/结算页面同步展示抽成基准与实时佣金。
  - `ProfilePage` 在首次进入时自动触发档案创建，展示信用档位与冻结状态，并允许用户更新联系方式供通知/合同使用。
  - 纠纷面板可以直接上传图片/视频附件并记录电话纪要，消费者、厂商与管理员在抽屉内即可查看并下载所有取证材料。
  - 自动支付模拟、`useVendorContext` 厂商身份刷新、Ant Design Vue + Pinia + Vue Router 组合支撑桌面级交互。
- **系统保障**
  - `platform-common` 内置轻量级 `IdempotencyService`，`/orders`、`/payments` 等写操作支持 `Idempotency-Key` 防重复提交。
  - `OrderMaintenanceScheduler` 周期性取消超时订单、释放库存并推送通知，RabbitMQ `order.events` → `notification-service` 统一消费。
  - `flexlease.payment.auto-confirm` 默认开启以模拟端到端自动支付，置为 `false` 后可通过 `/payments/{id}/confirm`/`/callback` 体验人工确认与失败回调。
  - 商品库存预占/释放使用基于 `@Version` 的乐观锁 + 可配置自动重试（`flexlease.inventory.concurrency.*`），可在百级并发下保持库存正确性且避免数据库长时间行锁。

## 微服务实现要点

- **auth-service**：`backend/auth-service/src/main/java/com/flexlease/auth/config/DataInitializer.java` 会根据 `flexlease.bootstrap.admin.*` 环境变量初始化管理员账号并写入 `ADMIN/VENDOR/USER` 三种角色，账号状态使用 `ENABLED/DISABLED/PENDING_REVIEW` 枚举，`/api/v1/internal/users/**` 以内置 `X-Internal-Token` 保护供其它服务启停账号或绑定 `vendorId`。
- **user-service**：`VendorApplicationController` 支持厂商反复提交/查看申请，`VendorService` 统一维护 `users.vendor` 表并暴露 `/api/v1/internal/vendors/{id}/commission-profile` 供支付服务读取抽成策略；管理员在 `/vendors/applications/{id}/approve` 触发 `AuthServiceClient` 激活账号。`CustomerProfileController` 会在首次访问时自动建档，管理员可通过 `/admin/users/**` 远程调用认证服务冻结账号，并在 `/api/v1/admin/users/{id}/credit-adjustments` 中记录人工信用调整。`CreditEventController` 面向内部服务开放 `/credit-events` 用于记录实名认证、按时支付、提前归还、友好协商、恶意行为（扣 30 分+冻结 30 天）等信用事件并推送提醒。`CommissionReviewScheduler` 定期调用订单服务 `/api/v1/internal/vendors/{id}/performance-metrics` 计算 SLA，并据此调整 `commissionCreditTier` 与通知厂商费率变化。
- **product-service**：`VendorProductController` 暴露商品/方案/SKU 以及库存调整 API，`InventoryChangeType` 支持 `INBOUND/OUTBOUND/RESERVE/RELEASE`，并通过 `InventoryReservationService` 处理 `/api/v1/internal/inventory/reservations` 批量预占；`ProductMediaController` 以上传到 `FLEXLEASE_STORAGE_ROOT` 目录为中心，返回媒体的 `fileName/fileUrl/contentType/fileSize/sortOrder`；`ProductInquiryService` 用于处理消费者咨询、72 小时过期策略与厂商回复通知。
- **order-service**：`RentalOrderService` 会读取 `product-service` Catalog 验证计划与 SKU，支持 `cartItemIds` 合并下单、生成 `OrderContract` 并同步 `order_event`、续租/退租/买断审批、押金自动退款以及 `CartService` 的访问控制；`OrderMaintenanceScheduler` 根据 `flexlease.order.maintenance.*` 周期取消 `PENDING_PAYMENT` 订单；`OrderAnalyticsService` 聚合平台与厂商指标供 `/analytics/**` 使用；`CreditRewardService` 负责调用 `/credit-events` 奖惩信用，`ProofPolicyController` 暴露多阶段凭证规范，`OrderDisputeMaintenanceScheduler` 则具备倒计时提醒与自动升级仲裁能力；`VendorPerformanceService` 结合订单事件统计 48 小时内发货率并聚合纠纷/取消指标，通过 `/api/v1/internal/vendors/{id}/performance-metrics` 提供给用户服务。
- **payment-service**：`PaymentTransactionService` 结合 `IdempotencyService` 限制同一订单/场景只存在一条待支付流水，`flexlease.payment.auto-confirm` 为真时自动将状态切换为 `SUCCEEDED` 并调用 `order-service` `/internal/orders/{id}/payment-success`；支付初始化时会读取订单信用快照，对信用优享（>=90）订单额外下调平台抽成 1 个百分点（最低为 0）；退款通过 `PaymentClient.createRefund` 回流，结算接口会统计押金/租金/买断/违约金及退款窗口。
- **notification-service**：开启 `flexlease.redis.enabled=true` 时通过 `NotificationTemplateProvider` + Spring Cache 缓存模板，`NotificationService` 根据角色自动收敛 `/notifications/logs` 查询范围，`OrderEventListener` 监听 `order.events.notification` 队列对厂商推送“新订单”站内信。
- **gateway-service / registry-service**：Gateway 依据 `backend/gateway-service/src/main/resources/application.yml` 中的路由表把 `/api/v1/**` 映射到各微服务，Eureka 负责注册发现，所有服务默认以 `prefer-ip-address=true` 注册节点。
- **frontend**：Vite + Vue 3 + Ant Design Vue。`views/dashboard/DashboardHome.vue` 同时拉取 `/analytics/dashboard`、`/analytics/vendor/{id}`、`/notifications/logs` 与最新订单，提供卡片化指标、7 日趋势、纠纷与信用分布；消费者端通过 `views/checkout/CartBoardView.vue` + `autoCompleteInitialPayment` 与 `/payments/{orderId}/init` 形成“下单即付”的体验；`views/catalog/ProductDetailShell.vue` 重绘方案/Sticky CTA，并挂载 72 小时咨询面板；`views/vendor/workbench/**` 把商品、履约、指标、结算拆分成 Tab（履约列表支持“仅查看预警/manualReviewOnly”筛选）；`views/profile/ProfileOverviewView.vue` 集成信用展示与资料编辑，与 user-service 的信用/冻结逻辑实时同步。

## 配置要点

- 所有服务共用 `security.jwt.secret` 与 `security.jwt.internal-access-token`，请在部署时统一覆盖；内部调用统一在 Header 中写入 `X-Internal-Token`（默认 `flexlease-internal-secret`）。
- `flexlease.bootstrap.admin.username/password` 控制认证服务默认管理员账号；`FLEXLEASE_STORAGE_ROOT` 指定商品媒体文件目录；`FLEXLEASE_*_BASE_URL` 用于跨服务调用（order→product/payment/notification 等）。
- `FLEXLEASE_PAYMENT_AUTO_CONFIRM`（或 `flexlease.payment.auto-confirm`）控制支付是否自动成功；`FLEXLEASE_ORDER_MAINTENANCE_PENDING_PAYMENT_EXPIRE_MINUTES` 与 `FLEXLEASE_ORDER_MAINTENANCE_SCAN_INTERVAL_MS` 调整待支付超时策略；`FLEXLEASE_MESSAGING_ENABLED` 与 `FLEXLEASE_REDIS_ENABLED` 可在开发环境禁用 RabbitMQ 或 Redis 依赖。
- `flexlease.notification-service.base-url` 被多个服务用于调用通知服务（站内信），如需联调自定义域名请统一覆盖相关服务配置。
- `flexlease.order.proof-policy.*`（如 `shipment-photo-required/shipment-video-required/receive-photo-required/receive-video-required/return-photo-required/return-video-required`）与 `FLEXLEASE_ORDER_PROOF_ROOT` 控制取证最低数量与存储目录，可按实际履约规范调整照片/视频要求及水印文案。
- “纠纷仲裁建议”默认走离线模板输出（无需外网/Key，便于演示与 E2E）；如需接入外部 LLM，在仓库根目录创建 `.env`（参考 `.env.example`），设置 `FLEXLEASE_LLM_ENABLED=true` 并填写 `FLEXLEASE_LLM_API_KEY`。详见 `docs/纠纷仲裁智能助手.md`。

## 多角色能力速览

- **平台管理员**
  - 入驻/商品审核：`/app/admin/review` 汇总厂商入驻与商品审核任务，分别调用 `user-service` 与 `product-service` 的审核接口。
  - 订单监控：`/app/admin/orders` 支持按用户/厂商/状态过滤，并提供“仅人工审核”筛选（`manualReviewOnly`）用于快速定位信用预警订单（风险关注标记，不阻断支付/履约），抽屉内嵌电子合同查看、操作时间线与 `/admin/orders/{id}/force-close`。
- **厂商**
  - 商品与库存：`/app/vendor/workbench/products`（`frontend/src/views/vendor/workbench/ProductBoardView.vue`）可配置方案、SKU、媒体并调用 `/inventory/adjust`。
  - 履约操作：`/app/vendor/workbench/fulfillment`（`frontend/src/views/vendor/workbench/FulfillmentBoardView.vue` + `frontend/src/views/vendor/workbench/FulfillmentDetailSheet.vue`）覆盖发货、续租/退租/买断审批、订单留言、凭证上传与纠纷处理。
  - 指标与结算：`/app/vendor/workbench/insights`、`/app/vendor/workbench/settlement` 分别调用 `/analytics/vendor/{vendorId}`、`/payments/settlements`，依赖登录会话携带的 `vendorId`（缺少时需重新登录）。
- **消费者**
  - 自助下单：商品目录 → 详情 → 购物车/结算页 → `/orders` & `/payments` 的试算、下单与自动支付流程。
  - 订单售后：详情页直接操作续租/退租/买断/确认收货、上传取证、发起/回复纠纷，会自动触发通知与站内信；纠纷结案后收到满意度调查邀请。
  - 通知中心：基于 `/notifications/logs` 的时间轴展示最新站内信，可交叉验证订单事件。

## 运行指南

### Docker Compose（推荐）

```bash
docker compose up --build
```

命令会依次构建并启动数据库、Redis、RabbitMQ、Eureka、所有微服务以及 Nginx 容器，首次执行耗时取决于网络与 Maven 缓存。完成后可访问：

- 统一入口（管理端 + `/api` 反代到网关）：http://localhost:8080
- Eureka 面板：http://localhost:8761
- RabbitMQ 控制台：http://localhost:15672（`guest/guest`）

> Compose 会挂载 `product-media` 卷存放商品图片，其余数据存放在 `postgres-data`、`redis-data`、`rabbitmq-*` 卷中。停止环境使用 `docker compose down`，如需清理持久化可加 `-v`。

### 本地开发

1. **后端单服务**：`SPRING_PROFILES_ACTIVE=dev` 使用 H2 + 自动建 schema，或 `postgres` profile 连接本地数据库；服务间调用默认走 Eureka 服务发现，若要联调请优先使用 Compose（或至少启动 `registry-service` + 依赖服务）。
   ```bash
   ./mvnw -pl backend/order-service -am spring-boot:run
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
   ./mvnw -pl backend/product-service -am test
   npm run build && npm run test:e2e
   ```

### 功能体验建议

1. 通过 `/api/v1/auth/register/vendor` 注册厂商账号 → 登录管理端 → 填写入驻资料 → 管理员在 `/vendors/applications/{id}/approve` 审核通过（认证中心自动激活账号）。
2. 厂商工作台创建商品/方案/SKU，上传媒体、调整库存并提交审核，管理员在 `/api/v1/admin/products/**` 审核后即可在 Catalog 中看到。
3. 消费者登录 → 浏览 `/app/catalog` → 进入商品详情 → 加入购物车/直接试算 `/orders/preview` → 创建订单（可附带 `cartItemIds`）→ 支付由 `payment-service` 自动确认。
4. 在商品详情底部提交“下单前咨询”，并在商品详情页查看咨询记录、倒计时与厂商回复；厂商在工作台的咨询抽屉查看与回复，消费者也会收到通知中心提醒。
5. 在订单详情体验续租/退租/买断、支付回执与电子合同签署，并查看凭证卡片展示的 `/api/v1/proof-policy` 拍摄指引与水印示例，同时打开通知中心验证 `/notifications/logs` 的最新日志。
6. 切换到厂商角色，在订单履约抽屉中完成发货/审批，接着打开运营指标（含抽成洞察）与结算页面（若缺少 `vendorId`，请退出账号后重新登录）。
7. 切换管理员查看订单监控列表、过滤条件与合同抽屉，并使用强制关闭演练补偿流程；如需纯 API 调试可直接参考 `docs/API设计.md` 并用 `curl`/IDE HTTP Client 发起请求。
8. 使用仲裁管理人员账号进入 `/app/arbitration/orders`，对升级仲裁的纠纷生成仲裁建议并提交裁决（可在同一抽屉中查看聊天/凭证/时间线）。

## 辅助脚本

- `scripts/dashboard-report.mjs`：调用 `/analytics/dashboard` 生成 Markdown 报表，默认写入 `reports/`。运行前请准备管理员或 INTERNAL 角色 token，并按需覆盖环境变量：

  ```bash
  FLEXLEASE_API_BASE=http://localhost:8080/api/v1 \
  FLEXLEASE_API_TOKEN=<JWT> \
  FLEXLEASE_REPORT_DIR=reports \
  node scripts/dashboard-report.mjs
  ```

  也可使用内部访问令牌（无需 JWT）：

  ```bash
  FLEXLEASE_API_BASE=http://localhost:8080/api/v1 \
  FLEXLEASE_INTERNAL_TOKEN=flexlease-internal-secret \
  FLEXLEASE_REPORT_DIR=reports \
  node scripts/dashboard-report.mjs
  ```

  报表会输出核心 GMV/订单指标、信用分布、纠纷耗时与满意度情况，可直接作为周报/复盘附件。

## 测试与质量

- `backend/auth-service`：`AuthServiceApplicationTests` 覆盖注册/登录/刷新/密码重置，确保默认管理员初始化与 JWT 解析配置无误。
- `backend/user-service`：`UserServiceApplicationTests` 演练厂商入驻到审批流程，校验对认证中心内部接口的启用与厂商 ID 绑定，并新增 `internalCreditEventEndpointUpdatesScore` 场景验证 `/credit-events` 奖惩逻辑。
- `backend/product-service`：`ProductServiceIntegrationTest` 走通商品创建→方案/SKU→库存调整→审核→ Catalog 暴露的完整链路。
- `backend/order-service`：`RentalOrderServiceIntegrationTest` 验证购物车合并、库存预占/释放、支付回执、发货、售后、电子合同与 `OrderMaintenanceScheduler`；`OrderAnalyticsServiceIntegrationTest` 校验 `/analytics/**` 聚合。
- `backend/payment-service`：`PaymentTransactionServiceIntegrationTest`（自动确认 + 结算）、`PaymentTransactionServiceManualFlowTest`（手动确认与失败回调）覆盖退款/分账/事件回调。
- `backend/notification-service`：`NotificationServiceIntegrationTest` 检查模板渲染与 Redis 缓存，`OrderEventListener` 监听 RabbitMQ 并推送厂商提醒。
- `./mvnw -pl backend/product-service -am -Dtest=InventoryReservationConcurrencyTest -Dsurefire.failIfNoSpecifiedTests=false test` 可直接触发库存高并发回归，验证乐观锁 + 自动重试链路。
- `./mvnw clean verify` 在 H2 + Flyway 下执行，CI 挂载 PostgreSQL 验证脚本一致性；前端执行 `npm run build` 完成产物校验。
- `platform-common` 提供异常枚举、JWT 解析、幂等工具、消息常量等基础能力，保障跨服务契约。

### 前端 E2E（Playwright）

前端提供 1 条端到端脚本，覆盖 `docs/用户故事地图.md` 的旅程主线（A-F）与主题要点（信用/沟通/抽成/证明/矛盾），适合演示“完整闭环”：

- 注册登录（消费者/厂商）→ 厂商入驻 → 商品上架与审核
- 租前咨询（消费者发起，厂商回复）
- 下单与支付（目录直达下单 + 购物车试算下单）
- 订单沟通（双方聊天）
- 履约证明（发货/收货凭证上传）
- 巡检请求与信用加分校验
- 纠纷升级 → 仲裁管理人员生成仲裁建议并裁决（信用扣分）
- 买断申请 → 厂商审批
- 结算中心（平台抽成）/驾驶舱（信用与纠纷）/通知中心（CREDIT/DISPUTE）

**前置条件**

1. 保证网关与前端可访问：打开 http://localhost:8080 能看到登录页。
   - 推荐直接使用 `docker compose up --build` 拉起全套环境。
   - 启动后建议等待约 60 秒，确保全部服务完成注册与初始化。
2. “生成仲裁建议”支持两种模式：
   - **离线模板模式（默认）**：无需外网/Key，直接运行即可（输出会标注“离线模式”）。
   - **外部 LLM 模式（可选）**：`cp .env.example .env`，设置 `FLEXLEASE_LLM_ENABLED=true` 并填写 `FLEXLEASE_LLM_API_KEY`。
3. 安装前端依赖与 Playwright 浏览器：

```powershell
cd frontend
npm install
npx playwright install
```

**运行（默认 headless）**

```powershell
cd frontend
npm run test:e2e
```

**演示（headed + 三窗口并排）**

该脚本会同时打开 3 个窗口（管理员 / 厂商 / 消费者），用于现场讲解多角色协作。

```powershell
cd frontend
$env:E2E_DEMO_CHROME_SCALE_FACTOR=1  # 建议：抵消 Windows 200% 缩放造成的“看起来放大”
npm run test:e2e -- --headed
```

**常用环境变量**（可按屏幕大小与偏好微调）

- 目标地址：
  - `E2E_BASE_URL`：默认 `http://localhost:8080`
- 演示节奏：
  - `E2E_SLOW_MO_MS`：headed 演示每步延迟（默认 200ms）
- 三窗口布局（单位：像素；默认值已适配 3200×2000 + 200% 缩放的演示环境，可直接用）
  - `E2E_DEMO_WIN_W` / `E2E_DEMO_WIN_H`：每个窗口大小
  - `E2E_DEMO_WIN_GAP`：窗口间距
  - `E2E_DEMO_WIN_TOP`：顶部偏移
  - `E2E_DEMO_WIN_MARGIN`：左右边距
- 桌面端渲染（避免移动端抽屉/侧边栏动画影响演示稳定性）
  - `E2E_DEMO_RENDER_MODE`：默认 `desktop`（可切换 `mobile`）
  - `E2E_DEMO_DSF`：deviceScaleFactor（默认 0.5，用于“桌面端布局塞进小窗口”）
  - `E2E_DEMO_VIEWPORT_W` / `E2E_DEMO_VIEWPORT_H`：桌面端 viewport（默认 1040×1600）
- Windows 缩放相关（一般不用改）
  - `E2E_DEMO_CHROME_SCALE_FACTOR`：Chromium 启动时 `--force-device-scale-factor`（默认 1）
  - `E2E_DEMO_WIN_DPI_SCALE`：窗口坐标/大小换算倍率（默认自动推断；必要时可强制设为 2）

> 如果窗口“看起来太小/太大”，优先调 `E2E_DEMO_WIN_W/H`；如果内容密度不合适，调 `E2E_DEMO_DSF`。

## 文档索引

- 《项目说明》：`docs/项目说明-原始.md`
- 《项目规划与任务分解》：`docs/项目规划与任务分解.md`
- 《API 设计》：`docs/API设计.md`
- 《数据库设计》：`docs/数据库设计.md`
- 《用例设计》：`docs/用例设计.md`
- 《测试与质量策略》：`docs/测试与质量策略.md`
- 《纠纷仲裁智能助手》：`docs/纠纷仲裁智能助手.md`

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
