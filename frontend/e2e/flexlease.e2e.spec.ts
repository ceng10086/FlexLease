/// <reference types="node" />
import { test, expect, chromium, type Browser, type Page, type Locator } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';

const adminCreds = {
  username: process.env.E2E_ADMIN_USERNAME ?? 'admin@flexlease.test',
  password: process.env.E2E_ADMIN_PASSWORD ?? 'Admin@123'
};

const passwords = {
  user: process.env.E2E_USER_PASSWORD ?? 'Test@123456',
  vendor: process.env.E2E_VENDOR_PASSWORD ?? 'Vendor@123456'
};

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const assetsDir = path.resolve(__dirname, '../../test/assets');
const asset = (name: string) => path.join(assetsDir, name);

const assetPayload = (name: string, mimeType: string) => ({
  name,
  mimeType,
  buffer: fs.readFileSync(asset(name))
});

const rawBaseURL = process.env.E2E_BASE_URL ?? 'http://localhost:8080';

const defaultSlowMoMs = Number(process.env.E2E_SLOW_MO_MS ?? 200);

type DemoWindowSlot = 0 | 1 | 2;

const demoWindowLayout = {
  // 适配演示环境：3200*2000 且 Windows 200% 缩放下，Chromium 的可用坐标通常约等于 1600*1000。
  // 默认值按“看起来更舒服”设置得偏大；若遮挡可自行调小。
  width: Number(process.env.E2E_DEMO_WIN_W ?? 1000),
  height: Number(process.env.E2E_DEMO_WIN_H ?? 1800),
  gap: Number(process.env.E2E_DEMO_WIN_GAP ?? 16),
  top: Number(process.env.E2E_DEMO_WIN_TOP ?? 0)
};

// 默认用“桌面端布局”，但通过 deviceScaleFactor 把 CSS 像素缩小到能塞进三列小窗口。
// 这样避免移动端侧边栏/抽屉动画导致的“滑来滑去卡住”。
const demoRenderMode = (process.env.E2E_DEMO_RENDER_MODE ?? 'desktop').toLowerCase();

// Windows 200% 缩放会让 Chromium 的 UI/页面整体看起来放大。
// 可通过 --force-device-scale-factor 抵消（默认 1）。
const demoChromeScaleFactor = Number(process.env.E2E_DEMO_CHROME_SCALE_FACTOR ?? 1);
// Windows 200% 缩放下通常为 2。用于把“逻辑像素”(screen.* / CSS px) 转为窗口管理的像素单位。
const demoWinDpiScaleOverride = Number(process.env.E2E_DEMO_WIN_DPI_SCALE ?? 0);

type WindowBounds = {
  left: number;
  top: number;
  width: number;
  height: number;
};

const setWindowBounds = async (page: Page, bounds: WindowBounds) => {
  // 通过 CDP 强制设置窗口位置/大小，比启动参数更可靠。
  try {
    const context = page.context();
    const client = await context.newCDPSession(page);
    const { windowId } = await client.send('Browser.getWindowForTarget');
    await client.send('Browser.setWindowBounds', {
      windowId,
      bounds: {
        windowState: 'normal',
        left: Math.max(0, Math.floor(bounds.left)),
        top: Math.max(0, Math.floor(bounds.top)),
        width: Math.max(320, Math.floor(bounds.width)),
        height: Math.max(480, Math.floor(bounds.height))
      }
    });
  } catch {
    // 忽略异常
  }
};

const getWindowBounds = async (page: Page) => {
  try {
    const context = page.context();
    const client = await context.newCDPSession(page);
    const { windowId, bounds } = await client.send('Browser.getWindowForTarget');
    return { windowId, bounds };
  } catch {
    return null;
  }
};

const detectWindowManagerScale = async (page: Page) => {
  // 在 Windows 缩放 (如 200%) 下：
  // - JS 的 window.outerWidth/outerHeight 往往是“逻辑像素”(DIP)
  // - CDP Browser.setWindowBounds 往往按“窗口管理像素单位”
  // 通过设置一个探针尺寸并对比 outerWidth 来估算比例。
  const override = demoWinDpiScaleOverride;
  if (override > 0) return override;

  const probe = { width: 800, height: 600 };
  await setWindowBounds(page, { left: 0, top: 0, width: probe.width, height: probe.height });
  await page.waitForTimeout(100);

  let outer = { width: 0, height: 0 };
  try {
    outer = (await page.evaluate(() => {
      return { width: window.outerWidth ?? 0, height: window.outerHeight ?? 0 };
    })) as { width: number; height: number };
  } catch {
    // 忽略异常
  }

  const safeOuterW = Math.max(1, outer.width);
  const safeOuterH = Math.max(1, outer.height);
  const scaleW = probe.width / safeOuterW;
  const scaleH = probe.height / safeOuterH;
  const raw = (scaleW + scaleH) / 2;

  const clamped = Math.max(1, Math.min(3, raw));
  // 取整到 0.25，避免浮点抖动。
  return Math.round(clamped * 4) / 4;
};

