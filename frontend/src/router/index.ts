/**
 * 路由入口：
 * - 以 `/app` 为登录后主布局（AuthenticatedLayout）
 * - 通过 `meta.public/meta.requiresAuth/meta.roles` 做最小权限控制
 */
import { createRouter, createWebHistory } from 'vue-router';
import type { RouteLocationNormalized } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      redirect: '/app/dashboard'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../pages/auth/LoginPage.vue'),
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../pages/auth/RegisterPage.vue'),
      meta: { public: true }
    },
    {
      path: '/app',
      component: () => import('../layouts/AuthenticatedLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/dashboard/DashboardHome.vue'),
          meta: { navKey: 'dashboard-home' }
        },
        {
          path: 'catalog',
          name: 'catalog-feed',
          component: () => import('../views/catalog/ProductFeedView.vue'),
          meta: { navKey: 'catalog-feed', roles: ['USER'] }
        },
        {
          path: 'catalog/:productId',
          name: 'catalog-product',
          component: () => import('../views/catalog/ProductDetailShell.vue'),
          meta: { navKey: 'catalog-feed', roles: ['USER'] }
        },
        {
          path: 'checkout',
          name: 'checkout',
          component: () => import('../views/checkout/CheckoutShell.vue'),
          meta: { navKey: 'checkout-flow', roles: ['USER'] }
        },
        {
          path: 'cart',
          name: 'cart',
          component: () => import('../views/checkout/CartBoardView.vue'),
          meta: { navKey: 'checkout-flow', roles: ['USER'] }
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('../views/orders/OrderHubView.vue'),
          meta: { navKey: 'order-hub', roles: ['USER'] }
        },
        {
          path: 'orders/chat-center',
          name: 'order-chat-center',
          component: () => import('../views/orders/OrderConversationHubView.vue'),
          meta: { navKey: 'order-chat-center', roles: ['USER'] }
        },
        {
          path: 'orders/:orderId/contract',
          name: 'order-contract',
          component: () => import('../views/orders/detail/OrderContractSignView.vue'),
          meta: { navKey: 'order-hub', roles: ['USER'] }
        },
        {
          path: 'orders/:orderId/payment',
          name: 'order-payment',
          component: () => import('../views/orders/detail/OrderPaymentView.vue'),
          meta: { navKey: 'order-hub', roles: ['USER'] }
        },
        {
          path: 'orders/:orderId',
          component: () => import('../views/orders/detail/OrderDetailShell.vue'),
          meta: { navKey: 'order-hub', roles: ['USER'] },
          children: [
            {
              path: '',
              redirect: { name: 'order-overview' }
            },
            {
              path: 'overview',
              name: 'order-overview',
              component: () => import('../views/orders/detail/OrderOverviewView.vue'),
              meta: { navKey: 'order-hub', roles: ['USER'] }
            },
            {
              path: 'chat',
              name: 'order-chat',
              component: () => import('../views/orders/detail/OrderChatView.vue'),
              meta: { navKey: 'order-hub', roles: ['USER'] }
            },
            {
              path: 'proofs',
              name: 'order-proofs',
              component: () => import('../views/orders/detail/OrderProofView.vue'),
              meta: { navKey: 'order-hub', roles: ['USER'] }
            },
            {
              path: 'timeline',
              name: 'order-timeline',
              component: () => import('../views/orders/detail/OrderTimelineView.vue'),
              meta: { navKey: 'order-hub', roles: ['USER'] }
            }
          ]
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/profile/ProfileOverviewView.vue'),
          meta: { navKey: 'profile', roles: ['USER', 'ADMIN'] }
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('../views/notifications/NotificationCenterView.vue'),
          meta: { navKey: 'notifications-center' }
        },
        {
          path: 'admin/review',
          name: 'admin-review',
          component: () => import('../views/admin/AdminReviewSuiteView.vue'),
          meta: { roles: ['ADMIN'], navKey: 'admin-review' }
        },
        {
          path: 'admin/orders',
          name: 'admin-orders',
          component: () => import('../views/admin/AdminOrderMonitorView.vue'),
          meta: { roles: ['ADMIN'], navKey: 'admin-orders' }
        },
        {
          path: 'arbitration/orders',
          name: 'arbitration-orders',
          component: () => import('../views/arbitration/ArbitrationOrderMonitorView.vue'),
          meta: { roles: ['ARBITRATOR', 'REVIEW_PANEL'], navKey: 'arbitration-orders' }
        },
        {
          path: 'vendor/onboarding',
          name: 'vendor-onboarding',
          component: () => import('../views/vendor/onboarding/VendorOnboardingView.vue'),
          meta: { roles: ['VENDOR'], navKey: 'vendor-onboarding' }
        },
        {
          path: 'vendor/chat-center',
          name: 'vendor-chat-center',
          component: () => import('../views/vendor/VendorConversationHubView.vue'),
          meta: { roles: ['VENDOR'], navKey: 'vendor-chat-center' }
        },
        {
          path: 'vendor/workbench',
          component: () => import('../views/vendor/workbench/VendorWorkbenchLayout.vue'),
          meta: { roles: ['VENDOR'] },
          children: [
            {
              path: '',
              redirect: { name: 'vendor-workbench-products' }
            },
            {
              path: 'products',
              name: 'vendor-workbench-products',
              component: () => import('../views/vendor/workbench/ProductBoardView.vue'),
              meta: { roles: ['VENDOR'], navKey: 'vendor-products' }
            },
            {
              path: 'fulfillment',
              name: 'vendor-workbench-fulfillment',
              component: () => import('../views/vendor/workbench/FulfillmentBoardView.vue'),
              meta: { roles: ['VENDOR'], navKey: 'vendor-fulfillment' }
            },
            {
              path: 'insights',
              name: 'vendor-workbench-insights',
              component: () => import('../views/vendor/workbench/InsightsBoardView.vue'),
              meta: { roles: ['VENDOR'], navKey: 'vendor-insights' }
            },
            {
              path: 'settlement',
              name: 'vendor-workbench-settlement',
              component: () => import('../views/vendor/workbench/SettlementBoardView.vue'),
              meta: { roles: ['VENDOR'], navKey: 'vendor-settlement' }
            }
          ]
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../pages/NotFoundPage.vue'),
      meta: { public: true }
    }
  ]
});

router.beforeEach(async (to: RouteLocationNormalized) => {
  const auth = useAuthStore();

  // 首次进入应用时拉取 /auth/me，确定当前登录态与角色信息
  if (auth.initializing) {
    await auth.bootstrap();
  }

  if (to.meta.public) {
    if (auth.isAuthenticated && (to.name === 'login' || to.name === 'register')) {
      return { path: '/app/dashboard' };
    }
    return true;
  }

  const requiresAuth = to.matched.some((record) => record.meta?.requiresAuth);
  if (requiresAuth && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  // 按页面声明的 roles 做简单 RBAC；无权限则回到驾驶舱
  const requiredRoles = to.meta.roles as string[] | undefined;
  if (requiredRoles && requiredRoles.length > 0) {
    const allowed = requiredRoles.some((role) => auth.hasRole(role));
    if (!allowed) {
      return { path: '/app/dashboard' };
    }
  }

  return true;
});

export default router;
