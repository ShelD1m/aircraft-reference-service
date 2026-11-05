package ru.demo.nsi.repository;

import ru.demo.nsi.domain.DirectoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RecordRepository extends JpaRepository<DirectoryRecord, UUID> {
    List<DirectoryRecord> findByVersionIdOrderByCode(UUID versionId);

    Optional<DirectoryRecord> findByIdAndVersionId(UUID id, UUID versionId);

    Optional<DirectoryRecord> findByVersionIdAndLogicalId(UUID versionId, UUID logicalId);

    Optional<DirectoryRecord> findByVersionIdAndCode(UUID versionId, String code);
}