const positionDemoWindows = async (adminPage: Page, vendorPage: Page, userPage: Page) => {
  const { width: desiredWidth, height: desiredHeight, gap, top } = demoWindowLayout;
  const margin = Number(process.env.E2E_DEMO_WIN_MARGIN ?? 24);

  // 注意：我们在 demo/desktop 模式下会用 deviceScaleFactor 做页面缩放，
  // 这会让 window.screen.availWidth/availHeight 变成“模拟设备屏幕”，不能用于真实窗口布局。
  // 因此：窗口摆放完全基于 CDP 读回的真实 bounds，逐个窗口计算 nextLeft，保证不重叠。

  const dpiScale = await detectWindowManagerScale(adminPage);
  const desiredWidthPx = Math.floor(desiredWidth * dpiScale);
  const desiredHeightPx = Math.floor(desiredHeight * dpiScale);
  const gapPx = Math.floor(gap * dpiScale);
  const topPx = Math.floor(top * dpiScale);
  const marginPx = Math.floor(margin * dpiScale);

  // 先顺序摆放（左起），每次 set 之后立刻读回实际宽度（Chromium 有最小宽度约束）。
  await setWindowBounds(adminPage, { left: marginPx, top: topPx, width: desiredWidthPx, height: desiredHeightPx });
  await adminPage.waitForTimeout(60);
  const b1 = (await getWindowBounds(adminPage))?.bounds;
  const w1 = b1?.width ?? desiredWidthPx;
  const l1 = b1?.left ?? marginPx;

  await setWindowBounds(vendorPage, {
    left: l1 + w1 + gapPx,
    top: topPx,
    width: desiredWidthPx,
    height: desiredHeightPx
  });
  await vendorPage.waitForTimeout(60);
  const b2 = (await getWindowBounds(vendorPage))?.bounds;
  const w2 = b2?.width ?? desiredWidthPx;
  const l2 = b2?.left ?? l1 + w1 + gapPx;

  await setWindowBounds(userPage, {
    left: l2 + w2 + gapPx,
    top: topPx,
    width: desiredWidthPx,
    height: desiredHeightPx
  });
  await userPage.waitForTimeout(60);
  const b3 = (await getWindowBounds(userPage))?.bounds;

  // 可选：尝试用 CDP 获取真实屏幕可用宽度，然后整体居中一次。
  let screenAvailWidthPx = 0;
  try {
    const client = await adminPage.context().newCDPSession(adminPage);
    const infos = (await client.send('Emulation.getScreenInfos').catch(() => null)) as any;
    const primary = infos?.screenInfos?.[0];
    const avail = primary?.availWidth ?? primary?.workArea?.width ?? 0;
    screenAvailWidthPx = typeof avail === 'number' ? avail : 0;
  } catch {
    // 忽略异常
  }

  if (screenAvailWidthPx > 0 && b1 && b2 && b3) {
    const leftMost = b1.left ?? marginPx;
    const rightMost = (b3.left ?? 0) + (b3.width ?? 0);
    const total = rightMost - leftMost;
    if (total > 0 && screenAvailWidthPx > total + marginPx * 2) {
      const targetLeft = Math.floor((screenAvailWidthPx - total) / 2);
      const delta = targetLeft - leftMost;
      await Promise.all([
        setWindowBounds(adminPage, { left: (b1.left ?? 0) + delta, top: b1.top ?? topPx, width: b1.width ?? desiredWidthPx, height: b1.height ?? desiredHeightPx }),
        setWindowBounds(vendorPage, { left: (b2.left ?? 0) + delta, top: b2.top ?? topPx, width: b2.width ?? desiredWidthPx, height: b2.height ?? desiredHeightPx }),
        setWindowBounds(userPage, { left: (b3.left ?? 0) + delta, top: b3.top ?? topPx, width: b3.width ?? desiredWidthPx, height: b3.height ?? desiredHeightPx })
      ]);
    }
  }

  // 调试输出：方便确认“逻辑像素 vs CDP 单位”的换算是否命中。
  try {
    const adminOuter = await adminPage.evaluate(() => ({ x: window.screenX, y: window.screenY, w: window.outerWidth, h: window.outerHeight }));
    const adminBounds = await getWindowBounds(adminPage);
    console.log(
      `[e2e][window] dpiScale=${dpiScale} desired=${desiredWidth}x${desiredHeight} ` +
        `adminOuter=${adminOuter.w}x${adminOuter.h} adminBounds=${JSON.stringify(adminBounds?.bounds ?? null)} screenAvailWidthPx=${screenAvailWidthPx || 'n/a'}`
    );
  } catch {
    // 忽略异常
  }
};

const demoViewportLayout = {
  // 窗口物理宽度较小（例如 500），但我们希望触发桌面端断点（通常 >= 1024）。
  // 用 deviceScaleFactor < 1 让 viewport( CSS 像素 ) 变大而不增大窗口物理尺寸。
  // 经验值：0.5 时 500px 窗口约等于 1000px 的布局宽度。
  deviceScaleFactor: Number(process.env.E2E_DEMO_DSF ?? 0.5),
  // 让桌面端布局可用（>=1024），同时确保 viewportWidth * dsf <= winWidth。
  width: Number(process.env.E2E_DEMO_VIEWPORT_W ?? 1040),
  height: Number(process.env.E2E_DEMO_VIEWPORT_H ?? 1600)
};

const launchDemoBrowser = async (slot: DemoWindowSlot) => {
  const { width, height, gap, top } = demoWindowLayout;
  const left = slot * (width + gap);

  return chromium.launch({
    headless: false,
    // slowMo 将在 test 内根据是否 headed 来决定传多少（这里只保留占位）。
    args: [`--window-size=${width},${height}`, `--window-position=${left},${top}`]
  });
};

const resolveUrl = (page: Page, pathname: string) => {
  const current = page.url();
  const base = current && current.startsWith('http') ? current : rawBaseURL;
  return new URL(pathname, base).toString();
};

const gotoPath = async (page: Page, pathname: string) => {
  const inApp = await page.locator('.auth-layout__shell').count().then((c) => c > 0).catch(() => false);
  if (inApp && pathname.startsWith('/app/')) {
    await page
      .evaluate((path) => {
        window.history.pushState({}, '', path);
        window.dispatchEvent(new PopStateEvent('popstate'));
      }, pathname)
      .catch(() => undefined);
    await page.waitForURL(`**${pathname}**`, { timeout: 30_000 }).catch(async () => {
      await page.goto(resolveUrl(page, pathname));
    });
    return;
  }
  await page.goto(resolveUrl(page, pathname));
};

const uniqueEmail = (prefix: string) => {
  const ts = Date.now();
  return `${prefix}_${ts}@example.com`;
};

async function gotoLogin(page: Page) {
  await gotoPath(page, '/login');
  await expect(page.getByText('账号登录')).toBeVisible();
}

async function logoutIfNeeded(page: Page) {
  // 已登录时，/login 会被 router redirect 到 /app/dashboard
  await gotoPath(page, '/app/dashboard');
  const userButton = page.getByRole('button', { name: /@/ });
  if (await userButton.isVisible().catch(() => false)) {
    await userButton.click();
    await page.getByRole('menuitem', { name: '退出登录' }).click();
  }
  await gotoLogin(page);
}

async function login(page: Page, username: string, password: string) {
  await gotoLogin(page);
  await page.getByLabel('用户名').fill(username);
  await page.getByLabel('密码').fill(password);
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await page.waitForURL('**/app/**', { timeout: 30_000 });
  // 小视口/移动端样式下，侧边栏可能收起导致 logo 不可见；以 header 是否渲染为准。
  await expect(page.locator('.auth-layout__header')).toBeVisible({ timeout: 30_000 });
  const usernameLocator = page.locator('.auth-layout__username');
  if (await usernameLocator.isVisible().catch(() => false)) {
    await expect(usernameLocator).toContainText(username, { timeout: 30_000 });
  }
}

async function register(page: Page, role: 'USER' | 'VENDOR', username: string, password: string) {
  // 某些部署环境可能会把直达 /register 重定向回 /login；因此统一从登录页进入注册页。
  await gotoLogin(page);
  await page.getByText('立即注册', { exact: true }).click();
  await page.waitForURL('**/register**', { timeout: 30_000 });
  await expect(page.getByText('用户注册')).toBeVisible();

  // 注册类型 segmented：点击可见标签（避免 radio input 不可见导致卡死）
  if (role === 'VENDOR') {
    await page.locator('.ant-segmented-item-label', { hasText: '厂商' }).click();
  } else {
    await page.locator('.ant-segmented-item-label', { hasText: '消费者' }).click();
  }

  await page.getByLabel('用户名').fill(username);
  await page.getByLabel('密码', { exact: true }).fill(password);
  await page.getByLabel('确认密码', { exact: true }).fill(password);
  await page.getByRole('button', { name: /注\s*册/ }).click();

  // 注册成功会回到登录页（带 query.username）
  await page.waitForURL('**/login**', { timeout: 30_000 });
}

