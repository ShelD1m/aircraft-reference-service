package ru.demo.nsi.integration.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "integrations.logging", name = "enabled", havingValue = "true")
public class AuditOutboxPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(AuditOutboxPublisher.class);
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final int batch;
    private final long timeout;
    @Value("${integrations.logging.retention-days:14}")
    private int retentionDays;
    public AuditOutboxPublisher(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka,
            @Value("${integrations.logging.batch-size:20}") int batch,
            @Value("${integrations.logging.send-timeout-ms:5000}") long timeout) {
        if (batch < 1 || batch > 1000 || timeout < 1) throw new IllegalArgumentException("Invalid outbox settings");
        this.jdbc = jdbc; this.kafka = kafka; this.batch = batch; this.timeout = timeout;
    }
    @Scheduled(cron = "${integrations.logging.cleanup-cron:0 30 3 * * *}", zone = "UTC")
    @Transactional
    public void cleanup() {
        if (retentionDays < 1) throw new IllegalArgumentException("retention-days must be positive");
        jdbc.update("DELETE FROM reference_audit_outbox WHERE published_at < now() - (? * interval '1 day')", retentionDays);
    }
    @Scheduled(fixedDelayString = "${integrations.logging.poll-interval-ms:1000}")
    @Transactional
    public void publish() {
        var rows = jdbc.queryForList("SELECT * FROM reference_audit_outbox WHERE published_at IS NULL " +
                "AND next_attempt_at <= now() ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED", batch);
        for (var row : rows) {
            var id = row.get("event_id");
            try {
                kafka.send((String) row.get("topic"), (String) row.get("message_key"), (String) row.get("payload"))
                        .get(timeout, TimeUnit.MILLISECONDS);
                jdbc.update("UPDATE reference_audit_outbox SET published_at=now(), last_error=NULL WHERE event_id=?", id);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                int attempts = ((Number) row.get("attempt_count")).intValue();
                long delay = Math.min(300, 1L << Math.min(attempts, 9));
                jdbc.update("UPDATE reference_audit_outbox SET attempt_count=attempt_count+1, " +
                        "next_attempt_at=now() + (? * interval '1 second'), last_error=? WHERE event_id=?",
                        delay, ex.getClass().getSimpleName(), id);
                LOG.warn("Audit delivery failed; eventId={} retrySeconds={}", id, delay);
                // Avoid waiting for a whole batch while the broker is down.
                break;
            }
        }
    }
}
