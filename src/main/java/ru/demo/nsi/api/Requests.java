package ru.demo.nsi.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.*;

public final class Requests {
    private Requests() {
    }

    public record DirectoryCreate(@NotBlank String code, @NotBlank String kind, @NotBlank String name,
                                  String description) {
    }

    public record DirectoryPatch(String name, String description) {
    }

    public record VersionCreate(UUID sourceVersionId, String changeReason) {
    }

    public record RecordCreate(@NotBlank String code, @NotBlank String name, String description, JsonNode data) {
    }

    public record RecordPatch(String code, String name, String description, JsonNode data) {
    }

    public record StageCreate(@NotNull @Min(1) Integer sequenceNumber, @NotBlank String operationCode,
                              String description, @NotNull @Min(1) Integer expectedDurationSeconds, String requiredRole,
                              Boolean mandatory) {
    }

    public record StagePatch(Integer sequenceNumber, String operationCode, String description,
                             Integer expectedDurationSeconds, String requiredRole, Boolean mandatory) {
    }

    public record Reason(String reason) {
    }

    public record ImportRequest(Boolean dryRun, String mode, @NotNull List<@Valid RecordCreate> items) {
    }
}
