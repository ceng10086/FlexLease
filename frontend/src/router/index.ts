import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const AppLayout = () => import('@/components/layout/AppLayout.vue');

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/customer',
    component: AppLayout,
    meta: { requiresAuth: true, role: 'CUSTOMER', title: '客户工作台' },
    children: [
      {
        path: '',
        redirect: '/customer/overview'
      },
      {
        path: 'overview',
        name: 'customer-overview',
        component: () => import('@/views/customer/CustomerOverview.vue'),
        meta: { requiresAuth: true, role: 'CUSTOMER', title: '租赁概览' }
      },
      {
        path: 'catalog',
        name: 'customer-catalog',
        component: () => import('@/views/customer/CustomerCatalog.vue'),
        meta: { requiresAuth: true, role: 'CUSTOMER', title: '商品目录' }
      },
      {
        path: 'orders',
        name: 'customer-orders',
        component: () => import('@/views/customer/CustomerOrders.vue'),
        meta: { requiresAuth: true, role: 'CUSTOMER', title: '租赁订单' }
      },
      {
        path: 'payments',
        name: 'customer-payments',
        component: () => import('@/views/customer/CustomerPayments.vue'),
        meta: { requiresAuth: true, role: 'CUSTOMER', title: '支付与结算' }
      }
    ]
  },
  {
    path: '/vendor',
    component: AppLayout,
    meta: { requiresAuth: true, role: 'VENDOR', title: '厂商工作台' },
    children: [
      {
        path: '',
        redirect: '/vendor/overview'
      },
      {
        path: 'overview',
        name: 'vendor-overview',
        component: () => import('@/views/vendor/VendorDashboard.vue'),
        meta: { requiresAuth: true, role: 'VENDOR', title: '运营总览' }
      },
      {
        path: 'products',
        name: 'vendor-products',
        component: () => import('@/views/vendor/VendorProducts.vue'),
        meta: { requiresAuth: true, role: 'VENDOR', title: '商品管理' }
      },
      {
        path: 'orders',
        name: 'vendor-orders',
        component: () => import('@/views/vendor/VendorOrders.vue'),
        meta: { requiresAuth: true, role: 'VENDOR', title: '订单协同' }
      },
      {
        path: 'analytics',
        name: 'vendor-analytics',
        component: () => import('@/views/vendor/VendorAnalytics.vue'),
        meta: { requiresAuth: true, role: 'VENDOR', title: '运营分析' }
      }
    ]
  },
  {
    path: '/admin',
    component: AppLayout,
    meta: { requiresAuth: true, role: 'ADMIN', title: '平台运营中心' },
    children: [
      {
        path: '',
        redirect: '/admin/overview'
      },
      {
        path: 'overview',
        name: 'admin-overview',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { requiresAuth: true, role: 'ADMIN', title: '平台概览' }
      },
      {
        path: 'products',
        name: 'admin-products',
        component: () => import('@/views/admin/AdminProducts.vue'),
        meta: { requiresAuth: true, role: 'ADMIN', title: '商品审核' }
      },
      {
        path: 'settlements',
        name: 'admin-settlements',
        component: () => import('@/views/admin/AdminSettlements.vue'),
        meta: { requiresAuth: true, role: 'ADMIN', title: '资金结算' }
      },
      {
        path: 'vendors',
        name: 'admin-vendors',
        component: () => import('@/views/admin/AdminVendorApplications.vue'),
        meta: { requiresAuth: true, role: 'ADMIN', title: '厂商入驻' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore();
  if (!auth.initialized) {
    await auth.bootstrap();
  }

  if (to.meta?.title) {
    document.title = `FlexLease | ${to.meta.title}`;
  } else {
    document.title = 'FlexLease 智能共享租赁平台';
  }

  if (to.meta?.public) {
    if (to.path === '/login' && auth.isAuthenticated) {
      return next(auth.getDefaultRoute());
    }
    return next();
  }

  if (to.meta?.requiresAuth && !auth.isAuthenticated) {
    return next({
      path: '/login',
      query: { redirect: to.fullPath }
    });
  }

  if (to.meta?.role && !auth.hasRole(to.meta.role)) {
    return next(auth.isAuthenticated ? auth.getDefaultRoute() : '/login');
  }

  return next();
});

export default router;

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    public?: boolean;
    requiresAuth?: boolean;
    role?: 'CUSTOMER' | 'VENDOR' | 'ADMIN';
  }
}
