# 智能化共享租赁平台 API 设计

> 约定 RESTful 风格，统一前缀 `/api/v1`（下文表格中的 URL 默认省略该前缀；实际调用时请补上），所有响应使用统一包装：
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
| POST | `/auth/token` | 获取访问令牌 | `{ "username", "password" }` | `accessToken`, `expiresInSeconds`, `refreshToken`, `refreshExpiresInSeconds` |
| GET | `/auth/me` | 获取当前登录用户信息 | `Authorization: Bearer <token>` | `id`, `vendorId?`, `username`, `roles`, `lastLoginAt` |

> 说明：注册接口当前仅返回账号摘要，厂商入驻详情在用户服务中维护；`/auth/token` 使用 JSON 请求体而非表单，并同时返回访问令牌与刷新令牌。`/auth/me` 会在厂商审核通过并映射 vendorId 后返回 `vendorId` 字段，前端可据此加载厂商工作台。

### 2.2 账号管理（已实现）
| 方法 | URL | 描述 | 请求体要点 | 备注 |
| ---- | --- | ---- | ---------- | ---- |
| POST | `/auth/logout` | 客户端主动注销 | - | 无状态实现，前端清理 token 即可 |
| POST | `/auth/password/reset` | 重置密码 | `{ "username", "oldPassword", "newPassword" }` | 校验旧密码后写入新密文 |
| POST | `/auth/token/refresh` | 刷新令牌 | `{ "refreshToken" }` | 返回新的 `accessToken`、`refreshToken` 及各自剩余有效期 |

### 2.3 内部接口
> 内部接口供微服务之间调用，需在请求头中携带 `X-Internal-Token`，默认值可在配置中覆盖。

| 方法 | URL | 描述 | 备注 |
| ---- | --- | ---- | ---- |
| PATCH | `/internal/users/{userId}/status` | 更新指定账号状态 | 典型用例：厂商入驻审核通过后将账号从 `PENDING_REVIEW` 调整为 `ENABLED`；支持 `ENABLED`/`DISABLED`，请求体 `{ "status": "ENABLED" }` |
| PATCH | `/internal/users/{userId}/vendor` | 绑定认证账号与厂商 ID | 审核通过后由用户服务调用，确保 JWT 与 `/auth/me` 返回 `vendorId`，请求体 `{ "vendorId": "UUID" }` |
| POST | `/internal/users/{userId}/credit-events` | 记录信用事件，`eventType` 取值 `KYC_VERIFIED/ON_TIME_PAYMENT/EARLY_RETURN/LATE_PAYMENT/INSPECTION_COOPERATED/FRIENDLY_DISPUTE/MALICIOUS_BEHAVIOR`，`attributes` 可携带 `orderNo/disputeId/reason` 等上下文 | 仅 `INTERNAL` 角色可调用，用于触发信用分加减与站内信提醒，恶意行为会附带冻结信息 |

## 3. 用户 & 厂商管理（user-service）
### 3.1 厂商入驻
| 方法 | URL | 角色 | 描述 | 备注 |
| ---- | --- | ---- | ---- | ---- |
| POST | `/vendors/applications` | 厂商 | 提交入驻资料 | 需厂商账号登录；已被驳回的申请可在原记录上重新提交 |
| GET | `/vendors/applications/{id}` | 管理员/申请人 | 查看申请详情 | - |
| GET | `/vendors/applications` | 管理员/厂商 | 列表查询 | 管理员可查看全部；厂商仅能查看自己提交的记录，均支持 `status` 过滤 |
| POST | `/vendors/applications/{id}/approve` | 管理员 | 审核通过 | 请求体 `{ remark? }`，调用认证服务激活账号并同步厂商 `vendorId` 至认证中心 |
| POST | `/vendors/applications/{id}/reject` | 管理员 | 审核驳回 | 请求体 `{ remark? }` |

> `status` 取值 `DRAFT/SUBMITTED/APPROVED/REJECTED/SUSPENDED`，厂商重新提交时会在原记录上更新资料与 `submitted_at`。

