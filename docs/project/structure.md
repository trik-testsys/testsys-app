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
├── gradle.properties         # Параметры JVM Gradle-демона (память)
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
| `testsys-web`                        | Веб-приложение на Vaadin Flow: точка входа, тема, тексты интерфейса из локализации, страницы. Дизайн-система — в `design-system/`, см. [design-system/README.md](../../testsys-web/design-system/README.md). | Каркас: dev-витрина, собственная 404, страниц Кабинетов нет |
| `testsys-web:ui`                     | Kotlin-DSL дизайн-системы на Vaadin Flow: сетка, блоки, компоненты, см. [ui/README.md](../../testsys-web/ui/README.md). Без Spring и домена. | В разработке |

Отсутствующие маршруты обрабатывает `testsys-web/src/main/kotlin/tech/testsys/web/error/NotFoundView.kt`
через штатный `HasErrorParameter<NotFoundException>` с кодом 404 во всех профилях. Экран показывает бренд,
локализованные заголовок, пояснение и действие «Назад» по истории браузера; до определения истории и без предыдущей
записи кнопка недоступна. Ссылки на выдуманную главную, подробностей исключения и списка dev-маршрутов нет.
Страницы витрины вне профиля `dev` также переходят на этот экран.
`vaadin.eagerServerLoad=true` в `application.yml` включает начальную серверную маршрутизацию до отправки HTML:
код 404 получает и первый HTTP-запрос, а не только последующая навигация. Это создаёт сессию и UI уже при первом
запросе страницы; без этой настройки Vaadin отдаёт HTML с кодом 200 и разрешает маршрут позднее.

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
  `build/generated/` исключён). Задача `check` зависит от `detektMain`. Detekt — версии 2 (плагин `dev.detekt`,
  пока альфа — единственная ветка Detekt с поддержкой Kotlin 2.4); вместо `build.maxIssues` сборку валит
  `failOnSeverity = FailOnSeverity.Info` (падает на замечании любой значимости), а правила ktlint подключены через
  `dev.detekt:detekt-rules-ktlint-wrapper` вместо устаревшего `detekt-formatting`.
- `gradle/libs.versions.toml` — версии, библиотеки и bundles. Версии зависимостей указываются только здесь.
  Версия Spring Boot одна на весь проект. Модули со Spring подключают BOM `libs.spring.boot.bom` через
  `platform(...)`; версии стартеров, Hibernate, Liquibase, H2 и драйвера PostgreSQL задаёт BOM, в каталоге их нет.
- `gradle.properties` — память Gradle-демона (`org.gradle.jvmargs`): значений Gradle по умолчанию полной сборке
  с KSP, Vaadin и Detekt не хватает. Демон Kotlin наследует эти параметры.
- В `build.gradle.kts` модуля, кроме плагинов сверх конвенций (`ksp`, `plugin.spring`, `plugin.jpa`, у `testsys-web` —
  ещё Spring Boot и Vaadin) и зависимостей, может быть и модуль-специфичная логика сборки — она остаётся в скрипте
  своего модуля. Так, в `testsys-infra/localization` это кодогенерация (см.
  [localization/README.md](../../testsys-infra/localization/README.md)), в `testsys-web:ui` — копирование CSS
  и канонического React дизайн-системы в jar и сохранение временных меток его файлов, в `testsys-web` — условие задачи
  `vaadinBuildFrontend` (см. ниже).

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
(хранится в git) и `src/main/frontend/generated/` (в `.gitignore`). Свои файлы (в том числе настройки dev-сервера)
dev-режим кладёт в `build/` — так задаёт `vaadin.build.folder` в `application.yml`; без него, при запуске без
токен-файла Gradle-плагина, использовался бы каталог Maven `target/`.

Приложение обновляет открытые страницы без действия пользователя через push Vaadin по WebSocket (`/VAADIN/push`),
см. раздел «Живые обновления» в [ui/README.md](../../testsys-web/ui/README.md). Реверс-прокси перед приложением
должен пропускать заголовки `Upgrade`/`Connection` для этого пути. При нескольких экземплярах приложения нужны
sticky sessions: сессия Vaadin и состояние её UI живут в памяти одного экземпляра. Живые обновления между
экземплярами не распространяются: фоновые `refresh`/`reload` и локальные сигналы действуют только на страницы
своего экземпляра.

`bootJar` собирает приложение в production-режиме, включая собственные клиентские адаптеры из jar UI.
Node.js плагин устанавливает в `~/.vaadin`; упаковка канонического React описана в
[ui/README.md](../../testsys-web/ui/README.md), раздел «Упаковка React».

Production-сборку фронтенда (`vaadinBuildFrontend`) выполняют только запуски с `bootJar` или `bootBuildImage`
(`assemble` и `build` включают `bootJar`); `bootRun`, тесты и `check` её пропускают. Условие проверяет граф задач
в `testsys-web/build.gradle.kts`: плагин Vaadin 25.2–25.3 включает production-режим, если задача `bootJar` просто есть
в проекте, и без условия собирал бы фронтенд перед каждым запуском и тестами.

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
