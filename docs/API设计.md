# 智能化共享租赁平台 API 设计

> 约定 RESTful 风格，统一前缀 `/api/v1`，所有响应使用统一包装：
> ```json
> {
>   "code": 0,
>   "message": "ok",
>   "data": {}
> }
> ```
> 认证方式为 OAuth2 + JWT，访问令牌放置在 `Authorization: Bearer <token>` 头部。除 `auth` 模块外，其余接口均需鉴权。角色控制通过 RBAC 校验。

- **分页**：使用 `PagedResponse` 包装，字段包含 `content`、`page`、`size`、`totalElements`、`totalPages`。

## 1. 公共与约定
- **HTTP 状态码**：200 成功；4xx 客户端错误；5xx 服务错误。
- **分页参数**：`page`（从 1 起）、`size`、`sort`（如 `createdAt,desc`）。
- **时间格式**：ISO 8601（UTC），示例 `2025-01-15T08:00:00Z`。
- **幂等**：下单、支付相关接口支持 Idempotency-Key 请求头。
- **错误码**（参考 `platform-common` 枚举）：
  - `0` 成功
  - `1001` 参数校验失败
  - `1002` 资源不存在
  - `1003` 资源已存在
  - `2001` 未认证
  - `2003` 无权访问
  - `2004` 凭证错误
  - `5000` 系统异常

## 2. 认证与账号
### 2.1 注册登录（已实现）
| 方法 | URL | 描述 | 请求体 | 响应关键字段 |
| ---- | --- | ---- | ------ | ------------ |
| POST | `/auth/register/customer` | C 端注册 | `{ "username", "password" }` | `id`, `username`, `roles` |
| POST | `/auth/register/vendor` | 厂商管理员注册 | `{ "username", "password" }` | `id`, `username`, `roles` |
| POST | `/auth/token` | 获取访问令牌 | `{ "username", "password" }` | `accessToken`, `expiresInSeconds` |
| GET | `/auth/me` | 获取当前登录用户信息 | `Authorization: Bearer <token>` | `id`, `username`, `roles`, `lastLoginAt` |

> 说明：注册接口当前仅返回账号摘要，厂商入驻详情在用户服务中维护；`/auth/token` 使用 JSON 请求体而非表单。

### 2.2 账号管理（规划中）
| 方法 | URL | 描述 | 状态 |
| ---- | --- | ---- | ---- |
| POST | `/auth/logout` | 注销（失效 token） | 待实现 |
| POST | `/auth/password/reset` | 重置密码 | 待实现 |
| POST | `/auth/token/refresh` | 刷新令牌 | 待实现 |

### 2.3 内部接口
> 内部接口供微服务之间调用，需在请求头中携带 `X-Internal-Token`，默认值可在配置中覆盖。

| 方法 | URL | 描述 | 备注 |
| ---- | --- | ---- | ---- |
| PATCH | `/internal/users/{userId}/status` | 更新指定账号状态 | 典型用例：厂商入驻审核通过后将账号从 `PENDING_REVIEW` 调整为 `ENABLED`；支持 `ENABLED`/`DISABLED`，请求体 `{ "status": "ENABLED" }` |

## 3. 用户 & 厂商管理（user-service）
### 3.1 厂商入驻
| 方法 | URL | 角色 | 描述 | 备注 |
| ---- | --- | ---- | ---- | ---- |
| POST | `/vendors/applications` | 厂商 | 提交入驻资料 | 需要 `X-User-Id` 头标识申请人 |
| GET | `/vendors/applications/{id}` | 管理员/申请人 | 查看申请详情 | - |
| GET | `/vendors/applications` | 管理员 | 列表查询 | 支持 `status` 过滤 |
| POST | `/vendors/applications/{id}/approve` | 管理员 | 审核通过 | 请求体 `{ reviewerId, remark }`，调用认证服务激活账号 |
| POST | `/vendors/applications/{id}/reject` | 管理员 | 审核驳回 | 请求体 `{ reviewerId, remark }` |

