CREATE TABLE IF NOT EXISTS "order".order_satisfaction_survey (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES "order".rental_order(id) ON DELETE CASCADE,
    dispute_id UUID REFERENCES "order".order_dispute(id) ON DELETE CASCADE,
    target_role VARCHAR(20) NOT NULL,
    target_ref UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    rating INTEGER,
    comment TEXT,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_order_survey_order ON "order".order_satisfaction_survey(order_id);
CREATE INDEX IF NOT EXISTS idx_order_survey_status_available ON "order".order_satisfaction_survey(status, available_at);
CREATE INDEX IF NOT EXISTS idx_order_survey_target_role ON "order".order_satisfaction_survey(target_ref, target_role);
