package ru.demo.nsi.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reference_versions")
public class ReferenceVersion {
    public enum Status {DRAFT, REVIEW, APPROVED, ARCHIVED}

    @Id
    public UUID id;
    @Column(name = "directory_id", nullable = false)
    public UUID directoryId;
    @Column(name = "version_number")
    public Integer versionNumber;
    @Enumerated(EnumType.STRING)
    public Status status;
    @Column(name = "base_version_id")
    public UUID baseVersionId;
    @Column(name = "change_reason", columnDefinition = "text")
    public String changeReason;
    @Column(name = "created_by")
    public String createdBy;
    @Column(name = "approved_by")
    public String approvedBy;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
    @Column(name = "published_at")
    public Instant publishedAt;
}
