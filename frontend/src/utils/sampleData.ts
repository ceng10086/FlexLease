import type {
  AdminProductReview,
  CatalogProduct,
  DashboardMetric,
  PaymentRecord,
  RentalOrderSummary,
  SettlementRecord,
  VendorApplicationSummary,
  VendorMetric,
  VendorOrderSummary,
  VendorProductSummary
} from '@/types';

export const sampleDashboardMetrics: DashboardMetric[] = [
  { title: '在租商品', value: 128, trend: 8.6, trendLabel: '较上周' },
  { title: '月度GMV', value: '¥ 1,280,000', trend: 5.1, trendLabel: '环比' },
  { title: '履约完成率', value: '96.4%', trend: 2.3, trendLabel: '同比' }
];

export const sampleCatalogProducts: CatalogProduct[] = [
  {
    id: 'prd-1001',
    name: '智能会议屏 X1',
    category: '企业服务',
    vendorName: '优智科技',
    cover: 'https://images.unsplash.com/photo-1580894894513-541e068a3e2a?auto=format&fit=crop&w=800&q=80',
    pricePerMonth: 1899,
    deposit: 5000,
    modes: ['先租后买', '共享租赁'],
    inventory: 42,
    rating: 4.7,
    description: '支持4K无线投屏与AI语音助手的智能会议屏，适配远程协同场景。',
    tags: ['企业办公', 'AI助手', '远程协同'],
    planHighlights: ['含上门安装调试', '7×24小时云监控', '租满12个月可买断']
  },
  {
    id: 'prd-1002',
    name: '医用心电监护仪 S8',
    category: '医疗设备',
    vendorName: '安康医疗',
    cover: 'https://images.unsplash.com/photo-1583912268181-11b2de0d17d2?auto=format&fit=crop&w=800&q=80',
    pricePerMonth: 2599,
    deposit: 8000,
    modes: ['共享租赁', '以租代售'],
    inventory: 15,
    rating: 4.9,
    description: '三导联心电同步监护，支持医院 HIS 系统对接与远程诊疗数据同步。',
    tags: ['医疗', '物联网', '远程监护'],
    planHighlights: ['设备云端质检', '含培训与保养', '支持分期买断']
  },
  {
    id: 'prd-1003',
    name: '新能源轻卡',
    category: '城市物流',
    vendorName: '启程出行',
    cover: 'https://images.unsplash.com/photo-1581578017427-569726c27c51?auto=format&fit=crop&w=800&q=80',
    pricePerMonth: 4299,
    deposit: 12000,
    modes: ['共享租赁', '灵活日租'],
    inventory: 26,
    rating: 4.6,
    description: '城配专用新能源轻卡，支持车联网实时调度与能耗分析。',
    tags: ['新能源', '物流', '车联网'],
    planHighlights: ['含商业保险', '智能调度系统接入', '可申请碳积分补贴']
  }
];

export const sampleCustomerOrders: RentalOrderSummary[] = [
  {
    id: 'ord-9001',
    productName: '智能会议屏 X1',
    vendorName: '优智科技',
    planName: '共享租赁-12个月',
    mode: '共享租赁',
    status: '履约中',
    startDate: '2024-01-02',
    endDate: '2024-12-31',
    nextAction: '本月15日前完成续租付款',
    amountDue: 1899,
    timeline: [
      { label: '下单', time: '2023-12-28', status: 'completed' },
      { label: '发货', time: '2023-12-30', status: 'completed' },
      { label: '验收', time: '2024-01-05', status: 'completed' },
      { label: '下期扣款', time: '2024-11-15', status: 'pending' }
    ]
  },
  {
    id: 'ord-9002',
    productName: '医用心电监护仪 S8',
    vendorName: '安康医疗',
    planName: '以租代售-24个月',
    mode: '以租代售',
    status: '待续租',
    startDate: '2023-07-16',
    endDate: '2025-07-15',
    nextAction: '等待厂商审核续租申请',
    amountDue: 2599,
    timeline: [
      { label: '下单', time: '2023-07-10', status: 'completed' },
      { label: '发货', time: '2023-07-12', status: 'completed' },
      { label: '验收', time: '2023-07-16', status: 'completed' },
      { label: '续租审批', time: '2024-07-01', status: 'warning' }
    ]
  }
];

