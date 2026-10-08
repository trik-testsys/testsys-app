# Модуль `testsys-infra:database`

Документ описывает, как устроен модуль хранения: его слои, генерацию идентификаторов, хранение файлов и схему БД.
Он предназначен для тех, кто работает с хранением доменных сущностей или настраивает запуск приложения.

Место модуля в архитектуре — в [structure.md](../../docs/project/structure.md). Пошаговое добавление хранения
сущности и правила для каждого слоя — в [implement-entity.md](../../docs/guides/implement-entity.md).
Кодогенерация для JPA-сущностей — в [codegen/README.md](codegen/README.md).

## Слои

Каждый слой разбит на подпакеты `entry`, `group`, `task`, `user`, повторяющие домен.

| Пакет                         | Что лежит                                                                                  | Шаг гайда |
|-------------------------------|--------------------------------------------------------------------------------------------|-----------|
| `internal/jpa/entity`         | JPA-сущности: `SnowflakeJpaEntity` для сущностей, `CompositeJpaEntity` для join-таблиц      | 5         |
| `internal/jpa/repository`     | Spring Data репозитории                                                                    | 6         |
| `internal/mapping`            | `object XMapping` — преобразования между доменом и JPA                                     | 8         |
| `api/persistence/adapter`     | Адаптеры портов хранения `XPersistenceAdapter`                                             | 9         |
| `api/persistence`             | `FileDataStorage` — хранение файлов, `FileSystemBlobStorage` — содержимое файлов на диске  | —         |
| `internal/jpa/id`             | Генератор идентификаторов                                                                  | —         |
| `internal/utils`              | Общие помощники: `syncJoinTable`, `requireId`, `requireVersion`, `findByIdOrError`, `populateFields` | —         |

Всё в `internal` помечено `@InternalDatabaseApi` (`@RequiresOptIn`): снаружи модуля используется только `api`.
Бины регистрирует [DatabaseConfiguration.kt](src/main/kotlin/tech/testsys/infra/database/api/DatabaseConfiguration.kt),
настройки Hibernate по умолчанию — в [hibernate-defaults.properties](src/main/resources/hibernate-defaults.properties).
Приложение подключает `DatabaseConfiguration` через `@Import`.

## Постраничный поиск Задач и Туров

Методы `findAvailableToDeveloper` в
[TaskPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/TaskPersistenceAdapter.kt)
и [ContestPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/ContestPersistenceAdapter.kt)
передают условия доступа и фильтры в `JpaSpecificationExecutor`. БД выбирает собственные сущности
или сущности, доступные через переданные Сообщества. Предикат `EXISTS` исключает повторы при нескольких
основаниях доступа. При пустом наборе Сообществ запрос проверяет только владельца.

Фильтры названия, владельца, состояния Задачи и предоставленного доступа Сообществу применяются до выбора
страницы и подсчёта общего числа. Связь с выбранным Сообществом проверяется отдельным предикатом `EXISTS`.
Подсчёт использует ту же спецификацию. Сопоставление названия использует `locate` и `lower`;
правила фильтрации определены в [features.md](../../docs/domain/features.md).
Без параметров сортировки сущности упорядочиваются по идентификатору по возрастанию.
Этот порядок дополняет указанную сортировку, если в ней нет идентификатора.

Адаптеры собирают доменные сущности только выбранной страницы и возвращают исходные параметры пагинации.
Все чтения выполняются в транзакции с `readOnly = true`; исключения хранилища выходят к вызывающему коду.

## Постраничный поиск Классов и Соревнований

Методы `findAvailableToManager` в
[ClassPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/group/ClassPersistenceAdapter.kt)
и [CompetitionPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/group/CompetitionPersistenceAdapter.kt)
передают владельца и фильтры в `JpaSpecificationExecutor`. БД выбирает только сущности переданного владельца.

