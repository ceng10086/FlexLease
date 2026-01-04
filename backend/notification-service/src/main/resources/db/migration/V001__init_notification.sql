CREATE TABLE notification.notification_template (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    subject VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification.notification_log (
    id UUID PRIMARY KEY,
    template_code VARCHAR(50),
    recipient VARCHAR(100) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_log_status_created_at
    ON notification.notification_log (status, created_at);

CREATE INDEX idx_notification_log_recipient
    ON notification.notification_log (recipient);

INSERT INTO notification.notification_template (id, code, subject, content)
VALUES
    ('11111111-1111-4111-8111-111111111111', 'ORDER_SHIPPED', '订单发货提醒', '您的订单 {{orderNo}} 已由 {{carrier}} 发出，运单号 {{trackingNo}}。'),
    ('22222222-2222-4222-8222-222222222222', 'PAYMENT_RECEIVED', '支付成功通知', '订单 {{orderNo}} 已完成支付，金额 ¥{{amount}}。');