async function openNav(page: Page, label: string) {
  // 侧边栏菜单项为 <span> 文本，Playwright 可直接点。
  await page.getByRole('menuitem', { name: label }).click();
}

async function submitVendorOnboarding(page: Page, contactEmail: string) {
  await gotoPath(page, '/app/vendor/onboarding');
  await expect(page.getByRole('heading', { name: '厂商入驻 · 一站式跟踪' })).toBeVisible();

  const stamp = String(Date.now()).slice(-9).padStart(9, '0');
  const unifiedSocialCode = `91110000${stamp}X`;

  await page.getByPlaceholder('请输入公司主体名称').fill('测试科技有限公司');
  await page.getByPlaceholder('请输入 18 位信用代码').fill(unifiedSocialCode);
  await page.getByPlaceholder('负责人姓名').fill('张三');
  await page.getByPlaceholder('手机或座机').fill('13800138000');
  await page.getByPlaceholder('用于接收通知（可选）').fill(contactEmail);
  await page.getByRole('button', { name: /提\s*交\s*申\s*请/ }).click();

  // 提交成功后会刷新列表，最新申请将展示“审核中”
  await expect(page.getByText('审核中', { exact: true })).toBeVisible({ timeout: 60_000 });
}

async function adminApproveLatestVendorApplication(page: Page) {
  await gotoPath(page, '/app/admin/review');
  await expect(page.locator('.page-header__title', { hasText: '审核大厅' })).toBeVisible();

  // 厂商入驻卡片：包含公司名
  await page.locator('.review-card', { hasText: '测试科技有限公司' }).first().click();
  // 抽屉内点击“通过”
  await page.getByRole('button', { name: /通\s*过/ }).click();
  await expect(page.getByText('已通过申请')).toBeVisible();
}

async function vendorCreateAndSubmitProduct(page: Page, productName: string) {
  await gotoPath(page, '/app/vendor/workbench/products');
  await expect(page.getByRole('heading', { name: '商品集合' })).toBeVisible();

  await page.getByRole('button', { name: '新建商品' }).click();

  const createDrawer = page.locator('.ant-drawer').filter({ hasText: '新建商品' }).first();
  await expect(createDrawer).toBeVisible();

  await createDrawer.getByPlaceholder('请输入名称').fill(productName);
  await createDrawer.getByPlaceholder('如 OFFICE').fill('3C');

  await createDrawer.locator('input[type="file"]').setInputFiles(asset('product-cover.jpg'));

  await createDrawer.getByRole('button', { name: '创建商品' }).click();

  // 进入详情抽屉
  const detailDrawer = page.locator('.ant-drawer').filter({ hasText: productName }).first();
  await expect(detailDrawer).toBeVisible({ timeout: 30_000 });

  // 新增方案
  await detailDrawer.getByRole('tab', { name: '方案 & SKU' }).click();
  await detailDrawer.getByRole('button', { name: '新增方案' }).click();

  const planModal = page.getByRole('dialog', { name: '新增租赁方案' });
  await expect(planModal).toBeVisible();

  await planModal.locator('.ant-form-item', { hasText: '租期（月）' }).locator('input').fill('3');
  await planModal.locator('.ant-form-item', { hasText: '押金 (¥)' }).locator('input').fill('2000');
  await planModal.locator('.ant-form-item', { hasText: '月租金 (¥)' }).locator('input').fill('300');

  // 点击“确定”按钮（兼容 OK 文案）
  await planModal.getByRole('button', { name: /确\s*定|OK/ }).click();
  await expect(planModal).toBeHidden({ timeout: 30_000 });
  await expect(detailDrawer.getByRole('button', { name: '新增 SKU' }).first()).toBeVisible({ timeout: 30_000 });

  // 新增 SKU（第一个方案卡片里）
  const skuCode = `E2E-SKU-${Date.now()}`;
  await detailDrawer.getByRole('button', { name: '新增 SKU' }).first().click();
  const skuModal = page.getByRole('dialog', { name: '新增 SKU' });
  await expect(skuModal).toBeVisible();
  await skuModal.locator('.ant-form-item', { hasText: 'SKU 编码' }).locator('input').fill(skuCode);
  await skuModal.locator('.ant-form-item', { hasText: '总库存' }).locator('input').fill('10');
  await skuModal.getByRole('button', { name: /确\s*定|OK/ }).click();
  await expect(skuModal).toBeHidden({ timeout: 30_000 });
  await expect(detailDrawer.getByText(skuCode)).toBeVisible({ timeout: 30_000 });

  // 启用方案（目录侧仅展示 ACTIVE 方案）
  await detailDrawer.getByRole('button', { name: /启\s*用/ }).first().click();
  await expect(page.getByText('方案已启用')).toBeVisible({ timeout: 30_000 });

  // 提交审核
  await detailDrawer.getByRole('tab', { name: '基础资料' }).click();
  await detailDrawer.getByRole('button', { name: /提\s*交\s*审\s*核/ }).click();
  await expect(detailDrawer.getByRole('button', { name: /提\s*交\s*审\s*核/ })).toBeDisabled({ timeout: 30_000 });

  // 关闭抽屉
  await detailDrawer.locator('button[aria-label="Close"], .ant-drawer-close').first().click().catch(() => undefined);
}

async function adminApproveProductByUi(page: Page, productName: string) {
  await gotoPath(page, '/app/admin/review');
  await expect(page.locator('.page-header__title', { hasText: '审核大厅' })).toBeVisible();

  const productSection = page
    .locator('section.page-section')
    .filter({ has: page.getByRole('heading', { name: '商品审核' }) })
    .first();
  await expect(productSection).toBeVisible({ timeout: 30_000 });

  const productCard = productSection.locator('.review-card', { hasText: productName }).first();

  // 列表可能尚未刷新（管理员此前可能已打开过该页），通过切换商品筛选触发 loadProducts。
  try {
    await expect(productCard).toBeVisible({ timeout: 15_000 });
  } catch {
    // 切到“已上线”再切回“待审核”（限定在商品审核区块，避免点到厂商筛选）
    await productSection.getByText('已上线', { exact: true }).click().catch(() => undefined);
    await productSection.getByText('待审核', { exact: true }).click().catch(() => undefined);
    await expect(productCard).toBeVisible({ timeout: 60_000 });
  }

  await productCard.click();
  const drawer = page.locator('.ant-drawer').filter({ hasText: '商品审核' }).first();
  await expect(drawer).toBeVisible({ timeout: 30_000 });
  await expect(drawer.getByText(productName)).toBeVisible({ timeout: 30_000 });

  // 填写备注（可选），再通过
  await drawer.getByPlaceholder('审核备注').fill('e2e approve');
  await drawer.getByRole('button', { name: /通\s*过/ }).click();
  await expect(page.getByText('已通过商品')).toBeVisible({ timeout: 60_000 });
}

