package ru.demo.nsi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import ru.demo.nsi.integration.logging.*;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(AuditOutboxTest.Config.class)
class AuditOutboxTest {
    @Configuration @EnableTransactionManagement
    static class Config {
        @Bean DataSource dataSource() { return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build(); }
        @Bean JdbcTemplate jdbc(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean PlatformTransactionManager tx(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean AuditOutboxWriter writer(JdbcTemplate jdbc) {
            return new AuditOutboxWriter(jdbc,new ObjectMapper().findAndRegisterModules(),"hcc.audit.events.v1");
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager tx;
    @Autowired ApplicationEventPublisher events;
    @Test void commitsOutboxWithBusinessChangeAndRollsBothBackOnFailure() throws Exception {
        jdbc.execute("CREATE TABLE business_change(id UUID PRIMARY KEY)");
        jdbc.execute("CREATE TABLE reference_audit_outbox(event_id UUID PRIMARY KEY, topic VARCHAR(249), message_key VARCHAR(100), payload CLOB)");
        var transaction = new TransactionTemplate(tx);
        var committed = event();
        transaction.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO business_change VALUES (?)",committed.entityId()); events.publishEvent(committed);
        });
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM reference_audit_outbox",Integer.class));
        var payload=new ObjectMapper().readTree(jdbc.queryForObject("SELECT payload FROM reference_audit_outbox",String.class));
        assertEquals("audit.log",payload.path("event_type").asText());
        assertEquals(committed.eventId().toString(),payload.path("event_id").asText());
        assertTrue(payload.path("payload").path("actor_user_id").isNull());
        assertFalse(payload.toString().contains("token"));
        assertThrows(IllegalStateException.class,()->transaction.executeWithoutResult(status -> {
            var e=event(); jdbc.update("INSERT INTO business_change VALUES (?)",e.entityId());
            events.publishEvent(e); throw new IllegalStateException("rollback");
        }));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM business_change",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM reference_audit_outbox",Integer.class));
        // An insert failure in BEFORE_COMMIT must also roll back the business write.
        assertThrows(Exception.class,()->transaction.executeWithoutResult(status -> {
            var e=event(); jdbc.update("INSERT INTO business_change VALUES (?)",e.entityId());
            events.publishEvent(committed); // duplicate outbox primary key
        }));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM business_change",Integer.class));
    }
    private ReferenceAuditEvent event() {
        return new ReferenceAuditEvent(UUID.randomUUID(),"reference-service","REFERENCE_UPDATE","AIRCRAFT",
                UUID.randomUUID(),null,"demo-supervisor","request-1",Instant.now());
    }
}
