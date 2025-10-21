# 数据库迁移说明

- 采用 Flyway 进行数据库版本管理，初始版本为 `V000__create_schema.sql`。
- 不同业务域分别维护独立的 PostgreSQL schema：`auth`、`user`、`product`、`order`、`payment`、`notification`。
- 后续迁移文件命名规则：`V<版本号>__<描述>.sql`，版本号递增，三位填充，例如 `V001__create_user_tables.sql`。
- 执行顺序由文件名决定，所有迁移脚本应幂等，并包含必要的回滚说明（如需）。
