package ru.demo.nsi.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "directory_records")
public class DirectoryRecord {
    @Id
    public UUID id;
    @Column(name = "logical_id")
    public UUID logicalId;
    @Column(name = "version_id")
    public UUID versionId;
    public String code;
    public String name;
    @Column(columnDefinition = "text")
    public String description;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public JsonNode data;
    @Column(name = "created_at")
    public Instant createdAt;
}
