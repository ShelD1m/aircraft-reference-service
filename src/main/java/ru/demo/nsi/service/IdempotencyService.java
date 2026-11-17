package ru.demo.nsi.service;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.demo.nsi.api.ApiException;
import ru.demo.nsi.config.ActorContext;
import ru.demo.nsi.domain.IdempotencyRequest;
import ru.demo.nsi.repository.IdempotencyRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyService {
    private final IdempotencyRepository repo;
    private final HttpServletRequest request;
    private final ActorContext actor;
    private final ObjectMapper mapper;

    public IdempotencyService(IdempotencyRepository repo, HttpServletRequest request, ActorContext actor, ObjectMapper mapper) {
        this.repo = repo;
        this.request = request;
        this.actor = actor;
        this.mapper = mapper;
    }

    @Transactional
    public ResponseEntity<Object> run(Object body, int successStatus, Supplier<Object> execute) {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank() || key.length() > 200)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Передайте заголовок Idempotency-Key длиной 1–200 символов");
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String actorId = actor.actorId();
        String signature;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(body));
            signature = HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        var previous = repo.findByActorIdAndHttpMethodAndRequestUriAndIdempotencyKey(actorId, method, uri, key);
        if (previous.isPresent()) {
            var item = previous.get();
            if (!signature.equals(item.requestHash))
                throw new ApiException(HttpStatus.CONFLICT, "Idempotency-Key уже применён к другому содержимому запроса");
            Object result = item.responseBody == null || item.responseBody.isNull() ? null : item.responseBody;
            return ResponseEntity.status(item.responseStatus).body(result);
        }
        Object response = execute.get();
        IdempotencyRequest saved = new IdempotencyRequest();
        saved.id = UUID.randomUUID();
        saved.actorId = actorId;
        saved.httpMethod = method;
        saved.requestUri = uri;
        saved.idempotencyKey = key;
        saved.requestHash = signature;
        saved.responseStatus = successStatus;
        saved.responseBody = mapper.valueToTree(response);
        saved.createdAt = Instant.now();
        repo.save(saved);
        return ResponseEntity.status(successStatus).body(response);
    }
}