Фильтры названия и даты создания применяются до выбора страницы и подсчёта общего числа.
Подсчёт использует ту же спецификацию. Правила фильтрации определены в [features.md](../../docs/domain/features.md).
Порядок по умолчанию, сборка страницы и транзакция чтения устроены так же, как в разделе
[«Постраничный поиск Задач и Туров»](#постраничный-поиск-задач-и-туров).

## Постраничный поиск Вердиктов

Метод `findAvailableToJudge` в
[VerdictPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/VerdictPersistenceAdapter.kt)
передаёт идентификаторы автора, Посылки, Класса и Соревнования из `VerdictFilter` и `Pageable`
в `VerdictJpaEntityRepository`. Членство автора и принадлежность Тура проверяются через `EXISTS`;
правила фильтрации определены в [features.md](../../docs/domain/features.md).
БД фильтрует и сортирует Вердикты, затем выбирает страницу. Для подсчёта общего числа задан отдельный `countQuery` с теми же условиями.
Адаптер одним запросом загружает результаты Полигонов для всех Вердиктов выбранной страницы,
группирует их по идентификатору Вердикта и собирает страницу через `VerdictMapping`.
Для пустой страницы запрос результатов Полигонов не выполняется.
Результат содержит переданные параметры пагинации; связи с логами и видеозаписями остаются lazy.
Чтение выполняется в транзакции с `readOnly = true`.

## Результаты Тура

Метод `findContestResults` в
[SubmissionPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/SubmissionPersistenceAdapter.kt)
собирает результаты Тура по переданным авторам и Задачам. Если один из наборов пуст, адаптер возвращает
пустой список и не обращается к БД.

Иначе адаптер читает из БД не больше трёх наборов строк, каждый одним запросом:

1. Посылки Тура с видом `GRADING`, авторы и Задачи которых входят в переданные наборы.
2. Судейские вердикты успешно проверенных Посылок.
3. Результаты Полигонов для Вердиктов успешно проверенных Посылок без Судейских вердиктов.

Второй и третий запросы не выполняются, если для них нет Посылок. Адаптер группирует Посылки по автору и Задаче,
считает их количество и выбирает лучший результат. Последний Судейский вердикт определяется по времени создания,
а при равном времени — по идентификатору. Правила подсчёта определены в [features.md](../../docs/domain/features.md).
Чтение выполняется в транзакции с `readOnly = true`.

## Создание Участников Соревнования

Метод `saveToCompetition` в
[ParticipantPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/user/single/ParticipantPersistenceAdapter.kt)
сохраняет переданный пакет Участников в одной транзакции. Имя Участника зависит от выданного ему идентификатора,
поэтому адаптер сначала сохраняет Участника с пустым именем, а затем в той же транзакции записывает имя через `update`.
Любое исключение, в том числе нарушение ограничения `uk_ts_user_access_token`, откатывает весь пакет.
Правила псевдонима и Кода-доступа Участника определены в [features.md](../../docs/domain/features.md).

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

Основная реализация порта — компонент
[FileSystemBlobStorage.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/FileSystemBlobStorage.kt).
Она хранит каждый блоб отдельным файлом с именем-UUID по пути, переданному в вызов, и не перезаписывает
существующие файлы. Путь должен быть абсолютным; `store` создаёт каталог, если его нет.

Путь выбирает адаптер вида Ресурса из
[FileStoragePaths.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/FileStoragePaths.kt) — класса
`@ConfigurationProperties` с обязательными свойствами `testsys.file-storage.paths.<вид>`, который регистрирует
`DatabaseConfiguration`. Виды: `statement`, `exercise`, `test`, `solution`, `recording` и `logs`. Файл Авторского
Решения хранится по пути `solution`. Если свойства нет, приложение не запустится.

`StoredBlobRef` из `findFileRef` действителен только вместе с путём своего вида, а путь знает только адаптер.
Поэтому код вне модуля пока не может загрузить файл по такой ссылке.

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

`AbstractUserPersistenceAdapter.findByAccessToken` хэширует исходный КД алгоритмом `Identity` и ищет строку
`ts_user` по сохранённому значению и алгоритму
([AbstractUserPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/user/AbstractUserPersistenceAdapter.kt)).
Строку Пользователя другого вида адаптер отбрасывает через `supports`. Поиск находит Пользователя, только пока
сохранённое значение совпадает с исходным КД, поэтому метод помечен `@RawAccessTokenDependency`.
При переходе к хэшированию с солью этот поиск нужно пересмотреть.

## Коды-приглашения

Коды-приглашения хранятся в таблицах `ts_class_invite` и `ts_community_invite`, созданных в
[changelog.15-init-invite.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.15-init-invite.xml).
Колонка `code` хранит сохранённое значение, `code_hash_algorithm` — алгоритм строкой из `HashAlgorithmJpaEnum`.
Обе колонки создаются с `NOT NULL` и без значения по умолчанию. Маппинги собирают `InviteCodeHash` из этих колонок
и передают его в `storedCode(hash)`; при записи оба поля переносятся без хэширования.

Ссылки хранят группы: `ts_class.invite_id`, `ts_community.manager_invite_id` и `ts_community.developer_invite_id`
— колонки `NOT NULL` с внешними ключами на таблицы Кодов-приглашений. В таблицах Кодов-приглашений колонок Класса
и Сообщества нет. Колонка `ts_community_invite.role` определяет вариант `CommunityInvite`.

Ограничения уникальности:

| Ограничение | Что обеспечивает |
|-------------|------------------|
| `uk_ts_class_invite_code`, `uk_ts_community_invite_code` | Различие Кодов-приглашений одного вида |
| `uk_ts_class_invite_id` | Код-приглашение принадлежит одному Классу |
| `uk_ts_community_manager_invite_id`, `uk_ts_community_developer_invite_id` | Код-приглашение для каждой Роли принадлежит одному Сообществу |

Ограничения на `code` обеспечивают уникальность исходных Кодов-приглашений благодаря `Identity`, поэтому
`ClassInviteJpaEntity` и `CommunityInviteJpaEntity` помечены `@RawInviteCodeDependency`. При переходе к хэшированию
с индивидуальной солью эти ограничения и поиск нужно пересмотреть. Правило аннотации — в разделе
[«Коды-приглашения»](../../testsys-domain/README.md#коды-приглашения).

`findByCode` ищет строку с равными значением и алгоритмом. `findExpired` возвращает идентификаторы строк
с `expires_at` не позже переданного момента по возрастанию; для этого поиска на `expires_at` есть индексы.

`ManagerCommunityInvitePersistenceAdapter` и `DeveloperCommunityInvitePersistenceAdapter` наследуют
`AbstractCommunityInvitePersistenceAdapter` и работают со строками `ts_community_invite` своей Роли:
строку другой Роли поиск не находит, удаление пропускает, а `update` отклоняет с `IllegalArgumentException`.
`update` не меняет `role`:
колонка берётся из текущей строки.

`ClassPersistenceAdapter.saveWithInvite` и `CommunityPersistenceAdapter.saveWithInvites` в одной транзакции
сохраняют строки Кодов-приглашений, а затем строку группы. `update` группы не меняет ссылки на Коды-приглашения:
их колонки берутся из текущей строки. Каскадного удаления в БД нет: `removeById` группы удаляет её строку,
а затем строки её Кодов-приглашений.

`ClassPersistenceAdapter.addStudent` в одной транзакции добавляет строку `ts_student_to_class`, если её нет,
и не меняет остальные данные Класса. `MultipleRoleUserPersistenceAdapter.addCommunityMembership` блокирует
строку Пользователя через `lockById` и в той же транзакции добавляет недостающие строки данных Роли и членства
в Сообществе.

## Регистрация, смена почты и поиск по почте

`RegistrationRequestPersistenceAdapter` хранит запросы регистрации в `ts_registration_request`
([changelog.16-init-registration-request.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.16-init-registration-request.xml)).
Ограничение `uk_ts_registration_request_email` не даёт сохранить второй запрос для той же почты.
Поля, которые `update` берёт из сохранённой строки, перечислены в KDoc
`RegistrationRequestMapping.toJpaEntity(entity, current)`
([RegistrationRequestMapping.kt](src/main/kotlin/tech/testsys/infra/database/internal/mapping/user/RegistrationRequestMapping.kt)).
`findByEmail` ищет запрос по точному совпадению почты. Код подтверждения хранится в исходном виде.

`EmailChangeRequestPersistenceAdapter` хранит запросы смены почты в `ts_email_change_request`
([changelog.17-init-email-change-request.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.17-init-email-change-request.xml)).
Ограничение `uk_ts_email_change_request_user_id` не даёт сохранить второй запрос того же Пользователя, а почта
запроса не уникальна. Внешний ключ `fk_ts_email_change_request_user_id` удаляет запрос вместе с Пользователем.
`update` берёт `user_id` из сохранённой строки (KDoc `EmailChangeRequestMapping.toJpaEntity(entity, current)`
в [EmailChangeRequestMapping.kt](src/main/kotlin/tech/testsys/infra/database/internal/mapping/user/EmailChangeRequestMapping.kt)).
`findByUser` ищет запрос по идентификатору Пользователя. Код подтверждения хранится в исходном виде.

`MultipleRoleUserPersistenceAdapter.findByEmail` ищет строку `ts_user` по точному совпадению почты
и отбрасывает строку Пользователя другого вида через `supports`.

## Схема БД

- Схемой управляет Liquibase: changelog'и лежат в `src/main/resources/db/changelog/changes/<версия>/`.
- Hibernate запускается с `ddl-auto=validate`, поэтому каждое изменение JPA-сущности требует changeset.
- Диалект SQL не задаётся: Hibernate определяет его по соединению (PostgreSQL в продакшене, H2 в тестах).
- Имена таблиц и колонок вычисляет `TestsysPhysicalNamingStrategy`.
- `SchemaValidationTests` (H2 в режиме PostgreSQL) применяет changelog'и и выполняет ту же валидацию.

Правила написания changeset — в шаге 7 [implement-entity.md](../../docs/guides/implement-entity.md).

Версию Liquibase задаёт BOM Spring Boot (раздел «Сборка» в [structure.md](../../docs/project/structure.md)).
С версии 5.0 Liquibase Community распространяется под Functional Source License (FSL) вместо Apache 2.0.
Использование в разработке, тестах и продакшене свободное; запрещено только строить на Liquibase конкурирующий
коммерческий сервис. Каждая версия через два года после выхода переходит на Apache 2.0.

## Хранение первого входа

Адаптеры `ParticipantContestEntryPersistenceAdapter` и `StudentContestEntryPersistenceAdapter`
сохраняют неизменяемые записи первого входа.
Метод `findOrCreate` блокирует строку Пользователя в короткой транзакции, читает полный контекст
и возвращает существующую запись либо сохраняет новую.
Уникальные ограничения контекста дополнительно запрещают двойную вставку.

Таблицы и внешние ключи добавлены в
[changelog.14-init-study-entry.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.14-init-study-entry.xml).
Новые внешние ключи удаляют записи каскадно при удалении родителей.
Удаление членства и связей группы с Туром не затрагивает эти таблицы.
Типы контекстов и порты — в разделе [«Записи первого входа»](../../testsys-domain/README.md#записи-первого-входа).