### 3.2 厂商资料（规划中）
| 方法 | URL | 角色 | 描述 |
| ---- | --- | ---- | ---- |
| GET | `/vendors` | ADMIN | 分页查询厂商 |
| GET | `/vendors/{vendorId}` | ADMIN/VENDOR | 查看详情 |
| PUT | `/vendors/{vendorId}` | VENDOR | 更新资料 |
| POST | `/vendors/{vendorId}/suspend` | ADMIN | 冻结账号 |

### 3.3 用户资料（规划中）
| 方法 | URL | 角色 | 描述 |
| ---- | --- | ---- | ---- |
| GET | `/customers/profile` | USER | 获取个人资料 |
| PUT | `/customers/profile` | USER | 编辑个人资料 |
| GET | `/admin/users` | ADMIN | 查询用户列表 |
| PUT | `/admin/users/{userId}/status` | ADMIN | 启用/禁用用户 |

## 4. 商品与租赁方案（product-service）
### 4.1 商品管理（B 端）
| 方法 | URL | 描述 | 备注 |
| ---- | --- | ---- | ---- |
| POST | `/vendors/{vendorId}/products` | 创建商品 | 请求体 `{ name, categoryCode, description?, coverImageUrl? }` |
| GET | `/vendors/{vendorId}/products` | 商品列表 | 支持 `page/size/status/keyword` 查询 |
| GET | `/vendors/{vendorId}/products/{productId}` | 商品详情 | 返回租赁方案 & SKU 列表 |
| PUT | `/vendors/{vendorId}/products/{productId}` | 修改商品 | 审核中商品不可修改 |
| POST | `/vendors/{vendorId}/products/{productId}/submit` | 提交审核 | 需至少配置 1 个租赁方案 |
| POST | `/vendors/{vendorId}/products/{productId}/shelve` | 上/下架商品 | 请求体 `{ "publish": true|false }`，仅 ACTIVE/INACTIVE 状态转换 |

### 4.2 商品审核（管理员）
| 方法 | URL | 描述 | 备注 |
| ---- | --- | ---- | ---- |
| GET | `/admin/products` | 商品列表 | 默认筛选待审核，可指定 `status` |
| POST | `/admin/products/{productId}/approve` | 审核通过 | 请求体 `{ reviewerId, remark? }`，置状态 `ACTIVE` |
| POST | `/admin/products/{productId}/reject` | 审核驳回 | 请求体 `{ reviewerId, remark? }`，置状态 `REJECTED` |

### 4.3 租赁方案与 SKU
| 方法 | URL | 描述 | 备注 |
| ---- | --- | ---- | ---- |
| GET | `/vendors/{vendorId}/products/{productId}/rental-plans` | 查询方案 | - |
| POST | `/vendors/{vendorId}/products/{productId}/rental-plans` | 新增方案 | `{ planType, termMonths, depositAmount, rentAmountMonthly, ... }` |
| PUT | `/vendors/{vendorId}/products/{productId}/rental-plans/{planId}` | 更新方案 | 启用状态需先停用 |
| POST | `/vendors/{vendorId}/products/{productId}/rental-plans/{planId}/activate` | 启用方案 | - |
| POST | `/vendors/{vendorId}/products/{productId}/rental-plans/{planId}/deactivate` | 停用方案 | - |
| POST | `/vendors/{vendorId}/products/{productId}/rental-plans/{planId}/skus` | 新增 SKU | `{ skuCode, attributes?, stockTotal, stockAvailable?, status? }` |
| PUT | `/vendors/{vendorId}/products/{productId}/rental-plans/{planId}/skus/{skuId}` | 编辑 SKU | SKU 编码唯一校验 |
| POST | `/vendors/{vendorId}/products/{productId}/rental-plans/{planId}/skus/{skuId}/inventory/adjust` | 调整库存 | `{ changeType, quantity, referenceId? }`，自动记录库存流水 |

