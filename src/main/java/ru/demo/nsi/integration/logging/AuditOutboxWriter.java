package ru.demo.nsi.integration.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.UUID;
import static ru.demo.nsi.service.NsiService.obj;

@Component
public class AuditOutboxWriter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final String topic;
    public AuditOutboxWriter(JdbcTemplate jdbc, ObjectMapper mapper,
            @Value("${integrations.logging.topic:hcc.audit.events.v1}") String topic) {
        this.jdbc = jdbc; this.mapper = mapper; this.topic = topic;
    }
    public String serialize(ReferenceAuditEvent e) {
        var data = obj("action", e.eventType(), "entityType", e.entityType(), "entityId", e.entityId(),
                "referenceVersionId", e.referenceVersionId(), "actorId", e.userId(), "requestId", e.requestId());
        var payload = obj("level", "INFO", "source", "ru.demo.nsi", "message", e.eventType(),
                "data", data, "actor_user_id", uuidOrNull(e.userId()));
        var envelope = obj("event_id", e.eventId(), "event_type", "audit.log", "producer_service", "REFERENCE",
                "occurred_at", e.timestamp().toString(), "correlation_id", uuidOrNull(MDC.get("correlationId")),
                "causation_id", null, "operation_id", null, "payload", payload);
        try { return mapper.writeValueAsString(envelope); }
        catch (Exception ex) { throw new IllegalStateException("Cannot serialize audit event", ex); }
    }
    private UUID uuidOrNull(String value) {
        try { return UUID.fromString(value); } catch (RuntimeException ex) { return null; }
    }
    // BEFORE_COMMIT inserts using the same DB transaction as the business change and audit_log.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void record(ReferenceAuditEvent event) {
        jdbc.update("INSERT INTO reference_audit_outbox(event_id, topic, message_key, payload) VALUES (?, ?, ?, ?)",
                event.eventId(), topic, event.entityId().toString(), serialize(event));
    }
}
