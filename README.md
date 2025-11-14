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
  - 单点登录 + 多角色菜单：消费者（目录/购物车/订单/通知）・厂商（商品/订单/指标/结算）・管理员（入驻审核/商品审核/订单监控）。
  - 平台 & 厂商仪表盘提供 7 日 GMV/订单趋势、租赁模式构成与环比洞察，支持运营快速识别波动。
  - 自动支付模拟替换手动运营工具，Playwright 覆盖仪表盘渲染。

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
   需要代理网关可设置 `VITE_API_PROXY=http://localhost:8080`。
3. **常用命令**
   ```bash
   ./mvnw clean verify      # 全量构建+测试
   mvn -pl backend/product-service -am test
   npm run build && npm run test:e2e
   ```

### 功能体验建议

1. 通过 `/api/v1/auth/register/vendor` 注册厂商账号 → 登录前端 → 提交入驻申请 → 管理员审核通过。
2. 在厂商工作台创建商品/方案/SKU → 上传媒体 → 提交审核 → 管理员审批。
3. 消费者登录 → 浏览 Catalog → 试算 & 下单（可从购物车导入）→ 自动支付回调 → 厂商发货 → 用户确认收货。
4. 后续可体验续租/退租/买断/合同签署等流程，并在 `/api/v1/analytics/**`、`/api/v1/notifications/**` 验证指标与通知。
5. Postman 集合 `docs/postman/cart-api.postman_collection.json` 覆盖购物车 → 下单链路，可用于 API 调试。

## 测试与质量

- 后端各服务均包含 Spring Boot 集成测试：  
  `product-service`（商品生命周期）、`order-service`（下单/购物车/调度）、`payment-service`（支付+退款+结算）、`notification-service`（模板 & 日志）。
- `./mvnw clean verify` 在 H2 + Flyway 下执行，CI 同步 PostgreSQL 行为。  
  `npm run build && npm run test:e2e` 覆盖前端构建与 Playwright 仪表盘回归。
- 全局 `platform-common` 提供统一异常封装、JWT 解析、幂等工具、消息常量，保障跨服务一致性。

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