### 3.2 厂商资料（已实现）
| 方法 | URL | 角色 | 描述 | 请求/响应要点 |
| ---- | --- | ---- | ---- | ------------- |
| GET | `/vendors` | ADMIN | 分页查询厂商 | 支持 `status`/`page`/`size`；返回 `PagedResponse<VendorResponse>` |
| GET | `/vendors/{vendorId}` | ADMIN/VENDOR | 查看详情 | 返回公司资料、联系人、地址、状态等 |
| PUT | `/vendors/{vendorId}` | VENDOR | 更新资料 | 请求体 `{ contactName, contactPhone, contactEmail?, province?, city?, address? }` |
| POST | `/vendors/{vendorId}/suspend` | ADMIN | 更新厂商状态 | 请求体 `{ status }`，支持 `ACTIVE`/`SUSPENDED` |
| PUT | `/vendors/{vendorId}/commission-profile` | ADMIN | 配置抽成策略（行业、基准费率、信用档位、SLA） | 请求体 `{ industryCategory, commissionBaseRate, commissionCreditTier, commissionSlaScore }` |

> 厂商账号需携带 JWT 中的 `vendorId` 才能访问；当账号仍在审批或尚未重新登录导致令牌缺少 `vendorId` 时，仅申请人本人可以继续查看/更新该厂商资料，其它厂商成员会被拒绝。

### 3.3 用户资料（已实现）
| 方法 | URL | 角色 | 描述 | 请求/响应要点 |
| ---- | --- | ---- | ---- | ------------- |
| GET | `/customers/profile` | USER | 获取个人资料 | 登录消费者账号后自动补建档 |
| PUT | `/customers/profile` | USER | 编辑个人资料 | 请求体 `{ fullName, gender, phone, email, address }`，`gender` 取值 `UNKNOWN/MALE/FEMALE` |
| GET | `/admin/users` | ADMIN | 查询用户列表 | 支持 `keyword` 模糊搜索姓名，返回分页 |
| PUT | `/admin/users/{userId}/status` | ADMIN | 启用/禁用用户 | 请求体 `{ status }`，通过认证服务内部接口生效 |
| POST | `/admin/users/{userId}/credit-adjustments` | ADMIN | 管理员人工调整信用分 | `{ delta, reason }`，同时写入信用历史并下发通知 |
| GET | `/admin/users/{userId}/credit-adjustments` | ADMIN | 查看信用调整记录 | 返回 `PagedResponse<CreditAdjustmentResponse>`（含 `delta/reason/operatorId/createdAt`） |

> `/customers/profile` 在首次访问时会自动补建档案；`/admin/users/{userId}/status` 实际由 user-service 代为调用认证服务 `/api/v1/internal/users/{id}/status`，请求头需携带 `X-Internal-Token`。

### 3.4 内部接口
| 方法 | URL | 角色 | 描述 | 备注 |
| ---- | --- | ---- | ---- | ---- |
| GET | `/api/v1/internal/vendors/{vendorId}/commission-profile` | INTERNAL | 供支付/订单等服务读取厂商当前抽成配置 | 返回行业分类、基准费率、信用档、最近一次 SLA 评分等字段，调用方根据该信息计算 `commissionRate` |

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
| POST | `/admin/products/{productId}/approve` | 审核通过 | 登录管理员后请求体 `{ remark? }`，置状态 `ACTIVE` |
| POST | `/admin/products/{productId}/reject` | 审核驳回 | 登录管理员后请求体 `{ remark? }`，置状态 `REJECTED` |

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
| POST | `/vendors/{vendorId}/products/{productId}/rental-plans/{planId}/skus/{skuId}/inventory/adjust` | 调整库存 | `{ changeType, quantity, referenceId? }`，`changeType` 取值 `INBOUND/OUTBOUND/RESERVE/RELEASE`，`quantity` 需 ≥1，执行后会写入 `inventory_snapshot` |

