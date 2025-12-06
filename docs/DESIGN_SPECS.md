# DESIGN_SPECS

## 视觉风格（淘宝/闲鱼调性）

- **主色**：沿用品牌蓝 `#2563eb`（primary / CTA），并衍生深浅两级（`#1d4ed8`、`#dbeafe`）。辅助色选用闲鱼常见的暖橙 `#ff8a00`（提醒）与薄荷绿 `#10b981`（成功），危险色 `#f43f5e`。
- **背景**：页面背景使用柔和的米白 `#f7f7f5`，卡片背景 `#ffffff`，投影采用 `0 8px 24px rgba(15,23,42,0.08)`，避免过度渐变。
- **字体**：基础字号 14px，层级如下：`title-xl 24px/34`、`title-lg 20px/30`、`title-md 18px/28`、`body 14px/22`、`caption 12px/18`。Heading 字重 600，其余 400~500；中英文混排字体栈 `Inter, "PingFang SC", "Microsoft YaHei", sans-serif`。
- **间距**：外层 padding `clamp(16px, 4vw, 32px)`，模块间距 16/24px，列表单元 12px；圆角统一 16px（卡片）、12px（按钮）、999px（徽标）。
- **图标**：统一使用线性图标（Ant Design Outline + 自定义 SVG），颜色随语义变化；状态标签采用实心 pill。

## 交互与布局要求

### 气泡聊天（交流区）
- 组件：`OrderChatPanel`（订单详情）、`VendorChatPanel`（厂商工作台）。
- **气泡样式**：用户端右对齐、品牌蓝底 + 白字；厂商/系统左对齐、浅灰底 + 深字；系统事件采用时间线样式（虚线 + 图标）。
- **时间/状态**：每组消息显示发送时间，近期消息在右下角展示 `已读/未读`；长按气泡出现“复制/另存/撤销（若可）”动作。
- **输入区**：底部固定浮层，包含文本框、附件按钮（支持图片/视频/文件）、快捷短语面板。移动端使用可伸缩 TextArea，提交后自动滚动到底部。

### 商品/订单列表（瀑布流 & 大图卡片）
- **商品瀑布流**：两列布局（移动端 2 列、桌面 4 列），图片高度随内容自适应，卡片包含封面、标题、方案标签（押金、租金）、收藏/分享按钮。滚动加载 + 空状态插画。
- **订单卡片**：按照状态分组（待支付、履约中、售后等）。每张卡片包含基本信息、操作按钮（付款、查看物流、上传凭证等），并配置 sticky 操作条。
- **审核/结算列表**：管理员与厂商端使用“分组列表 + 抽屉”组合，列表项展示最关键的 3 个字段，其余放在详情面板；支持左右滑动 reveal 操作（移动端）。

## 组件风格要点

| 组件 | 视觉/交互要点 |
| --- | --- |
| `PlanSelector` | 横向滑块，选中态加粗 + 渐变描边，下方展示押金/租金标签与权益图标。 |
| `StickyActionBar` | 背景半透明磨砂，包含主 CTA + 次按钮 + 价格概要；滚动到页面底部仍固定。 |
| `ProofGallery` | 3 列瀑布，点击放大后可左右滑动，文件上传入口固定在首列。 |
| `FilterBar` | 底部抽屉展示多选项，顶部 Tabs 仅显示活跃条件；移动端点击标签或图标可快速清空。 |
| `ReviewDrawer` | 顶部展示关键摘要（订单号/厂商/状态），主体使用 Tab 切换“资料/操作/记录”，底部固定操作按钮。 |
| `NotificationCard` | 左侧标签表示渠道，右上角显示状态；已读卡片背景改成浅灰，悬浮展示动作按钮。 |

## 字体与颜色规范

| 类型 | Token | 值 |
| --- | --- | --- |
| Primary | `--color-primary` | `#2563eb` |
| Primary Hover | `--color-primary-hover` | `#1d4ed8` |
| Accent / Warning | `--color-accent` | `#ff8a00` |
| Success | `--color-success` | `#10b981` |
| Danger | `--color-danger` | `#f43f5e` |
| Neutral Text | `--color-text` | `#1f2937` |
| Secondary Text | `--color-text-secondary` | `#64748b` |
| Border | `--color-border` | `#e2e8f0` |
| Surface | `--color-surface` | `#ffffff` |
| Surface Muted | `--color-surface-muted` | `#f7f7f5` |

字体层级：

| Token | 字重/字号 | 用途 |
| --- | --- | --- |
| `--font-title-xl` | 600 / 24px | 页面标题 |
| `--font-title-lg` | 600 / 20px | 模块标题 |
| `--font-body` | 400 / 14px | 正文 |
| `--font-caption` | 400 / 12px | 标签、辅助信息 |

## 交互细化

1. **骨架屏 & 空状态**：每个数据组件提供 Skeleton（3 条灰条 + 圆角卡片）、空状态插画 + 文案 + CTA，避免突然空白。
2. **反馈提示**：成功提示使用顶部 Toast（2 秒自动消失），失败则在相关区域内展示 `InlineError`，确保上下文一致。
3. **动画**：瀑布流卡片、聊天气泡进入时使用 120ms 的淡入/位移动画；底部操作条使用 200ms 的 slide-up。
4. **触摸目标**：所有交互控件最小高度 40px，保证触控友好。

## 文件拆分映射

| 旧文件 | 新结构 |
| --- | --- |
| `frontend/src/pages/customer/OrderDetailPage.vue` | → `views/orders/OrderOverviewView.vue`（摘要+操作） + `views/orders/OrderChatView.vue`（聊天） + `views/orders/OrderProofView.vue`（凭证） + `views/orders/OrderTimelineView.vue`（事件/纠纷/调查） + `components/orders/OrderActionBar.vue`（sticky CTA）。 |
| `frontend/src/pages/vendor/VendorOrderWorkspacePage.vue` | → `views/vendor/fulfillment/FulfillmentBoard.vue`（列表） + `views/vendor/fulfillment/FulfillmentDetailSheet.vue`（包含发货/退租/续租/买断 Tab） + 共用 `OrderChatPanel` / `ProofGallery` / `DisputePanel`。 |
| `frontend/src/pages/vendor/VendorProductWorkspacePage.vue` | → `views/vendor/products/ProductListView.vue`（列表 + 筛选） + `views/vendor/products/ProductEditor.vue`（表单 + 媒体） + `views/vendor/products/PlanSkuManager.vue`（方案 & SKU） + `views/vendor/products/InquiryInbox.vue`。 |
| `frontend/src/pages/customer/CatalogPage.vue` & `ProductDetailPage.vue` | → `views/catalog/ProductFeed.vue`（瀑布流） + `views/catalog/ProductDetailShell.vue`（包含 `MediaGallery`、`PlanSelector`、`InquiryPanel`、`RecommendationRail`）。 |
| `frontend/src/pages/admin/AdminOrderMonitorPage.vue` | → `views/admin/orders/AdminOrderList.vue`（卡片/表格切换） + `views/admin/orders/AdminOrderDrawer.vue`（Tab: 概览/凭证/聊天/纠纷）。 |
| `frontend/src/pages/overview/OverviewPage.vue` | → `views/dashboard/DashboardHome.vue`（角色自适应） + 子组件 `MetricGrid`, `AnnouncementRail`, `TrendWidget`, `PlanBreakdownCard`（保留但重绘）。 |

> 新文件命名统一放入 `frontend/src/views/**` 与 `frontend/src/components/**`，原 `pages/**` 目录逐步废弃；通用逻辑下沉到 `src/composables/**`（如 `useOrders`, `useProofs`, `useChat`）。
