# Модуль `testsys-operation`

Документ описывает, как устроен модуль операций: где лежат операции, как они связаны с фичами и Ролями, в каких
транзакциях выполняются, как настраиваются значения, которые операциям нужны, как операции сообщают об ошибках
и какие вспомогательные функции есть в модуле. Он предназначен для всех, кто реализует или вызывает пользовательские фичи.

Место модуля в архитектуре — в [structure.md](../docs/project/structure.md), сами фичи — в
[features.md](../docs/domain/features.md), доменная модель и порты — в
[testsys-domain/README.md](../testsys-domain/README.md). Пошаговая реализация фичи — в
[implement-feature.md](../docs/guides/implement-feature.md), правила тестов — в
[unit-tests.md](../docs/project/unit-tests.md).

## Пакеты

| Пакет                                  | Что лежит                                                                     |
|----------------------------------------|-------------------------------------------------------------------------------|
| `tech.testsys.operation.user`          | Классы операций `<Actor>Operations`, см. раздел «Операции»                    |
| `tech.testsys.operation.config`        | Интерфейсы конфигурации операций, см. раздел «Конфигурация»                   |
| `tech.testsys.operation.error`         | Модель результата и ошибок операций                                           |
| `tech.testsys.operation.annotation`    | Аннотации `@Feature`, `@OperationConfig`, `@ConfigProperty` и opt-in-маркер `@InternalOperationsApi` |
| `tech.testsys.operation.util`          | Вспомогательные функции для операций                                          |

## Публичный и внутренний API

Снаружи модуля используются классы `<Actor>Operations`, `OperationResult` с `getOrThrow()` и `getOrElse { … }`,
`OperationException` и ошибки из [Errors.kt](src/main/kotlin/tech/testsys/operation/error/Errors.kt).

Всё, что нужно только для написания операций, помечено `@InternalOperationsApi` (`@RequiresOptIn`): `operation`,
`Raise`, `OperationFailure`, `raise()`, `ensure`, `getOrRaise`, `asSuccess()` и все функции из `util`. Класс операций
и тестовый класс, который использует эти объявления напрямую, объявляют `@OptIn(InternalOperationsApi::class)`; новая
вспомогательная функция для операций сама помечается `@InternalOperationsApi`.

## Операции

- **Операция** — публичный метод класса `<Actor>Operations`, реализующий одну пользовательскую фичу из раздела
  `testsys.user` в [features.md](../docs/domain/features.md). Метод помечается `@Feature("<кодификатор>")`.
- `<Actor>` — тот, кому доступна фича: Роль или группа Пользователей. Класс выбирается по самому длинному
  подходящему префиксу кодификатора фичи из таблицы ниже; смысл префиксов — в разделе `testsys.user`
  в [features.md](../docs/domain/features.md).
- Зависимости операций — порты из `tech.testsys.domain.contract` и интерфейсы конфигурации из
  `tech.testsys.operation.config` (см. раздел «Конфигурация») — передаются через конструктор класса.
- Первый параметр операции — Пользователь, который её выполняет; остальные — входные данные фичи.
  Исключения — `UserOperations.authenticate`, `UserOperations.requestRegistration` и
  `UserOperations.confirmRegistration`: до входа Пользователь неизвестен, поэтому операции принимают только
  входные данные фичи (см. разделы «Вход в Систему» и «Регистрация»).
- Операция возвращает `OperationResult<T, <Operation>Error>`.

| Префикс кодификатора                | Класс                        |
|-------------------------------------|------------------------------|
| `testsys.user.*`                    | `UserOperations`             |
| `testsys.user.single.*`             | `SingleRoleUserOperations`   |
| `testsys.user.single.participant.*` | `ParticipantOperations`      |
| `testsys.user.study.*`              | `StudyOperations`            |
| `testsys.user.single.observer.*`    | `ObserverOperations`         |
| `testsys.user.multi.*`              | `MultipleRoleUserOperations` |
| `testsys.user.multi.developer.*`    | `DeveloperOperations`        |
| `testsys.user.multi.student.*`      | `StudentOperations`          |
| `testsys.user.multi.judge.*`        | `JudgeOperations`            |
| `testsys.user.multi.admin.*`        | `AdministratorOperations`    |
| `testsys.user.multi.manager.*`      | `ManagerOperations`          |

Образец — [DeveloperOperations.kt](src/main/kotlin/tech/testsys/operation/user/DeveloperOperations.kt).

## Проверка длины текста