### 4.4 商品媒体资源
| 方法 | URL | 描述 | 请求/响应要点 |
| ---- | --- | ---- | ------------- |
| GET | `/vendors/{vendorId}/products/{productId}/media` | 查询商品媒体资源 | 返回按 `sortOrder` 排序的图片列表 |
| POST | `/vendors/{vendorId}/products/{productId}/media` | 上传媒体文件 | `multipart/form-data`，字段 `file`（必传），`sortOrder?`；返回上传后的 `MediaAsset` |
| PUT | `/vendors/{vendorId}/products/{productId}/media/{mediaId}/sort-order` | 调整显示顺序 | 请求体 `{ sortOrder }` |
| DELETE | `/vendors/{vendorId}/products/{productId}/media/{mediaId}` | 删除媒体文件 | 同步移除本地文件并更新列表 |

> 媒体资源会返回 `fileName/fileUrl/contentType/fileSize/sortOrder` 等字段；上传需使用 `multipart/form-data`，文件默认落盘至 `FLEXLEASE_STORAGE_ROOT` 配置的目录，由 `product-service` 对外暴露 `/media/**` 静态资源。

| 方法 | URL | 描述 | 请求/响应要点 |
| ---- | --- | ---- | ------------- |
| POST | `/vendors/{vendorId}/products/cover-image` | 上传商品封面图（未创建商品也可使用） | `multipart/form-data`，字段 `file`；返回 `FileUploadResponse`（`fileName/fileUrl/contentType/fileSize`），前端可将 `fileUrl` 回填到 `coverImageUrl` 字段 |
| DELETE | `/vendors/{vendorId}/products/cover-image` | 删除未使用的封面文件 | `fileName` 作为 query 参数，供用户取消创建商品或更换封面时清理临时文件 |

### 4.5 C 端商品展示
| 方法 | URL | 描述 | 响应要点 |
| ---- | --- | ---- | -------- |
| GET | `/catalog/products` | 商品搜索/分类过滤 | 返回 `ACTIVE` 商品的分页数据，可按 `keyword`/`categoryCode` 筛选，包含方案与库存摘要 |
| GET | `/catalog/products/{productId}` | 商品详情 | 仅允许查询 `ACTIVE` 商品，返回同上结构 |

### 4.6 库存内部接口
| 方法 | URL | 描述 | 请求体要点 |
| ---- | --- | ---- | ---------- |
| POST | `/internal/inventory/reservations` | 批量预占/释放库存（内部调用） | `{ referenceId, items: [{ skuId, quantity, changeType }] }`，`changeType` 取值 `RESERVE`/`RELEASE`/`INBOUND`/`OUTBOUND` |

> 请求需由内部服务发起并在 Header 附带 `X-Internal-Token`；接口会以数据库锁保证库存一致性，并在 `inventory_snapshot` 中记录流水。

### 4.7 商品咨询
| 方法 | URL | 角色 | 描述 | 请求/响应要点 |
| ---- | --- | ---- | ---- | ------------- |
| POST | `/catalog/products/{productId}/inquiries` | USER | 消费者在下单前提问，系统记录 72 小时有效的咨询 | `{ contactName?, contactMethod?, message }`，服务端自动带上 `requesterId` 并设置过期时间 |
| GET | `/catalog/products/{productId}/inquiries` | USER | 消费者查看自己在该商品下的咨询记录 | 返回数组，包含 `status/reply/expiresAt/respondedAt/createdAt`，用于展示 72 小时倒计时与厂商答复 |
| GET | `/vendors/{vendorId}/inquiries` | VENDOR | 厂商查看咨询列表 | 支持 `status`（`OPEN/RESPONDED/EXPIRED`）过滤；返回数组包含 `reply/expiresAt` |
| POST | `/vendors/{vendorId}/inquiries/{inquiryId}/reply` | VENDOR | 厂商回复咨询 | `{ reply }`，仅 `OPEN` 状态可回复，成功后触发通知 |

