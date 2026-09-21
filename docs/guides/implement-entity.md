# Добавление доменной сущности

Гайд описывает, как добавить в систему новую доменную сущность — от модели в `testsys-domain` до хранения в БД
и тестов. Он предназначен для разработчиков, которые добавляют сущность.

Гайд не повторяет общие правила: стиль кода и KDoc — в [code-style.md](../project/code-style.md), модули и команды
сборки — в [structure.md](../project/structure.md), устройство домена — в [testsys-domain/README.md](../../testsys-domain/README.md),
устройство модуля хранения — в [database/README.md](../../testsys-infra/database/README.md), пользовательские строки —
в [localization/README.md](../../testsys-infra/localization/README.md). Нужно ли описывать сущность в разделе
`testsys.entity`, определяет [features.md](../domain/features.md). Порты, не связанные с хранением сущности, —
в [implement-port.md](implement-port.md).

Сущность здесь — это то, что наследует `DomainEntity<Id>`: имеет собственный идентификатор, живёт в БД
отдельной строкой и имеет порт хранения. Значение внутри `Data` (например `Score`, `TrikStudioVersion`)
сущностью не является — для него достаточно объявить тип в файле модели.

Образец для всех шагов — `Contest` (Тур): сущность со связью «многие ко многим», ссылкой на справочник
и опциональным полем. Простой образец без связей — `Community`.

## Чек-лист

| #  | Шаг                          | Куда                                                                                  |
|----|------------------------------|---------------------------------------------------------------------------------------|
| 1  | Модель                       | `testsys-domain/.../model/<group\|task\|user>/X.kt`                                    |
| 2  | Билдер                       | `testsys-domain/.../builder/<group\|task\|user>/XBuilder.kt`                           |
| 3  | Точка входа DSL и `withData` | `testsys-domain/.../builder/api/{Group,Task,User}Api.kt`                               |
| 4  | Порт хранения                | `testsys-domain/.../contract/persistence/repository/{Group,Task,User}Repositories.kt` |
| 5  | JPA-сущность                 | `testsys-infra/database/.../internal/jpa/entity/<group\|task\|user>/X.kt`              |
| 6  | Spring Data репозиторий      | `testsys-infra/database/.../internal/jpa/repository/<group\|task\|user>/X.kt`          |
| 7  | Liquibase changeset          | `testsys-infra/database/src/main/resources/db/changelog/changes/...`                   |
| 8  | Маппинг                      | `testsys-infra/database/.../internal/mapping/<group\|task\|user>/XMapping.kt`          |
| 9  | Адаптер порта                | `testsys-infra/database/.../api/persistence/adapter/<group\|task\|user>/XPersistenceAdapter.kt` |
| 10 | Тесты                        | Тесты билдера, `withData`, маппинга, адаптера + фикстура в `DatabaseFixtures`          |

Подпакет (`group`, `task`, `user`) выбирается по смыслу сущности и **одинаков во всех слоях**.

Шаги 1–4 (домен) самодостаточны: домен компилируется и тестируется без инфраструктуры. Шаги 5–10 —
реализация хранения. Оба набора обычно делаются в одном PR.

## 1. Модель

Образец — [Contest.kt](../../testsys-domain/src/main/kotlin/tech/testsys/domain/model/task/Contest.kt).
В одном файле объявляются идентификатор `XId`, данные `XData` и сама сущность `X`. Как устроены сущность,
её версия и связи — в разделе «Модель» в [testsys-domain/README.md](../../testsys-domain/README.md).

- Заведите сущности собственный `XId`, реализующий `DomainId`, — даже если по форме он совпадает с чужим.
- Не переопределяйте `equals` и `hashCode`: они уже реализованы в `DomainEntity`.
- Связи с другими сущностями объявляйте только как `LazyEntity` / `LazyEntityList`, никогда самой сущностью.
- Вычислимые поля объявляются в `Data` как `val` и в БД не хранятся (`ContestData.endsAt`).

## 2. Билдер

Конструкторы доменных объектов напрямую в коде не вызываются — всё создаётся через DSL билдеров.
Образец — [ContestBuilder.kt](../../testsys-domain/src/main/kotlin/tech/testsys/domain/builder/task/ContestBuilder.kt):
в файле два класса, `XDataBuilder` и `XBuilder`.