`DeveloperOperations` проверяет названия при создании и редактировании Задач и Туров,
а также названия и имена файлов при добавлении и обновлении всех видов Ресурсов.
Оба варианта `StudyOperations.sendSolution` проверяют имя файла Решения.
Проверки выполняются после доступа и существующих ограничений, до первого сохранения.
Превышение пределов из [testsys.entity.textLimits](../docs/domain/features.md#testsysentitytextlimits-implemented)
возвращает `TaskNameTooLongError`, `ContestNameTooLongError`, `ResourceNameTooLongError`
или `UploadedFileNameTooLongError` из [Errors.kt](src/main/kotlin/tech/testsys/operation/error/Errors.kt).

## Транзакции

Модуль операций транзакциями не управляет: транзакцию открывает вызывающий код.

- Одна операция выполняется в одной транзакции.
- **Операция вызывается только через транзакционный сервис.** В `testsys-web` это прокси-сервисы с `@Transactional`;
  методы чтения выполняются с `readOnly = true`, см. раздел
  [Прокси-сервисы](../testsys-web/app/README.md#прокси-сервисы) в app/README.md.
- `TaskValidationOperations` и `TaskValidationDispatcher` работают вне транзакций сервисов: каждый вызов порта
  хранения выполняется в своей транзакции адаптера.
- Конфликт с параллельной транзакцией повторяет модуль хранения: он выполняет всю транзакцию сервиса заново,
  поэтому тело операции может выполниться несколько раз. Правила повтора — в разделе
  [«Транзакции и согласованность»](../testsys-infra/database/README.md#транзакции-и-согласованность)
  в database/README.md.
- Действия вне БД выполняются после фиксации транзакции: Посылку грейдеру передаёт `AfterCommitGrader`
  в `testsys-web`, письма отправляет адаптер почты (см. [mail/README.md](../testsys-infra/mail/README.md)).

`ManagerOperations`, `AdministratorOperations`, `UserOperations`, `MultipleRoleUserOperations`, `ObserverOperations`
и `InviteRefresher` пока не подключены к приложению, и транзакционного сервиса у них нет.

## Обработка запросов валидации

`DeveloperOperations.testTask` проверяет доступ, рабочую версию и предварительные условия
[фичи testTask](../docs/domain/features.md#testsysusermultidevelopertasktesttask-partially-implemented).
После успешных проверок операция сохраняет или возвращает активный запрос и ставит его в очередь
`TaskValidationDispatcher`, не дожидаясь результатов.
Условие и Упражнения проверяются по рабочему содержимому и не входят в снимок проверки.
`DeveloperOperations.viewTaskValidationRequests` возвращает историю запросов Задачи.

[`TaskValidationOperations`](src/main/kotlin/tech/testsys/operation/TaskValidationOperations.kt)
обрабатывает один сохранённый запрос:

| Метод | Что выполняет |
|-------|---------------|
| `runDiagnostics(requestId)` | Анализирует Полигоны снимка и сохраняет каждый результат. |
| `proceed(requestId)` | Выполняет Диагностики, создаёт и отправляет Посылки, а когда все Посылки получили результат, завершает запрос с итогом. |
| `resendUnfinishedSubmissions(requestId)` | Повторно отправляет грейдеру Посылки без результата. Нужен после перезапуска приложения. |

Методы помечены `@Feature`: диагностический этап — кодификатором `runDiagnostics`,
остальные — кодификатором `testTask`.

Диагностический этап пропускает записанные результаты Полигонов и продолжает анализ после Error.
После сохранения всех результатов хранение завершает этап: Error переводит запрос в `StoppedByDiagnostics`,
иначе запрос переходит в `AwaitingSubmissions`.
Завершённые запросы и запросы с завершёнными Диагностиками не анализируются повторно.

На Авторском этапе `TaskValidationRequestRepository.createSubmissions` атомарно сохраняет отдельную Посылку
для каждого элемента `TaskValidationSnapshot.authorRuns`: по идентификатору Авторского Решения, затем по строке версии
TRIK Studio. Затем `proceed` отправляет Посылки грейдеру.
Пока хотя бы одна Посылка без результата, `proceed` возвращает активный запрос.
Когда результат есть у всех, `completeTesting` сохраняет все проваленные Посылки в том же порядке
как список `AuthorSubmissionFailure`. Пустой список означает успех.
Сумма баллов Посылки считается в `Long` и сравнивается с ожидаемым баллом её Авторского Решения.
Итог хранится в запросе и не пересчитывается.

Повторный вызов `proceed` не создаёт и не отправляет Посылки повторно.
Технические исключения выходят из `TaskValidationOperations`, сохранённый прогресс остаётся.
Обработка сохранённого снимка не зависит от последующей фиксации Задачи.

### Диспетчер

[`TaskValidationDispatcher`](src/main/kotlin/tech/testsys/operation/TaskValidationDispatcher.kt) вызывает
`TaskValidationOperations` на переданном `Executor`. Этот `Executor` должен выполнять задачи по одному:
так один экземпляр приложения обрабатывает каждый запрос монопольно.

- `schedule(requestId)` ставит запрос в очередь. Запрос, который ещё ждёт обработки, повторно не ставится.
- `start()` вызывается один раз при запуске приложения. Метод подписывается на результаты грейдера
  и для каждого активного запроса вызывает `resendUnfinishedSubmissions`, а затем ставит запрос в очередь.
  Результат Авторской Посылки ставит в очередь её активный запрос.

Исключение на любом шаге диспетчер записывает как техническую остановку через `recordTechnicalFailure`.
Приложение создаёт `Executor` и вызывает `start()`, см. раздел
[Прокси-сервисы](../testsys-web/app/README.md#прокси-сервисы) в app/README.md.

## Замена Кодов-приглашений

`ManagerOperations`, `AdministratorOperations`, `StudentOperations` и `MultipleRoleUserOperations` получают `Clock`
через конструктор. Создание Класса, создание, продление и замена Кода-приглашения читают время и срок действия
из `ClassInviteConfig` или `CommunityInviteConfig` не более одного раза; момент окончания усекается до микросекунд.
Правила срока, замены и продления определены в `testsys.entity.invite` в [features.md](../docs/domain/features.md).

Замену одного Кода-приглашения с истёкшим сроком выполняют операции Ролей:

| Операция | Что выполняет |
|----------|---------------|
| `ManagerOperations.refreshClassInvite(user, classId)` | Заменяет код и момент окончания Кода-приглашения в Класс. |
| `AdministratorOperations.refreshCommunityInvite(user, communityId, kind)` | Заменяет код и момент окончания Кода-приглашения в Сообщество для Роли, выбранной `CommunityInvite.Kind`. |

Операции помечены `@Feature`: замена в Классе — кодификатором `createInvite`, в Сообществе — кодификатором `inviteUser`.
Проверки те же, что при продлении: Роль, существование Класса или Сообщества, владение.
Операция заново читает Код-приглашение и проверяет срок; действующий Код-приглашение возвращается без изменений.
Технические исключения, в том числе конфликт версий, выходят к вызывающему коду.

[`InviteRefresher`](src/main/kotlin/tech/testsys/operation/InviteRefresher.kt) вызывает эти операции
на переданном `ScheduledExecutorService` от имени владельца: Организатора, создавшего Класс, или Администратора,
создавшего Сообщество. Группу Кода-приглашения он находит через `findByInvite` в `ClassRepository`
или `CommunityRepository`, владельца — через `MultipleRoleUserRepository`.
Если Код-приглашение, Класс или Сообщество удалены после поиска, замена пропускается.

- `start()` вызывается один раз при запуске приложения. Метод планирует два прохода — для Классов и для Сообществ.
  Первый проход выполняется сразу, следующие — через `refreshPeriod` соответствующей конфигурации после завершения
  предыдущего. Повторный вызов и неположительный период бросают `IllegalStateException`.
- Проход находит Коды-приглашения с истёкшим сроком через `findExpired` в `ClassInviteRepository` или
  в `ManagerCommunityInviteRepository` и `DeveloperCommunityInviteRepository` и заменяет каждый отдельно.
  Ошибка операции (например, `MissedManagerRoleError`, если владелец потерял Роль) и исключение записываются
  в `System.Logger` с уровнем `WARNING` и не останавливают ни проход, ни следующие проходы.
- `close()` останавливает `ScheduledExecutorService` через `shutdownNow()`; повторный вызов безопасен.

Приложение создаёт `ScheduledExecutorService` и вызывает `start()`; в `testsys-web` это пока не реализовано.

## Конфигурация

**Конфигурация операций** — значения, которые настраиваются при развёртывании системы и нужны самим операциям,
например предельное общее число Участников в Соревновании. Модуль не зависит от фреймворка приложения, поэтому
конфигурация объявляется в нём интерфейсом, а реализации живут снаружи модуля.

- Интерфейс конфигурации лежит в `tech.testsys.operation.config` и помечается `@OperationConfig("<имя конфигурации>")`.
- Интерфейс содержит только `val`-свойства без параметров.
- Конфигурации маленькие и разделены по областям; единой конфигурации на весь модуль нет.
- Интерфейс передаётся в `<Actor>Operations` через конструктор рядом с портами.

Ключ свойства складывается из сегментов: `testsys.operation.<имя конфигурации>.<имя свойства>`, где
`<имя конфигурации>` — значение `@OperationConfig`, а `<имя свойства>` по умолчанию — имя Kotlin-свойства
в kebab-case: `maxParticipants` → `max-participants`.

`@ConfigProperty(name, isDynamic)` ставится на свойство интерфейса конфигурации и уточняет ключ и способ получения
значения:

- `name` — сегмент ключа вместо выведенного из имени свойства; пустое значение (по умолчанию) означает kebab-case
  от имени свойства;
- `isDynamic = false` (по умолчанию) — значение фиксировано на всё время работы приложения и разрешается один раз
  при старте;
- `isDynamic = true` — значение может меняться от вызова к вызову, пока приложение работает.

Аннотация необязательна: свойство без неё — статическое, с выведенным именем сегмента.

**Динамическое свойство читается один раз за вызов операции**, в локальную переменную. Иначе два чтения внутри
одного вызова могут дать разные значения, и операция проверит ограничение по одному значению, а сообщит о другом.

Конфигурацией операций **не является**:

- значение, своё у каждой сущности (например, ограничение конкретного Сообщества), — это доменные данные или порт;
- технические детали адаптеров — таймауты, ретраи, SMTP, URL: они настраиваются там, где живёт адаптер.

Образец — [CompetitionConfig.kt](src/main/kotlin/tech/testsys/operation/config/CompetitionConfig.kt).

Реализации `CommunityConfig`, `EmailConfirmationConfig`, `CommunityInviteConfig`, `CompetitionConfig`
и `ClassInviteConfig` создаёт `testsys-web:app` из обязательных свойств с этими ключами, см. раздел
«Запуск и проверка» в [app/README.md](../testsys-web/app/README.md#запуск-и-проверка).

## Ошибки

Ожидаемый отказ, описанный фичей (нет доступа, сущность не найдена, нарушено ограничение), — это **ошибка
операции**, а не исключение. Нарушение контракта аргументов и внутреннего состояния по-прежнему оформляется
исключениями по разделу «Ошибки и проверки» в [code-style.md](../docs/project/code-style.md).
### Объявление ошибок

Все ошибки объявлены в [Errors.kt](src/main/kotlin/tech/testsys/operation/error/Errors.kt).

- У каждой операции свой `sealed interface <Operation>Error : OperationError` (`CreateTaskError`,
  `AttachStatementError`). Он перечисляет все отказы операции, и `when` по нему проверяется компилятором.
- Конкретная ошибка — `data object` или `data class` с данными для диагностики (`TaskNotExistsError(taskId)`).
  Она реализует интерфейсы **всех** операций, которые могут её вернуть; общие ошибки (`MissedDeveloperRoleError`)
  не дублируются.
- Имя конкретной ошибки называет нарушенное условие: `TaskAlreadyHasStatementError`, а не `AttachError`.

#### Общие типы ошибок

Интерфейс операции отвечает на вопрос «что может вернуть эта операция», а общий тип из региона `CommonTypes` —
«какого рода этот отказ». Общие типы нужны вызывающему коду, чтобы обрабатывать ошибки одинаково для всех
операций, не перечисляя конкретные классы: например, показать «не найдено» для любого `EntityNotExistsError`.

- Конкретная ошибка реализует общий тип **в дополнение** к интерфейсам операций, если её отказ относится к этому
  роду. Ошибка, нарушающая ограничение самой фичи (`TaskAlreadyHasStatementError`, `TaskNotCommittedError`),
  общего типа не имеет.
- Общий тип не заменяет интерфейс операции: операция возвращает `<Operation>Error`, а не общий тип.
- Новый общий тип заводится, когда один и тот же род отказа встречается в нескольких операциях и вызывающему
  коду нужно обрабатывать его единообразно.

### Выполнение

Тело операции оборачивается в `operation<T, E> { … }`
([OperationFailure.kt](src/main/kotlin/tech/testsys/operation/error/OperationFailure.kt)). Внутри:

| Функция                          | Что делает                                                                  |
|----------------------------------|-----------------------------------------------------------------------------|
| `ensure(condition, error)`       | Прерывает операцию с `error`, если `condition` ложно                        |
| `ensure(condition) { error }`    | То же, но ошибка создаётся только при отказе                                |
| `error.raise()`                  | Безусловно прерывает операцию с `error`                                     |
| `result.getOrRaise()`            | Возвращает значение успешного `result`; иначе прерывает операцию с его ошибкой |
| `result.getOrRaise { error -> … }` | То же, но операция прерывается ошибкой, в которую лямбда превращает ошибку `result` |

После `ensure(x != null) { … }` компилятор считает `x` не-`null` (smart cast).

Отказ реализован внутренним исключением `OperationFailure`, которое `operation` превращает
в `OperationResult.Error`, только если оно брошено в **его собственной** области. Наружу оно не отдаётся:
`Error.failure` — это `OperationException`, публичная обёртка с ошибкой и исходным `OperationFailure` в `cause`.

Результат другой функции, возвращающей `OperationResult`, внутри `operation { … }` разворачивается через
`getOrRaise()`, а если ошибку нужно заменить — через `getOrRaise { error -> … }`. Обе формы сохраняют `failure`
вложенного результата как `cause` нового `OperationFailure`, поэтому стек-трейс места, где ошибка возникла,
не теряется. `getOrElse { error -> … }` подходит, только когда ошибка обрабатывается без прерывания операции:
`raise()` внутри него исходный стек-трейс теряет. `getOrThrow()` бросает `OperationException`, который `operation`
не перехватывает, поэтому внутри операции он вылетает наружу как исключение; он предназначен для кода вне
операций, например для тестов.

## Загрузка нескольких сущностей

Функции из [Repository.kt](src/main/kotlin/tech/testsys/operation/util/Repository.kt) загружают несколько сущностей
одним вызовом порта и возвращают словарь «идентификатор → сущность»:

| Функция               | Что делает                                                                             |
|-----------------------|----------------------------------------------------------------------------------------|
| `findByIdsAsMap(ids)` | Вызывает `findByIds`; идентификаторов ненайденных сущностей в словаре нет              |
| `loadByIdsAsMap(ids)` | Вызывает `load` списка; падает с `IllegalArgumentException`, если какой-то сущности нет |

Обе функции убирают повторяющиеся идентификаторы и не вызывают порт при пустом наборе. Порт не гарантирует порядок
сущностей в ответе. Поэтому, если результат сопоставляется с исходным списком по позиции, сущности берутся
из словаря по исходным идентификаторам (образец — `loadSubmissions` в
[TaskValidationOperations.kt](src/main/kotlin/tech/testsys/operation/TaskValidationOperations.kt)).

`StudyOperations.downloadTaskResource` читает файл через `FileContentReader` после проверок доступа
и возвращает `FileData` с байтами. При копировании версии операция передаёт исходный `FileData` в хранилище.

## Тесты

- Тесты операций класса `<Actor>Operations` — в `<Actor>OperationsTests`; порты подменяются моками MockK.
- Тестовые Пользователи и сущности создаются хелперами из
  [TestDataBuilders.kt](src/test/kotlin/tech/testsys/operation/util/TestDataBuilders.kt) (`testDeveloper`,
  `testNewTask`, `testUncommittedTask`, …).
- Ошибка операции проверяется через `assertRaises(expected) { … }` из
  [Assertions.kt](src/test/kotlin/tech/testsys/operation/util/Assertions.kt).

Образец — [DeveloperOperationsTests.kt](src/test/kotlin/tech/testsys/operation/user/DeveloperOperationsTests.kt).

## Просмотр Тура

[StudyOperations](src/main/kotlin/tech/testsys/operation/user/StudyOperations.kt) предоставляет две перегрузки
`viewContest`: для Участника и для Ученика с идентификатором выбранного Класса.
Требования просмотра определены в `testsys.user.study.viewContest` в [features.md](../docs/domain/features.md).

Обе перегрузки возвращают `Triple<Instant?, Contest, List<Task>>`: сохранённый момент первого входа
в выбранном контексте либо `null`, исходный `Contest` и его Задачи. Порт возвращает Задачи в любом порядке,
поэтому операция загружает Задачи Тура и упорядочивает их по порядку Тура.
Просмотр не записывает вход и не вычисляет оставшееся время.

## Просмотр Задачи

[StudyOperations](src/main/kotlin/tech/testsys/operation/user/StudyOperations.kt) предоставляет две перегрузки
`viewTask`: для Участника и для Ученика с идентификатором выбранного Класса.
Требования просмотра определены в `testsys.user.study.viewTask` в [features.md](../docs/domain/features.md).

Обе перегрузки возвращают `StudyOperations.StudyTask`. Условие, Упражнения, Авторские Решения, Решения Посылок,
Вердикты и Судейские вердикты операция загружает через порты, поэтому ленивые ссылки возвращённых сущностей
остаются незагруженными. Имя файла Решения операция берёт из загруженного Решения вместе с его содержимым.

## Отправка Решения

[StudyOperations](src/main/kotlin/tech/testsys/operation/user/StudyOperations.kt) предоставляет две перегрузки
`sendSolution`: для Участника и для Ученика с идентификатором выбранного Класса.
Требования отправки определены в `testsys.user.study.sendSolution` в [features.md](../docs/domain/features.md).

Обе перегрузки сохраняют `Solution` и `Submission` и возвращают сохранённую `Submission`.
Операция не передаёт Посылку в `Grader`: это делает вызывающая сторона после фиксации транзакции.
`StudyOperations` получает `Clock` через конструктор и использует его только для проверки времени отправки.
Вызов читает время не более одного раза.

## Вход в Тур

[ParticipantOperations](src/main/kotlin/tech/testsys/operation/user/ParticipantOperations.kt) предоставляет
`viewContests` и `enterContest`.
[StudentOperations](src/main/kotlin/tech/testsys/operation/user/StudentOperations.kt) предоставляет
`viewContests` и `enterContest` для выбранного Класса.

Обе операции `viewContests` возвращают `List<Pair<Instant?, Contest>>`:
первый элемент — сохранённый момент входа в выбранном контексте либо `null`, второй — сам Тур.
Операции не вычисляют оставшееся время и не записывают вход.
`ParticipantOperations` и `StudentOperations` получают `Clock` через конструктор. `ParticipantOperations` использует его
только во входе в Тур, `StudentOperations` — во входе в Тур и в проверке срока Кода-приглашения при присоединении
к Классу.
Вызов читает время не более одного раза; сохранённый момент нормализуется до микросекунд.
Проверки первого и повторного входа описаны в [features.md](../docs/domain/features.md).

## Вход в Систему

[UserOperations](src/main/kotlin/tech/testsys/operation/user/UserOperations.kt) предоставляет
`authenticate(accessToken)`. Требования входа определены в `testsys.user.authentication`
в [features.md](../docs/domain/features.md).

Операция ищет Пользователя по Коду-доступа в портах хранения всех четырёх видов Пользователей и возвращает
найденного Пользователя как `User<*>`. Если КД не присвоен ни одному Пользователю, операция возвращает
`InvalidAccessTokenError`. Данные Пользователей операция не изменяет: она только записывает момент входа через
`UserRepository.recordLogin`. `UserOperations` получает `UserRepository` через конструктор; вызов читает время
один раз и усекает его до микросекунд.

## Регистрация

[UserOperations](src/main/kotlin/tech/testsys/operation/user/UserOperations.kt) предоставляет
`requestRegistration(email)` и `confirmRegistration(registrationRequestId, confirmationCode, name, role)`.
Обе операции помечены кодификатором `testsys.user.registration`; требования определены в этой фиче
в [features.md](../docs/domain/features.md).

- `requestRegistration` ищет запрос через `RegistrationRequestRepository.findByEmail` и возвращает идентификатор
  `RegistrationRequest`. Действующий запрос операция не сохраняет заново, а недействующий перезаписывает через
  `update`, поэтому идентификатор не меняется.
- `confirmRegistration` принимает Роль как `RegistrationRole` и возвращает `Pair<MultipleRoleUser, String>`:
  сохранённого Пользователя и исходный Код-доступа. До сравнения кода операция сохраняет запрос с уменьшенным
  `attemptsLeft` через `update` с токеном `version`. Если версия устарела, `update` бросает исключение и код
  не сравнивается. Неверный код возвращает `InvalidConfirmationCodeError`. Вызывающий код должен зафиксировать
  транзакцию и при этой ошибке, иначе попытка не будет потрачена; в `testsys-web:app` это делает
  `UserService.confirmRegistration`. После сохранения Пользователя операция удаляет запрос.
- Письма отправляет порт `UserMailSender`; обе операции вызывают его последним шагом. Исключения порта,
  как и исключения хранения, выходят из операций; уже сохранённые изменения операция не откатывает.
- Момент отправки письма и обработка сбоев после фиксации транзакции — в
  [mail/README.md](../testsys-infra/mail/README.md).

`UserOperations` получает через конструктор `CommunityConfig` с Публичным Сообществом, `EmailConfirmationConfig`
со сроком действия кода и числом попыток, `Clock` и `RandomGenerator`. Вызов читает время не более одного раза;
момент окончания срока нормализуется до микросекунд. Код подтверждения и КД генерируют функции из
[Credentials.kt](src/main/kotlin/tech/testsys/operation/util/Credentials.kt) с этим `RandomGenerator`. Приложение
должно передать криптостойкий генератор, например `SecureRandom`. Приведение и проверку почты, момент окончания
срока кода и проверку, действует ли запрос, выполняют функции из
[Email.kt](src/main/kotlin/tech/testsys/operation/util/Email.kt).

## Смена почты

[MultipleRoleUserOperations](src/main/kotlin/tech/testsys/operation/user/MultipleRoleUserOperations.kt)
предоставляет `requestEmailChange(user, email)` и `confirmEmailChange(user, confirmationCode)`. Обе операции
помечены кодификатором `testsys.user.multi.changeMail`; требования определены в этой фиче
в [features.md](../docs/domain/features.md).

- Операции ищут запрос смены почты через `EmailChangeRequestRepository.findByUser`. `requestEmailChange`
  ничего не возвращает. Действующий запрос на ту же почту операция не сохраняет заново, а другой запрос
  Пользователя перезаписывает через `update`, поэтому идентификатор запроса не меняется.
- `requestEmailChange` проверяет новую почту по сохранённым Пользователям через `findByEmail`. Если найден
  Пользователь с тем же `id`, операция возвращает `EmailUnchangedError`, если с другим — `EmailAlreadyBoundError`.
  Почта переданного Пользователя в проверках не участвует.
- `confirmEmailChange` до сравнения кода сохраняет запрос с уменьшенным `attemptsLeft` через `update` с токеном
  `version`. Если версия устарела, `update` бросает исключение и код не сравнивается. Неверный код возвращает
  `InvalidConfirmationCodeError`. Вызывающий код должен зафиксировать транзакцию и при этой ошибке, иначе попытка
  не будет потрачена. В `testsys-web` это делает `MultipleRoleUserService.confirmEmailChange`.
- При совпадении кода операция перечитывает Пользователя через `findById` и меняет почту у сохранённой версии,
  поэтому Код-доступа, Псевдоним и Роли берутся из хранилища, а не из переданного Пользователя. Затем операция
  удаляет запрос и возвращает обновлённого Пользователя.
- `requestEmailChange` последним шагом отправляет код на новую почту, `confirmEmailChange` — уведомление
  на прежнюю почту через `UserMailSender`. Исключения порта и хранения выходят из операций так же, как
  в регистрации.

`viewProfile(user)` из того же класса возвращает Роли Пользователя в порядке его Ролей, каждую со списком
её Сообществ по возрастанию идентификатора. Сообщества всех Ролей загружаются одним вызовом
`CommunityRepository.findByIds`. Псевдоним и почту вызывающий код берёт у переданного Пользователя.
Отказов у операции нет: Пользователя с Фиксированной Ролью исключает тип параметра, поэтому `ViewProfileError`
не содержит ни одной ошибки.

`MultipleRoleUserOperations` получает через конструктор `MultipleRoleUserRepository`, `CommunityRepository`,
`ManagerCommunityInviteRepository`, `DeveloperCommunityInviteRepository`, `EmailChangeRequestRepository`, `UserMailSender`, `EmailConfirmationConfig`, `Clock`, `RandomGenerator`
и `CommunityConfig` с Публичным Сообществом.
Код подтверждения, срок его действия и чтение времени — те же, что в регистрации.

## Первое получение Роли

`joinCommunity` из [Role.kt](src/main/kotlin/tech/testsys/operation/util/Role.kt) включает Пользователя
в Сообщество в Роли через `MultipleRoleUserRepository.addCommunityMembership`. Если у переданного Пользователя
этой Роли ещё нет, функция сначала включает его в Публичное Сообщество в той же Роли. Правило определено
в `testsys.entity.multi.role` в [features.md](../docs/domain/features.md). Функцию вызывают
`MultipleRoleUserOperations.joinCommunity` и `AdministratorOperations.grantRole`. Вызывающий код выполняет оба
включения в одной транзакции, чтобы исключение откатило их вместе; в `testsys-web:app` её открывает прокси-сервис.

## Кабинет Организатора

[ManagerOperations](src/main/kotlin/tech/testsys/operation/user/ManagerOperations.kt) реализует фичи
`testsys.user.multi.manager.*`; требования определены в [features.md](../docs/domain/features.md).

- `viewClass` возвращает `ManagerOperations.ClassDetails`: Класс, его Код-приглашение, Учеников с моментами
  последнего входа и добавленные Туры. `viewCompetition` так же возвращает `CompetitionDetails` с Участниками.
  Учеников, Участников и Туры операции читают через `findByIds` и упорядочивают по составу группы, моменты входа —
  через `UserRepository.findLastLogins` одним вызовом.
- `viewClassContest` и `viewCompetitionContest` возвращают `ContestResults`: Тур, его Задачи в порядке Тура,
  Учеников или Участников и результаты пар автора и Задачи с Посылками.
- `deleteParticipant` проверяет Посылки Участника в Турах Соревнования через `SubmissionRepository.countGrading`
  и отклоняет удаление ошибкой `ParticipantHasSubmissionsError`, если они есть. Иначе операция удаляет Участника
  через `ParticipantRepository.removeById` и возвращает его загруженное состояние.
- `viewAvailableContests` помечена кодификатором `addContest`. Она возвращает страницу Туров, открытых Сообществам
  Пользователя в Роли Организатора, через `ContestRepository.findSharedTo`; из них выбирается добавляемый Тур.

## Кабинет Администратора

[AdministratorOperations](src/main/kotlin/tech/testsys/operation/user/AdministratorOperations.kt) реализует фичи
`testsys.user.multi.admin.*`; требования определены в [features.md](../docs/domain/features.md).

- `viewUsers` возвращает страницу пар `Pair<User<*>, Instant?>`, а `viewUser` — одну такую пару: Пользователя
  и момент его последнего входа либо `null`. Моменты читает `UserRepository.findLastLogins` одним вызовом на страницу.
- `viewCommunities` возвращает пары из Сообщества и числа доступных через него Пользователей.
  Число считает `UserRepository.countAvailableToAdministrator` отдельно для каждого Сообщества.
- `createCommunity` после сохранения Сообщества включает в него создателя в Роли Администратора через
  `MultipleRoleUserRepository.addCommunityMembership`.
- `grantRole` принимает Роль как `CommunityRole`; Роли Судьи и Администратора операция отклоняет ошибкой
  `RoleNotGrantableError`.
  Операция включает Пользователя через `joinCommunity` (см. раздел [Первое получение Роли](#первое-получение-роли))
  и для Пользователя, который уже состоит в Сообществе в этой Роли: порт такое членство не меняет.
- `removeFromCommunity` перечитывает Пользователя через `MultipleRoleUserRepository.findById` и вызывает
  `removeCommunityMembership`. Публичное Сообщество из `CommunityConfig` операция отклоняет ошибкой
  `CommunityIsPublicError`, Роль Администратора — ошибкой `RoleNotRemovableError`, отсутствующее членство — ошибкой
  `UserNotCommunityMemberError`.
- `deleteObserver` удаляет Наблюдателя через `ObserverRepository.removeById` и возвращает его загруженное
  состояние. Наблюдателя чужого Сообщества операция отклоняет ошибкой `UserAccessDeniedError`.
- Разделы Ролей страницы Пользователя возвращают отдельные операции с кодификатором `viewUser`: `viewUserTasks`,
  `viewUserContests`, `viewUserClasses`, `viewUserCompetitions`, `viewUserJudgments` и `viewUserAssignedContests`.
  Каждая сначала выполняет проверки `viewUser` и возвращает пустой список, если у Пользователя нет нужной Роли.
  Количества Посылок считают `SubmissionRepository.countGradingByTask` и `countGrading`, Соревнования Туров
  находит `CompetitionRepository.findByContestIds`. Предыдущий результат Судейского вердикта операция вычисляет
  в `Long`: это балл предыдущего Судейского вердикта той же Посылки по времени и идентификатору, иначе сумма баллов
  Вердикта или `null` без успешного Вердикта.
- `viewCommunityContests` помечена кодификатором `createObserver`. Она возвращает страницу Туров, открытых
  Сообществу Администратора, из которых выбираются Туры Наблюдателя.

## Кабинет Судьи

[JudgeOperations](src/main/kotlin/tech/testsys/operation/user/JudgeOperations.kt) реализует фичи
`testsys.user.multi.judge.*`; требования определены в [features.md](../docs/domain/features.md).

- `viewResults` возвращает страницу пар `Pair<Verdict, User<*>>`: Вердикт и автора его Посылки. Посылку операция
  читает через `SubmissionRepository.load`, автора — через `MultipleRoleUserRepository` или `ParticipantRepository`
  по виду идентификатора. Файлы при этом не загружаются.
- `viewSolution` возвращает `SubmissionDetails`: Посылку с автором, Задачей, Туром и Решением, текущий успешный
  Вердикт с Полигонами в порядке его результатов, итоговый балл и Судейские вердикты. Судейские вердикты упорядочены
  по времени выставления, затем по идентификатору, и каждый идёт вместе со своим Судьёй. Решение и Полигоны
  загружаются вместе с файлами. Итоговый балл операция вычисляет в `Long`; без успешного Вердикта он `null`.
- Файлы возвращают отдельные операции с кодификатором `viewSolution`: `downloadSolution`, `downloadLogs`
  и `downloadRecording`. Каждая возвращает `FileData`. Полигон вне успешного Вердикта операции отклоняют ошибкой
  `TestNotInVerdictError`, Полигон без видеозаписи — ошибкой `RecordingNotExistsError`.

## Кабинет Разработчика

[DeveloperOperations](src/main/kotlin/tech/testsys/operation/user/DeveloperOperations.kt) реализует фичи
`testsys.user.multi.developer.*`; требования определены в [features.md](../docs/domain/features.md).

- `viewContests` возвращает страницу пар `Pair<Contest, List<Community>>`, а `viewTask` — пару из Задачи
  и её Сообществ доступа. `viewContest` возвращает тройку из Тура, его Задач и Сообществ доступа в порядке Тура.
  Связанные сущности операции загружают через `findByIds`; отсутствующие пропускаются.
- `viewResource` возвращает версии цепочки парами `Pair<DomainEntity<*>, Solution?>`: у версии Авторского Решения
  второй элемент — её Решение, у остальных — `null`.
- `downloadResourceVersion` возвращает файл версии как `FileData`, у Авторского Решения — файл его Решения.
  Версия другой цепочки считается отсутствующей.
- `viewTrikStudioVersions` помечена кодификатором `createContest`. Она возвращает версии TRIK Studio,
  зарегистрированные в Системе, через `ContestRepository.findTrikStudioVersions`; из них выбираются версия Тура
  и поддерживаемые версии Задачи.