async function userFillProfile(page: Page, userEmail: string) {
  await gotoPath(page, '/app/profile');
  await expect(page.getByRole('heading', { name: '资料与信用档案' })).toBeVisible();

  await page.getByLabel('姓名').fill('测试用户');
  await page.getByRole('radio', { name: '保密' }).click();
  await page.getByLabel('手机号').fill('13900139000');
  await page.getByLabel('邮箱').fill(userEmail);
  await page.getByLabel('联系地址').fill('广东省深圳市南山区 XX 路 XX 号');

  await page.getByRole('button', { name: '保存修改' }).first().click();
  await expect(page.getByText('个人资料已更新')).toBeVisible();
}

async function readCreditScoreFromProfile(page: Page): Promise<number> {
  await gotoPath(page, '/app/profile');
  await expect(page.getByRole('heading', { name: '资料与信用档案' })).toBeVisible();

  const tag = page.locator('.profile-summary .credit-card .ant-tag').first();
  await expect(tag).toBeVisible({ timeout: 30_000 });
  const text = (await tag.textContent().catch(() => '')) ?? '';
  const match = text.match(/(\d+)\s*分/);
  if (!match) {
    throw new Error(`无法解析信用分：${text}`);
  }
  return Number(match[1]);
}

async function readOrderNoFromOrderDetail(page: Page): Promise<string> {
  const eyebrow = page.locator('.detail-header__eyebrow').first();
  await expect(eyebrow).toBeVisible({ timeout: 60_000 });
  await expect(eyebrow).not.toHaveText(/加载中/, { timeout: 60_000 });
  const text = ((await eyebrow.textContent().catch(() => '')) ?? '').trim();
  const match = text.match(/订单号\s*(\S+)/);
  if (!match) {
    throw new Error(`无法从订单详情页解析订单号: ${text}`);
  }
  return match[1];
}

async function userCreateOrderFromCatalog(
  page: Page,
  productName: string,
  options?: { inquiryMessage?: string; remark?: string }
) {
  const inquiryMessage = options?.inquiryMessage ?? '请问大概多久可以发货？';
  const remark = options?.remark ?? '请尽快发货';

  await gotoPath(page, '/app/catalog');
  await expect(page.getByRole('heading', { name: '逛逛精选' })).toBeVisible();

  await page.getByPlaceholder('搜索品类或厂商').fill('iPhone');
  await page.getByPlaceholder('搜索品类或厂商').press('Enter');

  await page.locator('article.product-card', { hasText: productName }).first().click();
  await expect(page.getByRole('heading', { name: productName })).toBeVisible();

  // 下单前咨询
  await page.getByPlaceholder('填写租赁需求、交付时间或更多问题，厂商将在 72 小时内回复').fill(inquiryMessage);
  await page.getByRole('button', { name: '发送咨询' }).click();
  await expect(page.getByText('咨询已发送')).toBeVisible();

  // 立即租赁 -> checkout
  await page.getByRole('button', { name: '立即租赁' }).click();
  await page.waitForURL('**/app/checkout**');

  await page.getByPlaceholder('例如发货时间、配送注意事项（选填）').fill(remark);
  await page.getByRole('button', { name: '提交订单' }).click();

  // 创建订单后跳转到订单详情
  await page.waitForURL('**/app/orders/**/overview', { timeout: 60_000 });

  const orderNo = await readOrderNoFromOrderDetail(page);

  return { orderNo };
}

async function openOrderDetailByOrderNo(page: Page, orderNo: string) {
  await gotoPath(page, '/app/orders');
  await expect(page.getByRole('heading', { name: '订单时间线' })).toBeVisible({ timeout: 30_000 });

  const card = page.locator('article.order-card', { hasText: orderNo }).first();
  for (let attempt = 0; attempt < 8; attempt += 1) {
    if (await card.isVisible().catch(() => false)) {
      break;
    }
    const loadMore = page.getByRole('button', { name: '查看更多' });
    if (await loadMore.isVisible().catch(() => false)) {
      await loadMore.click();
      await page.waitForTimeout(250);
    } else {
      break;
    }
  }
  await expect(card).toBeVisible({ timeout: 60_000 });
  await card.getByRole('button', { name: '查看详情' }).click();
  await page.waitForURL('**/app/orders/**/overview', { timeout: 60_000 });
  await expect(page.locator('.detail-header__eyebrow', { hasText: orderNo }).first()).toBeVisible({ timeout: 60_000 });
}

async function switchOrderTab(page: Page, tabLabel: '概览' | '聊天' | '凭证' | '时间线') {
  const tab = page.getByRole('tab', { name: tabLabel });
  await expect(tab).toBeVisible({ timeout: 30_000 });
  await tab.click({ timeout: 30_000, force: true });
}

async function vendorReplyInquiry(page: Page, inquiryMessage: string, replyText: string) {
  await gotoPath(page, '/app/vendor/workbench/products');
  await expect(page.getByRole('heading', { name: '商品集合' })).toBeVisible();

  const inquirySection = page
    .locator('section.page-section')
    .filter({ has: page.getByRole('heading', { name: '咨询收件箱' }) })
    .first();
  await expect(inquirySection).toBeVisible({ timeout: 30_000 });

  await inquirySection.getByRole('button', { name: '刷新' }).click().catch(() => undefined);

  const item = inquirySection.locator('.inquiry-item', { hasText: inquiryMessage }).first();
  await expect(item).toBeVisible({ timeout: 60_000 });

  await item.getByPlaceholder('输入回复').fill(replyText);
  await item.getByRole('button', { name: '发送回复' }).click();
  await expect(page.getByText('回复成功')).toBeVisible({ timeout: 60_000 });
}

async function userAssertInquiryReply(page: Page, productName: string, replyText: string) {
  await gotoPath(page, '/app/catalog');
  await expect(page.getByRole('heading', { name: '逛逛精选' })).toBeVisible();

  await page.getByPlaceholder('搜索品类或厂商').fill('iPhone');
  await page.getByPlaceholder('搜索品类或厂商').press('Enter');

  await page.locator('article.product-card', { hasText: productName }).first().click();
  await expect(page.getByRole('heading', { name: productName })).toBeVisible();

  await page.reload();
  await expect(page.getByText('厂商回复')).toBeVisible({ timeout: 60_000 });
  await expect(page.locator('.history-reply-text', { hasText: replyText }).first()).toBeVisible({ timeout: 60_000 });
}

async function userSendOrderChat(page: Page, orderNo: string, messageText: string) {
  await openOrderDetailByOrderNo(page, orderNo);
  await switchOrderTab(page, '聊天');
  await expect(page.getByRole('heading', { name: '聊天' })).toBeVisible({ timeout: 30_000 });

  const input = page.getByPlaceholder('输入要发送的内容…');
  await input.fill(messageText);
  await input.press('Enter');
  await expect(page.getByText('已发送')).toBeVisible({ timeout: 60_000 });
  await expect(page.locator('.chat-bubble__body', { hasText: messageText }).first()).toBeVisible({ timeout: 60_000 });
}

