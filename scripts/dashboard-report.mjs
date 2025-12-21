#!/usr/bin/env node

/**
 * 简易仪表盘报表脚本
 * 用法：
 *   # 方式 1：管理员 JWT（推荐）
 *   FLEXLEASE_API_BASE=http://localhost:8080/api/v1 FLEXLEASE_API_TOKEN=xxx node scripts/dashboard-report.mjs
 *
 *   # 方式 2：内部调用（X-Internal-Token）
 *   FLEXLEASE_API_BASE=http://localhost:8080/api/v1 FLEXLEASE_INTERNAL_TOKEN=xxx node scripts/dashboard-report.mjs
 */

import fs from 'node:fs/promises';
import path from 'node:path';

const API_BASE = process.env.FLEXLEASE_API_BASE ?? 'http://localhost:8080/api/v1';
const TOKEN = process.env.FLEXLEASE_API_TOKEN;
const INTERNAL_TOKEN = process.env.FLEXLEASE_INTERNAL_TOKEN;
const OUTPUT_DIR = process.env.FLEXLEASE_REPORT_DIR ?? 'reports';

const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
const outputPath = path.join(process.cwd(), OUTPUT_DIR, `dashboard-${timestamp}.md`);

async function fetchDashboard() {
  const headers = TOKEN
    ? { Authorization: `Bearer ${TOKEN}` }
    : INTERNAL_TOKEN
      ? { 'X-Internal-Token': INTERNAL_TOKEN }
      : undefined;
  const response = await fetch(`${API_BASE}/analytics/dashboard`, {
    headers
  });
  if (!response.ok) {
    throw new Error(`请求失败：${response.status} ${response.statusText}`);
  }
  const payload = await response.json();
  if (payload.code !== 0) {
    throw new Error(`接口返回异常：${payload.message}`);
  }
  return payload.data;
}

const numberFormatter = new Intl.NumberFormat('zh-CN', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2
});

function formatSection(metrics) {
  const lines = [];
  lines.push(`# FlexLease 平台运营报表 (${new Date().toLocaleString('zh-CN')})`);
  lines.push('');
  lines.push('## 核心指标');
  lines.push(`- 总订单：${metrics.totalOrders}`);
  lines.push(`- 活跃订单：${metrics.activeOrders}`);
  lines.push(`- 在租中：${metrics.inLeaseCount}`);
  lines.push(`- 待退租：${metrics.pendingReturns}`);
  lines.push(`- GMV：¥${numberFormatter.format(metrics.totalGmv ?? 0)}`);
  lines.push('');
  lines.push('## 信用概览');
  lines.push(`- 平均信用分：${numberFormatter.format(metrics.creditMetrics?.averageScore ?? 0)}`);
  if (metrics.creditMetrics?.tierDistribution) {
    lines.push('- 信用分布：');
    Object.entries(metrics.creditMetrics.tierDistribution).forEach(([tier, count]) => {
      lines.push(`  - ${tier}: ${count}`);
    });
  }
  lines.push('');
  lines.push('## 纠纷态势');
  lines.push(`- 协商中：${metrics.disputeMetrics?.openCount ?? 0}`);
  lines.push(`- 待平台：${metrics.disputeMetrics?.pendingAdminCount ?? 0}`);
  lines.push(`- 已结案：${metrics.disputeMetrics?.resolvedCount ?? 0}`);
  lines.push(
    `- 平均处理时长：${numberFormatter.format(
      metrics.disputeMetrics?.averageResolutionHours ?? 0
    )} 小时`
  );
  lines.push('');
  lines.push('## 满意度调研');
  lines.push(
    `- 平均评分：${numberFormatter.format(metrics.surveyMetrics?.averageRating ?? 0)} 分`
  );
  lines.push(`- 待开放：${metrics.surveyMetrics?.pendingCount ?? 0}`);
  lines.push(`- 待填写：${metrics.surveyMetrics?.openCount ?? 0}`);
  lines.push(`- 已完成：${metrics.surveyMetrics?.completedCount ?? 0}`);
  lines.push('');
  return lines.join('\n');
}

try {
  const data = await fetchDashboard();
  const markdown = formatSection(data);
  await fs.mkdir(path.dirname(outputPath), { recursive: true });
  await fs.writeFile(outputPath, markdown, 'utf-8');
  console.log(`报表已生成：${outputPath}`);
} catch (error) {
  console.error('生成报表失败：', error.message);
  process.exitCode = 1;
}
