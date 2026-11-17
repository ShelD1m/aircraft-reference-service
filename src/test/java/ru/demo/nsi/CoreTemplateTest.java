package ru.demo.nsi;
import org.junit.jupiter.api.Test;
import ru.demo.nsi.integration.core.CoreTemplateService;
import ru.demo.nsi.service.NsiService;
import ru.demo.nsi.api.ApiException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoreTemplateTest {
    private final NsiService nsi = mock(NsiService.class);
    private final UUID template = UUID.randomUUID(), version = UUID.randomUUID();
    private Map<String,Object> stage(int n, int seconds, boolean mandatory) {
        return NsiService.obj("sequence_number", n, "expected_duration_seconds", seconds,
                "mandatory", mandatory, "required_role", "TECHNICIAN", "description", "Step " + n, "operation_code", "CHECK");
    }
    private CoreTemplateService service(String policy, List<Map<String,Object>> stages) {
        when(nsi.resolve("A320", "PRE_DEPARTURE")).thenReturn(NsiService.obj(
                "template_id", template, "reference_version_id", version, "stages", stages));
        return new CoreTemplateService(nsi, "PRE_DEPARTURE", policy, 300);
    }
    @Test void mapsPublishedSnapshotAndSequentialNonContiguousSteps() {
        var result = service("exact", List.of(stage(3,120,true),stage(1,60,true))).resolve("A320");
        assertEquals(template,result.templateId()); assertEquals(version,result.templateVersionId());
        assertEquals(1,result.items().getFirst().stepNumber());
        assertEquals(List.of(3),result.items().getFirst().allowedNextSteps());
        assertEquals(List.of(),result.items().getLast().allowedNextSteps());
        assertEquals(2,result.items().getLast().timeNormMinutes());
    }
    @Test void refusesSilentRounding() {
        assertThrows(ApiException.class,()->service("exact",List.of(stage(1,61,true))).resolve("A320"));
        assertEquals(2,service("ceil",List.of(stage(1,61,true))).resolve("A320").items().getFirst().timeNormMinutes());
    }
    @Test void rejectsUnsupportedAndInvalidTemplates() {
        assertThrows(ApiException.class,()->service("exact",List.of()).resolve("A320"));
        assertThrows(ApiException.class,()->service("exact",List.of(stage(1,60,false))).resolve("A320"));
        assertThrows(ApiException.class,()->service("exact",List.of(stage(1,60,true),stage(1,60,true))).resolve("A320"));
    }
}
