CREATE TABLE reference_audit_outbox (
    event_id UUID PRIMARY KEY,
    topic VARCHAR(249) NOT NULL,
    message_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(100)
);
CREATE INDEX ix_reference_outbox_pending ON reference_audit_outbox(next_attempt_at, created_at)
    WHERE published_at IS NULL;
