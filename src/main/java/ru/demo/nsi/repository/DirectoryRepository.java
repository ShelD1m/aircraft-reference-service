package ru.demo.nsi.repository;

import ru.demo.nsi.domain.DirectoryEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.*;

public interface DirectoryRepository extends JpaRepository<DirectoryEntity, UUID> {
    Optional<DirectoryEntity> findByCode(String code);

    List<DirectoryEntity> findByKind(String kind);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DirectoryEntity d where d.id = :id")
    Optional<DirectoryEntity> lockById(@Param("id") UUID id);
}