## 5. 订单与租赁流程（order-service，已实现）
### 5.1 下单与草稿
| 方法 | URL | 描述 | 请求体要点 | 响应要点 |
| ---- | --- | ---- | ---------- | -------- |
| POST | `/orders/preview` | 价格试算（押金、租金、合计） | `{ userId, vendorId, planType?, leaseStartAt?, leaseEndAt?, items: [{ productId, skuId?, planId?, productName, skuCode?, planSnapshot?, quantity, unitRentAmount, unitDepositAmount, buyoutPrice? }] }` | `depositAmount`, `rentAmount`, `totalAmount` |
| POST | `/orders` | 创建订单（可传 `items` 或 `cartItemIds`，服务端生成 `orderNo` 并固化明细快照） | `{ userId, vendorId, planType?, leaseStartAt?, leaseEndAt?, items?: [...], cartItemIds?: [], remark? }`<br>当 `cartItemIds` 提供时自动从购物车加载明细并清空对应条目 | `RentalOrderResponse`（含订单基础信息、明细、事件、续租/退租记录） |
| GET | `/orders/{orderId}` | 查看订单详情 | - | `RentalOrderResponse` |
| GET | `/orders` | 查询订单列表 | 需提供 `userId` 或 `vendorId` 其一，可选 `status`、`manualReviewOnly`、`page`、`size` | `PagedResponse<RentalOrderSummaryResponse>` |

> 注意：`userId` 会与当前登录用户二次校验；传入 `cartItemIds` 时要求所有条目属于同一 `vendorId`，成功下单后自动删除对应购物车记录；订单明细会调用 Catalog 校验 plan/sku，并更新 `planSnapshot` 以固化定价。

### 5.2 订单状态流转
| 方法 | URL | 角色 | 描述 | 请求体要点 |
| ---- | --- | ---- | ---- | ---------- |
| POST | `/orders/{orderId}/pay` | USER | 支付完成回执，订单从 `PENDING_PAYMENT` → `AWAITING_SHIPMENT` | `{ userId, paymentReference, paidAmount }`，`paymentReference` 为支付流水 ID（UUID 字符串），服务端会调用 payment-service 校验 |
| POST | `/internal/orders/{orderId}/payment-success` | INTERNAL | 支付服务回调订单成功 | `{ transactionId }`，幂等；需携带内部访问令牌 |
| POST | `/orders/{orderId}/cancel` | USER | 支付前取消订单 | `{ userId, reason? }` |
| POST | `/orders/{orderId}/ship` | VENDOR | 填写发货信息，订单进入 `IN_LEASE` | `{ vendorId, carrier, trackingNumber }` |
| POST | `/orders/{orderId}/confirm-receive` | USER | 确认收货，记录事件 | `{ actorId }` |
| POST | `/orders/{orderId}/inspection/request` | VENDOR | 发起巡检请求，提示用户补充巡检凭证 | `{ vendorId, remark? }` |
| POST | `/orders/{orderId}/extend` | USER | 发起续租申请 | `{ userId, additionalMonths, remark? }` |
| POST | `/orders/{orderId}/extend/approve` | VENDOR | 处理续租申请，更新租期 | `{ vendorId, approve, remark? }` |
| POST | `/orders/{orderId}/return` | USER | 发起退租申请，记录物流信息 | `{ userId, reason?, logisticsCompany?, trackingNumber? }` |
| POST | `/orders/{orderId}/return/approve` | VENDOR | 审核退租，更新状态（完成/退回租赁） | `{ vendorId, approve, remark? }` |
| POST | `/orders/{orderId}/return/complete` | VENDOR | 确认退租完成，触发入库与押金退款 | `{ vendorId, remark? }` |
| POST | `/orders/{orderId}/buyout` | USER | 申请买断，可调整买断金额 | `{ userId, buyoutAmount?, remark? }` |
| POST | `/orders/{orderId}/buyout/confirm` | VENDOR | 处理买断请求（通过/驳回） | `{ vendorId, approve, remark? }` |

> 厂商端订单操作需携带登录厂商的 `vendorId`，服务端会基于 JWT 内的厂商身份做二次校验，仅允许匹配租赁单的厂商执行操作；管理员/内部角色可跳过该限制以便干预。发货会自动触发库存 `OUTBOUND + RELEASE`，退租审批通过后需调用 `/return/complete`，届时才会执行 `INBOUND` 并按押金余额发起退款。