- Обязательные поля — `var ...: T? = null` + `requireField` в `build()`; необязательные остаются
  nullable и передаются как есть, коллекции — пустой `mutableListOf()`.
- `XBuilder` наследует `DomainEntityWithDataBuilder` и получает `id`, `createdAt`, `version`, `data`
  бесплатно; переопределяются только `dataBuilder()` и `build()`.
- `version` необязателен: его не проверяют через `requireField`, а переносят на готовую сущность
  вызовом `applyVersion(version)` в конце `build()` — в конструктор сущности токен не передаётся.
- Для каждого поля-идентификатора добавляется перегрузка от «сырого» `Long` (`fun owner(owner: Long)`),
  для списков — от `Iterable<Long>`.
- Идентификаторы превращаются в связи через `lazify()` прямо в `build()`.
- Билдер проверяет только наличие обязательных полей, целостность данных — не его задача.

### Sealed-поля: `Chooser`

Если поле — sealed-тип, вместо `var` в билдер кладётся `val` типа `Chooser<T>`: он записывает выбранный
вариант и падает в `build()`, если вариант не выбран. Образец использования — поле `status`
в [SubmissionBuilder.kt](../../testsys-domain/src/main/kotlin/tech/testsys/domain/builder/task/SubmissionBuilder.kt).

Сам `XChooser : Chooser<T>()` живёт в `builder/util/chooser/` и на каждый вариант имеет функцию:
без параметров для `object`-вариантов и с блоком-билдером для вариантов с полями.
Образцы — [SubmissionStatusChooser.kt](../../testsys-domain/src/main/kotlin/tech/testsys/domain/builder/util/chooser/SubmissionStatusChooser.kt),
[TaskContentChooser.kt](../../testsys-domain/src/main/kotlin/tech/testsys/domain/builder/util/chooser/TaskContentChooser.kt).

## 3. Точка входа DSL и `withData`

В `builder/api/{Group,Task,User}Api.kt` (файл выбирается по подпакету сущности) добавляются функции `xData { }`,
`x { }`, приватная `XData.toBuilder()` и `X.withData { }`. Образец — функции `contestData`, `contest`
и `Contest.withData` в [TaskApi.kt](../../testsys-domain/src/main/kotlin/tech/testsys/domain/builder/api/TaskApi.kt).

`X.withData { }` создаёт новый экземпляр из `id`, `createdAt` и новых данных и переносит токен
оптимистической блокировки тем же `applyVersion(this.version)`.

`toBuilder()` **перечисляет все поля `Data` без исключения**: забытое поле молча потеряется при каждом
`withData`. Для sealed-полей восстановление варианта пишется через `when` без ветки `else`
(см. `SubmissionData.toBuilder()` и `populateFrom` в том же файле) — тогда новый вариант сломает
компиляцию, а не поведение.

## 4. Порт хранения

В `contract/persistence/repository/{Group,Task,User}Repositories.kt` добавляется интерфейс
`XRepository : EntityRepository<XData, XId, X>`. Образец — `ContestRepository`
в [TaskRepositories.kt](../../testsys-domain/src/main/kotlin/tech/testsys/domain/contract/persistence/repository/TaskRepositories.kt).

`EntityRepository` уже содержит поиск, загрузку, сохранение, обновление и удаление — см.
[Common.kt](../../testsys-domain/src/main/kotlin/tech/testsys/domain/contract/persistence/repository/Common.kt).
Свои методы добавляются в `XRepository`, только если операциям действительно нужен другой способ поиска;
реализовывать их тогда придётся в адаптере.

## 5. JPA-сущность

Образец — `ContestJpaEntity` в [Contest.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/internal/jpa/entity/task/Contest.kt).

- Класс называется `XJpaEntity` и наследует `SnowflakeJpaEntity` (как выдаются идентификаторы — в разделе
  «Идентификаторы» в [database/README.md](../../testsys-infra/database/README.md)). `createdAt`, `updatedAt`
  и `version` приходят из `JpaEntity` и ведутся Hibernate — не объявляйте их заново.
- `@InternalDatabaseApi` — на всех объявлениях в `internal`.
- **Ассоциаций JPA (`@ManyToOne`, `@OneToMany`) в проекте нет.** Внешние ключи хранятся как обычные
  `Long`-колонки (`ownerId`, `trikStudioVersionId`), связи «многие ко многим» — отдельными join-таблицами.
