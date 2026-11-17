package ru.demo.nsi.repository;

import ru.demo.nsi.domain.IdempotencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRequest, UUID> {
    Optional<IdempotencyRequest> findByActorIdAndHttpMethodAndRequestUriAndIdempotencyKey(
            String actorId, String httpMethod, String requestUri, String idempotencyKey);
}
