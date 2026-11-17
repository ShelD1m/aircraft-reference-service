package ru.demo.nsi.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    public UUID id;
    @Column(name = "actor_id")
    public String actorId;
    @Column(name = "entity_type")
    public String entityType;
    @Column(name = "entity_id")
    public UUID entityId;
    @Column(name = "version_id")
    public UUID versionId;
    public String operation;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    public JsonNode oldValue;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    public JsonNode newValue;
    @Column(columnDefinition = "text")
    public String reason;
    @Column(name = "request_id")
    public String requestId;
    @Column(name = "created_at")
    public Instant createdAt;
}
