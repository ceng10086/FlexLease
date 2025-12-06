# FlexLease 前端

基于 **Vite + Vue 3 + TypeScript** 的多角色管理端单页应用，覆盖消费者下单/售后、厂商商品与履约工作台、管理员审核与监控。配套 Pinia、Vue Router、Ant Design Vue，支撑 Docker Compose 与独立本地开发。

## 功能概览
- 🔐 鉴权与会话：登录表单调用 `/api/v1/auth/token`，Pinia 将访问/刷新令牌持久化到 `localStorage`，并在 HTTP 拦截器中自动刷新。
- 🧭 多角色导航：`AuthenticatedLayout` + `router/menu` 根据角色渲染菜单并在移动端自动折叠，`useViewport` 与面包屑/头像等组件统一头部体验。
- 🛒 商品目录 & 购物车：`views/catalog/ProductFeedView.vue`、`views/catalog/ProductDetailShell.vue`、`views/checkout/CartBoardView.vue` 与 `views/checkout/CheckoutShell.vue` 直连 `/catalog/products`、`/cart`、`/orders/preview`，支持瀑布流选品、Sticky CTA 以及购物车卡片化结算。
- 📃 订单详情与合同：`views/orders/OrderHubView.vue` + `views/orders/detail/*.vue` 拆分概览/聊天/凭证/时间线，配合 `components/orders/OrderContractDrawer.vue` 自动刷新 `/orders/{id}` 与 `/orders/{id}/contract`。
- 🔔 通知中心：`views/notifications/NotificationCenterView.vue` 调用 `/notifications/logs` 并提供状态/渠道/上下文筛选，卡片化展示站内信。
- 👤 个人资料：`views/profile/ProfileOverviewView.vue` 首次进入即触发 `/customers/profile` 建档，展示信用档位/冻结状态并允许修改姓名、联系方式与地址，保存后实时同步 user-service。
- 🧾 厂商工作台：`VendorWorkbenchLayout` 下的 `ProductBoardView`、`FulfillmentBoardView`、`InsightsBoardView` 与 `SettlementBoardView` 以 Tabs 串联商品/履约/指标/结算，配套 `ProductDetailDrawer` 完成方案&SKU 管理、库存调整与咨询回复。
- 🚚 厂商履约 & 结算：`FulfillmentBoardView` + `FulfillmentDetailSheet` 提供发货、续租/退租/买断审批、凭证墙与聊天抽屉；结算页基于 `/payments/settlements` 输出拆分卡片，依赖登录会话中的 `vendorId`。
- 🛂 管理员运营：`AdminReviewSuiteView` 将厂商&商品审核汇总到 Review Split View，`AdminOrderMonitorView` + `AdminOrderDetailDrawer` 具备过滤、聊天/凭证/时间线展示与 `/admin/orders/{id}/force-close`、纠纷裁决入口。
- 📊 仪表盘：`views/dashboard/DashboardHome.vue` 汇总平台 & 厂商 GMV、订单状态分布、7 日趋势、租赁模式构成、纠纷与信用统计，并挂载公告/快捷入口。
- ⚙️ 工程化：Ant Design Vue 组件库、`services/*.ts` API 封装、`stores/auth` Token 恢复，以及 `flexlease.payment.auto-confirm` 驱动的自动支付模拟。

## 快速开始
```powershell
cd frontend
npm install
npm run dev
```

> 需要 Node.js 20+ 与 npm 10+（与 CI 保持一致）。

开发环境默认通过 Vite 代理分别联调 9001～9006 服务，可在 `vite.config.ts` 调整；若希望统一走网关，可设置 `VITE_API_PROXY=http://localhost:8080`，构建时使用 `VITE_API_BASE_URL`（默认 `/api/v1`）指定 API 前缀。

## 生产构建
```powershell
npm run build
```

产物位于 `frontend/dist`，可交由网关或静态资源服务器托管。

## 后续迭代建议
- 引入 ESLint + Prettier、lint-staged/Commitlint 等工程规范，统一格式与提交校验。
- 增补组件级单测（Vitest/Testing Library）并扩充关键业务流的自动化测试，覆盖购物车结算、厂商履约与管理员强制关闭等场景。
- 通知中心 & 订单详情可加入“标记已读”“关键字搜索”“支付/退款流水”展示，并提供可配置的支付失败/延迟模拟，便于演练异常场景。
