# Модуль `testsys-infra:database`

Документ описывает, как устроен модуль хранения: его слои, генерацию идентификаторов, хранение файлов и схему БД.
Он предназначен для тех, кто работает с хранением доменных сущностей или настраивает запуск приложения.

Место модуля в архитектуре — в [structure.md](../../docs/project/structure.md). Пошаговое добавление хранения
сущности и правила для каждого слоя — в [implement-entity.md](../../docs/guides/implement-entity.md).
Кодогенерация для JPA-сущностей — в [codegen/README.md](codegen/README.md).

## Слои

Каждый слой разбит на подпакеты `group`, `task`, `user`, повторяющие домен.

| Пакет                         | Что лежит                                                                                  | Шаг гайда |
|-------------------------------|--------------------------------------------------------------------------------------------|-----------|
| `internal/jpa/entity`         | JPA-сущности: `SnowflakeJpaEntity` для сущностей, `CompositeJpaEntity` для join-таблиц      | 5         |
| `internal/jpa/repository`     | Spring Data репозитории                                                                    | 6         |
| `internal/mapping`            | `object XMapping` — преобразования между доменом и JPA                                     | 8         |
| `api/persistence/adapter`     | Адаптеры портов хранения `XPersistenceAdapter`                                             | 9         |
| `api/persistence`             | `FileDataStorage` — хранение файлов                                                        | —         |
| `internal/jpa/id`             | Генератор идентификаторов                                                                  | —         |
| `internal/utils`              | Общие помощники: `syncJoinTable`, `requireId`, `requireVersion`, `findByIdOrError`, `populateFields` | —         |

Всё в `internal` помечено `@InternalDatabaseApi` (`@RequiresOptIn`): снаружи модуля используется только `api`.
Бины регистрирует [DatabaseConfiguration.kt](src/main/kotlin/tech/testsys/infra/database/internal/jpa/DatabaseConfiguration.kt),
настройки Hibernate по умолчанию — в [hibernate-defaults.properties](src/main/resources/hibernate-defaults.properties).

## Идентификаторы

Идентификаторы сущностей выдаёт [SnowflakeIdGenerator.kt](src/main/kotlin/tech/testsys/infra/database/internal/jpa/id/SnowflakeIdGenerator.kt),
последовательностей в БД нет. Идентификатор состоит из 32 бит секунд с начала эпохи Unix, 10 бит node id и 16 бит
счётчика в пределах секунды.

Node id задаётся свойством `spring.jpa.properties.testsys.id.node-id`:

- по умолчанию `0`; у каждого одновременно запущенного экземпляра значение должно быть своим, в диапазоне `0..1023`;
- пустое, нечисловое или выходящее за диапазон значение роняет запуск;
- переменная окружения `SPRING_JPA_PROPERTIES_TESTSYS_ID_NODE_ID` **не работает**: Spring Boot превращает её
  в ключ `testsys.id.node.id`. Задавайте значение в application properties, аргументом командной строки
  `--spring.jpa.properties.testsys.id.node-id=N` или системным свойством JVM `-D...`.

## Хранение файлов

[FileDataStorage.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/FileDataStorage.kt) хранит файлы
только добавлением: метаданные — строкой `FileDataJpaEntity`, содержимое — в порту `FileBlobStorage`.

- Версии одного логического файла объединены общим UUID `versionBucket`.
- `storeIfChanged` не создаёт новую версию, если имя файла и хеш содержимого не изменились.
- У авторского Решения бакет один и тот же в `ts_developer_solution`, `ts_solution` и `ts_file_data`:
  все версии его файла лежат в одном бакете.
- Неверсионируемые файлы (логи, записи) получают свежий бакет при каждом сохранении.

## Схема БД

- Схемой управляет Liquibase: changelog'и лежат в `src/main/resources/db/changelog/changes/<версия>/`.
- Hibernate запускается с `ddl-auto=validate`, поэтому каждое изменение JPA-сущности требует changeset.
- Имена таблиц и колонок вычисляет `TestsysPhysicalNamingStrategy`.
- `SchemaValidationTests` (H2 в режиме PostgreSQL) применяет changelog'и и выполняет ту же валидацию.

Правила написания changeset — в шаге 7 [implement-entity.md](../../docs/guides/implement-entity.md).
