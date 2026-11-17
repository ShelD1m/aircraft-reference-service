package ru.demo.nsi.integration.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.demo.nsi.api.ApiException;
import ru.demo.nsi.service.NsiService;
import java.util.*;

@Service
public class CoreTemplateService {
    private final NsiService nsi;
    private final String procedure;
    private final String minutePolicy;
    private final long ttl;
    public CoreTemplateService(NsiService nsi,
            @Value("${integrations.core.procedure-code:PRE_DEPARTURE}") String procedure,
            @Value("${integrations.core.minute-policy:exact}") String minutePolicy,
            @Value("${integrations.core.cache-ttl-seconds:300}") long ttl) {
        this.nsi = nsi;
        this.procedure = procedure;
        this.minutePolicy = minutePolicy;
        this.ttl = ttl;
        if (ttl < 0 || !Set.of("exact", "ceil").contains(minutePolicy))
            throw new IllegalArgumentException("Invalid CORE TTL or minute-policy (exact/ceil)");
    }
    public record Item(int stepNumber, String title, int timeNormMinutes, List<Integer> allowedNextSteps) {}
    public record Template(String aircraftTypeId, UUID templateId, UUID templateVersionId,
                           List<Item> items, long cacheTtlSeconds) {}

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Template resolve(String code) {
        if (!code.matches("[A-Z0-9_-]{1,100}")) throw invalid("Use a registered uppercase aircraft type code");
        Map<String, Object> source = nsi.resolve(code, procedure);
        List<Map<String, Object>> stages = new ArrayList<>((List<Map<String, Object>>) source.get("stages"));
        if (stages.isEmpty()) throw invalid("Published template has no stages");
        stages.sort(Comparator.comparingInt(s -> ((Number) s.get("sequence_number")).intValue()));
        Set<Integer> numbers = new HashSet<>();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            var s = stages.get(i);
            int number = ((Number) s.get("sequence_number")).intValue();
            int seconds = ((Number) s.get("expected_duration_seconds")).intValue();
            if (number < 1 || !numbers.add(number) || seconds < 1) throw invalid("Invalid stage number or duration");
            if (Boolean.FALSE.equals(s.get("mandatory"))) throw invalid("CORE does not support optional stages");
            if (minutePolicy.equals("exact") && seconds % 60 != 0)
                throw invalid("Duration is not whole minutes; agree minute-policy=ceil before enabling rounding");
            if (!"TECHNICIAN".equals(s.get("required_role")))
                throw invalid("CORE cannot enforce this stage role; only TECHNICIAN is supported");
            String title = Objects.toString(s.get("description"), "").trim();
            if (title.isEmpty()) title = Objects.toString(s.get("operation_code"), "");
            if (title.isBlank() || title.length() > 300) throw invalid("Stage title must contain 1-300 characters");
            List<Integer> next = i + 1 < stages.size()
                    ? List.of(((Number) stages.get(i + 1).get("sequence_number")).intValue()) : List.of();
            items.add(new Item(number, title, (int) ((seconds + 59L) / 60), next));
        }
        return new Template(code, (UUID) source.get("template_id"),
                (UUID) source.get("reference_version_id"), List.copyOf(items), ttl);
    }
    private ApiException invalid(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
}
