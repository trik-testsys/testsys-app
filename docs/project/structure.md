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
├── testsys-web/              # Контейнер веб-модулей
│   ├── components/           # Kotlin-DSL, общие тексты и экран 404
│   ├── app/                  # Основное приложение
│   └── dev-app/              # Независимая витрина компонентов
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
| `testsys-infra:localization`         | Типобезопасный API локализованных сообщений, генерируемый из MF2-сообщений и форматируемый ICU4J MF2. | Реализован        |
| `testsys-web:app` | Основное приложение Vaadin Flow: точка входа, конфигурация и маршрутизация ошибок, см. [app/README.md](../../testsys-web/app/README.md). | Каркас; предметных страниц Кабинетов нет |
| `testsys-web:components` | Kotlin-DSL, общая фабрика текстов и визуальная часть 404, см. [components/README.md](../../testsys-web/components/README.md). Без Spring и домена. Общие UI-ресурсы и необходимые клиентские реализации находятся в стандартных resources-каталогах. | Реализован |
| `testsys-web:dev-app` | Самостоятельная витрина компонентов и демонстраций, см. [dev-app/README.md](../../testsys-web/dev-app/README.md). | Реализован |

Отсутствующие маршруты каждое веб-приложение обрабатывает само: см. раздел «Отсутствующие маршруты»
в [app/README.md](../../testsys-web/app/README.md#отсутствующие-маршруты) и
[dev-app/README.md](../../testsys-web/dev-app/README.md#отсутствующие-маршруты).

### Зависимости между модулями

Архитектура гексагональная: домен объявляет порты, инфраструктурные модули их реализуют, а веб-приложение
собирает всё вместе.

Правила:

- `testsys-domain` ни от чего не зависит. Любой новый код, которому нужен Spring, JPA или сеть, живёт вне домена.
- Инфраструктура зависит от домена, но не наоборот: домен знает только интерфейсы из `tech.testsys.domain.contract`.
- Сейчас в Gradle прописаны связи `operation → domain`, `database → domain`, `database → codegen-api`,
  `database → codegen` (через `ksp`), `app → components` и `dev-app → components`.
  Остальные связи — целевая архитектура.

## Сборка

- `settings.gradle.kts` — список модулей. Модуль, не попавший в него, не собирается, и его тесты не запускаются.
- `buildSrc/src/main/kotlin/testsys.conventions.gradle.kts` — общий плагин, который подключает каждый модуль:
  Kotlin JVM 21, `allWarningsAsErrors = true`, JUnit Platform, Detekt (`detekt.yml`, `autoCorrect = true`,
  `build/generated/` исключён). Задача `check` зависит от общего агрегатора `detekt`.
  Detekt — версии 2 (плагин `dev.detekt`,
  пока альфа — единственная ветка Detekt с поддержкой Kotlin 2.4); вместо `build.maxIssues` сборку валит
  `failOnSeverity = FailOnSeverity.Info` (падает на замечании любой значимости), а правила ktlint подключены через
  `dev.detekt:detekt-rules-ktlint-wrapper` вместо устаревшего `detekt-formatting`.
- `gradle/libs.versions.toml` — версии, библиотеки и bundles. Версии зависимостей указываются только здесь.
  Версия Spring Boot одна на весь проект. Модули со Spring подключают BOM `libs.spring.boot.bom` через
  `platform(...)`; версии стартеров, Hibernate, Liquibase, H2 и драйвера PostgreSQL задаёт BOM, в каталоге их нет.
- `gradle.properties` — память Gradle-демона (`org.gradle.jvmargs`): значений Gradle по умолчанию полной сборке
  с KSP, Vaadin и Detekt не хватает. Демон Kotlin наследует эти параметры.
- В `build.gradle.kts` модуля, кроме плагинов сверх конвенций (`ksp`, `plugin.spring`, `plugin.jpa`, у веб-приложений —
  ещё Spring Boot и Vaadin в `app` и `dev-app`) и зависимостей, может быть и модуль-специфичная логика сборки — она остаётся в скрипте
  своего модуля. Так, в `testsys-infra/localization` это кодогенерация (см.
  [localization/README.md](../../testsys-infra/localization/README.md)), в `testsys-web:components` — сохранение временных
  меток файлов jar. Общая политика frontend применяется helper-ом; см. [Сборка веб-приложений](#сборка-веб-приложений).

Агрегатор `detekt` лениво подключает задачи анализа компиляций (`detekt<Compilation>`), автоматически создаваемые
плагином Detekt, включая тесты и дополнительные source set, объявленные модулем после применения конвенций. Каждая
такая задача проверяет свои исходники со своим classpath и разрешением типов. Задачи `detekt<SourceSet>SourceSet`
анализируют те же файлы, поэтому агрегатор их не запускает; у самого агрегатора источник пуст, и повторного анализа
нет. Конфигурация задаётся через расширение Detekt, отчёты SARIF включены для всех задач анализа.

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

### Сборка веб-приложений

Запуск основного приложения (порт 8080; пока доступны только обработчики отсутствующих маршрутов):

```bash
./gradlew :testsys-web:app:bootRun -Pdetekt.autoCorrect=false
```

Запуск витрины компонентов (`http://localhost:8081/dev/showcase`):

```bash
./gradlew :testsys-web:dev-app:bootRun -Pdetekt.autoCorrect=false --args='--spring.profiles.active=dev'
```

Исходники веб-подмодулей организованы по пакетам, описанным в их README:
[components](../../testsys-web/components/README.md#устройство),
[app](../../testsys-web/app/README.md) и
[dev-app](../../testsys-web/dev-app/README.md#устройство-исходников).

Корневой `testsys-web` не содержит запускаемого приложения; у каждого приложения свой `src/main/frontend/`
и собственные generated/build-результаты.

Режиму разработки Vaadin нужен `com.vaadin:vaadin-dev`; он подключён как `developmentOnly`, поэтому есть в classpath
`bootRun`, но не попадает в `bootJar`. Плагин Vaadin создаёт HTML-оболочку в `src/main/frontend/generated/index.html`
каждого приложения. Каталог `src/main/frontend/generated/` игнорируется Git; отдельные исключения также покрывают
`app/src/main/frontend/index.html` и `dev-app/src/main/frontend/index.html`.
Свои файлы (в том числе настройки dev-сервера)
dev-режим кладёт в `build/` — так задаёт `vaadin.build.folder` в `application.yml`; без него, при запуске без
токен-файла Gradle-плагина, использовался бы каталог Maven `target/`.

Приложение обновляет открытые страницы без действия пользователя через push Vaadin по WebSocket (`/VAADIN/push`),
см. раздел «Живые обновления» в [components/README.md](../../testsys-web/components/README.md). Реверс-прокси перед приложением
должен пропускать заголовки `Upgrade`/`Connection` для этого пути. При нескольких экземплярах приложения нужны
sticky sessions: сессия Vaadin и состояние её UI живут в памяти одного экземпляра. Живые обновления между
экземплярами не распространяются: фоновые `refresh`/`reload` и локальные сигналы действуют только на страницы
своего экземпляра.

`bootJar` собирает приложение в production-режиме, включая собственные клиентские адаптеры из jar `components`.
Node.js плагин устанавливает в `~/.vaadin`; упаковка клиентских компонентов описана в
[components/README.md](../../testsys-web/components/README.md), раздел «Клиентская реализация компонентов».

Production-сборку фронтенда (`vaadinBuildFrontend`) выполняют только запуски с `bootJar` или `bootBuildImage`
(`assemble` и `build` включают `bootJar`); `bootRun`, тесты и `check` её пропускают. Условие реализует
`configureWebFrontend` в [WebFrontend.kt](../../buildSrc/src/main/kotlin/WebFrontend.kt), применяемый обоими приложениями.
Плагин Vaadin 25.2–25.3 включает production-режим, если задача `bootJar` просто есть в проекте,
и без условия собирал бы фронтенд перед каждым запуском и тестами.

HTTP-тесты обоих приложений запускают Vaadin в production-режиме. Для начального ответа они используют минимальный
`index.html` из `src/test/resources/META-INF/VAADIN/webapp/` своего модуля. Тестовая HTML-оболочка позволяет проверить
HTTP-код и cookie сессии после `clean`, без сборки клиентского JavaScript. Она не входит в `bootJar`;
работу клиентского кода эти тесты не проверяют.

Vaadin 25.3 регистрирует сервис восстановления production-токена `vaadinBuildFrontendToken` один раз
на весь Gradle build. При сборке двух приложений он восстанавливает токен только первого: второй JAR
может ошибочно запускаться в dev-режиме. Поэтому `bootJar` каждого приложения отдельно включает
свой `build/cached-flow-build-info.json` под именем `META-INF/VAADIN/config/flow-build-info.json`.
Это не меняет токен запуска `bootRun`; обход можно удалить после исправления сервиса в плагине.

## CI

Workflow лежат в `.github/workflows`.

| Workflow                  | Когда                          | Что делает                                                      |
|---------------------------|--------------------------------|-----------------------------------------------------------------|
| `build.yml`               | push/PR в `master`, `dev`      | `./gradlew assemble` — компиляция и сборка без тестов, jar-артефакты, аннотации ошибок компиляции в PR |
| `test.yml`                | push/PR в `master`, `dev`      | `./gradlew testAll -Pdetekt.autoCorrect=false --no-daemon` — все тесты без Detekt, затем клиентские тесты `components` на Node 24 (`node --test`); отчёт в Summary запуска, в check `Test report` и комментарием в PR, аннотации упавших тестов |
| `lint.yml`                | push/PR в `master`, `dev`      | `./gradlew detekt -Pdetekt.autoCorrect=false --continue --no-daemon` — все source set всех модулей без правки файлов; таблица замечаний в Summary запуска, исходные и подготовленные SARIF-артефакты; отдельная загрузка в GitHub Security для каждого модуля с сохранением категории на модуль и задачу |
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
| Токены, стили, бренд и иконки | `components/src/main/resources/META-INF/resources/testsys-ui/`, правила — [ui-design.md](ui-design.md) |
| Необходимая клиентская реализация компонента | `components/src/main/resources/META-INF/frontend/testsys-ui/`, API — components/README.md |
| Kotlin-компонент интерфейса, страницу Кабинета  | Компонент — `testsys-web/components`, рабочая страница — `testsys-web/app`, витрина — `testsys-web/dev-app`, см. [components/README.md](../../testsys-web/components/README.md) |