- Доменные типы разворачиваются в примитивы: `Duration` → `...Millis: Long`, sealed-тип → enum-колонка
  (см. [MappingUtils.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/internal/utils/MappingUtils.kt)),
  опциональное поле → nullable-свойство Kotlin.
- Длинный текст помечается `@field:JdbcTypeCode(SqlTypes.LONG32VARCHAR)` и ложится в `TEXT`.
- Имена таблиц и колонок считает `TestsysPhysicalNamingStrategy`: `ContestJpaEntity` → `ts_contest`,
  `contestDurationMillis` → `contest_duration_millis`. Руками `@Table`/`@Column` не задаём.
- Регистрировать сущность нигде не нужно: `DatabaseConfiguration` сканирует пакеты
  `...internal.jpa.entity`, `...internal.jpa.repository` и `...api`.

### Join-таблица для связи «многие ко многим»

Кладётся в тот же файл, что и сущность: `@Embeddable data class AToBId : CompositeId` и
`@CompositeKeyConstructor class AToBJpaEntity : CompositeJpaEntity<AToBId>`. Образец — `TaskToContestId`
и `TaskToContestJpaEntity` в том же [Contest.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/internal/jpa/entity/task/Contest.kt).
Что генерирует `@CompositeKeyConstructor` и какие требования он предъявляет к классу, — в
[codegen/README.md](../../testsys-infra/database/codegen/README.md).

## 6. Spring Data репозиторий

Образец — [Contest.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/internal/jpa/repository/task/Contest.kt)
в пакете `repository`.

Базовые интерфейсы — `SnowflakeJpaEntityRepository` для обычных сущностей и
`CompositeJpaEntityRepository<Entity, Id>` для join-таблиц; оба включают `JpaSpecificationExecutor`.
Для join-таблиц добавляются выборки по каждой стороне ключа — через `@Query`, потому что поля лежат
внутри `id` (`where e.id.contestId = :contestId`), плюс перегрузка с `Pageable`, если нужна постраничность.

## 7. Liquibase changeset

Hibernate стартует с `ddl-auto=validate`, поэтому **любая новая таблица или колонка требует changeset** —
иначе приложение и тесты не поднимутся. Образец — [changelog.07-init-contest.xml](../../testsys-infra/database/src/main/resources/db/changelog/changes/1.0.0/changelog.07-init-contest.xml).

- Каждая таблица получает `id`, `created_at`, `updated_at`, `version` — это поля `JpaEntity`.
- Имена таблиц и колонок должны совпадать с тем, что вычислит `TestsysPhysicalNamingStrategy`.
- Nullability колонок — ровно как в JPA-сущности, иначе `validate` не пройдёт.
- Внешние ключи именуются `fk_<таблица>_<поле>`, первичные — `pk_<таблица>`.
  У join-таблицы обе колонки входят в составной первичный ключ с одним `primaryKeyName`.
- [db.changelog-master.yaml](../../testsys-infra/database/src/main/resources/db/changelog/db.changelog-master.yaml)
  подключает каталог версии, а [changelog.master.xml](../../testsys-infra/database/src/main/resources/db/changelog/changes/1.0.0/changelog.master.xml)
  внутри версии перечисляет файлы в порядке применения — новый файл нужно в него добавить.

## 8. Маппинг

Образец — [ContestMapping.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/internal/mapping/task/ContestMapping.kt).

- `object XMapping : EntityMapping<Domain, Jpa>` — без состояния.
- `toDomain(...)` собирает доменный объект **только через DSL билдеров**; `populateFields(jpaEntity)`
  проставляет `id`, `createdAt` и `version` из строки.
- `toJpaEntity` всегда две перегрузки: от `Data` — для новой строки (без `id`), и от сущности плюс текущей
  строки — для обновления. Вторая обязана перенести `createdAt` из текущей строки и `version` из доменной
  сущности через `requireVersion()`: именно так до Hibernate доезжает токен оптимистической блокировки,
  а сущность, не полученная из хранилища, приводит к `IllegalArgumentException`.
- Данные, которых нет в самой строке (идентификаторы из join-таблиц, значения справочников), приходят
  отдельными параметрами — маппинг ничего не читает из БД сам.
