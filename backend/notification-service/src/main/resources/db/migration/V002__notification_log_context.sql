ALTER TABLE notification.notification_log
    ADD COLUMN context_type VARCHAR(50);

ALTER TABLE notification.notification_log
    ADD COLUMN context_ref VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_notification_log_context
    ON notification.notification_log (context_type, recipient, status);
