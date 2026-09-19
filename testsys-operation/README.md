# Модуль `testsys-operation`

Документ описывает, как устроен модуль операций: где лежат операции, как они связаны с фичами и Ролями, как
операции сообщают об ошибках и какие вспомогательные функции есть в модуле. Он предназначен для всех, кто
реализует или вызывает пользовательские фичи.

Место модуля в архитектуре — в [structure.md](../docs/project/structure.md), сами фичи — в
[features.md](../docs/domain/features.md), доменная модель и порты — в
[testsys-domain/README.md](../testsys-domain/README.md). Пошаговая реализация фичи — в
[implement-feature.md](../docs/guides/implement-feature.md), правила тестов — в
[unit-tests.md](../docs/project/unit-tests.md).

## Пакеты

| Пакет                                  | Что лежит                                                                     |
|----------------------------------------|-------------------------------------------------------------------------------|
| `tech.testsys.operation.user`          | Классы операций `<Actor>Operations`, см. раздел «Операции»                    |
| `tech.testsys.operation.error`         | Модель результата и ошибок операций                                           |
| `tech.testsys.operation.annotation`    | Аннотация `@Feature` и opt-in-маркер `@InternalOperationsApi`                 |
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
- Зависимости операций — порты из `tech.testsys.domain.contract` — передаются через конструктор класса.
- Первый параметр операции — Пользователь, который её выполняет; остальные — входные данные фичи.
- Операция возвращает `OperationResult<T, <Operation>Error>`.

| Префикс кодификатора                | Класс                        |
|-------------------------------------|------------------------------|
| `testsys.user.*`                    | `UserOperations`             |
| `testsys.user.single.*`             | `SingleRoleUserOperations`   |
| `testsys.user.single.participant.*` | `ParticipantOperations`      |
| `testsys.user.single.observer.*`    | `ObserverOperations`         |
| `testsys.user.multi.*`              | `MultipleRoleUserOperations` |
| `testsys.user.multi.developer.*`    | `DeveloperOperations`        |
| `testsys.user.multi.student.*`      | `StudentOperations`          |
| `testsys.user.multi.judge.*`        | `JudgeOperations`            |
| `testsys.user.multi.admin.*`        | `AdministratorOperations`    |
| `testsys.user.multi.manager.*`      | `ManagerOperations`          |

Образец — [DeveloperOperations.kt](src/main/kotlin/tech/testsys/operation/user/DeveloperOperations.kt).

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

## Тесты

- Тесты операций класса `<Actor>Operations` — в `<Actor>OperationsTests`; порты подменяются моками MockK.
- Тестовые Пользователи и сущности создаются хелперами из
  [TestDataBuilders.kt](src/test/kotlin/tech/testsys/operation/util/TestDataBuilders.kt) (`testDeveloper`,
  `testNewTask`, `testUncommittedTask`, …).
- Ошибка операции проверяется через `assertRaises(expected) { … }` из
  [Assertions.kt](src/test/kotlin/tech/testsys/operation/util/Assertions.kt).

Образец — [DeveloperOperationsTests.kt](src/test/kotlin/tech/testsys/operation/user/DeveloperOperationsTests.kt).