async function vendorShipOrder(
  page: Page,
  orderNo: string,
  chat?: { expectInbound?: string; reply?: string }
) {
  await gotoPath(page, '/app/vendor/workbench/fulfillment');
  await expect(page.getByRole('heading', { name: '履约任务墙' })).toBeVisible();

  // 默认列表分页较小，且历史数据可能较多；先切到“待发货”并刷新，必要时再加载更多。
  const wallSection = page
    .locator('section.page-section')
    .filter({ has: page.getByRole('heading', { name: '履约任务墙' }) })
    .first();
  await wallSection
    .locator('.filter-row')
    .getByText('待发货', { exact: true })
    .click({ timeout: 5_000, force: true })
    .catch(() => undefined);
  await wallSection.getByRole('button', { name: '刷新' }).click({ timeout: 5_000, force: true }).catch(() => undefined);

  // 找到目标订单卡片
  const card = page.locator('article.vendor-order-card', { hasText: orderNo }).first();
  for (let i = 0; i < 6; i += 1) {
    if (await card.isVisible().catch(() => false)) {
      break;
    }
    const loadMore = wallSection.getByRole('button', { name: '加载更多' });
    if (await loadMore.isVisible().catch(() => false)) {
      await loadMore.click();
    } else {
      break;
    }
  }
  await expect(card).toBeVisible({ timeout: 60_000 });
  await card.getByRole('button', { name: '进入工作台' }).click();

  const drawer = page.locator('.ant-drawer').filter({ hasText: '订单履约详情' }).first();
  await expect(drawer).toBeVisible();
  await expect(drawer.getByText(orderNo)).toBeVisible({ timeout: 60_000 });

  if (chat?.expectInbound || chat?.reply) {
    const chatPanel = drawer
      .locator('section.page-section')
      .filter({ has: page.getByRole('heading', { name: '沟通记录' }) })
      .first();
    await expect(chatPanel).toBeVisible({ timeout: 30_000 });

    if (chat.expectInbound) {
      await expect(chatPanel.getByText(chat.expectInbound)).toBeVisible({ timeout: 60_000 });
    }
    if (chat.reply) {
      const input = chatPanel.getByPlaceholder('输入要发送的内容…');
      await input.fill(chat.reply);
      await input.press('Enter');
      await expect(page.getByText('已发送')).toBeVisible({ timeout: 60_000 });
    }
  }

  // 上传发货凭证：至少 3 张照片 + 1 段视频
  const proofSection = drawer
    .locator('section.page-section')
    .filter({ has: page.getByRole('heading', { name: '凭证' }) })
    .first();
  await expect(proofSection).toBeVisible({ timeout: 30_000 });
  await proofSection.locator('.ant-radio-button-wrapper', { hasText: '发货' }).click();

  await proofSection.locator('input.proof-uploader__file-input').setInputFiles([
    assetPayload('proof-shipment-1.jpg', 'image/jpeg'),
    assetPayload('proof-shipment-2.jpg', 'image/jpeg'),
    assetPayload('proof-shipment-3.jpg', 'image/jpeg'),
    assetPayload('proof-video.mp4', 'video/mp4')
  ]);
  await proofSection.getByRole('button', { name: '上传全部' }).click();
  await expect(page.getByText('凭证已上传').first()).toBeVisible({ timeout: 60_000 });

  // 提交发货（纯 UI 操作）
  const shipCard = drawer.locator('.action-card', { hasText: '发货' }).first();
  await expect(shipCard).toBeVisible({ timeout: 30_000 });

  // 等待按钮可点：只有满足凭证要求才允许提交
  const submitShipButton = shipCard.getByRole('button', { name: '提交发货' });
  await expect(submitShipButton).toBeEnabled({ timeout: 60_000 });

  await shipCard.locator('.ant-form-item', { hasText: '承运方' }).locator('input').fill('SF');
  await shipCard.locator('.ant-form-item', { hasText: '运单号' }).locator('input').fill('SF123456789');
  await shipCard
    .locator('.ant-form-item', { hasText: '留言给用户' })
    .locator('textarea')
    .fill('商品已发出，请留意签收');

  await submitShipButton.click();
  await expect(page.getByText('已提交发货')).toBeVisible({ timeout: 60_000 });

  // 关闭抽屉，避免后续操作被遮罩层阻挡。
  await drawer.locator('button[aria-label="Close"], .ant-drawer-close').first().click().catch(() => undefined);
}

async function userReceiveAndConfirm(page: Page, orderNo: string) {
  await openOrderDetailByOrderNo(page, orderNo);
  await switchOrderTab(page, '凭证');
  await expect(page.getByRole('heading', { name: '凭证墙' })).toBeVisible();

  // 上传收货凭证：至少 2 张照片 + 1 段视频
  await page.locator('.ant-radio-button-wrapper', { hasText: '收货' }).click();
  await page.locator('input.proof-uploader__file-input').setInputFiles([
    assetPayload('proof-receive-1.jpg', 'image/jpeg'),
    assetPayload('proof-receive-2.jpg', 'image/jpeg'),
    assetPayload('proof-video.mp4', 'video/mp4')
  ]);
  await page.getByRole('button', { name: '上传全部' }).click();
  await expect(page.getByText('已上传凭证').first()).toBeVisible({ timeout: 60_000 });

  // 确认收货
  await switchOrderTab(page, '概览');
  await page.getByRole('button', { name: '确认收货' }).click();

  const confirmModal = page.locator('.ant-modal-confirm').filter({ hasText: '确认已收到货物？' }).first();
  await expect(confirmModal).toBeVisible();
  await confirmModal.getByRole('button', { name: /确\s*定|OK/ }).click();
  await expect(page.getByText('已确认收货')).toBeVisible({ timeout: 60_000 });
}

async function vendorRequestInspection(page: Page, orderNo: string) {
  await gotoPath(page, '/app/vendor/workbench/fulfillment');
  await expect(page.getByRole('heading', { name: '履约任务墙' })).toBeVisible();

  const wallSection = page
    .locator('section.page-section')
    .filter({ has: page.getByRole('heading', { name: '履约任务墙' }) })
    .first();
  await wallSection
    .locator('.filter-row')
    .getByText('履约中', { exact: true })
    .click({ timeout: 5_000, force: true })
    .catch(() => undefined);
  await wallSection.getByRole('button', { name: '刷新' }).click({ timeout: 5_000, force: true }).catch(() => undefined);

  const card = page.locator('article.vendor-order-card', { hasText: orderNo }).first();
  for (let i = 0; i < 6; i += 1) {
    if (await card.isVisible().catch(() => false)) {
      break;
    }
    const loadMore = wallSection.getByRole('button', { name: '加载更多' });
    if (await loadMore.isVisible().catch(() => false)) {
      await loadMore.click();
    } else {
      break;
    }
  }
  await expect(card).toBeVisible({ timeout: 60_000 });
  await card.getByRole('button', { name: '进入工作台' }).click();

  const drawer = page.locator('.ant-drawer').filter({ hasText: '订单履约详情' }).first();
  await expect(drawer).toBeVisible({ timeout: 30_000 });
  await expect(drawer.getByText(orderNo)).toBeVisible({ timeout: 30_000 });

  const inspectionCard = drawer.locator('.action-card', { hasText: '巡检' }).first();
  await expect(inspectionCard).toBeVisible({ timeout: 60_000 });

  await inspectionCard
    .getByPlaceholder(/例如：请拍摄/)
    .fill('请拍摄设备外观与开机状态（e2e）')
    .catch(() => undefined);
  await inspectionCard.getByRole('button', { name: '发起巡检请求' }).click();
  await expect(page.getByText('已发起巡检请求')).toBeVisible({ timeout: 60_000 });

  await drawer.locator('button[aria-label="Close"], .ant-drawer-close').first().click().catch(() => undefined);
}

