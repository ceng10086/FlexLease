# FlexLease 前端

该前端基于 [Vite](https://vitejs.dev/) + [Vue 3](https://vuejs.org/) + [Ant Design Vue](https://www.antdv.com/) 重写，实现了平台管理员、B 端厂商、C 端用户三类角色的完整工作台体验，并按照《项目说明.md》所述业务范围提供了模拟数据与后端对接能力。

## 功能概览

- **统一登录入口**：支持真实接口登录与三种角色的模拟体验模式。
- **角色工作台**：
  - *C 端用户*：租赁概览、商品目录、订单管理、支付账单。
  - *B 端厂商*：运营指标、商品管理、订单协同、运营分析。
  - *平台管理员*：平台指标、商品审核、资金结算、厂商入驻。
- **数据降级机制**：所有 API 模块在接口不可达时使用 `src/utils/sampleData.ts` 中的示例数据，保障演示连贯性。
- **可扩展的 API 封装**：`src/api` 下按业务领域拆分服务调用，并在 `vite.config.ts` 中提供本地代理配置。

## 目录结构

```
frontend/
├── src
│   ├── api/               # 后端接口封装与降级策略
│   ├── components/        # 通用组件与布局组件
│   ├── stores/            # Pinia 状态管理（认证等）
│   ├── utils/             # 工具函数与示例数据
│   ├── views/             # 各角色页面
│   ├── router/            # 路由与守卫
│   ├── styles/            # 全局样式
│   └── App.vue / main.ts  # 应用入口
```

## 本地开发

```bash
cd frontend
npm install
npm run dev
```

- 默认端口 `5173`。
- 若需转发到后端微服务，可在 `.env` 或运行参数中设置 `VITE_API_PROXY`。
- 当只有部分服务启动时，前端会自动切换到示例数据并在控制台给出提示。

## 构建与预览

```bash
npm run build
npm run preview
```

构建输出位于 `frontend/dist` 目录，可用于静态部署。

## 角色模拟说明

在登录页选择“快速体验”即可进入各角色工作台。模拟模式会生成虚拟账号并缓存到浏览器 Storage，随时可以在右上角“退出”返回登录页或切换角色。
