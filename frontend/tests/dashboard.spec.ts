import { test, expect } from '@playwright/test';
import type { Route } from '@playwright/test';

const mockUserResponse = {
  success: true,
  message: 'ok',
  data: {
    id: '00000000-0000-0000-0000-000000000001',
    username: 'admin@example.com',
    roles: ['ADMIN'],
    lastLoginAt: null
  }
};

const mockDashboardResponse = {
  success: true,
  message: 'ok',
  data: [
    { title: '在租商品', value: 128, trend: 8.6, trendLabel: '较上周' },
    { title: '月度GMV', value: '¥ 1,280,000', trend: 5.1, trendLabel: '环比' },
    { title: '履约完成率', value: '96.4%', trend: 2.3, trendLabel: '同比' }
  ]
};

test.describe('Dashboard analytics', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem('flexlease/token', 'internal-test-token');
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
  });

  test('renders dashboard metrics from analytics API', async ({ page }) => {
    await page.goto('/admin/overview');

    await expect(page.getByText('在租商品')).toBeVisible();
    await expect(page.getByText('128')).toBeVisible();
    await expect(page.getByText('月度GMV')).toBeVisible();
    await expect(page.getByText('¥ 1,280,000')).toBeVisible();
    await expect(page.getByText('履约完成率')).toBeVisible();
    await expect(page.getByText('+8.6% 较上周')).toBeVisible();
  });
});
