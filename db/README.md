# 数据库迁移说明

FlexLease 采用 **Flyway + PostgreSQL**，按微服务拆分 schema。每个微服务在启动时都会校验/创建自身 schema 与表结构；根目录下的 `db/` 目录用于集中存放示例脚本及跨服务协作时的基线设计。

## 拆分策略

- 业务域与 schema 一一对应：`auth`、`users`、`product`、`order`、`payment`、`notification`。
- 统一审计/回放 schema：`audit`（`api_audit_log`、`business_replay_log`），用于 HTTP 请求级审计与跨服务消息回放（详见 `docs/日志与审计.md`）。
- 所有主键使用 `UUID`，时间采用 `TIMESTAMP WITH TIME ZONE`；金额字段以 `NUMERIC(18,2)` 为主（部分比例/快照字段会使用更适合的精度，例如抽成比例 `NUMERIC(5,4)`）。

## 目录布局

- `db/migration/<schema>/Vxxx__*.sql`：面向竞赛/文档的示例脚本，便于快速回顾当前 schema 的最新结构。
- `backend/<service>/src/main/resources/db/migration`：服务内实际执行的 Flyway 脚本，包含 DDL、种子数据及热修复脚本。
- 迁移命名规则：`V<版本号>__<描述>.sql`，版本号递增、三位补零，例如 `V003__add_order_dispute_tables.sql`。执行顺序完全由文件名决定。

> 约定：以 `backend/<service>/src/main/resources/db/migration` 为权威来源；根目录 `db/migration/<schema>` 更偏向“文档快照/精选脚本”，用于阅读与讲解，不要求覆盖每一次演进（如需对外分享或课设汇报，可按需同步更新）。

## 执行方式

1. **服务启动自动迁移**：`backend/*-service` 的 `application.yml` 已开启 `spring.flyway.enabled=true`，本地/Compose 环境下拉起服务即会迁移。
2. **手动迁移示例**（以 order-service 为例）：

   ```bash
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/flexlease \
   SPRING_DATASOURCE_USERNAME=flexlease \
   SPRING_DATASOURCE_PASSWORD=flexlease \
   ./mvnw -pl backend/order-service -am flyway:migrate
   ```

   当需要在 CI 或独立数据库上执行迁移时，可通过上述方式显式运行。

## 注意事项

- 所有脚本需保持幂等（避免重复创建资源），必要时可补充数据修复语句。
- 优先使用约束/索引命名规范：`idx_<table>_<column>`、`fk_<from>_<to>` 等，便于排查。
- 对历史数据有破坏性的迁移请在脚本顶部写明背景与回滚方式，并在 PR 描述中同步。
- 需要核对最终结构时，以各服务实际运行的 Flyway 脚本为准；`docs/数据库设计.md` 也会对关键表做说明。

> 补充：`audit` schema 的迁移脚本位于各服务内的 `V900__audit_logs.sql`，因此根目录 `db/migration/` 目录中可能不会单独出现 `audit/` 文件夹（以服务内脚本为准）。
