# Структура проекта

Документ описывает, как устроен репозиторий `testsys-app`: из каких модулей он состоит, как они зависят друг
от друга, как собирается проект и где искать (или куда класть) код. Он предназначен для всех, кто работает
с кодом проекта.

Правила написания кода — в [code-style.md](code-style.md), перечень документации и гайдов —
в [docs.md](../docs.md), термины предметной области — в [definitions.md](../domain/definitions.md).

## Корень репозитория

```
testsys-app/
├── .github/workflows/        # CI: build, lint (Detekt + SARIF), проверка ветки PR
├── buildSrc/                 # Convention-плагин testsys.conventions
├── docs/                     # Документация проекта
├── gradle/
│   ├── libs.versions.toml    # Каталог версий зависимостей (единственное место с версиями)
│   └── wrapper/
├── scripts/                  # Вспомогательные скрипты: проверка KDoc (см. code-style.md)
├── testsys-domain/           # Доменная модель и порты
├── testsys-operation/        # Операции (сценарии фич testsys.user.*)
├── testsys-infra/            # Реализации портов
├── testsys-web/              # Веб-приложение
├── detekt.yml                # Конфигурация Detekt
├── settings.gradle.kts       # Список модулей (корневого build.gradle.kts нет)
└── gradlew, gradlew.bat
```

## Модули

| Модуль                               | Назначение                                                                                           | Состояние         |
|--------------------------------------|------------------------------------------------------------------------------------------------------|-------------------|
| `testsys-domain`                     | Доменные модели, порты (`contract`), DSL билдеров. Без Spring, JPA и любых внешних зависимостей.     | Реализован        |
| `testsys-operation`                  | Операции — реализация пользовательских фич из [features.md](../domain/features.md), по классу на Роль. | В разработке      |
| `testsys-infra:service`              | Сервисы, которые используют операции.                                                                | Заготовка (пусто) |
| `testsys-infra:database`             | Реализация портов хранения домена: JPA-сущности, репозитории, маппинги, адаптеры, Liquibase.         | Реализован        |
| `testsys-infra:database:codegen-api` | Аннотации (`@CompositeKeyConstructor`), обрабатываемые KSP.                                          | Реализован        |
| `testsys-infra:database:codegen`     | KSP-процессоры, генерирующие вспомогательный код для JPA-сущностей.                                  | Реализован        |
| `testsys-infra:grpc`                 | Связь с внешним грейдером решений TRIK Studio (реализация порта `Grader`).                           | Заготовка (пусто) |
| `testsys-infra:localization`         | Типобезопасный API локализованных сообщений, генерируемый из ICU-паттернов.                          | Реализован        |
| `testsys-web`                        | Веб-приложение (Кабинеты): точка входа, собирающая все модули; вызывает операции.                    | Заготовка (пусто) |

### Зависимости между модулями

Архитектура гексагональная: домен объявляет порты, инфраструктурные модули их реализуют, а веб-приложение
собирает всё вместе.

Правила:

- `testsys-domain` ни от чего не зависит. Любой новый код, которому нужен Spring, JPA или сеть, живёт вне домена.
- Инфраструктура зависит от домена, но не наоборот: домен знает только интерфейсы из `tech.testsys.domain.contract`.
- Сейчас в Gradle прописаны только связи `operation → domain`, `database → domain`, `database → codegen-api`
  и `database → codegen` (через `ksp`). Остальные связи — целевая архитектура.

## Сборка

- `settings.gradle.kts` — список модулей. Модуль, не попавший в него, не собирается, и его тесты не запускаются.
- `buildSrc/src/main/kotlin/testsys.conventions.gradle.kts` — общий плагин, который подключает каждый модуль:
  Kotlin JVM 21, `allWarningsAsErrors = true`, JUnit Platform, Detekt (`detekt.yml`, `autoCorrect = true`,
  `build/generated/` исключён). Задача `check` зависит от `detektMain`.
- `gradle/libs.versions.toml` — версии, библиотеки и bundles. Версии зависимостей указываются только здесь.
- В `build.gradle.kts` модуля остаются только плагины сверх конвенций (`ksp`, `plugin.spring`, `plugin.jpa`)
  и зависимости.

Полная сборка — компиляция, тесты и Detekt:

```bash
./gradlew build
```

Только Detekt. По умолчанию он запускается с `autoCorrect = true` и сам переписывает файлы, поэтому после запуска
просмотрите `git diff`:

```bash
./gradlew detektMain
```

Чтобы проверить код, не изменяя файлы (например, при ревью), передайте свойство `detekt.autoCorrect=false`.
Оно действует на любую задачу, которая запускает Detekt, в том числе на `build`:

```bash
./gradlew build -Pdetekt.autoCorrect=false
```

## CI

Workflow лежат в `.github/workflows`.

| Workflow                  | Когда                          | Что делает                                                      |
|---------------------------|--------------------------------|-----------------------------------------------------------------|
| `build.yml`               | push/PR в `master`, `dev`      | `./gradlew build -x detekt -x detektMain`, артефакты и отчёты   |
| `lint.yml`                | push/PR в `master`, `dev`      | `./gradlew detektMain`, загрузка SARIF в GitHub Security        |
| `check-source-branch.yml` | PR в `dev`                     | Разрешает PR только из веток `sh1sh4k1n9/`, `ch3zych3z/`, `KarasssDev/` |
| `release.yml`             | —                              | Пока пустой                                                     |

## Куда класть новый код

| Что добавляем                                   | Куда                                                                                              |
|-------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Новую доменную сущность                         | `domain/model/<group\|task\|user>` + билдер + порт хранения, см. [implement-entity.md](../guides/implement-entity.md) |
| Хранение сущности в БД                          | `testsys-infra:database`, см. [implement-entity.md](../guides/implement-entity.md)               |
| Новый внешний порт (хранилище, внешняя система) | Интерфейс в `domain/contract`, реализация — в `testsys-infra`, см. [implement-port.md](../guides/implement-port.md) |
| Пользовательскую фичу                           | Метод с `@Feature` в `operation/user/<Role>Operations.kt`, см. [implement-feature.md](../guides/implement-feature.md) |
| Переиспользуемую логику для операций            | `testsys-infra:service`                                                                           |
| Локализованное сообщение                        | См. [add-localization.md](../guides/add-localization.md)                                          |
| Версию библиотеки                               | `gradle/libs.versions.toml`                                                                       |