> `planSnapshot` 均为服务端认可的方案快照 JSON，字段包含 `planId`、`planType`、`termMonths`、`depositAmount`、`rentAmountMonthly`、`buyoutPrice`，前端/工具侧在构造请求或读取响应时请以该格式为准。

> 支付服务在标记成功后会调用 `/api/v1/internal/orders/{orderId}/payment-success`，订单服务将再次校验流水并自动更新状态，该接口为幂等设计。用户端的 `/orders/{orderId}/pay` 仍支持手动回执，两条路径都会核对支付金额与订单应付金额是否一致；若超过 `flexlease.order.maintenance.pending-payment-expire-minutes` 未支付，将由调度器自动取消并释放库存。

### 5.3 合同与票据（已实现）
| 方法 | URL | 描述 | 请求要点 |
| ---- | --- | ---- | -------- |
| GET | `/orders/{orderId}/contract` | 获取订单合同（若不存在则生成草稿） | Header 需携带访问令牌；返回 `OrderContractResponse`，包含编号、内容、签署状态 |
| POST | `/orders/{orderId}/contract/sign` | 用户签署订单合同 | `{ userId, signature }`；仅订单所属用户可签署，成功后返回更新后的 `OrderContractResponse` |
| GET | `/orders/{orderId}/events` | 获取状态日志 | 可通过 `RentalOrderResponse.events` 获取，独立接口待定 |

### 5.4 管理侧订单（已实现）
| 方法 | URL | 描述 | 请求要点 |
| ---- | --- | ---- | -------- |
| GET | `/admin/orders` | 分页检索订单 | 支持 `userId`、`vendorId`、`status`、`manualReviewOnly`、`page`、`size`；`manualReviewOnly=true` 时仅返回需人工审核的订单 |
| GET | `/admin/orders/{orderId}` | 查看详情 | 返回与 `/orders/{id}` 相同的订单详情 |
| POST | `/admin/orders/{orderId}/force-close` | 强制关闭订单 | `{ "adminId", "reason?" }`，将订单置为 `EXCEPTION_CLOSED` 并追加事件记录 |

### 5.5 购物车接口
| 方法 | URL | 描述 | 请求体要点 | 备注 |
| ---- | --- | ---- | ---------- | ---- |
| GET | `/cart` | 查询用户购物车 | `userId`（query 参数） | 返回 `List<CartItemResponse>` |
| POST | `/cart/items` | 新增/合并条目 | `{ userId, vendorId, productId, skuId, planId?, productName, skuCode?, planSnapshot?, quantity, unitRentAmount, unitDepositAmount, buyoutPrice? }` | 相同用户+SKU 会合并数量并刷新定价 |
| PUT | `/cart/items/{itemId}` | 更新数量 | `{ userId, quantity }` | 数量需 ≥1 |
| DELETE | `/cart/items/{itemId}` | 删除条目 | `userId`（query 参数） | - |
| DELETE | `/cart` | 清空购物车 | `userId`（query 参数） | - |

### 5.6 取证策略与指导
| 方法 | URL | 描述 | 备注 |
| ---- | --- | ---- | ---- |
| GET | `/proof-policy` | 返回发货、收货、退租阶段的最小照片/视频数量、拍摄指引、水印示例 | `watermarkExample` 默认为说明文本（如 `WATERMARK: 订单号 + 发货时间`），前端应按文本展示（若为 URL 则可打开）；配置来源于 `flexlease.order.proof-policy.*` |

> 下单传入 `cartItemIds` 后端会自动加载并移除对应购物车条目，同时触发库存预占。