### 4.4 C 端商品展示
| 方法 | URL | 描述 | 响应要点 |
| ---- | --- | ---- | -------- |
| GET | `/catalog/products` | 商品搜索/分类过滤 | 返回分页数据，包含商品摘要与可用方案/库存概览 |
| GET | `/catalog/products/{productId}` | 商品详情 | 仅允许查询 `ACTIVE` 商品，返回同上结构 |

## 5. 订单与租赁流程（order-service，规划中）
### 5.1 下单与草稿
| 方法 | URL | 描述 | 请求体要点 | 响应要点 |
| ---- | --- | ---- | ---------- | -------- |
| POST | `/orders/preview` | 价格试算（押金、租金、合计） | `{ userId, vendorId, planType?, leaseStartAt?, leaseEndAt?, items: [{ productId, skuId?, planId?, productName, skuCode?, planSnapshot?, quantity, unitRentAmount, unitDepositAmount, buyoutPrice? }] }` | `depositAmount`, `rentAmount`, `totalAmount` |
| POST | `/orders` | 创建订单（与预览结构一致，服务端生成 `orderNo` 并固化明细快照） | 同上 | `RentalOrderResponse`（含订单基础信息、明细、事件、续租/退租记录） |
| GET | `/orders/{orderId}` | 查看订单详情 | - | `RentalOrderResponse` |
| GET | `/orders` | 查询订单列表 | 需提供 `userId` 或 `vendorId` 其一，可选 `status`、`page`、`size` | `PagedResponse<RentalOrderSummaryResponse>` |

### 5.2 订单状态流转
| 方法 | URL | 角色 | 描述 | 请求体要点 |
| ---- | --- | ---- | ---- | ---------- |
| POST | `/orders/{orderId}/pay` | USER | 支付完成回执，订单从 `PENDING_PAYMENT` → `AWAITING_SHIPMENT` | `{ userId, paymentReference(UUID), paidAmount }` |
| POST | `/orders/{orderId}/cancel` | USER | 支付前取消订单 | `{ userId, reason? }` |
| POST | `/orders/{orderId}/ship` | VENDOR | 填写发货信息，订单进入 `IN_LEASE` | `{ vendorId, carrier, trackingNumber }` |
| POST | `/orders/{orderId}/confirm-receive` | USER | 确认收货，记录事件 | `{ actorId }` |
| POST | `/orders/{orderId}/extend` | USER | 发起续租申请 | `{ userId, additionalMonths, remark? }` |
| POST | `/orders/{orderId}/extend/approve` | VENDOR | 处理续租申请，更新租期 | `{ vendorId, approve, remark? }` |
| POST | `/orders/{orderId}/return` | USER | 发起退租申请，记录物流信息 | `{ userId, reason?, logisticsCompany?, trackingNumber? }` |
| POST | `/orders/{orderId}/return/approve` | VENDOR | 审核退租，更新状态（完成/退回租赁） | `{ vendorId, approve, remark? }` |
| POST | `/orders/{orderId}/buyout` | USER | 申请买断，可调整买断金额 | `{ userId, buyoutAmount?, remark? }` |
| POST | `/orders/{orderId}/buyout/confirm` | VENDOR | 处理买断请求（通过/驳回） | `{ vendorId, approve, remark? }` |

> 订单支付确认会回调支付服务核验流水，`paymentReference` 需提供支付服务返回的交易 UUID，金额必须与订单应付一致。

### 5.3 合同与票据（规划中）
| 方法 | URL | 描述 | 状态 |
| ---- | --- | ---- | ---- |
| GET | `/orders/{orderId}/contract` | 获取合同信息 | 待实现 |
| POST | `/orders/{orderId}/contract/sign` | 上传/生成合同 | 待实现 |
| GET | `/orders/{orderId}/events` | 获取状态日志 | 可通过 `RentalOrderResponse.events` 获取，独立接口待定 |

