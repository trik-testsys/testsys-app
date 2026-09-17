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
| `tech.testsys.operation.user`          | Классы операций, по одному на Роль: `<Role>Operations`                        |
| `tech.testsys.operation.error`         | Модель результата и ошибок операций                                           |
| `tech.testsys.operation.annotation`    | Аннотация `@Feature`                                                          |
| `tech.testsys.operation.util`          | Вспомогательные функции для операций                                          |

## Операции

- **Операция** — публичный метод класса `<Role>Operations`, реализующий одну пользовательскую фичу из раздела
  `testsys.user` в [features.md](../docs/domain/features.md). Метод помечается `@Feature("<кодификатор>")`.
- Класс выбирается по префиксу кодификатора фичи: `testsys.user.multi.developer.*` — `DeveloperOperations`,
  `testsys.user.multi.student.*` — `StudentOperations` и т. д. Смысл префиксов — в разделе `testsys.user`
  в [features.md](../docs/domain/features.md).
- Зависимости операций — порты из `tech.testsys.domain.contract` — передаются через конструктор класса.
- Первый параметр операции — Пользователь, который её выполняет; остальные — входные данные фичи.
- Операция возвращает `OperationResult<T, <Operation>Error>`.

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
  Она реализует интерфейсы **всех** операций, которые могут её вернуть; общие ошибки (`AuthorizationError`)
  не дублируются.
- Имя конкретной ошибки называет нарушенное условие: `TaskAlreadyHasStatementError`, а не `AttachError`.

### Выполнение

Тело операции оборачивается в `operation<T, E> { … }`
([OperationFailure.kt](src/main/kotlin/tech/testsys/operation/error/OperationFailure.kt)). Внутри:

| Функция                          | Что делает                                                                  |
|----------------------------------|-----------------------------------------------------------------------------|
| `ensure(condition, error)`       | Прерывает операцию с `error`, если `condition` ложно                        |
| `ensure(condition) { error }`    | То же, но ошибка создаётся только при отказе                                |
| `error.raise()`                  | Безусловно прерывает операцию с `error`                                     |

После `ensure(x != null) { … }` компилятор считает `x` не-`null` (smart cast).

Отказ реализован исключением `OperationFailure`, которое `operation` превращает в `OperationResult.Error`, только
если оно брошено в **его собственной** области. Поэтому результат другой функции, возвращающей `OperationResult`,
внутри `operation { … }` разворачивается через `getOrElse { error -> error.raise() }`. `getOrThrow()` внутри
операции выбрасывает `OperationFailure` чужой области наружу как исключение; он предназначен для кода вне
операций, например для тестов.

## Вспомогательные функции

| Функция                          | Файл                                                          | Что делает                                         |
|----------------------------------|---------------------------------------------------------------|----------------------------------------------------|
| `MultipleRoleUser.hasRole<R>()`  | [Role.kt](src/main/kotlin/tech/testsys/operation/util/Role.kt) | Есть ли у Пользователя Роль `R`                   |
| `Task.getWip(onCommited)`        | [Task.kt](src/main/kotlin/tech/testsys/operation/util/Task.kt) | Рабочая версия Задачи в состоянии New или Uncommitted; для Committed — ошибка `onCommited` |
| `Task.changeWip(onCommited) { }` | [Task.kt](src/main/kotlin/tech/testsys/operation/util/Task.kt) | Новая Задача с изменённой рабочей версией для New и Uncommitted; для Committed — ошибка `onCommited` |

Проверка доступа Пользователя к конкретным сущностям (фичи `*.authorization`) общим механизмом пока
не реализована.

## Тесты

- Тесты операций Роли — в `<Role>OperationsTests`; порты подменяются моками MockK.
- Тестовые Пользователи и сущности создаются хелперами из
  [TestDataBuilders.kt](src/test/kotlin/tech/testsys/operation/util/TestDataBuilders.kt) (`testDeveloper`,
  `testNewTask`, `testUncommittedTask`, …).
- Ошибка операции проверяется через `assertRaises(expected) { … }` из
  [Assertions.kt](src/test/kotlin/tech/testsys/operation/util/Assertions.kt).

Образец — [DeveloperOperationsTests.kt](src/test/kotlin/tech/testsys/operation/user/DeveloperOperationsTests.kt).
