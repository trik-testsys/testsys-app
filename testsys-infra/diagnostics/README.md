# Диагностики Полигонов

Модуль `:testsys-infra:diagnostics` синхронно анализирует XML одного Полигона через порт
[`PolygonDiagnostics`](../../testsys-domain/src/main/kotlin/tech/testsys/domain/contract/PolygonDiagnostics.kt).
Обработка сохранённых запросов описана
в [testsys-operation/README.md](../../testsys-operation/README.md), хранение — в
[database/README.md](../database/README.md).

## Анализ

[`PolygonDiagnosticsAdapter`](src/main/kotlin/tech/testsys/infra/diagnostics/api/PolygonDiagnosticsAdapter.kt)
читает отдельный XML мира из файла Полигона. Перед Ksoup строгий SAX-парсер проверяет корректность XML.
DTD и внешние сущности запрещены. Корень содержит соседние `world` и `constraints`:
ограничения находятся по пути `/root/constraints`. Формат описан в
[руководстве TRIK Studio](https://help.trikset.com/studio/2d-model/restrictions).

Парсер поддерживает конструкции исходного анализа: ограничения времени, события, условия, триггеры и выражения.
Условия `conditions` и отрицания обходятся рекурсивно. Неизвестный элемент, включая `setState` и `using`,
даёт Info; его содержимое не анализируется. Ошибка синтаксиса XML или известной конструкции даёт Error.
При невозможности построить модель семантические диагностики файла не запускаются.

| Диагностика | Проверка |
|-------------|----------|
| `TimeLimitConstraintDiagnostic` | Ровно один `timelimit` в разрешённом диапазоне |
| `InvalidEventIdDiagnostic` | Ссылки `settedUp`, `dropped`, `setUp` и `drop` на существующие события |
| `ScoreOutputDiagnostic` | Известный триггер `message` с настроенным префиксом вывода баллов; при отсутствии — Warning |

Причины сообщений типизированы в доменном
[`TestDiagnosticResult`](../../testsys-domain/src/main/kotlin/tech/testsys/domain/model/task/TestDiagnosticResult.kt).
Локация содержит теги и индексы среди одноимённых соседей, а для атрибута — его имя.
Одинаковые узлы различаются по пути. Отсутствующие атрибуты и дети указывают на существующего родителя;
строки и столбцы не вычисляются.

## Настройка и API

Приложение подключает
[`DiagnosticsConfiguration`](src/main/kotlin/tech/testsys/infra/diagnostics/api/DiagnosticsConfiguration.kt).
Свойство `testsys.diagnostics.max-time-limit-millis` задаёт включительную верхнюю границу.
По умолчанию она равна `600000` мс; ноль допустим. Отрицательная настройка отклоняется при создании бина.
Свойство `testsys.diagnostics.score-prefix` задаёт подстроку в атрибуте `text` известного `message`,
по которой определяется вывод баллов. По умолчанию — «Набрано баллов:».
Сравнение учитывает регистр и пробелы; пустая или состоящая только из пробелов настройка отклоняется.
Каждый вызов создаёт собственные парсер и диагностики, поэтому результаты независимы при параллельных вызовах.

Внешний код обращается через доменный порт. Внутренние объявления помечены `@InternalDiagnosticsApi`;
адаптер, конфигурация и тесты используют `@OptIn`. Модуль зависит от домена, Ksoup и Spring для регистрации,
не содержит планировщика и не обращается к БД запросов.
