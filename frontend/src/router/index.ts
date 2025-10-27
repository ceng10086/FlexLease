import { createRouter, createWebHistory } from 'vue-router';
import type { RouteLocationNormalized } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      redirect: '/app/overview'
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
          path: 'overview',
          name: 'overview',
          component: () => import('../pages/overview/OverviewPage.vue'),
          meta: { navKey: 'overview' }
        },
        {
          path: 'catalog',
          name: 'catalog',
          component: () => import('../pages/customer/CatalogPage.vue'),
          meta: { navKey: 'catalog', roles: ['USER'] }
        },
        {
          path: 'catalog/:productId',
          name: 'catalog-product',
          component: () => import('../pages/customer/ProductDetailPage.vue'),
          meta: { navKey: 'catalog', roles: ['USER'] }
        },
        {
          path: 'checkout',
          name: 'checkout',
          component: () => import('../pages/customer/CheckoutPage.vue'),
          meta: { navKey: 'catalog', roles: ['USER'] }
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('../pages/customer/OrdersPage.vue'),
          meta: { navKey: 'orders', roles: ['USER'] }
        },
        {
          path: 'orders/:orderId',
          name: 'order-detail',
          component: () => import('../pages/customer/OrderDetailPage.vue'),
          meta: { navKey: 'orders', roles: ['USER'] }
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('../pages/customer/NotificationCenterPage.vue'),
          meta: { navKey: 'notifications' }
        },
        {
          path: 'admin/vendor-review',
          name: 'admin-vendor-review',
          component: () => import('../pages/admin/AdminVendorReviewPage.vue'),
          meta: { roles: ['ADMIN'], navKey: 'admin-vendor-review' }
        },
        {
          path: 'admin/product-review',
          name: 'admin-product-review',
          component: () => import('../pages/admin/AdminProductReviewPage.vue'),
          meta: { roles: ['ADMIN'], navKey: 'admin-product-review' }
        },
        {
          path: 'admin/orders',
          name: 'admin-orders',
          component: () => import('../pages/admin/AdminOrderMonitorPage.vue'),
          meta: { roles: ['ADMIN'], navKey: 'admin-orders' }
        },
        {
          path: 'admin/operations',
          name: 'admin-operations',
          component: () => import('../pages/admin/AdminOperationsCenterPage.vue'),
          meta: { roles: ['ADMIN'], navKey: 'admin-operations' }
        },
        {
          path: 'vendor/onboarding',
          name: 'vendor-onboarding',
          component: () => import('../pages/vendor/VendorOnboardingPage.vue'),
          meta: { roles: ['VENDOR'], navKey: 'vendor-onboarding' }
        },
        {
          path: 'vendor/products',
          name: 'vendor-products',
          component: () => import('../pages/vendor/VendorProductWorkspacePage.vue'),
          meta: { roles: ['VENDOR'], navKey: 'vendor-products' }
        },
        {
          path: 'vendor/orders',
          name: 'vendor-orders',
          component: () => import('../pages/vendor/VendorOrderWorkspacePage.vue'),
          meta: { roles: ['VENDOR'], navKey: 'vendor-orders' }
        },
        {
          path: 'vendor/analytics',
          name: 'vendor-analytics',
          component: () => import('../pages/vendor/VendorAnalyticsPage.vue'),
          meta: { roles: ['VENDOR'], navKey: 'vendor-analytics' }
        },
        {
          path: 'vendor/settlements',
          name: 'vendor-settlements',
          component: () => import('../pages/vendor/VendorSettlementPage.vue'),
          meta: { roles: ['VENDOR'], navKey: 'vendor-settlements' }
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

  if (auth.initializing) {
    await auth.bootstrap();
  }

  if (to.meta.public) {
    if (auth.isAuthenticated && (to.name === 'login' || to.name === 'register')) {
      return { path: '/app/overview' };
    }
    return true;
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  const requiredRoles = to.meta.roles as string[] | undefined;
  if (requiredRoles && requiredRoles.length > 0) {
    const allowed = requiredRoles.some((role) => auth.hasRole(role));
    if (!allowed) {
      return { path: '/app/overview' };
    }
  }

  return true;
});

export default router;