### 5.6 订单沟通与取证
| 方法 | URL | 描述 | 请求体要点 | 备注 |
| ---- | --- | ---- | ---------- | ---- |
| POST | `/orders/{orderId}/messages` | 在时间线中追加沟通记录 | `{ actorId, message }` | 用户/厂商/管理员均可调用，后端会校验请求人与当前登录用户一致，并触发站内信提醒对端 |
| GET | `/orders/{orderId}/proofs` | 查看全部取证资料 | - | 返回列表按 `uploadedAt` 排序 |
| POST | `/orders/{orderId}/proofs` | 上传取证文件 | `multipart/form-data`：`actorId`、`proofType=SHIPMENT/RECEIVE/RETURN/INSPECTION/OTHER`、`description?`、`file` | 根据角色限制可选 `proofType`，成功后在时间线追加 `PROOF_UPLOADED` 事件并通过 Notification Service 通知对方 |
| GET | `/proofs/{fileName}` | 下载取证文件（鉴权） | - | 需携带 `Authorization`；服务端会根据取证材料所属订单校验访问权限，返回 `inline` 资源流（前端预览通常以 `blob` 方式拉取并生成临时 URL） |

> `ProofPolicyProperties` 中可配置发货/退租阶段所需的最少照片/视频数量，未满足要求时 `/orders/{id}/ship`、`/return/complete` 会拒绝操作。

### 5.7 订单纠纷与仲裁
| 方法 | URL | 角色 | 描述 | 备注 |
| ---- | --- | ---- | ---- | ---- |
| GET | `/orders/{orderId}/disputes` | USER/VENDOR/ADMIN | 查看纠纷列表 | 自动按时间排序，返回 `OrderDisputeResponse` |
| POST | `/orders/{orderId}/disputes` | USER/VENDOR | 发起纠纷 | `{ actorId, option, reason, remark?, phoneMemo?, attachmentProofIds? }`；`attachmentProofIds` 为引用 `OrderProof` ID 的数组，最多 5 个 |
| POST | `/orders/{orderId}/disputes/{disputeId}/responses` | USER/VENDOR | 针对纠纷给出方案或确认 | `{ actorId, option, accept, remark?, phoneMemo?, attachmentProofIds? }`，双方轮流响应，附件规则同上 |
| POST | `/orders/{orderId}/disputes/{disputeId}/escalate` | USER/VENDOR | 升级平台仲裁 | `{ actorId, reason? }`，系统也会在截止时间到期后自动升级 |
| POST | `/orders/{orderId}/disputes/{disputeId}/appeal` | USER/VENDOR | 对已结案纠纷发起申诉 | `{ actorId, reason? }`，最多一次 |
| POST | `/admin/orders/{orderId}/disputes/{disputeId}/resolve` | ADMIN | 平台裁决 | `{ decision, penalizeUserDelta?, remark? }`；`penalizeUserDelta` 为信用扣分，正数代表扣减 |

> 纠纷的每个阶段都会同步写入时间线，且通过 Notification Service 向双方推送站内信；平台裁决后会自动触发满意度调查。
>
> 申诉接口仅对 `CLOSED`（平台裁决结案）纠纷开放；申诉后纠纷进入 `PENDING_REVIEW_PANEL`，裁决接口会根据纠纷状态要求 `ADMIN` 或 `REVIEW_PANEL` 权限处理。

### 5.8 满意度调研
| 方法 | URL | 描述 | 请求体要点 |
| ---- | --- | ---- | ---------- |
| GET | `/orders/{orderId}/surveys` | 查看订单下的所有调查 | - |
| POST | `/orders/{orderId}/surveys/{surveyId}/submit` | 提交调查问卷 | `{ actorId, rating (1~5), comment? }` |

> `OrderSurveyScheduler` 会根据 `flexlease.order.survey.*` 定时激活状态为 `PENDING` 的调查并发送邀请，问卷完成后附加时间线事件与感谢通知。

### 5.9 内部数据接口
| 方法 | URL | 角色 | 描述 | 响应要点 |
| ---- | --- | ---- | ---- | -------- |
| GET | `/internal/vendors/{vendorId}/performance-metrics` | INTERNAL | user-service 读取厂商履约指标（SLA 计算） | `onTimeShipmentRate`、`totalDisputes`、`friendlyDisputes`、`cancellationRate`；返回 0~1 的比率值 |

