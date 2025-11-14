import { test, expect } from '@playwright/test';
import type { Route } from '@playwright/test';

const mockUserResponse = {
  code: 0,
  message: 'ok',
  data: {
    id: '00000000-0000-0000-0000-000000000001',
    username: 'admin@example.com',
    roles: ['ADMIN'],
    lastLoginAt: null
  }
};

const mockDashboardResponse = {
  code: 0,
  message: 'ok',
  data: {
    totalOrders: 42,
    activeOrders: 18,
    totalGmv: 123456.78,
    inLeaseCount: 12,
    pendingReturns: 3,
    ordersByStatus: {
      AWAITING_SHIPMENT: 5,
      IN_LEASE: 12,
      RETURN_REQUESTED: 3
    },
    recentTrend: [
      { date: '2025-01-01', orders: 3, gmv: 1200 },
      { date: '2025-01-02', orders: 5, gmv: 2600 }
    ],
    planBreakdown: [
      { planType: 'STANDARD', orders: 30, gmv: 42000 },
      { planType: 'RENT_TO_OWN', orders: 12, gmv: 36000 }
    ],
    planBreakdown: [
      { planType: 'STANDARD', orders: 30, gmv: 42000 },
      { planType: 'RENT_TO_OWN', orders: 12, gmv: 36000 }
    ]
  }
};

const mockNotificationsResponse = {
  code: 0,
  message: 'ok',
  data: [
    {
      id: '1',
      subject: '系统维护通知',
      createdAt: '2025-01-01T10:00:00Z'
    }
  ]
};

test.describe('Dashboard analytics', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('flexlease_token', 'internal-test-token');
    });

    await page.route('**/api/v1/auth/me', async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockUserResponse)
      });
    });

    await page.route('**/api/v1/analytics/dashboard', async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockDashboardResponse)
      });
    });

    await page.route('**/api/v1/notifications/logs', async (route: Route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockNotificationsResponse)
      });
    });
  });

  test('renders dashboard metrics from analytics API', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByText('总订单')).toBeVisible();
    await expect(page.getByText(/^42$/).first()).toBeVisible();
    await expect(page.getByText('GMV (¥)')).toBeVisible();
    await expect(page.getByText('123,456.78')).toBeVisible();
    await expect(page.getByText('订单状态分布：')).toBeVisible();
    await expect(page.getByText('IN_LEASE：12')).toBeVisible();
    await expect(page.getByText('7 日 GMV 与订单趋势')).toBeVisible();
    await expect(page.getByText('租赁模式构成')).toBeVisible();
    await expect(page.getByText('标准方案')).toBeVisible();
  });
});
