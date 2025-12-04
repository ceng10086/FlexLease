-- 添加用户冻结截止时间字段，用于自动解冻调度
ALTER TABLE users.user_profile
ADD COLUMN suspended_until TIMESTAMP WITH TIME ZONE;