## 6. 支付与结算（payment-service）
> 枚举说明：`scene` 取值 `DEPOSIT`/`RENT`/`BUYOUT`/`PENALTY`；`channel` 取值 `MOCK`/`ALIPAY`/`WECHAT`/`BANK_TRANSFER`；`status` 取值 `PENDING`/`SUCCEEDED`/`FAILED`。

| 方法 | URL | 描述 | 请求体要点 | 响应 |
| ---- | --- | ---- | -------- | ---- |
| POST | `/payments/{orderId}/init` | 创建支付单 | `{ userId, vendorId, scene, channel, amount, description?, splits?: [{ splitType, amount, beneficiary }] }` | `PaymentTransactionResponse` |
| GET | `/payments/{transactionId}` | 查询支付详情 | - | `PaymentTransactionResponse` |
| POST | `/payments/{transactionId}/confirm` | 后台手动确认支付成功 | `{ channelTransactionNo, paidAt? }` | `PaymentTransactionResponse` |
| POST | `/payments/{transactionId}/callback` | 模拟支付通道回调 | `{ status, channelTransactionNo, paidAt?, failureReason? }` | `PaymentTransactionResponse` |
| POST | `/payments/{transactionId}/refund` | 发起退款（立即成功） | `{ amount, reason? }` | `RefundTransactionResponse` |
| POST | `/internal/payments/{transactionId}/refund` | 内部服务触发退款（订单服务用于退租、押金返还） | `{ amount, reason? }`，Header 需携带 `X-Internal-Token` | `RefundTransactionResponse` |
| GET | `/payments/settlements` | 查询厂商结算汇总 | `vendorId?`、`from?`、`to?`、`refundFrom?`、`refundTo?`（ISO8601 时间） | `List<PaymentSettlementResponse>` |

> `PaymentTransactionResponse` 返回基础字段、`splits`（分账明细）以及 `refunds`（退款流水）。`PaymentSettlementResponse` 汇总厂商维度的总金额、押金/租金/买断/违约金拆分、已退款金额、净入账金额以及最近一次支付时间；可按支付时间和退款完成时间两个时间窗过滤。所有 `/internal/**` 接口均需携带 `X-Internal-Token` 以限制为服务间调用。

> 说明：`scene` 取值 `DEPOSIT/RENT/BUYOUT/PENALTY`，`splitType` 取值 `DEPOSIT_RESERVE/VENDOR_INCOME/PLATFORM_COMMISSION`；`/payments/{orderId}/init` 支持 `Idempotency-Key` 并拒绝同一订单 + 场景存在多条 `PENDING` 流水；`confirm` 与 `callback` 仅允许 ADMIN/INTERNAL 手动触发；外部退款接口面向管理员，`/internal/payments/**` 提供给订单服务在退租/押金归还时调用；`/payments/settlements` 管理员可查询任意厂商，厂商角色需要在 JWT 中具备 `vendorId` 且只能查询自身（未传 `vendorId` 时默认取当前厂商）。

## 7. 通知与运营（notification-service, analytics）
### 7.1 通知服务
| 方法 | URL | 描述 | 请求体要点 | 响应 |
| ---- | --- | ---- | -------- | ---- |
| POST | `/notifications/send` | 发送单条通知，支持模板渲染或自定义内容 | `{ templateCode?, channel?, recipient, subject?, content?, variables? }`<br>当 `templateCode` 指定时，可省略 `channel/subject/content`，系统按模板渲染；`variables` 为键值对用于替换 `{{key}}` 占位符。 | `NotificationLogResponse`（包含通知 ID、渠道、状态、发送时间等）|
| GET | `/notifications/logs` | 查询最近 50 条通知记录，可按状态/接收方/上下文过滤 | `status?=PENDING|SENT|FAILED`、`recipient?=<userId/vendorId>`、`contextType?`、`channel?` | `List<NotificationLogResponse>` |
| GET | `/notifications/templates` | 查看系统内置模板 | - | `List<NotificationTemplateResponse>` |

