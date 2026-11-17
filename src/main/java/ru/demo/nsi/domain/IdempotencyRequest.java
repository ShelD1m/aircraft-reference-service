package ru.demo.nsi.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_requests")
public class IdempotencyRequest {
    @Id
    public UUID id;
    @Column(name = "actor_id")
    public String actorId;
    @Column(name = "http_method")
    public String httpMethod;
    @Column(name = "request_uri")
    public String requestUri;
    @Column(name = "idempotency_key")
    public String idempotencyKey;
    @Column(name = "request_hash")
    public String requestHash;
    @Column(name = "response_status")
    public Integer responseStatus;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    public JsonNode responseBody;
    @Column(name = "created_at")
    public Instant createdAt;
}
