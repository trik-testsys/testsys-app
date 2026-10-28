# Модуль `testsys-infra:database`

Документ описывает, как устроен модуль хранения: его слои, транзакции, генерацию идентификаторов, хранение файлов,
схему БД и тесты.
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
| `api/transaction`             | Повтор транзакций при конфликте, см. раздел [«Транзакции и согласованность»](#транзакции-и-согласованность) | — |
| `api/settings`                | `DatabaseSettingsStore` — хранение служебных настроек                                      | —         |
| `api/registry`                | `DatabaseGradingNodeStore` — хранение реестра Проверяющих узлов                            | —         |
| `internal/jpa/id`             | Генератор идентификаторов                                                                  | —         |
| `internal/utils`              | Общие помощники: `syncJoinTable`, `findLinkedIds`, `findAllInChunks`, `findAllByIdOrError`, `requireId`, `requireVersion`, `findByIdOrError`, `populateFields` | —         |

Всё в `internal` помечено `@InternalDatabaseApi` (`@RequiresOptIn`): снаружи модуля используется только `api`.
Бины регистрирует [DatabaseConfiguration.kt](src/main/kotlin/tech/testsys/infra/database/api/DatabaseConfiguration.kt),
настройки Hibernate по умолчанию — в [hibernate-defaults.properties](src/main/resources/hibernate-defaults.properties).
Приложение подключает `DatabaseConfiguration` через `@Import`.

## Транзакции и согласованность

Все транзакции выполняются с уровнем изоляции REPEATABLE READ: его задаёт свойство
`spring.datasource.hikari.transaction-isolation` в `hibernate-defaults.properties`. Транзакция читает один снимок
БД. Если она записывает строку, которую после начала транзакции изменила другая зафиксированная транзакция,
PostgreSQL отклоняет запись с SQLSTATE `40001`.

### Версия корня агрегата

Агрегат — корневая строка и строки, которые существуют только ради неё (части). Любая запись в строку агрегата
повышает версию корня, а если адаптер получил доменную сущность, сначала сверяет её токен `version` с версией
корня. Так `update` с устаревшим снимком падает, даже если между чтением и записью изменились только части.

Правило реализует метод `touchRoot` в
[AbstractPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/AbstractPersistenceAdapter.kt):

1. Метод находит корень и сверяет переданный токен.
2. Метод записывает колонки корня, если запись их меняет, — `update` делает это через `updateRoot`.
3. Если версия корня ещё не выросла в этой транзакции, метод сразу повышает её блокировкой
   `PESSIMISTIC_FORCE_INCREMENT`: Hibernate выполняет `UPDATE` версии с проверкой прежнего значения.
4. Метод возвращает корень с версией, которая записана в БД.

В одной транзакции принудительное повышение выполняется не больше одного раза. Охранная запись — вызов `touchRoot`,
который повышает версию корня, но данных агрегата не меняет: например, создание запроса проверки повышает версию
Задачи. Токен принимается, если он равен текущей версии корня. Токен, прочитанный до охранной записи этой же
транзакции, тоже принимается. После записи, которая меняет данные, такой токен
отклоняется с `ObjectOptimisticLockingFailureException`. Корень, созданный в этой же транзакции, принудительно
не повышается. Если охранная запись и изменение колонок корня выполнены в одной транзакции, версия вырастет дважды.

Исходная версия, создание корня и изменения данных учитываются до завершения транзакции независимо от очистки
контекста постоянства. Учёт в
[AggregateVersionTracker.kt](src/main/kotlin/tech/testsys/infra/database/internal/jpa/AggregateVersionTracker.kt)
принадлежит синхронизации текущей транзакции: отдельная транзакция `REQUIRES_NEW` получает собственное состояние,
а после фиксации или отката оно не используется снова.

Правило действует для каждого записывающего метода адаптера, в том числе для методов, которые пока никто
не вызывает. Удаление тоже проходит через `touchRoot`: `removeById`, `removeByIds` и `remove` вызывают защищённый
метод `removeRoot` для каждой строки, а `remove` дополнительно сверяет токен удаляемой сущности. Базовый
`removeRoot` удаляет корень без частей; адаптер агрегата с частями или с записями в чужие агрегаты переопределяет
`removeRoot`, а не публичные методы удаления. Части агрегата можно удалять массово (`@Modifying`-запросом) только
после `touchRoot` корня в той же транзакции; такие запросы очищают контекст постоянства.

Любое изменение множества, которое защищает корень, повышает его версию — и создание, и удаление элемента.
Поэтому каждая запись из таблицы ниже повышает версию чужого корня и при создании, и при удалении.

| Корень | Части и что защищает версия |
|--------|-----------------------------|
| Задача, `ts_task` | `ts_task_content` и связи ревизий с Упражнениями, Полигонами, Авторскими Решениями и версиями TRIK Studio, `ts_community_to_task`, `ts_version_bucket_to_task`. Версия защищает состав ревизий, доступ, последнюю версию Ресурса в прикреплённой цепочке и предотвращает параллельное создание одинаковых активных снимков через `findOrCreateActive` |
| Тур, `ts_contest` | `ts_task_to_contest`, `ts_community_to_contest` |
| Класс, `ts_class` | `ts_student_to_class`, `ts_contest_to_class` |
| Соревнование, `ts_competition` | `ts_contest_to_competition`. Версия защищает лимит Участников |
| Сообщество, `ts_community` | Частей нет |
| Пользователь с несколькими Ролями, `ts_user` | `ts_multiple_role_to_user`, `ts_developer_data`, `ts_student_data`, `ts_judge_data`, `ts_manager_data`, `ts_administrator_data` |
| Пользователь с одной Ролью, `ts_user` | `ts_single_role_to_user`, `ts_participant_data`, `ts_observer_data`, `ts_supervisor_data`, `ts_contest_to_observer` |
| Запрос проверки, `ts_task_validation_request` | Четыре таблицы `ts_*_to_task_validation_request`, `ts_test_diagnostic_result`, `ts_diagnostic_report` |
| Перепроверка, `ts_regrade` | `ts_test_to_regrade`, `ts_submission_to_regrade` |
| Посылка, Приказ судьи, Вердикт, Коды-приглашения, запросы регистрации, смены почты и восстановления доступа, записи первого входа | Частей нет; у Вердикта часть `ts_test_verdict` |

Некоторые записи меняют чужой агрегат и поэтому повышают его версию:

| Запись | Повышаемый корень |
|--------|-------------------|
| Создание, изменение и удаление версии Условия, Упражнения, Полигона и Авторского Решения | Задача, к которой прикреплена цепочка (`findTaskIdByVersionBucket`) |
| Создание запроса проверки (`findOrCreateActive`, `save`) и его удаление | Задача запроса |
| Создание Участника (`save`, `saveToCompetition`) и его удаление | Соревнование |
| Удаление Пользователя с несколькими Ролями | Классы Ученика по возрастанию идентификатора |
| Удаление Класса и Сообщества | Их Коды-приглашения |
| Добавление Ученика в Класс: `save`, `update` и `addStudent` Класса | Пользователь-Ученик |

Пока Ученик состоит в Классах (есть строки `ts_student_to_class`), `MultipleRoleUserPersistenceAdapter.update`
не снимает с него Роль Ученика и падает с `IllegalArgumentException`. Проверка выполняется после того, как `update`
повысил версию строки Пользователя, а добавление в Класс повышает ту же версию. Поэтому параллельные снятие Роли
и вход в Класс конфликтуют, и повтор транзакции видит результат другой.

`save` и `update` Класса повышают версии всех добавленных Учеников одним запросом через метод `touchRoots`
базового адаптера. Метод предварительно читает корни пакетами, чтобы запомнить исходные версии даже незагруженных
Пользователей, пропускает строки, версия которых уже выросла в этой транзакции, и сдвигает версию
управляемых строк в контексте постоянства. Поэтому последующие `touchRoot` и `updateRoot` этих Пользователей
в той же транзакции работают так же, как после `touchRoot`.

Соревнование Участника фиксируется при создании: `ParticipantPersistenceAdapter.update` его не меняет.

Обратные списки только читаются: Задачи и Туры Разработчика, Классы и Соревнования Организатора, Классы
и Посылки Ученика, Приказы судьи у Судьи и у Посылки, Участники Соревнования. Запись в их исходные таблицы
защищает владеющий агрегат. Строки `ts_solution`, `ts_logs`, `ts_recording` и `ts_file_data` после сохранения
не меняются, поэтому версия их не защищает. Строку `ts_trik_studio_version` меняет только служебная смена тега,
см. [«Служебная регистрация версий TRIK Studio»](#служебная-регистрация-версий-trik-studio). Файл и другие
поля версии Ресурса, фиксируемые при создании, тоже не меняются; `update` версии Ресурса сверяет токен её
собственной строки.

### Повтор транзакций

Модуль повторяет транзакцию, которая столкнулась с параллельной транзакцией. Конфликтом
[TransactionConflicts.kt](src/main/kotlin/tech/testsys/infra/database/api/transaction/TransactionConflicts.kt) считает
`ConcurrencyFailureException`, `OptimisticLockException`, `StaleStateException` и SQLSTATE `40001`, `40P01`,
`23505` в любом месте цепочки причин. Остальные исключения, в том числе `OperationException` и нарушения внешнего
ключа, не повторяются.

[TransactionRetry.kt](src/main/kotlin/tech/testsys/infra/database/api/transaction/TransactionRetry.kt) выполняет
повтор на `RetryTemplate` Spring Framework. Транзакция выполняется не больше трёх раз; пауза перед вторым
разом — 10–50 мс, перед третьим — 10–100 мс. Внутри активной транзакции повтора нет: повторить можно только
внешнюю транзакцию целиком. Каждый повтор записывается в лог с уровнем DEBUG. Конфликт, оставшийся после
последнего повтора, записывается с уровнем WARN и выходит к вызывающему коду исходным исключением.
Бин `transactionRetry` считает каждый повтор в метрике `testsys.transaction.retries`, если в контексте есть реестр
метрик; каталог метрик — в [monitoring/README.md](../monitoring/README.md#каталог-метрик).

`DatabaseConfiguration` подключает повтор двумя способами:

- advisor с `RetryingTransactionInterceptor` стоит снаружи `TransactionInterceptor` каждого метода
  с `@Transactional`; каждая попытка открывает и фиксирует свою транзакцию, поэтому повторяется и конфликт
  при фиксации;
- бин `TransactionOperations` — `RetryingTransactionOperations` для программных транзакций; он заменяет
  `TransactionTemplate` Spring Boot.

Повтор устаревшего внешнего токена не исправляет: такая транзакция падает после трёх попыток.

### Ограничения

- Повтор не отменяет запись файлов в `FileBlobStorage`: файлы откатившейся попытки остаются.
- Версия корня защищает один агрегат. Транзакция, которая читает один агрегат и пишет в другой без охранной
  записи, может опираться на устаревшие данные: REPEATABLE READ в PostgreSQL такой конфликт (write skew)
  не обнаруживает.
- Цепочка версий Ресурса, не прикреплённая к Задаче, версией не защищена: две параллельные правки могут создать
  две последние версии.
## Сборка доменных сущностей

Адаптер собирает доменные сущности из строк методом `assembleAll` базового класса
[AbstractPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/AbstractPersistenceAdapter.kt).
Метод возвращает сущности в порядке переданных строк. `findByIds`, `load` списка, страницы и списочные выборки
передают ему все найденные строки одним вызовом. `assemble` собирает одну строку: вызывает `assembleAll`
со списком из этой строки.

Вспомогательные функции `findLinkedIds`, `findAllInChunks` и `findAllByIdOrError` из
[BatchLoadUtils.kt](src/main/kotlin/tech/testsys/infra/database/internal/utils/BatchLoadUtils.kt) читают строки
для всего списка одним запросом на таблицу. Условие `IN` они делят на порции по 1024 идентификатора, а при пустом
наборе идентификаторов не обращаются к БД. `findAllByIdOrError` падает с `IllegalArgumentException`, если строки
с каким-либо из идентификаторов нет.

Пары идентификаторов `Long` из связанной таблицы репозиторий может возвращать проекцией
[LinkedIdRow](src/main/kotlin/tech/testsys/infra/database/internal/jpa/repository/LinkedIdRow.kt) вместо целых строк,
если адаптеру нужны только они.

Адаптеры Условий, Упражнений, Полигонов, Решений, логов и видеозаписей читают строки `ts_file_data`
пакетами через `FileDataStorage.loadAll`. Сущности содержат ссылки на блобы; сборка не читает их содержимое.

## Постраничный поиск Задач и Туров

Методы `findAvailableToDeveloper` в
[TaskPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/TaskPersistenceAdapter.kt)
и [ContestPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/ContestPersistenceAdapter.kt)
передают условия доступа и фильтры в `JpaSpecificationExecutor`. БД выбирает собственные сущности
или сущности, доступные через переданные Сообщества. Предикат `EXISTS` исключает повторы при нескольких
основаниях доступа. При пустом наборе Сообществ запрос проверяет только владельца.
Метод `findSharedTo` в `ContestPersistenceAdapter` выбирает Туры только по переданным Сообществам, без учёта
владельца. При пустом наборе Сообществ он возвращает пустую страницу без запроса к БД.

Фильтры названия, владельца, исключаемого владельца Задачи, состояния Задачи и предоставленного доступа Сообществу применяются до выбора
страницы и подсчёта общего числа. Связь с выбранным Сообществом проверяется отдельным предикатом `EXISTS`.
Подсчёт использует ту же спецификацию. Сопоставление названия использует `locate` и `lower`;
правила фильтрации определены в [features.md](../../docs/domain/features.md).
Без параметров сортировки сущности упорядочиваются по идентификатору по возрастанию.
Этот порядок дополняет указанную сортировку, если в ней нет идентификатора.

Адаптеры собирают доменные сущности только выбранной страницы (раздел
[«Сборка доменных сущностей»](#сборка-доменных-сущностей)) и возвращают исходные параметры пагинации.
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

`CompetitionPersistenceAdapter.findAvailableToObserver` собирает пары Соревнование–назначенный Тур из текущих
связей. Он применяет фильтры и сортировку к парам до выбора страницы и возвращает их точное общее число.
Один Тур может входить в несколько Соревнований и занимать несколько строк.

## Постраничный поиск Вердиктов

Метод `findAvailableToJudge` в
[VerdictPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/VerdictPersistenceAdapter.kt)
строит `Specification` по `VerdictFilter` и передаёт её с `Pageable` в `JpaSpecificationExecutor`.
Условия автора, Посылки, Класса и Соревнования добавляются только для заданных в фильтре значений.
Посылка Вердикта, членство автора и принадлежность Тура проверяются через `EXISTS`;
правила фильтрации определены в [features.md](../../docs/domain/features.md).
БД фильтрует и сортирует Вердикты, затем выбирает страницу. Подсчёт общего числа использует ту же спецификацию.
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

Метод `findGradingByContest` того же адаптера возвращает Посылки одного автора по набору Задач Тура.
Он использует пакетный JPA-запрос Посылок из этого раздела и собирает доменные объекты.
Порядок и условия выборки определены в KDoc порта `SubmissionRepository`.

## Количество Посылок

Методы `countGradingByTask` и `countGrading` в
[SubmissionPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/task/SubmissionPersistenceAdapter.kt)
считают Посылки вида `GRADING` в любом статусе одним агрегирующим запросом `SubmissionJpaEntityRepository`
и не загружают сами Посылки. `countGradingByTask` группирует Посылки Тура по Задаче. `countGrading` считает Посылки
переданных авторов в переданных Турах и число разных авторов через `count(distinct ...)`; если один из наборов пуст,
адаптер возвращает нули и не обращается к БД. Метод `findGradingAuthorIds` находит переданных авторов с такими
Посылками в переданных Турах одним запросом `select distinct` по автору; если один из наборов пуст, адаптер возвращает
пустой набор и не обращается к БД. Чтение выполняется в транзакции с `readOnly = true`.

## Прогресс Перепроверок

`RegradePersistenceAdapter.countGraded` считает прогресс каждой Перепроверки из набора. Что входит в прогресс,
определено в
[testsys.user.multi.developer.task.commitTask](../../docs/domain/features.md#testsysusermultidevelopertaskcommittask-partially-implemented).
Полигоны результатов берутся из `ts_test_verdict.test_id`, Полигоны Перепроверки — из `ts_test_to_regrade`.
Подсчёт выполняет один агрегирующий запрос `RegradeJpaEntityRepository.countGraded` с `count(distinct ...)` и
подзапросами `not exists`; адаптер не загружает сами Посылки и Вердикты.
Для пустого набора адаптер возвращает пустой результат и не обращается к БД.

## Соревнования Туров

`CompetitionPersistenceAdapter.findByContestIds` находит идентификаторы Соревнований одним запросом к связям
`ContestToCompetitionJpaEntity` и собирает Соревнования в порядке идентификаторов.

## Версии TRIK Studio

Строка `ts_trik_studio_version` хранит тег Docker-образа в колонке `tag` и номер версии в колонке `studio_version`.
Обе колонки обязательны и уникальны. Адаптеры Туров, Задач, Посылок и запросов валидации ищут строку версии по тегу,
сверяют её номер с номером переданной версии и хранят ссылку на идентификатор строки, поэтому смена тега
не затрагивает ссылки. Если строки с тегом нет или её номер другой, адаптер бросает `IllegalArgumentException`
без сохранения.
`ContestPersistenceAdapter.findTrikStudioVersions` читает все строки и возвращает версии с тегом и номером,
упорядоченные по тегу.

## Создание Участников Соревнования

Метод `saveToCompetition` в
[ParticipantPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/user/single/ParticipantPersistenceAdapter.kt)
сохраняет переданный пакет Участников в одной транзакции. Имя Участника зависит от выданного ему идентификатора.
Адаптер получает идентификатор при `persist`, ещё до записи строки в БД. Поэтому он сначала добавляет всех Участников
пакета с пустым именем, затем переносит в их строки имена и записывает всё одним `flush` в конце метода.
Hibernate отправляет строки пакетными INSERT и UPDATE. Любое исключение, в том числе нарушение ограничения
`uk_ts_user_access_token` при этом `flush`, откатывает весь пакет.
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

## Строки с составным ключом

[JpaEntity.kt](src/main/kotlin/tech/testsys/infra/database/internal/jpa/entity/JpaEntity.kt): `CompositeJpaEntity`
реализует `Persistable`. Экземпляр, созданный кодом, считается новой строкой, и `save` вставляет его через `persist`
без предварительного чтения. Экземпляр, загруженный из БД или переданный в `persist`, новым не считается.
Поэтому `save` создаёт строку только для ключа, которого нет в таблице: повтор ключа нарушает первичный ключ.
Изменять и удалять строку можно только через загруженный экземпляр. Созданный заново экземпляр с ключом
существующей строки Spring Data при `delete` пропускает без запроса. Функции маппинга `toXAssociations` возвращают
одну строку на ключ.

## Хранение файлов

[FileDataStorage.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/FileDataStorage.kt) хранит файлы
только добавлением: метаданные — строкой `FileDataJpaEntity`, содержимое — в порту `FileBlobStorage`.

- Каждый `save` сущности с файлом сохраняет файл новой строкой; строка `ts_file_data` после сохранения
  не меняется. Содержимое читается один раз через `FileContentReader` и записывается в новый блоб,
  в том числе при копировании сохранённого файла. Результат `save` содержит ссылку на новый блоб.
- Строка сущности не переключается на другой файл: `update` сверяет переданный файл с сохранённым через
  `FileDataStorage.matches` и падает, если они различаются. Для `Inline` сравниваются имя и SHA-256 байтов,
  для `Stored` — имя, ключ и вид хранилища. Сравнение не читает блоб.

`FileDataStorage` реализует `FileContentReader`: `read` возвращает байты `Inline` либо загружает блоб `Stored`.
Методы `load` и `loadAll` читают только метаданные и возвращают `FileData` со ссылкой. Ссылку можно прочитать
после завершения транзакции; отсутствующий блоб обнаруживается при явном `read`.

Основная реализация порта — компонент
[FileSystemBlobStorage.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/FileSystemBlobStorage.kt).
Она хранит каждый блоб отдельным файлом с именем-UUID по пути, переданному в вызов, и не перезаписывает
существующие файлы. Путь должен быть абсолютным; `store` создаёт каталог, если его нет.

Адаптер вида Ресурса передаёт `FileStorageKind`, а `FileDataStorage` выбирает путь из
[FileStoragePaths.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/FileStoragePaths.kt).
`DatabaseConfiguration` создаёт бин этого класса и один раз привязывает через `Binder` обязательные свойства
`testsys.file-storage.paths.<вид>`. Виды: `statement`, `exercise`, `test`, `solution`, `recording` и `logs`.
Файл Авторского Решения хранится по пути `solution`. Если свойства нет, приложение не запустится.

`StoredBlobRef` из `findFileRef` действителен только вместе с путём своего вида, а путь знает только адаптер.
Поэтому код вне модуля пока не может загрузить файл по такой ссылке.

У каких сущностей файл фиксируется при создании и какие не поддерживают `update` — в разделе «Модель»,
как из таких строк складываются версии Ресурсов — в разделе «Версионирование Ресурсов»
в [testsys-domain/README.md](../../testsys-domain/README.md).

### Очистка сирот

Бин [FileStorageCleanup](src/main/kotlin/tech/testsys/infra/database/api/persistence/FileStorageCleanup.kt)
предоставляет явный метод `run()`. Вызывайте его вне транзакции в режиме обслуживания: остановите всех писателей,
дождитесь завершения их транзакций и используйте каталоги, выделенные этой БД. Автоматического расписания нет.
Возраст файла не исключает незавершённую транзакцию, поэтому проход при работающих писателях небезопасен.

Значения по умолчанию заданы в [file-storage-defaults.properties](src/main/resources/file-storage-defaults.properties)
и перечислены в таблице раздела «Параметры приложения»
в [deployment.md](../../docs/project/deployment.md#параметры-приложения):

| Свойство | Значение |
|----------|----------|
| `testsys.file-storage.cleanup.minimum-age` | Минимальное время после изменения физического файла, положительная длительность |
| `testsys.file-storage.cleanup.batch-size` | Максимальный размер пакета, положительное целое число |

`DatabaseConfiguration` объявляет `Settings<FileStorageCleanupProperties>`, а `FileStorageCleanup` один раз в начале
`run()` получает из него оба значения. Возраст принимает ISO-8601 и единицы Spring, например `24h`.
Неверная настройка прерывает запуск контекста. Источники значений описаны в
[settings](../settings/README.md#типизированные-значения).

Проход удаляет строки `ts_file_data` без владельцев независимо от времени создания.
Для физических файлов он фиксирует один порог времени и удаляет только файлы со временем изменения строго раньше
этого порога. Момент потери владельца не учитывается.
Удаления выполняются в следующем порядке:

| Объект | Ссылки, которые сохраняют объект |
|--------|---------------------------------|
| `ts_file_data` | `file_data_id` в `ts_statement`, `ts_exercise`, `ts_test`, `ts_solution`, `ts_logs`, `ts_recording` |
| Файл на диске | Любая строка `ts_file_data` с его `stored_file_name` |

Каждый пакет строк удаляется отдельной транзакцией через Spring Data JPA репозиторий. Первый JPQL-запрос выбирает
ограниченную страницу идентификаторов сирот по возрастанию `id`. Второй массово удаляет выбранные
строки, повторяя проверку отсутствия ссылок. Ошибка БД прекращает проход.
Файловый этап начинается после фиксации всех пакетов строк и проверяет ключи пакетами. Ошибка этой проверки
останавливает дальнейшее удаление файлов. Ошибка удаления файла записывается в лог; следующий проход повторит попытку.

Проход обходит только обычные файлы с каноническими UUID-именами непосредственно в настроенных каталогах.
Символические ссылки, вложенные каталоги и посторонние имена пропускаются. Совпадающие физические каталоги
обходятся один раз, отсутствующие не создаются. Сохранённый ключ защищает одноимённые файлы во всех каталогах.

Версии Ресурсов, Посылки, Вердикты, Решения, логи и видеозаписи проход не удаляет.
Неприкреплённые версии Ресурсов, самостоятельные Решения, логи и видеозаписи сохраняют свои файлы.
Файлы неудачных транзакций и зависимости удалённых владельцев могут оставаться до очередной очистки.
Падение между удалением строки и файла оставляет файл для следующего прохода; отсутствие уже удалённого файла допустимо.

## Загруженные Ресурсы Задачи

Адаптеры Условий, Упражнений, Полигонов и Авторских Решений возвращают историю существующими доменными сущностями.
Сборка сущностей и переименование через `update` не читают содержимое файлов.
Их `findFileRef` читает только метаданные файла и возвращает существующий `StoredBlobRef`, не обращаясь к `FileBlobStorage`.

`TaskData.uploadedResources` хранится в `ts_version_bucket_to_task`: составной ключ включает `task_id`
и `version_bucket`, а уникальное ограничение на `version_bucket` исключает одновременную принадлежность
цепочки двум Задачам. В таблицах версий Ресурсов `version_bucket` остаётся UUID.

`TaskPersistenceAdapter` сохраняет и синхронизирует строки принадлежности отдельно от ревизий содержимого.
Удаление Задачи очищает эти строки, сохраняя версии Ресурсов и их файлы. Таблица хранит только текущую
принадлежность; история владельцев цепочки не сохраняется. Операций переноса или удаления Ресурсов пока нет.

## Коды-доступа и Пароли Пользователей

Код-доступа есть только у Пользователей с Фиксированной Ролью, Пароль — только у Пользователей с Нефиксированными
Ролями. `ts_user.access_token_hash_algorithm` хранит алгоритм строкой из `HashAlgorithmJpaEnum` рядом
с `access_token`, `ts_user.password_hash` — хеш Пароля
([User.kt](src/main/kotlin/tech/testsys/infra/database/internal/jpa/entity/user/User.kt)). Все три колонки допускают
`NULL`; какие из них заполнены, задаёт `CHECK` `ck_ts_user_type_credentials` в
[changelog.03-init-user.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.03-init-user.xml): у строки
`MULTIPLE_ROLE` заполнен только `password_hash`, у строки `SINGLE_ROLE` — только `access_token`
и `access_token_hash_algorithm`. `CHECK` `ck_ts_user_access_token_hash_algorithm` допускает в
`access_token_hash_algorithm` только `HMAC_SHA256`: `access_token` хранит HMAC-SHA-256 КД в hex. Значение
`IDENTITY` из `HashAlgorithmJpaEnum` используют только Коды-приглашения. Значений по умолчанию у колонок нет:
при записи они передаются явно.

Колонки и ограничения изменены в changeset'ах `1.0.0-03-1` и `1.0.0-03-init-user-checks`, поэтому их контрольные
суммы изменились. Базы окружений dev и e2e, созданные до этого изменения, пересоздаются командой `clean`,
см. раздел [«Остановка и очистка»](../../docs/project/deployment.md#остановка-и-очистка) deployment.md.

Маппинги Пользователей с Фиксированной Ролью объединяют значение и алгоритм в `AccessTokenHash` и передают его
в `storedAccessToken(hash)`; строку без КД они не собирают и бросают `IllegalArgumentException`. Маппинг Пользователя
с Нефиксированными Ролями так же требует `password_hash`. При записи поля переносятся из доменных данных
без хэширования. Ввод исходного КД и хеш Пароля описаны в разделе
[«Пользователи»](../../testsys-domain/README.md#пользователи).

Хеш КД детерминирован, поэтому ограничение `uk_ts_user_access_token` обеспечивает и уникальность исходных КД.
Хеширование описано в разделе [«Пользователи»](../../testsys-domain/README.md#пользователи).

`DatabaseConfiguration` создаёт бин `AccessTokenHasher` с ключом из обязательного свойства
`testsys.access-token.secret` длиной не меньше 64 символов
([DatabaseConfiguration.kt](src/main/kotlin/tech/testsys/infra/database/api/DatabaseConfiguration.kt)); где его
задают — в таблице раздела «Параметры приложения»
в [deployment.md](../../docs/project/deployment.md#параметры-приложения). Без свойства или с более коротким
значением контекст не запускается. После смены секрета сохранённые КД Участников и
Наблюдателей перестают находиться. Тесты модуля задают фиксированный секрет в
`src/test/resources/application.properties`.

`AbstractUserPersistenceAdapter.findByAccessToken` хеширует исходный КД этим бином и ищет строку `ts_user`
по полученному значению и алгоритму
([AbstractUserPersistenceAdapter.kt](src/main/kotlin/tech/testsys/infra/database/api/persistence/adapter/user/AbstractUserPersistenceAdapter.kt)).
Строку Пользователя другого вида метод не возвращает: `assembleSupported` адаптера собирает только строки своего вида.
Порт `MultipleRoleUserRepository` этот метод не объявляет: у Пользователей с Нефиксированными Ролями нет КД.

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

`CommunityPersistenceAdapter.saveWithIdAndInvites` вставляет новое Сообщество с заданным ID. Конфликт первичного
ключа откатывает всю транзакцию, включая оба Кода-приглашения.

`ClassPersistenceAdapter.addStudent` в одной транзакции повышает версии Класса и Ученика и добавляет строку
`ts_student_to_class`, если её нет; остальные данные Класса не меняются.
`MultipleRoleUserPersistenceAdapter.addCommunityMembership` в одной транзакции повышает версию Пользователя
и добавляет недостающие строки данных Роли и членства в Сообществе. Поэтому `update` со снимком, прочитанным
до этих методов, падает с конфликтом версий и не удаляет добавленные строки.
Роль выбирается по `CommunityRole`.
`removeCommunityMembership` повышает версию корня Пользователя и удаляет одну строку членства,
если она есть; строки данных Роли остаются.

## Регистрация, смена почты, восстановление доступа и поиск по почте

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

`AccessRestorationRequestPersistenceAdapter` хранит запросы восстановления доступа
в `ts_access_restoration_request`
([changelog.21-init-access-restoration-request.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.21-init-access-restoration-request.xml)).
Ограничение `uk_ts_access_restoration_request_user_id` не даёт сохранить второй запрос того же Пользователя,
а внешний ключ `fk_ts_access_restoration_request_user_id` удаляет запрос вместе с Пользователем. `update` берёт
`user_id` из сохранённой строки (KDoc `AccessRestorationRequestMapping.toJpaEntity(entity, current)`
в [AccessRestorationRequestMapping.kt](src/main/kotlin/tech/testsys/infra/database/internal/mapping/user/AccessRestorationRequestMapping.kt)).
`findByUser` ищет запрос по идентификатору Пользователя. Код подтверждения хранится в исходном виде.

`MultipleRoleUserPersistenceAdapter.findByEmail` ищет строку `ts_user` по точному совпадению почты
и не возвращает строку Пользователя другого вида.

## Последний вход

Момент последнего входа хранится в колонке `ts_user.last_login_at`, которая допускает `NULL`
([changelog.03-init-user.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.03-init-user.xml)).
`UserPersistenceAdapter.recordLogin` записывает её запросом `update` по идентификатору и виду строки
(`UserJpaEntityRepository.updateLastLoginAt`). Перед записью адаптер защищает корень агрегата и повышает `version` по правилам
раздела «Транзакции и согласованность». Для отсутствующего Пользователя или Пользователя другого вида
он ничего не обновляет. Свойство `UserJpaEntity.lastLoginAt` помечено
`insertable = false, updatable = false`, поэтому `save` и `update` адаптеров Пользователей колонку не перезаписывают.
`UserPersistenceAdapter.findLastLogins` загружает строки переданных идентификаторов одним `findAllById` и пропускает
строки другого вида и строки без входа.

## Сообщества и Пользователи Администратора

`CommunityPersistenceAdapter.findByOwner` выбирает Сообщества владельца по `owner_id` в порядке идентификаторов.
`UserPersistenceAdapter.countAvailableToAdministrator` считает Пользователей по той же спецификации, по которой
`findAvailableToAdministrator` выбирает страницу, поэтому при тех же фильтрах число совпадает с общим числом
страницы. Правила доступа определены в `testsys.user.multi.admin.authorization`
в [features.md](../../docs/domain/features.md).

`UserPersistenceAdapter` реализует `SupervisorUserReadPort`: запрос выбирает всех Пользователей, кроме
Супервайзеров, применяет фильтры до подсчёта и разбиения на страницы и собирает Пользователей по видам.
Участники входят только в список без фильтра по Ролям и Сообществу. `CommunityPersistenceAdapter` реализует
`SupervisorCommunityReadPort` и возвращает все Сообщества по идентификатору.

## Схема БД

Именованные `CHECK` в init-changelog проверяют сочетания состояния и nullable-полей Посылки, Задачи,
Пользователя и запроса валидации, результат Авторской Посылки, перечисления и виды диагностик.
Число оставшихся попыток, позиции и Судейский балл неотрицательны.
Матрицы допустимых и недопустимых `INSERT`/`UPDATE` проверяет
[SchemaConstraintsTests](src/test/kotlin/tech/testsys/infra/database/internal/jpa/SchemaConstraintsTests.kt).
Принадлежность ревизии одной Задаче и соответствие Роли Кода-приглашения ссылке Сообщества
этими ограничениями не обеспечиваются.

`FileDataStorage.store` проверяет имя файла через `TextLimits` до чтения и записи блоба.
Превышение предела из [testsys.entity.textLimits](../../docs/domain/features.md#testsysentitytextlimits-implemented)
приводит к `IllegalArgumentException` без файлового I/O и записи метаданных.

- Схемой управляет Liquibase: changelog'и лежат в `src/main/resources/db/changelog/changes/<версия>/`.
- Hibernate запускается с `ddl-auto=validate`, поэтому каждое изменение JPA-сущности требует changeset.
- Диалект SQL не задаётся: Hibernate определяет его по соединению.
- Имена таблиц и колонок вычисляет `TestsysPhysicalNamingStrategy`.
- `SchemaValidationTests` применяет changelog'и к PostgreSQL и выполняет ту же валидацию. Тест также проверяет
  частичный индекс очереди судьи `ix_ts_submission_judge_queue`: его changeset выполняется только на PostgreSQL
  (`dbms="postgresql"`).

Правила написания changeset — в шаге 7 [implement-entity.md](../../docs/guides/implement-entity.md).

Версию Liquibase задаёт BOM Spring Boot (раздел «Сборка» в [structure.md](../../docs/project/structure.md)).
С версии 5.0 Liquibase Community распространяется под Functional Source License (FSL) вместо Apache 2.0.
Использование в разработке, тестах и продакшене свободное; запрещено только строить на Liquibase конкурирующий
коммерческий сервис. Каждая версия через два года после выхода переходит на Apache 2.0.

## Хранение первого входа

Адаптеры `ParticipantContestEntryPersistenceAdapter` и `StudentContestEntryPersistenceAdapter`
сохраняют неизменяемые записи первого входа.
Метод `findOrCreate` проверяет, что Пользователь существует, читает полный контекст
и возвращает существующую запись либо сохраняет новую. Блокировок метод не берёт.
Единственность записи обеспечивают уникальные ограничения контекста: параллельная вторая вставка нарушает
ограничение (SQLSTATE `23505`), транзакция повторяется и находит уже сохранённую запись.

Таблицы и внешние ключи добавлены в
[changelog.14-init-study-entry.xml](src/main/resources/db/changelog/changes/1.0.0/changelog.14-init-study-entry.xml).
Новые внешние ключи удаляют записи каскадно при удалении родителей.
Удаление членства и связей группы с Туром не затрагивает эти таблицы.
Типы контекстов и порты — в разделе [«Записи первого входа»](../../testsys-domain/README.md#записи-первого-входа).

## Тесты

Интеграционные тесты модуля работают с PostgreSQL 17 в контейнере Testcontainers, поэтому для них нужен
запущенный Docker. `PostgresTestContainer` в тестовых исходниках запускает один контейнер на тестовую JVM,
а Testcontainers останавливает его при выходе из JVM. `PostgresTestConfiguration` подключает к контейнеру
Spring-контекст через `@ServiceConnection`.

Тесты на основе `DatabaseIntegrationTests` используют общий кэшированный Spring-контекст. После каждого теста
базовый класс очищает все таблицы `ts_*` текущей схемы одной командой `TRUNCATE … CASCADE`.

## Служебные настройки

`DatabaseSettingsStore` сохраняет один технический набор в `ts_runtime_settings`; доменных сущностей
и связей у таблицы нет. `set_key` уникален; `revision` растёт при каждой записи, `change_id` идентифицирует
commit. Чтение и CAS-запись выполняются в завершённых `REQUIRES_NEW` транзакциях. Повтор охватывает только
работу с БД. Полный протокол компенсации и восстановления — в [settings](../settings/README.md).

`DatabaseGradingNodeStore` хранит реестр Проверяющих узлов в `ts_grading_node`: адрес и `is_active`, без доменных
сущностей и связей. Ограничение `uk_ts_grading_node_address` запрещает повтор адреса. Каждая операция выполняется
в завершённой `REQUIRES_NEW` транзакции, повтор охватывает только работу с БД. Операции идемпотентны: при гонке
первой вставки повтор находит строку и сравнивает её с добавляемой. Протокол применения реестра — в
[grpc](../grpc/README.md#реестр-узлов).

## Служебный поиск Пользователей

`DatabaseServiceUserSearch` в пакете `api/service` реализует `ServiceUserSearch` для служебного кабинета. Поиск
находит Пользователей всех видов и не применяет правила доступа Кабинетов. Правила условий определены в
[testsys.dev.service.userSearch](../../docs/domain/features.md#testsysdevserviceusersearch-implemented).

`find` строит одну `Specification` над строками `ts_user`. Каждое условие — предикат строки Пользователя или
подзапрос `EXISTS`; связанные таблицы соединяются только внутри подзапроса, поэтому строка попадает в результат
один раз при любом числе оснований. Общее число считается той же спецификацией. Псевдоним и почта сравниваются
через `locate` и `lower`. Сортировать можно только по `id` и `name`: другое поле приводит
к `IllegalArgumentException`. Если сортировки по идентификатору нет, порядок дополняется `id` по возрастанию.

Если заданы и Роли, и Сообщество, их заменяет одно условие: для каждой выбранной Роли подзапрос `EXISTS`
проверяет, что Пользователь имеет её в этом Сообществе. Для Нефиксированной Роли подзапрос ищет членство
в Сообществе в этой Роли, для Роли Наблюдателя — Наблюдателя Сообщества. Для Роли Администратора подходит
и создатель Сообщества. Для Ролей Участника и Супервайзера подзапроса нет: по ним Пользователь не находится.

Выбранные строки собираются по виду: строки Пользователей с Нефиксированными Ролями собирает
`MultipleRoleUserRepository`, строки Пользователей с Фиксированными Ролями — адаптеры Участников, Наблюдателей
и Супервайзеров. `findById` собирает так же Пользователя любого вида или возвращает `null`. Оба метода читают
в транзакции с `readOnly = true` и возвращают доменные сущности; страница `find` содержит переданные параметры
пагинации.

## Служебная регистрация версий TRIK Studio

`DatabaseServiceTrikStudioVersions` в пакете `api/service` реализует `ServiceTrikStudioVersions` для служебного
кабинета. Требования к регистрации — в
[testsys.dev.service.trikStudioVersions](../../docs/domain/features.md#testsysdevservicetrikstudioversions-implemented).

`findAll` возвращает версии через `ContestRepository.findTrikStudioVersions` в транзакции с `readOnly = true`.
`register` отбрасывает пробелы по краям тега и номера и проверяет сначала номер, затем тег. Для каждого значения
проверяются пустота, длина в кодовых точках Unicode (не больше 255 для номера и 512 для тега) и занятость
в `ts_trik_studio_version`. Первая непройденная проверка возвращается как `Rejected` с причиной
`TrikStudioVersionRejection`, и строка не сохраняется. Иначе метод сохраняет строку и возвращает `Registered`.

`findUsedTags` одним запросом находит теги версий, на которые ссылаются `ts_contest`, `ts_submission`,
`ts_trik_studio_version_to_task_content` или `ts_trik_studio_version_to_task_validation_request`.
`changeTag` находит строку по прежнему тегу, проверяет новый тег так же, как `register`, и меняет только колонку
`tag`; совпадение с прежним тегом принимается без проверок. `delete` удаляет строку, если её тега нет среди
используемых, иначе возвращает `InUse`. Для отсутствующего тега оба метода возвращают `NotFound`.

Параллельная регистрация или смена тега на то же значение нарушает уникальное ограничение (SQLSTATE `23505`).
Транзакция повторяется по разделу [«Повтор транзакций»](#повтор-транзакций), и повтор отклоняет значение как уже
зарегистрированное. Если версию начали использовать параллельно с удалением, удаление нарушает внешний ключ,
и транзакция откатывается без изменений.
