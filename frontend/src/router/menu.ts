export type NavLeaf = {
  key: string;
  label: string;
  path: string;
  icon?: string;
};

export type NavModule = {
  key: string;
  label: string;
  path?: string;
  icon?: string;
  children?: NavLeaf[];
};

export type NavSection = {
  key: string;
  label: string;
  roles: string[] | 'ANY';
  modules: NavModule[];
};

const NAV_TREE: NavSection[] = [
  {
    key: 'shared',
    label: '常用',
    roles: 'ANY',
    modules: [
      {
        key: 'dashboard-home',
        label: '驾驶舱',
        path: '/app/dashboard',
        icon: 'DashboardOutlined'
      },
      {
        key: 'notifications-center',
        label: '通知中心',
        path: '/app/notifications',
        icon: 'BellOutlined'
      }
    ]
  },
  {
    key: 'consumer',
    label: '消费者',
    roles: ['USER'],
    modules: [
      {
        key: 'catalog',
        label: '商品与下单',
        icon: 'AppstoreOutlined',
        children: [
          {
            key: 'catalog-feed',
            label: '逛逛',
            path: '/app/catalog',
            icon: 'AppstoreOutlined'
          },
          {
            key: 'checkout-flow',
            label: '确认下单',
            path: '/app/checkout',
            icon: 'CreditCardOutlined'
          }
        ]
      },
      {
        key: 'consumer-orders',
        label: '订单与聊天',
        icon: 'ScheduleOutlined',
        children: [
          {
            key: 'order-hub',
            label: '订单墙',
            path: '/app/orders',
            icon: 'ScheduleOutlined'
          },
          {
            key: 'order-chat-center',
            label: '聊天面板',
            path: '/app/orders/chat-center',
            icon: 'MessageOutlined'
          }
        ]
      },
      {
        key: 'profile',
        label: '个人资料',
        path: '/app/profile',
        icon: 'UserOutlined'
      }
    ]
  },
  {
    key: 'vendor',
    label: '厂商工作台',
    roles: ['VENDOR'],
    modules: [
      {
        key: 'vendor-workbench',
        label: '统一工作台',
        icon: 'ShopOutlined',
        children: [
          {
            key: 'vendor-products',
            label: '商品看板',
            path: '/app/vendor/workbench/products',
            icon: 'ContainerOutlined'
          },
          {
            key: 'vendor-fulfillment',
            label: '履约中台',
            path: '/app/vendor/workbench/fulfillment',
            icon: 'SendOutlined'
          },
          {
            key: 'vendor-insights',
            label: '指标洞察',
            path: '/app/vendor/workbench/insights',
            icon: 'FundOutlined'
          },
          {
            key: 'vendor-settlement',
            label: '结算中心',
            path: '/app/vendor/workbench/settlement',
            icon: 'TransactionOutlined'
          }
        ]
      },
      {
        key: 'vendor-chat-center',
        label: '沟通中心',
        path: '/app/vendor/chat-center',
        icon: 'MessageOutlined'
      },
      {
        key: 'vendor-onboarding',
        label: '入驻进度',
        path: '/app/vendor/onboarding',
        icon: 'FileAddOutlined'
      }
    ]
  },
  {
    key: 'admin',
    label: '平台管理',
    roles: ['ADMIN'],
    modules: [
      {
        key: 'admin-ops',
        label: '运营调度',
        children: [
          {
            key: 'admin-review',
            label: '审核大厅',
            path: '/app/admin/review',
            icon: 'TeamOutlined'
          },
          {
            key: 'admin-orders',
            label: '订单监控',
            path: '/app/admin/orders',
            icon: 'ProfileOutlined'
          }
        ]
      }
    ]
  },
  {
    key: 'arbitration',
    label: '仲裁管理',
    roles: ['ARBITRATOR', 'REVIEW_PANEL'],
    modules: [
      {
        key: 'arbitration-ops',
        label: '纠纷处理',
        children: [
          {
            key: 'arbitration-orders',
            label: '仲裁中心',
            path: '/app/arbitration/orders',
            icon: 'ControlOutlined'
          }
        ]
      }
    ]
  }
];

