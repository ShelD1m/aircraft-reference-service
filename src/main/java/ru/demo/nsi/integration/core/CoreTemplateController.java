package ru.demo.nsi.integration.core;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.demo.nsi.config.ActorContext;
import java.time.Duration;

@RestController
@RequestMapping("/reference/aircraft-types")
public class CoreTemplateController {
    private final CoreTemplateService service;
    private final ActorContext actor;
    private final boolean requireAuth;
    public CoreTemplateController(CoreTemplateService service, ActorContext actor,
            @Value("${integrations.core.require-auth:false}") boolean requireAuth,
            @Value("${integrations.auth.enabled:false}") boolean authEnabled) {
        if (requireAuth && !authEnabled) throw new IllegalArgumentException("CORE_REQUIRE_AUTH requires AUTH_ENABLED");
        this.service = service;
        this.actor = actor;
        this.requireAuth = requireAuth;
    }
    @Operation(summary = "CORE: published sequential checklist template")
    @GetMapping("/{aircraftTypeId}/checklist-template")
    public ResponseEntity<CoreTemplateService.Template> template(@PathVariable String aircraftTypeId) {
        if (requireAuth) actor.read();
        var template = service.resolve(aircraftTypeId);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(template.cacheTtlSeconds())))
                .body(template);
    }
}