- Для join-таблиц добавляются функции `toXAssociations(ownerId, ids)`, собирающие строки связи.
- Имена `toDomain`/`toJpaEntity` и типы результата проверяются рефлексией в `EntityMappingTest` — не
  переименовывайте их и не возвращайте из перегрузок посторонние типы.

## 9. Адаптер порта

Образец — [ContestPersistenceAdapter.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/ContestPersistenceAdapter.kt).

[AbstractPersistenceAdapter.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/AbstractPersistenceAdapter.kt)
уже реализует `findById`, `findByIds`, `load`, удаление и списочные перегрузки `save`/`update`.
Подкласс реализует три метода:

- `save(data)` — собрать строку маппингом, сохранить, при наличии связей сохранить строки join-таблиц,
  вернуть доменный объект. Идентификатор сохранённой строки берётся через `requireId()`.
- `update(entity)` — прочитать текущую строку (`findByIdOrError`), собрать новую перегрузкой `toJpaEntity`
  с `current`, сохранить через `saveAndFlush` (иначе конфликт версий всплывёт не там, где ожидается),
  затем синхронизировать join-таблицы через `syncJoinTable(...)` из
  [PersistenceUtils.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/internal/utils/PersistenceUtils.kt).
- `assemble(jpaEntity)` — собрать доменный объект из строки, дочитав идентификаторы связей и справочники.

Если у сущности есть join-таблицы, может дополнительно потребоваться переопределить `removeById`/`removeByIds`, если строки связей
нужно удалить до самой сущности. Для сущностей-пользователей базовый класс другой —
[AbstractUserPersistenceAdapter.kt](../../testsys-infra/database/src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/user/AbstractUserPersistenceAdapter.kt),
который дополнительно требует `supports(jpaEntity)`: все виды пользователей лежат в одной таблице `ts_user`,
и фильтр не даёт собрать чужую строку.

## 10. Тесты

| Что                  | Базовый класс                          | Что писать                                                  | Образец                   |
|----------------------|----------------------------------------|-------------------------------------------------------------|---------------------------|
| Билдер               | `DomainEntityBuilderTests`             | Только `buildDataWithAllFields()`                           | [ContestBuilderTests.kt](../../testsys-domain/src/test/kotlin/tech/testsys/domain/builder/task/ContestBuilderTests.kt) |
| `withData`           | `{Group,Task,User}ApiTest`             | `@Nested inner class XTests`: поля и токен `version` не теряются, поля изменяются | `ContestTests` в [TaskApiTest.kt](../../testsys-domain/src/test/kotlin/tech/testsys/domain/builder/api/TaskApiTest.kt) |
| Маппинг              | `EntityMappingTest<XMapping>`          | Только `override val mapping = XMapping`                    | [ContestMappingTest.kt](../../testsys-infra/database/src/test/kotlin/tech/testsys/infra/database/internal/mapping/task/ContestMappingTest.kt) |
| Адаптер              | `PersistenceAdapterContractTest`       | `newData()`, `modified()`, `detached()`, `idOf()`, `assertSameData()` + свои тесты | [ContestPersistenceAdapterTest.kt](../../testsys-infra/database/src/test/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/ContestPersistenceAdapterTest.kt) |
| Фикстура             | —                                      | Метод `fun x(...): X`                                       | [DatabaseFixtures.kt](../../testsys-infra/database/src/test/kotlin/tech/testsys/infra/database/DatabaseFixtures.kt) |

- `DomainEntityBuilderTests` сам проверяет, что `build()` падает без обязательных полей, а с ними — нет.
  Возвращайте из `buildDataWithAllFields()` несколько вариантов: минимальный и со всеми опциональными полями.
- `PersistenceAdapterContractTest` даёт весь контракт репозитория (идентификаторы, версии, поиск, загрузку,
  оптимистическую блокировку, удаление). В `modified()` меняйте **все** изменяемые поля, включая связи,
  иначе часть `update` останется непроверенной. Специфика сущности оформляется отдельными `@Test`
  в том же классе.
- Прочие сущности для теста создаются только через `fixtures`, а не руками; всё уникальное — через
  `fixtures.unique(...)`. Фикстура новой сущности пишется так же: через её же адаптер.
- Тесты БД поднимают H2 в режиме совместимости с PostgreSQL, применяют changelog'и и проверяют схему
  (`SchemaValidationTest`) — отдельный тест на changeset писать не нужно, достаточно не сломать этот.
