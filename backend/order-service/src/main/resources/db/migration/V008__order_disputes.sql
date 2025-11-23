CREATE TABLE IF NOT EXISTS "order".order_dispute (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES "order".rental_order(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    initiator_id UUID NOT NULL,
    initiator_role VARCHAR(20) NOT NULL,
    initiator_option VARCHAR(30) NOT NULL,
    initiator_reason TEXT,
    initiator_remark TEXT,
    respondent_id UUID,
    respondent_role VARCHAR(20),
    respondent_option VARCHAR(30),
    respondent_remark TEXT,
    responded_at TIMESTAMP WITH TIME ZONE,
    deadline_at TIMESTAMP WITH TIME ZONE,
    escalated_by UUID,
    escalated_at TIMESTAMP WITH TIME ZONE,
    admin_decision_option VARCHAR(30),
    admin_decision_remark TEXT,
    admin_decision_by UUID,
    admin_decision_at TIMESTAMP WITH TIME ZONE,
    user_credit_delta INT,
    appeal_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_dispute_order ON "order".order_dispute(order_id, created_at);
CREATE INDEX IF NOT EXISTS idx_order_dispute_status ON "order".order_dispute(status);
