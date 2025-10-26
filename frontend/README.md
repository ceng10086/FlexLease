# FlexLease 前端

基于 **Vite + Vue 3 + TypeScript** 的管理端单页应用，已完成登录、仪表盘，以及迭代 2 新增的厂商商品管理与管理员审核界面。配套 Pinia 状态管理、Vue Router 与 Ant Design Vue 组件库。

## 功能概览
- 🔐 登录表单：调用 `/api/v1/auth/token` 完成账号认证，并在 Pinia 中持久化 JWT。
- 👤 用户信息：登录后自动读取 `/api/v1/auth/me`，展示用户名、角色等摘要。
- 🧭 路由守卫：支持未登录重定向、角色控制（管理员可访问审核页）。
- 🛠️ 厂商工作台：新增商品、配置租赁方案与 SKU、调整库存、提交审核。
- 🧾 入驻申请：厂商可在线提交/查看入驻申请进度，复用 `/api/v1/vendors/applications` 接口。
- ✅ 管理员审核：查看待审商品列表，执行通过/驳回操作。
- 🗳️ 厂商审核后台：管理员可筛选厂商入驻申请并完成审批。
- 🔄 运营工具箱：集成订单、支付、通知的常用操作入口，支持快速验证迭代 3-5 功能。
- 📊 运营仪表盘：接入 `/api/v1/analytics/dashboard` 展示 GMV、状态分布等核心指标。
- 🎨 UI 框架：集成 Ant Design Vue，提供布局、表单、表格、抽屉等组件。

## 快速开始
```powershell
cd frontend
npm install
npm run dev
# 端到端测试
npm run test:e2e
```

开发环境默认通过 Vite 代理分别联调 9001/9002/9003 服务，可在 `vite.config.ts` 调整。

## 生产构建
```powershell
npm run build
```

## 测试
```powershell
npm run test:e2e        # 启动本地预览并运行 Playwright 端到端测试
```

产物位于 `frontend/dist`，可交由网关或静态资源服务器托管。

## 后续迭代建议
- 面向 C 端用户的下单体验、订单列表与售后流程仍待补齐。
- 引入 ESLint + Prettier、组件级单测（Vitest / Testing Library）。
- 加入响应式布局、文件上传等交互体验优化。
