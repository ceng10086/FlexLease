# FlexLease 前端

迭代 1 已交付基于 **Vite + Vue 3 + TypeScript** 的管理端单页应用骨架，内置登录页与仪表盘占位，配套 Pinia 状态管理、Vue Router 以及 Ant Design Vue 组件库。

## 功能概览
- 🔐 登录表单：调用 `/api/v1/auth/token` 完成账号认证，持久化 JWT。
- 👤 用户信息：登录后自动读取 `/api/v1/auth/me`，展示用户名、角色等摘要。
- 🧭 路由守卫：未登录用户会被重定向至登录页，支持登录后返回原页面。
- 🎨 UI 框架：集成 Ant Design Vue，提供基础布局、表单、卡片、步骤条等组件。

## 快速开始
```powershell
cd frontend
npm install
npm run dev
```

开发环境默认连接 `http://localhost:9001/api/v1`，可通过 `.env.development` 调整。

## 生产构建
```powershell
npm run build
```

产物位于 `frontend/dist`，可交由网关或静态资源服务器托管。

## 后续迭代建议
- 接入实际的厂商入驻、商品管理等业务页面。
- 集成前端路由权限与菜单配置。
- 加入 UI/UX 设计语言与响应式布局优化。
