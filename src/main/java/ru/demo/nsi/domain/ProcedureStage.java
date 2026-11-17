package ru.demo.nsi.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "procedure_stages")
public class ProcedureStage {
    @Id
    public UUID id;
    @Column(name = "logical_id")
    public UUID logicalId;
    @Column(name = "record_id")
    public UUID recordId;
    @Column(name = "sequence_number")
    public Integer sequenceNumber;
    @Column(name = "operation_code")
    public String operationCode;
    @Column(columnDefinition = "text")
    public String description;
    @Column(name = "expected_duration_seconds")
    public Integer expectedDurationSeconds;
    @Column(name = "required_role")
    public String requiredRole;
    public Boolean mandatory;
}
