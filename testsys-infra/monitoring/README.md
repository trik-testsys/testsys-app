# Модуль `testsys-infra:monitoring`

Модуль `testsys-infra:monitoring` учитывает технические сбои и владеет каталогом собственных метрик TestSys:
именами, метками и их допустимыми значениями. Документ предназначен для тех, кто добавляет метрику или учёт сбоя
в модуль системы. Требования к метрикам — в разделе `testsys.dev.metric` в [features.md](../../docs/domain/features.md),
HTTP-метрики, реестр метрик и сервер экспорта — в разделе [«Метрики»](../../testsys-web/app/README.md#метрики)
app/README.md. Дашборды Grafana по этим метрикам описаны в [deploy/monitoring/README.md](../../deploy/monitoring/README.md).

Если меняется имя метрики, метка или перечень её допустимых значений, обновите и дашборды
в `deploy/monitoring/grafana/dashboards/`.

## Компоненты

| Компонент | Назначение |
|-----------|------------|
| `api/TechnicalFailures` | Счётчик технических сбоев с защитой от повторного учёта одного исключения |
| `api/TestSysMetrics` | Имена метрик и ключи меток из каталога ниже |
| `api/MonitoringConfiguration` | Бин `TechnicalFailures` на реестре метрик контекста |

Модуль зависит только от Micrometer (`micrometer-core`) и Spring Context. Инфраструктурные модули, которые учитывают
сбои, получают `TechnicalFailures` необязательным параметром. Без него они используют `TechnicalFailures.NONE`: сбои принимаются,
но никуда не экспортируются. Так же работает `MonitoringConfiguration` в контексте без `MeterRegistry`.

## Учёт технических сбоев

Технический сбой учитывается вызовом `TechnicalFailures.record(source, stage, failure)` там, где обработка сбоя
заканчивается: исключение записано в лог и дальше не пробрасывается, превращено в результат или повтор.
Если код пробрасывает исключение дальше, сбой учитывает следующий обработчик. Исключение из этого правила — граница
прокси-сервисов приложения: она учитывает исключение и пробрасывает его, а обработчики интерфейса повторно его
не считают.

- `source` и `stage` — строчные слова через подчёркивание (`[a-z][a-z0-9_]*`); другое значение отклоняется
  `IllegalArgumentException`. Допустимые значения перечислены в таблице ниже.
- Метка `exception` — простое имя класса исключения. Для обработанного отказа без исключения, например отклонённого
  ответа узла, передаётся `failure = null`, и метка получает значение `none`.
- Одно исключение учитывается один раз. `TechnicalFailures` помнит учтённые исключения по ссылке вместе с цепочкой
  их причин и не мешает сборщику мусора их удалить. Повторно не считаются учтённое исключение, его причина
  и исключение, среди причин которого есть учтённое. Общая более глубокая причина двух разных исключений не мешает
  их учёту. Каждая неудачная попытка с новым исключением считается отдельно.

| `source` | `stage` | Где учитывается |
|----------|---------|-----------------|
| `main` | `service`, `event`, `navigation` | Граница прокси-сервисов, обработчик событий и экран ошибки навигации основного интерфейса |
| `service` | `event`, `navigation` | Обработчик событий и экран ошибки навигации служебного интерфейса |
| `grading` | `prepare`, `send`, `node_unavailable`, `node_deadline`, `node_error`, `invalid_result`, `save`, `subscriber`, `work`, `rejected` | `GradingCoordinator` в [grpc](../grpc/README.md#метрики) |
| `nodes` | `start`, `write`, `record_mismatch` | `GradingNodeRegistryCoordinator` в [grpc](../grpc/README.md#метрики) |
| `settings` | `prepare`, `write` | `SettingsCoordinator` в [settings](../settings/README.md#применение-и-ошибки) |
| `mail` | `after_commit` | `SmtpUserMailSender` в [mail](../mail/README.md#компоненты) |
| `validation` | `processing` | Диспетчер запросов валидации, см. [testsys-operation/README.md](../../testsys-operation/README.md) |

Источники `main` и `service` описаны в разделе [«Метрики»](../../testsys-web/app/README.md#метрики) app/README.md.

## Каталог метрик

Prometheus получает имена с подчёркиваниями вместо точек; к имени счётчика добавляется `_total`, например
`testsys_technical_failures_total`.

| Метрика | Тип | Метки и значения |
|---------|-----|------------------|
| `testsys.technical.failures` | Счётчик | `source`, `stage` — из таблицы выше; `exception` — простое имя класса или `none` |
| `testsys.operation.errors` | Счётчик | `operation` — `<Сервис>.<метод>` прокси-сервиса; `kind` — `not_found`, `access_denied`, `missing_role`, `resource_access` или `other`; `error` — простое имя класса ошибки операции |
| `testsys.transaction.retries` | Счётчик | Без меток |
| `testsys.grading.checks` | Счётчик | `result` — `verdict`, `failure` или `timeout` |
| `testsys.grading.queue` | Значение | `state` — `preparing`, `waiting`, `grading` или `saving` |
| `testsys.grading.nodes` | Значение | `participation` — `active` или `inactive`; `status` — `unknown`, `available` или `unavailable` |
| `testsys.grading.nodes.queued` | Значение | Без меток |
| `testsys.grading.nodes.capacity` | Значение | Без меток |

Метка `kind` соответствует общим типам ошибок из раздела
[«Общие типы ошибок»](../../testsys-operation/README.md#общие-типы-ошибок) testsys-operation/README.md:
`EntityNotExistsError`, `AccessDeniedError`, `MissedRequiredRoleError`, `ResourceAccessError`, остальные — `other`.
Ошибки операций учитывает приложение, повторы транзакций — модуль [database](../database/README.md#повтор-транзакций),
метрики проверки — [grpc](../grpc/README.md#метрики).

## Запрещённые значения меток

Значение метки берётся только из фиксированного перечня этого каталога, из шаблона маршрута или из простого имени
класса. В метки не попадают:

- идентификаторы Пользователей, Посылок и других сущностей;
- Коды-доступа, код служебного входа и адреса почты;
- адреса Проверяющих узлов;
- адреса страниц со значениями параметров и строки запроса;
- тексты исключений и описания ошибок.
