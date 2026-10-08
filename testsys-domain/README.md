# Модуль `testsys-domain`

Документ описывает, как устроен доменный модуль: модель сущностей, порты и DSL билдеров. Он предназначен для всех,
кто пишет код, работающий с доменными объектами, — в домене, операциях или инфраструктуре.

Место модуля в архитектуре — в [structure.md](../docs/project/structure.md), термины предметной области —
в [definitions.md](../docs/domain/definitions.md). Пошаговое добавление сущности —
в [implement-entity.md](../docs/guides/implement-entity.md), нового порта — в [implement-port.md](../docs/guides/implement-port.md).

## Пакеты

| Пакет                                  | Что лежит                                                          |
|----------------------------------------|--------------------------------------------------------------------|
| `tech.testsys.domain.model`            | Сущности и их данные, по подпакетам `entry`, `group`, `task`, `user`        |
| `tech.testsys.domain.contract`         | Порты: интерфейсы, через которые домен обращается к внешнему миру  |
| `tech.testsys.domain.builder`          | DSL билдеров: билдеры, точки входа `api`, `Chooser`'ы в `util`     |

## Модель

Базовые типы — в [DomainEntity.kt](src/main/kotlin/tech/testsys/domain/model/DomainEntity.kt).

- Каждая сущность наследует `DomainEntity<Id>`. Идентификатор — `@JvmInline value class`, реализующий `DomainId`
  (`TaskId`, `ContestId`, …). Равенство сущностей — по классу и `id`.
  Исключение: `SingleRoleUserId` общий для `Participant`, `Observer` и `Supervisor`.
- Сущность неизменяема и разделена на `X(id, createdAt, data)` и полезную нагрузку `XData`.
  Обновление создаёт новый экземпляр через `X.withData { … }`, который переносит токен `version`.
- `version: EntityVersion?` — непрозрачный токен оптимистической блокировки. В конструктор он не входит:
  это отдельное свойство, сеттер которого `internal` для `testsys-domain`, а снаружи токен попадает в сущность
  через поле `version` билдера. По соглашению это поле в продуктовом коде заполняет только модуль хранения.
  У сущности, не полученной из хранилища, токен равен `null`, и `update` такой сущности падает.
  Домен передаёт токен обратно в `update` и никогда не сравнивает и не изменяет. Устаревший токен приводит
  к ошибке `update`.
- Часть полей `XData` фиксируется при создании: `owner` у Задачи, Тура, Класса, Соревнования и Сообщества;
  `author`, `solution`, `task` и `kind` со всеми его полями у Посылки; `judge` и `submission`
  у Судейского вердикта (`JudgmentOrder`); `versionBucket` у Полигона, Условия, Упражнения и Авторского
  Решения; `invite` у Класса; `managerInvite` и `developerInvite` у Сообщества; `email` у запроса регистрации
  (`RegistrationRequest`); `user` у запроса смены почты (`EmailChangeRequest`); `competition` у Участника.
  `save` их записывает, а `update` игнорирует значение из переданной сущности и оставляет сохранённое.
  В KDoc такое поле помечено «fixed on creation and ignored on update».
