package ru.demo.nsi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

import java.util.stream.Stream;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Модуль Справочники (НСИ) — DEMO", version = "0.1.0",
        description = "Учебный прототип. Все данные вымышленные. В Authorize введите SUPERVISOR, REFERENCE_EDITOR или TECHNICIAN.\nИзменяющие запросы требуют Idempotency-Key."),
        security = @SecurityRequirement(name = "X-Demo-Role"))
@SecurityScheme(name = "X-Demo-Role", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-Demo-Role",
        description = "Только для демонстрации: SUPERVISOR / REFERENCE_EDITOR / TECHNICIAN. Не использовать как настоящую авторизацию.")
public class SwaggerConfig {
    @Bean
    OpenApiCustomizer idempotencyHeader() {
        return api -> api.getPaths().forEach((path, item) -> {
            Stream.of(item.getPost(), item.getPatch(), item.getDelete()).filter(java.util.Objects::nonNull)
                    .forEach(op -> op.addParametersItem(new Parameter().name("Idempotency-Key").in("header")
                            .required(true).description("Уникальный ключ запроса; повтор возвращает исходный ответ")
                            .schema(new StringSchema())));
        });
    }
}