export const resolveMenuForRoles = (roles: string[]): NavSection[] => {
  const normalizedRoles = roles.length ? roles : [];
  return NAV_TREE.filter((section) => {
    if (section.roles === 'ANY') {
      return true;
    }
    return section.roles.some((role) => normalizedRoles.includes(role));
  });
};

export const flattenNavItems = (sections: NavSection[]): NavLeaf[] => {
  const leaves: NavLeaf[] = [];
  sections.forEach((section) => {
    section.modules.forEach((module) => {
      if (module.children && module.children.length) {
        module.children.forEach((child) => leaves.push(child));
      } else if (module.path) {
        leaves.push({
          key: module.key,
          label: module.label,
          path: module.path,
          icon: module.icon
        });
      }
    });
  });
  return leaves;
};

export const findNavItem = (key: string, sections: NavSection[]): NavLeaf | null => {
  for (const section of sections) {
    for (const module of section.modules) {
      if (module.key === key && module.path) {
        return {
          key: module.key,
          label: module.label,
          path: module.path,
          icon: module.icon
        };
      }
      if (module.children) {
        const found = module.children.find((child) => child.key === key);
        if (found) {
          return found;
        }
      }
    }
  }
  return null;
};

export const findNavPath = (key: string, sections: NavSection[]): string[] | null => {
  for (const section of sections) {
    for (const module of section.modules) {
      if (module.key === key) {
        return [section.key, module.key];
      }
      if (module.children) {
        const child = module.children.find((item) => item.key === key);
        if (child) {
          return [section.key, module.key, child.key];
        }
      }
    }
  }
  return null;
};

export type MobileTab = {
  key: string;
  label: string;
  path: string;
  icon: string;
};

const MOBILE_TAB_PRESETS: Record<string, MobileTab[]> = {
  USER: [
    { key: 'catalog-feed', label: '逛逛', path: '/app/catalog', icon: 'AppstoreOutlined' },
    { key: 'order-hub', label: '订单', path: '/app/orders', icon: 'ScheduleOutlined' },
    { key: 'order-chat-center', label: '聊天', path: '/app/orders/chat-center', icon: 'MessageOutlined' }
  ],
  VENDOR: [
    {
      key: 'vendor-products',
      label: '商品',
      path: '/app/vendor/workbench/products',
      icon: 'ContainerOutlined'
    },
    {
      key: 'vendor-fulfillment',
      label: '履约',
      path: '/app/vendor/workbench/fulfillment',
      icon: 'SendOutlined'
    },
    {
      key: 'vendor-chat',
      label: '沟通',
      path: '/app/vendor/chat-center',
      icon: 'MessageOutlined'
    }
  ],
  ADMIN: [
    { key: 'dashboard-home', label: '总览', path: '/app/dashboard', icon: 'DashboardOutlined' },
    { key: 'admin-review', label: '审核', path: '/app/admin/review', icon: 'TeamOutlined' },
    { key: 'admin-orders', label: '订单', path: '/app/admin/orders', icon: 'ProfileOutlined' }
  ],
  ARBITRATOR: [
    { key: 'dashboard-home', label: '总览', path: '/app/dashboard', icon: 'DashboardOutlined' },
    { key: 'arbitration-orders', label: '仲裁', path: '/app/arbitration/orders', icon: 'ControlOutlined' }
  ],
  REVIEW_PANEL: [
    { key: 'dashboard-home', label: '总览', path: '/app/dashboard', icon: 'DashboardOutlined' },
    { key: 'arbitration-orders', label: '复核', path: '/app/arbitration/orders', icon: 'ControlOutlined' }
  ]
};

export const resolveMobileTabs = (roles: string[]): MobileTab[] => {
  const dedup = new Map<string, MobileTab>();
  roles.forEach((role) => {
    const preset = MOBILE_TAB_PRESETS[role];
    if (!preset) {
      return;
    }
    preset.forEach((tab) => {
      if (!dedup.has(tab.key)) {
        dedup.set(tab.key, tab);
      }
    });
  });
  if (!dedup.size) {
    MOBILE_TAB_PRESETS.USER.forEach((tab) => dedup.set(tab.key, tab));
  }
  return Array.from(dedup.values());
};