- Ещё часть полей фиксируется при создании так, что `update` с другим значением падает: `file` у Полигона,
  Условия и Упражнения, `language` у Упражнения, `solution` и `expectedScore` у Авторского Решения.
  Такой `update` бросает `UnsupportedOperationException` и ничего не сохраняет. В KDoc такое поле помечено
  «fixed on creation; update fails if it differs». Зачем это нужно — в разделе
  [Версионирование Ресурсов](#версионирование-ресурсов).
- Если при создании фиксируются все поля сущности, её `update` не поддерживается и бросает
  `UnsupportedOperationException`. Это Решение (`Solution`: `file` и `language`), логи (`Logs`) и запись
  (`Recording`), у которых единственное поле — `file`.
- У Вердикта (`Verdict`) при создании фиксируются все поля — `task`, `submission` и `testVerdicts`, поэтому
  `update` Вердикта не поддерживается: новая проверка Посылки создаёт новый Вердикт. Результат по каждому
  Полигону — это значение `TestVerdict` внутри `VerdictData`.
- `FileData` после сохранения не меняется: изменённый файл сохраняется как новый.
- Сущности не хранят ссылок на другие сущности. Связь — это `LazyEntity<Id, E>` или `LazyEntityList<Id, E>`:
  внутри только идентификаторы, разрешаются они через `EntityLoader.load(...)`. Создаются через `id.lazify()`
  и `ids.lazify()`.
- Варианты состояния моделируются sealed-иерархиями: `TaskContent` (`New` / `Uncommitted` / `Committed`),
  `SubmissionStatus`, `SubmissionKind`, `GradingResult`, `TrikSupportedLanguage`. Сущность Кода-приглашения
  в Сообщество — sealed-класс `CommunityInvite` с вариантами `Manager` и `Developer`.

### Запросы валидации Задач

[`TaskValidationRequest`](src/main/kotlin/tech/testsys/domain/model/task/TaskValidationRequest.kt) сохраняет
неизменяемый снимок непосредственных входных данных.
`TaskValidationExecution.PendingDiagnostics` означает незавершённые Диагностики,
в том числе при наличии сохранённых результатов отдельных Полигонов.
`TaskValidationExecution.Completed` завершает обработку
и содержит время завершения, Диагностики, упорядоченные ссылки на Авторские Посылки
и список проваленных Посылок `AuthorSubmissionFailure`; пустой список означает успех.
Порядок Посылок задаёт `TaskValidationSnapshot.authorRuns`: каждая пара Авторского Решения и версии TRIK Studio.
Условия успешного тестирования описаны в
[features.md](../docs/domain/features.md#testsysusermultidevelopertasktesttask-partially-implemented).

### Версионирование Ресурсов

Версионируются сущности с `versionBucket`: Полигон (`Test`), Условие (`Statement`), Упражнение (`Exercise`)
и Авторское Решение (`DeveloperSolution`). Поле имеет тип `VersionBucket`, оборачивающий UUID; это значение,
а не идентификатор доменной сущности.

- Версии одного Ресурса — отдельные сущности с общим `versionBucket`. Сущность — это одна версия: её файл
  (у Упражнения ещё и язык, у Авторского Решения — ссылка `solution` и ожидаемый Вердикт `expectedScore`)
  после создания не меняется.
- Новая версия — это новая сущность, сохранённая через `save` с `versionBucket` прежней версии; прежняя версия
  и её файл остаются без изменений. `save` не проверяет, есть ли уже сущности с таким `versionBucket`.
- Версии Авторского Решения могут ссылаться на одно и то же Решение (`Solution`): версия с новым ожидаемым
  Вердиктом и прежней программой переиспользует `solution` прежней версии.
- `update` меняет у версии только название и описание. Как падает попытка сменить зафиксированное поле —
  в разделе [Модель](#модель).
- `TaskContent` ссылается на идентификаторы сущностей, поэтому каждая ревизия Задачи закрепляет конкретные
  версии Ресурсов вместе с их зафиксированными полями.
- Название и описание ревизией Задачи не закрепляются: `update` меняет их у версии на месте, и новое значение
  видно во всех ревизиях, которые ссылаются на эту версию, в том числе в зафиксированной.

`TaskData.uploadedResources` хранит набор идентификаторов цепочек версий, загруженных в Задачу. Он включает Ресурсы,
не использованные в `TaskContent`. Содержимое по-прежнему ссылается на конкретные версии через их идентификаторы.
Билдер копирует набор при сборке, а `withData` сохраняет его и позволяет изменить копию. Проверки допустимости
прикрепления выполняют операции; `TaskRepository.update` допускает произвольное изменение набора. Требования
к принадлежности и прикреплению Ресурсов — в разделе `testsys.entity.task` в
[features.md](../docs/domain/features.md).

### Ограничения текста

[`TextLimits`](src/main/kotlin/tech/testsys/domain/model/TextLimits.kt) проверяет длину названий и имён файлов
в кодовых точках Unicode. Пределы определены в
[testsys.entity.textLimits](../docs/domain/features.md#testsysentitytextlimits-implemented).
Операции и инфраструктурные адаптеры вызывают эти проверки до сохранения.

### Содержимое файлов

`FileData` в [Common.kt](src/main/kotlin/tech/testsys/domain/model/task/Common.kt) содержит имя файла и
`FileContent`: байты `Inline` либо ссылку `Stored` с видом хранилища `FileStorageKind`.
Конструктор с `ByteArray` создаёт `Inline`; равенство `FileData` остаётся ссылочным.
Билдеры принимают готовый файл, а `withData` переносит его без чтения.

Порт `FileContentReader` из [File.kt](src/main/kotlin/tech/testsys/domain/contract/File.kt) явно читает байты.
Ссылка не зависит от транзакции; техническая ошибка отсутствующего блоба возникает при чтении.

### Пользователи

- `MultipleRoleUser` владеет набором `CompatibleUserRole`: `Developer`, `Student`, `Administrator`, `Judge`, `Manager`.
- `SingleRoleUser` — один из `Participant`, `Observer`, `Supervisor`.

Смысл Фиксированных и Нефиксированных Ролей — в [definitions.md](../docs/domain/definitions.md).

`UserData.accessTokenHash` хранит представление Кода-доступа и алгоритм в одном объекте `AccessTokenHash`
([User.kt](src/main/kotlin/tech/testsys/domain/model/user/User.kt)). Сейчас доступен только `HashAlgorithm.Identity`:
он оставляет Код-доступа без изменений, поэтому сохранённое значение совпадает с исходным КД.
`Identity` — временное решение и не обеспечивает криптографическую защиту.

Объявления, поведение которых зависит от совпадения сохранённого значения с исходным КД, помечаются
`@RawAccessTokenDependency` с объяснением зависимости в обязательном параметре `reason`
([RawAccessTokenDependency.kt](src/main/kotlin/tech/testsys/domain/model/user/RawAccessTokenDependency.kt)).
При замене `Identity` все отмеченные объявления должны быть проверены и исправлены до включения нового алгоритма.
Простое копирование значения между объектами такой зависимости не создаёт. Методы,
показывающие исходный КД, должны получить эту аннотацию.

Все четыре билдера данных Пользователей наследуют `UserDataBuilder`
([UserBuilder.kt](src/main/kotlin/tech/testsys/domain/builder/user/UserBuilder.kt)).
Метод `accessToken(rawAccessToken, algorithm)` вызывает функцию `AccessTokenHash.hashAccessToken`
([User.kt](src/main/kotlin/tech/testsys/domain/model/user/User.kt)).
Она применяет переданный алгоритм и возвращает значение вместе с ним в `AccessTokenHash`.
Алгоритм исходного КД передаётся явно; сейчас доступен только `HashAlgorithm.Identity`.
Метод `storedAccessToken(hash)` принимает готовый `AccessTokenHash` без хэширования.
`build()` проверяет наличие объекта, но не хэширует его.

`withData` переносит сохранённый `AccessTokenHash`. Для замены КД вызывается `accessToken(newToken, algorithm)` внутри блока `withData`.
Конструкторы классов данных и их `copy` принимают готовый `AccessTokenHash`.

Порты хранения Пользователей `MultipleRoleUserRepository`, `ParticipantRepository`, `ObserverRepository`
и `SupervisorRepository` расширяют `UserAccessTokenFinder`
([UserRepositories.kt](src/main/kotlin/tech/testsys/domain/contract/persistence/repository/UserRepositories.kt)).
Его метод `findByAccessToken` принимает исходный КД и возвращает Пользователя своего вида, которому этот КД
присвоен сейчас, или `null`. КД сравнивается без нормализации. Хэширование и сравнение выполняет адаптер хранения.

`MultipleRoleUserRepository.findByEmail` и `RegistrationRequestRepository.findByEmail` сравнивают почту точно,
без нормализации; нормализует почту операция.

`UserRepository.recordLogin` записывает момент последнего входа Пользователя любого вида. Этот момент не входит
в данные Пользователя, но его запись повышает токен `version` корня агрегата. `UserRepository.findLastLogins` читает эти моменты
сразу для нескольких Пользователей.

`MultipleRoleUserRepository.addCommunityMembership` включает Пользователя в Сообщество в Роли из перечисления
[`CommunityRole`](src/main/kotlin/tech/testsys/domain/model/user/CommunityRole.kt): Организатора, Разработчика
или Ученика. Роль выбирает операция: по Коду-приглашению или по решению Администратора.

### Запросы регистрации

[`RegistrationRequest`](src/main/kotlin/tech/testsys/domain/model/user/RegistrationRequest.kt) хранит
незавершённую регистрацию: почту, код подтверждения, момент окончания его срока и число оставшихся попыток.
Для одной почты хранится не более одного запроса. Роль, которую Пользователь выбирает при подтверждении
регистрации, задаётся перечислением
[`RegistrationRole`](src/main/kotlin/tech/testsys/domain/model/user/RegistrationRole.kt).

### Запросы смены почты

[`EmailChangeRequest`](src/main/kotlin/tech/testsys/domain/model/user/EmailChangeRequest.kt) хранит
незавершённую смену почты Пользователя без Фиксированной Роли: Пользователя, новую почту, код подтверждения,
момент окончания его срока и число оставшихся попыток. У одного Пользователя хранится не более одного запроса;
`EmailChangeRequestRepository.findByUser` находит запрос по Пользователю. Запрос не резервирует новую почту:
одну и ту же почту могут указать запросы разных Пользователей.

### Коды-приглашения

[`ClassInvite`](src/main/kotlin/tech/testsys/domain/model/group/ClassInvite.kt) хранит Код-приглашение
в Класс. [`CommunityInvite`](src/main/kotlin/tech/testsys/domain/model/group/CommunityInvite.kt) хранит
Код-приглашение в Сообщество; вариант `CommunityInvite.Manager` или `CommunityInvite.Developer` задаёт Роль.
Операции получают Роль селектором `CommunityInvite.Kind` и выбирают вариант через `when` без `else`.
Поле `expiresAt` хранит момент окончания срока. Правила срока, замены и продления определены
в `testsys.entity.invite` в [features.md](../docs/domain/features.md).

Ссылку хранит только группа: `ClassData.invite`, `CommunityData.managerInvite` и `CommunityData.developerInvite`
— обязательные `LazyEntity`, а данные Кода-приглашения не ссылаются на Класс или Сообщество.
Группу по Коду-приглашению находят `ClassRepository.findByInvite` и `CommunityRepository.findByInvite`.
`ClassRepository.saveWithInvite` и `CommunityRepository.saveWithInvites` в одной транзакции сохраняют
Коды-приглашения и группу, данные которой строятся по их идентификаторам. Обычный `save` группы принимает данные
со ссылками на уже сохранённые Коды-приглашения.

Каждый вариант `CommunityInvite` хранится через свой порт: `ManagerCommunityInviteRepository`
и `DeveloperCommunityInviteRepository` наследуют `CommunityInviteRepository<Invite>`, и Коды-приглашения
другого варианта в них отсутствуют. Заменяется и продлевается Код-приглашение через `update` его порта.

Поле `codeHash` хранит представление Кода-приглашения и алгоритм в одном объекте `InviteCodeHash`
([InviteCodeHash.kt](src/main/kotlin/tech/testsys/domain/model/group/InviteCodeHash.kt)).
Функция `InviteCodeHash.hashInviteCode` применяет переданный алгоритм без нормализации ввода.
Как и для КД, сейчас доступен только `HashAlgorithm.Identity`: сохранённое значение совпадает с исходным кодом.

`ClassInviteDataBuilder` и `CommunityInviteDataBuilder` наследуют `InviteCodeDataBuilder`
([InviteCodeDataBuilder.kt](src/main/kotlin/tech/testsys/domain/builder/group/InviteCodeDataBuilder.kt)).
Метод `code(rawInviteCode, algorithm)` хэширует исходный код, `storedCode(hash)` принимает готовый `InviteCodeHash`.
Варианты `CommunityInvite` строятся через `managerCommunityInvite { … }` и `developerCommunityInvite { … }`,
у каждого варианта свой `withData`, сохраняющий вариант. `withData` переносит сохранённый `InviteCodeHash`;
для замены кода внутри блока `withData` вызывается `code(newCode, algorithm)`.

Объявления, поведение которых зависит от совпадения сохранённого значения с исходным Кодом-приглашением,
помечаются `@RawInviteCodeDependency` с обязательным параметром `reason`
([RawInviteCodeDependency.kt](src/main/kotlin/tech/testsys/domain/model/group/RawInviteCodeDependency.kt)).
Аннотацию получают методы, которые возвращают выданный код, и методы, которые ищут запись по введённому коду.
При замене `Identity` все отмеченные объявления должны быть проверены и исправлены до включения нового алгоритма.

## Порты

- `EntityRepository<Data, Id, Entity>` объединяет `EntityFinder`, `EntityLoader`, `EntitySaver` и `EntityRemover`
  ([Common.kt](src/main/kotlin/tech/testsys/domain/contract/persistence/repository/Common.kt)). Порты хранения
  конкретных сущностей объявлены в `contract/persistence/repository/*Repositories.kt`.
- `FileBlobStorage` получает путь (`java.nio.file.Path`) в каждом вызове `store`, `load` и `delete`.
  `StoredBlobRef` действителен только вместе с путём, по которому создан блоб.
- Остальные порты и их состояние — в разделе «Убедиться, что порт нужен»
  в [implement-port.md](../docs/guides/implement-port.md).

## DSL билдеров

- Точки входа — в `builder/api/{Entry,Group,Task,User}Api.kt`: `task { … }`, `taskData { … }`, `submission { … }`,
  `X.withData { … }`.
- Для полей sealed-типов используются `Chooser`'ы (`LanguageChooser`, `SubmissionStatusChooser`, …)
  из `builder/util/chooser`.
- Билдер проверяет обязательные поля и бросает `IllegalArgumentException` в `build()`.
- Доменные объекты за пределами домена, в том числе в маппингах инфраструктуры, создаются **только через DSL**.

## Записи первого входа

Первый вход хранится в неизменяемых сущностях:

| Сущность | Контекст |
|----------|----------|
| [ParticipantContestEntry](src/main/kotlin/tech/testsys/domain/model/entry/ParticipantContestEntry.kt) | Участник, Соревнование и Тур |
| [StudentContestEntry](src/main/kotlin/tech/testsys/domain/model/entry/StudentContestEntry.kt) | Пользователь, Класс и Тур |

Связи и поле `enteredAt` фиксируются при создании; `update` бросает `UnsupportedOperationException`.
Порты объявлены в [EntryRepositories.kt](src/main/kotlin/tech/testsys/domain/contract/persistence/repository/EntryRepositories.kt).
Метод `findOrCreate` сохраняет первый момент атомарно, а поиск по контексту и пакетный поиск по Турам не меняют записи.
Правила доступа и первого входа определены в `testsys.entity.studyEntry` в [features.md](../docs/domain/features.md).