async function userUploadInspectionProof(page: Page, orderNo: string) {
  await openOrderDetailByOrderNo(page, orderNo);
  await switchOrderTab(page, '凭证');
  await expect(page.getByRole('heading', { name: '凭证墙' })).toBeVisible();

  await page.locator('.ant-radio-button-wrapper', { hasText: '巡检' }).click();
  await page.locator('input.proof-uploader__file-input').setInputFiles([assetPayload('proof-receive-1.jpg', 'image/jpeg')]);
  await page.getByRole('button', { name: '上传全部' }).click();
  await expect(page.getByText('已上传凭证').first()).toBeVisible({ timeout: 60_000 });
}

async function adminResolveDispute(page: Page, orderNo: string, penalizeDelta: number) {
  await gotoPath(page, '/app/admin/orders');
  await expect(page.getByRole('main').getByRole('heading', { name: '订单监控' })).toBeVisible();

  const card = page.locator('.admin-card', { hasText: orderNo }).first();
  await expect(card).toBeVisible({ timeout: 60_000 });
  await card.getByRole('button', { name: '查看' }).click();

  const drawer = page.locator('.ant-drawer').filter({ hasText: '订单详情' }).first();
  await expect(drawer).toBeVisible({ timeout: 30_000 });
  await expect(drawer.getByText(orderNo)).toBeVisible({ timeout: 30_000 });

  const disputeCard = drawer.locator('.dispute-card').first();
  await expect(disputeCard).toBeVisible({ timeout: 60_000 });

  // headless 下如果仍用 force click，可能点到旁边的下拉框导致请求未触发（Playwright 不做命中校验）。
  // 这里显式确保按钮可交互，再点击触发请求。
  const aiSuggestBtn = disputeCard.locator('[data-testid^="dispute-ai-suggest-"]').first();
  await expect(aiSuggestBtn).toBeVisible({ timeout: 30_000 });
  await expect(aiSuggestBtn).toBeEnabled({ timeout: 30_000 });
  await aiSuggestBtn.click({ timeout: 30_000 });
  await expect(disputeCard.locator('[data-testid^="dispute-ai-suggest-result-"]')).toBeVisible({ timeout: 120_000 });
  await expect(disputeCard.getByText('事实摘要')).toBeVisible({ timeout: 30_000 });

  const penalizeInput = disputeCard.locator('.ant-input-number-input').first();
  await expect(penalizeInput).toBeVisible({ timeout: 30_000 });
  await penalizeInput.fill(String(penalizeDelta));
  await disputeCard.getByPlaceholder('备注').fill('e2e resolve');
  await disputeCard.getByRole('button', { name: /裁\s*决/ }).click({ timeout: 30_000, force: true });

  await expect(page.getByText('已提交裁决')).toBeVisible({ timeout: 60_000 });
  await drawer.locator('button[aria-label="Close"], .ant-drawer-close').first().click().catch(() => undefined);
}

async function pickOrderCardByUi(page: Page, options?: { excludeOrderNos?: string[] }) {
  const exclude = new Set(options?.excludeOrderNos ?? []);

  const parseCardOrderNo = async (card: Locator) => {
    const raw = ((await card.locator('.order-card__no').first().textContent().catch(() => '')) ?? '').trim();
    const match = raw.match(/订单号：\s*(\S+)/);
    return match?.[1] ?? '';
  };

  // 默认页 size=6，最多点几次“查看更多”即可找到新增订单
  for (let attempt = 0; attempt < 6; attempt += 1) {
    const cards = page.locator('article.order-card');
    const count = await cards.count();
    for (let i = 0; i < count; i += 1) {
      const card = cards.nth(i);
      const orderNo = await parseCardOrderNo(card);
      if (!orderNo) continue;
      if (!exclude.has(orderNo)) {
        return { card, orderNo };
      }
    }

    const loadMore = page.getByRole('button', { name: '查看更多' });
    if (await loadMore.isVisible().catch(() => false)) {
      await loadMore.click();
      await page.waitForTimeout(200);
    } else {
      break;
    }
  }

  throw new Error(`无法在订单墙中找到目标订单卡片（exclude=${Array.from(exclude).join(',') || 'n/a'}）`);
}

async function userCreateOrderFromCart(page: Page, productName: string, options?: { excludeOrderNos?: string[] }) {
  await gotoPath(page, '/app/catalog');
  await expect(page.getByRole('heading', { name: '逛逛精选' })).toBeVisible();

  await page.getByPlaceholder('搜索品类或厂商').fill('iPhone');
  await page.getByPlaceholder('搜索品类或厂商').press('Enter');

  await page.locator('article.product-card', { hasText: productName }).first().click();
  await expect(page.getByRole('heading', { name: productName })).toBeVisible();

  await page.getByRole('button', { name: '加入购物车' }).click();
  await expect(page.getByText('已加入购物车')).toBeVisible({ timeout: 60_000 });

  await page.waitForURL('**/app/cart**', { timeout: 30_000 });

  await page.getByRole('button', { name: '生成订单' }).click();
  await expect(page.getByText('订单试算')).toBeVisible({ timeout: 30_000 });

  await page.getByRole('button', { name: '确认创建订单' }).click();
  await page.waitForURL('**/app/orders**', { timeout: 60_000 });

  await expect(page.getByRole('heading', { name: '订单时间线' })).toBeVisible({ timeout: 30_000 });

  // 只从 UI 上找订单卡片，不从网络响应取数。
  const picked = await pickOrderCardByUi(page, { excludeOrderNos: options?.excludeOrderNos });
  await expect(picked.card).toBeVisible({ timeout: 60_000 });
  await picked.card.getByRole('button', { name: '查看详情' }).click();

  await page.waitForURL('**/app/orders/**/overview', { timeout: 60_000 });
  const orderNo = await readOrderNoFromOrderDetail(page);
  return { orderNo };
}

async function userApplyBuyout(page: Page, orderNo: string) {
  await openOrderDetailByOrderNo(page, orderNo);
  await switchOrderTab(page, '概览');
  await expect(page.getByRole('heading', { name: '租赁摘要' })).toBeVisible({ timeout: 30_000 });

  await page.getByRole('button', { name: '申请买断' }).click();
  const modal = page.getByRole('dialog', { name: '买断申请' });
  await expect(modal).toBeVisible({ timeout: 30_000 });
  await modal.locator('.ant-form-item', { hasText: '买断金额' }).locator('input').fill('100');
  await modal.locator('.ant-form-item', { hasText: '备注' }).locator('textarea').fill('e2e buyout');
  await modal.getByRole('button', { name: '提交申请' }).click();
  await expect(page.getByText('买断申请已提交')).toBeVisible({ timeout: 60_000 });
}

