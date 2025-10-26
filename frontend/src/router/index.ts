import { createRouter, createWebHistory } from 'vue-router';
import type { RouteLocationNormalized } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/products',
      name: 'products',
      component: () => import('../views/ProductManagementView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/vendor/onboarding',
      name: 'vendor-onboarding',
      component: () => import('../views/VendorOnboardingView.vue'),
      meta: { requiresAuth: true, roles: ['VENDOR'] }
    },
    {
      path: '/admin/products',
      name: 'admin-products',
      component: () => import('../views/AdminProductView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/admin/vendors',
      name: 'admin-vendors',
      component: () => import('../views/AdminVendorApplicationsView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/admin/operations',
      name: 'operations-console',
      component: () => import('../views/OperationsConsoleView.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
});

router.beforeEach(async (to: RouteLocationNormalized) => {
  const auth = useAuthStore();

  if (auth.initializing) {
    await auth.bootstrap();
  }

  if (to.meta.public) {
    if (auth.isAuthenticated) {
      return { path: '/' };
    }
    return true;
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  const requiredRoles = to.meta.roles as string[] | undefined;
  if (requiredRoles && requiredRoles.length > 0) {
    const userRoles = auth.user?.roles ?? [];
    const hasRole = userRoles.some((role) => requiredRoles.includes(role));
    if (!hasRole) {
      return { path: '/' };
    }
  }

  return true;
});

export default router;
