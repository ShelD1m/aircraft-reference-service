package ru.demo.nsi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Общий X-Request-ID для НСИ, будущего сервиса логирования и цепочек межсервисных вызовов.
 */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE = RequestCorrelationFilter.class.getName() + ".requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader("X-Request-ID");
        String id = incoming != null && incoming.matches("[a-zA-Z0-9._:-]{1,128}")
                ? incoming : UUID.randomUUID().toString();
        String correlationId;
        try { correlationId = UUID.fromString(request.getHeader("X-Correlation-Id")).toString(); }
        catch (RuntimeException ex) { correlationId = UUID.randomUUID().toString(); }
        request.setAttribute("correlationId", correlationId);
        response.setHeader("X-Correlation-Id", correlationId);
        MDC.put("correlationId", correlationId);
        request.setAttribute(ATTRIBUTE, id);
        response.setHeader("X-Request-ID", id);
        MDC.put("requestId", id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("correlationId");
        }
    }
}