async function vendorApproveBuyout(page: Page, orderNo: string) {
  await gotoPath(page, '/app/vendor/workbench/fulfillment');
  await expect(page.getByRole('heading', { name: '履约任务墙' })).toBeVisible();

  const wallSection = page
    .locator('section.page-section')
    .filter({ has: page.getByRole('heading', { name: '履约任务墙' }) })
    .first();
  await wallSection
    .locator('.filter-row')
    .getByText('买断申请', { exact: true })
    .click({ timeout: 5_000, force: true })
    .catch(() => undefined);
  await wallSection.getByRole('button', { name: '刷新' }).click({ timeout: 5_000, force: true }).catch(() => undefined);

  const card = page.locator('article.vendor-order-card', { hasText: orderNo }).first();
  for (let i = 0; i < 6; i += 1) {
    if (await card.isVisible().catch(() => false)) {
      break;
    }
    const loadMore = wallSection.getByRole('button', { name: '加载更多' });
    if (await loadMore.isVisible().catch(() => false)) {
      await loadMore.click();
    } else {
      break;
    }
  }
  await expect(card).toBeVisible({ timeout: 60_000 });
  await card.getByRole('button', { name: '进入工作台' }).click();

  const drawer = page.locator('.ant-drawer').filter({ hasText: '订单履约详情' }).first();
  await expect(drawer).toBeVisible({ timeout: 30_000 });
  await expect(drawer.getByText(orderNo)).toBeVisible({ timeout: 30_000 });

  const buyoutCard = drawer.locator('.action-card', { hasText: '买断审批' }).first();
  await expect(buyoutCard).toBeVisible({ timeout: 60_000 });
  await buyoutCard.getByRole('button', { name: '同意买断' }).click();

  await expect(drawer.getByText('BUYOUT_COMPLETED')).toBeVisible({ timeout: 60_000 });
  await drawer.locator('button[aria-label="Close"], .ant-drawer-close').first().click().catch(() => undefined);
}

async function vendorAssertSettlementHasCommission(page: Page) {
  await gotoPath(page, '/app/vendor/workbench/settlement');
  await expect(page.locator('h2.page-section__title', { hasText: '结算中心' }).first()).toBeVisible({ timeout: 30_000 });
  await expect(page.getByText('平台抽成：¥').first()).toBeVisible({ timeout: 60_000 });
}

async function adminAssertDashboardHasMetrics(page: Page) {
  await gotoPath(page, '/app/dashboard');
  await expect(page.getByRole('heading', { name: 'FlexLease 驾驶舱' })).toBeVisible({ timeout: 30_000 });
  await expect(page.getByText('信用分布')).toBeVisible({ timeout: 30_000 });
  await expect(page.getByText('纠纷态势')).toBeVisible({ timeout: 30_000 });
}

async function userAssertNotificationHasContextType(page: Page, contextType: string) {
  await gotoPath(page, '/app/notifications');
  await expect(page.getByRole('heading', { name: '通知中心 · 信用与纠纷同频' })).toBeVisible({ timeout: 30_000 });
  await expect(page.locator('.notification-card__pills .ant-tag', { hasText: contextType }).first()).toBeVisible({
    timeout: 60_000
  });
}

async function userDisputeAndEscalate(page: Page, orderNo: string) {
  await openOrderDetailByOrderNo(page, orderNo);
  await switchOrderTab(page, '时间线');
  await expect(page.getByRole('heading', { name: '纠纷与仲裁' })).toBeVisible();

  await page.getByRole('button', { name: '发起纠纷' }).click();

  const disputeModal = page.getByRole('dialog', { name: '发起纠纷' });
  await expect(disputeModal).toBeVisible();

  await disputeModal.getByPlaceholder('请描述问题').fill(
    '收到的设备屏幕有明显划痕，与发货凭证中的设备状态不符，疑似发错货或运输损坏。'
  );
  await disputeModal
    .locator('.ant-form-item', { hasText: '补充说明' })
    .locator('textarea')
    .fill('已通过聊天联系厂商但未得到满意答复');

  await disputeModal.getByRole('button', { name: /提\s*交/ }).click();
  await expect(page.getByText('纠纷已提交')).toBeVisible({ timeout: 60_000 });

  await expect(page.getByText('协商中')).toBeVisible();
  await page.getByRole('button', { name: '升级平台仲裁' }).click();

  const escalateModal = page.getByRole('dialog', { name: '升级平台仲裁' });
  await expect(escalateModal).toBeVisible();
  await escalateModal
    .locator('.ant-form-item', { hasText: '补充说明' })
    .locator('textarea')
    .fill('与厂商协商未达成一致，申请平台介入仲裁');
  await escalateModal.getByRole('button', { name: /确\s*认/ }).click();

  await expect(page.getByText('已提交升级请求')).toBeVisible({ timeout: 60_000 });
  await expect(page.getByText('待平台处理')).toBeVisible();
}

async function ensureE2EBaseUp(page: Page) {
  await gotoPath(page, '/login');
  await expect(page.getByText('账号登录')).toBeVisible();
}

async function newContextWithBase(browser: Browser) {
  return browser.newContext({
    baseURL: rawBaseURL
  });
}

async function newContextWithBaseForDemo(browser: Browser) {
  // demo 演示默认使用“桌面端渲染 + 缩放适配小窗口”。
  // 关键点：viewport 是 CSS 像素，window-size 是物理窗口像素；两者配合 deviceScaleFactor 才能既三列小窗又保持桌面端布局。
  const viewportWidth = demoRenderMode === 'mobile' ? demoWindowLayout.width : demoViewportLayout.width;
  const viewportHeight = demoRenderMode === 'mobile' ? demoWindowLayout.height : demoViewportLayout.height;
  const deviceScaleFactor =
    demoRenderMode === 'mobile' ? undefined : demoViewportLayout.deviceScaleFactor;

  return browser.newContext({
    baseURL: rawBaseURL,
    viewport: {
      width: viewportWidth,
      height: viewportHeight
    },
    deviceScaleFactor,
    isMobile: demoRenderMode === 'mobile',
    hasTouch: demoRenderMode === 'mobile'
  });
}

