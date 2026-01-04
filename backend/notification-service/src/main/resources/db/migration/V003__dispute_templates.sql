INSERT INTO notification.notification_template (id, code, subject, content)
VALUES
    ('44444444-4444-4444-8444-444444444444', 'DISPUTE_COUNTDOWN', '纠纷协商倒计时',
     '订单 {{orderNo}} 的纠纷将在 {{hoursLeft}} 小时后自动升级平台仲裁，请及时回复。'),
    ('55555555-5555-4555-8555-555555555555', 'DISPUTE_RESOLVED', '纠纷处理结果',
     '订单 {{orderNo}} 的纠纷已{{result}}, 请进入订单详情查看具体说明。');