> 目前通知发送为模拟实现：若渠道为 `EMAIL` 且收件人不为合法邮箱格式，将返回校验错误；其它渠道默认视为发送成功并写入通知日志。`/notifications/logs` 会根据当前角色自动收敛可见范围——消费者仅能查看自身 `userId`，厂商仅能查看其 `vendorId`，管理员/内部可查看任何 `recipient` 或省略参数获取全局 Top 50。可结合 `contextType`（如 `DISPUTE`、`CREDIT`）与 `channel` 精确过滤。

> 说明：运营指标接口实际由订单服务提供，仍透出在 `/api/v1/analytics/**` 路径下，网关/前端需路由至 `order-service`。

### 7.2 运营看板
| 方法 | URL | 描述 | 响应字段 |
| ---- | --- | ---- | -------- |
| GET | `/analytics/dashboard` | 平台级运营指标（ADMIN/INTERNAL） | `totalOrders`、`activeOrders`、`totalGmv`、`inLeaseCount`、`pendingReturns`、`ordersByStatus`（Map，键为 `OrderStatus` 枚举）、`recentTrend`（最近 7 日 GMV/订单趋势数组）、`planBreakdown`（按租赁模式拆分的订单/Gmv 汇总）|
| GET | `/analytics/vendor/{vendorId}` | 指定厂商的运营指标 | `vendorId`、`totalOrders`、`activeOrders`、`totalGmv`、`inLeaseCount`、`pendingReturns`、`ordersByStatus`、`recentTrend`、`planBreakdown` |

`recentTrend` 数组结构：`[{ "date": "2025-01-01", "orders": 5, "gmv": 1234.56 }]`，按日期顺序补齐 7 天数据。`planBreakdown` 返回每个租赁模式的 `planType`、`orders`、`gmv`，用于前端绘制占比卡片。

> GMV 合计包含待发货、租赁中、退租中、已完成以及买断相关订单；`activeOrders` 聚合待发货、租赁中、退租处理中及买断申请的订单量。返回金额使用 `BigDecimal`（两位小数）。

> `/analytics/dashboard` 仅 ADMIN/INTERNAL 账号可访问；`/analytics/vendor/{vendorId}` 允许管理员查看任意厂商，厂商角色将强制使用自身 `vendorId`。

## 8. 网关与前端约定
- 所有微服务注册到 Eureka（`registry-service`，端口 8761），网关根据路径转发：
  - `/api/v1/auth/**` → auth-service
  - `/api/v1/users/**` `/api/v1/vendors/**`（除 `/api/v1/vendors/*/inquiries/**`） → user-service
  - `/api/v1/products/**` `/api/v1/catalog/**` `/api/v1/vendors/*/inquiries/**` → product-service
  - `/api/v1/orders/**` `/api/v1/cart/**` → order-service
  - `/api/v1/payments/**` → payment-service
  - `/api/v1/notifications/**` → notification-service
  - `/api/v1/analytics/**` → order-service
- 前端 SPA 使用 axios 拦截器统一注入 token 与错误提示。

## 9. 事件与集成接口
- **订单事件消息总线**：`order-service` 将 `OrderCreated`、`OrderPaid`/`PaymentConfirmed`、`OrderShipped`、`OrderCancelled` 等状态以 JSON 消息发布到 RabbitMQ `order.events` 主题交换机（路由键 `order.*`）。`notification-service` 订阅 `order.events.notification` 队列，当前已基于 `ORDER_CREATED` 事件向厂商推送“新订单待处理”站内通知，后续可扩展更多消费者。
- **支付回调**：支付服务在模拟成功后推送 `PaymentSucceeded` 消息（内部 HTTP 调用），订单服务校验流水后更新订单状态并追加订单事件。
- **库存同步**：库存预占/释放仍通过 `product-service` 暴露的内部接口完成，待后续视需要迁移至消息驱动模式。

## 10. 后续细化清单
- 请求/响应 DTO 需在编码前补充字段类型、示例。
- OpenAPI 3.0 文档将通过 SpringDoc 自动生成，维护在 `docs/openapi`。
- 外部电商商品同步已确认不在当前范围内，平台以自建商品管理和模拟支付闭环支撑核心流程。
