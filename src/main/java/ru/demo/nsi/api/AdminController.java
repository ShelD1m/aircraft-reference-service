package ru.demo.nsi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.demo.nsi.api.Requests.*;
import ru.demo.nsi.config.ActorContext;
import ru.demo.nsi.service.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/references")
@Tag(name = "2. Внутреннее API (редактор и супервайзер)", description = "Версии, черновики, CRUD, согласование, импорт")
public class AdminController {
    private final NsiService nsi;
    private final ActorContext actor;
    private final IdempotencyService idem;

    public AdminController(NsiService nsi, ActorContext actor, IdempotencyService idem) {
        this.nsi = nsi;
        this.actor = actor;
        this.idem = idem;
    }

    @Operation(summary = "I-01: Список справочников")
    @GetMapping("/directories")
    public Object directories(@RequestParam(required = false) String search, @RequestParam(required = false) String kind,
                              @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset) {
        actor.read();
        return nsi.directories(search, kind, limit, offset);
    }

    @Operation(summary = "I-02: Создать новый справочник и черновик", description = "Роль SUPERVISOR")
    @PostMapping("/directories")
    public ResponseEntity<Object> createDirectory(@Valid @RequestBody DirectoryCreate req) {
        actor.supervisor();
        return idem.run(req, 201, () -> nsi.createDirectory(req));
    }

    @Operation(summary = "I-03: Метаданные справочника (ETag)")
    @GetMapping("/directories/{id}")
    public ResponseEntity<Object> directory(@PathVariable UUID id) {
        actor.read();
        Map<String, Object> item = nsi.directoryDetails(id);
        return ResponseEntity.ok().eTag("\"" + item.get("revision") + "\"").body(item);
    }

    @Operation(summary = "I-04: Изменить название/описание справочника", description = "Требуются If-Match из предыдущего GET, Idempotency-Key и роль SUPERVISOR")
    @PatchMapping("/directories/{id}")
    public ResponseEntity<Object> patchDirectory(@PathVariable UUID id,
                                                 @RequestHeader("If-Match") String ifMatch, @RequestBody DirectoryPatch req) {
        actor.supervisor();
        return idem.run(List.of(req, ifMatch), 200, () -> nsi.patchDirectory(id, req, ifMatch));
    }

    @Operation(summary = "I-05: История версий")
    @GetMapping("/directories/{id}/versions")
    public Object versions(@PathVariable UUID id, @RequestParam(required = false) String status,
                           @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset) {
        actor.editor();
        return nsi.versions(id, limit, offset, status);
    }

    @Operation(summary = "I-06: Создать DRAFT, скопировав APPROVED")
    @PostMapping("/directories/{id}/versions")
    public ResponseEntity<Object> createVersion(@PathVariable UUID id, @RequestBody(required = false) VersionCreate req) {
        actor.editor();
        return idem.run(req, 201, () -> nsi.createVersion(id, req));
    }

    @Operation(summary = "I-07: Метаданные точной версии")
    @GetMapping("/directories/{id}/versions/{versionId}")
    public Object version(@PathVariable UUID id, @PathVariable UUID versionId) {
        actor.editor();
        return nsi.versionDetails(id, versionId);
    }

    @Operation(summary = "I-20: Сравнение двух версий")
    @GetMapping("/directories/{id}/versions/diff")
    public Object diff(@PathVariable UUID id, @RequestParam(name = "from_version_id") UUID from,
                       @RequestParam(name = "to_version_id") UUID to) {
        actor.editor();
        return nsi.diff(id, from, to);
    }

    @Operation(summary = "I-08: Записи конкретной версии")
    @GetMapping("/directories/{id}/versions/{versionId}/records")
    public Object records(@PathVariable UUID id, @PathVariable UUID versionId, @RequestParam(required = false) String search,
                          @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset) {
        actor.editor();
        return nsi.records(id, versionId, search, limit, offset);
    }

    @Operation(summary = "I-09: Запись конкретной версии")
    @GetMapping("/directories/{id}/versions/{versionId}/records/{recordId}")
    public Object record(@PathVariable UUID id, @PathVariable UUID versionId, @PathVariable UUID recordId) {
        actor.editor();
        return nsi.record(id, versionId, recordId);
    }

    @Operation(summary = "I-10: Добавить запись в DRAFT")
    @PostMapping("/directories/{id}/versions/{versionId}/records")
    public ResponseEntity<Object> createRecord(@PathVariable UUID id, @PathVariable UUID versionId, @Valid @RequestBody RecordCreate req) {
        actor.editor();
        return idem.run(req, 201, () -> nsi.createRecord(id, versionId, req));
    }

    @Operation(summary = "I-11: Изменить запись в DRAFT")
    @PatchMapping("/directories/{id}/versions/{versionId}/records/{recordId}")
    public ResponseEntity<Object> patchRecord(@PathVariable UUID id, @PathVariable UUID versionId, @PathVariable UUID recordId,
                                              @RequestBody RecordPatch req) {
        actor.editor();
        return idem.run(req, 200, () -> nsi.patchRecord(id, versionId, recordId, req));
    }

