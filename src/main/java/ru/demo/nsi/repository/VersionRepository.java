package ru.demo.nsi.repository;

import ru.demo.nsi.domain.ReferenceVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface VersionRepository extends JpaRepository<ReferenceVersion, UUID> {
    List<ReferenceVersion> findByDirectoryIdOrderByVersionNumberDesc(UUID directoryId);

    Optional<ReferenceVersion> findByIdAndDirectoryId(UUID id, UUID directoryId);

    Optional<ReferenceVersion> findByDirectoryIdAndStatus(UUID directoryId, ReferenceVersion.Status status);

    List<ReferenceVersion> findByStatus(ReferenceVersion.Status status);
}
