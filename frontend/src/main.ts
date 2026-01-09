/**
 * 应用入口：
 * - 注册 Pinia、路由与 Ant Design Vue
 * - 引入全局样式（tokens/global）
 */
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import './styles/tokens.css';
import './styles/global.css';

import App from './App.vue';
import router from './router';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(Antd);

app.mount('#app');
