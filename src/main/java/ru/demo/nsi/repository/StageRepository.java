package ru.demo.nsi.repository;

import ru.demo.nsi.domain.ProcedureStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface StageRepository extends JpaRepository<ProcedureStage, UUID> {
    List<ProcedureStage> findByRecordIdOrderBySequenceNumber(UUID recordId);

    Optional<ProcedureStage> findByIdAndRecordId(UUID id, UUID recordId);

    Optional<ProcedureStage> findByRecordIdAndSequenceNumber(UUID recordId, Integer sequenceNumber);
}