test.describe.serial('FlexLease E2E（由 playwright-mcp 操作转换）', () => {
  test.setTimeout(15 * 60 * 1000);

  test('关键旅程覆盖：注册→入驻→上架→咨询/沟通→下单(直达+购物车)→履约(发货/收货/巡检)→纠纷裁决→买断→结算/看板/通知', async ({}, testInfo) => {
    // 以 Playwright 的实际配置为准判断是否 headed（比 argv/env 更可靠）。
    const isHeaded = (testInfo.project.use as any)?.headless === false;
    const slowMoMs = Number(process.env.E2E_SLOW_MO_MS ?? (isHeaded ? defaultSlowMoMs : 0));

    console.log(
      `[e2e] headed=${isHeaded} slowMo=${slowMoMs} win=${demoWindowLayout.width}x${demoWindowLayout.height} gap=${demoWindowLayout.gap} chromeScale=${demoChromeScaleFactor} winDpiScale=${demoWinDpiScaleOverride || 'auto'}`
    );

    const userEmail = uniqueEmail('e2e_user');
    const vendorEmail = uniqueEmail('e2e_vendor');
    const productName = `iPhone 15 Pro Max E2E ${Date.now()}`;

    let sharedBrowser: Browser | undefined;
    let adminBrowser: Browser | undefined;
    let vendorBrowser: Browser | undefined;
    let userBrowser: Browser | undefined;

    // 关键点：不要依赖 Playwright 的 browser fixture。
    // 否则 runner 仍会额外启动一个默认大窗口，导致“看起来还是重叠、没有三列”。
    if (isHeaded) {
      const { width, height, gap, top } = demoWindowLayout;
      const winArgs = (slot: DemoWindowSlot) => {
        const left = slot * (width + gap);
        return [
          `--window-size=${width},${height}`,
          `--window-position=${left},${top}`,
          '--high-dpi-support=1',
          `--force-device-scale-factor=${demoChromeScaleFactor}`
        ];
      };

      [adminBrowser, vendorBrowser, userBrowser] = await Promise.all([
        chromium.launch({ headless: false, slowMo: slowMoMs > 0 ? slowMoMs : undefined, args: winArgs(0) }),
        chromium.launch({ headless: false, slowMo: slowMoMs > 0 ? slowMoMs : undefined, args: winArgs(1) }),
        chromium.launch({ headless: false, slowMo: slowMoMs > 0 ? slowMoMs : undefined, args: winArgs(2) })
      ]);
    } else {
      sharedBrowser = await chromium.launch({ headless: true, slowMo: slowMoMs > 0 ? slowMoMs : undefined });
      adminBrowser = sharedBrowser;
      vendorBrowser = sharedBrowser;
      userBrowser = sharedBrowser;
    }

    const adminContext = isHeaded
      ? await newContextWithBaseForDemo(adminBrowser!)
      : await newContextWithBase(adminBrowser!);
    const vendorContext = isHeaded
      ? await newContextWithBaseForDemo(vendorBrowser!)
      : await newContextWithBase(vendorBrowser!);
    const userContext = isHeaded
      ? await newContextWithBaseForDemo(userBrowser!)
      : await newContextWithBase(userBrowser!);

    const adminPage = await adminContext.newPage();
    const vendorPage = await vendorContext.newPage();
    const userPage = await userContext.newPage();

    if (isHeaded) {
      // 多窗口演示：避免窗口被系统“挤到左边/重叠”，在 page 创建后再用 CDP 强制布局。
      await positionDemoWindows(adminPage, vendorPage, userPage);
    }

    await ensureE2EBaseUp(adminPage);

    // 注册/登录
    await register(userPage, 'USER', userEmail, passwords.user);
    await login(userPage, userEmail, passwords.user);

    await register(vendorPage, 'VENDOR', vendorEmail, passwords.vendor);
    await login(vendorPage, vendorEmail, passwords.vendor);

    // 厂商入驻 + 管理员审核
    await submitVendorOnboarding(vendorPage, vendorEmail);

    await login(adminPage, adminCreds.username, adminCreds.password);
    await adminApproveLatestVendorApplication(adminPage);

    // 建议审核通过后重新登录，让 vendorId 进入会话
    await logoutIfNeeded(vendorPage);
    await login(vendorPage, vendorEmail, passwords.vendor);

    // 厂商创建商品并提交审核 + 管理员审核上架
    await vendorCreateAndSubmitProduct(vendorPage, productName);
    await adminApproveProductByUi(adminPage, productName);

    const inquiryMessage = '请问大概多久可以发货？';
    const inquiryReply = `预计 24 小时内发货（e2e ${Date.now()}）`;
    const userChatMessage = '你好，我想确认发货时间与包装情况（e2e）';
    const vendorChatReply = '已收到，会按凭证指引发货并同步物流（e2e）';

    // 旅程 C：发现与试算（咨询）
    await userFillProfile(userPage, userEmail);

    // 旅程 D：下单与支付（直接结算）
    const order1 = await userCreateOrderFromCatalog(userPage, productName, {
      inquiryMessage,
      remark: '请尽快发货'
    });

    // 主题：沟通（咨询回复 + 订单聊天）
    await vendorReplyInquiry(vendorPage, inquiryMessage, inquiryReply);
    await userAssertInquiryReply(userPage, productName, inquiryReply);
    await userSendOrderChat(userPage, order1.orderNo, userChatMessage);

    // 旅程 E：履约与售后（发货/收货/巡检/纠纷）
    await vendorShipOrder(vendorPage, order1.orderNo, {
      expectInbound: userChatMessage,
      reply: vendorChatReply
    });
    await userReceiveAndConfirm(userPage, order1.orderNo);

    const creditBeforeInspection = await readCreditScoreFromProfile(userPage);
    await vendorRequestInspection(vendorPage, order1.orderNo);
    await userUploadInspectionProof(userPage, order1.orderNo);
    const creditAfterInspection = await readCreditScoreFromProfile(userPage);
    expect(creditAfterInspection).toBeGreaterThanOrEqual(creditBeforeInspection + 2);

    const creditBeforePenalty = await readCreditScoreFromProfile(userPage);
    await userDisputeAndEscalate(userPage, order1.orderNo);
    await adminResolveDispute(adminPage, order1.orderNo, 5);
    const creditAfterPenalty = await readCreditScoreFromProfile(userPage);
    expect(creditAfterPenalty).toBeLessThanOrEqual(creditBeforePenalty - 5);

    // 旅程 C：购物车试算下单（第二单）
    const order2 = await userCreateOrderFromCart(userPage, productName, { excludeOrderNos: [order1.orderNo] });
    await vendorShipOrder(vendorPage, order2.orderNo);
    await userReceiveAndConfirm(userPage, order2.orderNo);
    await userApplyBuyout(userPage, order2.orderNo);
    await vendorApproveBuyout(vendorPage, order2.orderNo);

    // 旅程 F：结算与运营
    await vendorAssertSettlementHasCommission(vendorPage);
    await adminAssertDashboardHasMetrics(adminPage);
    await userAssertNotificationHasContextType(userPage, 'DISPUTE');
    await userAssertNotificationHasContextType(userPage, 'CREDIT');

    await adminContext.close();
    await vendorContext.close();
    await userContext.close();

    // headed：关闭三个独立窗口；headless：关闭 sharedBrowser。
    if (isHeaded) {
      await adminBrowser?.close().catch(() => undefined);
      await vendorBrowser?.close().catch(() => undefined);
      await userBrowser?.close().catch(() => undefined);
    } else {
      await sharedBrowser?.close().catch(() => undefined);
    }
  });
});
