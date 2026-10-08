# Структура проекта

Документ описывает, как устроен репозиторий `testsys-app`: из каких модулей он состоит, как они зависят друг
от друга, как собирается проект и где искать (или куда класть) код. Он предназначен для всех, кто работает
с кодом проекта.

Правила написания кода — в [code-style.md](code-style.md), перечень документации и гайдов —
в [docs.md](../docs.md), термины предметной области — в [definitions.md](../domain/definitions.md).

## Корень репозитория

```
testsys-app/
├── .github/workflows/        # CI: build, test, lint (Detekt + SARIF), проверка ветки PR
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
| `testsys-operation`                  | Операции — реализация пользовательских фич из [features.md](../domain/features.md), по классу на Роль или группу Пользователей. | В разработке      |
| `testsys-infra:database`             | Реализация портов хранения домена: JPA-сущности, репозитории, маппинги, адаптеры, Liquibase.         | Реализован        |
| `testsys-infra:diagnostics`          | Синхронный анализ XML одного Полигона через порт `PolygonDiagnostics`.                           | Реализован        |
| `testsys-infra:grpc`                 | Связь с Проверяющими узлами и балансировка проверок (реализация порта `Grader`).                    | Реализован        |
| `testsys-infra:localization`         | Типобезопасный API локализованных сообщений, генерируемый из MF2-сообщений и форматируемый ICU4J MF2. | Реализован        |
| `testsys-web`                        | Веб-приложение (Кабинеты): точка входа, собирающая все модули; вызывает операции.                    | Заготовка (пусто) |

### Зависимости между модулями

Архитектура гексагональная: домен объявляет порты, инфраструктурные модули их реализуют, а веб-приложение
собирает всё вместе.

Правила:

- `testsys-domain` ни от чего не зависит. Любой новый код, которому нужен Spring, JPA или сеть, живёт вне домена.
- Инфраструктура зависит от домена, но не наоборот: домен знает только интерфейсы из `tech.testsys.domain.contract`.
- Сейчас в Gradle прописаны только связи `operation → domain`, `database → domain`, `database → codegen-api`,
  `database → codegen` (через `ksp`), `grpc → domain` и `diagnostics → domain`. Остальные связи — целевая архитектура.

## Сборка

- `settings.gradle.kts` — список модулей. Модуль, не попавший в него, не собирается, и его тесты не запускаются.
- `buildSrc/src/main/kotlin/testsys.conventions.gradle.kts` — общий плагин, который подключает каждый модуль:
  Kotlin JVM 21, `allWarningsAsErrors = true`, JUnit Platform, Detekt (`detekt.yml`, `autoCorrect = true`,
  `build/generated/` исключён). Задача `check` зависит от общего агрегатора `detekt`.
- `gradle/libs.versions.toml` — версии, библиотеки и bundles. Версии зависимостей указываются только здесь.
- В `build.gradle.kts` модуля остаются только плагины сверх конвенций (`ksp`, `plugin.spring`, `plugin.jpa`)
  и зависимости.

Агрегатор `detekt` лениво подключает все задачи анализа source set, автоматически создаваемые плагином Detekt,
включая тесты и дополнительные source set, объявленные модулем после применения конвенций. Каждая такая задача
проверяет свои исходники со своим classpath и разрешением типов; у агрегатора источник пуст, поэтому повторного
анализа нет. Конфигурация задаётся через расширение Detekt, отчёты SARIF включены для всех задач анализа.

Агрегатор `testAll` лениво подключает все задачи типа `Test`, включая дополнительные наборы тестов модулей.
Он запускает тесты без Detekt; `build` и `check` сохраняют полную проверку.

Полная сборка — компиляция, тесты и Detekt:

```bash
./gradlew build
```

Только Detekt. По умолчанию он запускается с `autoCorrect = true` и сам переписывает файлы, поэтому после запуска
просмотрите `git diff`:

```bash
./gradlew detekt
```

Чтобы проверить код, не изменяя файлы (например, при ревью), передайте свойство `detekt.autoCorrect=false`.
Оно действует на любую задачу, которая запускает Detekt, в том числе на `build`:

```bash
./gradlew build -Pdetekt.autoCorrect=false
```

Все тесты без анализа кода:

```bash
./gradlew testAll -Pdetekt.autoCorrect=false
```

Detekt 1.23 не разбирает context parameters (`context(name: Type)`): правила набора `formatting` на таком файле
падают с исключением. Файл с context parameters добавляется в `excludes` набора `formatting` в `detekt.yml`;
остальные правила на нём продолжают работать. Сейчас так исключён `OperationFailure.kt` из `testsys-operation`.

## CI

Workflow лежат в `.github/workflows`.

| Workflow                  | Когда                          | Что делает                                                      |
|---------------------------|--------------------------------|-----------------------------------------------------------------|
| `build.yml`               | push/PR в `master`, `dev`      | `./gradlew assemble` — компиляция и сборка без тестов, jar-артефакты, аннотации ошибок компиляции в PR |
| `test.yml`                | push/PR в `master`, `dev`      | `./gradlew testAll -Pdetekt.autoCorrect=false --no-daemon` — все тесты без Detekt; отчёт в Summary запуска, в check `Test report` и комментарием в PR, аннотации упавших тестов |
| `lint.yml`                | push/PR в `master`, `dev`      | `./gradlew detekt -Pdetekt.autoCorrect=false --continue --no-daemon` — все source set всех модулей без правки файлов; таблица замечаний в Summary запуска, загрузка SARIF в GitHub Security с отдельной категорией на модуль и задачу |
| `check-source-branch.yml` | PR в `dev`                     | Разрешает PR только из веток `sh1sh4k1n9/`, `ch3zych3z/`, `KarasssDev/`, `DirewolfPrime/`, `LutovolkVPraime/` |
| `release.yml`             | —                              | Пока пустой                                                     |

## Куда класть новый код

| Что добавляем                                   | Куда                                                                                              |
|-------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Новую доменную сущность                         | `domain/model/<entry\|group\|task\|user>` + билдер + порт хранения, см. [implement-entity.md](../guides/implement-entity.md) |
| Хранение сущности в БД                          | `testsys-infra:database`, см. [implement-entity.md](../guides/implement-entity.md)               |
| Новый внешний порт (хранилище, внешняя система) | Интерфейс в `domain/contract`, реализация — в `testsys-infra`, см. [implement-port.md](../guides/implement-port.md) |
| Пользовательскую фичу                           | Метод с `@Feature` в `operation/user/<Actor>Operations.kt`, см. [implement-feature.md](../guides/implement-feature.md) |
| Служебную обработку запроса валидации           | `TaskValidationOperations` в `testsys-operation`; контракт вызова — в [testsys-operation/README.md](../../testsys-operation/README.md) |
| Служебную замену Кодов-приглашений с истёкшим сроком | `refreshClassInvite` в `ManagerOperations` и `refreshCommunityInvite` в `AdministratorOperations`; контракт вызова — в [testsys-operation/README.md](../../testsys-operation/README.md) |
| Локализованное сообщение                        | См. [add-localization.md](../guides/add-localization.md)                                          |
| Версию библиотеки                               | `gradle/libs.versions.toml`                                                                       |
