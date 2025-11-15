# FlexLease 前端

基于 **Vite + Vue 3 + TypeScript** 的多角色管理端单页应用，覆盖消费者下单/售后、厂商商品与履约工作台、管理员审核与监控。配套 Pinia、Vue Router、Ant Design Vue、Playwright，支撑 Docker Compose 与独立本地开发。

## 功能概览
- 🔐 鉴权与会话：登录表单调用 `/api/v1/auth/token`，Pinia 将访问/刷新令牌持久化到 `localStorage`，并在 HTTP 拦截器中自动刷新。
- 🧭 多角色导航：`AuthenticatedLayout` + `router/menu` 根据角色渲染菜单并在移动端自动折叠，`useViewport` 与面包屑/头像等组件统一头部体验。
- 🛒 商品目录 & 购物车：`CatalogPage`、`ProductDetailPage`、`CartPage` 与 `CheckoutPage` 直连 `/catalog/products`、`/cart`、`/orders/preview`，支持多方案对比、购物车合并及结算预估。
- 📃 订单详情与合同：`OrdersPage` 与 `OrderDetailPage` 统一展示订单、合同抽屉和续租/退租/买断/确认收货等操作，自动刷新 `/orders/{id}` 与 `/orders/{id}/contract`。
- 🔔 通知中心：`NotificationCenterPage` 调用 `/notifications/logs` 显示时间轴，便于验证订单事件与手动发送的站内信。
- 🧾 厂商工作台：`VendorProductWorkspacePage`、`components/vendor` 抽屉支持商品/方案/SKU CRU、库存调整与媒体上传，配合 `/vendors/{vendorId}/products/**`。
- 🚚 厂商履约 & 结算：`VendorOrderWorkspacePage` 提供发货、续租/退租/买断审批表单，`VendorAnalyticsPage`、`VendorSettlementPage` 调用 `/analytics/vendor/{id}` 与 `/payments/settlements`，依赖登录会话中的 `vendorId`（缺少时需重新登录）。
- 🛂 管理员运营：`AdminVendorReviewPage` 与 `AdminProductReviewPage` 审核入驻/商品；`AdminOrderMonitorPage` 具备过滤、抽屉详情、电子合同预览与 `/admin/orders/{id}/force-close` 按钮。
- 📊 仪表盘：`OverviewPage` 汇总平台与厂商 GMV、订单状态分布、7 日趋势、租赁模式构成、最新通知与常用入口。
- ⚙️ 工程化：Ant Design Vue 组件库、`services/*.ts` API 封装、`stores/auth` Token 恢复、Playwright `tests/dashboard.spec.ts`、以及 `flexlease.payment.auto-confirm` 驱动的自动支付模拟。

## 快速开始
```powershell
cd frontend
npm install
npm run dev
# 端到端测试
npm run test:e2e
```

> 需要 Node.js 20+ 与 npm 10+（与 CI 保持一致）。

开发环境默认通过 Vite 代理分别联调 9001～9006 服务，可在 `vite.config.ts` 调整；若希望统一走网关，可设置 `VITE_API_PROXY=http://localhost:8080`，构建时使用 `VITE_API_BASE_URL`（默认 `/api/v1`）指定 API 前缀。

## 生产构建
```powershell
npm run build
```

## 测试
```powershell
npm run test:e2e        # 启动本地预览并运行 Playwright 端到端测试
```

`tests/dashboard.spec.ts` 会模拟 `/auth/me`、`/analytics/dashboard` 与 `/notifications/logs`，校验 Overview 关键指标的渲染。

产物位于 `frontend/dist`，可交由网关或静态资源服务器托管。

## 后续迭代建议
- 引入 ESLint + Prettier、lint-staged/Commitlint 等工程规范，统一格式与提交校验。
- 增补组件级单测（Vitest/Testing Library）并扩充 Playwright 用例，覆盖购物车结算、厂商履约与管理员强制关闭等关键流。
- 通知中心 & 订单详情可加入“标记已读”“关键字搜索”“支付/退款流水”展示，并提供可配置的支付失败/延迟模拟，便于演练异常场景。
