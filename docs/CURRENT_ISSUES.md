# CURRENT_ISSUES

> 说明：该文档最初基于旧版 `frontend/src/pages/**` 结构整理；目前前端已重构为 `frontend/src/views/**` 为主（登录/注册仍在 `pages/**`）。下文问题清单仅供历史参考，以当前代码实现为准。

对 `frontend` 目录的全部页面与公共样式逐行排查后，当前界面存在以下主要问题。

## 页面诊断

### 消费者端
- **总览页**（`frontend/src/pages/overview/OverviewPage.vue`）把平台指标、通知、厂商指标、消费者快捷入口堆在同一层级，左右列全部使用 `a-card` + `a-row`，屏幕宽度减小时卡片直接换行，没有优先级指引，`a-statistic`、`a-tag` 也缺少颜色编码，导致管理员与消费者同时登陆时信息噪声极大。
- **商品目录**（`customer/CatalogPage.vue`）依赖传统 `a-row/a-col` 宫格和 `a-card`，缺少类目筛选、瀑布流布局以及图片懒加载；卡片 hover 才有反馈，移动端是完整的 block，体验与“逛闲鱼”式的流量入口南辕北辙。
- **商品详情**（`customer/ProductDetailPage.vue`）把租赁方案渲染成 `a-radio` 列表，缺少视觉层级、SKU 图片和租赁权益说明；“下单前咨询”直接堆在页面底部，没有状态展示，72 小时有效期用户也感知不到。
- **结算页**（`customer/CheckoutPage.vue`）在一个狭长的 `a-card` 里塞入 `a-descriptions`、`a-form`、信用提示等多个模块，支付概要只有静态数字，没有可展开的费用明细或拆分展示，移动端浏览需要不停滚动。
- **购物车与订单列表**（`customer/CartPage.vue`、`customer/OrdersPage.vue`）依旧使用标准表格布局：在手机上每一行都要左右滑动才能看到全部信息，操作按钮尺寸也过小；没有分组、没有骨架屏，也无法预览商品。
- **订单详情**（`customer/OrderDetailPage.vue` 近 1700 行）集成了确认收货、续租、退租、买断、聊天、取证、纠纷、调查、支付等所有操作，形成典型的 God Component。所有功能以 `a-card` 平铺，缺少锚点导航，用户很难在手机上定位到“我要留言”或“上传凭证”入口；上传凭证、聊天、纠纷、满意度调查等 UI 块与厂商端完全重复，却各自维护一套状态。
- **通知中心**（`customer/NotificationCenterPage.vue`）虽然用列表代替表格，但筛选控件挤在顶部一行，缺少按状态/渠道的快速标签或折叠，列表项也没有“已读/未读”状态和跳转动作。

### 厂商端
- **入驻页**（`vendor/VendorOnboardingPage.vue`）的“最新申请”与“提交新申请”左右并列，但没有为移动端拆分；流程提示固定写死，不能根据状态动态展示下一步（例如审核通过后提示“重新登录以刷新厂商身份”）。
- **商品工作台**（`vendor/VendorProductWorkspacePage.vue`）仍以表格 + 抽屉组织，商品详情抽屉内同时包含租赁方案、SKU、库存、媒体、咨询回复等功能，一次性加载大量数据且缺少分段导航；文件上传只暴露原生 `<input>`，没有图片预览或压缩能力。
- **厂商订单工作台**（`vendor/VendorOrderWorkspacePage.vue` 近 4000 行）与消费者订单详情一样臃肿，将发货、退租审批、续租、买断、取证、沟通、纠纷、满意度等模块堆在一个 Drawer 中，且大量逻辑与 `OrderDetailPage` 重复。由于使用 `a-drawer` 固定宽度 840px，在小屏下字体被压缩且滚动困难。
- **厂商指标 / 结算**（`vendor/VendorAnalyticsPage.vue`、`vendor/VendorSettlementPage.vue`）只展示简单数字卡片和传统表格，缺少对抽成变化、产品动销的可视化解释，更没有针对移动端的卡片式渲染。

### 管理员端
- **入驻/商品审核页**（`admin/AdminVendorReviewPage.vue`、`admin/AdminProductReviewPage.vue`）全部依赖 `a-table` 和 `a-drawer`，列表信息密集且没有空状态/批量操作，无法快速定位“待我处理”的任务。
- **订单监控页**（`admin/AdminOrderMonitorPage.vue`）在单页集成筛选表单、订单表格、详情抽屉、纠纷裁决等能力。筛选控件使用 `a-form` inline 布局，在移动端换行后标签与控件错位；详情抽屉仍旧是 `a-descriptions` + `a-table` 的信息流，没有操作时间线、凭证、聊天的可视化呈现。

## 架构痛点
- **God Components**：`OrderDetailPage.vue`、`VendorOrderWorkspacePage.vue`、`AdminOrderMonitorPage.vue` 均超过 1500 行，既负责数据拉取、状态管理，又渲染所有业务区域，代码无法复用，任何变更都牵一发动全身。
- **重复实现**：消费者与厂商的取证上传、聊天、纠纷、满意度等完全复制粘贴，接口调用、错误处理、UI 结构各自维护，导致体验不一致。
- **非响应式硬编码**：大量 Drawer、Modal 直接写死 `width: 720/840px`（例如 `OrderContractDrawer.vue`、`AdminOrderMonitorPage.vue`），小屏幕只能依赖浏览器缩放；`global.css` 里固定 `page-container` 宽度为 `min(1200px, 100%)`，没有考虑横向栅格与内边距的同步调整。
- **状态逻辑散落**：数据获取全部写在页面 `setup` 函数里，没有统一的 `useQuery` 或状态容器。Loading、空状态、错误提示都需要手写，造成体验不一致。
- **视觉缺乏体系**：页面背景使用大面积渐变（`global.css`）但卡片仍为 AntD 默认样式，缺少品牌化元素；同一页面同时出现 `a-tag`、`a-alert`、`a-space`、`a-table`，缺乏统一节奏。

## 不要什么
- 不要再使用“表格 + 抽屉”作为移动端主要交互（尤其是购物车、订单列表、审核列表、结算页），需要卡片化或列表 + 详情页的结构。
- 不要在新的实现中继续堆砌大组件；拆分成视图容器 + 原子组件 + 组合区块，禁止再出现 1000 行以上的 `.vue`。
- 不要继续依赖固定宽度的 Drawer/Modal，改为自适应的全屏面板或底部弹层。
- 不要再把消费者/厂商/管理员共用的模块（聊天、取证、纠纷、满意度等）复制粘贴，必须抽成独立组件和 `use` 级逻辑。
- 不要延续“仅靠 AntD 默认样式”的视觉方案，需要建立 FlexLease 自己的卡片、按钮、留白与字体层级。
