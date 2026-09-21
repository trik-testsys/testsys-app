# Модуль `testsys-domain`

Документ описывает, как устроен доменный модуль: модель сущностей, порты и DSL билдеров. Он предназначен для всех,
кто пишет код, работающий с доменными объектами, — в домене, операциях или инфраструктуре.

Место модуля в архитектуре — в [structure.md](../docs/project/structure.md), термины предметной области —
в [definitions.md](../docs/domain/definitions.md). Пошаговое добавление сущности —
в [implement-entity.md](../docs/guides/implement-entity.md), нового порта — в [implement-port.md](../docs/guides/implement-port.md).

## Пакеты

| Пакет                                  | Что лежит                                                          |
|----------------------------------------|--------------------------------------------------------------------|
| `tech.testsys.domain.model`            | Сущности и их данные, по подпакетам `group`, `task`, `user`        |
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
- Сущности не хранят ссылок на другие сущности. Связь — это `LazyEntity<Id, E>` или `LazyEntityList<Id, E>`:
  внутри только идентификаторы, разрешаются они через `EntityLoader.load(...)`. Создаются через `id.lazify()`
  и `ids.lazify()`.
- Варианты состояния моделируются sealed-иерархиями: `TaskContent` (`New` / `Uncommitted` / `Committed`),
  `SubmissionStatus`, `SubmissionKind`, `GradingResult`, `TrikSupportedLanguage`.

### Пользователи

- `MultipleRoleUser` владеет набором `CompatibleUserRole`: `Developer`, `Student`, `Administrator`, `Judge`, `Manager`.
- `SingleRoleUser` — один из `Participant`, `Observer`, `Supervisor`.

Смысл Фиксированных и Нефиксированных Ролей — в [definitions.md](../docs/domain/definitions.md).

## Порты

- `EntityRepository<Data, Id, Entity>` объединяет `EntityFinder`, `EntityLoader`, `EntitySaver` и `EntityRemover`
  ([Common.kt](src/main/kotlin/tech/testsys/domain/contract/persistence/repository/Common.kt)). Порты хранения
  конкретных сущностей объявлены в `contract/persistence/repository/*Repositories.kt`.
- Остальные порты и их состояние — в разделе «Убедиться, что порт нужен»
  в [implement-port.md](../docs/guides/implement-port.md).

## DSL билдеров

- Точки входа — в `builder/api/{Task,Group,User}Api.kt`: `task { … }`, `taskData { … }`, `submission { … }`,
  `X.withData { … }`.
- Для полей sealed-типов используются `Chooser`'ы (`LanguageChooser`, `SubmissionStatusChooser`, …)
  из `builder/util/chooser`.
- Билдер проверяет обязательные поля и бросает `IllegalArgumentException` в `build()`.
- Доменные объекты за пределами домена, в том числе в маппингах инфраструктуры, создаются **только через DSL**.
