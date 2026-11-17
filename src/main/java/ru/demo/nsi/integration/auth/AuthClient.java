package ru.demo.nsi.integration.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import ru.demo.nsi.api.ApiException;

import java.time.Duration;
import java.util.Map;

/**
 * INTEGRATION TODO — команда «Авторизация».
 * Контракт взят из присланной спецификации: POST /api/auth/verify и
 * POST /api/auth/permissions/check. В MVP компонент НЕ создаётся (enabled=false).
 * Перед включением согласовать Bearer JWT / сессионную cookie от BFF, URL и справочник прав.
 */
@Component
@ConditionalOnProperty(prefix = "integrations.auth", name = "enabled", havingValue = "true")
public class AuthClient {
    private final RestClient rest;

    public AuthClient(RestClient.Builder builder,
                      @Value("${integrations.auth.base-url}") String baseUrl,
                      @Value("${integrations.auth.service-token:}") String serviceToken) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        if (!serviceToken.isBlank()) builder.defaultHeader("Authorization", "Bearer " + serviceToken);
        this.rest = builder.baseUrl(baseUrl).requestFactory(factory).requestInterceptor((request, body, execution) -> {
            String correlation = org.slf4j.MDC.get("correlationId");
            String requestId = org.slf4j.MDC.get("requestId");
            if (correlation != null) request.getHeaders().set("X-Correlation-Id", correlation);
            if (requestId != null) request.getHeaders().set("X-Request-ID", requestId);
            return execution.execute(request, body);
        }).build();
    }

    public Identity verifyBearer(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Не передан Bearer-токен");
        }
        try {
            // TODO(команда Авторизация): удостовериться, что /verify принимает именно JWT, а не BFF-cookie.
            Identity user = rest.post().uri("/api/auth/verify")
                    .body(Map.of("token", header.substring(7).trim()))
                    .retrieve().body(Identity.class);
            if (user != null) {
                try { java.util.UUID.fromString(user.userId()); }
                catch (RuntimeException ex) { throw new ApiException(HttpStatus.BAD_GATEWAY, "AUTH returned an invalid userId"); }
            }
            if (user == null || user.userId() == null || user.userId().isBlank() ||
                    user.role() == null || user.role().isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Сервис авторизации вернул неполную личность");
            }
            return user;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Недействительный токен");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Сервис авторизации вернул ошибку");
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис авторизации недоступен");
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Некорректный ответ сервиса авторизации");
        }
    }

    public boolean hasPermission(String userId, String permission) {
        try {
            // TODO(команда Авторизация): подтвердить имена reference.* в таблице permissions.
            PermissionDecision result = rest.post().uri("/api/auth/permissions/check")
                    .body(Map.of("userId", userId, "permission", permission))
                    .retrieve().body(PermissionDecision.class);
            if (result == null || result.allowed() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Сервис авторизации не вернул allowed");
            }
            return result.allowed();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 403) return false;
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Ошибка проверки прав через сервис авторизации");
        } catch (ResourceAccessException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Сервис авторизации недоступен");
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Некорректный ответ сервиса авторизации");
        }
    }

    public record Identity(@JsonAlias("user_id") String userId, String role) {
    }

    public record PermissionDecision(Boolean allowed) {
    }
}
