# Матрица API прототипа

Префикс всех методов: `/api/v1/references`.

## Внешний API: 12 методов

| № | Метод | Путь |
|---|---|---|
| E-01 | GET | `/aircraft-types` |
| E-02 | GET | `/aircraft-types/{id}` |
| E-03 | GET | `/aircraft` |
| E-04 | GET | `/aircraft/{id}` |
| E-05 | GET | `/operation-types` |
| E-06 | GET | `/operation-types/{id}` |
| E-07 | GET | `/procedure-templates` |
| E-08 | GET | `/procedure-templates/{id}` |
| E-09 | GET | `/procedure-templates/resolve` |
| E-10 | GET | `/snapshots/{referenceVersionId}` |
| E-11 | GET | `/sync/manifest` |
| E-12 | GET | `/versions/{versionId}/status` |

## Внутренний API: 23 метода

| № | Метод | Путь |
|---|---|---|
| I-01 | GET | `/directories` |
| I-02 | POST | `/directories` |
| I-03 | GET | `/directories/{id}` |
| I-04 | PATCH | `/directories/{id}` |
| I-05 | GET | `/directories/{id}/versions` |
| I-06 | POST | `/directories/{id}/versions` |
| I-07 | GET | `/directories/{id}/versions/{versionId}` |
| I-08 | GET | `/directories/{id}/versions/{versionId}/records` |
| I-09 | GET | `/directories/{id}/versions/{versionId}/records/{recordId}` |
| I-10 | POST | `/directories/{id}/versions/{versionId}/records` |
| I-11 | PATCH | `/directories/{id}/versions/{versionId}/records/{recordId}` |
| I-12 | DELETE | `/directories/{id}/versions/{versionId}/records/{recordId}` |
| I-13 | POST | `/directories/{id}/versions/{versionId}/records/{recordId}/stages` |
| I-14 | PATCH | `/directories/{id}/versions/{versionId}/records/{recordId}/stages/{stageId}` |
| I-15 | DELETE | `/directories/{id}/versions/{versionId}/records/{recordId}/stages/{stageId}` |
| I-16 | POST | `/directories/{id}/versions/{versionId}/submit` |
| I-17 | POST | `/directories/{id}/versions/{versionId}/approve` |
| I-18 | POST | `/directories/{id}/versions/{versionId}/reject` |
| I-19 | POST | `/directories/{id}/versions/{versionId}/archive` |
| I-20 | GET | `/directories/{id}/versions/diff` |
| I-21 | POST | `/directories/{id}/versions/{versionId}/import` |
| I-22 | GET | `/audit` |
| I-23 | GET | `/audit/{auditId}` |

Методы сопоставлены с редакцией 0.9 Word-спецификации. Реализация демонстрационная; часть ограничений промышленного контракта пока упрощена (см. README).
