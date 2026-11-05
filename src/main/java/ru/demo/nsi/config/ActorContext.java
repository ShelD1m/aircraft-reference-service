package ru.demo.nsi.config;

import ru.demo.nsi.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Заголовки ниже ТОЛЬКО для изолированной учебной демонстрации; не для реальной системы.
 */
@Component
public class ActorContext {
    private final HttpServletRequest request;

    public ActorContext(HttpServletRequest request) {
        this.request = request;
    }

    public String role() {
        String x = request.getHeader("X-Demo-Role");
        return x == null ? "TECHNICIAN" : x.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public String actorId() {
        String actor = request.getHeader("X-Demo-User");
        return actor == null || actor.isBlank() ? "demo-" + role().toLowerCase(java.util.Locale.ROOT) : actor;
    }

    public String requestId() {
        String x = request.getHeader("X-Request-ID");
        return x == null || x.isBlank() ? null : x;
    }

    public void read() {
        require("TECHNICIAN", "REFERENCE_EDITOR", "SUPERVISOR");
    }

    public void editor() {
        require("REFERENCE_EDITOR", "SUPERVISOR");
    }

    public void supervisor() {
        require("SUPERVISOR");
    }

    public void require(String... roles) {
        for (String r : roles) if (r.equals(role())) return;
        throw new ApiException(HttpStatus.FORBIDDEN, "Недостаточно прав. Учебный заголовок X-Demo-Role=" + role());
    }
}
