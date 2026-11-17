# Интеграция справочников

Изменения ограничены aircraft-reference-service-main.

## CORE

GET /reference/aircraft-types/A320/checklist-template — совместимость с текущим клиентом CORE.
aircraftTypeId — стабильный регистрозависимый код, не UUID.
Возвращается только актуальный APPROVED-шаблон CORE_PROCEDURE_CODE (по умолчанию PRE_DEPARTURE).

Ответ: aircraftTypeId, templateId (logical_id), templateVersionId (UUID версии справочника),
items[{stepNumber,title,timeNormMinutes,allowedNextSteps}], cacheTtlSeconds.
Название этапа — description, при отсутствии — operation_code.
Переходы последовательные: следующий sequence_number, у последнего [].
Необязательные этапы и роли кроме TECHNICIAN отклоняются, поскольку CORE не сохраняет их семантику.
Это не заменяет проверку прав исполнителей на стороне CORE.

CORE_MINUTE_POLICY=exact требует время, кратное 60 секундам.
После согласования можно включить ceil: округление вверх (61 секунда → 2 минуты).
Пустой шаблон, повтор номера, неподдерживаемый этап или неоднозначный выбор — 409.
Неизвестный/неактивный тип, отсутствие опубликованной процедуры — 404.
Ошибки: application/problem+json (status, title, detail, instance).
CORE может преобразовывать HTTP-ошибку зависимости в общий 503 REFERENCE_UNAVAILABLE.
Таймауты и повторы входящего запроса задаёт клиент CORE.

TTL 300 секунд; 0 отключает кэш. Публикация не сбрасывает кэш CORE:
предыдущая версия может использоваться ещё до TTL. Существующие чек-листы сохраняют снимки.
UUID версии справочника общий для всех её шаблонов; logical_id шаблона сохраняется.
Откат — публикация новой версии с восстановленным содержимым, без изменения истории.

CORE_REQUIRE_AUTH=false: только эта ручка не проверяет пользователя, поскольку CORE не передаёт токен.
Разместите endpoint в согласованной внутренней сети; путь /reference сам по себе сеть не ограничивает.
CORE_REQUIRE_AUTH=true требует AUTH_ENABLED=true и Bearer от клиента.
Без изменений CORE защищённый режим несовместим с текущим клиентом.

## AUTH

AUTH_BASE_URL — прямой адрес auth-service (локально localhost:8081).
POST /api/auth/verify: {"token":"<user access token>"}.
Ответ: {"userId":"<UUID>","role":"ADMIN|SUPERVISOR|TECHNICIAN"}.
POST /api/auth/permissions/check: {"userId":"<UUID>","permission":"reference.read"}.
Ответ: {"allowed":true}.
AUTH_SERVICE_TOKEN при наличии передаётся обоим запросам отдельным Authorization: Bearer.
Пользовательский токен передаётся только в body verify; сессионная cookie BFF здесь не поддерживается.
Connect timeout 2 секунды, read timeout 3 секунды; verify — один раз на входящий запрос.

При AUTH_ENABLED=true права определяет AUTH; роль сама по себе не даёт доступ.
Демо-заголовки игнорируются. allowed=false/403 проверки прав → 403.
Невалидный пользовательский токен → 401; ошибка/неполный ответ AUTH → 502; недоступность → 503.
Обхода AUTH при сбое нет. Неверный сервисный credential (401) пока неотличим от неверного
пользовательского токена — требуется договориться об ответах AUTH.

Необходимые permissions:
- reference.read — чтение;
- reference.record.write — изменение данных, создание/редактирование черновиков;
- reference.directory.create — создание аэропортов, типов ВС, видов операций;
- reference.version.submit — отправка версии на согласование;
- reference.version.approve — утверждение и отклонение;
- reference.audit.read — просмотр аудита.