### 5.4 管理侧订单（规划中）
| 方法 | URL | 描述 | 状态 |
| ---- | --- | ---- | ---- |
| GET | `/admin/orders` | 分页检索订单（多条件）| 待实现 |
| GET | `/admin/orders/{orderId}` | 详情 | 待实现 |
| POST | `/admin/orders/{orderId}/force-close` | 强制关闭订单 | 待实现 |

## 6. 支付与结算（payment-service）
> 枚举说明：`scene` 取值 `DEPOSIT`/`RENT`/`BUYOUT`/`PENALTY`；`channel` 取值 `MOCK`/`ALIPAY`/`WECHAT`/`BANK_TRANSFER`；`status` 取值 `PENDING`/`SUCCEEDED`/`FAILED`。

| 方法 | URL | 描述 | 请求体要点 | 响应 |
| ---- | --- | ---- | -------- | ---- |
| POST | `/payments/{orderId}/init` | 创建支付单 | `{ userId, vendorId, scene, channel, amount, description?, splits?: [{ splitType, amount, beneficiary }] }` | `PaymentTransactionResponse` |
| GET | `/payments/{transactionId}` | 查询支付详情 | - | `PaymentTransactionResponse` |
| POST | `/payments/{transactionId}/confirm` | 后台手动确认支付成功 | `{ channelTransactionNo, paidAt? }` | `PaymentTransactionResponse` |
| POST | `/payments/{transactionId}/callback` | 模拟支付通道回调 | `{ status, channelTransactionNo, paidAt?, failureReason? }` | `PaymentTransactionResponse` |
| POST | `/payments/{transactionId}/refund` | 发起退款（立即成功） | `{ amount, reason? }` | `RefundTransactionResponse` |
| GET | `/payments/settlements` | 查询厂商结算汇总 | `vendorId?`、`from?`、`to?`、`refundFrom?`、`refundTo?`（ISO8601 时间） | `List<PaymentSettlementResponse>` |

> `PaymentTransactionResponse` 返回基础字段、`splits`（分账明细）以及 `refunds`（退款流水）。`PaymentSettlementResponse` 汇总厂商维度的总金额、押金/租金/买断/违约金拆分、已退款金额、净入账金额以及最近一次支付时间；可按支付时间和退款完成时间两个时间窗过滤。

## 7. 通知与运营（notification-service, analytics，规划中）
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| GET | `/notifications/logs` | 通知记录查询 |
| POST | `/notifications/send` | 调试用发送通知 |
| GET | `/analytics/dashboard` | 仪表盘指标（订单量、GMV、续租率等）|
| GET | `/analytics/vendor/{vendorId}` | 厂商维度数据 |

## 8. 网关与前端约定
- 所有微服务注册到 Nacos，以 `service-name` 暴露，网关根据路径转发：
  - `/api/v1/auth/**` → auth-service
  - `/api/v1/users/**` `/api/v1/vendors/**` → user-service
  - `/api/v1/products/**` `/api/v1/catalog/**` → product-service
  - `/api/v1/orders/**` → order-service
  - `/api/v1/payments/**` → payment-service
  - `/api/v1/notifications/**` `/api/v1/analytics/**` → notification-service
- 前端 SPA 使用 axios 拦截器统一注入 token 与错误提示。

## 9. 事件与集成接口
- **订单事件**：`OrderCreated`、`OrderPaid`、`OrderShipped`、`OrderCompleted`、`OrderCancelled`，通过消息队列广播。
- **支付回调**：支付服务在模拟成功后推送 `PaymentSucceeded`；订单服务监听更新状态。
- **库存同步**：商品服务监听 `OrderCreated` 扣减库存，`OrderCancelled` 回补。

## 10. 后续细化清单
- 请求/响应 DTO 需在编码前补充字段类型、示例。
- OpenAPI 3.0 文档将通过 SpringDoc 自动生成，维护在 `docs/openapi`。
- 对接外部（如 eBay/速卖通）留接口模块，可在后续迭代补充。