    @Operation(summary = "I-12: Удалить запись из DRAFT")
    @DeleteMapping("/directories/{id}/versions/{versionId}/records/{recordId}")
    public ResponseEntity<Object> deleteRecord(@PathVariable UUID id, @PathVariable UUID versionId, @PathVariable UUID recordId) {
        actor.editor();
        return idem.run(null, 204, () -> nsi.deleteRecord(id, versionId, recordId));
    }

    @Operation(summary = "I-13: Добавить этап процедуры в DRAFT")
    @PostMapping("/directories/{id}/versions/{versionId}/records/{recordId}/stages")
    public ResponseEntity<Object> createStage(@PathVariable UUID id, @PathVariable UUID versionId, @PathVariable UUID recordId,
                                              @Valid @RequestBody StageCreate req) {
        actor.editor();
        return idem.run(req, 201, () -> nsi.createStage(id, versionId, recordId, req));
    }

    @Operation(summary = "I-14: Изменить этап процедуры в DRAFT")
    @PatchMapping("/directories/{id}/versions/{versionId}/records/{recordId}/stages/{stageId}")
    public ResponseEntity<Object> patchStage(@PathVariable UUID id, @PathVariable UUID versionId,
                                             @PathVariable UUID recordId, @PathVariable UUID stageId, @RequestBody StagePatch req) {
        actor.editor();
        return idem.run(req, 200, () -> nsi.patchStage(id, versionId, recordId, stageId, req));
    }

    @Operation(summary = "I-15: Удалить этап процедуры из DRAFT")
    @DeleteMapping("/directories/{id}/versions/{versionId}/records/{recordId}/stages/{stageId}")
    public ResponseEntity<Object> deleteStage(@PathVariable UUID id, @PathVariable UUID versionId,
                                              @PathVariable UUID recordId, @PathVariable UUID stageId) {
        actor.editor();
        return idem.run(null, 204, () -> nsi.deleteStage(id, versionId, recordId, stageId));
    }

    @Operation(summary = "I-16: Отправить DRAFT на REVIEW")
    @PostMapping("/directories/{id}/versions/{versionId}/submit")
    public ResponseEntity<Object> submit(@PathVariable UUID id, @PathVariable UUID versionId) {
        actor.editor();
        return idem.run(null, 200, () -> nsi.submit(id, versionId));
    }

    @Operation(summary = "I-17: Утвердить REVIEW; архивировать предыдущую версию")
    @PostMapping("/directories/{id}/versions/{versionId}/approve")
    public ResponseEntity<Object> approve(@PathVariable UUID id, @PathVariable UUID versionId) {
        actor.supervisor();
        return idem.run(null, 200, () -> nsi.approve(id, versionId));
    }

    @Operation(summary = "I-18: Отклонить REVIEW с причиной")
    @PostMapping("/directories/{id}/versions/{versionId}/reject")
    public ResponseEntity<Object> reject(@PathVariable UUID id, @PathVariable UUID versionId, @RequestBody Reason reason) {
        actor.supervisor();
        return idem.run(reason, 200, () -> nsi.reject(id, versionId, reason.reason()));
    }

    @Operation(summary = "I-19: Архивировать APPROVED вручную")
    @PostMapping("/directories/{id}/versions/{versionId}/archive")
    public ResponseEntity<Object> archive(@PathVariable UUID id, @PathVariable UUID versionId,
                                          @RequestBody(required = false) Reason reason) {
        actor.supervisor();
        return idem.run(reason, 200, () -> nsi.archive(id, versionId, reason == null ? null : reason.reason()));
    }

    @Operation(summary = "I-21: Массовый импорт JSON (dry_run или UPSERT)")
    @PostMapping("/directories/{id}/versions/{versionId}/import")
    public ResponseEntity<Object> importRecords(@PathVariable UUID id, @PathVariable UUID versionId, @Valid @RequestBody ImportRequest req) {
        actor.editor();
        return idem.run(req, 200, () -> nsi.importRecords(id, versionId, req));
    }

    @Operation(summary = "I-22: Журнал аудита — только SUPERVISOR")
    @GetMapping("/audit")
    public Object audits(@RequestParam(name = "user_id", required = false) String user,
                         @RequestParam(name = "entity_type", required = false) String entity,
                         @RequestParam(name = "entity_id", required = false) UUID entityId,
                         @RequestParam(defaultValue = "50") int limit, @RequestParam(defaultValue = "0") int offset) {
        actor.supervisor();
        return nsi.audits(user, entity, entityId, limit, offset);
    }

    @Operation(summary = "I-23: Конкретное событие аудита")
    @GetMapping("/audit/{auditId}")
    public Object audit(@PathVariable UUID auditId) {
        actor.supervisor();
        return nsi.audit(auditId);
    }
}
