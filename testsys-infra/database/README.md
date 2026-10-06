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

## Постраничный поиск Задач и Туров

Методы `findAvailableToDeveloper` в
[TaskPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/TaskPersistenceAdapter.kt)
и [ContestPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/ContestPersistenceAdapter.kt)
передают условия доступа и фильтры в `JpaSpecificationExecutor`. БД выбирает собственные сущности
или сущности, доступные через переданные Сообщества. Предикат `EXISTS` исключает повторы при нескольких
основаниях доступа. При пустом наборе Сообществ запрос проверяет только владельца.

Фильтры названия, владельца и состояния Задачи применяются до выбора страницы и подсчёта общего числа.
Подсчёт использует ту же спецификацию. Сопоставление названия использует `locate` и `lower`;
правила фильтрации определены в [features.md](../../docs/domain/features.md).
Без параметров сортировки сущности упорядочиваются по идентификатору по возрастанию.
Этот порядок дополняет указанную сортировку, если в ней нет идентификатора.

Адаптеры собирают доменные сущности только выбранной страницы и возвращают исходные параметры пагинации.
Все чтения выполняются в транзакции с `readOnly = true`; исключения хранилища выходят к вызывающему коду.

## Постраничный поиск Вердиктов

Метод `findAvailableToJudge` в
[VerdictPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/VerdictPersistenceAdapter.kt)
передаёт идентификатор автора из `VerdictFilter` и `Pageable` в `VerdictJpaEntityRepository`.
БД фильтрует и сортирует Вердикты, затем выбирает страницу. Для подсчёта общего числа задан отдельный `countQuery` с теми же условиями.
Адаптер одним запросом загружает результаты Полигонов для всех Вердиктов выбранной страницы,
группирует их по идентификатору Вердикта и собирает страницу через `VerdictMapping`.
Для пустой страницы запрос результатов Полигонов не выполняется.
Результат содержит переданные параметры пагинации; связи с логами и видеозаписями остаются lazy.
Чтение выполняется в транзакции с `readOnly = true`.

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

- Каждый `save` сущности с файлом сохраняет файл новой строкой; строка `ts_file_data` после сохранения
  не меняется.
- Строка сущности не переключается на другой файл: `update` сверяет переданный файл с сохранённым через
  `FileDataStorage.matches` — по имени файла и хешу содержимого — и падает, если они различаются.

У каких сущностей файл фиксируется при создании и какие не поддерживают `update` — в разделе «Модель»,
как из таких строк складываются версии Ресурсов — в разделе «Версионирование Ресурсов»
в [testsys-domain/README.md](../../testsys-domain/README.md).

## Загруженные Ресурсы Задачи

Адаптеры Условий, Упражнений, Полигонов и Авторских Решений возвращают историю существующими доменными сущностями.
Их `findFileRef` читает только метаданные файла и возвращает существующий `StoredBlobRef`, не обращаясь к `FileBlobStorage`.

`TaskData.uploadedResources` хранится в `ts_version_bucket_to_task`: составной ключ включает `task_id`
и `version_bucket`, а уникальное ограничение на `version_bucket` исключает одновременную принадлежность
цепочки двум Задачам. В таблицах версий Ресурсов `version_bucket` остаётся UUID.

`TaskPersistenceAdapter` сохраняет и синхронизирует строки принадлежности отдельно от ревизий содержимого.
Удаление Задачи очищает эти строки, сохраняя версии Ресурсов и их файлы. Таблица хранит только текущую
принадлежность; история владельцев цепочки не сохраняется. Операций переноса или удаления Ресурсов пока нет.

## Коды-доступа Пользователей

`ts_user.access_token_hash_algorithm` хранит алгоритм строкой из `HashAlgorithmJpaEnum` рядом с `access_token`
([User.kt](src/main/kotlin/tech/testsys/infra/database/internal/jpa/entity/user/User.kt)).
Колонка создаётся с `NOT NULL` в
[changelog.03-init-user.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.03-init-user.xml).
Значения по умолчанию у колонки нет: при записи алгоритм передаётся явно.

Маппинги Пользователей объединяют значение и алгоритм в `AccessTokenHash` и передают его в `storedAccessToken(hash)`.
При записи оба поля переносятся из доменных данных без хэширования. Ввод исходного КД описан в разделе
[«Пользователи»](../../testsys-domain/README.md#пользователи).

Ограничение `uk_ts_user_access_token` обеспечивает уникальность исходных КД благодаря `Identity`.
Поэтому `UserJpaEntity` помечен `@RawAccessTokenDependency`. При переходе к хэшированию с индивидуальной солью
это ограничение нужно пересмотреть. Семантика сохранённого КД и правило аннотации — в разделе
[«Пользователи»](../../testsys-domain/README.md#пользователи).

## Схема БД

- Схемой управляет Liquibase: changelog'и лежат в `src/main/resources/db/changelog/changes/<версия>/`.
- Hibernate запускается с `ddl-auto=validate`, поэтому каждое изменение JPA-сущности требует changeset.
- Имена таблиц и колонок вычисляет `TestsysPhysicalNamingStrategy`.
- `SchemaValidationTests` (H2 в режиме PostgreSQL) применяет changelog'и и выполняет ту же валидацию.

Правила написания changeset — в шаге 7 [implement-entity.md](../../docs/guides/implement-entity.md).
