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
- Часть полей `XData` фиксируется при создании: `owner` у Задачи, Тура, Класса, Соревнования и Сообщества;
  `author`, `solution`, `task` и `kind` у Посылки; `task` и `submission` у Вердикта; `judge` и `verdict`
  у Судейского вердикта (`JudgmentOrder`); `versionBucket` у Полигона, Условия, Упражнения и авторского
  Решения. `save` их записывает, а `update` игнорирует значение из переданной сущности и оставляет сохранённое.
  В KDoc такое поле помечено «fixed on creation and ignored on update».
- Ещё часть полей фиксируется при создании так, что `update` с другим значением падает: `file` у Полигона,
  Условия и Упражнения, `language` у Упражнения, `solution` и `expectedScore` у авторского Решения.
  Такой `update` бросает `UnsupportedOperationException` и ничего не сохраняет. В KDoc такое поле помечено
  «fixed on creation; update fails if it differs». Зачем это нужно — в разделе
  [Версионирование Ресурсов](#версионирование-ресурсов).
- Если при создании фиксируются все поля сущности, её `update` не поддерживается и бросает
  `UnsupportedOperationException`. Это Решение (`Solution`: `file` и `language`), логи (`Logs`) и запись
  (`Recording`), у которых единственное поле — `file`.
- `FileData` после сохранения не меняется: изменённый файл сохраняется как новый.
- Сущности не хранят ссылок на другие сущности. Связь — это `LazyEntity<Id, E>` или `LazyEntityList<Id, E>`:
  внутри только идентификаторы, разрешаются они через `EntityLoader.load(...)`. Создаются через `id.lazify()`
  и `ids.lazify()`.
- Варианты состояния моделируются sealed-иерархиями: `TaskContent` (`New` / `Uncommitted` / `Committed`),
  `SubmissionStatus`, `SubmissionKind`, `GradingResult`, `TrikSupportedLanguage`.

### Версионирование Ресурсов

Версионируются сущности с `versionBucket`: Полигон (`Test`), Условие (`Statement`), Упражнение (`Exercise`)
и авторское Решение (`DeveloperSolution`).

- Версии одного Ресурса — отдельные сущности с общим `versionBucket`. Сущность — это одна версия: её файл
  (у Упражнения ещё и язык, у авторского Решения — ссылка `solution` и ожидаемый Вердикт `expectedScore`)
  после создания не меняется.
- Новая версия — это новая сущность, сохранённая через `save` с `versionBucket` прежней версии; прежняя версия
  и её файл остаются без изменений. `save` не проверяет, есть ли уже сущности с таким `versionBucket`.
- Версии авторского Решения могут ссылаться на одно и то же Решение (`Solution`): версия с новым ожидаемым
  Вердиктом и прежней программой переиспользует `solution` прежней версии.
- `update` меняет у версии только название и описание. Как падает попытка сменить зафиксированное поле —
  в разделе [Модель](#модель).
- `TaskContent` ссылается на идентификаторы сущностей, поэтому каждая ревизия Задачи закрепляет конкретные
  версии Ресурсов вместе с их зафиксированными полями.
- Название и описание ревизией Задачи не закрепляются: `update` меняет их у версии на месте, и новое значение
  видно во всех ревизиях, которые ссылаются на эту версию, в том числе в зафиксированной.

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
