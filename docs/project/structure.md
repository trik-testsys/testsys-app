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
│   ├── design-system/        # Дизайн-система Кабинетов
│   └── ui/                   # Kotlin-DSL дизайн-системы
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
| `testsys-infra:grpc`                 | Связь с внешним грейдером решений TRIK Studio (реализация порта `Grader`).                           | Заготовка (пусто) |
| `testsys-infra:localization`         | Типобезопасный API локализованных сообщений, генерируемый из ICU-паттернов.                          | Реализован        |
| `testsys-web`                        | Веб-приложение на Vaadin Flow: точка входа, тема, тексты интерфейса из локализации, страницы. Дизайн-система — в `design-system/`, см. [design-system/README.md](../../testsys-web/design-system/README.md). | Каркас: витрина `/dev/showcase`, страниц Кабинетов нет |
| `testsys-web:ui`                     | Kotlin-DSL дизайн-системы на Vaadin Flow: сетка, блоки, компоненты, см. [ui/README.md](../../testsys-web/ui/README.md). Без Spring и домена. | В разработке |

### Зависимости между модулями

Архитектура гексагональная: домен объявляет порты, инфраструктурные модули их реализуют, а веб-приложение
собирает всё вместе.

Правила:

- `testsys-domain` ни от чего не зависит. Любой новый код, которому нужен Spring, JPA или сеть, живёт вне домена.
- Инфраструктура зависит от домена, но не наоборот: домен знает только интерфейсы из `tech.testsys.domain.contract`.
- Сейчас в Gradle прописаны связи `operation → domain`, `database → domain`, `database → codegen-api`,
  `database → codegen` (через `ksp`), `web → ui` и `web → localization`. Остальные связи — целевая архитектура.

## Сборка

- `settings.gradle.kts` — список модулей. Модуль, не попавший в него, не собирается, и его тесты не запускаются.
- `buildSrc/src/main/kotlin/testsys.conventions.gradle.kts` — общий плагин, который подключает каждый модуль:
  Kotlin JVM 21, `allWarningsAsErrors = true`, JUnit Platform, Detekt (`detekt.yml`, `autoCorrect = true`,
  `build/generated/` исключён). Задача `check` зависит от `detektMain`. Ещё плагин выравнивает `kotlin-stdlib*`,
  `kotlin-reflect` и `kotlin-test*` (группа `org.jetbrains.kotlin`) во всех конфигурациях по версии компилятора
  Kotlin: BOM Spring Boot задаёт более новые версии этих библиотек, а встроенный в Detekt компилятор не читает
  их метаданные и ошибается в выводе типов (например, считает код после `?: return` недостижимым). Правило
  понижает stdlib и для библиотек вне BOM, собранных под более новый Kotlin (сейчас kotlinpoet, kctfork,
  Karibu-Testing), поэтому при обновлении таких библиотек проверяйте, что тесты и генерация кода проходят.
- `gradle/libs.versions.toml` — версии, библиотеки и bundles. Версии зависимостей указываются только здесь.
  Версия Spring Boot одна на весь проект. Модули со Spring подключают BOM `libs.spring.boot.bom` через
  `platform(...)`; версии стартеров, Hibernate, Liquibase, H2 и драйвера PostgreSQL задаёт BOM, в каталоге их нет.
- В `build.gradle.kts` модуля остаются только плагины сверх конвенций (`ksp`, `plugin.spring`, `plugin.jpa`,
  у `testsys-web` — ещё Spring Boot и Vaadin) и зависимости; исключение — задача `processResources`
  в `testsys-web:ui`, которая копирует CSS дизайн-системы в jar.

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

Запуск веб-приложения для разработки (витрина компонентов — `http://localhost:8080/dev/showcase`):

```bash
./gradlew :testsys-web:bootRun --args='--spring.profiles.active=dev'
```

Режиму разработки Vaadin нужен `com.vaadin:vaadin-dev`; он подключён как `developmentOnly`, поэтому есть в classpath
`bootRun`, но не попадает в `bootJar`. Плагин Vaadin генерирует `testsys-web/src/main/frontend/index.html`
(хранится в git) и `src/main/frontend/generated/` (в `.gitignore`).

`bootJar` собирает приложение в production-режиме. Пока своего клиентского кода нет, Vaadin использует готовый
бандл; Node.js (плагин ставит его в `~/.vaadin`) понадобится, когда появятся собственные клиентские модули.

Detekt 1.23 не разбирает context parameters (`context(name: Type)`): правила набора `formatting` на таком файле
падают с исключением. Файл с context parameters добавляется в `excludes` набора `formatting` в `detekt.yml`;
остальные правила на нём продолжают работать. Сейчас так исключён `OperationFailure.kt` из `testsys-operation`.

## CI

Workflow лежат в `.github/workflows`.

| Workflow                  | Когда                          | Что делает                                                      |
|---------------------------|--------------------------------|-----------------------------------------------------------------|
| `build.yml`               | push/PR в `master`, `dev`      | `./gradlew assemble` — компиляция и сборка без тестов, jar-артефакты, аннотации ошибок компиляции в PR |
| `test.yml`                | push/PR в `master`, `dev`      | `./gradlew check -x detekt -x detektMain` — все тесты; отчёт в Summary запуска, в check `Test report` и комментарием в PR, аннотации упавших тестов |
| `lint.yml`                | push/PR в `master`, `dev`      | `./gradlew detektMain -Pdetekt.autoCorrect=false --continue` — Detekt по всем модулям без правки файлов; таблица замечаний в Summary запуска, загрузка SARIF в GitHub Security |
| `check-source-branch.yml` | PR в `dev`                     | Разрешает PR только из веток `sh1sh4k1n9/`, `ch3zych3z/`, `KarasssDev/`, `DirewolfPrime/`, `LutovolkVPraime/` |
| `release.yml`             | —                              | Пока пустой                                                     |

## Куда класть новый код

| Что добавляем                                   | Куда                                                                                              |
|-------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Новую доменную сущность                         | `domain/model/<group\|task\|user>` + билдер + порт хранения, см. [implement-entity.md](../guides/implement-entity.md) |
| Хранение сущности в БД                          | `testsys-infra:database`, см. [implement-entity.md](../guides/implement-entity.md)               |
| Новый внешний порт (хранилище, внешняя система) | Интерфейс в `domain/contract`, реализация — в `testsys-infra`, см. [implement-port.md](../guides/implement-port.md) |
| Пользовательскую фичу                           | Метод с `@Feature` в `operation/user/<Actor>Operations.kt`, см. [implement-feature.md](../guides/implement-feature.md) |
| Локализованное сообщение                        | См. [add-localization.md](../guides/add-localization.md)                                          |
| Версию библиотеки                               | `gradle/libs.versions.toml`                                                                       |
| Токены, стили, эталонные React-компоненты       | `testsys-web/design-system`, см. [design-system/README.md](../../testsys-web/design-system/README.md) |
| Kotlin-компонент интерфейса, страницу Кабинета  | Компонент — `testsys-web/ui`, страница — `testsys-web`, см. [ui/README.md](../../testsys-web/ui/README.md) |
