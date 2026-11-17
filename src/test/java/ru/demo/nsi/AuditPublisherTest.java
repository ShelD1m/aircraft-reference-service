package ru.demo.nsi;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.demo.nsi.integration.logging.AuditOutboxPublisher;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AuditPublisherTest {
    @Test @SuppressWarnings("unchecked") void marksPublishedOnlyAfterBrokerAcknowledges() {
        var jdbc=mock(JdbcTemplate.class);
        KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
        UUID id=UUID.randomUUID();
        when(jdbc.queryForList(anyString(),eq(20))).thenReturn(List.of(Map.of(
                "event_id",id,"topic","audit","message_key","entity","payload","{}","attempt_count",0)));
        when(kafka.send("audit","entity","{}")).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        new AuditOutboxPublisher(jdbc,kafka,20,100).publish();
        verify(jdbc).update(contains("SET published_at=now()"),eq(id));
    }
    @Test @SuppressWarnings("unchecked") void keepsFailedEventPendingForRetry() {
        var jdbc=mock(JdbcTemplate.class);
        KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
        UUID id=UUID.randomUUID();
        when(jdbc.queryForList(anyString(),eq(20))).thenReturn(List.of(Map.of(
                "event_id",id,"topic","audit","message_key","entity","payload","{}","attempt_count",2)));
        when(kafka.send("audit","entity","{}")).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        new AuditOutboxPublisher(jdbc,kafka,20,100).publish();
        verify(jdbc).update(contains("attempt_count=attempt_count+1"),eq(4L),eq("ExecutionException"),eq(id));
        verify(jdbc,never()).update(contains("SET published_at=now()"),eq(id));
    }
}
