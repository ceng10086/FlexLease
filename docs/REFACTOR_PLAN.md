# REFACTOR_PLAN

## 路由重组

| 新路由 | 说明 | 关键拆分 |
| --- | --- | --- |
| `/app/dashboard` | 仅聚焦“当前角色的关键指标”，管理员再通过切换查看平台/厂商数据。 | `DashboardHome`（指标区）+ `AnnouncementRail`（通知）+ `QuickEntryGrid` |
| `/app/catalog` | 取代旧目录页，顶部为类目筛选，主体为瀑布流。 | `CatalogFilters` + `ProductWaterfall` + `LoadMore` |
| `/app/catalog/:id` | 商品详情重构为“信息 / 套餐 / 评价 / 咨询”多段落。 | `MediaGallery` + `PlanCarousel` + `InquiryPanel` + `StickyActionBar` |
| `/app/orders` | 列表页采用卡片式时间线，新增“状态分段 Tab”。 | `OrderTabNav` + `OrderCardList` |
| `/app/orders/:id` | 拆成子页：`overview`（摘要 + 操作）、`chat`（气泡聊天）、`proofs`（凭证与上传）、`timeline`（事件/纠纷/调查）。 | `OrderOverviewPage` + `OrderChatPage` + `OrderProofPage` + `OrderTimelinePage` |
| `/app/vendor/workbench` | 厂商端统一入口，子 Tab 分别为 `products`、`fulfillment`、`insights`、`settlement`。 | `VendorWorkbenchLayout` 控制 Tab，与现有单页解耦。 |
| `/app/admin/review` | 管理员审核集合页，左右分栏显示厂商与商品，减少路由跳转。 | `ReviewSplitView` + `ReviewDrawer` |

> 现有 `router/menu.ts` 将改成“角色 → 模块 → 子视图”的树形配置，移动端新增底部导航（消费者看 Catalog/Orders/Chat、厂商看 Workbench/Orders/Chat）。

## 布局规范（Mobile-First）

1. **断点**：`<640px` 视为 mobile，`640–1024px` tablet，`>1024px` desktop。默认按 mobile 布局开发，借助 CSS Grid/Clamp 在较大屏幕扩展。
2. **栅格**：移动端使用 4 列（间距 12px），桌面使用 12 列（间距 24px）。内容容器最大宽度 1200px，但左右余量改用 `max(16px, 4vw)`.
3. **导航**：桌面保留侧边栏，移动端使用 Bottom Tab + 顶部 `PageHeader`（含回退、搜索、操作）；Drawer 改成全屏覆盖或 Bottom Sheet。
4. **留白**：基础间距 8px，卡片内外分别使用 16px / 20px，标题与正文行距 1.4~1.6，避免文字挤在一起。
5. **反馈**：统一 Loading Skeleton、空状态、错误提示抽象成组件，避免各页自定义 `a-empty`。

## 组件原子化

| 新组件 | 描述 |
| --- | --- |
| `PageShell` / `PageHeader` / `PageSection` | 统一页面骨架与留白策略，支持标题、副标题、操作区插槽。 |
| `StatWidget`, `TrendWidget`, `PlanBadge` | 指标组件化，适用于平台/厂商/消费者端。 |
| `ProductWaterfall` / `ProductCardLarge` | 支持瀑布流、图片懒加载、加购物车/收藏入口；调用 `/catalog/products`. |
| `PlanSelector` | 以横向滚动卡片展示租赁方案，包含押金/租金/权益标签。 |
| `StickyActionBar` | 底部浮层，容纳“加入购物车”“立即租赁”等主要 CTA。 |
| `OrderCard` / `OrderProgressPill` | 列表项组件，内置状态色、时间线片段。 |
| `OrderChatPanel` | 微信/闲鱼式对话 UI，支持图文消息、系统气泡、已读状态；挂接 `/orders/{id}/messages`. |
| `ProofGallery` + `ProofUploader` | 凭证网格 + 上传组件（多文件、进度条、指引 toast），复用 `/orders/{id}/proofs`. |
| `Timeline` + `TimelineItem` | 封装事件列表、纠纷、调查记录，支持筛选/展开。 |
| `ReviewDrawer` | 通用抽屉（在桌面）/全屏面板（移动），内置 Tab 与操作按钮，用于审核/订单详情。 |
| `FilterBar` / `BottomSheetFilter` | 统一的筛选交互，适配列表、通知、结算等场景。 |

## API 映射

| 组件 / 页面 | 数据来源 |
| --- | --- |
| `DashboardHome` | `/analytics/dashboard`，管理员可切换 `/analytics/vendor/{vendorId}` |
| `AnnouncementRail` | `/notifications/logs?contextType=ANNOUNCEMENT` |
| `ProductWaterfall` | `/catalog/products`（分页 + 瀑布流） |
| `PlanSelector` | `/catalog/products/{id}` |
| `CartMiniSummary` | `/cart` |
| `CheckoutSummary` | `/orders/preview` + `/orders` |
| `OrderCardList` | `/orders?userId=...`（或 vendor/admin 版本） |
| `OrderChatPanel` | `/orders/{id}/messages` |
| `ProofGallery` | `/orders/{id}/proofs` + `/proof-policy` |
| `DisputePanel` | `/orders/{id}/disputes` + `/orders/{id}/disputes/**` |
| `SurveyList` | `/orders/{id}/surveys` |
| `VendorProductBoard` | `/vendors/{vendorId}/products/**` |
| `SettlementTable` | `/payments/settlements` |
| `AdminReviewSplitView` | `/vendors/applications` + `/admin/products` |

> 数据交互层统一通过 `useQuery`/`useMutation`（基于 axios hooks 或轻量状态库）封装，页面只拿到 `data/loading/error`。

## 重构路线图

1. **基础设施（Week 1）**：引入 Design Token、响应式布局（CSS Variables + Tailwind/UnoCSS 或自定义 SCSS）、Skeleton/Empty/Error 三件套、新的 `PageShell`。搭建 `useQuery` 工具和消息提示规范。
2. **消费者体验（Weeks 2-3）**：重做 Catalog/Detail/Checkout/Orders/OrderDetail/Notification。重点交付瀑布流、sticky CTA、分步订单详情（含气泡聊天、凭证墙）。老页面标记 deprecated。
3. **厂商工作台（Weeks 4-5）**：实现 `VendorWorkbenchLayout`，拆分产品/履约/指标/结算子模块；共用聊天、凭证、纠纷组件，补充抽成洞察卡片。
4. **管理员套件（Week 6）**：整合入驻/商品审核、订单监控、纠纷裁决到统一的 `ReviewSplitView`，并将新的 Timeline/Proof/Chat 组件植入详情抽屉。
5. **收尾与硬化（Week 7）**：统一空状态、主题色、文案；补齐关键流回归脚本（商品瀑布、订单聊天、厂商履约）；清理旧页面与未使用样式，更新 README/设计规范。
