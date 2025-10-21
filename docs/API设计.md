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

## 1. 公共与约定
- **HTTP 状态码**：200 成功；4xx 客户端错误；5xx 服务错误。
- **分页参数**：`page`（从 1 起）、`size`、`sort`（如 `createdAt,desc`）。
- **时间格式**：ISO 8601（UTC），示例 `2025-01-15T08:00:00Z`。
- **幂等**：下单、支付相关接口支持 Idempotency-Key 请求头。
- **错误码**：
  - `1000` 通用失败
  - `2001` 未登录/令牌失效
  - `300X` 业务错误（库存不足、状态不允许）

## 2. 认证与账号
### 2.1 注册登录
| 方法 | URL | 描述 | 请求体 | 响应 |
| ---- | --- | ---- | ------ | ---- |
| POST | `/auth/register/customer` | C 端注册 | `{username,password,phone,verificationCode}` | 创建用户 |
| POST | `/auth/register/vendor` | B 端厂商管理员注册 | `{username,password,companyName,...}` | 返回入驻申请编号 |
| POST | `/auth/token` | 获取访问令牌 | `grant_type=password` 或 `client_credentials` | `{access_token, refresh_token, expires_in}` |
| POST | `/auth/token/refresh` | 刷新令牌 | `{refresh_token}` | 新 token |

### 2.2 账号管理
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| GET | `/auth/me` | 获取当前登录用户信息 |
| POST | `/auth/logout` | 注销（失效 token） |
| POST | `/auth/password/reset` | 重置密码（短信/邮箱验证码） |

## 3. 用户 & 厂商管理（user-service）
### 3.1 厂商入驻
| 方法 | URL | 角色 | 描述 |
| ---- | --- | ---- | ---- |
| POST | `/vendors/applications` | VENDOR | 提交入驻资料 |
| GET | `/vendors/applications/{id}` | ADMIN/VENDOR(本人) | 查看申请详情 |
| GET | `/vendors/applications` | ADMIN | 分页查询（支持状态过滤） |
| POST | `/vendors/applications/{id}/approve` | ADMIN | 审核通过 |
| POST | `/vendors/applications/{id}/reject` | ADMIN | 审核驳回 |

### 3.2 厂商资料
| 方法 | URL | 角色 | 描述 |
| ---- | --- | ---- | ---- |
| GET | `/vendors` | ADMIN | 分页查询厂商 |
| GET | `/vendors/{vendorId}` | ADMIN/VENDOR | 查看详情 |
| PUT | `/vendors/{vendorId}` | VENDOR | 更新资料 |
| POST | `/vendors/{vendorId}/suspend` | ADMIN | 冻结账号 |

### 3.3 用户资料
| 方法 | URL | 角色 | 描述 |
| ---- | --- | ---- | ---- |
| GET | `/customers/profile` | USER | 获取个人资料 |
| PUT | `/customers/profile` | USER | 编辑个人资料 |
| GET | `/admin/users` | ADMIN | 查询用户列表 |
| PUT | `/admin/users/{userId}/status` | ADMIN | 启用/禁用用户 |

## 4. 商品与租赁方案（product-service）
### 4.1 商品管理（B 端）
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| POST | `/vendors/{vendorId}/products` | 创建商品 |
| GET | `/vendors/{vendorId}/products` | 商品列表 |
| GET | `/vendors/{vendorId}/products/{productId}` | 商品详情 |
| PUT | `/vendors/{vendorId}/products/{productId}` | 修改商品 |
| POST | `/vendors/{vendorId}/products/{productId}/submit` | 提交审核 |
| POST | `/vendors/{vendorId}/products/{productId}/shelve` | 上架/下架 |

### 4.2 商品审核（管理员）
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| GET | `/admin/products/pending` | 待审核列表 |
| POST | `/admin/products/{productId}/approve` | 审核通过 |
| POST | `/admin/products/{productId}/reject` | 审核驳回 |

### 4.3 租赁方案与 SKU
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| POST | `/products/{productId}/rental-plans` | 新增租赁方案 |
| GET | `/products/{productId}/rental-plans` | 列表 |
| PUT | `/rental-plans/{planId}` | 更新方案 |
| POST | `/rental-plans/{planId}/activate` | 启用 |
| POST | `/rental-plans/{planId}/deactivate` | 停用 |
| POST | `/rental-plans/{planId}/skus` | 新增 SKU |
| PUT | `/rental-plans/{planId}/skus/{skuId}` | 编辑 SKU |
| POST | `/rental-plans/{planId}/skus/{skuId}/inventory/adjust` | 调整库存 |

### 4.4 C 端商品展示
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| GET | `/catalog/products` | 商品搜索/分类过滤 |
| GET | `/catalog/products/{productId}` | 商品详情 |
| GET | `/catalog/products/{productId}/plans` | 方案列表 |

## 5. 订单与租赁流程（order-service）
### 5.1 下单与草稿
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| POST | `/orders/preview` | 价格试算（包含押金、租金）|
| POST | `/orders` | 创建订单（选择 SKU + 方案）|
| GET | `/orders/{orderId}` | 查看订单详情 |
| GET | `/orders` | 我的订单列表（支持状态过滤）|

### 5.2 订单状态流转
| 方法 | URL | 角色 | 描述 |
| ---- | --- | ---- | ---- |
| POST | `/orders/{orderId}/pay` | USER | 发起支付（押金/租金）|
| POST | `/orders/{orderId}/cancel` | USER | 支付前取消 |
| POST | `/orders/{orderId}/ship` | VENDOR | 填写发货信息 |
| POST | `/orders/{orderId}/confirm-receive` | USER | 确认收货 |
| POST | `/orders/{orderId}/extend` | USER | 续租申请 |
| POST | `/orders/{orderId}/extend/approve` | VENDOR | 审核续租 |
| POST | `/orders/{orderId}/return` | USER | 退租申请 |
| POST | `/orders/{orderId}/return/approve` | VENDOR | 退租处理 |
| POST | `/orders/{orderId}/buyout` | USER | 买断申请 |
| POST | `/orders/{orderId}/buyout/confirm` | VENDOR | 买断确认 |

### 5.3 合同与票据
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| GET | `/orders/{orderId}/contract` | 获取合同信息 |
| POST | `/orders/{orderId}/contract/sign` | 上传/生成合同 |
| GET | `/orders/{orderId}/events` | 获取状态日志 |

### 5.4 管理侧订单
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| GET | `/admin/orders` | 分页检索订单（多条件）|
| GET | `/admin/orders/{orderId}` | 详情 |
| POST | `/admin/orders/{orderId}/force-close` | 强制关闭订单 |

## 6. 支付与结算（payment-service）
| 方法 | URL | 描述 |
| ---- | --- | ---- |
| POST | `/payments/{orderId}/init` | 创建支付单（指定支付场景）|
| GET | `/payments/{transactionId}` | 查询支付状态 |
| POST | `/payments/{transactionId}/callback` | 支付渠道回调（对内）|
| POST | `/payments/{transactionId}/confirm` | 平台侧确认（模拟）|
| POST | `/payments/{transactionId}/refund` | 发起退款 |
| GET | `/payments/settlements` | 结算查询 |

## 7. 通知与运营（notification-service, analytics）
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
