package ru.demo.nsi.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "directories")
public class DirectoryEntity {
    @Id
    public UUID id;
    @Column(nullable = false)
    public String code;
    @Column(nullable = false)
    public String kind;
    @Column(nullable = false)
    public String name;
    @Column(columnDefinition = "text")
    public String description;
    @Version
    public Long revision;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