export const samplePaymentRecords: PaymentRecord[] = [
  {
    id: 'pay-7001',
    orderId: 'ord-9001',
    amount: 1899,
    method: '平台余额',
    status: '支付成功',
    createdAt: '2024-10-15 09:32'
  },
  {
    id: 'pay-7002',
    orderId: 'ord-9002',
    amount: 2599,
    method: '对公转账',
    status: '待确认',
    createdAt: '2024-10-12 16:21'
  },
  {
    id: 'pay-7003',
    orderId: 'ord-9002',
    amount: 2599,
    method: '银联快捷',
    status: '已退款',
    createdAt: '2024-09-15 10:18'
  }
];

export const sampleVendorProducts: VendorProductSummary[] = [
  {
    id: 'prd-1001',
    name: '智能会议屏 X1',
    status: '已上架',
    rentalModes: ['先租后买', '共享租赁'],
    totalInventory: 80,
    leased: 54,
    pendingOrders: 5,
    lastUpdated: '2024-10-18'
  },
  {
    id: 'prd-1004',
    name: '智能语音中控',
    status: '待审核',
    rentalModes: ['共享租赁'],
    totalInventory: 30,
    leased: 0,
    pendingOrders: 0,
    lastUpdated: '2024-10-16'
  }
];

export const sampleVendorOrders: VendorOrderSummary[] = [
  {
    id: 'ord-9101',
    customerName: '星云创新',
    productName: '智能会议屏 X1',
    status: '待发货',
    shipBy: '2024-10-20',
    nextStep: '安排物流并上传提货单',
    mode: '共享租赁',
    value: 22788
  },
  {
    id: 'ord-9102',
    customerName: '蓝海诊疗',
    productName: '医用心电监护仪 S8',
    status: '续租审批',
    shipBy: '2024-10-25',
    nextStep: '评估设备状态并反馈客户',
    mode: '以租代售',
    value: 62376
  }
];

export const sampleVendorMetrics: VendorMetric[] = [
  { label: '本月新增订单', value: '36', trend: 12.4 },
  { label: '履约完成率', value: '97.2%', trend: 1.2 },
  { label: '平均审批时长', value: '3.4天', trend: -0.6 }
];

export const sampleAdminReviews: AdminProductReview[] = [
  {
    id: 'rev-01',
    vendorName: '优智科技',
    productName: '智能语音中控',
    submittedAt: '2024-10-16 14:20',
    rentalModes: ['共享租赁'],
    status: '待审核',
    riskScore: 28
  },
  {
    id: 'rev-02',
    vendorName: '启程出行',
    productName: '新能源轻卡',
    submittedAt: '2024-10-15 09:10',
    rentalModes: ['共享租赁', '灵活日租'],
    status: '复审中',
    riskScore: 46
  }
];

export const sampleSettlements: SettlementRecord[] = [
  {
    id: 'set-01',
    vendorName: '优智科技',
    period: '2024-09',
    orderCount: 128,
    grossAmount: 980000,
    refundAmount: 12000,
    netAmount: 968000,
    status: '待打款'
  },
  {
    id: 'set-02',
    vendorName: '安康医疗',
    period: '2024-09',
    orderCount: 64,
    grossAmount: 660000,
    refundAmount: 8000,
    netAmount: 652000,
    status: '已结算'
  }
];

export const sampleVendorApplications: VendorApplicationSummary[] = [
  {
    id: 'va-01',
    companyName: '星汉新能源科技有限公司',
    contact: '李雷 138****6721',
    status: '待审核',
    submittedAt: '2024-10-18 10:23',
    remark: '主营新能源汽车租赁业务'
  },
  {
    id: 'va-02',
    companyName: '智瞳医疗影像有限公司',
    contact: '王芳 139****9930',
    status: '补充材料',
    submittedAt: '2024-10-17 15:48',
    remark: '等待补充资质证书'
  }
];
