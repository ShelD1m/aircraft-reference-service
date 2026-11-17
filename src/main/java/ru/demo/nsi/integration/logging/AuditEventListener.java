package ru.demo.nsi.integration.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Writes metadata to stdout after commit. Durable external delivery uses the outbox.
 */
@Component
public class AuditEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(AuditEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAudit(ReferenceAuditEvent event) {
        LOG.info("nsi_audit eventId={} eventType={} entityType={} entityId={} actorId={} requestId={}",
                event.eventId(), event.eventType(), event.entityType(), event.entityId(),
                event.userId(), event.requestId());
    }
}
