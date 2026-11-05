-- Физическая демонстрационная модель НСИ. PostgreSQL 17 / Flyway.
-- Указанные экземпляры и нормативы в V2 являются вымышленными учебными данными.
CREATE TABLE directories (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    kind VARCHAR(40) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    revision BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_directory_kind CHECK (kind IN ('AIRCRAFT_TYPE','AIRCRAFT','OPERATION_TYPE','PROCEDURE_TEMPLATE','GENERIC'))
);

CREATE TABLE reference_versions (
    id UUID PRIMARY KEY,
    directory_id UUID NOT NULL REFERENCES directories(id) ON DELETE RESTRICT,
    version_number INTEGER NOT NULL CHECK (version_number > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','REVIEW','APPROVED','ARCHIVED')),
    base_version_id UUID REFERENCES reference_versions(id),
    change_reason TEXT,
    created_by VARCHAR(100) NOT NULL,
    approved_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    UNIQUE (directory_id, version_number)
);
CREATE UNIQUE INDEX ux_reference_one_approved ON reference_versions(directory_id) WHERE status = 'APPROVED';
CREATE INDEX ix_reference_versions_directory ON reference_versions(directory_id,version_number DESC);

CREATE TABLE directory_records (
    id UUID PRIMARY KEY,
    logical_id UUID NOT NULL,
    version_id UUID NOT NULL REFERENCES reference_versions(id) ON DELETE RESTRICT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (version_id, code),
    UNIQUE (version_id, logical_id)
);
CREATE INDEX ix_directory_records_version ON directory_records(version_id,code);

CREATE TABLE procedure_stages (
    id UUID PRIMARY KEY,
    logical_id UUID NOT NULL,
    record_id UUID NOT NULL REFERENCES directory_records(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL CHECK (sequence_number > 0),
    operation_code VARCHAR(100) NOT NULL,
    description TEXT,
    expected_duration_seconds INTEGER NOT NULL CHECK (expected_duration_seconds > 0),
    required_role VARCHAR(80) NOT NULL DEFAULT 'TECHNICIAN',
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (record_id, sequence_number),
    UNIQUE (record_id, logical_id)
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor_id VARCHAR(100) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID NOT NULL,
    version_id UUID,
    operation VARCHAR(50) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    reason TEXT,
    request_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_entity ON audit_log(entity_type,entity_id,created_at DESC);
CREATE INDEX ix_audit_actor ON audit_log(actor_id,created_at DESC);

CREATE TABLE idempotency_requests (
    id UUID PRIMARY KEY,
    actor_id VARCHAR(100) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_uri VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (actor_id,http_method,request_uri,idempotency_key)
);
CREATE INDEX ix_idempotency_created_at ON idempotency_requests(created_at);