В предоставленном AuthService-main BffController.verifyToken и checkPermission бросают
UnsupportedOperationException("TODO"). Клиент реализован по DTO, реальный доступ зависит от команды AUTH.
Профиль integration включает AUTH; обычный запуск и Compose по умолчанию оставляют demo.

## AUDIT LOGGER

Локальный audit_log сохраняется. Каждое новое изменение записывает событие в reference_audit_outbox
до COMMIT в той же транзакции; ошибка записи outbox откатывает изменение.
Outbox заполняется даже при KAFKA_ENABLED=false. Старые audit_log автоматически не переносятся.

KAFKA_ENABLED=true включает publisher. Topic по умолчанию hcc.audit.events.v1,
producer_service=REFERENCE, event_type=audit.log.
Envelope следует структуре схемы hcc-core-contract/schemas/events/audit.log.schema.json.
Общий JAR не требуется. Topic должен существовать; создание и ACL — задача инфраструктуры/Logger.
Message key — entityId, event_id совпадает с UUID локального audit_log.
payload.data содержит только метаданные, без старых/новых значений и токенов.
actor_user_id — UUID; в demo null, demo actor остаётся в data.actorId.
operation_id и causation_id=null: Idempotency-Key не является UUID бизнес-операции CORE.

At-least-once: published_at ставится после broker ack (acks=all).
Повторы с увеличением задержки до 300 секунд; FOR UPDATE SKIP LOCKED для нескольких экземпляров.
Сбой после broker ack до DB commit может дать дубликат: Logger обязан дедуплицировать по event_id.
Сбой Kafka не отменяет уже сохранённое изменение.
Опубликованные outbox удаляются спустя 14 дней (03:30 UTC); pending не удаляются.
Контролируйте накопление pending, если Kafka долго выключена.

Настройки: KAFKA_BOOTSTRAP_SERVERS, KAFKA_AUDIT_TOPIC, KAFKA_OUTBOX_BATCH_SIZE=20,
KAFKA_OUTBOX_POLL_INTERVAL_MS=1000, KAFKA_SEND_TIMEOUT_MS=5000.
Retention и cron: integrations.logging.retention-days / cleanup-cron.
SASL/SSL задаются штатными настройками SPRING_KAFKA_* по параметрам инфраструктуры.

## Корреляция

X-Correlation-Id — UUID: принимается или генерируется, возвращается в ответе и записывается в envelope.
X-Request-ID сохраняется для совместимости. Оба ID передаются в запросы AUTH.
Клиент CORE должен передавать X-Correlation-Id для единой сквозной цепочки между сервисами.

## Уточнения командам

CORE: подтвердить PRE_DEPARTURE, последовательные переходы, ограничения mandatory/TECHNICIAN,
минуты exact/ceil, TTL=300, внутренний доступ без токена, передачу X-Correlation-Id.
AUTH: реализовать verify/permissions/check; подтвердить userId/role/allowed, permissions,
сервисный credential и способ передачи access token от BFF.
OIDC/JWKS из документа CORE — отдельная задача AUTH/CORE.
Logger: подтвердить REFERENCE producer, topic/JSON Schema, nullable поля, entityId как key;
предоставить bootstrap servers, producer ACL, SASL/SSL, подтверждение дедупликации event_id.
Инфраструктура: DNS, порты и сеть, БД и Kafka, сохранность volume и тестовые credentials.

## Совместная приёмка

1. Поднять PostgreSQL и API: A320 возвращает непустой шаблон и UUID версий.
2. CORE создаёт чек-лист со снимком шаблона.
3. Публикация новой версии видна после TTL; прежний чек-лист неизменен.
4. AUTH: проверить успех, 401, 403 и отказ при недоступности.
5. Изменение справочника создаёт audit_log и outbox атомарно.
6. Kafka получает событие; Logger сохраняет тот же event_id.
7. Отключить Kafka, изменить справочник, восстановить Kafka: pending отправляется.
8. Повторить event_id: Logger хранит одну запись.

Unit/transaction tests не заменяют совместную приёмку с настоящими сервисами.

