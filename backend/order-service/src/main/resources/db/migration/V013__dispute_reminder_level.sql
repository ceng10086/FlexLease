-- 添加多阶段提醒级别字段
ALTER TABLE "order".order_dispute
ADD COLUMN countdown_reminder_level INT NOT NULL DEFAULT 0;
