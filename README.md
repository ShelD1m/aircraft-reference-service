# Aircraft Reference Service — справочники

Java 21, Spring Boot 3.5.16, PostgreSQL 17, JPA, Flyway, Swagger.
Учебные данные и длительности не являются авиационными нормативами.

Сервис содержит аэропорты/стоянки, типы ВС/борта, операции/нормативы и
версионируемые процедуры: DRAFT → REVIEW → APPROVED, предыдущая версия архивируется.

## Docker

Скопируйте .env.example в .env и запустите:

```powershell
docker compose up --build -d
```

Swagger: http://localhost:8082/swagger-ui.html (порт: NSI_HTTP_PORT).
CORE: http://localhost:8082/reference/aircraft-types/A320/checklist-template
Health: http://localhost:8082/actuator/health
PostgreSQL: localhost:5433/nsi. Остановка без удаления данных: docker compose down.

По умолчанию AUTH и отправка в Kafka выключены. Outbox заполняется для последующей отправки.
В демо X-Demo-Role: TECHNICIAN, REFERENCE_EDITOR, SUPERVISOR.
Для POST/PATCH нужен Idempotency-Key.
При AUTH_ENABLED=true демо-заголовки игнорируются.
В предоставленном AuthService-main методы verify/checkPermission пока не реализованы.

## Локально

```powershell
docker compose up -d db
mvn spring-boot:run
```

Обычный локальный профиль использует 8080. Профиль integration — 8082 и AUTH_ENABLED=true:

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=integration'
```

Docker использует AUTH_BASE_URL=http://host.docker.internal:8081; локально — localhost:8081.
Для общей Docker-сети укажите DNS-имена сервисов. Kafka должна рекламировать доступный адрес брокера.

## Проверки

```powershell
mvn test
python smoke_test.py
```

Smoke требует запущенного API на 8082 и создаёт тестовый аэропорт.
NSI_BASE_URL и NSI_BEARER_TOKEN позволяют выбрать адрес и пользовательский access token.

[Маршруты API](docs/API_MATRIX.md).
[Контракты, настройки, ограничения и вопросы командам](docs/INTEGRATION.md).
OpenAPI запущенного сервиса: /v3/api-docs.

