export type NavItem = {
  key: string;
  label: string;
  path: string;
  icon?: string;
  roles?: string[] | 'ANY';
  children?: NavItem[];
};

const NAV_SCHEMA: NavItem[] = [
  {
    key: 'overview',
    label: '平台总览',
    path: '/app/overview',
    icon: 'DashboardOutlined',
    roles: 'ANY'
  },
  {
    key: 'catalog',
    label: '商品目录',
    path: '/app/catalog',
    icon: 'AppstoreOutlined',
    roles: ['USER']
  },
  {
    key: 'cart',
    label: '购物车',
    path: '/app/cart',
    icon: 'ShoppingOutlined',
    roles: ['USER']
  },
  {
    key: 'orders',
    label: '我的订单',
    path: '/app/orders',
    icon: 'ShoppingCartOutlined',
    roles: ['USER']
  },
  {
    key: 'notifications',
    label: '通知中心',
    path: '/app/notifications',
    icon: 'BellOutlined',
    roles: 'ANY'
  },
  {
    key: 'admin-group',
    label: '平台管理',
    path: '',
    icon: 'ControlOutlined',
    roles: ['ADMIN'],
    children: [
      {
        key: 'admin-vendor-review',
        label: '厂商审核',
        path: '/app/admin/vendor-review',
        icon: 'TeamOutlined',
        roles: ['ADMIN']
      },
      {
        key: 'admin-product-review',
        label: '商品审核',
        path: '/app/admin/product-review',
        icon: 'InboxOutlined',
        roles: ['ADMIN']
      },
      {
        key: 'admin-orders',
        label: '订单监控',
        path: '/app/admin/orders',
        icon: 'ProfileOutlined',
        roles: ['ADMIN']
      },
      {
        key: 'admin-operations',
        label: '运营工具箱',
        path: '/app/admin/operations',
        icon: 'ToolOutlined',
        roles: ['ADMIN']
      }
    ]
  },
  {
    key: 'vendor-group',
    label: '厂商工作台',
    path: '',
    icon: 'ShopOutlined',
    roles: ['VENDOR'],
    children: [
      {
        key: 'vendor-onboarding',
        label: '入驻进度',
        path: '/app/vendor/onboarding',
        icon: 'FileAddOutlined',
        roles: ['VENDOR']
      },
      {
        key: 'vendor-products',
        label: '商品与方案',
        path: '/app/vendor/products',
        icon: 'ContainerOutlined',
        roles: ['VENDOR']
      },
      {
        key: 'vendor-orders',
        label: '订单履约',
        path: '/app/vendor/orders',
        icon: 'SendOutlined',
        roles: ['VENDOR']
      },
      {
        key: 'vendor-analytics',
        label: '运营指标',
        path: '/app/vendor/analytics',
        icon: 'FundOutlined',
        roles: ['VENDOR']
      },
      {
        key: 'vendor-settlements',
        label: '结算中心',
        path: '/app/vendor/settlements',
        icon: 'TransactionOutlined',
        roles: ['VENDOR']
      }
    ]
  }
];

const hasRole = (itemRoles: NavItem['roles'], roles: string[]): boolean => {
  if (!itemRoles || itemRoles === 'ANY') {
    return true;
  }
  return roles.some((role) => itemRoles.includes(role));
};

const filterMenu = (items: NavItem[], roles: string[]): NavItem[] =>
  items
    .map((item) => {
      if (item.children && item.children.length > 0) {
        const children = filterMenu(item.children, roles);
        if (children.length === 0) {
          return null;
        }
        return { ...item, children };
      }
      if (!hasRole(item.roles, roles)) {
        return null;
      }
      return item;
    })
    .filter((item): item is NavItem => Boolean(item));

export const resolveMenuForRoles = (roles: string[]): NavItem[] => {
  const safeRoles = roles.length > 0 ? roles : [];
  return filterMenu(NAV_SCHEMA, safeRoles);
};

export const findNavItem = (key: string, items: NavItem[] = NAV_SCHEMA): NavItem | null => {
  for (const item of items) {
    if (item.key === key) {
      return item;
    }
    if (item.children) {
      const found = findNavItem(key, item.children);
      if (found) {
        return found;
      }
    }
  }
  return null;
};

export const findNavPath = (
  key: string,
  items: NavItem[] = NAV_SCHEMA,
  trail: string[] = []
): string[] | null => {
  for (const item of items) {
    if (item.key === key) {
      return [...trail, item.key];
    }
    if (item.children) {
      const childTrail = findNavPath(key, item.children, [...trail, item.key]);
      if (childTrail) {
        return childTrail;
      }
    }
  }
  return null;
};
